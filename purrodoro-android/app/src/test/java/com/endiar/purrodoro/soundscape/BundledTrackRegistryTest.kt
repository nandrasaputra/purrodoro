package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledTrackRegistryTest {

    @Test
    fun `resolveRaw maps bundled ambience to R raw id`() {
        assertEquals(R.raw.l_amb_rain, BundledTrackRegistry.rawResId("l_amb_rain"))
    }

    @Test
    fun `resolveRawResId falls back when catalog name is unknown`() {
        assertEquals(
            R.raw.l_amb_rain,
            BundledTrackRegistry.resolveRawResId("stale_server_name", "l_amb_rain"),
        )
    }

    @Test
    fun `resolveRawResId trims whitespace on catalog name`() {
        assertEquals(
            R.raw.l_amb_rain,
            BundledTrackRegistry.resolveRawResId("  l_amb_rain  ", "l_amb_cafe"),
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
    fun `bundled raw resources are ambience`() {
        BundledTrackRegistry.defaults.forEach { def ->
            assertEquals("ambience", def.kind)
            assertTrue(
                "${def.rawResource} should start with l_amb_",
                def.rawResource.startsWith("l_amb_"),
            )
        }
    }

    @Test
    fun `resolveRawResId resolves legacy catalog raw names`() {
        assertEquals(
            R.raw.l_amb_fireplace,
            BundledTrackRegistry.resolveRawResId("ambience_fireplace", "l_amb_fireplace"),
        )
    }

    @Test
    fun `defaults include six ambience tracks and no music`() {
        assertEquals(6, BundledTrackRegistry.defaults.count { it.kind == "ambience" })
        assertEquals(0, BundledTrackRegistry.defaults.count { it.kind == "music" })
    }
}
