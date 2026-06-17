package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.R
import org.junit.Assert.assertEquals
import org.junit.Test

class BundledTrackRegistryTest {

    @Test
    fun `resolveRaw maps ambience_rain to R raw id`() {
        assertEquals(R.raw.ambience_rain, BundledTrackRegistry.rawResId("ambience_rain"))
    }

    @Test
    fun `defaults include six ambience and three music tracks`() {
        assertEquals(6, BundledTrackRegistry.defaults.count { it.kind == "ambience" })
        assertEquals(3, BundledTrackRegistry.defaults.count { it.kind == "music" })
    }
}
