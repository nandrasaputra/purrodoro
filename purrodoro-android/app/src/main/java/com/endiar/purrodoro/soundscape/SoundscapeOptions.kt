package com.endiar.purrodoro.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.data.local.SoundscapeBundledEntity
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.RemoteTrackInfo
import com.endiar.purrodoro.model.SoundOption

// Maps catalog cache rows to the option models the Sounds UI renders.
// `audio` is only non-null once the file is on disk; `is_premium` reuses the
// existing locked/premium gating.

fun SoundscapeTrackEntity.toMusicOption(
    downloadState: DownloadState,
    localPath: String?,
): MusicOption = MusicOption(
    id = id,
    name = name,
    artist = artist,
    audio = localPath?.let { AudioSource.LocalFile(it) },
    locked = isPremium,
    remote = RemoteTrackInfo(
        fileSizeBytes = fileSizeBytes,
        downloadState = downloadState,
        thumbnailPath = SoundscapeConfig.thumbnailKey(thumbnailPath, filePath),
        filePath = filePath,
        cacheVersion = updatedAtMillis,
    ),
)

fun SoundscapeTrackEntity.toSoundOption(
    downloadState: DownloadState,
    localPath: String?,
): SoundOption = SoundOption(
    id = id,
    name = name,
    description = artist,
    locked = isPremium,
    symbol = symbol,
    audio = localPath?.let { AudioSource.LocalFile(it) },
    remote = RemoteTrackInfo(
        fileSizeBytes = fileSizeBytes,
        downloadState = downloadState,
        thumbnailPath = SoundscapeConfig.thumbnailKey(thumbnailPath, filePath),
        filePath = filePath,
        cacheVersion = updatedAtMillis,
    ),
)

/** Bundled display strings resolve via [stringResource] so locale changes recompose. */
@Composable
fun buildLocalizedAmbienceOptions(
    bundledRows: List<SoundscapeBundledEntity>,
    remoteRows: List<SoundscapeTrackEntity>,
    downloadStates: Map<Int, DownloadState>,
    localPaths: Map<Int, String>,
): List<SoundOption> {
    val bundledNames = BundledTrackRegistry.defaults
        .filter { it.kind == "ambience" }
        .associate { it.localId to stringResource(it.nameRes) }
    val bundledSubtitles = BundledTrackRegistry.defaults
        .filter { it.kind == "ambience" }
        .associate { it.localId to stringResource(it.subtitleRes) }
    return SoundscapeCatalogBuilder.buildAmbienceOptions(
        bundledRows = bundledRows,
        remoteRows = remoteRows,
        downloadStates = downloadStates,
        localPaths = localPaths,
        nameFor = { _, def -> bundledNames.getValue(def.localId) },
        subtitleFor = { _, def -> bundledSubtitles.getValue(def.localId) },
    )
}

@Composable
fun buildLocalizedMusicOptions(
    bundledRows: List<SoundscapeBundledEntity>,
    remoteRows: List<SoundscapeTrackEntity>,
    downloadStates: Map<Int, DownloadState>,
    localPaths: Map<Int, String>,
): List<MusicOption> {
    val bundledNames = BundledTrackRegistry.defaults
        .filter { it.kind == "music" }
        .associate { it.localId to stringResource(it.nameRes) }
    val bundledSubtitles = BundledTrackRegistry.defaults
        .filter { it.kind == "music" }
        .associate { it.localId to stringResource(it.subtitleRes) }
    return SoundscapeCatalogBuilder.buildMusicOptions(
        bundledRows = bundledRows,
        remoteRows = remoteRows,
        downloadStates = downloadStates,
        localPaths = localPaths,
        nameFor = { _, def -> bundledNames.getValue(def.localId) },
        subtitleFor = { _, def -> bundledSubtitles.getValue(def.localId) },
    )
}
