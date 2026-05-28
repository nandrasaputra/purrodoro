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

/** What the Quick Start Kuro widget renders. */
data class QuickStartWidgetState(
    val mood: QuickStartMood,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
)

/** What the Today With Kuro widget renders. */
data class TodayWidgetState(
    val focusCount: Int,
    val targetDots: Int,
    val filledDots: Int,
    val encouragement: String,
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
            return if (pendingCycleAck) {
                QuickStartWidgetState(
                    mood = QuickStartMood.Celebrating,
                    title = "Nice work today",
                    subtitle = "Kuro is proud of you",
                    actionLabel = "Open Kuro",
                )
            } else {
                QuickStartWidgetState(
                    mood = QuickStartMood.Ready,
                    title = "Kuro is ready",
                    subtitle = "Start a calm focus",
                    actionLabel = "Start Focus",
                )
            }
        }
        return when (runtime.session.timerState) {
            TimerState.Playing -> QuickStartWidgetState(
                mood = QuickStartMood.Focusing,
                title = "Kuro is focusing",
                subtitle = "Stay with your session",
                actionLabel = "Open Timer",
            )

            TimerState.Paused -> QuickStartWidgetState(
                mood = QuickStartMood.Waiting,
                title = "Kuro is waiting",
                subtitle = "Tap to resume",
                actionLabel = "Open Timer",
            )

            TimerState.Idle -> if (pendingCycleAck) {
                QuickStartWidgetState(
                    mood = QuickStartMood.Celebrating,
                    title = "Nice work today",
                    subtitle = "Kuro is proud of you",
                    actionLabel = "Open Kuro",
                )
            } else {
                QuickStartWidgetState(
                    mood = QuickStartMood.Ready,
                    title = "Kuro is ready",
                    subtitle = "Start a calm focus",
                    actionLabel = "Start Focus",
                )
            }
        }
    }

    /**
     * Derives the Today widget state.
     *
     * Dot row size is the user's [TimerSettings.focusSessionsPerCycle] (1..4).
     * Filled dots are `min(focusCount, target)` so going past the goal does
     * not visually wrap.
     */
    fun today(
        focusCountToday: Int,
        settings: TimerSettings,
    ): TodayWidgetState {
        val target = settings.focusSessionsPerCycle.coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE)
        val safeCount = focusCountToday.coerceAtLeast(0)
        val filled = safeCount.coerceAtMost(target)
        val encouragement = when {
            safeCount <= 0 -> "A soft start is enough"
            safeCount == 1 -> "Kuro saw your first focus"
            safeCount >= target -> "Goal reached with Kuro"
            else -> "Kuro is proud today"
        }
        return TodayWidgetState(
            focusCount = safeCount,
            targetDots = target,
            filledDots = filled,
            encouragement = encouragement,
        )
    }
}
