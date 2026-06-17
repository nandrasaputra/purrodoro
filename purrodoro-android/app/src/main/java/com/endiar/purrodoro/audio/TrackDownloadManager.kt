package com.endiar.purrodoro.audio

import android.content.Context
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Downloads cloud soundscape tracks to internal storage and tracks their
 * [DownloadState] per track id. Files are named `{id}-v{updatedAtMillis}.mp3`
 * so a server-side audio replacement (which bumps updated_at) naturally
 * invalidates the old file; [prune] sweeps files that no longer correspond to
 * a current catalog row. Internal storage needs no permissions and is wiped
 * on uninstall.
 */
class TrackDownloadManager(
    context: Context,
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    private val dir = File(context.applicationContext.filesDir, "soundscapes")

    private val _states = MutableStateFlow<Map<Int, DownloadState>>(emptyMap())
    val states: StateFlow<Map<Int, DownloadState>> = _states.asStateFlow()

    fun localFile(track: SoundscapeTrackEntity): File =
        File(dir, "${track.id}-v${track.updatedAtMillis}.mp3")

    fun isDownloaded(track: SoundscapeTrackEntity): Boolean = localFile(track).exists()

    /** On-disk size of the downloaded file in bytes, or 0 if not present. */
    fun fileSizeBytes(track: SoundscapeTrackEntity): Long =
        localFile(track).let { if (it.exists()) it.length() else 0L }

    /** Recompute states from disk for the current catalog snapshot. */
    fun refreshStates(tracks: List<SoundscapeTrackEntity>) {
        _states.value = tracks.associate { track ->
            track.id to if (isDownloaded(track)) DownloadState.Downloaded else DownloadState.NotDownloaded
        }
    }

    /**
     * Streams the track to a temp file, then renames into place so a partial
     * download is never mistaken for a complete one. Returns success.
     */
    suspend fun download(track: SoundscapeTrackEntity): Boolean {
        val target = localFile(track)
        if (target.exists()) {
            setState(track.id, DownloadState.Downloaded)
            return true
        }
        dir.mkdirs()
        val tmp = File(dir, "${target.name}.part")
        setState(track.id, DownloadState.Downloading(0f))
        val result = runCatching {
            val response = httpClient.get(
                "$baseUrl/storage/v1/object/public/soundscape-audio/${track.filePath}"
            ) {
                onDownload { bytesSentTotal, contentLength ->
                    val total = contentLength ?: track.fileSizeBytes
                    if (total > 0) {
                        val progress = (bytesSentTotal.toFloat() / total).coerceIn(0f, 1f)
                        setState(track.id, DownloadState.Downloading(progress))
                    }
                }
            }
            check(response.status.isSuccess()) { "HTTP ${response.status}" }
            tmp.outputStream().use { out -> response.bodyAsChannel().copyTo(out) }
            check(tmp.renameTo(target)) { "rename failed" }
        }
        tmp.delete()
        return if (result.isSuccess) {
            setState(track.id, DownloadState.Downloaded)
            true
        } else {
            target.delete()
            setState(track.id, DownloadState.NotDownloaded)
            false
        }
    }

    fun delete(track: SoundscapeTrackEntity) {
        localFile(track).delete()
        setState(track.id, DownloadState.NotDownloaded)
    }

    /** Delete files that don't match any current catalog row (id + version). */
    fun prune(tracks: List<SoundscapeTrackEntity>) {
        val valid = tracks.map { localFile(it).name }.toSet()
        dir.listFiles().orEmpty()
            .filter { it.name !in valid }
            .forEach { it.delete() }
        refreshStates(tracks)
    }

    private fun setState(id: Int, state: DownloadState) {
        _states.update { it + (id to state) }
    }
}
