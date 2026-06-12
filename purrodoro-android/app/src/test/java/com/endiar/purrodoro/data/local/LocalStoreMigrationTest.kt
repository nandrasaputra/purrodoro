package com.endiar.purrodoro.data.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalStoreMigrationTest {

    private class FakeSink : MigrationSink {
        var tasks: List<TaskEntity> = emptyList()
        var categories: List<CategoryEntity> = emptyList()
        var habits: List<HabitEntity> = emptyList()
        var habitLogs: List<HabitLogEntity> = emptyList()
        var records: List<SessionRecordEntity> = emptyList()
        var stamps: List<DailyStampEntity> = emptyList()

        override suspend fun write(
            tasks: List<TaskEntity>,
            categories: List<CategoryEntity>,
            habits: List<HabitEntity>,
            habitLogs: List<HabitLogEntity>,
            records: List<SessionRecordEntity>,
            stamps: List<DailyStampEntity>,
        ) {
            this.tasks = tasks
            this.categories = categories
            this.habits = habits
            this.habitLogs = habitLogs
            this.records = records
            this.stamps = stamps
        }
    }

    private fun migration() = LocalStoreMigration(
        uuidFor = { type, key -> "$type:$key" },
        now = { 1_750_000_000_000L },
    )

    private fun blobs(
        tasksRaw: String = "",
        categoriesRaw: String = "",
        habitsRaw: String = "",
        recordsRaw: String = "",
        stampsRaw: String = "",
    ) = LegacyBlobs(tasksRaw, categoriesRaw, habitsRaw, recordsRaw, stampsRaw)

    @Test
    fun `tasks are remapped onto category uuids and marked dirty`() = runTest {
        val sink = FakeSink()
        migration().migrate(
            blobs(
                tasksRaw = "1:Read:25:false:1:0:9:N:\n2:Write:25:true:1:0::H:2026-06-15",
                categoriesRaw = "9:Work:",
            ),
            sink,
        )

        assertEquals(1, sink.categories.size)
        val workCategory = sink.categories.single()
        assertEquals(9, workCategory.legacyId)

        assertEquals(2, sink.tasks.size)
        val task1 = sink.tasks.single { it.legacyId == 1 }
        val task2 = sink.tasks.single { it.legacyId == 2 }
        assertEquals(workCategory.id, task1.categoryId)
        assertNull(task2.categoryId)
        assertEquals("H", task2.priorityTag)
        assertEquals("2026-06-15", task2.dueDateIso)

        assertTrue(sink.tasks.all { it.dirty })
        assertTrue(sink.categories.all { it.dirty })
        assertEquals(1_750_000_000_000L, task1.locallyModifiedAt)
    }

    @Test
    fun `dangling category reference maps to null`() = runTest {
        val sink = FakeSink()
        migration().migrate(
            blobs(tasksRaw = "1:Read:25:false:1:0:99:N:"),
            sink,
        )
        assertEquals(1, sink.tasks.size)
        assertNull(sink.tasks.single().categoryId)
        assertTrue(sink.categories.isEmpty())
    }

    @Test
    fun `habit logs become one row per date`() = runTest {
        val sink = FakeSink()
        // v2 habit line: id|urlName|urlEmoji|colorArgb|createdAt|B|1|oTag|oData|bTag|logsCsv
        migration().migrate(
            blobs(
                habitsRaw = "3|Meditate|%F0%9F%A7%98|4287944601|1718000000000|B|1|D||X|" +
                    "2026-06-10~C,2026-06-11~S",
            ),
            sink,
        )

        assertEquals(1, sink.habits.size)
        val habit = sink.habits.single()
        assertEquals(3, habit.legacyId)
        assertEquals("D", habit.occurrenceTag)

        assertEquals(2, sink.habitLogs.size)
        val byDate = sink.habitLogs.associateBy { it.logDateIso }
        assertEquals("C", byDate["2026-06-10"]?.stateTag)
        assertEquals("S", byDate["2026-06-11"]?.stateTag)
        assertTrue(sink.habitLogs.all { it.habitId == habit.id && it.dirty })
    }

    @Test
    fun `session records and stamps migrate dirty`() = runTest {
        val sink = FakeSink()
        migration().migrate(
            blobs(
                recordsRaw = "1700000000000|1699999000000|1700000500000|25|5|15|3|4|Completed||2|1",
                stampsRaw = "2026-06-10=2",
            ),
            sink,
        )
        assertEquals(1, sink.records.size)
        val record = sink.records.single()
        assertEquals(1_700_000_000_000L, record.legacyId)
        assertEquals("Completed", record.statusName)
        assertTrue(record.dirty)

        assertEquals(1, sink.stamps.size)
        val stamp = sink.stamps.single()
        assertEquals("2026-06-10", stamp.stampDateIso)
        assertEquals(2, stamp.count)
        assertTrue(stamp.dirty)
    }

    @Test
    fun `empty store migrates nothing`() = runTest {
        val sink = FakeSink()
        migration().migrate(blobs(), sink)
        assertTrue(sink.tasks.isEmpty())
        assertTrue(sink.categories.isEmpty())
        assertTrue(sink.habits.isEmpty())
        assertTrue(sink.habitLogs.isEmpty())
        assertTrue(sink.records.isEmpty())
        assertTrue(sink.stamps.isEmpty())
    }

    @Test
    fun `bracket empty blobs migrate nothing`() = runTest {
        val sink = FakeSink()
        migration().migrate(
            blobs(tasksRaw = "[]", categoriesRaw = "[]", habitsRaw = "[]"),
            sink,
        )
        assertTrue(sink.tasks.isEmpty())
        assertTrue(sink.categories.isEmpty())
        assertTrue(sink.habits.isEmpty())
    }
}
