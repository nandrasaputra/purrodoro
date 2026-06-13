package com.endiar.purrodoro.audio

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SoundscapeLoopPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `equal sources compare equal so resume path is reachable`() {
        // The player's resume-vs-restart branch relies on AudioSource value
        // equality; this pins that contract for both variants.
        assertEquals(AudioSource.Bundled(42), AudioSource.Bundled(42))
        assertEquals(
            AudioSource.LocalFile("/data/x/soundscapes/100-v1.mp3"),
            AudioSource.LocalFile("/data/x/soundscapes/100-v1.mp3"),
        )
    }

    @Test
    fun `playing a missing local file does not crash and is a no-op`() {
        val player = SoundscapeLoopPlayer(context)
        player.play(AudioSource.LocalFile("/nonexistent/path/track.mp3"), volume = 0.5f)
        player.pause()   // must not throw — nothing was loaded
        player.release() // must not throw
    }
}
