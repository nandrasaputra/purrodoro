package com.endiar.kuropomodoro.data

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    // --- SessionRecord persistence ---

    @Test
    fun encode_then_decode_sessionRecords_preserves_break_counts() {
        val records = listOf(
            SessionRecord(
                id = 1L,
                startedAt = 1_000L,
                endedAt = 2_000L,
                focusMinutes = 25,
                shortBreakMinutes = 5,
                longBreakMinutes = 15,
                completedFocusCount = 3,
                totalFocusCount = 4,
                status = SessionStatus.Partial,
                taskName = "Write tests",
                completedShortBreakCount = 2,
                completedLongBreakCount = 0
            )
        )
        val decoded = decodeSessionRecords(encodeSessionRecords(records))
        assertEquals(records, decoded)
    }

    @Test
    fun decode_legacy_sessionRecord_without_break_counts_defaults_to_zero() {
        // v1 schema: 10 pipe-separated fields, no break counts.
        // Existing user records on disk must still decode without loss.
        val legacy = listOf(
            "1",            // id
            "1000",         // startedAt
            "2000",         // endedAt
            "25",           // focusMinutes
            "5",            // shortBreakMinutes
            "15",           // longBreakMinutes
            "3",            // completedFocusCount
            "4",            // totalFocusCount
            "Partial",      // status
            ""              // taskName (empty)
        ).joinToString("|")

        val decoded = decodeSessionRecords(legacy)
        assertEquals(1, decoded.size)
        assertEquals(3, decoded[0].completedFocusCount)
        assertEquals(0, decoded[0].completedShortBreakCount)
        assertEquals(0, decoded[0].completedLongBreakCount)
        assertNull(decoded[0].taskName)
    }

    @Test
    fun decode_legacy_runtimeSnapshot_openRecord_defaults_break_counts_to_zero() {
        // A v1 runtime snapshot — openRecord field has 10 semicolon-separated entries.
        val legacyOpenRecord = listOf(
            "1",                // id
            "1000",             // startedAt
            "0",                // endedAt
            "25",               // focusMinutes
            "5",                // shortBreakMinutes
            "15",               // longBreakMinutes
            "1",                // completedFocusCount
            "2",                // totalFocusCount
            "Partial",          // status
            ""                  // taskName
        ).joinToString(";")
        val legacy = listOf(
            "Playing",          // timerState
            "0",                // currentIndex
            "1500",             // remainingSeconds
            "1700000000000",    // endsAtMillis
            "F:1500,S:300",     // itemsCsv
            "",                 // taskField (no active task)
            legacyOpenRecord
        ).joinToString("|")

        val decoded = decodeRuntimeSnapshot(legacy)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.openRecord?.completedFocusCount)
        assertEquals(0, decoded.openRecord?.completedShortBreakCount)
        assertEquals(0, decoded.openRecord?.completedLongBreakCount)
    }

    // ── activeHabitId (v3) ───────────────────────────────────────────────────

    @Test
    fun encode_then_decode_roundTrips_activeHabitId() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
                currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Playing
            ),
            endsAtMillis = 1_700_000_000_000L,
            activeTask = null, openRecord = null,
            activeHabitId = 7,
        )
        val decoded = decodeRuntimeSnapshot(encodeRuntimeSnapshot(state))
        assertEquals(7, decoded?.activeHabitId)
        assertEquals(FocusType.Empty, decoded?.focusType)
    }

    @Test
    fun encode_then_decode_roundTrips_focusType_habit() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
                currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Playing
            ),
            endsAtMillis = 1_700_000_000_000L,
            activeTask = null, openRecord = null,
            activeHabitId = 3,
            focusType = FocusType.Habit,
        )
        val decoded = decodeRuntimeSnapshot(encodeRuntimeSnapshot(state))
        assertEquals(FocusType.Habit, decoded?.focusType)
    }

    @Test
    fun encode_then_decode_roundTrips_focusType_task() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
                currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Playing
            ),
            endsAtMillis = 1_700_000_000_000L,
            activeTask = FocusTask(id = 5, title = "Read docs", minutes = 25),
            openRecord = null,
            focusType = FocusType.Task,
        )
        val decoded = decodeRuntimeSnapshot(encodeRuntimeSnapshot(state))
        assertEquals(FocusType.Task, decoded?.focusType)
    }

    @Test
    fun encode_then_decode_roundTrips_focusType_simpleNote() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
                currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Playing
            ),
            endsAtMillis = 1_700_000_000_000L,
            activeTask = FocusTask(id = -1, title = "Quick note", minutes = 0),
            openRecord = null,
            focusType = FocusType.SimpleNote,
        )
        val decoded = decodeRuntimeSnapshot(encodeRuntimeSnapshot(state))
        assertEquals(FocusType.SimpleNote, decoded?.focusType)
    }
}
