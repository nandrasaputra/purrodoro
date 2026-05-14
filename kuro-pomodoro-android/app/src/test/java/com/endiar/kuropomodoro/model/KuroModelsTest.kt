package com.endiar.kuropomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class KuroModelsTest {
    @Test
    fun timerModeFormatsRemainingTime() {
        assertEquals("25:00", TimerMode.Focus.formatDuration())
        assertEquals("05:00", TimerMode.ShortBreak.formatDuration())
        assertEquals("15:00", TimerMode.LongBreak.formatDuration())
        assertEquals("01:05", formatSeconds(65))
    }

    @Test
    fun taskToggleChangesCompletionState() {
        val task = FocusTask(id = 1, title = "Read Kotlin docs", minutes = 25)

        assertFalse(task.completed)
        assertTrue(task.toggleCompleted().completed)
    }

    @Test
    fun timerSettingsUpdateClampsFocusDuration() {
        assertEquals(180, TimerSettings().update(TimerMode.Focus, 500).focusMinutes)
        assertEquals(1, TimerSettings().update(TimerMode.Focus, 0).focusMinutes)
    }

    @Test
    fun adaptiveGlassReadabilityUsesReadableTreatmentForBrightBackgrounds() {
        assertEquals(
            GlassTreatment.Readable,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(6, 0))
        )
        assertEquals(
            GlassTreatment.Readable,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(9, 0))
        )
        assertEquals(
            GlassTreatment.Readable,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(12, 0))
        )
    }

    @Test
    fun adaptiveGlassReadabilityUsesClearTreatmentForDarkBackgrounds() {
        assertEquals(
            GlassTreatment.Clear,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(17, 0))
        )
        assertEquals(
            GlassTreatment.Clear,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(20, 0))
        )
        assertEquals(
            GlassTreatment.Clear,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(23, 0))
        )
    }

    @Test
    fun explicitGlassReadabilityModesIgnoreBackgroundBrightness() {
        assertEquals(
            GlassTreatment.Clear,
            glassTreatmentFor(GlassReadabilityMode.Clear, LocalTime.of(12, 0))
        )
        assertEquals(
            GlassTreatment.Readable,
            glassTreatmentFor(GlassReadabilityMode.Readable, LocalTime.of(23, 0))
        )
    }

    @Test
    fun adaptiveGlassReadabilityBoundaryHours() {
        assertEquals(
            GlassTreatment.Clear,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(4, 0))
        )
        assertEquals(
            GlassTreatment.Readable,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(5, 0))
        )
        assertEquals(
            GlassTreatment.Readable,
            glassTreatmentFor(GlassReadabilityMode.Adaptive, LocalTime.of(16, 0))
        )
    }
}
