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
    fun `resolveRaw maps bundled lofi to R raw id`() {
        assertEquals(R.raw.l_lofi_desk_lamp_rain, BundledTrackRegistry.rawResId("l_lofi_desk_lamp_rain"))
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
    fun `bundled raw resources use kind prefix`() {
        BundledTrackRegistry.defaults.forEach { def ->
            when (def.kind) {
                "ambience" -> assertTrue(
                    "${def.rawResource} should start with l_amb_",
                    def.rawResource.startsWith("l_amb_"),
                )
                "music" -> assertTrue(
                    "${def.rawResource} should start with l_lofi_",
                    def.rawResource.startsWith("l_lofi_"),
                )
            }
        }
    }

    @Test
    fun `resolveRawResId resolves legacy catalog raw names`() {
        assertEquals(
            R.raw.l_lofi_desk_lamp_rain,
            BundledTrackRegistry.resolveRawResId("lofi_bloom_yasumu", "l_lofi_desk_lamp_rain"),
        )
        assertEquals(
            R.raw.l_amb_fireplace,
            BundledTrackRegistry.resolveRawResId("ambience_fireplace", "l_amb_fireplace"),
        )
    }

    @Test
    fun `defaults include six ambience and six music tracks`() {
        assertEquals(6, BundledTrackRegistry.defaults.count { it.kind == "ambience" })
        assertEquals(6, BundledTrackRegistry.defaults.count { it.kind == "music" })
    }
}
