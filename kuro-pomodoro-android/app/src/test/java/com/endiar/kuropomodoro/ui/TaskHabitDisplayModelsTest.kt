package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitLogState
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.HabitTargetType
import com.endiar.kuropomodoro.model.TimeBlock
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.withLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class TaskHabitDisplayModelsTest {
    private val today = LocalDate.of(2026, 5, 24)

    @Test
    fun taskFilterBucketsMapToExistingCompletionState() {
        val active = FocusTask(id = 1, title = "Compose Migration", minutes = 25, completed = false)
        val done = FocusTask(id = 2, title = "Fix UI Bugs", minutes = 25, completed = true)
        val tasks = listOf(active, done)

        assertEquals(tasks, filterTasksForUi(tasks, TaskUiFilter.All))
        assertEquals(listOf(active), filterTasksForUi(tasks, TaskUiFilter.Active))
        assertEquals(listOf(done), filterTasksForUi(tasks, TaskUiFilter.Completed))
        assertEquals(emptyList<FocusTask>(), filterTasksForUi(tasks, TaskUiFilter.Archived))
    }

    @Test
    fun taskOverviewCountsAndEstimatesFocusTime() {
        val tasks = listOf(
            FocusTask(id = 1, title = "Compose Migration", minutes = 25, plannedSessions = 5, completedSessions = 2),
            FocusTask(id = 2, title = "Fix UI Bugs", minutes = 25, completed = true, plannedSessions = 3, completedSessions = 3),
        )

        val overview = taskOverview(tasks, TimerSettings(focusMinutes = 25))

        assertEquals(2, overview.total)
        assertEquals(1, overview.inProgress)
        assertEquals(1, overview.completed)
        assertEquals("3h 20m", overview.estimatedFocusTimeLabel)
    }

    @Test
    fun habitFiltersMapToCurrentOccurrenceTypes() {
        val daily = habit(id = 1, occurrence = HabitOccurrence.Daily)
        val weekly = habit(id = 2, occurrence = HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY)))
        val habits = listOf(daily, weekly)

        assertEquals(habits, filterHabitsForUi(habits, HabitUiFilter.All))
        assertEquals(listOf(daily), filterHabitsForUi(habits, HabitUiFilter.Daily))
        assertEquals(listOf(weekly), filterHabitsForUi(habits, HabitUiFilter.Weekly))
        assertEquals(emptyList<Habit>(), filterHabitsForUi(habits, HabitUiFilter.Custom))
    }

    @Test
    fun habitWeekDotsShowScheduledAndCompletedDays() {
        val habit = habit(occurrence = HabitOccurrence.Daily)
            .withLog(today.minusDays(1), HabitLogState.Completed)

        val dots = habitWeekDots(habit, today, Locale.US)

        assertEquals(7, dots.size)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), dots.map { it.label })
        assertTrue(dots[5].isCompleted)
        assertFalse(dots[6].isCompleted)
        assertTrue(dots.all { it.isScheduled })
    }

    @Test
    fun monthlyHabitProgressMarksDatesWithAnyCompletedHabit() {
        val completedDate = LocalDate.of(2026, 5, 20)
        val habits = listOf(habit().withLog(completedDate, HabitLogState.Completed))

        val dots = monthlyHabitProgressDots(habits, today)
        val completedDot = dots.first { it.date == completedDate }

        assertTrue(completedDot.isCompleted)
        assertEquals(31, dots.size)
    }

    private fun habit(
        id: Int = 1,
        occurrence: HabitOccurrence = HabitOccurrence.Daily,
    ) = Habit(
        id = id,
        name = "Read 10 Pages",
        emoji = "📖",
        colorArgb = 0xFF8EAD7EL,
        createdAt = 0L,
        targetType = HabitTargetType.Binary,
        targetValue = 1,
        occurrence = occurrence,
        timeBlock = TimeBlock.Anytime,
    )
}
