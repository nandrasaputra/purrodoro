package com.endiar.purrodoro.ui

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.HabitLog
import com.endiar.purrodoro.model.HabitLogState
import com.endiar.purrodoro.model.InterruptedFocus
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.buildCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun homeStartTaskDoesNotAutoBindContinueTask() {
        val continueTask = FocusTask(
            id = 1,
            title = "Inbox cleanup",
            minutes = 25,
            completed = false,
            plannedSessions = 3,
            completedSessions = 1
        )

        assertNull(homeStartTask(activeTask = null, continueTask = continueTask))
        assertEquals(continueTask, homeStartTask(activeTask = continueTask, continueTask = null))
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
        assertEquals(
            PomodoroProgress(3, 5, "Pomodoro 3 of 5", "3 / 5 Pomodoros completed"),
            pomodoroProgress(
                completedFocusSessions = 0,
                totalFocusSessions = 5,
                isStarted = true,
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
        assertEquals(
            PomodoroProgress(2, 5, "Pomodoro 2 of 5", "2 / 5 Pomodoros completed"),
            pomodoroProgress(
                completedFocusSessions = 1,
                totalFocusSessions = 5,
                isStarted = true,
                task = FocusTask(
                    id = 7,
                    title = "Compose Migration",
                    minutes = 25,
                    plannedSessions = 5,
                    completedSessions = 0
                ),
                settings = settings
            )
        )
    }

    @Test
    fun homeSuggestionResumesInterruptedTaskThatIsStillOpen() {
        val today = LocalDate.of(2026, 5, 30)
        val tasks = listOf(FocusTask(id = 4, title = "Write product spec", minutes = 25))
        val interrupted = InterruptedFocus(
            focusType = FocusType.Task,
            taskId = 4,
            habitId = null,
            title = "Write product spec",
            remainingSeconds = 12 * 60,
        )

        val result = homeSuggestion(interrupted, tasks, emptyList(), today, seed = today.toEpochDay())

        assertEquals(
            HomeSuggestion.Continue(FocusType.Task, "Write product spec", 12 * 60, taskId = 4, habitId = null),
            result
        )
    }

    @Test
    fun homeSuggestionResumesRemainingFocusBlock() {
        val today = LocalDate.of(2026, 5, 30)
        val tasks = listOf(FocusTask(id = 4, title = "Write product spec", minutes = 25))
        val interrupted = InterruptedFocus(
            focusType = FocusType.Task,
            taskId = 4,
            habitId = null,
            title = "Write product spec",
            remainingSeconds = 0,
            remainingFocusSessions = 2,
            focusMinutes = 25,
        )

        val result = homeSuggestion(interrupted, tasks, emptyList(), today, seed = today.toEpochDay())

        assertEquals(
            HomeSuggestion.Continue(
                FocusType.Task,
                "Write product spec",
                remainingSeconds = 0,
                taskId = 4,
                habitId = null,
                remainingFocusSessions = 2,
                focusMinutes = 25,
            ),
            result
        )
    }

    @Test
    fun homeSuggestionIgnoresInterruptedTaskOnceCompletedAndFallsBack() {
        val today = LocalDate.of(2026, 5, 30)
        val tasks = listOf(
            FocusTask(id = 4, title = "Write product spec", minutes = 25, completed = true),
            FocusTask(id = 5, title = "Review PR", minutes = 25),
        )
        val interrupted = InterruptedFocus(
            focusType = FocusType.Task,
            taskId = 4,
            habitId = null,
            title = "Write product spec",
            remainingSeconds = 12 * 60,
        )

        val result = homeSuggestion(interrupted, tasks, emptyList(), today, seed = today.toEpochDay())

        // Interrupted task is done, so we fall through to suggesting the open one.
        assertEquals(
            HomeSuggestion.Suggest(FocusType.Task, "Review PR", taskId = 5, habitId = null),
            result
        )
    }

    @Test
    fun homeSuggestionIsEmptyWhenNothingToContinueOrSuggest() {
        val today = LocalDate.of(2026, 5, 30)
        val tasks = listOf(FocusTask(id = 4, title = "Done", minutes = 25, completed = true))

        assertEquals(
            HomeSuggestion.Empty,
            homeSuggestion(null, tasks, emptyList(), today, seed = today.toEpochDay())
        )
    }

    @Test
    fun homeSuggestionSkipsHabitAlreadyMetToday() {
        val today = LocalDate.of(2026, 5, 30)
        val habit = Habit(
            id = 2,
            name = "Read",
            emoji = "📚",
            colorArgb = 0xFFFFFFFF,
            createdAt = 0L,
            logs = mapOf(today to HabitLog(HabitLogState.Completed)),
        )
        val interrupted = InterruptedFocus(
            focusType = FocusType.Habit,
            taskId = null,
            habitId = 2,
            title = "📚 Read",
            remainingSeconds = 8 * 60,
        )

        // Habit target met -> not resumable, and no other candidates.
        assertEquals(
            HomeSuggestion.Empty,
            homeSuggestion(interrupted, emptyList(), listOf(habit), today, seed = today.toEpochDay())
        )
    }

    @Test
    fun homeSuggestionPickIsStableForSameSeed() {
        val today = LocalDate.of(2026, 5, 30)
        val tasks = (1..6).map { FocusTask(id = it, title = "Task $it", minutes = 25) }

        val first = homeSuggestion(null, tasks, emptyList(), today, seed = 99L)
        val second = homeSuggestion(null, tasks, emptyList(), today, seed = 99L)

        assertEquals(first, second)
        assertTrue(first is HomeSuggestion.Suggest)
    }

    @Test
    fun interruptedRemainingMinutesRoundsUp() {
        assertEquals(1, interruptedRemainingMinutes(1))
        assertEquals(1, interruptedRemainingMinutes(60))
        assertEquals(2, interruptedRemainingMinutes(61))
        assertEquals(13, interruptedRemainingMinutes(12 * 60 + 1))
    }

    @Test
    fun remainingFocusSessionsAfterCurrentCountsFutureFocusSegmentsOnly() {
        val cycle = buildCycle(TimerSettings(focusSessionsPerCycle = 4))

        // Focus, short break, Focus, short break, Focus, short break, Focus, long break.
        assertEquals(2, remainingFocusSessionsAfterCurrent(cycle, currentIndex = 3))
        assertEquals(0, remainingFocusSessionsAfterCurrent(cycle, currentIndex = 7))
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
