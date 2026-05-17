package com.endiar.kuropomodoro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.MockBadge
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.components.StatusPill
import com.endiar.kuropomodoro.ui.components.StatusPillKind
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
private fun tileFillFor(soundId: Int): Color {
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
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 8.dp.roundToPx() }
    val showStickyBar by remember { derivedStateOf { scrollState.value > headerHeightPx + topPaddingPx } }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Sounds",
                color = tokens.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ambient sounds for your focus sessions",
                color = tokens.textSecondary,
                fontSize = 13.sp
            )
        }

        sounds.forEach { sound ->
            val isSelected = sound.id == selectedSoundId
            val isPlaying = sound.id == playingSoundId
            val pillKind = when {
                isPlaying    -> StatusPillKind.Playing
                isSelected   -> StatusPillKind.Selected
                sound.locked -> StatusPillKind.Locked
                else         -> StatusPillKind.Unlocked
            }
            val pillLabel = when {
                isPlaying    -> "Playing"
                isSelected   -> "Selected"
                sound.locked -> "Premium"
                else         -> "Tap to play"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isPlaying) tokens.accentSoft else Color.Transparent)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = when {
                                sound.locked -> "Unlock ${sound.name}"
                                isPlaying    -> "Stop ${sound.name}"
                                else         -> "Play ${sound.name}"
                            }
                        ) {
                            if (isPlaying) onToggleSound(sound) else onSoundSelected(sound)
                        },
                    active = isSelected,
                    glassTreatment = glassTreatment,
                    overBackdrop = overBackdrop,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tileFillFor(sound.id)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sound.symbol.take(2),
                                    fontSize = 22.sp
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sound.name,
                                        color = tokens.textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (sound.locked) MockBadge()
                                }
                                Text(
                                    text = sound.description,
                                    color = tokens.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        StatusPill(
                            label = pillLabel,
                            kind = pillKind,
                            glassTreatment = glassTreatment,
                            overBackdrop = overBackdrop,
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showStickyBar,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it },
    ) {
        ScreenStickyBar(title = "Sounds")
    }
    } // Box
}
