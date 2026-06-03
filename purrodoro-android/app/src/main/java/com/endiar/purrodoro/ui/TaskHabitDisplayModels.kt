package com.endiar.purrodoro.ui

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.isScheduledOn
import com.endiar.purrodoro.model.isTargetMetOn
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

private fun taskPriorityRank(p: TaskPriority): Int = when (p) {
    TaskPriority.High -> 0
    TaskPriority.Normal -> 1
    TaskPriority.Low -> 2
}

/**
 * Sorts active (not completed) tasks for the Tasks list. Precedence:
 *
 *   1. Due-date urgency — earliest date first, so the most overdue sits at the
 *      top, then today, tomorrow, future dates ascending; no-date tasks last.
 *   2. Priority — High > Normal > Low.
 *   3. Recency — most recently added first (higher id = newer).
 *
 * The Tasks hero ([nextFocusTask]) is just the head of this list, so the hero
 * card and the rows below it share one consistent ordering.
 */
fun sortedActiveTasks(tasks: List<FocusTask>, today: LocalDate): List<FocusTask> {
    // Days from today to the due date: negative = overdue (most overdue sorts
    // first), 0 = today, positive = upcoming; no due date sinks to the bottom.
    fun dueRank(task: FocusTask): Int {
        val due = task.dueDate ?: return Int.MAX_VALUE
        return ChronoUnit.DAYS.between(today, due).toInt()
    }
    return tasks
        .filterNot { it.completed }
        .sortedWith(
            compareBy<FocusTask> { dueRank(it) }
                .thenBy { taskPriorityRank(it.priority) }
                .thenByDescending { it.id }
        )
}

/**
 * The single "what to bring into focus next" task — the head of
 * [sortedActiveTasks]. Drives both the Tasks hero card and the Home
 * `continueTask` fallback. Returns null when there are no active tasks.
 */
fun nextFocusTask(tasks: List<FocusTask>, today: LocalDate): FocusTask? =
    sortedActiveTasks(tasks, today).firstOrNull()

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

data class HabitWeekDot(
    val label: String,
    val date: LocalDate,
    val isScheduled: Boolean,
    val isCompleted: Boolean,
    val isToday: Boolean = false,
)

/**
 * Splits habits into the ones due today (scheduled for [today]) and the ones
 * that aren't, for the grouped "Today" / "Coming up" sections on the Habits
 * list. Today's habits surface the still-to-do ones first (completed sink to
 * the bottom); coming-up habits are ordered by whichever lands soonest.
 */
data class GroupedHabits(
    val today: List<Habit>,
    val upcoming: List<Habit>,
)

fun groupHabitsByDueState(habits: List<Habit>, today: LocalDate): GroupedHabits {
    val (dueToday, later) = habits.partition { it.isScheduledOn(today) }
    return GroupedHabits(
        today = dueToday.sortedWith(
            compareBy({ it.isTargetMetOn(today) }, { it.createdAt }, { it.id }),
        ),
        upcoming = later.sortedWith(
            compareBy({ it.nextScheduledDate(today) }, { it.createdAt }, { it.id }),
        ),
    )
}

/**
 * The next date on or after [today] the habit is scheduled. Daily habits return
 * [today]; weekday habits walk forward to their next configured day (capped at a
 * week, which always contains a hit for a non-empty weekday set).
 */
fun Habit.nextScheduledDate(today: LocalDate): LocalDate {
    var date = today
    repeat(7) {
        if (isScheduledOn(date)) return date
        date = date.plusDays(1)
    }
    return today
}

data class MonthProgressDot(
    val date: LocalDate,
    val isCompleted: Boolean,
)

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
