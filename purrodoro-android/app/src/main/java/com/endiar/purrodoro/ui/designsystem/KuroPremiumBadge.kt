package com.endiar.purrodoro.ui.designsystem

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottom
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottomDark
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientTop
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientTopDark
import com.endiar.purrodoro.ui.theme.PremiumBadgeText
import com.endiar.purrodoro.ui.theme.isDarkColorScheme

/** Display size of [R.drawable.premium_star_sparkle] inside the pill badge. */
private val PremiumStarSize = 9.dp

/** Star size inside the compact circular badge. */
private val PremiumCompactStarSize = 10.dp

private val BadgeShape = RoundedCornerShape(KuroSpacing.radiusSm)

/** Fits the top corner of 84dp ambience tiles without clipping. */
private val CompactBadgeSize = 20.dp

/** Ambient shimmer sweep — Premium personality, ~2.4s loop. */
private const val ShimmerDurationMs = 2400

/** Secondary star twinkle — offset phase from shimmer. */
private const val StarTwinkleDurationMs = 1600

internal data class PremiumBadgeMotion(
    val shimmerProgress: Float,
    val starScale: Float,
    val starAlpha: Float,
    val reduceMotion: Boolean,
)

/**
 * Purrodoro **Starry Pill** premium badge (Design A).
 *
 * Purple gradient pill + golden star sparkle + "Premium" label. A soft shimmer
 * sweep and star twinkle signal premium without stealing focus from the tile.
 * See [docs/premium-badge.md](premium-badge.md).
 */
@Composable
fun KuroPremiumBadge(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.sound_pill_premium),
) {
    val badgeCd = stringResource(R.string.premium_badge_cd, label)
    val motion = rememberPremiumBadgeMotion()

    PremiumBadgeChrome(
        modifier = modifier,
        shape = BadgeShape,
        contentDescription = badgeCd,
        motion = motion,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PremiumBadgeStar(
                size = PremiumStarSize,
                motion = motion,
            )
            Text(
                text = label,
                style = KuroType.labelMeta.copy(fontWeight = FontWeight.SemiBold),
                color = PremiumBadgeText,
                maxLines = 1,
            )
        }
    }
}

/**
 * **Starry Dot** — icon-only premium badge for tight tile corners (e.g. ambience).
 * Same purple gradient + shimmer as [KuroPremiumBadge], without the label.
 */
@Composable
fun KuroPremiumBadgeCompact(
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.sound_pill_premium)
    val badgeCd = stringResource(R.string.premium_badge_cd, label)
    val motion = rememberPremiumBadgeMotion()

    PremiumBadgeChrome(
        modifier = modifier.size(CompactBadgeSize),
        shape = CircleShape,
        contentDescription = badgeCd,
        motion = motion,
        contentAlignment = Alignment.Center,
    ) {
        PremiumBadgeStar(
            size = PremiumCompactStarSize,
            motion = motion,
        )
    }
}

@Composable
private fun PremiumBadgeChrome(
    modifier: Modifier,
    shape: Shape,
    contentDescription: String,
    motion: PremiumBadgeMotion,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    PremiumShimmerSurface(
        modifier = modifier,
        shape = shape,
        contentDescription = contentDescription,
        motion = motion,
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * Purple gradient surface with badge shimmer + optional star twinkle motion.
 * Shared by [KuroPremiumBadge] and profile/discovery premium CTAs.
 */
@Composable
internal fun PremiumShimmerSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    motion: PremiumBadgeMotion = rememberPremiumBadgeMotion(),
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = isDarkColorScheme()
    val gradient = Brush.verticalGradient(
        colors = listOf(
            if (dark) PremiumBadgeGradientTopDark else PremiumBadgeGradientTop,
            if (dark) PremiumBadgeGradientBottomDark else PremiumBadgeGradientBottom,
        ),
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = shape,
                clip = false,
                ambientColor = PremiumBadgeGradientBottom.copy(alpha = 0.35f),
                spotColor = PremiumBadgeGradientBottom.copy(alpha = 0.45f),
            )
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        contentAlignment = contentAlignment,
    ) {
        Box(modifier = Modifier.matchParentSize().background(gradient))
        content()
        if (!motion.reduceMotion) {
            PremiumBadgeShimmerOverlay(
                progress = motion.shimmerProgress,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
internal fun PremiumBadgeStar(
    size: Dp,
    motion: PremiumBadgeMotion,
) {
    Image(
        painter = painterResource(id = R.drawable.premium_star_sparkle),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                if (!motion.reduceMotion) {
                    scaleX = motion.starScale
                    scaleY = motion.starScale
                    alpha = motion.starAlpha
                }
            },
    )
}

@Composable
internal fun rememberPremiumBadgeMotion(): PremiumBadgeMotion {
    val reduceMotion = rememberReduceMotionEnabled()
    val shimmerTransition = rememberInfiniteTransition(label = "premiumBadgeShimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShimmerDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val starScale by shimmerTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = StarTwinkleDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "starScale",
    )
    val starAlpha by shimmerTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = StarTwinkleDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "starAlpha",
    )
    return PremiumBadgeMotion(
        shimmerProgress = shimmerProgress,
        starScale = starScale,
        starAlpha = starAlpha,
        reduceMotion = reduceMotion,
    )
}

/** Horizontal highlight sweep — ambient shine layer. */
@Composable
private fun PremiumBadgeShimmerOverlay(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val bandWidth = size.width * 0.55f
        val centerX = -bandWidth * 0.5f + progress * (size.width + bandWidth)
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.35f to Color.White.copy(alpha = 0.05f),
                    0.5f to Color.White.copy(alpha = 0.32f),
                    0.65f to Color.White.copy(alpha = 0.05f),
                    1f to Color.Transparent,
                ),
                start = Offset(centerX - bandWidth / 2f, 0f),
                end = Offset(centerX + bandWidth / 2f, 0f),
            ),
            size = size,
        )
    }
}

@Composable
private fun rememberReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f) == 0f
    }
}
