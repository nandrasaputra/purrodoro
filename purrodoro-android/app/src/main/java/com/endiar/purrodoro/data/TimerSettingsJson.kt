package com.endiar.purrodoro.data

import com.endiar.purrodoro.model.TimerSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Json shape of [TimerSettings] as stored in the `profiles.timer_settings`
 * jsonb column (and mirrored verbatim in the local profile row). Every field
 * is defaulted so the server's `'{}'` column default decodes to stock
 * settings, and unknown keys written by newer clients are ignored.
 */
@Serializable
data class TimerSettingsJson(
    @SerialName("focus_minutes") val focusMinutes: Int = 25,
    @SerialName("short_break_minutes") val shortBreakMinutes: Int = 5,
    @SerialName("long_break_minutes") val longBreakMinutes: Int = 15,
    @SerialName("auto_start_next_session") val autoStartNextSession: Boolean = false,
    @SerialName("focus_sessions_per_cycle") val focusSessionsPerCycle: Int = 4,
)

private val timerSettingsJsonFormat = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeTimerSettings(value: TimerSettings): String =
    timerSettingsJsonFormat.encodeToString(
        TimerSettingsJson.serializer(),
        TimerSettingsJson(
            focusMinutes = value.focusMinutes,
            shortBreakMinutes = value.shortBreakMinutes,
            longBreakMinutes = value.longBreakMinutes,
            autoStartNextSession = value.autoStartNextSession,
            focusSessionsPerCycle = value.focusSessionsPerCycle,
        ),
    )

/** Malformed input falls back to stock settings — never wedge a sync on a bad row. */
fun decodeTimerSettings(raw: String): TimerSettings =
    runCatching { timerSettingsJsonFormat.decodeFromString(TimerSettingsJson.serializer(), raw) }
        .getOrDefault(TimerSettingsJson())
        .let {
            TimerSettings(
                focusMinutes = it.focusMinutes,
                shortBreakMinutes = it.shortBreakMinutes,
                longBreakMinutes = it.longBreakMinutes,
                autoStartNextSession = it.autoStartNextSession,
                focusSessionsPerCycle = it.focusSessionsPerCycle
                    .coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE),
            )
        }
