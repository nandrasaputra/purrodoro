package com.endiar.purrodoro.soundscape

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SoundscapeThumbnailLoaderIntegrationTest {

    @Test
    fun `loads a real cloud thumbnail from R2`() = runTest {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val loader = SoundscapeThumbnailLoader(
            context = context,
            httpClient = SoundscapeHttp.createKtorClient(),
            publicBaseUrl = SoundscapeConfig.R2_PUBLIC_URL,
        )
        val bitmap = loader.loadBitmap("lofi/lofi_cedar_rain_study.webp", cacheVersion = 99_001L)
        assertNotNull("Expected R2 thumbnail to download and decode", bitmap)
        assertTrue(bitmap!!.width > 0)
        assertTrue(bitmap.height > 0)
    }
}
