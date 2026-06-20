package com.endiar.purrodoro.audio

import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.soundscape.SoundscapeConfig
import io.ktor.client.HttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackDownloadManagerTest {

    private val manager = TrackDownloadManager(
        context = ApplicationProvider.getApplicationContext(),
        httpClient = HttpClient(),
        audioPublicBaseUrl = SoundscapeConfig.R2_PUBLIC_URL,
    )

    @Test
    fun `downloadUrl points at public R2 object`() {
        assertEquals(
            "${SoundscapeConfig.R2_PUBLIC_URL}/lofi/lofi_cedar_rain_study.mp3",
            manager.downloadUrl("lofi/lofi_cedar_rain_study.mp3"),
        )
    }
}
