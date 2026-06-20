package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.R
import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.data.local.SoundscapeBundledEntity
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundscapeCatalogBuilderTest {

    private val rainDefault = BundledTrackDefault(
        kind = "ambience", localId = 1, rawResource = "ambience_rain",
        nameRes = R.string.sound_rain_name, subtitleRes = R.string.sound_rain_desc,
        symbol = "Rain", defaultPremium = false, sortOrder = 10,
    )
    private val cafeDefault = BundledTrackDefault(
        kind = "ambience", localId = 2, rawResource = "ambience_cafe",
        nameRes = R.string.sound_cafe_name, subtitleRes = R.string.sound_cafe_desc,
        symbol = "Cafe", defaultPremium = false, sortOrder = 20,
    )

    private fun bundled(
        localId: Int,
        premium: Boolean = false,
        active: Boolean = true,
        sort: Int = 0,
    ) = SoundscapeBundledEntity(
        kind = "ambience", localId = localId, name = "", artist = "", symbol = "",
        rawResource = "ambience_rain", isPremium = premium, sortOrder = sort, isActive = active,
    )

    private fun remote(id: Int) = SoundscapeTrackEntity(
        id = id, kind = "music", name = "Cloud Track", artist = "Artist", symbol = "",
        filePath = "lofi/x.mp3", fileSizeBytes = 2_000_000, isPremium = false,
        sortOrder = 10, updatedAtMillis = 1_000,
    )

    @Test
    fun `missing rows fall back to defaults and stay visible`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = emptyList(),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault, cafeDefault),
        )
        assertEquals(listOf(1, 2), options.map { it.id })
        assertEquals(AudioSource.Bundled(R.raw.ambience_rain), options[0].audio)
        assertNull(options[0].remote)
    }

    @Test
    fun `inactive row hides its default`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(bundled(localId = 1, active = false)),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault, cafeDefault),
        )
        assertEquals(listOf(2), options.map { it.id }) // rain hidden, cafe remains
    }

    @Test
    fun `row premium override wins over default`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(bundled(localId = 1, premium = true)),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault),
        )
        assertTrue(options.single().locked)
    }

    @Test
    fun `row sort_order override reorders bundled tracks`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(
                bundled(localId = 1, sort = 99), // rain pushed last
                bundled(localId = 2, sort = 1),  // cafe pulled first
            ),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault, cafeDefault),
        )
        assertEquals(listOf(2, 1), options.map { it.id })
    }

    @Test
    fun `non-blank name and description overrides win`() {
        val row = bundled(localId = 1)
            .copy(name = "Custom Rain", artist = "Soft drizzle", symbol = "Storm")
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(row),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "default name" },
            subtitleFor = { _, _ -> "default sub" },
            bundledDefaults = listOf(rainDefault),
        )
        assertEquals("Custom Rain", options.single().name)
        assertEquals("Soft drizzle", options.single().description)
        assertEquals("Storm", options.single().symbol)
    }

    @Test
    fun `remote music not downloaded has null audio and NotDownloaded state`() {
        val options = SoundscapeCatalogBuilder.buildMusicOptions(
            bundledRows = emptyList(),
            remoteRows = listOf(remote(100)),
            downloadStates = mapOf(100 to DownloadState.NotDownloaded),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "unused" },
            subtitleFor = { _, _ -> "unused" },
            bundledDefaults = emptyList(),
        )
        assertNull(options.single().audio)
        assertEquals(DownloadState.NotDownloaded, options.single().remote?.downloadState)
    }

    @Test
    fun `unknown catalog rawResource falls back to registry default`() {
        val row = SoundscapeBundledEntity(
            kind = "ambience",
            localId = 1,
            name = "",
            artist = "",
            symbol = "",
            rawResource = "stale_server_name",
            isPremium = false,
            sortOrder = 10,
            isActive = true,
        )
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(row),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault),
        )
        assertEquals(AudioSource.Bundled(R.raw.ambience_rain), options.single().audio)
    }

    @Test
    fun `bundled track omitted when no raw resource resolves`() {
        val unmappedDefault = rainDefault.copy(rawResource = "does_not_exist")
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = emptyList(),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(unmappedDefault, cafeDefault),
        )
        assertEquals(listOf(2), options.map { it.id })
    }

    @Test
    fun `bundled tracks render before remote tracks`() {
        val options = SoundscapeCatalogBuilder.buildMusicOptions(
            bundledRows = emptyList(),
            remoteRows = listOf(remote(100)),
            downloadStates = mapOf(100 to DownloadState.NotDownloaded),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "Bloom" },
            subtitleFor = { _, _ -> "Yasumu" },
            bundledDefaults = listOf(
                BundledTrackDefault(
                    "music", 1, "lofi_bloom_yasumu",
                    R.string.music_bloom, R.string.music_bloom_artist, "", false, 10,
                ),
            ),
        )
        assertEquals(listOf(1, 100), options.map { it.id })
    }
}
