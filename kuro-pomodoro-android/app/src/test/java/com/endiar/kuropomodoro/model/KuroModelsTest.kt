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
    fun cycleDotsStateShowsAllDotsInIdleStateUsingSettings() {
        val settings = TimerSettings(focusSessionsPerCycle = 4)
        val state = cycleDotsState(
            cycleItems = emptyList(),
            cycleIndex = 0,
            timerState = TimerState.Idle,
            settings = settings
        )
        assertEquals(4, state.totalFocusSessions)
        assertEquals(0, state.completedFocusSessions)
        assertEquals(false, state.isCurrentlyFocus)
    }

    @Test
    fun cycleDotsStateShowsProgressDuringActiveSession() {
        val settings = TimerSettings(focusSessionsPerCycle = 4)
        val items = buildCycle(settings)
        // cycleIndex=2 means we are on the 2nd focus session (index 0=Focus, 1=ShortBreak, 2=Focus)
        val state = cycleDotsState(
            cycleItems = items,
            cycleIndex = 2,
            timerState = TimerState.Playing,
            settings = settings
        )
        assertEquals(4, state.totalFocusSessions)
        assertEquals(1, state.completedFocusSessions) // index 0 (Focus) is behind us
        assertEquals(true, state.isCurrentlyFocus)
    }

    @Test
    fun cycleDotsStateIdleAfterResetDoesNotHighlightFirstFocus() {
        // Repro: after natural finish or skip-final, items are preserved and currentIndex
        // is reset to 0 with timerState=Idle. The first dot must NOT be drawn as the
        // "currently focus" highlight — all dots should appear as outline.
        val settings = TimerSettings(focusSessionsPerCycle = 4)
        val items = buildCycle(settings)
        val state = cycleDotsState(
            cycleItems = items,
            cycleIndex = 0,
            timerState = TimerState.Idle,
            settings = settings
        )
        assertEquals(4, state.totalFocusSessions)
        assertEquals(0, state.completedFocusSessions)
        assertEquals(false, state.isCurrentlyFocus)
    }

    @Test
    fun cycleDotsStateWhilePausedKeepsCurrentFocusHighlight() {
        val settings = TimerSettings(focusSessionsPerCycle = 4)
        val items = buildCycle(settings)
        val state = cycleDotsState(
            cycleItems = items,
            cycleIndex = 0,
            timerState = TimerState.Paused,
            settings = settings
        )
        assertEquals(true, state.isCurrentlyFocus)
    }

    @Test
    fun stopShouldStampWhenMidFocusWithRemainingTime() {
        val settings = TimerSettings()
        val items = buildCycle(settings)
        val session = TimerSession(
            items = items,
            currentIndex = 0, // Focus
            remainingSeconds = 100,
            timerState = TimerState.Playing
        )
        assertTrue(shouldStampStopAsFocus(session))
    }

    @Test
    fun stopShouldStampWhenMidFocusEvenIfPaused() {
        val settings = TimerSettings()
        val items = buildCycle(settings)
        val session = TimerSession(
            items = items,
            currentIndex = 0,
            remainingSeconds = 60,
            timerState = TimerState.Paused
        )
        assertTrue(shouldStampStopAsFocus(session))
    }

    @Test
    fun stopShouldNotStampWhenIdle() {
        val settings = TimerSettings()
        val items = buildCycle(settings)
        val session = TimerSession(
            items = items,
            currentIndex = 0,
            remainingSeconds = items.first().type.seconds(),
            timerState = TimerState.Idle
        )
        assertFalse(shouldStampStopAsFocus(session))
    }

    @Test
    fun stopShouldNotStampWhenOnBreak() {
        val settings = TimerSettings(focusSessionsPerCycle = 4)
        val items = buildCycle(settings)
        // index 1 = ShortBreak after first Focus
        val session = TimerSession(
            items = items,
            currentIndex = 1,
            remainingSeconds = 60,
            timerState = TimerState.Playing
        )
        assertFalse(shouldStampStopAsFocus(session))
    }

    @Test
    fun stopShouldNotStampWhenFocusHasNoRemainingTime() {
        // Edge case: remainingSeconds==0 means the segment is effectively complete;
        // a normal completion path will stamp via TimerStateMachine. Don't double-stamp.
        val settings = TimerSettings()
        val items = buildCycle(settings)
        val session = TimerSession(
            items = items,
            currentIndex = 0,
            remainingSeconds = 0,
            timerState = TimerState.Playing
        )
        assertFalse(shouldStampStopAsFocus(session))
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
