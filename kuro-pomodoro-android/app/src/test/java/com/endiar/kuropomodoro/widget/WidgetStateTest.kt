package com.endiar.kuropomodoro.widget

import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Test

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
        assertEquals("Kuro is ready", state.title)
        assertEquals("Start Focus", state.actionLabel)
    }

    @Test
    fun `quickStart returns Celebrating when pending ack is set`() {
        val state = WidgetState.quickStart(runtime = null, pendingCycleAck = true)
        assertEquals(QuickStartMood.Celebrating, state.mood)
        assertEquals("Nice work today", state.title)
        assertEquals("Open Kuro", state.actionLabel)
    }

    @Test
    fun `quickStart returns Focusing when timer is Playing`() {
        val state = WidgetState.quickStart(runtime = runtime(TimerState.Playing), pendingCycleAck = false)
        assertEquals(QuickStartMood.Focusing, state.mood)
        assertEquals("Open Timer", state.actionLabel)
    }

    @Test
    fun `quickStart returns Waiting when timer is Paused`() {
        val state = WidgetState.quickStart(runtime = runtime(TimerState.Paused), pendingCycleAck = false)
        assertEquals(QuickStartMood.Waiting, state.mood)
        assertEquals("Open Timer", state.actionLabel)
    }

    @Test
    fun `today widget gives soft start copy at zero focus`() {
        val state = WidgetState.today(
            focusCountToday = 0,
            settings = TimerSettings(focusSessionsPerCycle = 4),
        )
        assertEquals(0, state.focusCount)
        assertEquals("A soft start is enough", state.encouragement)
    }

    @Test
    fun `today widget warms up at one focus`() {
        val state = WidgetState.today(
            focusCountToday = 1,
            settings = TimerSettings(focusSessionsPerCycle = 4),
        )
        assertEquals(1, state.focusCount)
        assertEquals("Kuro saw your first focus", state.encouragement)
    }

    @Test
    fun `today widget reaches goal copy at or past target`() {
        val state = WidgetState.today(
            focusCountToday = 7,
            settings = TimerSettings(focusSessionsPerCycle = 4),
        )
        assertEquals(7, state.focusCount)
        assertEquals("Goal reached with Kuro", state.encouragement)
    }

    @Test
    fun `today widget respects smaller cycle settings for goal copy`() {
        val state = WidgetState.today(
            focusCountToday = 2,
            settings = TimerSettings(focusSessionsPerCycle = 2),
        )
        assertEquals(2, state.focusCount)
        assertEquals("Goal reached with Kuro", state.encouragement)
    }
}
