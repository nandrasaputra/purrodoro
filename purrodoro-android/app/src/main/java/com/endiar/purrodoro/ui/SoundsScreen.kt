package com.endiar.purrodoro.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.composables.icons.lucide.CloudRain
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.GlassTreatment
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption
import com.endiar.purrodoro.ui.designsystem.KuroBadge
import com.endiar.purrodoro.ui.designsystem.KuroPill
import com.endiar.purrodoro.ui.designsystem.KuroPillKind
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.CardWhite
import com.endiar.purrodoro.ui.theme.Cocoa
import com.endiar.purrodoro.ui.theme.Coral
import com.endiar.purrodoro.ui.theme.LofiChillDark
import com.endiar.purrodoro.ui.theme.LofiChillLight
import com.endiar.purrodoro.ui.theme.LofiJazzDark
import com.endiar.purrodoro.ui.theme.LofiJazzLight
import com.endiar.purrodoro.ui.theme.LofiPianoDark
import com.endiar.purrodoro.ui.theme.LofiPianoLight
import com.endiar.purrodoro.ui.theme.LofiStudyDark
import com.endiar.purrodoro.ui.theme.LofiStudyLight
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.Peach
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

// NOTE: Soundscape audio is not active yet. Selection, volume, and mute state
// update UI only — real looping playback still needs wiring.

private val VinylBlack = Color(0xFF141414)
private val VinylGroove = Color(0xFF3A3A3A)

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
    val spinTransition = rememberInfiniteTransition(label = "transportVinylSpin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "transportVinylAngle",
    )
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
            TransportVinylDisc(
                isSpinning = isPlaying,
                spinAngle = spinAngle,
                modifier = Modifier.size(KuroSpacing.journeyMetricIconCircle),
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

@Composable
private fun TransportVinylDisc(
    isSpinning: Boolean,
    spinAngle: Float,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .rotate(if (isSpinning) spinAngle else 0f)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2f
            drawCircle(color = VinylBlack, radius = radius, center = center)
            val grooveStroke = Stroke(width = 0.9f)
            listOf(0.45f, 0.58f, 0.71f, 0.84f).forEach { scale ->
                drawCircle(
                    color = VinylGroove,
                    radius = radius * scale,
                    center = center,
                    style = grooveStroke,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(KuroSpacing.lg + KuroSpacing.xs)
                .clip(CircleShape)
                .background(Peach.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.Music,
                contentDescription = null,
                tint = if (isSpinning) Coral else tokens.textSecondary,
                modifier = Modifier.size(KuroSpacing.md + KuroSpacing.xs),
            )
        }
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(CardWhite)
                .border(0.5.dp, VinylGroove, CircleShape),
        )
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

@Composable
fun SoundsScreen(
    ambienceOptions: List<SoundOption>,
    musicOptions: List<MusicOption>,
    selectedAmbienceId: Int,
    playingAmbienceId: Int?,
    selectedMusicId: Int?,
    playingMusicId: Int?,
    ambienceVolume: Float,
    musicVolume: Float,
    onAmbienceSelected: (SoundOption) -> Unit,
    onAmbienceToggle: (SoundOption) -> Unit,
    onMusicSelected: (MusicOption) -> Unit,
    onMusicToggle: (MusicOption) -> Unit,
    onAmbienceVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onMuteAll: () -> Unit,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    onSeeAllAmbience: () -> Unit = {},
    onSeeAllMusic: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    @Suppress("UNUSED_PARAMETER") overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    // Scroll is owned by KuroAdaptiveBottomSheet — do not nest verticalScroll here
    // or Compose throws infinite-height constraint crash when the sheet opens.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(horizontal = KuroSpacing.lg)
            .padding(top = KuroSpacing.xs, bottom = KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        // Header: title + subtitle on the left, a coral "Done" pill on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
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
            SoundscapeDoneButton(onClick = onDone)
        }

        SoundscapeSectionHeader(
            title = stringResource(R.string.soundscape_section_ambience),
            onSeeAll = onSeeAllAmbience,
        )
        SoundscapeHorizontalRow {
            ambienceOptions.forEach { sound ->
                val isPlaying = sound.id == playingAmbienceId
                AmbienceTile(
                    sound = sound,
                    isPlaying = isPlaying,
                    tileLabel = ambienceShortName(sound.id),
                    onClick = {
                        if (isPlaying) onAmbienceToggle(sound) else onAmbienceSelected(sound)
                    },
                )
            }
        }

        SoundscapeSectionHeader(
            title = stringResource(R.string.soundscape_section_lofi),
            onSeeAll = onSeeAllMusic,
        )
        SoundscapeHorizontalRow(itemSpacing = KuroSpacing.md) {
            musicOptions.forEach { track ->
                val isPlaying = track.id == playingMusicId
                LofiVinylTile(
                    track = track,
                    isPlaying = isPlaying,
                    onClick = {
                        if (isPlaying) onMusicToggle(track) else onMusicSelected(track)
                    },
                )
            }
        }

        Spacer(Modifier.height(KuroSpacing.xs))

        // Reference places the two volume controls side-by-side.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.lg),
        ) {
            SoundscapeVolumeRow(
                label = stringResource(R.string.soundscape_volume_ambience),
                value = ambienceVolume,
                onValueChange = onAmbienceVolumeChange,
                modifier = Modifier.weight(1f),
            )
            SoundscapeVolumeRow(
                label = stringResource(R.string.soundscape_volume_music),
                value = musicVolume,
                onValueChange = onMusicVolumeChange,
                modifier = Modifier.weight(1f),
            )
        }

        MuteAllButton(onClick = onMuteAll)
    }
}

/** Coral "Done" pill in the Soundscape header (matches the reference design). */
@Composable
private fun SoundscapeDoneButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(Coral)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.common_done),
            color = Color.White,
            style = KuroType.labelStrong,
        )
    }
}

/**
 * Section header: a placeholder leading glyph + title on the left, a "See all ›"
 * affordance on the right. The leading icon is a temporary placeholder until
 * final per-section art is supplied.
 */
@Composable
private fun SoundscapeSectionHeader(
    title: String,
    onSeeAll: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        SoundscapePlaceholderIcon(
            tint = tokens.textSecondary,
            modifier = Modifier.size(KuroSpacing.lg),
        )
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
                    onClick = onSeeAll,
                )
                .padding(horizontal = KuroSpacing.xs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
        ) {
            Text(
                text = stringResource(R.string.soundscape_see_all),
                color = tokens.textSecondary,
                style = KuroType.labelStrong,
            )
            SoundscapePlaceholderIcon(
                tint = tokens.textFaint,
                modifier = Modifier.size(KuroSpacing.md),
            )
        }
    }
}

/**
 * Generic placeholder icon used across the Soundscape detail screen in place
 * of finalized iconography. Renders [R.drawable.soundscape_placeholder].
 */
@Composable
private fun SoundscapePlaceholderIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.soundscape_placeholder),
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
private fun SoundscapeHorizontalRow(
    itemSpacing: androidx.compose.ui.unit.Dp = KuroSpacing.sm,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        content()
        Spacer(Modifier.width(KuroSpacing.soundscapeScrollPeek))
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
    3 -> stringResource(R.string.sound_forest_short)
    4 -> stringResource(R.string.sound_fire_short)
    5 -> stringResource(R.string.sound_keys_short)
    else -> ""
}

@Composable
private fun AmbienceTile(
    sound: SoundOption,
    isPlaying: Boolean,
    tileLabel: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val borderColor = if (isPlaying) Coral else tokens.cardBorder.copy(alpha = 0.7f)
    val borderWidth = if (isPlaying) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        modifier = Modifier
            .width(KuroSpacing.soundscapeAmbienceTile)
            .clickable(
                role = Role.Button,
                onClickLabel = when {
                    sound.locked -> stringResource(R.string.sound_unlock_cd, sound.name)
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
                .background(tokens.cardFill)
                .border(borderWidth, borderColor, RoundedCornerShape(KuroSpacing.radiusMd)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.soundscapeAmbienceIcon)
                    .clip(CircleShape)
                    .background(ambienceTileFill(sound.id)),
                contentAlignment = Alignment.Center,
            ) {
                SoundscapePlaceholderIcon(
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(KuroSpacing.lg),
                )
            }
            if (sound.locked) {
                KuroBadge(
                    label = stringResource(R.string.sound_pill_premium),
                    fill = tokens.lockedFill,
                    textColor = tokens.lockedText,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KuroSpacing.xs),
                )
            }
        }
        Text(
            text = tileLabel,
            color = tokens.textPrimary,
            style = KuroType.labelMeta,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isPlaying) {
            SoundscapePlayingChip()
        } else {
            Spacer(Modifier.height(KuroSpacing.md + KuroSpacing.xs))
        }
    }
}

@Composable
private fun LofiVinylTile(
    track: MusicOption,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val vinylSize = KuroSpacing.soundscapeLofiVinyl
    val haloSize = vinylSize + 20.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        modifier = Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = when {
                    track.locked -> stringResource(R.string.sound_unlock_cd, track.name)
                    isPlaying -> stringResource(R.string.sound_stop_cd, track.name)
                    else -> stringResource(R.string.sound_play_cd, track.name)
                },
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier.size(if (isPlaying) haloSize else vinylSize),
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
                modifier = Modifier.size(vinylSize),
            )
            if (track.locked) {
                KuroBadge(
                    label = stringResource(R.string.sound_pill_premium),
                    fill = tokens.lockedFill,
                    textColor = tokens.lockedText,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KuroSpacing.xs),
                )
            }
        }
        Text(
            text = track.name,
            color = tokens.textPrimary,
            style = KuroType.labelMeta.copy(
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(KuroSpacing.soundscapeLofiVinyl + KuroSpacing.sm),
        )
    }
}

@Composable
private fun SoundscapeVolumeRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val percent = (value * 100f).toInt().coerceIn(0, 100)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            SoundscapePlaceholderIcon(
                tint = tokens.textSecondary,
                modifier = Modifier.size(KuroSpacing.lg),
            )
            Text(
                text = label,
                color = tokens.textPrimary,
                style = KuroType.labelStrong,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.soundscape_volume_percent, percent),
                color = tokens.textSecondary,
                style = KuroType.labelMeta,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Coral,
                activeTrackColor = Coral,
                inactiveTrackColor = Peach.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun MuteAllButton(onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .border(1.5.dp, Coral.copy(alpha = 0.65f), RoundedCornerShape(KuroSpacing.radiusPill))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = KuroSpacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoundscapePlaceholderIcon(
            tint = Coral,
            modifier = Modifier.size(KuroSpacing.lg),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = stringResource(R.string.soundscape_mute_all),
            color = Coral,
            style = KuroType.labelStrong,
        )
    }
}

@Composable
private fun ambienceTileFill(soundId: Int): Color {
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
private fun LofiVinylDisc(
    trackId: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clip(CircleShape), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2f
            drawCircle(color = VinylBlack, radius = radius, center = center)
            val grooveStroke = Stroke(width = 1.1f)
            listOf(0.40f, 0.50f, 0.60f, 0.70f, 0.80f, 0.90f).forEach { scale ->
                drawCircle(
                    color = VinylGroove,
                    radius = radius * scale,
                    center = center,
                    style = grooveStroke,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(KuroSpacing.soundscapeLofiLabel)
                .clip(CircleShape)
                .background(lofiVinylFill(trackId)),
            contentAlignment = Alignment.Center,
        ) {
            LofiVinylCenterIcon(trackId = trackId)
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(CardWhite)
                .border(0.75.dp, VinylGroove, CircleShape),
        )
    }
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

@Composable
private fun LofiVinylCenterIcon(trackId: Int) {
    Icon(
        painter = painterResource(lofiIconRes(trackId)),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(KuroSpacing.lg + KuroSpacing.xs),
    )
}

@DrawableRes
private fun lofiIconRes(musicId: Int): Int = when (musicId) {
    1 -> R.drawable.soundscape_lofi_cat
    2 -> R.drawable.soundscape_lofi_jazz_cat
    3 -> R.drawable.soundscape_lofi_piano
    4 -> R.drawable.soundscape_lofi_palm
    else -> R.drawable.soundscape_lofi_cat
}

@Composable
private fun lofiVinylFill(musicId: Int): Color {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return when (musicId) {
        1 -> if (isLight) LofiStudyLight else LofiStudyDark
        2 -> if (isLight) LofiJazzLight else LofiJazzDark
        3 -> if (isLight) LofiPianoLight else LofiPianoDark
        4 -> if (isLight) LofiChillLight else LofiChillDark
        else -> MaterialTheme.colorScheme.kuroTokens.cardFill
    }
}
