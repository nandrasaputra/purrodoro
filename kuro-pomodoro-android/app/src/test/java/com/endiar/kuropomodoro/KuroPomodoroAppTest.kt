package com.endiar.kuropomodoro

import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class KuroPomodoroAppTest {
    @Test
    fun timerDisplayTotalKeepsActiveDurationWhileRunning() {
        val editedSettings = TimerSettings(focusMinutes = 50)

        val displayTotal = timerDisplayTotalSeconds(
            settings = editedSettings,
            mode = TimerMode.Focus,
            isRunning = true,
            activeTotalSeconds = 25 * 60
        )

        assertEquals(25 * 60, displayTotal)
    }

    @Test
    fun timerStartTotalKeepsPausedSessionDurationWhenResuming() {
        val editedSettings = TimerSettings(focusMinutes = 50)

        val activeTotal = timerStartTotalSeconds(
            settings = editedSettings,
            mode = TimerMode.Focus,
            remainingSeconds = 24 * 60,
            activeTotalSeconds = 25 * 60
        )

        assertEquals(25 * 60, activeTotal)
    }
}
