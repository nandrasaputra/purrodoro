package com.endiar.purrodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FocusEligibilityTest {

    private val today = LocalDate.of(2024, 6, 17)

    private fun habit() = Habit(
        id = 1,
        name = "Read",
        emoji = "📚",
        colorArgb = 0xFF_90CAF9L,
        createdAt = 0L,
        occurrence = HabitOccurrence.Daily,
    )

    @Test
    fun `completed task cannot start focus`() {
        val task = FocusTask(id = 1, title = "Done", minutes = 0, completed = true)
        assertFalse(task.canStartFocus())
    }

    @Test
    fun `open task can start focus`() {
        val task = FocusTask(id = 1, title = "Open", minutes = 0, completed = false)
        assertTrue(task.canStartFocus())
    }

    @Test
    fun `habit blocks focus when target met skipped or not scheduled`() {
        val done = habit().withLog(today, HabitLogState.Completed)
        assertEquals(HabitFocusBlockReason.Completed, done.focusBlockReason(today))
        assertFalse(done.canStartFocus(today))

        val skipped = habit().withLog(today, HabitLogState.Skipped)
        assertEquals(HabitFocusBlockReason.Skipped, skipped.focusBlockReason(today))
        assertFalse(skipped.canStartFocus(today))

        val restDay = habit().copy(occurrence = HabitOccurrence.Weekdays(setOf(today.dayOfWeek.minus(1))))
        assertEquals(HabitFocusBlockReason.NotScheduled, restDay.focusBlockReason(today))
        assertFalse(restDay.canStartFocus(today))
    }

    @Test
    fun `eligible habit has no block reason`() {
        val open = habit()
        assertNull(open.focusBlockReason(today))
        assertTrue(open.canStartFocus(today))
    }
}
