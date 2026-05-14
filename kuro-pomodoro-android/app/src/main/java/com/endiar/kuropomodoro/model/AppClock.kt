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
 * Interface implemented by DebugRepository (debug source set only).
 * PremiumOverride.source is set to null in release builds.
 */
interface PremiumDebugSource {
    val isPremiumOverride: StateFlow<Boolean>
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

/**
 * App-wide premium override for debug builds.
 * In release builds, source is always null — isPremium comes solely from AppRepository.
 */
object PremiumOverride {
    var source: PremiumDebugSource? = null
        internal set

    /** Returns true if the debug override is active; null if no override (use AppRepository value). */
    fun overrideValue(): Boolean? = source?.isPremiumOverride?.value?.let { if (it) true else null }
}
