package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
    val current = if (isStarted) {
        (completedFocusSessions + 1).coerceIn(1, total)
    } else {
        ((task?.completedSessions ?: completedFocusSessions) + 0).coerceIn(0, total)
    }
    val displayCurrent = if (isStarted) current else current.coerceAtLeast(0)
    return PomodoroProgress(
        current = displayCurrent,
        total = total,
        activeLabel = "Pomodoro ${displayCurrent.coerceAtLeast(1)} of $total",
        completedLabel = "$displayCurrent / $total Pomodoros completed"
    )
}

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
