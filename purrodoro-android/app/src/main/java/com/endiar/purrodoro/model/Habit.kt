package com.endiar.purrodoro.model

import java.time.DayOfWeek
import java.time.LocalDate

// ─── Occurrence ──────────────────────────────────────────────────────────────

sealed class HabitOccurrence {
    object Daily : HabitOccurrence()
    data class Weekdays(val days: Set<DayOfWeek>) : HabitOccurrence() {
        init { require(days.isNotEmpty()) }
    }
}

// ─── Time-of-day grouping cue ─────────────────────────────────────────────────

enum class TimeBlock(val label: String) {
    Morning("Morning"),
    Afternoon("Afternoon"),
    Evening("Evening"),
    Anytime("Anytime"),
}

// ─── Per-day log state ───────────────────────────────────────────────────────

sealed class HabitLogState {
    object Completed : HabitLogState()
    data class Partial(val progress: Int) : HabitLogState()
    /**
     * Intentional pause (sick day / travel). Transparent to the streak engine:
     * treated exactly like an unscheduled day — neither extends nor breaks the streak.
     */
    object Skipped : HabitLogState()
}

data class HabitLog(val state: HabitLogState)

// ─── Core data class ─────────────────────────────────────────────────────────

data class Habit(
    val id: Int,
    val name: String,
    val emoji: String,
    /** Packed ARGB as Long (0xAARRGGBB). Stored/restored via toInt() for Color(). */
    val colorArgb: Long,
    val createdAt: Long,
    val occurrence: HabitOccurrence = HabitOccurrence.Daily,
    val timeBlock: TimeBlock = TimeBlock.Anytime,
    val logs: Map<LocalDate, HabitLog> = emptyMap(),
)

// ─── Pure helpers ────────────────────────────────────────────────────────────

/**
 * Habits are check-off (binary) only, so the effective per-day target is always 1.
 * Kept as a function so callers (timer integration, start-focus sheet) read clearly.
 */
fun Habit.effectiveTarget(): Int = 1

fun Habit.isScheduledOn(date: LocalDate): Boolean = when (val o = occurrence) {
    HabitOccurrence.Daily -> true
    is HabitOccurrence.Weekdays -> date.dayOfWeek in o.days
}

fun Habit.progressOn(date: LocalDate): Int {
    return when (val s = logs[date]?.state) {
        HabitLogState.Completed -> effectiveTarget()
        // Partial only survives in legacy data from the old quantity/pomodoro types;
        // any positive progress counts toward the binary target of 1.
        is HabitLogState.Partial -> s.progress
        HabitLogState.Skipped, null -> 0
    }
}

fun Habit.isTargetMetOn(date: LocalDate): Boolean =
    logs[date]?.state !is HabitLogState.Skipped &&
            progressOn(date) >= effectiveTarget()

fun Habit.isSkippedOn(date: LocalDate): Boolean =
    logs[date]?.state is HabitLogState.Skipped

fun Habit.hasLogOn(date: LocalDate): Boolean = logs.containsKey(date)

fun Habit.withLog(date: LocalDate, state: HabitLogState?): Habit {
    val m = logs.toMutableMap()
    if (state == null) m.remove(date) else m[date] = HabitLog(state)
    return copy(logs = m)
}

fun Habit.toggleBinaryOn(date: LocalDate): Habit {
    val next = if (logs[date]?.state is HabitLogState.Completed) null
               else HabitLogState.Completed
    return withLog(date, next)
}

/**
 * Mark the habit done (delta > 0) or clear it (delta <= 0) for [date].
 *
 * Habits are check-off only, so any positive delta completes the day. Kept
 * delta-based so the timer integration (which credits +1 on focus completion)
 * keeps working unchanged.
 */
fun Habit.incrementProgress(date: LocalDate, delta: Int = 1): Habit =
    if (delta > 0) withLog(date, HabitLogState.Completed) else withLog(date, null)

/**
 * Current consecutive *scheduled* days ending today. Skipped and unscheduled days
 * are transparent to the streak (they neither extend nor break it). If today is
 * scheduled but not yet met, the walk starts from yesterday so the displayed number
 * doesn't drop until the day is actually over.
 */
fun Habit.currentStreak(today: LocalDate): Int {
    if (logs.isEmpty()) return 0
    val startFromYesterday = isScheduledOn(today) && !isTargetMetOn(today)
    var cursor = if (startFromYesterday) today.minusDays(1) else today
    var count = 0
    val limit = today.minusDays(365L * 10)
    while (cursor.isAfter(limit)) {
        if (!isScheduledOn(cursor)) { cursor = cursor.minusDays(1); continue }
        if (isSkippedOn(cursor))     { cursor = cursor.minusDays(1); continue }
        if (isTargetMetOn(cursor)) { count++; cursor = cursor.minusDays(1) }
        else break
    }
    return count
}

/**
 * Longest run of completed scheduled days across the habit's entire lifetime.
 * Skipped / unscheduled days are transparent.
 */
fun Habit.bestStreak(today: LocalDate): Int {
    if (logs.isEmpty()) return 0
    val start = logs.keys.min()
    var cursor = start
    var run = 0; var best = 0
    while (!cursor.isAfter(today)) {
        when {
            !isScheduledOn(cursor) -> Unit
            isSkippedOn(cursor)    -> Unit
            isTargetMetOn(cursor)  -> { run++; if (run > best) best = run }
            else                   -> run = 0
        }
        cursor = cursor.plusDays(1)
    }
    return best
}

fun streakLabel(days: Int): String = when {
    days <= 0 -> "Every day"
    days < 7  -> if (days == 1) "1 day" else "$days days"
    else      -> { val w = days / 7; if (w == 1) "1 week" else "$w weeks" }
}

fun trailingDays(today: LocalDate, count: Int): List<LocalDate> =
    (count - 1 downTo 0).map { today.minusDays(it.toLong()) }

/**
 * Group habits into Morning → Afternoon → Evening → Anytime order,
 * dropping empty buckets.
 */
fun groupByTimeBlock(habits: List<Habit>): List<Pair<TimeBlock, List<Habit>>> {
    val order = listOf(TimeBlock.Morning, TimeBlock.Afternoon, TimeBlock.Evening, TimeBlock.Anytime)
    val grouped = habits.groupBy { it.timeBlock }
    return order.mapNotNull { b ->
        val list = grouped[b].orEmpty()
        if (list.isEmpty()) null else b to list
    }
}
