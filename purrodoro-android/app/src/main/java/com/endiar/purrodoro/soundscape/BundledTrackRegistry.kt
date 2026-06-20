package com.endiar.purrodoro.soundscape

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.endiar.purrodoro.R

/**
 * One APK-bundled soundscape track's static defaults: its `res/raw` audio, the
 * `strings.xml` display keys, and the fallback premium/order used when the
 * remote `soundscape_bundled_catalog` row is missing.
 */
data class BundledTrackDefault(
    val kind: String,
    val localId: Int,
    val rawResource: String,
    @StringRes val nameRes: Int,
    @StringRes val subtitleRes: Int,
    val symbol: String,
    val defaultPremium: Boolean,
    val sortOrder: Int,
)

/**
 * The source of *existence* for bundled tracks (audio ships in the APK). The
 * Supabase `soundscape_bundled_catalog` table only *overrides* visibility,
 * premium, order, and display — see [SoundscapeCatalogBuilder].
 */
object BundledTrackRegistry {

    val defaults: List<BundledTrackDefault> = listOf(
        BundledTrackDefault("ambience", 1, "ambience_rain", R.string.sound_rain_name, R.string.sound_rain_desc, "Rain", false, 10),
        BundledTrackDefault("ambience", 2, "ambience_cafe", R.string.sound_cafe_name, R.string.sound_cafe_desc, "Cafe", false, 20),
        BundledTrackDefault("ambience", 3, "ambience_fireplace", R.string.sound_fire_name, R.string.sound_fire_desc, "Fire", true, 30),
        BundledTrackDefault("ambience", 4, "ambience_forest", R.string.sound_forest_name, R.string.sound_forest_desc, "Forest", true, 40),
        BundledTrackDefault("ambience", 5, "ambience_sea_wave", R.string.sound_sea_wave_name, R.string.sound_sea_wave_desc, "Wave", true, 50),
        BundledTrackDefault("ambience", 6, "ambience_winter_wind", R.string.sound_winter_wind_name, R.string.sound_winter_wind_desc, "Wind", true, 60),
        BundledTrackDefault("music", 1, "lofi_bloom_yasumu", R.string.music_bloom, R.string.music_bloom_artist, "", false, 10),
        BundledTrackDefault("music", 2, "lofi_chill_fm_dope_cat", R.string.music_chill_fm, R.string.music_chill_fm_artist, "", false, 20),
        BundledTrackDefault("music", 3, "lofi_midnight_whisper_matrika", R.string.music_midnight_whisper, R.string.music_midnight_whisper_artist, "", true, 30),
    )

    private val rawByName: Map<String, Int> = mapOf(
        "ambience_rain" to R.raw.ambience_rain,
        "ambience_cafe" to R.raw.ambience_cafe,
        "ambience_fireplace" to R.raw.ambience_fireplace,
        "ambience_forest" to R.raw.ambience_forest,
        "ambience_sea_wave" to R.raw.ambience_sea_wave,
        "ambience_winter_wind" to R.raw.ambience_winter_wind,
        "lofi_bloom_yasumu" to R.raw.lofi_bloom_yasumu,
        "lofi_chill_fm_dope_cat" to R.raw.lofi_chill_fm_dope_cat,
        "lofi_midnight_whisper_matrika" to R.raw.lofi_midnight_whisper_matrika,
    )

    /** Lookup a bundled `res/raw` id, or null when [rawResource] is unknown. */
    fun rawResIdOrNull(rawResource: String): Int? =
        rawByName[rawResource.trim()]

    /**
     * Resolve audio for a bundled track. Tries the catalog/server [requested] name
     * first (trimmed), then the APK registry [fallback]. Handles stale catalog rows
     * that reference renamed or not-yet-shipped raw files without crashing.
     */
    fun resolveRawResId(requested: String?, fallback: String): Int? {
        val override = requested?.trim()?.takeIf { it.isNotEmpty() }
        return override?.let { rawResIdOrNull(it) } ?: rawResIdOrNull(fallback)
    }

    @RawRes
    fun rawResId(rawResource: String): Int =
        rawResIdOrNull(rawResource)
            ?: error("Unknown bundled raw resource: $rawResource")
}
