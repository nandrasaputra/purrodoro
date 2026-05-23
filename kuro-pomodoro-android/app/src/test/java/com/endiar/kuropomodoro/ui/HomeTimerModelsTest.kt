package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeTimerModelsTest {
    @Test
    fun greetingCopyUsesTimeOfDay() {
        assertEquals(
            HomeGreeting("Good morning,", "Let's make this morning meaningful."),
            homeGreetingFor(LocalTime.of(8, 0))
        )
        assertEquals(
            HomeGreeting("Good afternoon,", "Keep the afternoon moving gently."),
            homeGreetingFor(LocalTime.of(14, 0))
        )
        assertEquals(
            HomeGreeting("Good evening,", "Let's make this evening meaningful."),
            homeGreetingFor(LocalTime.of(20, 0))
        )
    }

    @Test
    fun todayFocusMetricsCountsOnlyRecordsEndedToday() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 5, 23)
        val yesterday = today.minusDays(1)
        val records = listOf(
            record(
                endedAt = millisFor(today, 9, 0, zone),
                focusMinutes = 25,
                completedFocusCount = 2,
                status = SessionStatus.Completed
            ),
            record(
                endedAt = millisFor(today, 11, 0, zone),
                focusMinutes = 25,
                completedFocusCount = 1,
                status = SessionStatus.Partial
            ),
            record(
                endedAt = millisFor(yesterday, 11, 0, zone),
                focusMinutes = 25,
                completedFocusCount = 4,
                status = SessionStatus.Completed
            )
        )

        val metrics = todayFocusMetrics(records, today, zone)

        assertEquals("1h 15m", metrics.focusTimeLabel)
        assertEquals("3", metrics.sessionsLabel)
        assertEquals("1", metrics.completedLabel)
    }

    @Test
    fun continueTaskPrefersActiveThenIncompleteTaskWithProgress() {
        val active = FocusTask(
            id = 2,
            title = "Compose Migration",
            minutes = 25,
            plannedSessions = 5,
            completedSessions = 2
        )
        val tasks = listOf(
            FocusTask(
                id = 1,
                title = "Inbox cleanup",
                minutes = 25,
                completed = false,
                plannedSessions = 3,
                completedSessions = 1
            )
        )

        assertEquals(active, continueTask(active, tasks))
        assertEquals(tasks.first(), continueTask(null, tasks))
    }

    @Test
    fun continueTaskReturnsNullWhenOnlyCompletedTasksExist() {
        val tasks = listOf(
            FocusTask(id = 1, title = "Done", minutes = 25, completed = true)
        )

        assertNull(continueTask(null, tasks))
    }

    @Test
    fun pomodoroProgressLabelUsesCurrentFocusPositionDuringCycle() {
        val settings = TimerSettings(focusSessionsPerCycle = 5)

        assertEquals(
            PomodoroProgress(2, 5, "Pomodoro 2 of 5", "2 / 5 Pomodoros completed"),
            pomodoroProgress(
                completedFocusSessions = 1,
                totalFocusSessions = 5,
                isStarted = true,
                task = null,
                settings = settings
            )
        )
        assertEquals(
            PomodoroProgress(2, 5, "Pomodoro 2 of 5", "2 / 5 Pomodoros completed"),
            pomodoroProgress(
                completedFocusSessions = 2,
                totalFocusSessions = 5,
                isStarted = false,
                task = FocusTask(
                    id = 7,
                    title = "Compose Migration",
                    minutes = 25,
                    plannedSessions = 5,
                    completedSessions = 2
                ),
                settings = settings
            )
        )
    }

    private fun record(
        endedAt: Long,
        focusMinutes: Int,
        completedFocusCount: Int,
        status: SessionStatus
    ): SessionRecord = SessionRecord(
        id = endedAt,
        startedAt = endedAt - 1_000L,
        endedAt = endedAt,
        focusMinutes = focusMinutes,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        completedFocusCount = completedFocusCount,
        totalFocusCount = 4,
        status = status,
        taskName = null
    )

    private fun millisFor(date: LocalDate, hour: Int, minute: Int, zone: ZoneId): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()
}
