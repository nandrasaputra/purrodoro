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
                // Real tasks no longer carry a per-task pomodoro target — completion
                // is binary and triggered by cycle success below, not by segment finish.
                // Habits keep their own progress model: the synthetic FocusTask (negative id)
                // is the carrier, and we still bump its display counter so any in-flight UI
                // that reads `activeTask.completedSessions` for habit cycles stays in sync.
                snapshot.activeHabitId?.let { habitId ->
                    repo.incrementHabitProgress(habitId, date)
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
            // Auto-complete the attached real task on a successful cycle finish.
            // Habits use the synthetic FocusTask wrapper (negative id) and are
            // credited via incrementHabitProgress above — they don't get
            // setTaskCompleted, since there's no real `FocusTask` row to mark.
            activeTask?.takeIf { it.id > 0 && !it.completed }?.let { task ->
                repo.setTaskCompleted(task.id, true)
                activeTask = task.copy(completed = true)
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
            // Single source of truth for the completion-dialog ack flag: any path
            // that lands on the natural end-of-cycle (foreground tick OR backstop
            // alarm replay after the service was killed) flips the flag here so
            // the dialog shows next time the UI is foregrounded. The skip-to-end
            // shortcut in TimerService.doSkip handles its own write via
            // finishCycleAndStop because it doesn't route through this function.
            repo.setPendingCycleCompletionAck(true)
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
