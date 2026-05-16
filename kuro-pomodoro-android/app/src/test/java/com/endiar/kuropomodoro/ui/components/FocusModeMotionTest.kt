package com.endiar.kuropomodoro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusModeMotionTest {

    @Test
    fun `enter is longer than exit so arriving feels deliberate`() {
        assertTrue(
            "enter must be > exit",
            FocusModeMotion.EnterDurationMs > FocusModeMotion.ExitDurationMs
        )
    }

    @Test
    fun `durations are within calm-cinematic range`() {
        assertEquals(700, FocusModeMotion.EnterDurationMs)
        assertEquals(400, FocusModeMotion.ExitDurationMs)
    }

    @Test
    fun `breath pulse fires after enter completes`() {
        assertTrue(
            "breath must wait until after enter finishes",
            FocusModeMotion.BreathDelayMs >= FocusModeMotion.EnterDurationMs
        )
        assertEquals(1200, FocusModeMotion.BreathDurationMs)
        assertEquals(1.02f, FocusModeMotion.BreathScalePeak, 0.0001f)
    }

    @Test
    fun `scrim alpha is darker than mid-grey but not opaque`() {
        assertEquals(0.45f, FocusModeMotion.ScrimAlphaFocus, 0.0001f)
    }

    @Test
    fun `easings exist`() {
        assertNotNull(FocusModeMotion.EnterEasing)
        assertNotNull(FocusModeMotion.ExitEasing)
    }
}
