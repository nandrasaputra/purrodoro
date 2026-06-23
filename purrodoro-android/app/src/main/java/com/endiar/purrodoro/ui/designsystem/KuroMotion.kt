package com.endiar.purrodoro.ui.designsystem

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.math.min

/**
 * Shared interaction-motion helpers for Purrodoro's calm surfaces.
 *
 * The app already nails its "moment" motion (focus transition, completion
 * dialog, sheets); these helpers cover the everyday touches — press feedback
 * and a reusable check-draw — so taps feel alive without breaking the
 * meditative tone.
 */

/** Pause held at each end before reversing — equal on both sides. */
private const val MarqueePauseMs = 1_400L
/** Constant scroll speed in dp/s — same regardless of text length. */
private val MarqueeScrollVelocity = 10.dp

/**
 * Single-line label that auto scrolls when it overflows its bounds.
 *
 * Ping-pong motion (start → end → start) like a music-player title — no user
 * scrolling. Calm pacing: pause at each end, linear drift between. Falls back
 * to ellipsis when motion is reduced or the text fits.
 */
@Composable
fun KuroMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = KuroType.labelStrong,
    color: Color = Color.Unspecified,
) {
    val reduceMotion = rememberReduceMotionEnabled()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textWidthPx = remember(text, style, density) {
        textMeasurer
            .measure(
                text = AnnotatedString(text),
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = Int.MAX_VALUE),
            )
            .size
            .width
            .toFloat()
    }

    // clipToBounds() on the outer box is the single clip boundary.
    // The Text must measure at its *natural* full width (wrapContentWidth unbounded),
    // otherwise BoxWithConstraints passes its capped maxWidth to children and the
    // Text layout rect = container width — translationX then slides a pre-clipped stub.
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val overflowPx = (textWidthPx - containerWidthPx).coerceAtLeast(0f)
        val shouldMarquee = !reduceMotion && overflowPx > 1f && text.isNotBlank()
        val offsetX = remember { Animatable(0f) }

        LaunchedEffect(text, shouldMarquee, overflowPx) {
            offsetX.snapTo(0f)
            if (!shouldMarquee) return@LaunchedEffect
            // Duration derived purely from velocity × distance — same dp/s regardless of length.
            // No coerceIn clamp so the pace never warps to fit an arbitrary time budget.
            val scrollDurationMs = with(density) {
                (overflowPx / MarqueeScrollVelocity.toPx() * 1_000f).toInt()
                    .coerceAtLeast(600) // floor only: avoids near-zero flicker on tiny overflow
            }
            while (isActive) {
                delay(MarqueePauseMs)           // pause at start before scrolling forward
                offsetX.animateTo(
                    targetValue = -overflowPx,
                    animationSpec = tween(durationMillis = scrollDurationMs, easing = LinearEasing),
                )
                delay(MarqueePauseMs)           // equal pause at end before scrolling back
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = scrollDurationMs, easing = LinearEasing),
                )
            }
        }

        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            // Always Clip so the natural-width text doesn't draw outside the parent bounds.
            overflow = TextOverflow.Clip,
            modifier = Modifier
                // unbounded = true: bypasses BoxWithConstraints' maxWidth so Text
                // measures at its natural full width, not the container width.
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = offsetX.value },
        )
    }
}

@Composable
internal fun rememberReduceMotionEnabled(): Boolean {
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
