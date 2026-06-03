package com.endiar.purrodoro.ui

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.HabitLogState
import com.endiar.purrodoro.model.HabitOccurrence
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimeBlock
import com.endiar.purrodoro.model.withLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(listOf(done), filterTasksForUi(tasks, TaskUiFilter.Done))
        assertEquals(emptyList<FocusTask>(), filterTasksForUi(tasks, TaskUiFilter.Archived))
    }

    @Test
    fun taskOverviewCountsByCompletion() {
        val tasks = listOf(
            FocusTask(id = 1, title = "Compose Migration", minutes = 25),
            FocusTask(id = 2, title = "Fix UI Bugs", minutes = 25, completed = true),
        )

        val overview = taskOverview(tasks)

        assertEquals(2, overview.total)
        assertEquals(1, overview.inProgress)
        assertEquals(1, overview.completed)
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
    fun nextScheduledDateReturnsTodayForDailyAndNextDayForWeekday() {
        val daily = habit(occurrence = HabitOccurrence.Daily)
        assertEquals(today, daily.nextScheduledDate(today))

        val tomorrow = today.plusDays(1)
        val tomorrowOnly = habit(occurrence = HabitOccurrence.Weekdays(setOf(tomorrow.dayOfWeek)))
        assertEquals(tomorrow, tomorrowOnly.nextScheduledDate(today))
    }

    @Test
    fun groupHabitsSplitsTodayFromUpcomingAndOrdersEachGroup() {
        val tomorrowDow = today.plusDays(1).dayOfWeek

        val dailyDone = habit(id = 1, occurrence = HabitOccurrence.Daily)
            .withLog(today, HabitLogState.Completed)
        val dailyPending = habit(id = 2, occurrence = HabitOccurrence.Daily)
        val dueTodayWeekday = habit(id = 3, occurrence = HabitOccurrence.Weekdays(setOf(today.dayOfWeek)))
        val tomorrowOnly = habit(id = 4, occurrence = HabitOccurrence.Weekdays(setOf(tomorrowDow)))

        val grouped = groupHabitsByDueState(
            listOf(dailyDone, dailyPending, dueTodayWeekday, tomorrowOnly),
            today,
        )

        // Today: still-to-do habits first (by id), completed sinks to the bottom.
        assertEquals(listOf(2, 3, 1), grouped.today.map { it.id })
        // Coming up: only the habit not scheduled today.
        assertEquals(listOf(4), grouped.upcoming.map { it.id })
    }

    @Test
    fun nextFocusFavorsOverdueOverTodayAndPriority() {
        val tasks = listOf(
            // High priority but not due → behind today's normal task
            FocusTask(id = 1, title = "High no date", minutes = 25, priority = TaskPriority.High),
            // Due today, normal priority
            FocusTask(id = 2, title = "Today normal", minutes = 25, dueDate = today),
            // Overdue → wins
            FocusTask(
                id = 3, title = "Overdue", minutes = 25,
                dueDate = today.minusDays(1), priority = TaskPriority.Low,
            ),
            // Tomorrow normal
            FocusTask(id = 4, title = "Tomorrow", minutes = 25, dueDate = today.plusDays(1)),
        )

        val pick = nextFocusTask(tasks, today)

        assertEquals(3, pick?.id)
    }

    @Test
    fun nextFocusBreaksTiesByPriorityThenNewestAdded() {
        val tasks = listOf(
            FocusTask(id = 1, title = "Today normal", minutes = 25, dueDate = today),
            FocusTask(
                id = 2, title = "Today high", minutes = 25,
                dueDate = today, priority = TaskPriority.High,
            ),
            FocusTask(
                id = 3, title = "Today high later", minutes = 25,
                dueDate = today, priority = TaskPriority.High,
            ),
        )

        // Same due date + same priority → newest added (highest id) wins.
        val pick = nextFocusTask(tasks, today)

        assertEquals(3, pick?.id)
    }

    @Test
    fun sortedActiveTasksOrdersByDueThenPriorityThenNewest() {
        val tasks = listOf(
            FocusTask(id = 1, title = "No date normal", minutes = 25),
            FocusTask(
                id = 2, title = "No date high", minutes = 25,
                priority = TaskPriority.High,
            ),
            FocusTask(id = 3, title = "Overdue", minutes = 25, dueDate = today.minusDays(1)),
            FocusTask(id = 4, title = "Today old", minutes = 25, dueDate = today),
            FocusTask(id = 5, title = "Today new", minutes = 25, dueDate = today),
            FocusTask(id = 6, title = "Done", minutes = 25, completed = true, dueDate = today),
        )

        val order = sortedActiveTasks(tasks, today).map { it.id }

        // Overdue → today (newest of the tie first) → no-date (High before Normal),
        // completed task excluded.
        assertEquals(listOf(3, 5, 4, 2, 1), order)
    }

    @Test
    fun nextFocusIgnoresCompletedTasks() {
        val tasks = listOf(
            FocusTask(
                id = 1, title = "Done overdue", minutes = 25, completed = true,
                dueDate = today.minusDays(2), priority = TaskPriority.High,
            ),
            FocusTask(id = 2, title = "Active", minutes = 25),
        )

        val pick = nextFocusTask(tasks, today)

        assertEquals(2, pick?.id)
    }

    @Test
    fun nextFocusReturnsNullWhenAllDone() {
        val tasks = listOf(
            FocusTask(id = 1, title = "Done", minutes = 25, completed = true),
            FocusTask(id = 2, title = "Done too", minutes = 25, completed = true),
        )

        assertNull(nextFocusTask(tasks, today))
    }

    @Test
    fun taskUrgencyBadgeReflectsDueDateThenHigh() {
        val today = this.today
        assertEquals(
            TaskUrgencyBadge.Overdue,
            taskUrgencyBadge(
                FocusTask(1, "x", 25, dueDate = today.minusDays(2)),
                today,
            ),
        )
        assertEquals(
            TaskUrgencyBadge.Today,
            taskUrgencyBadge(FocusTask(1, "x", 25, dueDate = today), today),
        )
        assertEquals(
            TaskUrgencyBadge.Tomorrow,
            taskUrgencyBadge(FocusTask(1, "x", 25, dueDate = today.plusDays(1)), today),
        )
        assertEquals(
            TaskUrgencyBadge.HighNoDate,
            taskUrgencyBadge(
                FocusTask(1, "x", 25, priority = TaskPriority.High),
                today,
            ),
        )
        assertNull(
            taskUrgencyBadge(
                FocusTask(1, "x", 25, priority = TaskPriority.Normal),
                today,
            ),
        )
    }

    @Test
    fun heroDueLabelHumanizesRelativeDates() {
        assertEquals("Due today", heroDueLabel(today, today))
        assertEquals("Due tomorrow", heroDueLabel(today.plusDays(1), today))
        assertEquals("Due in 3 days", heroDueLabel(today.plusDays(3), today))
        assertEquals("Overdue by 1 day", heroDueLabel(today.minusDays(1), today))
        assertEquals("Overdue by 4 days", heroDueLabel(today.minusDays(4), today))
        assertNull(heroDueLabel(null, today))
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
        occurrence = occurrence,
        timeBlock = TimeBlock.Anytime,
    )
}
