package com.endiar.kuropomodoro.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

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

    const val ScrimAlphaFocus = 0.45f

    /** Material 3 emphasized-decelerate — for entering content. */
    val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Material 3 emphasized-accelerate — for exiting content. */
    val ExitEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}
