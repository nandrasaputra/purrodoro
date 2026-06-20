package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.data.local.SoundscapeBundledEntity
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption

/**
 * Pure merge of the bundled (APK) registry + cached overrides + the remote
 * cloud catalog into the option lists the Sounds sheet renders.
 *
 * Ownership of "existence" is the registry: we always iterate [bundledDefaults]
 * and left-join the cached DB row by (kind, localId). A missing row keeps the
 * default and stays visible (a freshly-bundled track not yet seeded must never
 * silently disappear); an inactive row (`isActive == false`) hides its default.
 * Non-blank `name` / `artist` / `symbol` on the row override the string-res
 * defaults. Bundled tracks always render before remote tracks.
 */
object SoundscapeCatalogBuilder {

    fun buildAmbienceOptions(
        bundledRows: List<SoundscapeBundledEntity>,
        remoteRows: List<SoundscapeTrackEntity>,
        downloadStates: Map<Int, DownloadState>,
        localPaths: Map<Int, String>,
        nameFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        subtitleFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        bundledDefaults: List<BundledTrackDefault> = BundledTrackRegistry.defaults,
    ): List<SoundOption> {
        val bundled = mergeBundled("ambience", bundledRows, bundledDefaults)
            .mapNotNull { (def, row) ->
                val resId = rawResIdFor(row, def) ?: return@mapNotNull null
                SoundOption(
                    id = def.localId,
                    name = row?.name?.takeIf { it.isNotBlank() } ?: nameFor(row, def),
                    description = row?.artist?.takeIf { it.isNotBlank() } ?: subtitleFor(row, def),
                    locked = row?.isPremium ?: def.defaultPremium,
                    symbol = row?.symbol?.takeIf { it.isNotBlank() } ?: def.symbol,
                    audio = AudioSource.Bundled(resId),
                    remote = null,
                )
            }
        val remote = remoteRows
            .filter { it.kind == "ambience" }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { row ->
                row.toSoundOption(
                    downloadState = stateFor(row, downloadStates),
                    localPath = localPathFor(row, downloadStates, localPaths),
                )
            }
        return bundled + remote
    }

    fun buildMusicOptions(
        bundledRows: List<SoundscapeBundledEntity>,
        remoteRows: List<SoundscapeTrackEntity>,
        downloadStates: Map<Int, DownloadState>,
        localPaths: Map<Int, String>,
        nameFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        subtitleFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        bundledDefaults: List<BundledTrackDefault> = BundledTrackRegistry.defaults,
    ): List<MusicOption> {
        val bundled = mergeBundled("music", bundledRows, bundledDefaults)
            .mapNotNull { (def, row) ->
                val resId = rawResIdFor(row, def) ?: return@mapNotNull null
                MusicOption(
                    id = def.localId,
                    name = row?.name?.takeIf { it.isNotBlank() } ?: nameFor(row, def),
                    artist = row?.artist?.takeIf { it.isNotBlank() } ?: subtitleFor(row, def),
                    audio = AudioSource.Bundled(resId),
                    locked = row?.isPremium ?: def.defaultPremium,
                    remote = null,
                )
            }
        val remote = remoteRows
            .filter { it.kind == "music" }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { row ->
                row.toMusicOption(
                    downloadState = stateFor(row, downloadStates),
                    localPath = localPathFor(row, downloadStates, localPaths),
                )
            }
        return bundled + remote
    }

    /**
     * Left-join the registry defaults (existence) with cached rows (overrides).
     * Returns visible `(def, row?)` pairs sorted by effective sort order:
     * - row absent  → visible default
     * - row present and inactive → dropped (hidden)
     * - row present and active   → kept, overrides applied by the caller
     */
    private fun mergeBundled(
        kind: String,
        rows: List<SoundscapeBundledEntity>,
        defaults: List<BundledTrackDefault>,
    ): List<Pair<BundledTrackDefault, SoundscapeBundledEntity?>> {
        val byLocalId = rows.filter { it.kind == kind }.associateBy { it.localId }
        return defaults
            .filter { it.kind == kind }
            .mapNotNull { def ->
                val row = byLocalId[def.localId]
                if (row != null && !row.isActive) return@mapNotNull null
                def to row
            }
            .sortedBy { (def, row) -> row?.sortOrder ?: def.sortOrder }
    }

    private fun rawResIdFor(row: SoundscapeBundledEntity?, def: BundledTrackDefault): Int? =
        BundledTrackRegistry.resolveRawResId(row?.rawResource, def.rawResource)

    private fun stateFor(row: SoundscapeTrackEntity, states: Map<Int, DownloadState>): DownloadState =
        states[row.id] ?: DownloadState.NotDownloaded

    private fun localPathFor(
        row: SoundscapeTrackEntity,
        states: Map<Int, DownloadState>,
        localPaths: Map<Int, String>,
    ): String? = localPaths[row.id]?.takeIf { stateFor(row, states) == DownloadState.Downloaded }
}
