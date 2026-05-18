package com.endiar.kuropomodoro.timer

import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.seconds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReconcileResult(
    val state: TimerRuntimeState,
    val stampsToIncrement: List<LocalDate>,
    val closedRecords: List<SessionRecord>,
    val habitProgressDates: List<LocalDate> = emptyList(),
)

object TimerReconciler {

    /**
     * Replay segment completions against the wall clock to produce the correct
     * [TimerRuntimeState] for "right now". Pure function — no Android dependencies.
     *
     * Related: [com.endiar.kuropomodoro.model.reduce] handles event-driven transitions
     * (Start, Pause, Resume, Skip, Stop, TimerComplete). This reconciler is the
     * wall-clock replay variant used when the service was killed and the app reopens.
     */
    fun reconcile(
        snapshot: TimerRuntimeState,
        now: Long,
        settings: TimerSettings,
        zone: ZoneId = ZoneId.systemDefault()
    ): ReconcileResult {
        if (snapshot.session.timerState != TimerState.Playing || snapshot.endsAtMillis == null) {
            return ReconcileResult(snapshot, emptyList(), emptyList())
        }

        var session = snapshot.session
        var endsAt: Long = snapshot.endsAtMillis
        var openRecord = snapshot.openRecord
        val stamps = mutableListOf<LocalDate>()
        val closed = mutableListOf<SessionRecord>()
        val habitDates = mutableListOf<LocalDate>()

        while (now >= endsAt) {
            val currentItem = session.items.getOrNull(session.currentIndex) ?: break

            when (currentItem.type) {
                is SessionItemType.Focus -> {
                    val segDate = Instant.ofEpochMilli(endsAt).atZone(zone).toLocalDate()
                    stamps.add(segDate)
                    if (snapshot.activeHabitId != null) habitDates.add(segDate)
                    openRecord = openRecord?.copy(
                        completedFocusCount = openRecord.completedFocusCount + 1
                    )
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

            val nextIndex = session.currentIndex + 1
            if (nextIndex >= session.items.size) {
                openRecord?.let { rec ->
                    closed.add(
                        rec.copy(
                            endedAt = endsAt,
                            status = SessionStatus.Completed
                        )
                    )
                }
                return ReconcileResult(
                    state = snapshot.copy(
                        session = session.copy(
                            currentIndex = 0,
                            remainingSeconds = session.items.first().type.seconds(),
                            timerState = TimerState.Idle
                        ),
                        endsAtMillis = null,
                        openRecord = null
                    ),
                    stampsToIncrement = stamps,
                    closedRecords = closed,
                    habitProgressDates = habitDates,
                )
            }

            val nextItem = session.items[nextIndex]
            val nextSeconds = nextItem.type.seconds()
            session = session.copy(
                currentIndex = nextIndex,
                remainingSeconds = nextSeconds,
                timerState = if (settings.autoStartNextSession) TimerState.Playing else TimerState.Paused
            )

            if (settings.autoStartNextSession) {
                endsAt += nextSeconds * 1000L
            } else {
                return ReconcileResult(
                    state = snapshot.copy(
                        session = session,
                        endsAtMillis = null,
                        openRecord = openRecord
                    ),
                    stampsToIncrement = stamps,
                    closedRecords = closed,
                    habitProgressDates = habitDates,
                )
            }
        }

        val newRemaining = ((endsAt - now) / 1000L).toInt().coerceAtLeast(0)
        return ReconcileResult(
            state = snapshot.copy(
                session = session.copy(remainingSeconds = newRemaining),
                endsAtMillis = endsAt,
                openRecord = openRecord
            ),
            stampsToIncrement = stamps,
            closedRecords = closed,
            habitProgressDates = habitDates,
        )
    }
}
