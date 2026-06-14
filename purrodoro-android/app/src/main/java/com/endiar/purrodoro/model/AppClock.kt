package com.endiar.purrodoro.model

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
    /** When true, [premiumOverrideValue] replaces the DataStore premium flag. */
    val isPremiumOverrideEnabled: StateFlow<Boolean>
    val premiumOverrideValue: StateFlow<Boolean>
}

/**
 * Interface implemented by DebugRepository (debug source set only).
 * TimerSkipOverride.source is set to null in release builds.
 */
interface TimerSkipDebugSource {
    /**
     * When true, the timer compresses every segment's runtime duration to
     * [TimerSkipOverride.SKIPPED_SEGMENT_SECONDS] so a cycle can be walked
     * through near-instantly. The recorded [SessionRecord] keeps the real
     * configured durations — only the live countdown is shortened.
     */
    val isSkipTicksEnabled: StateFlow<Boolean>
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

    /** Forced premium flag when override is enabled; null → use [AppRepository] value. */
    fun overrideValue(): Boolean? {
        val src = source ?: return null
        if (!src.isPremiumOverrideEnabled.value) return null
        return src.premiumOverrideValue.value
    }
}

/**
 * App-wide "skip ticks" override for debug builds. When enabled, the timer runs
 * every segment for [SKIPPED_SEGMENT_SECONDS] instead of its configured length,
 * letting a developer walk a whole Pomodoro cycle in seconds.
 *
 * Only the *live runtime* duration is affected — the [SessionRecord] is still
 * built from the real [TimerSettings], so injected/recorded history reflects the
 * configured cycle (e.g. 4 cycles × 5 minutes), not the compressed run.
 *
 * In release builds, source is always null — segments run their full length.
 */
object TimerSkipOverride {
    /** Compressed segment length, in seconds, while skip-ticks is on. */
    const val SKIPPED_SEGMENT_SECONDS: Int = 1

    var source: TimerSkipDebugSource? = null
        internal set

    fun isEnabled(): Boolean = source?.isSkipTicksEnabled?.value ?: false

    /**
     * The duration to actually count down for the active segment. Returns the
     * compressed value when skip-ticks is on, otherwise the real [realSeconds].
     */
    fun effectiveSeconds(realSeconds: Int): Int =
        if (isEnabled()) SKIPPED_SEGMENT_SECONDS else realSeconds
}
