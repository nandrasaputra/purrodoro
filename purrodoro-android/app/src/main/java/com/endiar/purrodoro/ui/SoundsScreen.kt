package com.endiar.purrodoro.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.CloudDownload
import com.composables.icons.lucide.CloudRain
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.DownloadState
import com.endiar.purrodoro.model.GlassTreatment
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption
import com.endiar.purrodoro.ui.designsystem.KuroPremiumBadge
import com.endiar.purrodoro.ui.designsystem.KuroPill
import com.endiar.purrodoro.ui.designsystem.KuroPillKind
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.CardWhite
import com.endiar.purrodoro.ui.theme.Cocoa
import com.endiar.purrodoro.ui.theme.Coral
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.Peach
import com.endiar.purrodoro.ui.theme.kuroTokens
import com.endiar.purrodoro.ui.theme.luminance
import kotlin.math.roundToInt

private const val VOLUME_SNAP_STEPS = 4
private const val SOUNDSCAPE_VINYL_SPIN_MS = 4_000
private const val SOUNDSCAPE_EQ_CYCLE_MS = 520
/** Per-bar height at rest (fraction of [maxBarHeight]). */
private val SOUNDSCAPE_EQ_BAR_IDLE = listOf(0.42f, 0.62f, 0.38f, 0.52f)
/** Per-bar animated floor / peak — peak 1f means the bar touches full capsule height. */
private val SOUNDSCAPE_EQ_BAR_FLOOR = listOf(0.18f, 0.22f, 0.14f, 0.20f)
private val SOUNDSCAPE_EQ_BAR_PEAK = listOf(0.58f, 1f, 0.48f, 0.76f)
private val SOUNDSCAPE_EQ_BAR_PHASE_MS = listOf(0, 90, 180, 130)

private fun soundscapeEqBarFraction(floor: Float, peak: Float, anim: Float): Float =
    floor + (peak - floor) * anim.coerceIn(0f, 1f)

@Composable
private fun rememberSoundscapeVinylSpinAngle(label: String, isSpinning: Boolean): Float {
    val spinTransition = rememberInfiniteTransition(label = label)
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SOUNDSCAPE_VINYL_SPIN_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "${label}Angle",
    )
    return if (isSpinning) spinAngle else 0f
}

private fun snapVolume(raw: Float): Float {
    val stepped = (raw * VOLUME_SNAP_STEPS).roundToInt().coerceIn(0, VOLUME_SNAP_STEPS)
    return stepped / VOLUME_SNAP_STEPS.toFloat()
}

// Ambience + lo-fi selection drives [SoundscapeLoopPlayer] (res/raw MP3 loops).

/** One-line Soundscape control for the active timer card and focus mode. */
@Composable
fun SoundscapeCompactBar(
    playingAmbienceName: String?,
    playingMusicName: String?,
    onOpenSoundscape: () -> Unit,
    onMuteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isActive = playingAmbienceName != null || playingMusicName != null
    val statusText = soundscapeStatusLine(playingAmbienceName, playingMusicName)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(if (isActive) tokens.activeCardFill else tokens.cardFill)
            .border(
                1.dp,
                if (isActive) tokens.accent.copy(alpha = 0.35f) else tokens.cardBorder.copy(alpha = 0.6f),
                RoundedCornerShape(KuroSpacing.radiusPill),
            )
            .clickable(role = Role.Button, onClick = onOpenSoundscape)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Icon(
            imageVector = if (isActive) Lucide.AudioLines else Lucide.Music,
            contentDescription = null,
            tint = if (isActive) tokens.accent else tokens.textSecondary,
            modifier = Modifier.size(KuroSpacing.lg),
        )
        Text(
            text = statusText,
            color = if (isActive) tokens.textPrimary else tokens.textSecondary,
            style = KuroType.labelStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.journeyMetricIconCircle)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.soundscape_home_mute_cd),
                        onClick = onMuteAll,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.VolumeX,
                    contentDescription = stringResource(R.string.soundscape_home_mute_cd),
                    tint = tokens.accent,
                    modifier = Modifier.size(KuroSpacing.md + KuroSpacing.xs),
                )
            }
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = tokens.textFaint,
            modifier = Modifier.size(KuroSpacing.md + KuroSpacing.xs),
        )
    }
}

@Composable
internal fun soundscapeDockStatusLine(
    playingAmbienceName: String?,
    playingMusicName: String?,
): String = soundscapeStatusLine(playingAmbienceName, playingMusicName)

@Composable
internal fun soundscapeSavedMixLabel(
    ambienceName: String?,
    musicName: String?,
): String {
    val sep = stringResource(R.string.soundscape_compact_separator)
    return when {
        ambienceName != null && musicName != null -> "$ambienceName$sep$musicName"
        ambienceName != null -> ambienceName
        musicName != null -> musicName
        else -> ""
    }
}

/** Compact transport row fused into the Focus-tab bottom dock. */
@Composable
fun SoundscapeTransportBar(
    isPlaying: Boolean,
    displayName: String,
    showFirstRunHint: Boolean,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val rowFill = when {
        isPlaying -> Peach.copy(alpha = 0.32f)
        else -> tokens.cardFill.copy(alpha = 0.45f)
    }
    val nameColor = when {
        isPlaying -> Cocoa
        showFirstRunHint -> tokens.textSecondary
        else -> tokens.textMuted
    }
    val expandLabel = stringResource(R.string.soundscape_mini_open_cd)

    // Outer Box: full-bleed background + touch consumer. Inner Row: content insets.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(KuroSpacing.soundscapeTransportBarHeight)
            .background(rowFill)
            .clickable(
                role = Role.Button,
                onClickLabel = expandLabel,
                onClick = onExpand,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = KuroSpacing.soundscapeTransportInsetH,
                    vertical = KuroSpacing.soundscapeTransportInsetV,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            SoundscapeEqualizerCapsule(
                isPlaying = isPlaying,
                modifier = Modifier.size(KuroSpacing.journeyMetricIconCircle + KuroSpacing.xs),
            )
            Text(
                text = displayName,
                color = nameColor,
                style = KuroType.labelStrong.copy(
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SoundscapeTransportControlButton(
                isPlaying = isPlaying,
                onClick = onPlayPause,
            )
            Box(
                modifier = Modifier
                    .size(KuroSpacing.journeyMetricIconCircle)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = expandLabel,
                        onClick = onExpand,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.ChevronUp,
                    contentDescription = expandLabel,
                    tint = if (isPlaying) Coral.copy(alpha = 0.85f) else tokens.textFaint,
                    modifier = Modifier.size(KuroSpacing.lg),
                )
            }
        }
    }
}

/** Option 3 — coral capsule with animated EQ bars (transport bar leading glyph). */
@Composable
private fun SoundscapeEqualizerCapsule(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val barWidth = 3.dp
    val barGap = 3.dp
    val barAreaHeight = 18.dp
    val maxBarHeight = 14.dp

    if (!isPlaying) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Peach.copy(alpha = 0.4f))
                .border(1.dp, Coral.copy(alpha = 0.16f), shape),
            contentAlignment = Alignment.Center,
        ) {
            SoundscapeEqualizerBars(
                fractions = SOUNDSCAPE_EQ_BAR_IDLE,
                barColor = Coral.copy(alpha = 0.5f),
                barWidth = barWidth,
                barGap = barGap,
                barAreaHeight = barAreaHeight,
                maxBarHeight = maxBarHeight,
            )
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "soundscapeEqCapsule")
    val bar0 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SOUNDSCAPE_EQ_CYCLE_MS, delayMillis = SOUNDSCAPE_EQ_BAR_PHASE_MS[0], easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eqBar0",
    )
    val bar1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SOUNDSCAPE_EQ_CYCLE_MS, delayMillis = SOUNDSCAPE_EQ_BAR_PHASE_MS[1], easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eqBar1",
    )
    val bar2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SOUNDSCAPE_EQ_CYCLE_MS, delayMillis = SOUNDSCAPE_EQ_BAR_PHASE_MS[2], easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eqBar2",
    )
    val bar3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SOUNDSCAPE_EQ_CYCLE_MS, delayMillis = SOUNDSCAPE_EQ_BAR_PHASE_MS[3], easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eqBar3",
    )
    val animatedFractions = listOf(
        soundscapeEqBarFraction(SOUNDSCAPE_EQ_BAR_FLOOR[0], SOUNDSCAPE_EQ_BAR_PEAK[0], bar0),
        soundscapeEqBarFraction(SOUNDSCAPE_EQ_BAR_FLOOR[1], SOUNDSCAPE_EQ_BAR_PEAK[1], bar1),
        soundscapeEqBarFraction(SOUNDSCAPE_EQ_BAR_FLOOR[2], SOUNDSCAPE_EQ_BAR_PEAK[2], bar2),
        soundscapeEqBarFraction(SOUNDSCAPE_EQ_BAR_FLOOR[3], SOUNDSCAPE_EQ_BAR_PEAK[3], bar3),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(Peach.copy(alpha = 0.52f))
            .border(1.dp, Coral.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center,
    ) {
        SoundscapeEqualizerBars(
            fractions = animatedFractions,
            barColor = Coral.copy(alpha = 0.82f),
            barWidth = barWidth,
            barGap = barGap,
            barAreaHeight = barAreaHeight,
            maxBarHeight = maxBarHeight,
        )
    }
}

@Composable
private fun SoundscapeEqualizerBars(
    fractions: List<Float>,
    barColor: Color,
    barWidth: androidx.compose.ui.unit.Dp,
    barGap: androidx.compose.ui.unit.Dp,
    barAreaHeight: androidx.compose.ui.unit.Dp,
    maxBarHeight: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier.height(barAreaHeight),
        horizontalArrangement = Arrangement.spacedBy(barGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        fractions.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxBarHeight * fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun SoundscapeTransportControlButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val controlLabel = stringResource(
        if (isPlaying) R.string.soundscape_transport_pause_cd else R.string.soundscape_transport_play_cd,
    )
    Box(
        modifier = Modifier
            .size(KuroSpacing.journeyMetricIconCircle)
            .clip(CircleShape)
            .background(Coral)
            .clickable(role = Role.Button, onClickLabel = controlLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
            contentDescription = controlLabel,
            tint = Color.White,
            modifier = Modifier.size(KuroSpacing.journeyMetricIconGlyph),
        )
    }
}

@Deprecated("Replaced by SoundscapeTransportBar")
@Composable
fun SoundscapeDockRow(
    playingAmbienceName: String?,
    playingMusicName: String?,
    onOpenSoundscape: () -> Unit,
    onMuteAll: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isActive = playingAmbienceName != null || playingMusicName != null
    val statusText = if (isActive) {
        soundscapeStatusLine(playingAmbienceName, playingMusicName)
    } else {
        stringResource(R.string.soundscape_home_hint)
    }
    val openLabel = stringResource(R.string.soundscape_mini_open_cd)
    val rowFill = when {
        isActive -> Peach.copy(alpha = 0.35f)
        else -> tokens.cardFill.copy(alpha = 0.55f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(KuroSpacing.soundscapeTransportBarHeight)
            .background(rowFill)
            .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpenSoundscape)
            .padding(horizontal = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.journeyMetricIconCircle)
                .clip(CircleShape)
                .background(
                    if (isActive) Peach.copy(alpha = 0.55f) else tokens.segmentedTrack.copy(alpha = 0.65f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isActive) Lucide.AudioLines else Lucide.Music,
                contentDescription = null,
                tint = if (isActive) Coral else tokens.textSecondary,
                modifier = Modifier.size(KuroSpacing.lg),
            )
        }
        Text(
            text = statusText,
            color = if (isActive) Cocoa else tokens.textSecondary,
            style = KuroType.labelStrong.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.journeyMetricIconCircle)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.soundscape_home_mute_cd),
                        onClick = onMuteAll,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.VolumeX,
                    contentDescription = stringResource(R.string.soundscape_home_mute_cd),
                    tint = Coral,
                    modifier = Modifier.size(KuroSpacing.lg),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(KuroSpacing.journeyMetricIconCircle)
                .clip(CircleShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.soundscape_dock_collapse_cd),
                    onClick = onCollapse,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = stringResource(R.string.soundscape_dock_collapse_cd),
                tint = if (isActive) Coral.copy(alpha = 0.8f) else tokens.textFaint,
                modifier = Modifier.size(KuroSpacing.lg),
            )
        }
    }
}

@Deprecated("Replaced by SoundscapeTransportBar")
@Composable
fun SoundscapeDockPeekStrip(
    playingAmbienceName: String?,
    playingMusicName: String?,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isActive = playingAmbienceName != null || playingMusicName != null
    val statusText = if (isActive) {
        soundscapeStatusLine(playingAmbienceName, playingMusicName)
    } else {
        stringResource(R.string.soundscape_home_hint)
    }
    val expandLabel = stringResource(R.string.soundscape_dock_expand_cd)
    val rowFill = when {
        isActive -> Peach.copy(alpha = 0.28f)
        else -> tokens.cardFill.copy(alpha = 0.45f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(KuroSpacing.soundscapeTransportBarHeight)
            .background(rowFill)
            .clickable(role = Role.Button, onClickLabel = expandLabel, onClick = onExpand)
            .padding(horizontal = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Icon(
            imageVector = if (isActive) Lucide.AudioLines else Lucide.Music,
            contentDescription = null,
            tint = if (isActive) Coral else tokens.textSecondary,
            modifier = Modifier.size(KuroSpacing.md + KuroSpacing.xs),
        )
        Text(
            text = statusText,
            color = if (isActive) Cocoa else tokens.textSecondary,
            style = KuroType.labelMeta.copy(
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Lucide.ChevronUp,
            contentDescription = expandLabel,
            tint = if (isActive) Coral.copy(alpha = 0.85f) else tokens.textFaint,
            modifier = Modifier.size(KuroSpacing.lg),
        )
    }
}

/** Floating mini player above the bottom dock (Option C home integration). */
@Deprecated("Replaced by SoundscapeDockRow inside DashboardDock")
@Composable
fun SoundscapeMiniPlayer(
    playingAmbienceName: String?,
    playingMusicName: String?,
    onOpenSoundscape: () -> Unit,
    onMuteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isActive = playingAmbienceName != null || playingMusicName != null
    val statusText = if (isActive) {
        soundscapeStatusLine(playingAmbienceName, playingMusicName)
    } else {
        stringResource(R.string.soundscape_home_hint)
    }
    val openLabel = stringResource(R.string.soundscape_mini_open_cd)
    val shape = RoundedCornerShape(KuroSpacing.radiusPill)
    val shadowElevation = if (isActive) 10.dp else 4.dp
    val shadowColor = if (isActive) Coral.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.08f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(KuroSpacing.soundscapeTransportBarHeight)
            .shadow(shadowElevation, shape, spotColor = shadowColor, ambientColor = shadowColor)
            .clip(shape)
            .background(if (isActive) CardWhite else tokens.cardFill)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) Coral.copy(alpha = 0.42f) else tokens.cardBorder.copy(alpha = 0.55f),
                shape = shape,
            )
            .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpenSoundscape)
            .padding(start = KuroSpacing.sm, end = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.journeyMetricIconCircle + KuroSpacing.xs)
                .clip(CircleShape)
                .background(if (isActive) Peach.copy(alpha = 0.45f) else tokens.cardFill)
                .border(
                    1.dp,
                    if (isActive) Coral.copy(alpha = 0.2f) else tokens.cardBorder.copy(alpha = 0.45f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isActive) Lucide.AudioLines else Lucide.Music,
                contentDescription = null,
                tint = if (isActive) Coral else tokens.textSecondary,
                modifier = Modifier.size(KuroSpacing.lg),
            )
        }
        Text(
            text = statusText,
            color = if (isActive) Cocoa else tokens.textSecondary,
            style = KuroType.labelStrong.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.journeyMetricIconCircle)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.soundscape_home_mute_cd),
                        onClick = onMuteAll,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.Volume2,
                    contentDescription = stringResource(R.string.soundscape_home_mute_cd),
                    tint = Coral,
                    modifier = Modifier.size(KuroSpacing.lg),
                )
            }
        }
        Icon(
            imageVector = Lucide.ChevronDown,
            contentDescription = stringResource(R.string.soundscape_mini_expand_cd),
            tint = if (isActive) Coral.copy(alpha = 0.75f) else tokens.textFaint,
            modifier = Modifier.size(KuroSpacing.lg),
        )
    }
}

/** Focus-home section: Soundscape entry, live status, and quick mute. */
@Composable
fun SoundscapeHomeCard(
    playingAmbienceName: String?,
    playingMusicName: String?,
    ambienceVolume: Float,
    musicVolume: Float,
    onOpenSoundscape: () -> Unit,
    onMuteAll: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isActive = playingAmbienceName != null || playingMusicName != null

    if (compact) {
        SoundscapeCompactBar(
            playingAmbienceName = playingAmbienceName,
            playingMusicName = playingMusicName,
            onOpenSoundscape = onOpenSoundscape,
            onMuteAll = onMuteAll,
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(if (isActive) tokens.activeCardFill else tokens.cardFill)
            .border(
                width = 1.dp,
                color = if (isActive) tokens.accent.copy(alpha = 0.4f) else tokens.cardBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(KuroSpacing.radiusMd),
            )
            .clickable(role = Role.Button, onClick = onOpenSoundscape)
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(KuroSpacing.journeyMetricIconCircle + KuroSpacing.xs)
                        .clip(CircleShape)
                        .background(if (isActive) tokens.accent else tokens.cardFill)
                        .border(1.dp, if (isActive) Color.Transparent else tokens.cardBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isActive) Lucide.AudioLines else Lucide.Music,
                        contentDescription = null,
                        tint = if (isActive) tokens.accentOn else tokens.textSecondary,
                        modifier = Modifier.size(KuroSpacing.lg),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = KuroSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.sounds_title),
                        color = tokens.textPrimary,
                        style = KuroType.headingCard,
                    )
                    Text(
                        text = if (isActive) {
                            stringResource(R.string.soundscape_home_playing)
                        } else {
                            stringResource(R.string.soundscape_home_idle)
                        },
                        color = tokens.textSecondary,
                        style = KuroType.bodySecondary,
                    )
                }
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(KuroSpacing.journeyMetricIconCircle)
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.soundscape_home_mute_cd),
                                onClick = onMuteAll,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Lucide.VolumeX,
                            contentDescription = stringResource(R.string.soundscape_home_mute_cd),
                            tint = tokens.accent,
                            modifier = Modifier.size(KuroSpacing.lg),
                        )
                    }
                }
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = tokens.textSecondary,
                    modifier = Modifier.size(KuroSpacing.lg),
                )
            }

            if (isActive) {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
                    playingAmbienceName?.let { name ->
                        SoundscapeLayerStatusRow(
                            icon = Lucide.CloudRain,
                            label = stringResource(R.string.soundscape_volume_ambience),
                            trackName = name,
                            volume = ambienceVolume,
                        )
                    }
                    playingMusicName?.let { name ->
                        SoundscapeLayerStatusRow(
                            icon = Lucide.Music,
                            label = stringResource(R.string.soundscape_volume_music),
                            trackName = name,
                            volume = musicVolume,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.soundscape_home_hint),
                    color = tokens.textMuted,
                    style = KuroType.bodySecondary,
                )
                Text(
                    text = stringResource(R.string.soundscape_home_open),
                    color = tokens.accent,
                    style = KuroType.labelStrong,
                )
            }
        }
    }
}

@Composable
private fun SoundscapeLayerStatusRow(
    icon: ImageVector,
    label: String,
    trackName: String,
    volume: Float,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val percent = (volume * 100f).toInt().coerceIn(0, 100)

    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.textSecondary,
                modifier = Modifier.size(KuroSpacing.md + KuroSpacing.xs),
            )
            Text(
                text = label,
                color = tokens.textMuted,
                style = KuroType.labelMeta,
            )
            Text(
                text = trackName,
                color = tokens.textPrimary,
                style = KuroType.labelStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.soundscape_volume_percent, percent),
                color = tokens.textSecondary,
                style = KuroType.labelMeta,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(KuroSpacing.xs)
                .clip(RoundedCornerShape(KuroSpacing.xs))
                .background(tokens.segmentedTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(volume.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(KuroSpacing.xs))
                    .background(tokens.accent),
            )
        }
    }
}

/**
 * Resolves a music tile tap, gating cloud tracks on their download state:
 * locked tracks always route through [onMusicSelected] (premium sheet), a
 * download in flight is ignored, a not-downloaded track starts its download,
 * and a bundled/downloaded track plays or toggles as before.
 */
private fun musicTileClick(
    track: MusicOption,
    isPlaying: Boolean,
    onMusicSelected: (MusicOption) -> Unit,
    onMusicToggle: (MusicOption) -> Unit,
    onDownloadTrack: (Int) -> Unit,
): () -> Unit = when {
    track.locked -> { { onMusicSelected(track) } }
    track.remote?.downloadState is DownloadState.Downloading -> { {} }
    track.remote?.downloadState == DownloadState.NotDownloaded -> { { onDownloadTrack(track.id) } }
    else -> { { if (isPlaying) onMusicToggle(track) else onMusicSelected(track) } }
}

/** Ambience counterpart of [musicTileClick]. */
private fun ambienceTileClick(
    sound: SoundOption,
    isPlaying: Boolean,
    onAmbienceSelected: (SoundOption) -> Unit,
    onAmbienceToggle: (SoundOption) -> Unit,
    onDownloadTrack: (Int) -> Unit,
): () -> Unit = when {
    sound.locked -> { { onAmbienceSelected(sound) } }
    sound.remote?.downloadState is DownloadState.Downloading -> { {} }
    sound.remote?.downloadState == DownloadState.NotDownloaded -> { { onDownloadTrack(sound.id) } }
    else -> { { if (isPlaying) onAmbienceToggle(sound) else onAmbienceSelected(sound) } }
}

@Composable
fun SoundsScreen(
    ambienceOptions: List<SoundOption>,
    musicOptions: List<MusicOption>,
    selectedAmbienceId: Int,
    playingAmbienceId: Int?,
    selectedMusicId: Int?,
    playingMusicId: Int?,
    onAmbienceSelected: (SoundOption) -> Unit,
    onAmbienceToggle: (SoundOption) -> Unit,
    onMusicSelected: (MusicOption) -> Unit,
    onMusicToggle: (MusicOption) -> Unit,
    modifier: Modifier = Modifier,
    onDownloadTrack: (Int) -> Unit = {},
    onDeleteDownload: (Int) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    @Suppress("UNUSED_PARAMETER") overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var ambienceExpanded by remember { mutableStateOf(false) }
    var musicExpanded by remember { mutableStateOf(false) }

    // Scroll is owned by KuroAdaptiveBottomSheet — do not nest verticalScroll here
    // or Compose throws infinite-height constraint crash when the sheet opens.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(top = KuroSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = KuroSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        ) {
            Text(
                text = stringResource(R.string.sounds_title),
                color = Cocoa,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NunitoFamily,
            )
            Text(
                text = stringResource(R.string.sounds_subtitle),
                color = tokens.textSecondary,
                style = KuroType.bodySecondary,
            )
        }

        SoundscapeSectionHeader(
            title = stringResource(R.string.soundscape_section_ambience),
            expanded = ambienceExpanded,
            onToggleExpand = { ambienceExpanded = !ambienceExpanded },
            modifier = Modifier.padding(horizontal = KuroSpacing.lg),
        )
        if (ambienceExpanded) {
            SoundscapeExpandedAmbienceGrid(
                options = ambienceOptions,
                playingAmbienceId = playingAmbienceId,
                onAmbienceSelected = onAmbienceSelected,
                onAmbienceToggle = onAmbienceToggle,
                onDownloadTrack = onDownloadTrack,
                onDeleteDownload = onDeleteDownload,
                modifier = Modifier.padding(horizontal = KuroSpacing.lg),
            )
        } else {
            SoundscapeHorizontalRow {
                ambienceOptions.forEach { sound ->
                    val isPlaying = sound.id == playingAmbienceId
                    AmbienceTile(
                        sound = sound,
                        isPlaying = isPlaying,
                        tileLabel = ambienceShortName(sound.id),
                        onClick = ambienceTileClick(
                            sound = sound,
                            isPlaying = isPlaying,
                            onAmbienceSelected = onAmbienceSelected,
                            onAmbienceToggle = onAmbienceToggle,
                            onDownloadTrack = onDownloadTrack,
                        ),
                    )
                }
            }
        }

        SoundscapeSectionHeader(
            title = stringResource(R.string.soundscape_section_lofi),
            expanded = musicExpanded,
            onToggleExpand = { musicExpanded = !musicExpanded },
            modifier = Modifier.padding(horizontal = KuroSpacing.lg),
        )
        if (musicExpanded) {
            SoundscapeExpandedLofiGrid(
                options = musicOptions,
                playingMusicId = playingMusicId,
                onMusicSelected = onMusicSelected,
                onMusicToggle = onMusicToggle,
                onDownloadTrack = onDownloadTrack,
                onDeleteDownload = onDeleteDownload,
                modifier = Modifier.padding(horizontal = KuroSpacing.lg),
            )
        } else {
            SoundscapeHorizontalRow(itemSpacing = KuroSpacing.md) {
                musicOptions.forEach { track ->
                    val isPlaying = track.id == playingMusicId
                    LofiVinylTile(
                        track = track,
                        isPlaying = isPlaying,
                        onClick = musicTileClick(
                            track = track,
                            isPlaying = isPlaying,
                            onMusicSelected = onMusicSelected,
                            onMusicToggle = onMusicToggle,
                            onDownloadTrack = onDownloadTrack,
                        ),
                    )
                }
            }
        }
    }
}

/** Sticky Soundscape footer: divider, volume steps, play/pause. */
@Composable
fun SoundscapeSheetStickyFooter(
    ambienceVolume: Float,
    musicVolume: Float,
    onAmbienceVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    ambienceVolumeEnabled: Boolean,
    musicVolumeEnabled: Boolean,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    scrollOverlay: Float = 0f,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val overlay = scrollOverlay.coerceIn(0f, 1f)
    val footerElevation by animateDpAsState(
        targetValue = KuroSpacing.soundscapeStickyFooterElevation * overlay,
        animationSpec = tween(durationMillis = 180),
        label = "soundscapeFooterElevation",
    )
    val shadowAmbient = if (isDark) {
        Color.Black.copy(alpha = 0.52f * overlay)
    } else {
        Color(0xFF9A7B6A).copy(alpha = 0.38f * overlay)
    }
    val shadowSpot = if (isDark) {
        Color.Black.copy(alpha = 0.62f * overlay)
    } else {
        Color(0xFF7A5C4C).copy(alpha = 0.32f * overlay)
    }
    val scrimColor = if (isDark) Color.Black else Cocoa

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = footerElevation,
                shape = RectangleShape,
                clip = false,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot,
            )
            .background(tokens.cardFill),
    ) {
        if (overlay > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KuroSpacing.soundscapeStickyFooterScrim)
                    .align(Alignment.TopCenter)
                    .offset(y = -KuroSpacing.soundscapeStickyFooterScrim)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.55f to scrimColor.copy(alpha = 0.04f * overlay),
                                1f to scrimColor.copy(alpha = 0.14f * overlay),
                            ),
                        ),
                    ),
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(tokens.cardBorder.copy(alpha = 0.45f + (0.35f * overlay))),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.lg)
                .padding(top = KuroSpacing.sm, bottom = KuroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            SoundscapeVolumeStepRow(
                label = stringResource(R.string.soundscape_volume_ambience),
                value = ambienceVolume,
                onValueChange = onAmbienceVolumeChange,
                enabled = ambienceVolumeEnabled,
                compact = true,
                modifier = Modifier.weight(1f),
            )
            SoundscapeVolumeStepRow(
                label = stringResource(R.string.soundscape_volume_music),
                value = musicVolume,
                onValueChange = onMusicVolumeChange,
                enabled = musicVolumeEnabled,
                compact = true,
                modifier = Modifier.weight(1f),
            )
            SoundscapeSheetPlayPauseButton(
                isPlaying = isPlaying,
                onClick = onPlayPause,
                compact = true,
            )
        }
        }
    }
}

@Composable
private fun SoundscapeSheetPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val label = if (isPlaying) {
        stringResource(R.string.soundscape_sheet_pause)
    } else {
        stringResource(R.string.soundscape_sheet_play)
    }
    val contentDescription = if (isPlaying) {
        stringResource(R.string.soundscape_transport_pause_cd)
    } else {
        stringResource(R.string.soundscape_transport_play_cd)
    }

    if (compact) {
        Box(
            modifier = modifier
                .size(KuroSpacing.journeyMetricIconCircle + KuroSpacing.sm)
                .clip(CircleShape)
                .background(if (isPlaying) tokens.cardFill else Coral)
                .border(
                    width = if (isPlaying) 1.5.dp else 0.dp,
                    color = Coral.copy(alpha = 0.65f),
                    shape = CircleShape,
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = contentDescription,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                contentDescription = contentDescription,
                tint = if (isPlaying) Coral else Color.White,
                modifier = Modifier.size(KuroSpacing.journeyMetricIconGlyph),
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(if (isPlaying) tokens.cardFill else Coral)
            .border(
                width = if (isPlaying) 1.5.dp else 0.dp,
                color = Coral.copy(alpha = 0.65f),
                shape = RoundedCornerShape(KuroSpacing.radiusPill),
            )
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .padding(vertical = KuroSpacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
            contentDescription = null,
            tint = if (isPlaying) Coral else Color.White,
            modifier = Modifier.size(KuroSpacing.lg),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = label,
            color = if (isPlaying) Coral else Color.White,
            style = KuroType.labelStrong,
        )
    }
}

@Composable
private fun SoundscapeExpandedAmbienceGrid(
    options: List<SoundOption>,
    playingAmbienceId: Int?,
    onAmbienceSelected: (SoundOption) -> Unit,
    onAmbienceToggle: (SoundOption) -> Unit,
    modifier: Modifier = Modifier,
    onDownloadTrack: (Int) -> Unit = {},
    onDeleteDownload: (Int) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                row.forEach { sound ->
                    val isPlaying = sound.id == playingAmbienceId
                    AmbienceTile(
                        sound = sound,
                        isPlaying = isPlaying,
                        tileLabel = ambienceShortName(sound.id),
                        onClick = ambienceTileClick(
                            sound = sound,
                            isPlaying = isPlaying,
                            onAmbienceSelected = onAmbienceSelected,
                            onAmbienceToggle = onAmbienceToggle,
                            onDownloadTrack = onDownloadTrack,
                        ),
                        showRemoveDownload = true,
                        onDeleteDownload = onDeleteDownload,
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundscapeExpandedLofiGrid(
    options: List<MusicOption>,
    playingMusicId: Int?,
    onMusicSelected: (MusicOption) -> Unit,
    onMusicToggle: (MusicOption) -> Unit,
    modifier: Modifier = Modifier,
    onDownloadTrack: (Int) -> Unit = {},
    onDeleteDownload: (Int) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
                row.forEach { track ->
                    val isPlaying = track.id == playingMusicId
                    LofiVinylTile(
                        track = track,
                        isPlaying = isPlaying,
                        onClick = musicTileClick(
                            track = track,
                            isPlaying = isPlaying,
                            onMusicSelected = onMusicSelected,
                            onMusicToggle = onMusicToggle,
                            onDownloadTrack = onDownloadTrack,
                        ),
                        showRemoveDownload = true,
                        onDeleteDownload = onDeleteDownload,
                    )
                }
            }
        }
    }
}

/** Section header: title + "See all ›" affordance on the right. */
@Composable
private fun SoundscapeSectionHeader(
    title: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val toggleLabel = if (expanded) {
        stringResource(R.string.soundscape_show_less)
    } else {
        stringResource(R.string.soundscape_see_all)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Text(
            text = title,
            color = tokens.textPrimary,
            style = KuroType.headingCard,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.soundscape_see_all_cd, title),
                    onClick = onToggleExpand,
                )
                .padding(horizontal = KuroSpacing.xs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
        ) {
            Text(
                text = toggleLabel,
                color = tokens.textSecondary,
                style = KuroType.labelStrong,
            )
            Icon(
                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronRight,
                contentDescription = null,
                tint = tokens.textFaint,
                modifier = Modifier.size(KuroSpacing.md),
            )
        }
    }
}

@Composable
private fun SoundscapeHorizontalRow(
    itemSpacing: androidx.compose.ui.unit.Dp = KuroSpacing.sm,
    content: @Composable RowScope.() -> Unit,
) {
    // Full-width scroll row with symmetric edge insets so tiles and playing halos
    // are not clipped at the sheet edges.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { clip = false }
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        Spacer(Modifier.width(KuroSpacing.lg))
        content()
        Spacer(Modifier.width(KuroSpacing.lg + KuroSpacing.soundscapeScrollPeek))
    }
}

@Composable
private fun soundscapeStatusLine(
    playingAmbienceName: String?,
    playingMusicName: String?,
): String {
    val sep = stringResource(R.string.soundscape_compact_separator)
    return when {
        playingAmbienceName != null && playingMusicName != null ->
            stringResource(
                R.string.soundscape_compact_playing,
                "$playingAmbienceName$sep$playingMusicName",
            )
        playingAmbienceName != null ->
            stringResource(R.string.soundscape_compact_playing, playingAmbienceName)
        playingMusicName != null ->
            stringResource(R.string.soundscape_compact_playing, playingMusicName)
        else -> stringResource(R.string.soundscape_compact_idle)
    }
}

@Composable
private fun ambienceShortName(soundId: Int): String = when (soundId) {
    1 -> stringResource(R.string.sound_rain_short)
    2 -> stringResource(R.string.sound_cafe_short)
    else -> ""
}

@Composable
private fun AmbienceTile(
    sound: SoundOption,
    isPlaying: Boolean,
    tileLabel: String,
    onClick: () -> Unit,
    showRemoveDownload: Boolean = false,
    onDeleteDownload: (Int) -> Unit = {},
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val borderColor = if (isPlaying) Coral else tokens.cardBorder.copy(alpha = 0.7f)
    val borderWidth = if (isPlaying) 2.dp else 1.dp
    val downloadState = sound.remote?.downloadState

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        modifier = Modifier
            .width(KuroSpacing.soundscapeAmbienceTile)
            .clickable(
                role = Role.Button,
                onClickLabel = when {
                    sound.locked -> stringResource(R.string.sound_unlock_cd, sound.name)
                    downloadState == DownloadState.NotDownloaded ->
                        stringResource(R.string.soundscape_download)
                    isPlaying -> stringResource(R.string.sound_stop_cd, sound.name)
                    else -> stringResource(R.string.sound_play_cd, sound.name)
                },
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.soundscapeAmbienceTile)
                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                .border(borderWidth, borderColor, RoundedCornerShape(KuroSpacing.radiusMd)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(ambienceIconRes(sound.id)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (sound.locked) {
                KuroPremiumBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KuroSpacing.xs),
                )
            }
            when (downloadState) {
                DownloadState.NotDownloaded -> Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(KuroSpacing.xs)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.CloudDownload,
                        contentDescription = stringResource(R.string.soundscape_download),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                is DownloadState.Downloading -> CircularProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(KuroSpacing.soundscapeAmbienceTile - KuroSpacing.md),
                    strokeWidth = 2.dp,
                    color = Coral,
                )
                else -> Unit // bundled or downloaded: no badge
            }
        }
        Text(
            text = if (downloadState == DownloadState.NotDownloaded) {
                stringResource(
                    R.string.soundscape_download_size,
                    "%.1f".format((sound.remote?.fileSizeBytes ?: 0L) / 1_000_000f),
                )
            } else {
                tileLabel
            },
            color = tokens.textPrimary,
            style = KuroType.labelMeta,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            showRemoveDownload && downloadState == DownloadState.Downloaded -> {
                IconButton(onClick = { onDeleteDownload(sound.id) }) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = stringResource(R.string.soundscape_remove_download),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            isPlaying -> SoundscapePlayingChip()
            else -> Spacer(Modifier.height(KuroSpacing.md + KuroSpacing.xs))
        }
    }
}

@Composable
private fun LofiVinylTile(
    track: MusicOption,
    isPlaying: Boolean,
    onClick: () -> Unit,
    showRemoveDownload: Boolean = false,
    onDeleteDownload: (Int) -> Unit = {},
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val vinylSize = KuroSpacing.soundscapeLofiVinyl
    val haloSize = vinylSize + 20.dp
    val downloadState = track.remote?.downloadState

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        modifier = Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = when {
                    track.locked -> stringResource(R.string.sound_unlock_cd, track.name)
                    downloadState == DownloadState.NotDownloaded ->
                        stringResource(R.string.soundscape_download)
                    isPlaying -> stringResource(R.string.sound_stop_cd, track.name)
                    else -> stringResource(R.string.sound_play_cd, track.name)
                },
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier.size(haloSize),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(haloSize)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.10f)),
                )
                Box(
                    modifier = Modifier
                        .size(vinylSize + 12.dp)
                        .border(2.dp, Coral.copy(alpha = 0.32f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(vinylSize + 4.dp)
                        .border(2.dp, Coral, CircleShape),
                )
            }
            LofiVinylDisc(
                trackId = track.id,
                isSpinning = isPlaying,
                modifier = Modifier.size(vinylSize),
            )
            if (track.locked) {
                KuroPremiumBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KuroSpacing.xs),
                )
            }
            when (downloadState) {
                DownloadState.NotDownloaded -> Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.CloudDownload,
                        contentDescription = stringResource(R.string.soundscape_download),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                is DownloadState.Downloading -> CircularProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(vinylSize + 8.dp),
                    strokeWidth = 2.dp,
                    color = Coral,
                )
                else -> Unit // bundled or downloaded: no badge
            }
        }
        Column(
            modifier = Modifier.width(haloSize),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track.name,
                color = tokens.textPrimary,
                style = KuroType.labelMeta.copy(
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (downloadState == DownloadState.NotDownloaded) {
                    stringResource(
                        R.string.soundscape_download_size,
                        "%.1f".format((track.remote?.fileSizeBytes ?: 0L) / 1_000_000f),
                    )
                } else {
                    track.artist
                },
                color = tokens.textMuted,
                style = KuroType.labelMeta,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showRemoveDownload && downloadState == DownloadState.Downloaded) {
                IconButton(onClick = { onDeleteDownload(track.id) }) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = stringResource(R.string.soundscape_remove_download),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundscapeVolumeStepRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val snapped = snapVolume(value)
    val level = (snapped * VOLUME_SNAP_STEPS).roundToInt().coerceIn(0, VOLUME_SNAP_STEPS)
    val fraction = level / VOLUME_SNAP_STEPS.toFloat()
    val percent = (snapped * 100f).toInt().coerceIn(0, 100)
    val thumbWidth = if (compact) 10.dp else 14.dp
    val thumbHeight = if (compact) 18.dp else 22.dp
    val trackHeight = 3.dp
    val snapTickHeight = if (compact) 9.dp else 10.dp
    val labelColor = if (enabled) tokens.textPrimary else tokens.textFaint
    val percentColor = if (enabled) tokens.textSecondary else tokens.textFaint
    val trackFill = if (enabled) Peach.copy(alpha = 0.48f) else tokens.cardBorder.copy(alpha = 0.35f)
    val activeFill = if (enabled) Coral else tokens.textFaint.copy(alpha = 0.45f)
    val thumbFill = if (enabled) Coral else tokens.textFaint.copy(alpha = 0.55f)
    val snapTickActive = if (enabled) Coral.copy(alpha = 0.85f) else tokens.textFaint.copy(alpha = 0.35f)
    val snapTickIdle = if (enabled) tokens.cardBorder.copy(alpha = 0.65f) else tokens.cardBorder.copy(alpha = 0.35f)
    val onValueChangeState by rememberUpdatedState(onValueChange)
    // Local drag level keeps the thumb moving while the finger is down. Without
    // this, restarting pointerInput on each value change can cancel the gesture
    // until finger-up (fixed below with a stable pointerInput key).
    var dragLevel by remember { mutableIntStateOf(-1) }
    val displayLevel = if (dragLevel >= 0) dragLevel else level
    val displayFraction = displayLevel / VOLUME_SNAP_STEPS.toFloat()

    Column(
        modifier = if (compact) modifier else modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else KuroSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = labelColor,
                style = if (compact) KuroType.labelMeta else KuroType.labelStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.soundscape_volume_percent, percent),
                color = percentColor,
                style = KuroType.labelMeta,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight + if (compact) 2.dp else KuroSpacing.xs)
                .then(
                    if (enabled) {
                        // Stable key — do NOT use onValueChange/value as key or
                        // the coroutine restarts on every snap and cancels the gesture.
                        Modifier.pointerInput(Unit) {
                            fun setFromX(x: Float) {
                                val clamped = (x / size.width.toFloat()).coerceIn(0f, 1f)
                                val snapped = snapVolume(clamped)
                                dragLevel = (snapped * VOLUME_SNAP_STEPS)
                                    .roundToInt()
                                    .coerceIn(0, VOLUME_SNAP_STEPS)
                                onValueChangeState(snapped)
                            }
                            // Single gesture loop: handles press, drag, and release
                            // without the two-coroutine sequencing problem where
                            // detectTapGestures loops forever and blocks the drag handler.
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                setFromX(down.position.x)
                                // Track subsequent events until the pointer lifts.
                                var isDragging = true
                                while (isDragging) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || change.changedToUp()) {
                                        isDragging = false
                                    } else {
                                        change.consume()
                                        setFromX(change.position.x)
                                    }
                                }
                                dragLevel = -1
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            val maxTravel = (maxWidth - thumbWidth).coerceAtLeast(0.dp)
            val thumbOffset = maxTravel * displayFraction
            val activeTrackWidth = thumbOffset + thumbWidth / 2

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(trackFill),
            )
            if (level > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(activeTrackWidth.coerceAtMost(maxWidth))
                        .height(trackHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(activeFill),
                )
            }
            // Snap marks — 0 / 25 / 50 / 75 / 100 % (matches [snapVolume] steps).
            repeat(VOLUME_SNAP_STEPS + 1) { index ->
                val snapFraction = index / VOLUME_SNAP_STEPS.toFloat()
                val tickCenter = maxTravel * snapFraction + thumbWidth / 2
                val tickActive = enabled && index <= displayLevel
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (tickCenter - 0.5.dp).coerceAtLeast(0.dp))
                        .width(1.dp)
                        .height(snapTickHeight)
                        .clip(RoundedCornerShape(0.5.dp))
                        .background(if (tickActive) snapTickActive else snapTickIdle),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset)
                    .size(width = thumbWidth, height = thumbHeight)
                    .clip(RoundedCornerShape(if (compact) 5.dp else 6.dp))
                    .background(thumbFill)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = if (enabled) 0.35f else 0.12f),
                        shape = RoundedCornerShape(if (compact) 5.dp else 6.dp),
                    ),
            )
        }
    }
}

@Composable
private fun LofiVinylDisc(
    trackId: Int,
    isSpinning: Boolean,
    modifier: Modifier = Modifier,
) {
    val spinAngle = rememberSoundscapeVinylSpinAngle(
        label = "lofiVinylSpin_$trackId",
        isSpinning = isSpinning,
    )
    Image(
        painter = painterResource(lofiVinylDiscRes(trackId)),
        contentDescription = null,
        modifier = modifier
            .rotate(spinAngle)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun SoundscapePlayingChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(Peach.copy(alpha = 0.55f))
            .padding(horizontal = KuroSpacing.sm + 2.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.sound_pill_playing),
            color = Coral,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = NunitoFamily,
        )
    }
}

@DrawableRes
private fun ambienceIconRes(soundId: Int): Int = when (soundId) {
    1 -> R.drawable.soundscape_ambience_rain
    2 -> R.drawable.soundscape_ambience_cafe
    else -> R.drawable.soundscape_ambience_rain
}

@DrawableRes
private fun lofiVinylDiscRes(trackId: Int): Int = when (trackId) {
    1 -> R.drawable.soundscape_vinyl_soft_piano
    2 -> R.drawable.soundscape_vinyl_chill_hop
    3 -> R.drawable.soundscape_vinyl_midnight_jazz
    else -> R.drawable.soundscape_vinyl_base
}
