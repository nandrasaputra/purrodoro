package com.endiar.purrodoro.ui

import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class JourneyModelsTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 5, 24)
    private val selectedDate = LocalDate.of(2026, 5, 18)

    @Test
    fun journeySummaryBuildsWeeklyMetricsFromSessionRecords() {
        val records = listOf(
            record(selectedDate, 9, 0, focusMinutes = 25, completedFocusCount = 2),
            record(selectedDate, 14, 0, focusMinutes = 25, completedFocusCount = 1),
            record(LocalDate.of(2026, 5, 19), 11, 0, focusMinutes = 25, completedFocusCount = 1),
            record(LocalDate.of(2026, 5, 20), 10, 0, focusMinutes = 25, completedFocusCount = 2),
            record(today.minusWeeks(1), 10, 0, focusMinutes = 25, completedFocusCount = 10)
        )

        val summary = journeySummary(
            records = records,
            selectedDate = selectedDate,
            today = today,
            zone = zone,
            weeklyGoalMinutes = 12 * 60
        )

        assertEquals(150, summary.weekFocusMinutes)
        assertEquals(3, summary.focusDays)
        assertEquals(50, summary.dailyAverageMinutes)
        assertEquals(21, summary.weeklyGoalPercent)
        assertEquals(250, summary.previousWeekFocusMinutes)
        assertEquals(-40, summary.weekOverWeekPercent)
        assertEquals(7, summary.weekBars.size)
        assertEquals(listOf(75, 25, 50, 0, 0, 0, 0), summary.weekBars.map { it.focusMinutes })
    }

    @Test
    fun journeySummaryBuildsSelectedDayAndTimelineRowsWithoutActivityTypes() {
        val records = listOf(
            record(selectedDate, 9, 0, focusMinutes = 25, completedFocusCount = 1, taskName = "Design polish"),
            record(selectedDate, 14, 0, focusMinutes = 50, completedFocusCount = 2, status = SessionStatus.Partial),
            record(today, 9, 0, focusMinutes = 25, completedFocusCount = 1, taskName = "Inbox cleanup"),
            record(today, 21, 0, focusMinutes = 45, completedFocusCount = 1)
        )

        val summary = journeySummary(
            records = records,
            selectedDate = selectedDate,
            today = today,
            zone = zone,
            weeklyGoalMinutes = 12 * 60
        )

        assertEquals("May 18", summary.selectedDay.dateLabel)
        assertEquals("2h 5m", summary.selectedDay.focusTimeLabel)
        assertEquals(3, summary.selectedDay.focusSessions)
        assertEquals(2, summary.todayRows.size)
        assertEquals("9:00 AM", summary.todayRows.first().timeLabel)
        assertEquals("25 min", summary.todayRows.first().durationLabel)
        assertEquals("Completed", summary.todayRows.first().statusLabel)
        assertEquals("Inbox cleanup", summary.todayRows.first().taskName)
        assertFalse(summary.todayRows.any { row ->
            row.title.contains("Pomodoro", ignoreCase = true) ||
                row.title.contains("Deep Work", ignoreCase = true) ||
                row.title.contains("Study", ignoreCase = true) ||
                row.title.contains("Night Focus", ignoreCase = true)
        })
    }

    private fun record(
        date: LocalDate,
        hour: Int,
        minute: Int,
        focusMinutes: Int,
        completedFocusCount: Int,
        status: SessionStatus = SessionStatus.Completed,
        taskName: String? = null
    ): SessionRecord {
        val startedAt = ZonedDateTime.of(date, LocalTime.of(hour, minute), zone)
            .toInstant()
            .toEpochMilli()
        val durationMinutes = focusMinutes * completedFocusCount
        return SessionRecord(
            id = startedAt,
            startedAt = startedAt,
            endedAt = startedAt + durationMinutes * 60_000L,
            focusMinutes = focusMinutes,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            completedFocusCount = completedFocusCount,
            totalFocusCount = 4,
            status = status,
            taskName = taskName
        )
    }
}
