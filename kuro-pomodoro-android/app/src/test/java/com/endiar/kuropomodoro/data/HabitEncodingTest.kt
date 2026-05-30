package com.endiar.kuropomodoro.data

import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitLog
import com.endiar.kuropomodoro.model.HabitLogState
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.TimeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitEncodingTest {

    private val someDate = LocalDate.of(2024, 6, 10)

    private fun baseHabit(
        id: Int = 1,
        occurrence: HabitOccurrence = HabitOccurrence.Daily,
        timeBlock: TimeBlock = TimeBlock.Anytime,
        logs: Map<LocalDate, HabitLog> = emptyMap(),
    ) = Habit(
        id = id, name = "Meditate", emoji = "🧘",
        colorArgb = 0xFF_90CAF9L, createdAt = 1_718_000_000_000L,
        occurrence = occurrence, timeBlock = timeBlock, logs = logs,
    )

    // ── Round-trip ───────────────────────────────────────────────────────────

    @Test
    fun `v2 round-trip preserves all fields`() {
        val habit = baseHabit(
            occurrence = HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            timeBlock = TimeBlock.Morning,
            logs = mapOf(
                someDate to HabitLog(HabitLogState.Completed),
                someDate.plusDays(2) to HabitLog(HabitLogState.Skipped),
            ),
        )
        val decoded = decodeHabits(encodeHabits(listOf(habit)))
        assertEquals(1, decoded.size)
        assertEquals(habit, decoded[0])
    }

    @Test
    fun `empty string decodes to empty list`() {
        assertTrue(decodeHabits("").isEmpty())
    }

    @Test
    fun `bracket empty decodes to empty list`() {
        assertTrue(decodeHabits("[]").isEmpty())
    }

    @Test
    fun `multiple habits round-trip`() {
        val h1 = baseHabit(id = 1, timeBlock = TimeBlock.Morning)
        val h2 = baseHabit(id = 2, timeBlock = TimeBlock.Evening)
        val decoded = decodeHabits(encodeHabits(listOf(h1, h2)))
        assertEquals(2, decoded.size)
        assertEquals(h1, decoded[0])
        assertEquals(h2, decoded[1])
    }

    // ── v1 migration ─────────────────────────────────────────────────────────

    @Test
    fun `v1 line migrates to Binary Daily Anytime with Completed logs`() {
        val d1 = LocalDate.of(2024, 1, 10)
        val d2 = LocalDate.of(2024, 1, 11)
        // v1 schema: id|urlName|urlEmoji|colorArgb|createdAt|completionDatesCsv
        val v1Line = "7|Meditate|%F0%9F%A7%98|${0xFF_90CAF9L}|${1_718_000_000_000L}|$d1,$d2"
        val decoded = decodeHabits(v1Line)
        assertEquals(1, decoded.size)
        val h = decoded[0]
        assertEquals(7, h.id)
        assertEquals(HabitOccurrence.Daily, h.occurrence)
        assertEquals(TimeBlock.Anytime, h.timeBlock)
        assertEquals(HabitLogState.Completed, h.logs[d1]?.state)
        assertEquals(HabitLogState.Completed, h.logs[d2]?.state)
    }

    @Test
    fun `v1 re-encodes as v2`() {
        val v1Line = "7|Meditate|%F0%9F%A7%98|${0xFF_90CAF9L}|${1_718_000_000_000L}|"
        val decoded = decodeHabits(v1Line)
        val reEncoded = encodeHabits(decoded)
        // re-encoding produces v2 (11 pipe-separated fields)
        assertEquals(11, reEncoded.split("|").size)
    }
}
