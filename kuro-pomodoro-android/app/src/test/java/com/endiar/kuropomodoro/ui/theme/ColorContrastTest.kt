package com.endiar.kuropomodoro.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {

    // ── WCAG math ──────────────────────────────────────────────────────────────

    private fun linearize(c: Float): Double =
        if (c <= 0.04045f) c / 12.92 else ((c + 0.055f) / 1.055f).toDouble().pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * linearize(color.red) +
        0.7152 * linearize(color.green) +
        0.0722 * linearize(color.blue)

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    // ── Failing tests (RED) — fix these by updating Color.kt ──────────────────

    @Test
    fun `NightFaint on NightDeep meets WCAG AA 4-5 to 1`() {
        // Current NightFaint = #7A7388 → 4.37:1 — FAILS
        // After fix: #7D768B → 4.56:1 — PASSES
        assertTrue(
            "NightFaint on NightDeep: ${contrastRatio(NightFaint, NightDeep)}",
            contrastRatio(NightFaint, NightDeep) >= 4.5
        )
    }

    @Test
    fun `light textMuted CocoaFaint on Cream meets WCAG AA 4-5 to 1`() {
        // Current CocoaFaint = #A89486 on Cream → 2.75:1 — FAILS
        // After fix: textMuted switches to CocoaSoft (#8B6B5A) → 4.57:1 — PASSES
        // This test documents the requirement; update the assertion color to CocoaSoft
        // after Task 3 promotes CocoaSoft to textMuted in LightTokens.
        assertTrue(
            "CocoaFaint on Cream: ${contrastRatio(CocoaFaint, Cream)}",
            contrastRatio(CocoaFaint, Cream) >= 4.5
        )
    }

    // ── Regression tests (stay GREEN) ─────────────────────────────────────────

    @Test
    fun `NightMuted on NightDeep stays above 9 to 1`() {
        assertTrue(contrastRatio(NightMuted, NightDeep) >= 9.0)
    }

    @Test
    fun `Cocoa on Cream stays above 8 to 1`() {
        assertTrue(contrastRatio(Cocoa, Cream) >= 8.0)
    }
}
