package com.endiar.purrodoro.data

import com.endiar.purrodoro.model.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerSettingsJsonTest {

    @Test
    fun `settings round-trip through json`() {
        val settings = TimerSettings(
            focusMinutes = 50,
            shortBreakMinutes = 10,
            longBreakMinutes = 20,
            autoStartNextSession = true,
            focusSessionsPerCycle = 3,
        )

        assertEquals(settings, decodeTimerSettings(encodeTimerSettings(settings)))
    }

    @Test
    fun `out-of-range cycle size from another client is clamped`() {
        // Mirrors the buildAppState coercion: the widget layout caps at 4.
        assertEquals(
            TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE,
            decodeTimerSettings("""{"focus_sessions_per_cycle":9}""").focusSessionsPerCycle,
        )
    }

    @Test
    fun `empty object decodes to default settings`() {
        // '{}' is the server-side column default for profiles.timer_settings.
        assertEquals(TimerSettings(), decodeTimerSettings("{}"))
    }

    @Test
    fun `unknown extra field from a newer client is ignored`() {
        val decoded = decodeTimerSettings(
            """{"focus_minutes":30,"future_field":true}"""
        )

        assertEquals(TimerSettings(focusMinutes = 30), decoded)
    }
}
