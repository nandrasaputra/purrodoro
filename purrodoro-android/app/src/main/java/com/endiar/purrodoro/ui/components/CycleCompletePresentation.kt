package com.endiar.purrodoro.ui.components

import com.endiar.purrodoro.model.calculateStreak
import java.time.LocalDate

sealed interface CycleStreakPill {
    data object Hidden : CycleStreakPill
    data class MultiDay(val days: Int) : CycleStreakPill
    data object DayOneNew : CycleStreakPill
    data object DayOneBack : CycleStreakPill
}

fun resolveCycleStreakPill(stamps: Map<LocalDate, Int>, today: LocalDate): CycleStreakPill {
    val streak = calculateStreak(stamps, today)
    return when {
        streak >= 2 -> CycleStreakPill.MultiDay(streak)
        streak == 1 -> {
            val hadPriorStamps = stamps.any { (date, count) ->
                count > 0 && date.isBefore(today)
            }
            if (hadPriorStamps) CycleStreakPill.DayOneBack else CycleStreakPill.DayOneNew
        }
        else -> CycleStreakPill.Hidden
    }
}

enum class FocusDurationFormat {
    MinutesOnly,
    HoursOnly,
    HoursAndMinutes,
}

data class FocusDurationParts(
    val format: FocusDurationFormat,
    val minutes: Int = 0,
    val hours: Int = 0,
    val remainderMinutes: Int = 0,
)

fun focusDurationParts(totalMinutes: Int): FocusDurationParts {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    return when {
        safeMinutes < 60 -> FocusDurationParts(
            format = FocusDurationFormat.MinutesOnly,
            minutes = safeMinutes,
        )
        safeMinutes % 60 == 0 -> FocusDurationParts(
            format = FocusDurationFormat.HoursOnly,
            hours = safeMinutes / 60,
        )
        else -> FocusDurationParts(
            format = FocusDurationFormat.HoursAndMinutes,
            hours = safeMinutes / 60,
            remainderMinutes = safeMinutes % 60,
        )
    }
}
