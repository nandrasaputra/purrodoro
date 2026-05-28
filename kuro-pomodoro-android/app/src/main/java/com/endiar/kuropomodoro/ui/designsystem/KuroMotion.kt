package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.min

/**
 * Shared interaction-motion helpers for Kuro's calm surfaces.
 *
 * The app already nails its "moment" motion (focus transition, completion
 * dialog, sheets); these helpers cover the everyday touches — press feedback
 * and a reusable check-draw — so taps feel alive without breaking the
 * meditative tone.
 */

/**
 * Press-feedback scale. Combine with a `clickable`/`selectable` that shares the
 * same [interactionSource]: the element squashes to [pressedScale] on finger-
 * down and springs back on release with a light playful settle.
 *
 * Personality: Playful-light — fast (StiffnessMedium) with a touch of overshoot
 * (MediumBouncy) so the calm surfaces feel responsive, not bouncy-cartoonish.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    return this.scale(scale)
}

/**
 * Wraps a list item in a one-shot entrance: it fades in and rises a few dp,
 * delayed by [index] so a freshly-shown list cascades into place.
 *
 * The per-item delay is capped so the whole cascade always lands under ~360ms
 * (the skill's <500ms stagger budget), no matter how long the list is.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(min(index, 8) * 40L)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "staggerEntrance",
    )
    val risePx = with(density) { 12.dp.toPx() }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * risePx
        },
    ) {
        content()
    }
}

/**
 * A two-segment checkmark that draws along its own path as [progress] goes
 * 0 → 1. Shared by the cycle-complete dialog and the task checkbox so the
 * "done" gesture reads identically across the app.
 */
@Composable
fun KuroAnimatedCheck(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p0 = Offset(0.22f * w, 0.52f * h)
        val p1 = Offset(0.44f * w, 0.72f * h)
        val p2 = Offset(0.78f * w, 0.32f * h)
        val len1 = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
        val len2 = hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat()
        val totalLen = len1 + len2
        val drawn = progress.coerceIn(0f, 1f) * totalLen
        val stroke = 0.14f * minOf(w, h)
        if (drawn <= len1) {
            val t = if (len1 == 0f) 0f else drawn / len1
            val end = Offset(
                x = p0.x + (p1.x - p0.x) * t,
                y = p0.y + (p1.y - p0.y) * t,
            )
            drawLine(color, p0, end, strokeWidth = stroke, cap = StrokeCap.Round)
        } else {
            drawLine(color, p0, p1, strokeWidth = stroke, cap = StrokeCap.Round)
            val t2 = if (len2 == 0f) 0f else (drawn - len1) / len2
            val end2 = Offset(
                x = p1.x + (p2.x - p1.x) * t2,
                y = p1.y + (p2.y - p1.y) * t2,
            )
            drawLine(color, p1, end2, strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}
