package com.endiar.kuropomodoro.data

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppRepositoryRuntimeSnapshotTest {

    @Test
    fun encode_then_decode_roundTrips_a_running_state() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(
                    TimerSessionItem(SessionItemType.Focus(1500)),
                    TimerSessionItem(SessionItemType.ShortBreak(300)),
                    TimerSessionItem(SessionItemType.Focus(1500)),
                    TimerSessionItem(SessionItemType.LongBreak(900))
                ),
                currentIndex = 2,
                remainingSeconds = 1490,
                timerState = TimerState.Playing
            ),
            endsAtMillis = 1_700_000_000_000L,
            activeTask = FocusTask(id = 42, title = "Sketch with cat", minutes = 25),
            openRecord = SessionRecord(
                id = 1_700_000_000_000L,
                startedAt = 1_699_999_000_000L,
                endedAt = 0L,
                focusMinutes = 25,
                shortBreakMinutes = 5,
                longBreakMinutes = 15,
                completedFocusCount = 1,
                totalFocusCount = 2,
                status = SessionStatus.Partial,
                taskName = "Sketch with cat"
            )
        )
        val encoded = encodeRuntimeSnapshot(state)
        val decoded = decodeRuntimeSnapshot(encoded)
        assertEquals(state, decoded)
    }

    @Test
    fun encode_then_decode_roundTrips_a_paused_state_without_endsAtMillis() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(
                    TimerSessionItem(SessionItemType.Focus(1500)),
                    TimerSessionItem(SessionItemType.LongBreak(900))
                ),
                currentIndex = 0,
                remainingSeconds = 600,
                timerState = TimerState.Paused
            ),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null
        )
        val encoded = encodeRuntimeSnapshot(state)
        val decoded = decodeRuntimeSnapshot(encoded)
        assertEquals(state, decoded)
    }

    @Test
    fun decode_blank_returns_null() {
        assertNull(decodeRuntimeSnapshot(""))
    }

    @Test
    fun decode_garbage_returns_null() {
        assertNull(decodeRuntimeSnapshot("this is not a valid snapshot"))
    }
}
