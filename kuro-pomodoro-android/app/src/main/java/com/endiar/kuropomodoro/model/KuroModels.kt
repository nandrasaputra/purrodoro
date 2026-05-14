package com.endiar.kuropomodoro.model

import com.endiar.kuropomodoro.R
import java.time.LocalDate
import java.time.LocalTime

enum class AppTab(val label: String, val symbol: String) {
    Timer("Focus", "Home"),
    Todo("Todo", "Todo"),
    Calendar("Records", "Records"),
    Sounds("Sounds", "Sound"),
    Profile("Me", "Me")
}

enum class TimerMode(val label: String, val defaultSeconds: Int, val accentName: String) {
    Focus("Focus", 25 * 60, "peach"),
    ShortBreak("Short Break", 5 * 60, "mint"),
    LongBreak("Long Break", 15 * 60, "lavender");

    fun formatDuration(): String = formatSeconds(defaultSeconds)
}

sealed class SessionItemType {
    data class Focus(val seconds: Int) : SessionItemType()
    data class ShortBreak(val seconds: Int) : SessionItemType()
    data class LongBreak(val seconds: Int) : SessionItemType()
}

data class TimerSessionItem(val type: SessionItemType)

enum class TimerState { Idle, Playing, Paused }

data class TimerSession(
    val items: List<TimerSessionItem>,
    val currentIndex: Int,
    val remainingSeconds: Int,
    val timerState: TimerState
) {
    val isActive: Boolean get() = timerState != TimerState.Idle
    val isComplete: Boolean get() = currentIndex >= items.size
    val currentItem: TimerSessionItem? get() = items.getOrNull(currentIndex)
}

data class FocusTask(
    val id: Int,
    val title: String,
    val minutes: Int,
    val completed: Boolean = false,
    val plannedSessions: Int = 1,
    val completedSessions: Int = 0
) {
    fun toggleCompleted(): FocusTask = copy(completed = !completed)
}

data class TodoListState(
    val id: Int,
    val name: String,
    val tasks: List<FocusTask>
)

data class SoundOption(
    val id: Int,
    val name: String,
    val description: String,
    val locked: Boolean,
    val symbol: String
)

data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val autoStartNextSession: Boolean = false,
    val focusSessionsPerCycle: Int = 4
) {
    fun secondsFor(mode: TimerMode): Int = when (mode) {
        TimerMode.Focus -> focusMinutes * 60
        TimerMode.ShortBreak -> shortBreakMinutes * 60
        TimerMode.LongBreak -> longBreakMinutes * 60
    }

    fun update(mode: TimerMode, minutes: Int): TimerSettings {
        val safeMinutes = minutes.coerceIn(1, 180)
        return when (mode) {
            TimerMode.Focus -> copy(focusMinutes = safeMinutes)
            TimerMode.ShortBreak -> copy(shortBreakMinutes = safeMinutes)
            TimerMode.LongBreak -> copy(longBreakMinutes = safeMinutes)
        }
    }

    fun updateCycleSize(n: Int): TimerSettings = copy(focusSessionsPerCycle = n.coerceIn(1, 10))
}

/**
 * Builds the full Pomodoro cycle item list from settings.
 *
 * Pattern for N = focusSessionsPerCycle:
 *   (Focus + ShortBreak) × (N-1), then Focus + LongBreak
 *
 * Example for N=4:
 *   [Focus, ShortBreak, Focus, ShortBreak, Focus, ShortBreak, Focus, LongBreak]
 */
fun buildCycle(settings: TimerSettings): List<TimerSessionItem> {
    val items = mutableListOf<TimerSessionItem>()
    val n = settings.focusSessionsPerCycle.coerceAtLeast(1)
    repeat(n - 1) {
        items += TimerSessionItem(SessionItemType.Focus(settings.focusMinutes * 60))
        items += TimerSessionItem(SessionItemType.ShortBreak(settings.shortBreakMinutes * 60))
    }
    items += TimerSessionItem(SessionItemType.Focus(settings.focusMinutes * 60))
    items += TimerSessionItem(SessionItemType.LongBreak(settings.longBreakMinutes * 60))
    return items
}

/** Builds a fresh [TimerSession] from [settings] at the start of a new cycle. */
fun buildSession(settings: TimerSettings): TimerSession {
    val items = buildCycle(settings)
    return TimerSession(
        items = items,
        currentIndex = 0,
        remainingSeconds = items.first().type.let { t ->
            when (t) {
                is SessionItemType.Focus -> t.seconds
                is SessionItemType.ShortBreak -> t.seconds
                is SessionItemType.LongBreak -> t.seconds
            }
        },
        timerState = TimerState.Idle
    )
}

enum class GlassReadabilityMode(val label: String) {
    Adaptive("Adaptive"),
    Clear("Clear"),
    Readable("Readable")
}

enum class GlassTreatment {
    Clear,
    Readable
}

fun glassTreatmentFor(
    mode: GlassReadabilityMode,
    time: LocalTime = LocalTime.now()
): GlassTreatment {
    return when (mode) {
        GlassReadabilityMode.Clear -> GlassTreatment.Clear
        GlassReadabilityMode.Readable -> GlassTreatment.Readable
        GlassReadabilityMode.Adaptive -> if (isBrightBackgroundTime(time)) {
            GlassTreatment.Readable
        } else {
            GlassTreatment.Clear
        }
    }
}

private fun isBrightBackgroundTime(time: LocalTime): Boolean {
    val hour = time.hour
    return hour in 5..16
}

fun formatSeconds(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

data class FocusStamp(
    val completed: Int = 0,
    val unfinished: Int = 0
)

fun addCompletedStamp(stamps: Map<LocalDate, FocusStamp>, date: LocalDate): Map<LocalDate, FocusStamp> {
    val existing = stamps[date] ?: FocusStamp()
    return stamps + (date to existing.copy(completed = existing.completed + 1))
}

fun addUnfinishedStamp(stamps: Map<LocalDate, FocusStamp>, date: LocalDate): Map<LocalDate, FocusStamp> {
    val existing = stamps[date] ?: FocusStamp()
    return stamps + (date to existing.copy(unfinished = existing.unfinished + 1))
}

enum class SessionStatus { Completed, Partial }

data class SessionRecord(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val completedFocusCount: Int,
    val totalFocusCount: Int,
    val status: SessionStatus,
    val taskName: String?
)

/**
 * Maps a [LocalTime] to the corresponding cat background drawable resource.
 *
 * Ranges:
 *  - Mid Night : 22:00 – 04:59
 *  - Dawn      : 05:00 – 07:59
 *  - Morning   : 08:00 – 10:59
 *  - Day       : 11:00 – 16:59
 *  - Evening   : 17:00 – 19:59
 *  - Night     : 20:00 – 21:59
 */
fun backgroundImageForTime(time: LocalTime = LocalTime.now()): Int {
    val hour = time.hour
    return when {
        hour >= 22 || hour < 5  -> R.drawable.cat_mid_night
        hour < 8                -> R.drawable.cat_dawn
        hour < 11               -> R.drawable.cat_morning
        hour < 17               -> R.drawable.cat_day
        hour < 20               -> R.drawable.cat_evening
        else                    -> R.drawable.cat_night  // 20–21
    }
}

/** Returns the segment duration in seconds, regardless of variant. */
fun SessionItemType.seconds(): Int = when (this) {
    is SessionItemType.Focus -> seconds
    is SessionItemType.ShortBreak -> seconds
    is SessionItemType.LongBreak -> seconds
}
