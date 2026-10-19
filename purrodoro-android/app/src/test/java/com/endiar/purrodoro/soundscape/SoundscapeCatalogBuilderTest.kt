package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.R
import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundscapeCatalogBuilderTest {

    private val rain = BundledTrackDefault(
        kind = "ambience", localId = 1, rawResource = "l_amb_rain",
        nameRes = R.string.sound_rain_name, subtitleRes = R.string.sound_rain_desc,
        symbol = "Rain", defaultPremium = false, sortOrder = 10,
    )
    private val forest = BundledTrackDefault(
        kind = "ambience", localId = 4, rawResource = "l_amb_forest",
        nameRes = R.string.sound_forest_name, subtitleRes = R.string.sound_forest_desc,
        symbol = "Forest", defaultPremium = true, sortOrder = 40,
    )

    @Test
    fun `bundled ambience stays visible with registry premium locks`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            nameFor = { it.symbol },
            subtitleFor = { "sub" },
            bundledDefaults = listOf(forest, rain),
        )
        assertEquals(listOf(1, 4), options.map { it.id })
        assertEquals(AudioSource.Bundled(R.raw.l_amb_rain), options[0].audio)
        assertNull(options[0].remote)
        assertTrue(options[1].locked)
    }

    @Test
    fun `registry has six ambience tracks and no music`() {
        val ambience = SoundscapeCatalogBuilder.buildAmbienceOptions(
            nameFor = { it.rawResource },
            subtitleFor = { "" },
        )
        val music = SoundscapeCatalogBuilder.buildMusicOptions(
            nameFor = { it.rawResource },
            subtitleFor = { "" },
        )
        assertEquals(6, ambience.size)
        assertEquals(0, music.size)
        assertEquals(
            listOf("l_amb_forest", "l_amb_winter_wind"),
            ambience.filter { it.locked }.map { it.name },
        )
    }

    @Test
    fun `unknown ambience id falls back to rain`() {
        val options = listOf(sound(1, locked = false), sound(4, locked = true))
        assertEquals(1, SoundscapeCatalogBuilder.resolveAmbienceId(99, isPremium = false, options))
        assertEquals(1, SoundscapeCatalogBuilder.resolveAmbienceId(4, isPremium = false, options))
        assertEquals(4, SoundscapeCatalogBuilder.resolveAmbienceId(4, isPremium = true, options))
    }

    @Test
    fun `unknown music id falls back to the first free track`() {
        val options = listOf(music(1, locked = false), music(5, locked = true))
        assertNull(SoundscapeCatalogBuilder.resolveMusicId(null, isPremium = false, options))
        assertEquals(1, SoundscapeCatalogBuilder.resolveMusicId(99, isPremium = false, options))
        assertEquals(5, SoundscapeCatalogBuilder.resolveMusicId(5, isPremium = true, options))
    }

    @Test
    fun `saved music id clears when the catalog has no tracks`() {
        assertNull(SoundscapeCatalogBuilder.resolveMusicId(1, isPremium = true, emptyList()))
    }

    private fun sound(id: Int, locked: Boolean) = SoundOption(
        id = id, name = "n", description = "d", locked = locked, symbol = "",
        audio = AudioSource.Bundled(R.raw.l_amb_rain), remote = null,
    )

    private fun music(id: Int, locked: Boolean) = MusicOption(
        id = id, name = "n", artist = "a", locked = locked,
        audio = AudioSource.Bundled(R.raw.l_amb_rain), remote = null,
    )
}
