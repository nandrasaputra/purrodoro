package com.endiar.kuropomodoro.model

import java.time.LocalDate

enum class AppTab(val label: String, val symbol: String) {
    Timer("Focus", "Home"),
    Todo("Todo", "Todo"),
    Calendar("Calendar", "Stamp"),
    Sounds("Sounds", "Sound"),
    Profile("Me", "Me")
}

enum class TimerMode(val label: String, val defaultSeconds: Int, val accentName: String) {
    Focus("Focus", 25 * 60, "peach"),
    ShortBreak("Short Break", 5 * 60, "mint"),
    LongBreak("Long Break", 15 * 60, "lavender");

    fun formatDuration(): String = formatSeconds(defaultSeconds)
}

data class FocusTask(
    val id: Int,
    val title: String,
    val minutes: Int,
    val completed: Boolean = false
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
    val longBreakMinutes: Int = 15
) {
    fun secondsFor(mode: TimerMode): Int = when (mode) {
        TimerMode.Focus -> focusMinutes * 60
        TimerMode.ShortBreak -> shortBreakMinutes * 60
        TimerMode.LongBreak -> longBreakMinutes * 60
    }

    fun update(mode: TimerMode, minutes: Int): TimerSettings {
        val safeMinutes = minutes.coerceAtLeast(1)
        return when (mode) {
            TimerMode.Focus -> copy(focusMinutes = safeMinutes)
            TimerMode.ShortBreak -> copy(shortBreakMinutes = safeMinutes)
            TimerMode.LongBreak -> copy(longBreakMinutes = safeMinutes)
        }
    }
}

fun formatSeconds(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun addFocusStamp(stamps: Map<LocalDate, Int>, date: LocalDate): Map<LocalDate, Int> {
    return stamps + (date to ((stamps[date] ?: 0) + 1))
}
