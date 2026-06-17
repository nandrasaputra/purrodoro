package com.endiar.purrodoro.model

import java.time.LocalDate

enum class HabitFocusBlockReason {
    NotScheduled,
    Skipped,
    Completed,
}

fun FocusTask.canStartFocus(): Boolean = !completed

fun Habit.focusBlockReason(today: LocalDate): HabitFocusBlockReason? = when {
    !isScheduledOn(today) -> HabitFocusBlockReason.NotScheduled
    isSkippedOn(today) -> HabitFocusBlockReason.Skipped
    isTargetMetOn(today) -> HabitFocusBlockReason.Completed
    else -> null
}

fun Habit.canStartFocus(today: LocalDate): Boolean = focusBlockReason(today) == null
