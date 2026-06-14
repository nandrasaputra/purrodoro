package com.endiar.purrodoro.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins the `markAllDirty` sync helper on every synced DAO: all live rows are
 * marked for re-push with the given timestamp; tombstones stay untouched (the
 * claim/keep re-upload must not resurrect or re-push old deletions).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MarkAllDirtyDaoTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PurrodoroDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.close()
    }

    private fun task(id: String, legacyId: Int, deleted: Boolean = false) = TaskEntity(
        id = id, legacyId = legacyId, title = "t", completed = false,
        minutes = 25, plannedSessions = 1, completedSessions = 0,
        categoryId = null, priorityTag = "N", dueDateIso = null,
        dirty = false, locallyModifiedAt = 1L, deleted = deleted,
    )

    @Test
    fun `markAllDirty flags every live task and skips tombstones`() = runTest {
        db.taskDao().upsert(listOf(task("a", 1), task("b", 2), task("dead", 3, deleted = true)))

        db.taskDao().markAllDirty(now = 42L)

        val dirty = db.taskDao().getDirty()
        assertEquals(setOf("a", "b"), dirty.map { it.id }.toSet())
        assertTrue(dirty.all { it.locallyModifiedAt == 42L })
        val tombstone = db.taskDao().getById("dead")!!
        assertFalse("tombstone untouched", tombstone.dirty)
        assertEquals(1L, tombstone.locallyModifiedAt)
    }

    @Test
    fun `markAllDirty covers all six synced tables`() = runTest {
        db.taskDao().upsert(listOf(task("t1", 1)))
        db.categoryDao().upsert(
            listOf(
                CategoryEntity(
                    id = "c1", legacyId = 1, name = "Work", colorArgb = null,
                    dirty = false, locallyModifiedAt = 1L, deleted = false,
                )
            )
        )
        db.habitDao().upsert(
            listOf(
                HabitEntity(
                    id = "h1", legacyId = 1, name = "Read", emoji = "", colorArgb = 0L,
                    createdAtMillis = 0L, occurrenceTag = "D", occurrenceDays = "",
                    timeBlockTag = "X", dirty = false, locallyModifiedAt = 1L, deleted = false,
                )
            )
        )
        db.habitLogDao().upsert(
            listOf(
                HabitLogEntity(
                    id = "l1", habitId = "h1", logDateIso = "2026-06-12", stateTag = "C",
                    progress = 0, dirty = false, locallyModifiedAt = 1L, deleted = false,
                )
            )
        )
        db.sessionRecordDao().upsert(
            listOf(
                SessionRecordEntity(
                    id = "r1", legacyId = 1L, startedAt = 0L, endedAt = 1L,
                    focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15,
                    completedFocusCount = 1, totalFocusCount = 4,
                    completedShortBreakCount = 0, completedLongBreakCount = 0,
                    statusName = "Completed", taskName = null, focusTypeName = null,
                    dirty = false, locallyModifiedAt = 1L, deleted = false,
                )
            )
        )
        db.dailyStampDao().upsert(
            listOf(
                DailyStampEntity(
                    id = "s1", stampDateIso = "2026-06-12", count = 1,
                    dirty = false, locallyModifiedAt = 1L, deleted = false,
                )
            )
        )

        val now = 42L
        db.taskDao().markAllDirty(now)
        db.categoryDao().markAllDirty(now)
        db.habitDao().markAllDirty(now)
        db.habitLogDao().markAllDirty(now)
        db.sessionRecordDao().markAllDirty(now)
        db.dailyStampDao().markAllDirty(now)

        assertEquals(1, db.taskDao().getDirty().size)
        assertEquals(1, db.categoryDao().getDirty().size)
        assertEquals(1, db.habitDao().getDirty().size)
        assertEquals(1, db.habitLogDao().getDirty().size)
        assertEquals(1, db.sessionRecordDao().getDirty().size)
        assertEquals(1, db.dailyStampDao().getDirty().size)
    }
}
