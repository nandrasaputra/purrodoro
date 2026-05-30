package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.InterruptedFocus
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.isSkippedOn
import com.endiar.kuropomodoro.model.isTargetMetOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.random.Random

data class HomeGreeting(
    val salutation: String,
    val encouragement: String
)

data class TodayFocusMetrics(
    val focusTimeLabel: String,
    val sessionsLabel: String,
    val completedLabel: String
)

data class PomodoroProgress(
    val current: Int,
    val total: Int,
    val activeLabel: String,
    val completedLabel: String
)

fun homeGreetingFor(time: LocalTime): HomeGreeting {
    val hour = time.hour
    return when {
        hour < 12 -> HomeGreeting(
            salutation = "Good morning,",
            encouragement = "Let's make this morning meaningful."
        )
        hour < 17 -> HomeGreeting(
            salutation = "Good afternoon,",
            encouragement = "Keep the afternoon moving gently."
        )
        else -> HomeGreeting(
            salutation = "Good evening,",
            encouragement = "Let's make this evening meaningful."
        )
    }
}

fun todayFocusMetrics(
    sessionRecords: List<SessionRecord>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): TodayFocusMetrics {
    val todayRecords = sessionRecords.filter { record ->
        record.endedAt > 0L &&
            Instant.ofEpochMilli(record.endedAt).atZone(zone).toLocalDate() == today
    }
    val completedFocusSegments = todayRecords.sumOf { it.completedFocusCount.coerceAtLeast(0) }
    val focusMinutes = todayRecords.sumOf { record ->
        record.completedFocusCount.coerceAtLeast(0) * record.focusMinutes.coerceAtLeast(0)
    }
    val completedCycles = todayRecords.count { it.status == SessionStatus.Completed }
    return TodayFocusMetrics(
        focusTimeLabel = formatFocusMinutes(focusMinutes),
        sessionsLabel = completedFocusSegments.toString(),
        completedLabel = completedCycles.toString()
    )
}

fun continueTask(activeTask: FocusTask?, tasks: List<FocusTask>): FocusTask? {
    return activeTask ?: tasks.firstOrNull { !it.completed && it.completedSessions < it.plannedSessions }
        ?: tasks.firstOrNull { !it.completed }
}

fun homeStartTask(activeTask: FocusTask?, continueTask: FocusTask?): FocusTask? {
    return activeTask
}

fun pomodoroProgress(
    completedFocusSessions: Int,
    totalFocusSessions: Int,
    isStarted: Boolean,
    task: FocusTask?,
    settings: TimerSettings
): PomodoroProgress {
    val total = when {
        task != null -> task.plannedSessions.coerceAtLeast(1)
        totalFocusSessions > 0 -> totalFocusSessions
        else -> settings.focusSessionsPerCycle.coerceAtLeast(1)
    }
    val completed = maxOf(task?.completedSessions ?: 0, completedFocusSessions)
    val current = if (task != null && isStarted) {
        (completed + 1).coerceIn(1, total)
    } else if (task != null) {
        completed.coerceIn(0, total)
    } else if (isStarted) {
        (completed + 1).coerceIn(1, total)
    } else {
        completed.coerceIn(0, total)
    }
    val displayCurrent = if (isStarted) current else current.coerceAtLeast(0)
    return PomodoroProgress(
        current = displayCurrent,
        total = total,
        activeLabel = "Pomodoro ${displayCurrent.coerceAtLeast(1)} of $total",
        completedLabel = "$displayCurrent / $total Pomodoros completed"
    )
}

/**
 * What the Home suggestion card should offer right now, when no cycle is in
 * flight. Resolution order:
 *
 *  1. [Continue] — a real task/habit focus was stopped mid-segment and is still
 *     unfinished, so we offer to pick the intention back up (with the leftover
 *     time carried into the Start Focus sheet for the user to adjust).
 *  2. [Suggest] — nothing was interrupted, so gently surface one active task or
 *     due habit at a time. The pick is stable for a given [seed] (callers pass
 *     the day's epoch-day) so the card doesn't flicker between candidates on
 *     recomposition.
 *  3. [Empty] — there is nothing to continue and nothing to suggest.
 *
 * The active-timer case is handled by the caller (the card is simply not shown
 * while a session is running or paused), so it has no representation here.
 */
sealed class HomeSuggestion {
    data class Continue(
        val focusType: FocusType,
        val title: String,
        val remainingSeconds: Int,
        val taskId: Int?,
        val habitId: Int?,
    ) : HomeSuggestion()

    data class Suggest(
        val focusType: FocusType,
        val title: String,
        val taskId: Int?,
        val habitId: Int?,
    ) : HomeSuggestion()

    object Empty : HomeSuggestion()
}

fun homeSuggestion(
    interrupted: InterruptedFocus?,
    tasks: List<FocusTask>,
    habits: List<Habit>,
    today: LocalDate,
    seed: Long,
): HomeSuggestion {
    resolveInterruptedFocus(interrupted, tasks, habits, today)?.let { return it }

    val candidates = buildList {
        sortedActiveTasks(tasks, today).forEach { task ->
            add(HomeSuggestion.Suggest(FocusType.Task, task.title, taskId = task.id, habitId = null))
        }
        habits
            .filter { it.isScheduledOn(today) && !it.isTargetMetOn(today) && !it.isSkippedOn(today) }
            .forEach { habit ->
                add(
                    HomeSuggestion.Suggest(
                        focusType = FocusType.Habit,
                        title = "${habit.emoji} ${habit.name}",
                        taskId = null,
                        habitId = habit.id,
                    )
                )
            }
    }
    if (candidates.isEmpty()) return HomeSuggestion.Empty
    return candidates[Random(seed).nextInt(candidates.size)]
}

/**
 * Validates a stored [InterruptedFocus] against the current task/habit data and
 * returns a [HomeSuggestion.Continue] only when the intention is still
 * resumable. Returns null when the time was fully consumed, the task was
 * completed/deleted, or the habit is no longer due / already met today — in
 * which case the caller falls through to a fresh suggestion.
 */
private fun resolveInterruptedFocus(
    interrupted: InterruptedFocus?,
    tasks: List<FocusTask>,
    habits: List<Habit>,
    today: LocalDate,
): HomeSuggestion.Continue? {
    if (interrupted == null || interrupted.remainingSeconds <= 0) return null
    return when (interrupted.focusType) {
        FocusType.Task -> {
            val task = tasks.firstOrNull { it.id == interrupted.taskId && !it.completed }
                ?: return null
            HomeSuggestion.Continue(
                focusType = FocusType.Task,
                title = task.title,
                remainingSeconds = interrupted.remainingSeconds,
                taskId = task.id,
                habitId = null,
            )
        }
        FocusType.Habit -> {
            val habit = habits.firstOrNull { it.id == interrupted.habitId } ?: return null
            if (!habit.isScheduledOn(today) || habit.isTargetMetOn(today) || habit.isSkippedOn(today)) {
                return null
            }
            HomeSuggestion.Continue(
                focusType = FocusType.Habit,
                title = "${habit.emoji} ${habit.name}",
                remainingSeconds = interrupted.remainingSeconds,
                taskId = null,
                habitId = habit.id,
            )
        }
        FocusType.SimpleNote, FocusType.Empty -> null
    }
}

/** Whole-minute leftover for an interrupted focus, rounded up, never below 1. */
fun interruptedRemainingMinutes(remainingSeconds: Int): Int =
    ceil(remainingSeconds.coerceAtLeast(1) / 60.0).toInt().coerceAtLeast(1)

private fun formatFocusMinutes(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
