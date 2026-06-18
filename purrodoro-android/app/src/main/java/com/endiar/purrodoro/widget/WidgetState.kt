package com.endiar.purrodoro.widget

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.TimeBlock
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.calculateStreak
import com.endiar.purrodoro.model.isScheduledOn
import com.endiar.purrodoro.model.isSkippedOn
import com.endiar.purrodoro.model.isTargetMetOn
import java.time.LocalDate

/**
 * Pure UI view-models for the Purrodoro home-screen widgets.
 *
 * These types are intentionally free of Android dependencies (no `Context`,
 * `Resources`, etc.) so they can be derived from `AppRepository` data and
 * unit-tested without instrumentation.
 */

/** Encouragement tone for the Today widget, resolved to localized copy in the widget. */
enum class TodayEncouragement {
    SoftStart,
    FirstFocus,
    GoalReached,
    Proud,
}

/** What the Today With Purrodoro widget renders. */
data class TodayWidgetState(
    val focusCount: Int,
    val encouragement: TodayEncouragement,
)

// ─── Nudge model (shared by the 4x4 "reactive Purrodoro" widgets) ──────────────────

/**
 * Where a widget tap (or its CTA) should take the user. Mapped to launch-intent
 * extras by `WidgetActions` on the Android side.
 */
enum class NudgeTarget {
    /** Open the app and auto-surface the Start Focus sheet on the Timer tab. */
    StartFocus,

    /** Open the app on the live Timer tab (a cycle is in flight). */
    OpenTimer,

    /** Open the app on the Habits tab. */
    OpenHabits,

    /** Open the app on the Tasks tab. */
    OpenTasks,

    /** Just open the app wherever the user left off. */
    OpenApp,
}

/** Semantic flavor of the current nudge — lets a widget pick mascot art / accenting. */
enum class NudgeKind {
    Focusing,
    Paused,
    Celebrating,
    GoalReached,
    StreakAtRisk,
    HabitDue,
    TaskDue,
    FreshFocus,
}

/** A small actionable row (habit or task) for the plan / checklist widgets. */
data class WidgetChip(
    val emoji: String,
    val label: String,
    val target: NudgeTarget,
)

/** The single prioritized nudge surfaced by the reactive widgets. */
data class WidgetNudge(
    val kind: NudgeKind,
    val title: String,
    val line: String,
    val ctaLabel: String,
    val target: NudgeTarget,
)

/**
 * All facts the 4x4 widgets need, derived once from app state. Pure and free of
 * Android dependencies so it can be unit-tested; `WidgetDataLoader` maps the
 * repository's `AppState` onto [WidgetState.deriveData].
 */
data class KuroWidgetData(
    val focusToday: Int,
    val focusTarget: Int,
    val streak: Int,
    val streakAtRisk: Boolean,
    val goalReached: Boolean,
    val habitsScheduledToday: Int,
    val habitsDoneToday: Int,
    val pendingHabits: List<WidgetChip>,
    val pendingTasksCount: Int,
    val pendingTasks: List<WidgetChip>,
    val timerState: TimerState,
    val pendingCycleAck: Boolean,
    val nudge: WidgetNudge,
) {
    /** Up-to-[max] mixed agenda (habits first, then tasks) for the Daily Plan widget. */
    fun planChips(max: Int = 3): List<WidgetChip> =
        (pendingHabits + pendingTasks).take(max)
}

object WidgetState {

    /** Time-of-day ordering so the "next" habit nudge feels chronological. */
    private val timeBlockOrder = listOf(
        TimeBlock.Morning,
        TimeBlock.Afternoon,
        TimeBlock.Evening,
        TimeBlock.Anytime,
    )

    private const val TASK_EMOJI = "\uD83D\uDCDD" // 📝

    /**
     * Derives [KuroWidgetData] from the raw pieces of app state.
     *
     * Nudge priority (highest first):
     *  1. Live timer (Playing → Focusing, Paused → Paused)
     *  2. Pending cycle-completion ack → Celebrating
     *  3. Daily focus goal already reached → GoalReached
     *  4. A streak is alive but today has no focus yet → StreakAtRisk
     *  5. A habit is scheduled-and-due today → HabitDue
     *  6. A task is due today or overdue → TaskDue
     *  7. Otherwise → FreshFocus
     */
    fun deriveData(
        stamps: Map<LocalDate, Int>,
        settings: TimerSettings,
        habits: List<Habit>,
        tasks: List<FocusTask>,
        runtime: TimerRuntimeState?,
        pendingCycleAck: Boolean,
        today: LocalDate,
    ): KuroWidgetData {
        val target = settings.focusSessionsPerCycle
            .coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE)
        val focusToday = (stamps[today] ?: 0).coerceAtLeast(0)
        val streak = calculateStreak(stamps, today)
        val goalReached = focusToday >= target
        val streakAtRisk = streak > 0 && focusToday == 0

        val scheduledHabits = habits.filter { it.isScheduledOn(today) }
        val dueHabits = scheduledHabits
            .filter { !it.isTargetMetOn(today) && !it.isSkippedOn(today) }
            .sortedWith(compareBy({ timeBlockOrder.indexOf(it.timeBlock) }, { it.createdAt }))
        val pendingHabits = dueHabits.map { habit ->
            WidgetChip(
                emoji = habit.emoji.ifBlank { "\uD83C\uDF31" }, // 🌱 fallback
                label = habit.name,
                target = NudgeTarget.OpenHabits,
            )
        }
        val habitsDoneToday = scheduledHabits.count { it.isTargetMetOn(today) }

        val pendingTaskItems = tasks.filter { !it.completed }
        val dueTasks = pendingTaskItems
            .filter { it.dueDate != null && !it.dueDate.isAfter(today) }
            .sortedWith(compareBy({ it.dueDate }, { -it.priority.ordinal }))
        // For the plan / checklist we show due tasks first, then any other open task.
        val orderedTasks = (dueTasks + pendingTaskItems.filterNot { it in dueTasks })
        val pendingTasks = orderedTasks.map { task ->
            WidgetChip(emoji = TASK_EMOJI, label = task.title, target = NudgeTarget.OpenTasks)
        }

        val timerState = runtime?.session?.timerState ?: TimerState.Idle

        val nudge = computeNudge(
            timerState = timerState,
            pendingCycleAck = pendingCycleAck,
            goalReached = goalReached,
            streak = streak,
            streakAtRisk = streakAtRisk,
            firstHabit = pendingHabits.firstOrNull(),
            firstDueTask = dueTasks.firstOrNull(),
        )

        return KuroWidgetData(
            focusToday = focusToday,
            focusTarget = target,
            streak = streak,
            streakAtRisk = streakAtRisk,
            goalReached = goalReached,
            habitsScheduledToday = scheduledHabits.size,
            habitsDoneToday = habitsDoneToday,
            pendingHabits = pendingHabits,
            pendingTasksCount = pendingTaskItems.size,
            pendingTasks = pendingTasks,
            timerState = timerState,
            pendingCycleAck = pendingCycleAck,
            nudge = nudge,
        )
    }

    private fun computeNudge(
        timerState: TimerState,
        pendingCycleAck: Boolean,
        goalReached: Boolean,
        streak: Int,
        streakAtRisk: Boolean,
        firstHabit: WidgetChip?,
        firstDueTask: FocusTask?,
    ): WidgetNudge = when {
        timerState == TimerState.Playing -> WidgetNudge(
            kind = NudgeKind.Focusing,
            title = "Purrodoro is focusing",
            line = "Stay with your session",
            ctaLabel = "Open Timer",
            target = NudgeTarget.OpenTimer,
        )

        timerState == TimerState.Paused -> WidgetNudge(
            kind = NudgeKind.Paused,
            title = "Purrodoro is waiting",
            line = "Tap to resume",
            ctaLabel = "Resume",
            target = NudgeTarget.OpenTimer,
        )

        pendingCycleAck -> WidgetNudge(
            kind = NudgeKind.Celebrating,
            title = "Nice work today",
            line = "Purrodoro is proud of you",
            ctaLabel = "Open Purrodoro",
            target = NudgeTarget.OpenApp,
        )

        goalReached -> WidgetNudge(
            kind = NudgeKind.GoalReached,
            title = "Goal reached",
            line = "Purrodoro is so proud of you",
            ctaLabel = "One more?",
            target = NudgeTarget.StartFocus,
        )

        streakAtRisk -> WidgetNudge(
            kind = NudgeKind.StreakAtRisk,
            title = "Keep your ${streak}-day streak",
            line = "A small focus keeps it alive",
            ctaLabel = "Start Focus",
            target = NudgeTarget.StartFocus,
        )

        firstHabit != null -> WidgetNudge(
            kind = NudgeKind.HabitDue,
            title = "Time for ${firstHabit.label}?",
            line = "Purrodoro will keep you company",
            ctaLabel = "Open Habits",
            target = NudgeTarget.OpenHabits,
        )

        firstDueTask != null -> WidgetNudge(
            kind = NudgeKind.TaskDue,
            title = "${firstDueTask.title} is waiting",
            line = "Want to focus on it together?",
            ctaLabel = "Open Tasks",
            target = NudgeTarget.OpenTasks,
        )

        else -> WidgetNudge(
            kind = NudgeKind.FreshFocus,
            title = "Purrodoro is ready",
            line = "Purrodoro's ready when you are",
            ctaLabel = "Start Focus",
            target = NudgeTarget.StartFocus,
        )
    }


    /**
     * Derives the Today widget state.
     *
     * The daily goal ([TimerSettings.focusSessionsPerCycle], 1..4) drives the
     * encouragement copy: it softens at 0 focus and warms up once the goal is
     * reached.
     */
    fun today(
        focusCountToday: Int,
        settings: TimerSettings,
    ): TodayWidgetState {
        val target = settings.focusSessionsPerCycle.coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE)
        val safeCount = focusCountToday.coerceAtLeast(0)
        val encouragement = when {
            safeCount <= 0 -> TodayEncouragement.SoftStart
            safeCount == 1 -> TodayEncouragement.FirstFocus
            safeCount >= target -> TodayEncouragement.GoalReached
            else -> TodayEncouragement.Proud
        }
        return TodayWidgetState(
            focusCount = safeCount,
            encouragement = encouragement,
        )
    }
}
