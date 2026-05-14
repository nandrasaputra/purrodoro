package com.endiar.kuropomodoro.model

import java.time.LocalDate

sealed class TimerAction {
    object Start : TimerAction()
    object Pause : TimerAction()
    object Resume : TimerAction()
    object Stop : TimerAction()
    object Skip : TimerAction()
    object TimerComplete : TimerAction()
    data class Tick(val elapsed: Int) : TimerAction()
}

data class ReducerResult(
    val session: TimerSession,
    val stamps: Map<LocalDate, FocusStamp>
)

fun reduce(
    session: TimerSession,
    action: TimerAction,
    settings: TimerSettings,
    stamps: Map<LocalDate, FocusStamp>,
    today: LocalDate
): ReducerResult {
    return when (action) {
        is TimerAction.Start -> ReducerResult(
            session.copy(timerState = TimerState.Playing),
            stamps
        )

        is TimerAction.Pause -> ReducerResult(
            session.copy(timerState = TimerState.Paused),
            stamps
        )

        is TimerAction.Resume -> ReducerResult(
            session.copy(timerState = TimerState.Playing),
            stamps
        )

        is TimerAction.Stop -> {
            val newStamps = if (session.currentItem?.type is SessionItemType.Focus && session.remainingSeconds > 0) {
                addUnfinishedStamp(stamps, today)
            } else {
                stamps
            }
            ReducerResult(
                buildSession(settings),
                newStamps
            )
        }

        is TimerAction.Skip -> {
            val newStamps = if (session.currentItem?.type is SessionItemType.Focus && session.remainingSeconds > 0) {
                addUnfinishedStamp(stamps, today)
            } else {
                stamps
            }
            val nextIndex = session.currentIndex + 1
            val nextItem = session.items.getOrNull(nextIndex)
            val nextSeconds = nextItem?.type?.seconds() ?: 0
            val nextState = if (settings.autoStartNextSession) TimerState.Playing else TimerState.Paused
            ReducerResult(
                session.copy(
                    currentIndex = nextIndex,
                    remainingSeconds = nextSeconds,
                    timerState = nextState
                ),
                newStamps
            )
        }

        is TimerAction.TimerComplete -> {
            val newStamps = if (session.currentItem?.type is SessionItemType.Focus) {
                addCompletedStamp(stamps, today)
            } else {
                stamps
            }
            val nextIndex = session.currentIndex + 1
            if (nextIndex >= session.items.size) {
                // Cycle complete — reset to Idle
                ReducerResult(buildSession(settings), newStamps)
            } else {
                val nextItem = session.items[nextIndex]
                val nextSeconds = nextItem.type.seconds()
                val nextState = if (settings.autoStartNextSession) TimerState.Playing else TimerState.Paused
                ReducerResult(
                    session.copy(
                        currentIndex = nextIndex,
                        remainingSeconds = nextSeconds,
                        timerState = nextState
                    ),
                    newStamps
                )
            }
        }

        is TimerAction.Tick -> ReducerResult(
            session.copy(remainingSeconds = (session.remainingSeconds - action.elapsed).coerceAtLeast(0)),
            stamps
        )
    }
}

private fun SessionItemType.seconds(): Int = when (this) {
    is SessionItemType.Focus -> seconds
    is SessionItemType.ShortBreak -> seconds
    is SessionItemType.LongBreak -> seconds
}
