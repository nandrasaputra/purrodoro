package com.endiar.purrodoro.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import com.endiar.purrodoro.ui.tutorial.TooltipPlacement
import kotlin.math.roundToInt

@Composable
fun SpotlightTutorialOverlay(
    targetBounds: Rect,
    title: String,
    body: String,
    stepIndex: Int,
    totalSteps: Int,
    skipLabel: String,
    previousLabel: String?,
    nextLabel: String,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    placement: TooltipPlacement? = null,
    spotlightPadding: Dp = 12.dp,
    spotlightCornerRadius: Dp = 30.dp,
) {
    val density = LocalDensity.current
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    var overlayOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var overlayOriginReady by remember { mutableStateOf(false) }

    BackHandler(onBack = onSkip)

    val breathingTransition = rememberInfiniteTransition(label = "spotlightBreathing")
    val breath by breathingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1600,
                easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "spotlightBreath",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayOriginInRoot = coordinates.positionInRoot()
                overlayOriginReady = true
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            )
    ) {
        if (!overlayOriginReady) return@BoxWithConstraints

        val marginPx = with(density) { KuroSpacing.lg.toPx() }
        val gapPx = with(density) { 18.dp.toPx() }
        val arrowHeightPx = with(density) { 14.dp.toPx() }
        val cardWidth = if (maxWidth < 344.dp) maxWidth - (KuroSpacing.lg * 2) else 304.dp
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val highlight = targetBounds
            .translate(-overlayOriginInRoot.x, -overlayOriginInRoot.y)
            .inflate(
            horizontal = with(density) { spotlightPadding.toPx() },
            vertical = with(density) { spotlightPadding.toPx() },
        )

        var cardHeightPx by remember { mutableFloatStateOf(0f) }
        val groupHeightPx = cardHeightPx + arrowHeightPx
        val placeBelow = when (placement) {
            TooltipPlacement.Below -> true
            TooltipPlacement.Above -> false
            null -> highlight.center.y < screenHeightPx * 0.56f
        }
        val xPx = (highlight.center.x - cardWidthPx / 2f)
            .coerceIn(marginPx, screenWidthPx - cardWidthPx - marginPx)
        val yPx = if (placeBelow) {
            (highlight.bottom + gapPx)
                .coerceAtMost(screenHeightPx - groupHeightPx - marginPx)
        } else {
            (highlight.top - groupHeightPx - gapPx)
                .coerceAtLeast(marginPx)
        }
        val arrowCenterPx = (highlight.center.x - xPx)
            .coerceIn(with(density) { 34.dp.toPx() }, cardWidthPx - with(density) { 34.dp.toPx() })

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            val corner = CornerRadius(spotlightCornerRadius.toPx(), spotlightCornerRadius.toPx())
            val glowExpansion = 7.dp.toPx() * breath
            val glowAlpha = 0.26f - (0.12f * breath)
            val glowStroke = 18.dp.toPx() + (8.dp.toPx() * breath)
            drawRect(Color.Black.copy(alpha = 0.56f))
            drawRoundRect(
                color = tokens.accent.copy(alpha = glowAlpha),
                topLeft = Offset(highlight.left - glowExpansion, highlight.top - glowExpansion),
                size = Size(
                    width = highlight.width + (glowExpansion * 2f),
                    height = highlight.height + (glowExpansion * 2f),
                ),
                cornerRadius = CornerRadius(
                    x = corner.x + glowExpansion,
                    y = corner.y + glowExpansion,
                ),
                style = Stroke(width = glowStroke, cap = StrokeCap.Round),
            )
            drawRoundRect(
                color = tokens.accent.copy(alpha = 0.18f),
                topLeft = Offset(highlight.left, highlight.top),
                size = Size(highlight.width, highlight.height),
                cornerRadius = corner,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
            )
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(highlight.left, highlight.top),
                size = Size(highlight.width, highlight.height),
                cornerRadius = corner,
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.26f),
                topLeft = Offset(highlight.left, highlight.top),
                size = Size(highlight.width, highlight.height),
                cornerRadius = corner,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                .width(cardWidth),
            horizontalAlignment = Alignment.Start,
        ) {
            if (placeBelow) {
                TutorialArrow(
                    pointingUp = true,
                    color = tokens.cardFill,
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (arrowCenterPx - with(density) { 12.dp.toPx() }).roundToInt(),
                            y = 0,
                        )
                    }
                )
            }
            TutorialCard(
                title = title,
                body = body,
                stepIndex = stepIndex,
                totalSteps = totalSteps,
                skipLabel = skipLabel,
                previousLabel = previousLabel,
                nextLabel = nextLabel,
                onSkip = onSkip,
                onPrevious = onPrevious,
                onNext = onNext,
                onMeasured = { cardHeightPx = it },
            )
            if (!placeBelow) {
                TutorialArrow(
                    pointingUp = false,
                    color = tokens.cardFill,
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (arrowCenterPx - with(density) { 12.dp.toPx() }).roundToInt(),
                            y = 0,
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TutorialCard(
    title: String,
    body: String,
    stepIndex: Int,
    totalSteps: Int,
    skipLabel: String,
    previousLabel: String?,
    nextLabel: String,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMeasured: (Float) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Box {
        Box(
            modifier = Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(KuroSpacing.radiusLg),
                    ambientColor = Color(0xFFD7BDAF).copy(alpha = 0.18f),
                    spotColor = Color(0xFF5F4638).copy(alpha = 0.12f),
                )
                .clip(RoundedCornerShape(KuroSpacing.radiusLg))
                .background(tokens.cardFill)
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
                .onGloballyPositioned { onMeasured(it.size.height.toFloat()) }
                .padding(horizontal = KuroSpacing.xl, vertical = KuroSpacing.lg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                Text(
                    text = title,
                    color = tokens.textPrimary,
                    style = KuroType.headingCard,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = body,
                    color = tokens.textSecondary,
                    style = KuroType.bodySecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                TutorialDots(
                    stepIndex = stepIndex,
                    totalSteps = totalSteps,
                )
                Row(
                    modifier = Modifier.padding(top = KuroSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = skipLabel,
                            color = tokens.textMuted,
                            style = KuroType.labelStrong,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (previousLabel != null) {
                        TextButton(onClick = onPrevious) {
                            Text(
                                text = previousLabel,
                                color = tokens.textPrimary,
                                style = KuroType.labelStrong,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                            .background(tokens.interactiveFill)
                            .clickable(onClick = onNext)
                            .padding(horizontal = KuroSpacing.lg, vertical = 11.dp)
                    ) {
                        Text(
                            text = nextLabel,
                            color = tokens.interactiveText,
                            style = KuroType.labelStrong,
                        )
                    }
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.tutorial_kuro_peek),
            contentDescription = null,
            modifier = Modifier
                .offset(x = (-12).dp, y = (-18).dp)
                .size(width = 76.dp, height = 60.dp)
        )
    }
}

@Composable
private fun TutorialDots(stepIndex: Int, totalSteps: Int) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == stepIndex) 8.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == stepIndex) tokens.accent
                        else tokens.textMuted.copy(alpha = 0.26f)
                    )
            )
        }
    }
}

@Composable
private fun TutorialArrow(pointingUp: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 24.dp, height = 14.dp)) {
        val path = Path().apply {
            if (pointingUp) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}

private fun Rect.inflate(horizontal: Float, vertical: Float): Rect =
    Rect(left - horizontal, top - vertical, right + horizontal, bottom + vertical)
