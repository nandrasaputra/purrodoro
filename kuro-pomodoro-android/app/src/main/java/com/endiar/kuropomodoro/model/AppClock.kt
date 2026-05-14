package com.endiar.kuropomodoro.model

import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

/**
 * Interface implemented by DebugRepository (debug source set only).
 * AppClock.source is set to null in release builds.
 */
interface ClockDebugSource {
    val clockOverride: StateFlow<LocalTime?>
}

/**
 * App-wide clock. Use AppClock.now() instead of LocalTime.now() anywhere
 * the time-of-day affects app rendering or logic.
 *
 * In release builds, source is always null and behaviour is identical to LocalTime.now().
 * In debug builds, DebugRepository sets source so clock overrides take effect.
 */
object AppClock {
    var source: ClockDebugSource? = null
        internal set

    fun now(): LocalTime =
        source?.clockOverride?.value ?: LocalTime.now()
}
