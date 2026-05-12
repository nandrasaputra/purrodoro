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
    val longBreakMinutes: Int = 15
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

fun addFocusStamp(stamps: Map<LocalDate, Int>, date: LocalDate): Map<LocalDate, Int> {
    return stamps + (date to ((stamps[date] ?: 0) + 1))
}

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
