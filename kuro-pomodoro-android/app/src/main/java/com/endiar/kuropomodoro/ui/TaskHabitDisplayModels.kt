package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.isTargetMetOn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class TaskUiFilter(val label: String) {
    All("All"),
    Active("Active"),
    Completed("Completed"),
    Archived("Archived"),
}

data class TaskOverview(
    val total: Int,
    val inProgress: Int,
    val completed: Int,
    val estimatedFocusTimeLabel: String,
)

fun filterTasksForUi(tasks: List<FocusTask>, filter: TaskUiFilter): List<FocusTask> = when (filter) {
    TaskUiFilter.All -> tasks
    TaskUiFilter.Active -> tasks.filterNot { it.completed }
    TaskUiFilter.Completed -> tasks.filter { it.completed }
    TaskUiFilter.Archived -> emptyList()
}

fun taskOverview(tasks: List<FocusTask>, settings: TimerSettings): TaskOverview {
    val totalMinutes = tasks.sumOf { it.plannedSessions.coerceAtLeast(1) * settings.focusMinutes.coerceAtLeast(1) }
    return TaskOverview(
        total = tasks.size,
        inProgress = tasks.count { !it.completed },
        completed = tasks.count { it.completed },
        estimatedFocusTimeLabel = formatDurationMinutes(totalMinutes),
    )
}

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

fun formatDurationMinutes(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val hours = safe / 60
    val minutes = safe % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
