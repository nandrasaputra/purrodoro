package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
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
    val statusLabel: String,
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
        statusLabel = if (status == SessionStatus.Completed) "Completed" else "Partial",
        title = taskName?.takeIf { it.isNotBlank() } ?: "Focus time",
        taskName = taskName,
        record = this
    )
}
