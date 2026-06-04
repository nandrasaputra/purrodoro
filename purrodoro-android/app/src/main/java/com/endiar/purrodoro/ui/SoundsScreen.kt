package com.endiar.purrodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.GlassTreatment
import com.endiar.purrodoro.model.SoundOption
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroBadge
import com.endiar.purrodoro.ui.designsystem.KuroPill
import com.endiar.purrodoro.ui.designsystem.KuroPillKind
import com.endiar.purrodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.TileCafeDark
import com.endiar.purrodoro.ui.theme.TileCafeLight
import com.endiar.purrodoro.ui.theme.TileFireDark
import com.endiar.purrodoro.ui.theme.TileFireLight
import com.endiar.purrodoro.ui.theme.TileForestDark
import com.endiar.purrodoro.ui.theme.TileForestLight
import com.endiar.purrodoro.ui.theme.TileKeysDark
import com.endiar.purrodoro.ui.theme.TileKeysLight
import com.endiar.purrodoro.ui.theme.TileRainDark
import com.endiar.purrodoro.ui.theme.TileRainLight
import com.endiar.purrodoro.ui.theme.kuroTokens
import com.endiar.purrodoro.ui.theme.luminance

@Composable
private fun tileFillFor(soundId: Int): androidx.compose.ui.graphics.Color {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return when (soundId) {
        1 -> if (isLight) TileRainLight else TileRainDark
        2 -> if (isLight) TileCafeLight else TileCafeDark
        3 -> if (isLight) TileForestLight else TileForestDark
        4 -> if (isLight) TileFireLight else TileFireDark
        5 -> if (isLight) TileKeysLight else TileKeysDark
        else -> MaterialTheme.colorScheme.kuroTokens.cardFill
    }
}

// NOTE: Ambient focus sounds are not active. This screen is UI-only — selecting or
// toggling a sound just updates state; no real audio is played (no MediaPlayer/
// SoundPool/ExoPlayer). Looping audio + background playback still needs wiring.
@Composable
fun SoundsScreen(
    sounds: List<SoundOption>,
    selectedSoundId: Int,
    playingSoundId: Int?,
    onSoundSelected: (SoundOption) -> Unit,
    onToggleSound: (SoundOption) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    @Suppress("UNUSED_PARAMETER") overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    KuroScreenScaffold(
        stickyTitle = stringResource(R.string.sounds_title),
        modifier = modifier,
        contentPadding = contentPadding,
    ) { scrollState, headerMeasureModifier ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = KuroSpacing.lg)
                .padding(top = KuroSpacing.lg, bottom = KuroSpacing.dockClearance)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Column(
                modifier = headerMeasureModifier,
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
            ) {
                Text(text = stringResource(R.string.sounds_title), color = tokens.textPrimary, style = KuroType.headingDisplay)
                Text(
                    text = stringResource(R.string.sounds_subtitle),
                    color = tokens.textSecondary,
                    style = KuroType.bodySecondary,
                )
            }

            sounds.forEach { sound ->
                val isSelected = sound.id == selectedSoundId
                val isPlaying = sound.id == playingSoundId
                val pillKind = when {
                    isPlaying -> KuroPillKind.Playing
                    isSelected -> KuroPillKind.Selected
                    sound.locked -> KuroPillKind.Locked
                    else -> KuroPillKind.Default
                }
                val pillLabel = when {
                    isPlaying -> stringResource(R.string.sound_pill_playing)
                    isSelected -> stringResource(R.string.sound_pill_selected)
                    sound.locked -> stringResource(R.string.sound_pill_premium)
                    else -> stringResource(R.string.sound_pill_tap_to_play)
                }

                KuroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = when {
                                sound.locked -> stringResource(R.string.sound_unlock_cd, sound.name)
                                isPlaying -> stringResource(R.string.sound_stop_cd, sound.name)
                                else -> stringResource(R.string.sound_play_cd, sound.name)
                            },
                        ) {
                            if (isPlaying) onToggleSound(sound) else onSoundSelected(sound)
                        },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(KuroSpacing.xxxl + KuroSpacing.xs)
                                    .clip(CircleShape)
                                    .background(tileFillFor(sound.id)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = sound.symbol.take(2), style = KuroType.headingCard)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(text = sound.name, color = tokens.textPrimary, style = KuroType.headingCard)
                                    if (sound.locked) {
                                        KuroBadge(
                                            label = stringResource(R.string.sound_pill_premium),
                                            fill = tokens.lockedFill,
                                            textColor = tokens.lockedText,
                                        )
                                    }
                                }
                                Text(text = sound.description, color = tokens.textSecondary, style = KuroType.bodySecondary)
                            }
                        }
                        KuroPill(label = pillLabel, kind = pillKind)
                    }
                }
            }
        }
    }
}
