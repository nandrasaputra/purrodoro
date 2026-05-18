package com.endiar.kuropomodoro.model

import java.time.DayOfWeek
import java.time.LocalDate

// ─── Occurrence ──────────────────────────────────────────────────────────────

sealed class HabitOccurrence {
    object Daily : HabitOccurrence()
    data class Weekdays(val days: Set<DayOfWeek>) : HabitOccurrence() {
        init { require(days.isNotEmpty()) }
    }
}

// ─── Target type ─────────────────────────────────────────────────────────────

enum class HabitTargetType { Binary, Quantity, Pomodoro }

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
    val targetType: HabitTargetType = HabitTargetType.Binary,
    /**
     * Goal amount. For Binary: irrelevant (treated as 1). For Quantity: e.g. 4 glasses.
     * For Pomodoro: e.g. 2 focus segments.
     */
    val targetValue: Int = 1,
    val occurrence: HabitOccurrence = HabitOccurrence.Daily,
    val timeBlock: TimeBlock = TimeBlock.Anytime,
    val logs: Map<LocalDate, HabitLog> = emptyMap(),
)

// ─── Pure helpers ────────────────────────────────────────────────────────────

fun Habit.effectiveTarget(): Int = when (targetType) {
    HabitTargetType.Binary -> 1
    else -> targetValue.coerceAtLeast(1)
}

fun Habit.isScheduledOn(date: LocalDate): Boolean = when (val o = occurrence) {
    HabitOccurrence.Daily -> true
    is HabitOccurrence.Weekdays -> date.dayOfWeek in o.days
}

fun Habit.progressOn(date: LocalDate): Int {
    return when (val s = logs[date]?.state) {
        HabitLogState.Completed -> effectiveTarget()
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
 * Increment (or decrement if delta < 0) a Quantity/Pomodoro habit.
 * Going below 0 clears the log; reaching the target promotes to Completed.
 * Binary habits are treated as simple toggle-done / clear.
 */
fun Habit.incrementProgress(date: LocalDate, delta: Int = 1): Habit {
    if (targetType == HabitTargetType.Binary) {
        return if (delta > 0) withLog(date, HabitLogState.Completed) else withLog(date, null)
    }
    val target = effectiveTarget()
    val current = when (val s = logs[date]?.state) {
        HabitLogState.Completed -> target
        is HabitLogState.Partial -> s.progress
        HabitLogState.Skipped, null -> 0
    }
    val next = (current + delta).coerceAtLeast(0).coerceAtMost(target)
    val nextState: HabitLogState? = when {
        next <= 0 -> null
        next >= target -> HabitLogState.Completed
        else -> HabitLogState.Partial(next)
    }
    return withLog(date, nextState)
}

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
