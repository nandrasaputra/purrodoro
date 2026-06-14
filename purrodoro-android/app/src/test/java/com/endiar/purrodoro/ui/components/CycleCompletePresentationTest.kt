package com.endiar.purrodoro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CycleCompletePresentationTest {

    private val today = LocalDate.of(2026, 6, 14)

    @Test
    fun focusDurationParts_minutesOnly() {
        assertEquals(
            FocusDurationParts(format = FocusDurationFormat.MinutesOnly, minutes = 25),
            focusDurationParts(25),
        )
    }

    @Test
    fun focusDurationParts_hoursOnly() {
        assertEquals(
            FocusDurationParts(format = FocusDurationFormat.HoursOnly, hours = 2),
            focusDurationParts(120),
        )
    }

    @Test
    fun focusDurationParts_hoursAndMinutes() {
        assertEquals(
            FocusDurationParts(
                format = FocusDurationFormat.HoursAndMinutes,
                hours = 1,
                remainderMinutes = 30,
            ),
            focusDurationParts(90),
        )
    }

    @Test
    fun resolveCycleStreakPill_hidesWhenNoStamps() {
        assertEquals(
            CycleStreakPill.Hidden,
            resolveCycleStreakPill(emptyMap(), today),
        )
    }

    @Test
    fun resolveCycleStreakPill_firstDay() {
        val stamps = mapOf(today to 1)
        assertEquals(CycleStreakPill.DayOneNew, resolveCycleStreakPill(stamps, today))
    }

    @Test
    fun resolveCycleStreakPill_restarted() {
        val stamps = mapOf(
            today.minusDays(3) to 2,
            today to 1,
        )
        assertEquals(CycleStreakPill.DayOneBack, resolveCycleStreakPill(stamps, today))
    }

    @Test
    fun resolveCycleStreakPill_multiDay() {
        val stamps = mapOf(
            today.minusDays(2) to 1,
            today.minusDays(1) to 1,
            today to 1,
        )
        assertEquals(
            CycleStreakPill.MultiDay(3),
            resolveCycleStreakPill(stamps, today),
        )
    }
}
