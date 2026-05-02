package com.endiar.kuropomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KuroModelsTest {
    @Test
    fun timerModeFormatsRemainingTime() {
        assertEquals("25:00", TimerMode.Focus.formatDuration())
        assertEquals("05:00", TimerMode.ShortBreak.formatDuration())
        assertEquals("15:00", TimerMode.LongBreak.formatDuration())
        assertEquals("01:05", formatSeconds(65))
    }

    @Test
    fun stampCountIncrementsForSameDate() {
        val today = LocalDate.of(2026, 5, 2)
        val stamps = addFocusStamp(emptyMap(), today)
        val updated = addFocusStamp(stamps, today)

        assertEquals(2, updated[today])
    }

    @Test
    fun taskToggleChangesCompletionState() {
        val task = FocusTask(id = 1, title = "Read Kotlin docs", minutes = 25)

        assertFalse(task.completed)
        assertTrue(task.toggleCompleted().completed)
    }
}
