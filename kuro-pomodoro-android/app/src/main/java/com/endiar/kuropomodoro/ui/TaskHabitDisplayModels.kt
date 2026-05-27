package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.TaskPriority
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.isTargetMetOn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
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

/**
 * The quiet "what to bring into focus next" line that drives both the Tasks
 * hero card and the Home `continueTask` fallback.
 *
 * Active (not completed) tasks are ranked by:
 *   1. Overdue first (dueDate < today)
 *   2. Then today's due dates
 *   3. Then tomorrow's
 *   4. Then any other future date in ascending order
 *   5. Then tasks with no due date
 *
 * Within the same bucket, higher [TaskPriority] wins (High > Normal > Low),
 * with original list order as the final tie-break so the user's mental model
 * of "task I added last" stays stable.
 */
fun nextFocusTask(tasks: List<FocusTask>, today: LocalDate): FocusTask? {
    val active = tasks.withIndex().filterNot { it.value.completed }
    if (active.isEmpty()) return null

    fun bucket(task: FocusTask): Int {
        val due = task.dueDate ?: return 4
        return when {
            due.isBefore(today) -> 0
            due == today -> 1
            due == today.plusDays(1) -> 2
            else -> 3
        }
    }

    fun priorityRank(p: TaskPriority): Int = when (p) {
        TaskPriority.High -> 0
        TaskPriority.Normal -> 1
        TaskPriority.Low -> 2
    }

    return active.sortedWith(
        compareBy(
            { bucket(it.value) },
            { it.value.dueDate ?: LocalDate.MAX },
            { priorityRank(it.value.priority) },
            { it.index },
        )
    ).first().value
}

/**
 * Single right-side metadata badge a task row should show. Priority order is
 * Overdue > Today > Tomorrow > High (no date). Everything else suppresses the
 * badge to keep the list calm.
 */
sealed class TaskUrgencyBadge {
    object Overdue : TaskUrgencyBadge()
    object Today : TaskUrgencyBadge()
    object Tomorrow : TaskUrgencyBadge()
    /** Used when a high-priority task has no due date, so we still surface the signal. */
    object HighNoDate : TaskUrgencyBadge()
    /** A future due date (more than tomorrow) — shown as muted "MMM d". */
    data class FutureDate(val label: String) : TaskUrgencyBadge()
}

private val futureDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

fun taskUrgencyBadge(task: FocusTask, today: LocalDate): TaskUrgencyBadge? {
    val due = task.dueDate
    return when {
        due != null && due.isBefore(today) -> TaskUrgencyBadge.Overdue
        due == today -> TaskUrgencyBadge.Today
        due == today.plusDays(1) -> TaskUrgencyBadge.Tomorrow
        due != null -> TaskUrgencyBadge.FutureDate(due.format(futureDateFormatter))
        task.priority == TaskPriority.High -> TaskUrgencyBadge.HighNoDate
        else -> null
    }
}

/**
 * Human-friendly due-date label for the Next Focus hero card. Returns null
 * when there's no due date so the caller can hide the chip entirely.
 *
 *   today          -> "Due today"
 *   tomorrow       -> "Due tomorrow"
 *   past (today-1) -> "Overdue by 1 day"
 *   past (today-N) -> "Overdue by N days"
 *   future +N      -> "Due in N days"
 */
fun heroDueLabel(dueDate: LocalDate?, today: LocalDate): String? {
    if (dueDate == null) return null
    val days = ChronoUnit.DAYS.between(today, dueDate).toInt()
    return when {
        days == 0 -> "Due today"
        days == 1 -> "Due tomorrow"
        days < 0 -> {
            val ago = -days
            if (ago == 1) "Overdue by 1 day" else "Overdue by $ago days"
        }
        else -> "Due in $days days"
    }
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
