package com.endiar.kuropomodoro.widget

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetStateTest {

    private val sampleSession = TimerSession(
        items = listOf(TimerSessionItem(SessionItemType.Focus(60))),
        currentIndex = 0,
        remainingSeconds = 60,
        timerState = TimerState.Playing,
    )

    private fun runtime(state: TimerState) = TimerRuntimeState(
        session = sampleSession.copy(timerState = state),
        endsAtMillis = if (state == TimerState.Playing) 1_000_000L else null,
        activeTask = null,
        openRecord = null,
        activeHabitId = null,
        focusType = FocusType.Empty,
    )

    @Test
    fun `quickStart returns Ready when no cycle and no pending ack`() {
        val state = WidgetState.quickStart(runtime = null, pendingCycleAck = false)
        assertEquals(QuickStartMood.Ready, state.mood)
    }

    @Test
    fun `quickStart returns Celebrating when pending ack is set`() {
        val state = WidgetState.quickStart(runtime = null, pendingCycleAck = true)
        assertEquals(QuickStartMood.Celebrating, state.mood)
    }

    @Test
    fun `quickStart returns Focusing when timer is Playing`() {
        val state = WidgetState.quickStart(runtime = runtime(TimerState.Playing), pendingCycleAck = false)
        assertEquals(QuickStartMood.Focusing, state.mood)
    }

    @Test
    fun `quickStart returns Waiting when timer is Paused`() {
        val state = WidgetState.quickStart(runtime = runtime(TimerState.Paused), pendingCycleAck = false)
        assertEquals(QuickStartMood.Waiting, state.mood)
    }

    @Test
    fun `today widget gives soft start copy at zero focus`() {
        val state = WidgetState.today(
            focusCountToday = 0,
            settings = TimerSettings(focusSessionsPerCycle = 4),
        )
        assertEquals(0, state.focusCount)
        assertEquals(TodayEncouragement.SoftStart, state.encouragement)
    }

    @Test
    fun `today widget warms up at one focus`() {
        val state = WidgetState.today(
            focusCountToday = 1,
            settings = TimerSettings(focusSessionsPerCycle = 4),
        )
        assertEquals(1, state.focusCount)
        assertEquals(TodayEncouragement.FirstFocus, state.encouragement)
    }

    @Test
    fun `today widget reaches goal copy at or past target`() {
        val state = WidgetState.today(
            focusCountToday = 7,
            settings = TimerSettings(focusSessionsPerCycle = 4),
        )
        assertEquals(7, state.focusCount)
        assertEquals(TodayEncouragement.GoalReached, state.encouragement)
    }

    @Test
    fun `today widget respects smaller cycle settings for goal copy`() {
        val state = WidgetState.today(
            focusCountToday = 2,
            settings = TimerSettings(focusSessionsPerCycle = 2),
        )
        assertEquals(2, state.focusCount)
        assertEquals(TodayEncouragement.GoalReached, state.encouragement)
    }

    // ─── deriveData / nudge priority ──────────────────────────────────────────

    private val today: LocalDate = LocalDate.of(2026, 6, 1)

    private fun habit(
        id: Int = 1,
        name: String = "Meditate",
        emoji: String = "\uD83E\uDDD8",
        occurrence: HabitOccurrence = HabitOccurrence.Daily,
        logs: Map<LocalDate, com.endiar.kuropomodoro.model.HabitLog> = emptyMap(),
    ) = Habit(
        id = id,
        name = name,
        emoji = emoji,
        colorArgb = 0xFFFF7F73,
        createdAt = 0L,
        occurrence = occurrence,
        logs = logs,
    )

    private fun task(
        id: Int = 1,
        title: String = "Finish report",
        completed: Boolean = false,
        dueDate: LocalDate? = null,
    ) = FocusTask(id = id, title = title, minutes = 25, completed = completed, dueDate = dueDate)

    private fun derive(
        stamps: Map<LocalDate, Int> = emptyMap(),
        settings: TimerSettings = TimerSettings(focusSessionsPerCycle = 4),
        habits: List<Habit> = emptyList(),
        tasks: List<FocusTask> = emptyList(),
        runtime: TimerRuntimeState? = null,
        pendingCycleAck: Boolean = false,
    ) = WidgetState.deriveData(
        stamps = stamps,
        settings = settings,
        habits = habits,
        tasks = tasks,
        runtime = runtime,
        pendingCycleAck = pendingCycleAck,
        today = today,
    )

    @Test
    fun `nudge is Focusing while timer is playing`() {
        val data = derive(runtime = runtime(TimerState.Playing))
        assertEquals(NudgeKind.Focusing, data.nudge.kind)
        assertEquals(NudgeTarget.OpenTimer, data.nudge.target)
    }

    @Test
    fun `nudge is Paused while timer is paused`() {
        val data = derive(runtime = runtime(TimerState.Paused))
        assertEquals(NudgeKind.Paused, data.nudge.kind)
    }

    @Test
    fun `nudge is Celebrating when a cycle ack is pending`() {
        val data = derive(pendingCycleAck = true)
        assertEquals(NudgeKind.Celebrating, data.nudge.kind)
        assertEquals(NudgeTarget.OpenApp, data.nudge.target)
    }

    @Test
    fun `nudge is GoalReached once today meets the focus target`() {
        val data = derive(
            stamps = mapOf(today to 4),
            settings = TimerSettings(focusSessionsPerCycle = 4),
        )
        assertEquals(NudgeKind.GoalReached, data.nudge.kind)
        assertTrue(data.goalReached)
    }

    @Test
    fun `nudge protects a live streak when today has no focus yet`() {
        val data = derive(
            stamps = mapOf(today.minusDays(1) to 2, today.minusDays(2) to 1),
        )
        assertEquals(NudgeKind.StreakAtRisk, data.nudge.kind)
        assertEquals(NudgeTarget.StartFocus, data.nudge.target)
        assertEquals(2, data.streak)
        assertTrue(data.streakAtRisk)
        assertTrue(data.nudge.title.contains("2-day"))
    }

    @Test
    fun `nudge surfaces a due habit when no streak is at risk`() {
        val data = derive(habits = listOf(habit(name = "Stretch")))
        assertEquals(NudgeKind.HabitDue, data.nudge.kind)
        assertEquals(NudgeTarget.OpenHabits, data.nudge.target)
        assertTrue(data.nudge.title.contains("Stretch"))
        assertEquals(1, data.pendingHabits.size)
    }

    @Test
    fun `nudge surfaces a due task when no habit is pending`() {
        val data = derive(tasks = listOf(task(title = "Email Sam", dueDate = today)))
        assertEquals(NudgeKind.TaskDue, data.nudge.kind)
        assertEquals(NudgeTarget.OpenTasks, data.nudge.target)
        assertTrue(data.nudge.title.contains("Email Sam"))
    }

    @Test
    fun `nudge falls back to a fresh focus invitation`() {
        val data = derive()
        assertEquals(NudgeKind.FreshFocus, data.nudge.kind)
        assertEquals(NudgeTarget.StartFocus, data.nudge.target)
    }

    @Test
    fun `a habit outranks a task in the nudge priority`() {
        val data = derive(
            habits = listOf(habit(name = "Walk")),
            tasks = listOf(task(title = "Pay bills", dueDate = today)),
        )
        assertEquals(NudgeKind.HabitDue, data.nudge.kind)
    }

    @Test
    fun `completed habits and tasks are not pending`() {
        val data = derive(
            habits = listOf(
                habit(id = 1, name = "Done", logs = mapOf(today to com.endiar.kuropomodoro.model.HabitLog(com.endiar.kuropomodoro.model.HabitLogState.Completed))),
            ),
            tasks = listOf(task(id = 1, title = "Done task", completed = true)),
        )
        assertTrue(data.pendingHabits.isEmpty())
        assertEquals(0, data.pendingTasksCount)
        assertFalse(data.nudge.kind == NudgeKind.HabitDue)
    }

    @Test
    fun `planChips lists habits before tasks and respects the max`() {
        val data = derive(
            habits = listOf(habit(id = 1, name = "H1"), habit(id = 2, name = "H2")),
            tasks = listOf(task(id = 1, title = "T1"), task(id = 2, title = "T2")),
        )
        val chips = data.planChips(max = 3)
        assertEquals(3, chips.size)
        assertEquals("H1", chips[0].label)
        assertEquals("H2", chips[1].label)
        assertEquals("T1", chips[2].label)
    }
}
