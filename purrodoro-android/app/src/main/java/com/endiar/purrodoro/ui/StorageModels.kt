package com.endiar.purrodoro.ui

import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import java.util.Locale

/**
 * One downloadable asset as shown on the Storage screen. Generic on purpose:
 * today every item is a soundscape track, but the model carries no
 * soundscape-specific shape so other downloadable content can join later.
 */
data class StorageItem(
    val id: Int,
    val name: String,
    val kind: String,          // "music" | "ambience"
    val sizeBytes: Long,
    val isRemovable: Boolean,  // false while a download is in progress
    val progress: Float?,      // non-null (0f..1f) while downloading, else null
)

data class StorageSummary(
    val items: List<StorageItem>,
    /** Sum of the *downloaded* item sizes (in-progress rows excluded). */
    val totalBytes: Long,
)

/**
 * Pure builder for the Storage screen.
 *
 * - **Downloaded** tracks become removable rows (real on-disk size via [sizeFor]).
 * - **Downloading** tracks appear as non-removable rows carrying progress.
 * - **NotDownloaded** tracks are omitted.
 *
 * Rows sort downloaded-first, then by size desc, then name. The total counts
 * only downloaded items (we don't track orphaned/other on-disk bytes for now).
 */
fun buildStorageList(
    catalog: List<SoundscapeTrackEntity>,
    downloadStates: Map<Int, DownloadState>,
    sizeFor: (SoundscapeTrackEntity) -> Long,
): StorageSummary {
    val items = catalog.mapNotNull { track ->
        when (val state = downloadStates[track.id] ?: DownloadState.NotDownloaded) {
            DownloadState.Downloaded -> StorageItem(
                id = track.id,
                name = track.name,
                kind = track.kind,
                sizeBytes = sizeFor(track),
                isRemovable = true,
                progress = null,
            )
            is DownloadState.Downloading -> StorageItem(
                id = track.id,
                name = track.name,
                kind = track.kind,
                sizeBytes = 0L,
                isRemovable = false,
                progress = state.progress,
            )
            DownloadState.NotDownloaded -> null
        }
    }.sortedWith(
        compareBy<StorageItem> { it.progress != null } // downloaded (progress == null) first
            .thenByDescending { it.sizeBytes }
            .thenBy { it.name.lowercase() },
    )
    val total = items.filter { it.progress == null }.sumOf { it.sizeBytes }
    return StorageSummary(items = items, totalBytes = total)
}

/** Human-readable byte size, e.g. "0 MB", "740 KB", "12.4 MB". Locale-aware decimals. */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
    val kb = 1024.0
    val mb = kb * 1024
    return when {
        bytes <= 0L -> "0 MB"
        bytes < mb -> String.format(locale, "%.0f KB", bytes / kb)
        else -> String.format(locale, "%.1f MB", bytes / mb)
    }
}
