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
    fun `resolveRawResId falls back when catalog name is unknown`() {
        assertEquals(
            R.raw.ambience_rain,
            BundledTrackRegistry.resolveRawResId("stale_server_name", "ambience_rain"),
        )
    }

    @Test
    fun `resolveRawResId trims whitespace on catalog name`() {
        assertEquals(
            R.raw.ambience_rain,
            BundledTrackRegistry.resolveRawResId("  ambience_rain  ", "ambience_cafe"),
        )
    }

    @Test
    fun `resolveRawResId returns null when neither name resolves`() {
        assertEquals(
            null,
            BundledTrackRegistry.resolveRawResId("missing", "also_missing"),
        )
    }

    @Test
    fun `defaults include six ambience and three music tracks`() {
        assertEquals(6, BundledTrackRegistry.defaults.count { it.kind == "ambience" })
        assertEquals(3, BundledTrackRegistry.defaults.count { it.kind == "music" })
    }
}
