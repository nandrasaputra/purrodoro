package com.endiar.kuropomodoro.widget

import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState

/**
 * Pure UI view-models for the Kuro home-screen widgets.
 *
 * These types are intentionally free of Android dependencies (no `Context`,
 * `Resources`, etc.) so they can be derived from `AppRepository` data and
 * unit-tested without instrumentation.
 */

/** Visual / semantic mood of the [QuickStartWidgetState]. */
enum class QuickStartMood {
    /** No timer is running. Primary action: start a new focus session. */
    Ready,

    /** A focus session is in progress. Primary action: open the app to the live timer. */
    Focusing,

    /** A focus session is paused. Primary action: open the app to resume. */
    Waiting,

    /** A cycle has just finished and the user has not yet acknowledged it. */
    Celebrating,
}

/**
 * What the Quick Start Kuro widget renders. The [mood] fully determines the
 * title / subtitle / action copy, which is resolved against localized string
 * resources in the widget layer (keeping this model free of Android deps).
 */
data class QuickStartWidgetState(
    val mood: QuickStartMood,
)

/** Encouragement tone for the Today widget, resolved to localized copy in the widget. */
enum class TodayEncouragement {
    SoftStart,
    FirstFocus,
    GoalReached,
    Proud,
}

/** What the Today With Kuro widget renders. */
data class TodayWidgetState(
    val focusCount: Int,
    val encouragement: TodayEncouragement,
)

object WidgetState {

    /**
     * Derives the Quick Start widget state from the live runtime snapshot.
     *
     * - `null` snapshot (no in-flight cycle) → [QuickStartMood.Ready]
     * - Idle session with a `pendingCycleAck` flag → [QuickStartMood.Celebrating]
     * - Playing → [QuickStartMood.Focusing]
     * - Paused → [QuickStartMood.Waiting]
     */
    fun quickStart(
        runtime: TimerRuntimeState?,
        pendingCycleAck: Boolean,
    ): QuickStartWidgetState {
        if (runtime == null) {
            return QuickStartWidgetState(
                mood = if (pendingCycleAck) QuickStartMood.Celebrating else QuickStartMood.Ready,
            )
        }
        return when (runtime.session.timerState) {
            TimerState.Playing -> QuickStartWidgetState(mood = QuickStartMood.Focusing)
            TimerState.Paused -> QuickStartWidgetState(mood = QuickStartMood.Waiting)
            TimerState.Idle -> QuickStartWidgetState(
                mood = if (pendingCycleAck) QuickStartMood.Celebrating else QuickStartMood.Ready,
            )
        }
    }

    /**
     * Derives the Today widget state.
     *
     * The daily goal ([TimerSettings.focusSessionsPerCycle], 1..4) drives the
     * encouragement copy: it softens at 0 focus and warms up once the goal is
     * reached.
     */
    fun today(
        focusCountToday: Int,
        settings: TimerSettings,
    ): TodayWidgetState {
        val target = settings.focusSessionsPerCycle.coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE)
        val safeCount = focusCountToday.coerceAtLeast(0)
        val encouragement = when {
            safeCount <= 0 -> TodayEncouragement.SoftStart
            safeCount == 1 -> TodayEncouragement.FirstFocus
            safeCount >= target -> TodayEncouragement.GoalReached
            else -> TodayEncouragement.Proud
        }
        return TodayWidgetState(
            focusCount = safeCount,
            encouragement = encouragement,
        )
    }
}
