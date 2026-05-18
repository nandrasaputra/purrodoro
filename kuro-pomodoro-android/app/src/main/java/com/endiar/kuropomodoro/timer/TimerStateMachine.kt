package com.endiar.kuropomodoro.timer

import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.seconds
import java.time.Instant
import java.time.ZoneId

/**
 * Pure-ish state machine for advancing a Pomodoro cycle on segment completion.
 * Invoked by both [TimerService] (natural tick) and [TimerAlarmReceiver] (backstop alarm).
 *
 * @see TimerReconciler for restore-on-open logic.
 */
object TimerStateMachine {

    /**
     * Apply one TimerComplete transition: advance the cycle, accrue stamps,
     * close records on cycle end, persist the new runtime snapshot.
     *
     * Returns the new [TimerRuntimeState]. The caller decides whether to keep the
     * service running (still active) or stop it (Idle / Paused awaiting user input).
     */
    suspend fun applyTimerComplete(
        snapshot: TimerRuntimeState,
        settings: TimerSettings,
        now: Long,
        repo: AppRepository,
        zone: ZoneId = ZoneId.systemDefault()
    ): TimerRuntimeState {
        if (snapshot.session.timerState != TimerState.Playing) return snapshot

        snapshot.session.items.getOrNull(snapshot.session.currentIndex)
            ?: return snapshot

        val currentItem = snapshot.session.items[snapshot.session.currentIndex]
        var openRecord = snapshot.openRecord

        var activeTask = snapshot.activeTask
        when (currentItem.type) {
            is SessionItemType.Focus -> {
                val date = Instant.ofEpochMilli(snapshot.endsAtMillis ?: now)
                    .atZone(zone).toLocalDate()
                repo.incrementStampFor(date)
                openRecord = openRecord?.copy(
                    completedFocusCount = openRecord.completedFocusCount + 1
                )
                activeTask?.let { repo.incrementTaskCompletedSessions(it.id) }
                snapshot.activeHabitId?.let { habitId ->
                    repo.incrementHabitProgress(habitId, date)
                    // Keep UI counter in sync for display during the cycle.
                    activeTask = activeTask?.copy(completedSessions = (activeTask?.completedSessions ?: 0) + 1)
                }
            }
            is SessionItemType.ShortBreak -> {
                openRecord = openRecord?.copy(
                    completedShortBreakCount = openRecord.completedShortBreakCount + 1
                )
            }
            is SessionItemType.LongBreak -> {
                openRecord = openRecord?.copy(
                    completedLongBreakCount = openRecord.completedLongBreakCount + 1
                )
            }
        }

        val nextIndex = snapshot.session.currentIndex + 1
        if (nextIndex >= snapshot.session.items.size) {
            openRecord?.let { rec ->
                repo.appendSessionRecord(
                    rec.copy(
                        endedAt = snapshot.endsAtMillis ?: now,
                        status = SessionStatus.Completed
                    )
                )
            }
            val resetState = snapshot.copy(
                session = snapshot.session.copy(
                    currentIndex = 0,
                    remainingSeconds = snapshot.session.items.first().type.seconds(),
                    timerState = TimerState.Idle
                ),
                endsAtMillis = null,
                activeTask = activeTask,
                openRecord = null
            )
            repo.setRuntimeSnapshot(null)
            return resetState
        }

        val nextItem = snapshot.session.items[nextIndex]
        val nextSeconds = nextItem.type.seconds()
        val baseEndsAt = snapshot.endsAtMillis ?: now

        val advanced = if (settings.autoStartNextSession) {
            snapshot.copy(
                session = snapshot.session.copy(
                    currentIndex = nextIndex,
                    remainingSeconds = nextSeconds,
                    timerState = TimerState.Playing
                ),
                endsAtMillis = baseEndsAt + nextSeconds * 1000L,
                activeTask = activeTask,
                openRecord = openRecord
            )
        } else {
            snapshot.copy(
                session = snapshot.session.copy(
                    currentIndex = nextIndex,
                    remainingSeconds = nextSeconds,
                    timerState = TimerState.Paused
                ),
                endsAtMillis = null,
                activeTask = activeTask,
                openRecord = openRecord
            )
        }
        repo.setRuntimeSnapshot(advanced)
        return advanced
    }
}
