package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.audio.AudioSource
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
    remote = RemoteTrackInfo(fileSizeBytes = fileSizeBytes, downloadState = downloadState),
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
    remote = RemoteTrackInfo(fileSizeBytes = fileSizeBytes, downloadState = downloadState),
)
