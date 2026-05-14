package com.endiar.kuropomodoro.model

import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

/**
 * Interface implemented by DebugRepository (debug source set only).
 * AppClock.source is set to null in release builds.
 */
interface ClockDebugSource {
    val clockOverride: StateFlow<LocalTime?>
    val speedUpTimer: StateFlow<Boolean>
}

/**
 * App-wide clock. Use AppClock.now() instead of LocalTime.now() anywhere
 * the time-of-day affects app rendering or logic.
 *
 * In release builds, source is always null and behaviour is identical to LocalTime.now().
 * In debug builds, DebugRepository sets source so clock overrides take effect.
 */
object AppClock {
    private const val NORMAL_TICK_MS = 250L
    private const val FAST_TICK_MS = 25L

    var source: ClockDebugSource? = null
        internal set

    fun now(): LocalTime =
        source?.clockOverride?.value ?: LocalTime.now()

    fun tickIntervalMs(): Long =
        if (source?.speedUpTimer?.value == true) FAST_TICK_MS else NORMAL_TICK_MS
}
