package com.endiar.purrodoro.ui.components

import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `dashboard scrim alpha is subtle and not opaque`() {
        assertEquals(0.1f, FocusModeMotion.ScrimAlphaDashboard, 0.0001f)
    }

    @Test
    fun `easings exist`() {
        assertNotNull(FocusModeMotion.EnterEasing)
        assertNotNull(FocusModeMotion.ExitEasing)
    }

    @Test
    fun `focus session in flight when runtime is playing`() {
        val runtime = runtimeSnapshot(timerState = TimerState.Playing)
        assertTrue(focusSessionInFlight(runtime, focusStartPending = false))
    }

    @Test
    fun `focus session in flight when runtime is paused between segments`() {
        val runtime = runtimeSnapshot(timerState = TimerState.Paused)
        assertTrue(focusSessionInFlight(runtime, focusStartPending = false))
    }

    @Test
    fun `focus session not in flight when runtime cleared after stop`() {
        assertFalse(focusSessionInFlight(null, focusStartPending = false))
    }

    @Test
    fun `focus session in flight while start is pending before runtime arrives`() {
        assertTrue(focusSessionInFlight(null, focusStartPending = true))
    }

    @Test
    fun `focus session not in flight on idle warm-reset after cycle finish`() {
        // After a natural cycle finish the service briefly publishes an Idle
        // snapshot with items still preserved at currentIndex 0. The overlay
        // must drop then so it doesn't linger under the completion dialog.
        val runtime = runtimeSnapshot(timerState = TimerState.Idle)
        assertFalse(focusSessionInFlight(runtime, focusStartPending = false))
    }

    @Test
    fun `focus mode auto exits when session ends externally`() {
        assertFalse(focusModeActive(isFocusMode = true, runtimeState = null, focusStartPending = false))
    }

    private fun runtimeSnapshot(timerState: TimerState): TimerRuntimeState {
        val items = listOf(
            TimerSessionItem(SessionItemType.Focus(25 * 60)),
            TimerSessionItem(SessionItemType.ShortBreak(5 * 60)),
        )
        return TimerRuntimeState(
            session = TimerSession(items, currentIndex = 0, remainingSeconds = 25 * 60, timerState = timerState),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null,
            focusType = FocusType.Empty,
            activeHabitId = null,
        )
    }
}
