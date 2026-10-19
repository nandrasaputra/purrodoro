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
 *
 * Bundled `res/raw` stems are ambience only: `l_amb_*`.
 */
object BundledTrackRegistry {

    val defaults: List<BundledTrackDefault> = listOf(
        BundledTrackDefault("ambience", 1, "l_amb_rain", R.string.sound_rain_name, R.string.sound_rain_desc, "Rain", false, 10),
        BundledTrackDefault("ambience", 2, "l_amb_cafe", R.string.sound_cafe_name, R.string.sound_cafe_desc, "Cafe", false, 20),
        BundledTrackDefault("ambience", 3, "l_amb_fireplace", R.string.sound_fire_name, R.string.sound_fire_desc, "Fire", false, 30),
        BundledTrackDefault("ambience", 4, "l_amb_forest", R.string.sound_forest_name, R.string.sound_forest_desc, "Forest", true, 40),
        BundledTrackDefault("ambience", 5, "l_amb_sea_wave", R.string.sound_sea_wave_name, R.string.sound_sea_wave_desc, "Wave", false, 50),
        BundledTrackDefault("ambience", 6, "l_amb_winter_wind", R.string.sound_winter_wind_name, R.string.sound_winter_wind_desc, "Wind", true, 60),
    )

    private val rawByName: Map<String, Int> = mapOf(
        "l_amb_rain" to R.raw.l_amb_rain,
        "l_amb_cafe" to R.raw.l_amb_cafe,
        "l_amb_fireplace" to R.raw.l_amb_fireplace,
        "l_amb_forest" to R.raw.l_amb_forest,
        "l_amb_sea_wave" to R.raw.l_amb_sea_wave,
        "l_amb_winter_wind" to R.raw.l_amb_winter_wind,
    )

    /** Stale `soundscape_bundled_catalog.raw_resource` values from earlier APK seeds. */
    private val legacyRawAliases: Map<String, Int> = mapOf(
        // ambience (original seed)
        "ambience_rain" to R.raw.l_amb_rain,
        "ambience_cafe" to R.raw.l_amb_cafe,
        "ambience_fireplace" to R.raw.l_amb_fireplace,
        "ambience_forest" to R.raw.l_amb_forest,
        "ambience_sea_wave" to R.raw.l_amb_sea_wave,
        "ambience_winter_wind" to R.raw.l_amb_winter_wind,
        // ambience (intermediate l_* rename)
        "l_rain" to R.raw.l_amb_rain,
        "l_cafe" to R.raw.l_amb_cafe,
        "l_fireplace" to R.raw.l_amb_fireplace,
        "l_forest" to R.raw.l_amb_forest,
        "l_sea_wave" to R.raw.l_amb_sea_wave,
        "l_winter_wind" to R.raw.l_amb_winter_wind,
    )

    /** Lookup a bundled `res/raw` id, or null when [rawResource] is unknown. */
    fun rawResIdOrNull(rawResource: String): Int? {
        val key = rawResource.trim()
        return rawByName[key] ?: legacyRawAliases[key]
    }

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
