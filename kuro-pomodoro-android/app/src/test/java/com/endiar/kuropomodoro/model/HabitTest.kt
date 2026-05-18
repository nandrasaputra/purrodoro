package com.endiar.kuropomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitTest {

    private val monday = LocalDate.of(2024, 1, 1)       // Monday
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)
    private val friday = monday.plusDays(4)

    private fun binaryHabit(occurrence: HabitOccurrence = HabitOccurrence.Daily) = Habit(
        id = 1, name = "Meditate", emoji = "🧘", colorArgb = 0xFF_90CAF9L,
        createdAt = 0L, targetType = HabitTargetType.Binary, occurrence = occurrence,
    )

    private fun quantityHabit(target: Int = 4) = Habit(
        id = 2, name = "Water", emoji = "💧", colorArgb = 0xFF_80DEEAL,
        createdAt = 0L, targetType = HabitTargetType.Quantity, targetValue = target,
    )

    // ── isScheduledOn ───────────────────────────────────────────────────────

    @Test
    fun `daily habit is scheduled on every day`() {
        val h = binaryHabit(HabitOccurrence.Daily)
        DayOfWeek.values().forEachIndexed { i, _ ->
            assertTrue(h.isScheduledOn(monday.plusDays(i.toLong())))
        }
    }

    @Test
    fun `weekday habit is only scheduled on configured days`() {
        val h = binaryHabit(HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
        assertTrue(h.isScheduledOn(monday))
        assertFalse(h.isScheduledOn(tuesday))
        assertTrue(h.isScheduledOn(wednesday))
        assertFalse(h.isScheduledOn(thursday))
    }

    // ── Binary toggleBinaryOn ───────────────────────────────────────────────

    @Test
    fun `toggleBinaryOn marks completed when blank`() {
        val h = binaryHabit().toggleBinaryOn(monday)
        assertTrue(h.isTargetMetOn(monday))
    }

    @Test
    fun `toggleBinaryOn clears when already completed`() {
        val h = binaryHabit().toggleBinaryOn(monday).toggleBinaryOn(monday)
        assertFalse(h.isTargetMetOn(monday))
    }

    // ── Quantity incrementProgress ──────────────────────────────────────────

    @Test
    fun `incrementProgress moves to partial then completed`() {
        val h = quantityHabit(4)
        val h1 = h.incrementProgress(monday, 1)
        assertEquals(1, h1.progressOn(monday))
        assertFalse(h1.isTargetMetOn(monday))

        val h2 = h1.incrementProgress(monday, 3)
        assertEquals(4, h2.progressOn(monday))
        assertTrue(h2.isTargetMetOn(monday))
    }

    @Test
    fun `incrementProgress below zero clears log`() {
        val h = quantityHabit(4).incrementProgress(monday, 2).incrementProgress(monday, -5)
        assertFalse(h.hasLogOn(monday))
    }

    @Test
    fun `incrementProgress caps at target`() {
        val h = quantityHabit(2).incrementProgress(monday, 10)
        assertEquals(2, h.progressOn(monday))
        assertTrue(h.isTargetMetOn(monday))
    }

    // ── Skipped ─────────────────────────────────────────────────────────────

    @Test
    fun `skipped day does not count as target met`() {
        val h = binaryHabit().withLog(monday, HabitLogState.Skipped)
        assertFalse(h.isTargetMetOn(monday))
        assertTrue(h.isSkippedOn(monday))
    }

    // ── currentStreak ───────────────────────────────────────────────────────

    @Test
    fun `currentStreak counts consecutive completed scheduled days`() {
        val today = friday
        val h = binaryHabit()
            .toggleBinaryOn(monday)
            .toggleBinaryOn(tuesday)
            .toggleBinaryOn(wednesday)
            .toggleBinaryOn(thursday)
        // today (friday) not yet done — walk starts from thursday
        assertEquals(4, h.currentStreak(today))
    }

    @Test
    fun `skipped days are transparent to streak`() {
        val today = friday
        val h = binaryHabit()
            .toggleBinaryOn(monday)
            .toggleBinaryOn(tuesday)
            .withLog(wednesday, HabitLogState.Skipped)
            .toggleBinaryOn(thursday)
        // wednesday is skipped (transparent) → streak walks across it
        // thu=1, wed=skip(skip), tue=2, mon=3 → 3
        assertEquals(3, h.currentStreak(today))
    }

    @Test
    fun `unscheduled days are transparent to streak`() {
        val weekdayHabit = binaryHabit(HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))
        val today = wednesday
        val h = weekdayHabit.toggleBinaryOn(monday).toggleBinaryOn(wednesday)
        // Tuesday is unscheduled → transparent
        assertEquals(2, h.currentStreak(today))
    }

    @Test
    fun `streak breaks when a scheduled day is missed`() {
        val today = friday
        val h = binaryHabit()
            .toggleBinaryOn(monday)
            .toggleBinaryOn(tuesday)
            // wednesday missed
            .toggleBinaryOn(thursday)
        // walk from thursday: streak = 1, wednesday breaks
        assertEquals(1, h.currentStreak(today))
    }

    // ── bestStreak ──────────────────────────────────────────────────────────

    @Test
    fun `bestStreak finds longest historical run`() {
        val h = binaryHabit()
            .toggleBinaryOn(monday)
            .toggleBinaryOn(tuesday)
            .toggleBinaryOn(wednesday)
            // thursday missed
            .toggleBinaryOn(friday)
        assertEquals(3, h.bestStreak(friday))
    }

    @Test
    fun `bestStreak returns zero for empty logs`() {
        assertEquals(0, binaryHabit().bestStreak(friday))
    }

    // ── streakLabel ─────────────────────────────────────────────────────────

    @Test
    fun `streakLabel renders singular day`() {
        assertEquals("1 day", streakLabel(1))
    }

    @Test
    fun `streakLabel renders plural days`() {
        assertEquals("6 days", streakLabel(6))
    }

    @Test
    fun `streakLabel renders weeks`() {
        assertEquals("2 weeks", streakLabel(14))
    }

    // ── trailingDays ────────────────────────────────────────────────────────

    @Test
    fun `trailingDays returns count days ending today`() {
        val days = trailingDays(friday, 3)
        assertEquals(listOf(wednesday, thursday, friday), days)
    }

    // ── groupByTimeBlock ────────────────────────────────────────────────────

    @Test
    fun `groupByTimeBlock uses canonical order and drops empty buckets`() {
        val h1 = binaryHabit().copy(timeBlock = TimeBlock.Evening)
        val h2 = binaryHabit().copy(id = 3, timeBlock = TimeBlock.Morning)
        val groups = groupByTimeBlock(listOf(h1, h2))
        assertEquals(listOf(TimeBlock.Morning, TimeBlock.Evening), groups.map { it.first })
    }
}
