package com.endiar.purrodoro.data.local

import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.HabitLog
import com.endiar.purrodoro.model.HabitLogState
import com.endiar.purrodoro.model.HabitOccurrence
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class EntityMappersTest {

    private val now = 1_750_000_000_000L

    // ── Task ─────────────────────────────────────────────────────────────────

    @Test
    fun `task round-trips through entity preserving priority and due date`() {
        val task = FocusTask(
            id = 7,
            title = "Design onboarding",
            minutes = 25,
            completed = true,
            plannedSessions = 3,
            completedSessions = 1,
            categoryId = 9,
            priority = TaskPriority.High,
            dueDate = LocalDate.of(2026, 6, 15),
        )

        val entity = task.toEntity(
            uuid = "task-uuid",
            categoryUuid = "cat-uuid",
            dirty = true,
            modifiedAt = now,
        )
        assertEquals("task-uuid", entity.id)
        assertEquals(7, entity.legacyId)
        assertEquals("H", entity.priorityTag)
        assertEquals("2026-06-15", entity.dueDateIso)
        assertEquals("cat-uuid", entity.categoryId)
        assertTrue(entity.dirty)
        assertEquals(now, entity.locallyModifiedAt)

        val roundTripped = entity.toDomain(legacyCategoryId = 9)
        assertEquals(task, roundTripped)
    }

    @Test
    fun `task without category or due date round-trips`() {
        val task = FocusTask(id = 2, title = "Tidy desk", minutes = 25)
        val entity = task.toEntity(uuid = "u", categoryUuid = null, dirty = false, modifiedAt = now)
        assertNull(entity.categoryId)
        assertNull(entity.dueDateIso)
        assertEquals("N", entity.priorityTag)
        assertEquals(task, entity.toDomain(legacyCategoryId = null))
    }

    // ── Category ─────────────────────────────────────────────────────────────

    @Test
    fun `category round-trips through entity`() {
        val category = Category(id = 9, name = "Work", colorArgb = 0xFF112233L)
        val entity = category.toEntity(uuid = "cat-uuid", dirty = true, modifiedAt = now)
        assertEquals("cat-uuid", entity.id)
        assertEquals(9, entity.legacyId)
        assertEquals(category, entity.toDomain())
    }

    // ── Habit + logs ─────────────────────────────────────────────────────────

    @Test
    fun `weekday habit with logs round-trips through entity and log rows`() {
        val d1 = LocalDate.of(2026, 6, 8)
        val d2 = LocalDate.of(2026, 6, 9)
        val d3 = LocalDate.of(2026, 6, 10)
        val habit = Habit(
            id = 4,
            name = "Meditate",
            emoji = "🧘",
            colorArgb = 0xFF_90CAF9L,
            createdAt = 1_718_000_000_000L,
            occurrence = HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            timeBlock = TimeBlock.Morning,
            logs = mapOf(
                d1 to HabitLog(HabitLogState.Completed),
                d2 to HabitLog(HabitLogState.Skipped),
                d3 to HabitLog(HabitLogState.Partial(2)),
            ),
        )

        val entity = habit.toEntity(uuid = "habit-uuid", dirty = true, modifiedAt = now)
        assertEquals("W", entity.occurrenceTag)
        assertEquals("MONDAY,FRIDAY", entity.occurrenceDays)
        assertEquals("M", entity.timeBlockTag)

        val logEntities = habit.toLogEntities(
            habitUuid = "habit-uuid",
            idFor = { date -> "log-$date" },
            dirty = true,
            modifiedAt = now,
        )
        assertEquals(3, logEntities.size)
        val byDate = logEntities.associateBy { it.logDateIso }
        assertEquals("C", byDate["$d1"]?.stateTag)
        assertEquals("S", byDate["$d2"]?.stateTag)
        assertEquals("P", byDate["$d3"]?.stateTag)
        assertEquals(2, byDate["$d3"]?.progress)
        assertTrue(logEntities.all { it.habitId == "habit-uuid" && it.dirty })

        val roundTripped = entity.toDomain(logEntities)
        assertEquals(habit, roundTripped)
    }

    @Test
    fun `daily habit round-trips with empty occurrence days`() {
        val habit = Habit(
            id = 1, name = "Read", emoji = "📖",
            colorArgb = 0xFF112233L, createdAt = 1_718_000_000_000L,
        )
        val entity = habit.toEntity(uuid = "u", dirty = false, modifiedAt = now)
        assertEquals("D", entity.occurrenceTag)
        assertEquals("", entity.occurrenceDays)
        assertEquals("X", entity.timeBlockTag)
        assertEquals(habit, entity.toDomain(emptyList()))
    }

    @Test
    fun `toDomain skips deleted and unparsable log rows`() {
        val habit = Habit(
            id = 1, name = "Read", emoji = "📖",
            colorArgb = 0xFF112233L, createdAt = 1_718_000_000_000L,
        )
        val entity = habit.toEntity(uuid = "h", dirty = false, modifiedAt = now)
        val good = HabitLogEntity(
            id = "l1", habitId = "h", logDateIso = "2026-06-10",
            stateTag = "C", progress = 0,
            dirty = false, locallyModifiedAt = now, deleted = false,
        )
        val tombstone = good.copy(id = "l2", logDateIso = "2026-06-11", deleted = true)
        val badDate = good.copy(id = "l3", logDateIso = "not-a-date")
        val badTag = good.copy(id = "l4", logDateIso = "2026-06-12", stateTag = "Z")

        val domain = entity.toDomain(listOf(good, tombstone, badDate, badTag))
        assertEquals(
            mapOf(LocalDate.of(2026, 6, 10) to HabitLog(HabitLogState.Completed)),
            domain.logs,
        )
    }

    // ── Session record ───────────────────────────────────────────────────────

    @Test
    fun `session record round-trips through entity`() {
        val record = SessionRecord(
            id = 1_700_000_000_000L,
            startedAt = 1_699_999_000_000L,
            endedAt = 1_700_000_500_000L,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            completedFocusCount = 3,
            totalFocusCount = 4,
            status = SessionStatus.Completed,
            taskName = "Sketch with cat",
            completedShortBreakCount = 2,
            completedLongBreakCount = 1,
        )
        val entity = record.toEntity(uuid = "rec-uuid", dirty = true, modifiedAt = now)
        assertEquals("rec-uuid", entity.id)
        assertEquals(record.id, entity.legacyId)
        assertEquals("Completed", entity.statusName)
        assertEquals(record, entity.toDomain())
    }

    @Test
    fun `unknown session status falls back to Partial`() {
        val record = SessionRecord(
            id = 1L, startedAt = 0L, endedAt = 1L,
            focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15,
            completedFocusCount = 1, totalFocusCount = 4,
            status = SessionStatus.Completed, taskName = null,
        )
        val entity = record.toEntity(uuid = "u", dirty = false, modifiedAt = now)
            .copy(statusName = "SomethingNew")
        assertEquals(SessionStatus.Partial, entity.toDomain().status)
    }
}
