package com.endiar.purrodoro.ui.components

import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable

/**
 * Shared motion constants for the focus-mode enter/exit transition.
 *
 * Calm & cinematic: enter is decelerated and slightly longer (deliberate
 * arrival); exit is accelerated and shorter (responsive departure). A
 * one-shot breath pulse fires after enter completes to signal "you're in".
 *
 * See: docs/superpowers/specs/2026-05-16-focus-mode-transition-design.md
 */
object FocusModeMotion {
    const val EnterDurationMs = 700
    const val ExitDurationMs = 400

    const val BreathDelayMs = 800L
    const val BreathDurationMs = 1200
    const val BreathScalePeak = 1.02f

    /** Dim alpha applied on the dashboard; fades to 0 when entering focus mode. */
    const val ScrimAlphaDashboard = 0.1f

    /** Black overlay alpha when focus mode is fully active — dims the room. */
    const val ScrimAlphaFocus = 0.55f

    /** Material 3 emphasized-decelerate — for entering content. */
    val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Material 3 emphasized-accelerate — for exiting content. */
    val ExitEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /**
     * Standard transition spec for any property animated by the shared focus
     * transition: emphasized-decelerate when entering focus mode, emphasized-
     * accelerate when leaving. Use as `transitionSpec = FocusModeMotion.spec()`
     * for `animateFloat`, `animateInt`, etc.
     */
    fun <T> spec(): @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<T> = {
        if (targetState) {
            tween(durationMillis = EnterDurationMs, easing = EnterEasing)
        } else {
            tween(durationMillis = ExitDurationMs, easing = ExitEasing)
        }
    }
}

/**
 * Whether a Pomodoro cycle is still in flight on-device. Drives focus-mode
 * visibility: the overlay stays up while a session is running OR paused between
 * segments, or briefly while the service runtime hasn't arrived yet after
 * start.
 *
 * A cleared runtime (e.g. Stop from the notification) ends the session unless
 * [focusStartPending] is true for that short start window.
 *
 * An [TimerState.Idle] runtime is NOT in flight: the service briefly publishes
 * an idle warm-reset snapshot (currentIndex = 0, items preserved) after a
 * natural cycle finish or skip-to-end, and the overlay must drop then so it
 * doesn't linger under the cycle-complete dialog.
 */
fun focusSessionInFlight(
    runtimeState: TimerRuntimeState?,
    focusStartPending: Boolean,
): Boolean = when (val rt = runtimeState) {
    null -> focusStartPending
    else -> rt.session.timerState != TimerState.Idle
}

fun focusModeActive(
    isFocusMode: Boolean,
    runtimeState: TimerRuntimeState?,
    focusStartPending: Boolean,
): Boolean = isFocusMode && focusSessionInFlight(runtimeState, focusStartPending)
