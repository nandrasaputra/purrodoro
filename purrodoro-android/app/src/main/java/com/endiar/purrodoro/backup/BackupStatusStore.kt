package com.endiar.purrodoro.backup

import java.io.File

data class BackupStatus(
    val manualExportAt: Long? = null,
    val autoBackupAt: Long? = null,
) {
    fun latestAt(): Long? = listOfNotNull(manualExportAt, autoBackupAt).maxOrNull()
}

/**
 * Timestamps for this phone only. The file lives under `files/backup-status/`,
 * which Auto Backup excludes, so a restore does not overwrite it.
 */
object BackupStatusStore {
    private const val DIR = "backup-status"
    private const val FILE = "status.txt"
    private const val KEY_MANUAL = "manual"
    private const val KEY_AUTO = "auto"

    fun read(filesDir: File): BackupStatus {
        val file = statusFile(filesDir)
        if (!file.exists()) return BackupStatus()
        val values = runCatching { file.readLines() }.getOrDefault(emptyList())
            .mapNotNull { line ->
                val parts = line.split('=', limit = 2)
                if (parts.size != 2) null else parts[0] to parts[1].toLongOrNull()
            }
            .toMap()
        return BackupStatus(
            manualExportAt = values[KEY_MANUAL],
            autoBackupAt = values[KEY_AUTO],
        )
    }

    fun recordManualExport(filesDir: File, at: Long = System.currentTimeMillis()) {
        update(filesDir) { it.copy(manualExportAt = at) }
    }

    fun recordAutoBackup(filesDir: File, at: Long = System.currentTimeMillis()) {
        update(filesDir) { it.copy(autoBackupAt = at) }
    }

    private fun update(filesDir: File, transform: (BackupStatus) -> BackupStatus) {
        synchronized(this) {
            val next = transform(read(filesDir))
            val file = statusFile(filesDir)
            file.parentFile?.mkdirs()
            file.writeText(buildString {
                next.manualExportAt?.let { append(KEY_MANUAL).append('=').append(it).append('\n') }
                next.autoBackupAt?.let { append(KEY_AUTO).append('=').append(it).append('\n') }
            })
        }
    }

    private fun statusFile(filesDir: File) = File(File(filesDir, DIR), FILE)
}
