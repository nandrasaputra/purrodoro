package com.endiar.kuropomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TimerSessionReducerTest {

    private val defaultSettings = TimerSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        autoStartNextSession = false,
        focusSessionsPerCycle = 2
    )
    private val today: LocalDate = LocalDate.of(2026, 5, 14)
    private val emptyStamps: Map<LocalDate, FocusStamp> = emptyMap()

    private fun freshSession(settings: TimerSettings = defaultSettings) = buildSession(settings)

    @Test
    fun start_transitions_idle_to_playing() {
        val initial = freshSession()
        val result = reduce(initial, TimerAction.Start, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
        assertEquals(initial.currentIndex, result.session.currentIndex)
        assertEquals(initial.remainingSeconds, result.session.remainingSeconds)
        assertEquals(emptyStamps, result.stamps)
    }

    @Test
    fun pause_transitions_playing_to_paused() {
        val playing = freshSession().copy(timerState = TimerState.Playing)
        val result = reduce(playing, TimerAction.Pause, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Paused, result.session.timerState)
    }

    @Test
    fun resume_transitions_paused_to_playing() {
        val paused = freshSession().copy(timerState = TimerState.Paused)
        val result = reduce(paused, TimerAction.Resume, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
    }

    @Test
    fun stop_during_focus_with_remaining_time_adds_unfinished_stamp() {
        val midFocus = freshSession().copy(
            timerState = TimerState.Playing,
            remainingSeconds = 600 // 10 minutes left in Focus
        )
        val result = reduce(midFocus, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.unfinished)
        assertEquals(0, result.stamps[today]?.completed)
        // Stop also resets to a fresh session
        assertEquals(0, result.session.currentIndex)
        assertEquals(TimerState.Idle, result.session.timerState)
    }

    @Test
    fun stop_during_break_does_not_add_unfinished_stamp() {
        val midBreak = freshSession().copy(
            currentIndex = 1, // ShortBreak position in [F, S, F, L]
            timerState = TimerState.Playing,
            remainingSeconds = 100
        )
        val result = reduce(midBreak, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertEquals(emptyStamps, result.stamps)
    }

    @Test
    fun skip_during_focus_with_remaining_time_adds_unfinished_stamp_and_advances() {
        val midFocus = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 600)
        val result = reduce(midFocus, TimerAction.Skip, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.unfinished)
        assertEquals(1, result.session.currentIndex)
    }

    @Test
    fun skip_during_break_advances_without_stamp() {
        val midBreak = freshSession().copy(
            currentIndex = 1,
            timerState = TimerState.Playing,
            remainingSeconds = 100
        )
        val result = reduce(midBreak, TimerAction.Skip, defaultSettings, emptyStamps, today)
        assertEquals(2, result.session.currentIndex)
        assertEquals(emptyStamps, result.stamps)
    }

    @Test
    fun timerComplete_on_focus_adds_completed_stamp_and_advances() {
        val endingFocus = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 0)
        val result = reduce(endingFocus, TimerAction.TimerComplete, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.completed)
        assertEquals(1, result.session.currentIndex)
        // No auto-advance → next state is Paused
        assertEquals(TimerState.Paused, result.session.timerState)
    }

    @Test
    fun timerComplete_with_autoStart_keeps_playing_after_advance() {
        val autoSettings = defaultSettings.copy(autoStartNextSession = true)
        val endingFocus = buildSession(autoSettings).copy(timerState = TimerState.Playing, remainingSeconds = 0)
        val result = reduce(endingFocus, TimerAction.TimerComplete, autoSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
    }

    @Test
    fun timerComplete_on_last_item_resets_to_fresh_session() {
        val lastItemEnding = freshSession().let { initial ->
            initial.copy(
                currentIndex = initial.items.size - 1, // last (LongBreak)
                timerState = TimerState.Playing,
                remainingSeconds = 0
            )
        }
        val result = reduce(lastItemEnding, TimerAction.TimerComplete, defaultSettings, emptyStamps, today)
        assertEquals(0, result.session.currentIndex)
        assertEquals(TimerState.Idle, result.session.timerState)
    }

    @Test
    fun tick_decrements_remaining_seconds() {
        val playing = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 100)
        val result = reduce(playing, TimerAction.Tick(elapsed = 3), defaultSettings, emptyStamps, today)
        assertEquals(97, result.session.remainingSeconds)
    }

    @Test
    fun tick_clamps_remaining_seconds_at_zero() {
        val playing = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 2)
        val result = reduce(playing, TimerAction.Tick(elapsed = 10), defaultSettings, emptyStamps, today)
        assertEquals(0, result.session.remainingSeconds)
    }
}
