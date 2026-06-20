package com.endiar.purrodoro.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.theme.PremiumAuroraGold
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottom
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottomDark
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientTop
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientTopDark
import com.endiar.purrodoro.ui.theme.isDarkColorScheme

private val AuroraRingWidth = 2.dp

private data class AuroraColors(
    val top: androidx.compose.ui.graphics.Color,
    val bottom: androidx.compose.ui.graphics.Color,
)

@Composable
private fun rememberAuroraColors(): AuroraColors {
    val dark = isDarkColorScheme()
    return AuroraColors(
        top = if (dark) PremiumBadgeGradientTopDark else PremiumBadgeGradientTop,
        bottom = if (dark) PremiumBadgeGradientBottomDark else PremiumBadgeGradientBottom,
    )
}

private fun auroraSweepBrush(center: Offset, colors: AuroraColors): Brush =
    Brush.sweepGradient(
        0f to colors.top,
        0.22f to PremiumAuroraGold,
        0.5f to colors.bottom,
        0.78f to PremiumAuroraGold,
        1f to colors.top,
        center = center,
    )

/**
 * Aurora outline for premium cards. Uses [drawBehind] so the sweep gradient
 * always has an explicit center (avoids Compose draw crashes).
 */
@Composable
fun Modifier.premiumAuroraOutline(
    shape: Shape = RoundedCornerShape(KuroSpacing.radiusLg),
    strokeWidth: Dp = 1.5.dp,
): Modifier {
    val colors = rememberAuroraColors()
    val corner = when (shape) {
        is RoundedCornerShape -> KuroSpacing.radiusLg
        else -> KuroSpacing.radiusLg
    }
    return drawBehind {
        val stroke = strokeWidth.toPx()
        val half = stroke / 2f
        val brush = auroraSweepBrush(
            center = Offset(size.width / 2f, size.height / 2f),
            colors = colors,
        )
        when (shape) {
            CircleShape -> drawCircle(
                brush = brush,
                radius = size.minDimension / 2f - half,
                style = Stroke(width = stroke),
            )
            else -> {
                val cornerPx = corner.toPx()
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(half, half),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}

/**
 * Aurora Ring frame for premium members — gradient stroke + golden star accent.
 */
@Composable
fun KuroPremiumAvatarFrame(
    size: Dp,
    modifier: Modifier = Modifier,
    showStarAccent: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = rememberAuroraColors()
    val ringInset = AuroraRingWidth + 1.dp
    val starSize = size * (14f / 52f)

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val brush = auroraSweepBrush(
                        center = Offset(this.size.width / 2f, this.size.height / 2f),
                        colors = colors,
                    )
                    drawCircle(
                        brush = brush,
                        radius = this.size.minDimension / 2f,
                    )
                }
                .padding(AuroraRingWidth),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
                content = content,
            )
        }
        if (showStarAccent) {
            Image(
                painter = painterResource(id = R.drawable.premium_star_sparkle),
                contentDescription = stringResource(R.string.premium_star_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(starSize)
                    .padding(end = ringInset * 0.15f, bottom = ringInset * 0.15f),
            )
        }
    }
}
