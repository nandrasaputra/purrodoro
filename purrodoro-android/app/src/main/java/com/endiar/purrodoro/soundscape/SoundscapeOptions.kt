package com.endiar.purrodoro.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption

/** Bundled display strings resolve via [stringResource] so locale changes recompose. */
@Composable
fun buildLocalizedAmbienceOptions(): List<SoundOption> {
    val names = BundledTrackRegistry.defaults
        .filter { it.kind == "ambience" }
        .associate { it.localId to stringResource(it.nameRes) }
    val subtitles = BundledTrackRegistry.defaults
        .filter { it.kind == "ambience" }
        .associate { it.localId to stringResource(it.subtitleRes) }
    return SoundscapeCatalogBuilder.buildAmbienceOptions(
        nameFor = { def -> names.getValue(def.localId) },
        subtitleFor = { def -> subtitles.getValue(def.localId) },
    )
}

@Composable
fun buildLocalizedMusicOptions(): List<MusicOption> {
    val names = BundledTrackRegistry.defaults
        .filter { it.kind == "music" }
        .associate { it.localId to stringResource(it.nameRes) }
    val subtitles = BundledTrackRegistry.defaults
        .filter { it.kind == "music" }
        .associate { it.localId to stringResource(it.subtitleRes) }
    return SoundscapeCatalogBuilder.buildMusicOptions(
        nameFor = { def -> names.getValue(def.localId) },
        subtitleFor = { def -> subtitles.getValue(def.localId) },
    )
}
