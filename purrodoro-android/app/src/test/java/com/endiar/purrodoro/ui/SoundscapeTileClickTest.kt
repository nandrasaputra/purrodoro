package com.endiar.purrodoro.ui

import com.endiar.purrodoro.model.DownloadState
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.RemoteTrackInfo
import com.endiar.purrodoro.model.SoundOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundscapeTileClickTest {

    private val lockedCloudStorm = SoundOption(
        id = 120,
        name = "Storm",
        description = "Rolling thunder",
        locked = true,
        symbol = "Storm",
        audio = null,
        remote = RemoteTrackInfo(
            fileSizeBytes = 484_039,
            downloadState = DownloadState.NotDownloaded,
            thumbnailPath = "ambience/amb_storm.webp",
            filePath = "ambience/amb_storm.mp3",
            cacheVersion = 1L,
        ),
    )

    @Test
    fun `locked cloud ambience on free tier opens upsell not download`() {
        var upsell = false
        var downloadId: Int? = null
        ambienceTileClick(
            sound = lockedCloudStorm,
            isPlaying = false,
            isPremium = false,
            onAmbienceSelected = { upsell = true },
            onAmbienceToggle = {},
            onDownloadTrack = { downloadId = it },
        )()
        assertTrue(upsell)
        assertEquals(null, downloadId)
    }

    @Test
    fun `locked cloud ambience on premium tier starts download`() {
        var upsell = false
        var downloadId: Int? = null
        ambienceTileClick(
            sound = lockedCloudStorm,
            isPlaying = false,
            isPremium = true,
            onAmbienceSelected = { upsell = true },
            onAmbienceToggle = {},
            onDownloadTrack = { downloadId = it },
        )()
        assertEquals(false, upsell)
        assertEquals(120, downloadId)
    }

    @Test
    fun `locked cloud music on premium tier starts download`() {
        val track = MusicOption(
            id = 200,
            name = "Premium Lofi",
            artist = "Artist",
            audio = null,
            locked = true,
            remote = RemoteTrackInfo(
                fileSizeBytes = 1_000_000,
                downloadState = DownloadState.NotDownloaded,
                thumbnailPath = "lofi/foo.webp",
                filePath = "lofi/foo.mp3",
                cacheVersion = 1L,
            ),
        )
        var downloadId: Int? = null
        musicTileClick(
            track = track,
            isPlaying = false,
            isPremium = true,
            onMusicSelected = {},
            onMusicToggle = {},
            onDownloadTrack = { downloadId = it },
        )()
        assertEquals(200, downloadId)
    }
}
