package com.endiar.kuropomodoro.timer

import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.buildCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TimerReconcilerTest {

    private val noAutoSettings = TimerSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        autoStartNextSession = false,
        focusSessionsPerCycle = 2
    )
    private val autoSettings = noAutoSettings.copy(autoStartNextSession = true)

    private fun playingSnapshot(
        startMillis: Long,
        settings: TimerSettings,
        atIndex: Int = 0
    ): TimerRuntimeState {
        val items = buildCycle(settings)
        val seconds = items[atIndex].type.let {
            when (it) {
                is SessionItemType.Focus -> it.seconds
                is SessionItemType.ShortBreak -> it.seconds
                is SessionItemType.LongBreak -> it.seconds
            }
        }
        return TimerRuntimeState(
            session = TimerSession(
                items = items,
                currentIndex = atIndex,
                remainingSeconds = seconds,
                timerState = TimerState.Playing
            ),
            endsAtMillis = startMillis + seconds * 1000L,
            activeTask = null,
            openRecord = SessionRecord(
                id = startMillis,
                startedAt = startMillis,
                endedAt = 0L,
                focusMinutes = settings.focusMinutes,
                shortBreakMinutes = settings.shortBreakMinutes,
                longBreakMinutes = settings.longBreakMinutes,
                completedFocusCount = 0,
                totalFocusCount = items.count { it.type is SessionItemType.Focus },
                status = SessionStatus.Partial,
                taskName = null
            )
        )
    }

    private fun zonedDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    @Test
    fun mid_segment_no_advance_returns_state_unchanged() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, noAutoSettings)
        val now = startMillis + 5 * 60 * 1000L
        val result = TimerReconciler.reconcile(snapshot, now, noAutoSettings)

        assertEquals(snapshot.session.currentIndex, result.state.session.currentIndex)
        assertEquals(snapshot.endsAtMillis, result.state.endsAtMillis)
        assertEquals(TimerState.Playing, result.state.session.timerState)
        assertTrue(result.stampsToIncrement.isEmpty())
        assertTrue(result.closedRecords.isEmpty())
    }

    @Test
    fun after_one_focus_ends_no_autoStart_lands_paused_on_next_segment() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, noAutoSettings)
        val now = startMillis + 26 * 60 * 1000L

        val result = TimerReconciler.reconcile(snapshot, now, noAutoSettings)

        assertEquals(1, result.state.session.currentIndex)
        assertEquals(TimerState.Paused, result.state.session.timerState)
        assertNull(result.state.endsAtMillis)
        assertEquals(listOf(zonedDate(snapshot.endsAtMillis!!)), result.stampsToIncrement)
        assertTrue(result.closedRecords.isEmpty())
        assertEquals(1, result.state.openRecord?.completedFocusCount)
    }

    @Test
    fun after_one_focus_ends_with_autoStart_continues_into_break() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, autoSettings)
        val now = startMillis + 26 * 60 * 1000L

        val result = TimerReconciler.reconcile(snapshot, now, autoSettings)

        assertEquals(1, result.state.session.currentIndex)
        assertEquals(TimerState.Playing, result.state.session.timerState)
        assertNotNull(result.state.endsAtMillis)
        assertEquals(startMillis + 30 * 60 * 1000L, result.state.endsAtMillis)
    }

    @Test
    fun after_multiple_segments_with_autoStart_advances_through_chain() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, autoSettings)
        val now = startMillis + 32 * 60 * 1000L

        val result = TimerReconciler.reconcile(snapshot, now, autoSettings)

        assertEquals(2, result.state.session.currentIndex)
        assertEquals(TimerState.Playing, result.state.session.timerState)
        assertEquals(startMillis + 55 * 60 * 1000L, result.state.endsAtMillis)
        assertEquals(1, result.stampsToIncrement.size)
        assertEquals(1, result.state.openRecord?.completedFocusCount)
    }

    @Test
    fun after_full_cycle_ends_returns_idle_and_closes_record() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, autoSettings)
        val now = startMillis + 80 * 60 * 1000L

        val result = TimerReconciler.reconcile(snapshot, now, autoSettings)

        assertEquals(TimerState.Idle, result.state.session.timerState)
        assertNull(result.state.endsAtMillis)
        assertNull(result.state.openRecord)
        assertEquals(2, result.stampsToIncrement.size)
        assertEquals(1, result.closedRecords.size)
        assertEquals(SessionStatus.Completed, result.closedRecords[0].status)
        assertEquals(2, result.closedRecords[0].completedFocusCount)
        // Verify endedAt is the cycle's actual end moment (not `now`):
        //   startMillis + (25 + 5 + 25 + 15) * 60_000 = startMillis + 70 min in millis
        assertEquals(startMillis + 70 * 60 * 1000L, result.closedRecords[0].endedAt)
    }

    @Test
    fun paused_snapshot_is_returned_unchanged() {
        val snapshot = playingSnapshot(0L, noAutoSettings).let {
            it.copy(
                session = it.session.copy(timerState = TimerState.Paused),
                endsAtMillis = null
            )
        }
        val result = TimerReconciler.reconcile(snapshot, now = 999_999_999L, noAutoSettings)
        assertEquals(snapshot, result.state)
        assertTrue(result.stampsToIncrement.isEmpty())
        assertTrue(result.closedRecords.isEmpty())
    }

    @Test
    fun exactly_at_endsAt_treats_segment_as_completed() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, noAutoSettings)
        val now = snapshot.endsAtMillis!! // exactly at the boundary

        val result = TimerReconciler.reconcile(snapshot, now, noAutoSettings)

        assertEquals(1, result.state.session.currentIndex)
        assertEquals(TimerState.Paused, result.state.session.timerState)
        assertEquals(1, result.stampsToIncrement.size)
    }

    @Test
    fun mid_cycle_restore_from_break_advances_correctly() {
        // Snapshot was at second Focus (index 2) when last running, with auto-advance on.
        val startMillis = 1_700_000_000_000L
        val items = buildCycle(autoSettings)
        val secondFocusSeconds = items[2].type.let {
            when (it) {
                is SessionItemType.Focus -> it.seconds
                is SessionItemType.ShortBreak -> it.seconds
                is SessionItemType.LongBreak -> it.seconds
            }
        }
        val snapshot = TimerRuntimeState(
            session = TimerSession(
                items = items,
                currentIndex = 2, // second Focus
                remainingSeconds = secondFocusSeconds,
                timerState = TimerState.Playing
            ),
            endsAtMillis = startMillis + secondFocusSeconds * 1000L,
            activeTask = null,
            openRecord = null
        )
        // Now is 1 minute into the LongBreak segment (Focus#2 done, plus 1 of 15 LongBreak minutes)
        val now = startMillis + (25 + 1) * 60 * 1000L

        val result = TimerReconciler.reconcile(snapshot, now, autoSettings)

        assertEquals(3, result.state.session.currentIndex) // LongBreak
        assertEquals(TimerState.Playing, result.state.session.timerState)
        assertEquals(1, result.stampsToIncrement.size) // the second Focus that just completed
    }

    @Test
    fun playing_with_null_endsAtMillis_is_treated_as_unchanged() {
        // Malformed snapshot — invariant says Playing implies endsAtMillis != null.
        // Reconciler should defensively return the snapshot unchanged rather than NPE.
        val malformed = playingSnapshot(0L, noAutoSettings).copy(endsAtMillis = null)
        val result = TimerReconciler.reconcile(malformed, now = 999_999_999_999L, noAutoSettings)
        assertEquals(malformed, result.state)
        assertTrue(result.stampsToIncrement.isEmpty())
        assertTrue(result.closedRecords.isEmpty())
    }
}
