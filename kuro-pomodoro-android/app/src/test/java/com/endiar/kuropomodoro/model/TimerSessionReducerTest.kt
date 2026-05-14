package com.endiar.kuropomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TimerSessionReducerTest {

    private val defaultSettings = TimerSettings(focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15)
    private val today = LocalDate.of(2026, 5, 14)
    private val emptyStamps = emptyMap<LocalDate, FocusStamp>()

    private fun idleSession() = buildSession(defaultSettings)

    // ── Start ─────────────────────────────────────────────────────────────────

    @Test
    fun `Start from Idle sets state to Playing at index 0`() {
        val result = reduce(idleSession(), TimerAction.Start, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
        assertEquals(0, result.session.currentIndex)
    }

    // ── Pause ─────────────────────────────────────────────────────────────────

    @Test
    fun `Pause from Playing sets state to Paused`() {
        val playing = idleSession().copy(timerState = TimerState.Playing)
        val result = reduce(playing, TimerAction.Pause, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Paused, result.session.timerState)
        assertEquals(playing.remainingSeconds, result.session.remainingSeconds)
    }

    // ── Resume ────────────────────────────────────────────────────────────────

    @Test
    fun `Resume from Paused sets state to Playing`() {
        val paused = idleSession().copy(timerState = TimerState.Paused, remainingSeconds = 900)
        val result = reduce(paused, TimerAction.Resume, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
        assertEquals(900, result.session.remainingSeconds)
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    @Test
    fun `Stop from Playing resets to Idle at index 0`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 2, remainingSeconds = 900)
        val result = reduce(playing, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Idle, result.session.timerState)
        assertEquals(0, result.session.currentIndex)
        assertEquals(25 * 60, result.session.remainingSeconds)
    }

    @Test
    fun `Stop mid-Focus records unfinished stamp`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 0, remainingSeconds = 900)
        val result = reduce(playing, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.unfinished)
        assertEquals(0, result.stamps[today]?.completed ?: 0)
    }

    @Test
    fun `Stop mid-ShortBreak records no stamp`() {
        val session = idleSession().copy(timerState = TimerState.Playing, currentIndex = 1, remainingSeconds = 100)
        val result = reduce(session, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertTrue(result.stamps.isEmpty())
    }

    @Test
    fun `Stop from Paused mid-Focus records unfinished stamp`() {
        val paused = idleSession().copy(timerState = TimerState.Paused, currentIndex = 0, remainingSeconds = 500)
        val result = reduce(paused, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.unfinished)
    }

    // ── Skip ──────────────────────────────────────────────────────────────────

    @Test
    fun `Skip from Playing mid-Focus advances index and records unfinished`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 0, remainingSeconds = 900)
        val result = reduce(playing, TimerAction.Skip, defaultSettings, emptyStamps, today)
        assertEquals(1, result.session.currentIndex)
        assertEquals(1, result.stamps[today]?.unfinished)
    }

    @Test
    fun `Skip from Paused mid-Focus advances index and records unfinished`() {
        val paused = idleSession().copy(timerState = TimerState.Paused, currentIndex = 0, remainingSeconds = 600)
        val result = reduce(paused, TimerAction.Skip, defaultSettings, emptyStamps, today)
        assertEquals(1, result.session.currentIndex)
        assertEquals(1, result.stamps[today]?.unfinished)
    }

    @Test
    fun `Skip mid-ShortBreak records no stamp`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 1, remainingSeconds = 100)
        val result = reduce(playing, TimerAction.Skip, defaultSettings, emptyStamps, today)
        assertEquals(2, result.session.currentIndex)
        assertTrue(result.stamps.isEmpty())
    }

    @Test
    fun `Skip sets next state to Paused when autoStart is false`() {
        val settings = defaultSettings.copy(autoStartNextSession = false)
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 1, remainingSeconds = 100)
        val result = reduce(playing, TimerAction.Skip, settings, emptyStamps, today)
        assertEquals(TimerState.Paused, result.session.timerState)
    }

    @Test
    fun `Skip sets next state to Playing when autoStart is true`() {
        val settings = defaultSettings.copy(autoStartNextSession = true)
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 1, remainingSeconds = 100)
        val result = reduce(playing, TimerAction.Skip, settings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
    }

    // ── Timer hits zero ───────────────────────────────────────────────────────

    @Test
    fun `TimerComplete on Focus advances to next item and records completed stamp`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 0, remainingSeconds = 0)
        val result = reduce(playing, TimerAction.TimerComplete, defaultSettings, emptyStamps, today)
        assertEquals(1, result.session.currentIndex)
        assertEquals(1, result.stamps[today]?.completed)
        assertEquals(0, result.stamps[today]?.unfinished ?: 0)
    }

    @Test
    fun `TimerComplete on ShortBreak advances to next item with no stamp`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 1, remainingSeconds = 0)
        val result = reduce(playing, TimerAction.TimerComplete, defaultSettings, emptyStamps, today)
        assertEquals(2, result.session.currentIndex)
        assertTrue(result.stamps.isEmpty())
    }

    @Test
    fun `TimerComplete on last item resets to Idle`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 7, remainingSeconds = 0)
        val result = reduce(playing, TimerAction.TimerComplete, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Idle, result.session.timerState)
        assertEquals(0, result.session.currentIndex)
    }

    @Test
    fun `TimerComplete advances to Paused when autoStart is false`() {
        val settings = defaultSettings.copy(autoStartNextSession = false)
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 0, remainingSeconds = 0)
        val result = reduce(playing, TimerAction.TimerComplete, settings, emptyStamps, today)
        assertEquals(TimerState.Paused, result.session.timerState)
    }

    @Test
    fun `TimerComplete advances to Playing when autoStart is true`() {
        val settings = defaultSettings.copy(autoStartNextSession = true)
        val playing = idleSession().copy(timerState = TimerState.Playing, currentIndex = 0, remainingSeconds = 0)
        val result = reduce(playing, TimerAction.TimerComplete, settings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Test
    fun `Tick decrements remainingSeconds`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, remainingSeconds = 100)
        val result = reduce(playing, TimerAction.Tick(elapsed = 3), defaultSettings, emptyStamps, today)
        assertEquals(97, result.session.remainingSeconds)
    }

    @Test
    fun `Tick does not go below zero`() {
        val playing = idleSession().copy(timerState = TimerState.Playing, remainingSeconds = 1)
        val result = reduce(playing, TimerAction.Tick(elapsed = 5), defaultSettings, emptyStamps, today)
        assertEquals(0, result.session.remainingSeconds)
    }
}
