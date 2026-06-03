package com.endiar.purrodoro.ui

import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class JourneySummary(
    val weekFocusMinutes: Int,
    val previousWeekFocusMinutes: Int,
    val weekOverWeekPercent: Int?,
    val focusDays: Int,
    val dailyAverageMinutes: Int,
    val weeklyGoalMinutes: Int,
    val weeklyGoalPercent: Int,
    val weekBars: List<JourneyDayBar>,
    val selectedDay: JourneyDaySummary,
    val todayRows: List<JourneyTimelineRow>
)

data class JourneyDayBar(
    val date: LocalDate,
    val dayLabel: String,
    val focusMinutes: Int
)

data class JourneyDaySummary(
    val date: LocalDate,
    val dateLabel: String,
    val focusMinutes: Int,
    val focusTimeLabel: String,
    val focusSessions: Int
)

data class JourneyTimelineRow(
    val recordId: Long,
    val timeLabel: String,
    val durationLabel: String,
    val durationMinutes: Int,
    val statusLabel: String,
    val completed: Boolean,
    val title: String,
    val taskName: String?,
    val record: SessionRecord
)

fun journeySummary(
    records: List<SessionRecord>,
    selectedDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    weeklyGoalMinutes: Int = 12 * 60
): JourneySummary {
    val weekStart = today.with(DayOfWeek.MONDAY)
    val previousWeekStart = weekStart.minusWeeks(1)
    val recordsByDate = records.groupBy { it.startedDate(zone) }
    val weekBars = (0..6).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        JourneyDayBar(
            date = date,
            dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            focusMinutes = recordsByDate[date].orEmpty().focusMinutes()
        )
    }
    val weekFocusMinutes = weekBars.sumOf { it.focusMinutes }
    val previousWeekFocusMinutes = (0..6).sumOf { offset ->
        val date = previousWeekStart.plusDays(offset.toLong())
        recordsByDate[date].orEmpty().focusMinutes()
    }
    val focusDays = weekBars.count { it.focusMinutes > 0 }
    val dailyAverageMinutes = if (focusDays == 0) 0 else weekFocusMinutes / focusDays
    val weeklyGoalPercent = if (weeklyGoalMinutes <= 0) 0
        else ((weekFocusMinutes.toDouble() / weeklyGoalMinutes.toDouble()) * 100).roundToInt()
    val weekOverWeekPercent = if (previousWeekFocusMinutes <= 0) null else {
        (((weekFocusMinutes - previousWeekFocusMinutes).toDouble() / previousWeekFocusMinutes) * 100).roundToInt()
    }

    return JourneySummary(
        weekFocusMinutes = weekFocusMinutes,
        previousWeekFocusMinutes = previousWeekFocusMinutes,
        weekOverWeekPercent = weekOverWeekPercent,
        focusDays = focusDays,
        dailyAverageMinutes = dailyAverageMinutes,
        weeklyGoalMinutes = weeklyGoalMinutes,
        weeklyGoalPercent = weeklyGoalPercent.coerceIn(0, 999),
        weekBars = weekBars,
        selectedDay = daySummary(selectedDate, recordsByDate[selectedDate].orEmpty()),
        todayRows = recordsByDate[today].orEmpty()
            .sortedBy { it.startedAt }
            .map { it.toTimelineRow(zone) }
    )
}

/** Lifetime ("All time") rollups for the Journey hero card. */
data class JourneyAllTime(
    val totalFocusMinutes: Int,
    val totalFocusLabel: String,
    val bestStreakDays: Int,
    val totalSessions: Int,
)

/**
 * Aggregates every [SessionRecord] into lifetime totals.
 *
 * - [totalSessions] counts completed Focus segments (consistent with the rest
 *   of the Journey screen's "sessions" = focus count).
 * - [bestStreakDays] is the longest run of consecutive calendar days that each
 *   had at least one minute of focus.
 */
fun allTimeSummary(
    records: List<SessionRecord>,
    zone: ZoneId = ZoneId.systemDefault(),
): JourneyAllTime {
    val totalFocusMinutes = records.focusMinutes()
    val totalSessions = records.sumOf { it.completedFocusCount.coerceAtLeast(0) }
    val focusDates = records
        .groupBy { it.startedDate(zone) }
        .filterValues { it.focusMinutes() > 0 }
        .keys
        .sorted()
    return JourneyAllTime(
        totalFocusMinutes = totalFocusMinutes,
        totalFocusLabel = if (totalFocusMinutes >= 60) "${totalFocusMinutes / 60}h"
            else "${totalFocusMinutes.coerceAtLeast(0)}m",
        bestStreakDays = longestConsecutiveRun(focusDates),
        totalSessions = totalSessions,
    )
}

/**
 * Longest run of consecutive focused days that fall inside [month], derived
 * from the shared heatmap [intensities] map (level > 0 == focused).
 */
internal fun monthBestStreakDays(intensities: Map<LocalDate, Int>, month: java.time.YearMonth): Int {
    val days = intensities
        .filterValues { it > 0 }
        .keys
        .filter { java.time.YearMonth.from(it) == month }
        .sorted()
    return longestConsecutiveRun(days)
}

private fun longestConsecutiveRun(sortedDates: List<LocalDate>): Int {
    if (sortedDates.isEmpty()) return 0
    var best = 1
    var current = 1
    for (i in 1 until sortedDates.size) {
        current = if (sortedDates[i] == sortedDates[i - 1].plusDays(1)) current + 1 else 1
        best = maxOf(best, current)
    }
    return best
}

/** Per-month rollup driving the read-only "This month" heatmap on Journey. */
data class JourneyMonth(
    val focusDays: Int,
    val focusMinutes: Int,
    val focusTimeLabel: String,
)

fun monthSummary(
    records: List<SessionRecord>,
    month: java.time.YearMonth,
    zone: ZoneId = ZoneId.systemDefault(),
): JourneyMonth {
    val byDate = records.groupBy { it.startedDate(zone) }
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val focusMinutes = days.sumOf { byDate[it].orEmpty().focusMinutes() }
    val focusDays = days.count { byDate[it].orEmpty().focusMinutes() > 0 }
    return JourneyMonth(
        focusDays = focusDays,
        focusMinutes = focusMinutes,
        focusTimeLabel = journeyMinutesLabel(focusMinutes),
    )
}

/**
 * Maps each focused calendar date to a 1..4 intensity bucket, used to shade
 * the heatmap cells on the Journey month grid and the History calendar.
 * Days with no focus are absent from the map (treated as level 0).
 */
fun focusIntensitiesByDate(
    records: List<SessionRecord>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Int> =
    records.groupBy { it.startedDate(zone) }
        .mapNotNull { (date, recs) ->
            val minutes = recs.focusMinutes()
            if (minutes > 0) date to focusIntensityLevel(minutes) else null
        }
        .toMap()

/** Buckets a day's focus minutes into a 0..4 heatmap intensity level. */
fun focusIntensityLevel(minutes: Int): Int = when {
    minutes <= 0 -> 0
    minutes < 60 -> 1
    minutes < 120 -> 2
    minutes < 180 -> 3
    else -> 4
}

/** Full detail for a single day, shown on the History screen below the calendar. */
data class HistoryDayDetail(
    val date: LocalDate,
    val focusMinutes: Int,
    val focusTimeLabel: String,
    val focusSessions: Int,
    val restMinutes: Int,
    val restLabel: String,
    val rows: List<JourneyTimelineRow>,
)

fun historyDayDetail(
    records: List<SessionRecord>,
    date: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): HistoryDayDetail {
    val dayRecords = records.filter { it.startedDate(zone) == date }
    val focusMinutes = dayRecords.focusMinutes()
    val restMinutes = dayRecords.sumOf {
        it.completedShortBreakCount.coerceAtLeast(0) * it.shortBreakMinutes.coerceAtLeast(0) +
            it.completedLongBreakCount.coerceAtLeast(0) * it.longBreakMinutes.coerceAtLeast(0)
    }
    return HistoryDayDetail(
        date = date,
        focusMinutes = focusMinutes,
        focusTimeLabel = journeyMinutesLabel(focusMinutes),
        focusSessions = dayRecords.sumOf { it.completedFocusCount.coerceAtLeast(0) },
        restMinutes = restMinutes,
        restLabel = journeyMinutesLabel(restMinutes),
        rows = dayRecords.sortedBy { it.startedAt }.map { it.toTimelineRow(zone) },
    )
}

fun journeyMinutesLabel(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val mins = safeMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun daySummary(date: LocalDate, records: List<SessionRecord>): JourneyDaySummary {
    val focusMinutes = records.focusMinutes()
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return JourneyDaySummary(
        date = date,
        dateLabel = "$month ${date.dayOfMonth}",
        focusMinutes = focusMinutes,
        focusTimeLabel = journeyMinutesLabel(focusMinutes),
        focusSessions = records.sumOf { it.completedFocusCount.coerceAtLeast(0) }
    )
}

private fun List<SessionRecord>.focusMinutes(): Int =
    sumOf { it.completedFocusCount.coerceAtLeast(0) * it.focusMinutes.coerceAtLeast(0) }

private fun SessionRecord.startedDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()

private fun SessionRecord.toTimelineRow(zone: ZoneId): JourneyTimelineRow {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val start = Instant.ofEpochMilli(startedAt).atZone(zone)
    val focusMinutes = completedFocusCount.coerceAtLeast(0) * focusMinutes.coerceAtLeast(0)
    return JourneyTimelineRow(
        recordId = id,
        timeLabel = start.format(timeFormatter),
        durationLabel = "${focusMinutes.coerceAtLeast(0)} min",
        durationMinutes = focusMinutes.coerceAtLeast(0),
        statusLabel = if (status == SessionStatus.Completed) "Completed" else "Partial",
        completed = status == SessionStatus.Completed,
        title = taskName?.takeIf { it.isNotBlank() } ?: "Focus time",
        taskName = taskName,
        record = this
    )
}
