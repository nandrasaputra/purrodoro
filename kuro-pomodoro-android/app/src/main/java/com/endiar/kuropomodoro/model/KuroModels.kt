package com.endiar.kuropomodoro.model

import com.endiar.kuropomodoro.R
import java.time.LocalDate
import java.time.LocalTime

enum class AppTab(val label: String, val symbol: String) {
    Timer("Focus", "Home"),
    Todo("Tasks", "Tasks"),
    Calendar("Journey", "Journey"),
    Habits("Habits", "Habits")
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

enum class TaskPriority(val label: String) {
    Low("Low"),
    Normal("Normal"),
    High("High");

    /** Single-character tag used in persistence to keep the storage format compact. */
    val storageTag: String
        get() = when (this) {
            Low -> "L"
            Normal -> "N"
            High -> "H"
        }

    companion object {
        fun fromStorageTag(tag: String?): TaskPriority = when (tag) {
            "L" -> Low
            "H" -> High
            else -> Normal
        }
    }
}

data class FocusTask(
    val id: Int,
    val title: String,
    val minutes: Int,
    val completed: Boolean = false,
    val plannedSessions: Int = 1,
    val completedSessions: Int = 0,
    val categoryId: Int? = null,
    val priority: TaskPriority = TaskPriority.Normal,
    val dueDate: LocalDate? = null
) {
    fun toggleCompleted(): FocusTask = copy(completed = !completed)
}

/**
 * A task category. [colorArgb] is the user-picked color (packed ARGB stored as a
 * Long). When null — older categories created before the color picker — callers
 * fall back to the deterministic palette in `categoryColorFor`.
 */
data class Category(val id: Int, val name: String, val colorArgb: Long? = null)

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

    fun updateCycleSize(n: Int): TimerSettings = copy(focusSessionsPerCycle = n.coerceIn(1, MAX_FOCUS_SESSIONS_PER_CYCLE))

    companion object {
        /**
         * Hard cap for [focusSessionsPerCycle]. Aligns the in-app stepper, the
         * Today widget's dot row, and the persisted snapshot clamp so the
         * dot count never overflows the widget's fixed 4-dot layout.
         */
        const val MAX_FOCUS_SESSIONS_PER_CYCLE: Int = 4
    }
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

/** The inputs needed to render cycle progress dots in the UI. */
data class CycleDotsState(
    val totalFocusSessions: Int,
    val completedFocusSessions: Int,
    val isCurrentlyFocus: Boolean
)

/**
 * Derives [CycleDotsState] from the active cycle items and index.
 *
 * Falls back to [settings.focusSessionsPerCycle] when [cycleItems] is empty (idle state),
 * so the dots are always visible regardless of timer state.
 *
 * [timerState] gates the "currently focus" highlight: when the timer is [TimerState.Idle]
 * (e.g. after a natural finish or skip-final reset, where [cycleItems] are still populated
 * but [cycleIndex] has been reset to 0), no dot is highlighted as the active segment.
 */
fun cycleDotsState(
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    timerState: TimerState,
    settings: TimerSettings
): CycleDotsState {
    val focusItemIndices = cycleItems.mapIndexedNotNull { i, item ->
        if (item.type is SessionItemType.Focus) i else null
    }
    val totalFocusSessions = if (focusItemIndices.isNotEmpty()) focusItemIndices.size
                             else settings.focusSessionsPerCycle
    val completedFocusSessions = focusItemIndices.count { it < cycleIndex }
    val isCurrentlyFocus = timerState != TimerState.Idle &&
        cycleItems.getOrNull(cycleIndex)?.type is SessionItemType.Focus
    return CycleDotsState(totalFocusSessions, completedFocusSessions, isCurrentlyFocus)
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

enum class AppearanceMode(val label: String) {
    Light("Light"),
    Dark("Dark"),
    Auto("Auto");

    /**
     * Resolves the user's choice into a concrete dark/light decision.
     * `Auto` defers to [systemInDark]; explicit modes ignore it.
     */
    fun isDark(systemInDark: Boolean): Boolean = when (this) {
        Light -> false
        Dark -> true
        Auto -> systemInDark
    }
}

/**
 * In-app UI language. [tag] is a BCP-47 language tag used to build the
 * localized [java.util.Locale]; `null` means "follow the system locale"
 * (no override — resources resolve against the device configuration).
 *
 * [label] is the language's endonym, shown verbatim in the picker regardless
 * of the currently active locale (the conventional UX for language menus).
 */
enum class AppLanguage(val label: String, val tag: String?) {
    System("System", null),
    English("English", "en"),
    Indonesian("Bahasa Indonesia", "id");

    companion object {
        fun fromName(name: String?): AppLanguage =
            entries.firstOrNull { it.name == name } ?: System
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

enum class FocusType { Task, Habit, SimpleNote, Empty }

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

/**
 * Returns true when a Stop action should record a focus stamp for "today".
 *
 * Spec: stopping while a Focus segment is in progress (Playing or Paused) with time
 * still remaining should be acknowledged in the daily stamp counter, so partially-
 * worked focus periods are not lost from records / streaks.
 *
 * Returns false when:
 *  - the timer is Idle (nothing in progress)
 *  - the current segment is a break
 *  - remainingSeconds == 0 (the segment is effectively complete; the natural-finish
 *    path will stamp via TimerStateMachine — avoid double counting)
 */
fun shouldStampStopAsFocus(session: TimerSession): Boolean {
    if (session.timerState == TimerState.Idle) return false
    val current = session.currentItem ?: return false
    return current.type is SessionItemType.Focus && session.remainingSeconds > 0
}

enum class SessionStatus { Completed, Partial }

/**
 * A focus segment that was stopped before its time ran out while a real task or
 * habit was attached. Captured at Stop so Home can offer a calm "continue where
 * you were interrupted" entry point: the user resumes the same intention and
 * adjusts the leftover time in the Start Focus confirmation sheet.
 *
 * Only Task/Habit focus is worth remembering — Empty / Simple-note focus has no
 * durable intent to return to, so it is never captured here.
 */
data class InterruptedFocus(
    val focusType: FocusType,
    val taskId: Int?,
    val habitId: Int?,
    val title: String,
    val remainingSeconds: Int,
    val remainingFocusSessions: Int = 0,
    val focusMinutes: Int? = null,
)

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
    val taskName: String?,
    val completedShortBreakCount: Int = 0,
    val completedLongBreakCount: Int = 0
)

/** Returns the segment duration in seconds, regardless of variant. */
fun SessionItemType.seconds(): Int = when (this) {
    is SessionItemType.Focus -> seconds
    is SessionItemType.ShortBreak -> seconds
    is SessionItemType.LongBreak -> seconds
}

/**
 * Counts the number of consecutive days (ending today) that have at least one session.
 *
 * Convention: if today has no stamp yet, we look backwards from yesterday so
 * an in-progress day does not reset a legitimate streak.
 * The result is the count of that contiguous run, plus 1 if today also has a stamp.
 *
 * Examples:
 *   stamps on Mon, Tue, Wed; today = Thu with no stamp → streak = 3
 *   stamps on Mon, Tue, Wed; today = Thu with a stamp  → streak = 4
 *   no stamps at all                                    → streak = 0
 */
fun calculateStreak(stamps: Map<LocalDate, Int>, today: LocalDate): Int {
    val todayHasStamp = (stamps[today] ?: 0) > 0
    var streak = if (todayHasStamp) 1 else 0
    var date = today.minusDays(1)
    while ((stamps[date] ?: 0) > 0) {
        streak++
        date = date.minusDays(1)
    }
    return streak
}
