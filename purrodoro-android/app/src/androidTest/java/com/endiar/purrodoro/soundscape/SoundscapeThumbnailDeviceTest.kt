package com.endiar.purrodoro.soundscape

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoundscapeThumbnailDeviceTest {

    @Test
    fun loadsRealCloudThumbnailOnDevice() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = SoundscapeModule.get(context).thumbnailLoader
        val bitmap = loader.loadBitmap("lofi/lofi_cedar_rain_study.webp", cacheVersion = 88_001L)
        assertNotNull(bitmap)
    }
}
