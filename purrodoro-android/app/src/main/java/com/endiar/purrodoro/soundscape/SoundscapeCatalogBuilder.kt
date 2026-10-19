package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption

/**
 * Builds the Sounds sheet from the APK registry only. Cloud catalog rows and
 * downloaded files are not part of the list.
 */
object SoundscapeCatalogBuilder {

    const val DEFAULT_AMBIENCE_ID = 1

    fun buildAmbienceOptions(
        nameFor: (BundledTrackDefault) -> String,
        subtitleFor: (BundledTrackDefault) -> String,
        bundledDefaults: List<BundledTrackDefault> = BundledTrackRegistry.defaults,
    ): List<SoundOption> =
        bundledDefaults
            .filter { it.kind == "ambience" }
            .sortedBy { it.sortOrder }
            .mapNotNull { def ->
                val resId = BundledTrackRegistry.resolveRawResId(null, def.rawResource)
                    ?: return@mapNotNull null
                SoundOption(
                    id = def.localId,
                    name = nameFor(def),
                    description = subtitleFor(def),
                    locked = def.defaultPremium,
                    symbol = def.symbol,
                    audio = AudioSource.Bundled(resId),
                    remote = null,
                )
            }

    fun buildMusicOptions(
        nameFor: (BundledTrackDefault) -> String,
        subtitleFor: (BundledTrackDefault) -> String,
        bundledDefaults: List<BundledTrackDefault> = BundledTrackRegistry.defaults,
    ): List<MusicOption> =
        bundledDefaults
            .filter { it.kind == "music" }
            .sortedBy { it.sortOrder }
            .map { def ->
                val resId = BundledTrackRegistry.resolveRawResId(null, def.rawResource)
                MusicOption(
                    id = def.localId,
                    name = nameFor(def),
                    artist = subtitleFor(def),
                    audio = resId?.let { AudioSource.Bundled(it) },
                    locked = def.defaultPremium,
                    remote = null,
                )
            }

    /** Unknown or locked-for-this-user ids fall back to the first free ambience. */
    fun resolveAmbienceId(
        id: Int?,
        isPremium: Boolean,
        options: List<SoundOption>,
    ): Int {
        val match = options.firstOrNull { it.id == id && (!it.locked || isPremium) }
        if (match != null) return match.id
        return options.firstOrNull { !it.locked }?.id ?: DEFAULT_AMBIENCE_ID
    }

    /** No music catalog means no mix. An unknown id falls back to the first free track. */
    fun resolveMusicId(
        id: Int?,
        isPremium: Boolean,
        options: List<MusicOption>,
    ): Int? {
        if (id == null) return null
        val match = options.firstOrNull { it.id == id && (!it.locked || isPremium) }
        if (match != null) return match.id
        return options.firstOrNull { !it.locked }?.id
    }
}
