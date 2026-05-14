package com.endiar.kuropomodoro.model

/**
 * Runtime wrapper around [TimerSession] that the foreground service owns while a cycle is active.
 *
 * Invariants:
 *  - When [session].timerState == [TimerState.Playing], [endsAtMillis] is non-null.
 *  - While running, the displayed remaining time is derived from `endsAtMillis - now()`.
 *  - While paused, the displayed remaining time is read from [TimerSession.remainingSeconds].
 */
data class TimerRuntimeState(
    val session: TimerSession,
    val endsAtMillis: Long?,
    val activeTask: FocusTask?,
    val openRecord: SessionRecord?
) {
    val isActive: Boolean get() = session.timerState != TimerState.Idle
}
