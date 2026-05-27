package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.isTargetMetOn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Status filter values for the Tasks screen.
 *
 * "Done" replaces the older "Completed" terminology — shorter, casual, matches
 * the segmented control in the redesign.
 *
 * Archived stays as a placeholder for the unbuilt archive feature; selecting
 * it currently yields an empty list and a tailored empty state.
 */
enum class TaskUiFilter(val label: String) {
    All("All"),
    Active("Active"),
    Done("Done"),
    Archived("Archived"),
}

/**
 * Aggregate counts used to render counts inline on the status filter labels
 * (e.g. "All 6 / Active 4 / Done 2"), scoped by the currently selected category.
 *
 * Estimated-focus-time is intentionally absent: tasks are decoupled from per-task
 * pomodoro plans now, so multiplying [FocusTask.plannedSessions] by focus minutes
 * would invent a target the user never set.
 */
data class TaskOverview(
    val total: Int,
    val inProgress: Int,
    val completed: Int,
)

fun filterTasksForUi(tasks: List<FocusTask>, filter: TaskUiFilter): List<FocusTask> = when (filter) {
    TaskUiFilter.All -> tasks
    TaskUiFilter.Active -> tasks.filterNot { it.completed }
    TaskUiFilter.Done -> tasks.filter { it.completed }
    TaskUiFilter.Archived -> emptyList()
}

fun taskOverview(tasks: List<FocusTask>): TaskOverview = TaskOverview(
    total = tasks.size,
    inProgress = tasks.count { !it.completed },
    completed = tasks.count { it.completed },
)

enum class HabitUiFilter(val label: String) {
    All("All"),
    Daily("Daily"),
    Weekly("Weekly"),
    Custom("Custom"),
}

data class HabitWeekDot(
    val label: String,
    val date: LocalDate,
    val isScheduled: Boolean,
    val isCompleted: Boolean,
    val isToday: Boolean = false,
)

data class MonthProgressDot(
    val date: LocalDate,
    val isCompleted: Boolean,
)

fun filterHabitsForUi(habits: List<Habit>, filter: HabitUiFilter): List<Habit> = when (filter) {
    HabitUiFilter.All -> habits
    HabitUiFilter.Daily -> habits.filter { it.occurrence is HabitOccurrence.Daily }
    HabitUiFilter.Weekly -> habits.filter { it.occurrence is HabitOccurrence.Weekdays }
    HabitUiFilter.Custom -> emptyList()
}

fun habitWeekDots(
    habit: Habit,
    today: LocalDate,
    locale: Locale = Locale.getDefault(),
): List<HabitWeekDot> =
    (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        HabitWeekDot(
            label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
            date = date,
            isScheduled = habit.isScheduledOn(date),
            isCompleted = habit.isTargetMetOn(date),
            isToday = date == today,
        )
    }

fun monthlyHabitProgressDots(habits: List<Habit>, monthDate: LocalDate): List<MonthProgressDot> {
    val month = YearMonth.from(monthDate)
    return (1..month.lengthOfMonth()).map { day ->
        val date = month.atDay(day)
        MonthProgressDot(
            date = date,
            isCompleted = habits.any { it.isTargetMetOn(date) },
        )
    }
}
