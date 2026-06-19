package com.endiar.purrodoro.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.endiar.purrodoro.data.AppRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.flow.first

/** Outcome of a restore attempt, mapped to user-facing copy by the UI. */
sealed interface RestoreOutcome {
    data object Success : RestoreOutcome
    data object Malformed : RestoreOutcome
    data class UnsupportedVersion(val version: Int) : RestoreOutcome
    data object TooLarge : RestoreOutcome

    /** Failed before any data was touched (read/IO). Data is unchanged. */
    data object ReadError : RestoreOutcome

    /**
     * The decode succeeded and [AppRepository.applyRestore] was attempted but
     * threw. Data may be partially applied — but a safety backup of the prior
     * state was written first, so recovery is possible.
     */
    data object ApplyError : RestoreOutcome
}

/**
 * Orchestrates Backup & Restore file IO around the pure [BackupCodec] and the
 * transactional [AppRepository.applyRestore].
 *
 * IO is caller-dispatched (run me on `Dispatchers.IO`). Reads are size-capped to
 * stop a malicious/huge file from exhausting memory. Before any restore is
 * applied, the current state is written to a private **safety backup** so a
 * mistaken restore is recoverable.
 */
class BackupManager(
    private val repo: AppRepository,
    private val appVersionName: String,
    /** App-private directory for automatic pre-restore safety copies. */
    private val safetyDir: File,
) {

    companion object {
        /** Hard cap on a backup file we will read (10 MiB). Real backups are KBs. */
        const val MAX_FILE_BYTES: Int = 10 * 1024 * 1024
        private const val MAX_SAFETY_BACKUPS = 3

        /** Reads [input] fully unless it exceeds [max] bytes. Internal for testing. */
        internal fun readCapped(input: InputStream, max: Int): ReadResult {
            val out = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var total = 0
            while (true) {
                val n = input.read(chunk)
                if (n == -1) break
                total += n
                if (total > max) return ReadResult.TooLarge
                out.write(chunk, 0, n)
            }
            return ReadResult.Ok(out.toString(Charsets.UTF_8.name()))
        }
    }

    /** Serializes the current app state to [uri]. Returns true on success. */
    suspend fun export(resolver: ContentResolver, uri: Uri): Boolean {
        val json = BackupCodec.encode(repo.appState.first(), appVersionName, System.currentTimeMillis())
        return runCatching {
            resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: return false
            true
        }.getOrDefault(false)
    }

    /**
     * Reads, validates, and applies a backup from [uri]. Writes a safety copy of
     * the current data first. Never mutates app data unless the file decodes
     * cleanly.
     */
    suspend fun restore(resolver: ContentResolver, uri: Uri): RestoreOutcome {
        val read = try {
            resolver.openInputStream(uri)?.use { readCapped(it, MAX_FILE_BYTES) }
        } catch (e: Exception) {
            return RestoreOutcome.ReadError
        } ?: return RestoreOutcome.ReadError

        val text = when (read) {
            ReadResult.TooLarge -> return RestoreOutcome.TooLarge
            is ReadResult.Ok -> read.text
        }
        return applyDecoded(text)
    }

    /**
     * Decode + safety-backup + apply for already-read [text]. Split out from
     * [restore] so the decode→apply→outcome mapping is unit-testable without a
     * [ContentResolver].
     */
    internal suspend fun applyDecoded(text: String): RestoreOutcome =
        when (val parsed = BackupCodec.decode(text)) {
            BackupParseResult.Malformed -> RestoreOutcome.Malformed
            is BackupParseResult.UnsupportedVersion ->
                RestoreOutcome.UnsupportedVersion(parsed.version)
            is BackupParseResult.Success -> {
                writeSafetyBackup()
                runCatching { repo.applyRestore(parsed.bundle) }
                    .fold(onSuccess = { RestoreOutcome.Success }, onFailure = { RestoreOutcome.ApplyError })
            }
        }

    /** Best-effort snapshot of current data to [safetyDir]; failures are non-fatal. */
    private suspend fun writeSafetyBackup() {
        runCatching {
            if (!safetyDir.exists()) safetyDir.mkdirs()
            val json = BackupCodec.encode(repo.appState.first(), appVersionName, System.currentTimeMillis())
            File(safetyDir, "autobackup-${System.currentTimeMillis()}.json")
                .writeText(json, Charsets.UTF_8)
            pruneSafetyBackups()
        }
    }

    private fun pruneSafetyBackups() {
        val files = safetyDir.listFiles { f -> f.isFile && f.name.startsWith("autobackup-") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        files.drop(MAX_SAFETY_BACKUPS).forEach { runCatching { it.delete() } }
    }

    /** Capped read result: [Ok] holds the text, [TooLarge] means the cap was hit. */
    internal sealed interface ReadResult {
        data class Ok(val text: String) : ReadResult
        data object TooLarge : ReadResult
    }
}
