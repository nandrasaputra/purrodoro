package com.endiar.purrodoro.ui.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/** Motion tokens for [KuroAdaptiveBottomSheet]. */
object KuroSheetMotion {
    const val EnterDurationMs = 360
    const val ExitDurationMs = 260
    const val ContentSwapDurationMs = 200

    /** Emphasized decelerate — sheet slides up into place. */
    val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Emphasized accelerate — sheet slides down on dismiss. */
    val ExitEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}
