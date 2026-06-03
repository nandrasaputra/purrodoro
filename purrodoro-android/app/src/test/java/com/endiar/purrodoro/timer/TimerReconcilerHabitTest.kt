package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class TimerReconcilerHabitTest {

    private val utc = ZoneId.of("UTC")

    private val settings = TimerSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        autoStartNextSession = true,
        focusSessionsPerCycle = 2,
    )

    /** Snapshot with Focus[1500s] + ShortBreak[300s] already playing. */
    private fun playingFocusSnapshot(startMs: Long, habitId: Int?): TimerRuntimeState {
        val focusSec = 1500
        return TimerRuntimeState(
            session = TimerSession(
                items = listOf(
                    TimerSessionItem(SessionItemType.Focus(focusSec)),
                    TimerSessionItem(SessionItemType.ShortBreak(300)),
                ),
                currentIndex = 0,
                remainingSeconds = focusSec,
                timerState = TimerState.Playing,
            ),
            endsAtMillis = startMs + focusSec * 1000L,
            activeTask = null,
            openRecord = null,
            activeHabitId = habitId,
        )
    }

    @Test
    fun `replaying past one focus segment with a habit records that date`() {
        val focusSec = 1500
        val startMs = 0L
        val endsAt = startMs + focusSec * 1000L
        val snapshot = playingFocusSnapshot(startMs, habitId = 3)
        val now = endsAt + 60_000L  // 1 minute past the focus end

        val result = TimerReconciler.reconcile(snapshot, now, settings, zone = utc)

        assertEquals(1, result.habitProgressDates.size)
    }

    @Test
    fun `no activeHabitId means empty habitProgressDates`() {
        val focusSec = 1500
        val startMs = 0L
        val snapshot = playingFocusSnapshot(startMs, habitId = null)
        val now = startMs + focusSec * 1000L + 60_000L

        val result = TimerReconciler.reconcile(snapshot, now, settings, zone = utc)

        assertTrue(result.habitProgressDates.isEmpty())
    }

    @Test
    fun `replaying past two focus segments collects two dates`() {
        val focusSec = 1500
        val breakSec = 300
        val startMs = 0L
        // Two-focus cycle: F1 ends at 1500s, break ends at 1800s, F2 ends at 3300s
        val twoFocusSnapshot = TimerRuntimeState(
            session = TimerSession(
                items = listOf(
                    TimerSessionItem(SessionItemType.Focus(focusSec)),
                    TimerSessionItem(SessionItemType.ShortBreak(breakSec)),
                    TimerSessionItem(SessionItemType.Focus(focusSec)),
                ),
                currentIndex = 0,
                remainingSeconds = focusSec,
                timerState = TimerState.Playing,
            ),
            endsAtMillis = startMs + focusSec * 1000L,
            activeTask = null,
            openRecord = null,
            activeHabitId = 5,
        )
        val now = startMs + (focusSec + breakSec + focusSec) * 1000L + 60_000L

        val result = TimerReconciler.reconcile(twoFocusSnapshot, now, settings, zone = utc)

        assertEquals(2, result.habitProgressDates.size)
    }
}
