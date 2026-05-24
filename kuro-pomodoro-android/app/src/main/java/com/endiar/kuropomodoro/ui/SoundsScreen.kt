package com.endiar.kuropomodoro.ui

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.designsystem.KuroBadge
import com.endiar.kuropomodoro.ui.designsystem.KuroPill
import com.endiar.kuropomodoro.ui.designsystem.KuroPillKind
import com.endiar.kuropomodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.TileCafeDark
import com.endiar.kuropomodoro.ui.theme.TileCafeLight
import com.endiar.kuropomodoro.ui.theme.TileFireDark
import com.endiar.kuropomodoro.ui.theme.TileFireLight
import com.endiar.kuropomodoro.ui.theme.TileForestDark
import com.endiar.kuropomodoro.ui.theme.TileForestLight
import com.endiar.kuropomodoro.ui.theme.TileKeysDark
import com.endiar.kuropomodoro.ui.theme.TileKeysLight
import com.endiar.kuropomodoro.ui.theme.TileRainDark
import com.endiar.kuropomodoro.ui.theme.TileRainLight
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import com.endiar.kuropomodoro.ui.theme.luminance

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
        stickyTitle = "Sounds",
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
                Text(text = "Sounds", color = tokens.textPrimary, style = KuroType.headingDisplay)
                Text(
                    text = "Ambient sounds for your focus sessions",
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
                    isPlaying -> "Playing"
                    isSelected -> "Selected"
                    sound.locked -> "Premium"
                    else -> "Tap to play"
                }

                KuroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = when {
                                sound.locked -> "Unlock ${sound.name}"
                                isPlaying -> "Stop ${sound.name}"
                                else -> "Play ${sound.name}"
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
                                            label = "Premium",
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
