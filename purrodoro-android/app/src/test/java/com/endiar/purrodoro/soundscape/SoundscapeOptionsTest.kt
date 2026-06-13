package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundscapeOptionsTest {

    private fun entity(id: Int, kind: String) = SoundscapeTrackEntity(
        id = id,
        kind = kind,
        name = "Name $id",
        artist = "Artist $id",
        symbol = "Wave",
        filePath = "x/$id.mp3",
        fileSizeBytes = 2_000_000,
        isPremium = true,
        sortOrder = 0,
        updatedAtMillis = 1_000,
    )

    @Test
    fun `music entity maps to a locked cloud MusicOption`() {
        val option = entity(100, "music").toMusicOption(
            downloadState = DownloadState.NotDownloaded,
            localPath = null,
        )
        assertEquals(100, option.id)
        assertTrue(option.locked)
        assertNull(option.audio)
        assertEquals(DownloadState.NotDownloaded, option.remote?.downloadState)
    }

    @Test
    fun `downloaded entity maps with a LocalFile source`() {
        val option = entity(100, "music").toMusicOption(
            downloadState = DownloadState.Downloaded,
            localPath = "/files/soundscapes/100-v1000.mp3",
        )
        assertEquals(AudioSource.LocalFile("/files/soundscapes/100-v1000.mp3"), option.audio)
    }

    @Test
    fun `ambience entity maps to a SoundOption with symbol and description`() {
        val option = entity(200, "ambience").toSoundOption(
            downloadState = DownloadState.Downloaded,
            localPath = "/files/soundscapes/200-v1000.mp3",
        )
        assertEquals("Wave", option.symbol)
        assertEquals("Artist 200", option.description)
        assertEquals(AudioSource.LocalFile("/files/soundscapes/200-v1000.mp3"), option.audio)
    }
}
