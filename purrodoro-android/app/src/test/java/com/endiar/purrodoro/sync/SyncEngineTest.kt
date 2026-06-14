package com.endiar.purrodoro.sync

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.local.DailyStampEntity
import com.endiar.purrodoro.data.local.HabitLogEntity
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.data.local.SessionRecordEntity
import com.endiar.purrodoro.data.local.TaskEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PurrodoroDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val api = FakeSyncApi()
    private val checkpoints = FakeCheckpoints()

    @After
    fun tearDown() {
        db.close()
    }

    private fun buildEngine(): SyncEngine = SyncEngine(api, buildTables(), checkpoints)

    private fun buildTables(): List<TableSync> {
        var nextTask = 100
        var nextCategory = 100
        var nextHabit = 100
        val userId: suspend () -> String = { "user-1" }
        val inTransaction: TransactionRunner = { block -> db.withTransaction { block() } }
        return listOf(
            CategoryTableSync(db.categoryDao(), userId, inTransaction) { nextCategory++ },
            TaskTableSync(db.taskDao(), userId, inTransaction) { nextTask++ },
            HabitTableSync(db.habitDao(), userId, inTransaction) { nextHabit++ },
            HabitLogTableSync(db.habitLogDao(), userId, inTransaction),
            SessionRecordTableSync(db.sessionRecordDao(), userId, inTransaction),
            DailyStampTableSync(db.dailyStampDao(), userId, inTransaction),
        )
    }

    private fun localTask(
        id: String,
        legacyId: Int,
        title: String,
        dirty: Boolean = true,
        modifiedAt: Long = 1L,
    ) = TaskEntity(
        id = id, legacyId = legacyId, title = title, completed = false,
        minutes = 25, plannedSessions = 1, completedSessions = 0,
        categoryId = null, priorityTag = "N", dueDateIso = null,
        dirty = dirty, locallyModifiedAt = modifiedAt, deleted = false,
    )

    private fun taskRow(
        id: String,
        title: String,
        updatedAt: String,
        deleted: Boolean = false,
    ): JsonElement = buildJsonObject {
        put("id", id)
        put("user_id", "user-1")
        put("title", title)
        put("completed", false)
        put("minutes", 25)
        put("planned_sessions", 1)
        put("completed_sessions", 0)
        put("category_id", JsonNull)
        put("priority", "N")
        put("due_date", JsonNull)
        put("updated_at", updatedAt)
        put("deleted", deleted)
    }

    private fun categoryRow(
        id: String,
        name: String,
        updatedAt: String,
        deleted: Boolean = false,
    ): JsonElement = buildJsonObject {
        put("id", id)
        put("user_id", "user-1")
        put("name", name)
        put("color_argb", JsonNull)
        put("updated_at", updatedAt)
        put("deleted", deleted)
    }

    private fun localLog(
        id: String,
        dirty: Boolean,
        habitId: String = "habit-uuid",
        logDate: String = "2026-06-12",
        state: String = "C",
        modifiedAt: Long = 1L,
    ) = HabitLogEntity(
        id = id, habitId = habitId, logDateIso = logDate, stateTag = state,
        progress = 0, dirty = dirty, locallyModifiedAt = modifiedAt, deleted = false,
    )

    private fun habitLogRow(
        id: String,
        updatedAt: String,
        habitId: String = "habit-uuid",
        logDate: String = "2026-06-12",
        state: String = "C",
        deleted: Boolean = false,
    ): JsonElement = buildJsonObject {
        put("id", id)
        put("user_id", "user-1")
        put("habit_id", habitId)
        put("log_date", logDate)
        put("state", state)
        put("progress", 0)
        put("updated_at", updatedAt)
        put("deleted", deleted)
    }

    @Test
    fun `push clears dirty and pull applies remote rows`() = runTest {
        db.taskDao().upsert(listOf(localTask("local-1", 1, "Local task")))
        api.remote["tasks"] = mutableListOf(taskRow("remote-1", "Remote task", "2026-06-12T10:00:00+00:00"))

        val result = buildEngine().syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)

        // The dirty row was pushed without the server-owned updated_at column.
        val pushed = api.upserts.getValue("tasks")
        assertEquals(1, pushed.size)
        assertEquals("local-1", pushed[0].jsonObject.getValue("id").jsonPrimitive.content)
        assertEquals("user-1", pushed[0].jsonObject.getValue("user_id").jsonPrimitive.content)
        assertNull(pushed[0].jsonObject["updated_at"])
        assertFalse(db.taskDao().getById("local-1")!!.dirty)

        // The remote row was applied locally as a clean row with a minted legacy id.
        val applied = db.taskDao().getById("remote-1")
        assertNotNull(applied)
        assertEquals("Remote task", applied!!.title)
        assertEquals(100, applied.legacyId)
        assertFalse(applied.dirty)
    }

    @Test
    fun `dirty local row survives pull unchanged and stays dirty`() = runTest {
        db.taskDao().upsert(listOf(localTask("local-1", 1, "Pushed title", modifiedAt = 1L)))
        // Simulate a user edit racing the push: by the time the server upsert
        // returns, the row has changed again, so markPushed must keep it dirty.
        api.onUpsert = { table ->
            if (table == "tasks") {
                val current = db.taskDao().getById("local-1")!!
                db.taskDao().upsert(
                    listOf(current.copy(title = "Edited during push", locallyModifiedAt = 2L, dirty = true))
                )
            }
        }
        api.remote["tasks"] = mutableListOf(taskRow("local-1", "Remote title", "2026-06-12T10:00:00+00:00"))

        val result = buildEngine().syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)
        val row = db.taskDao().getById("local-1")!!
        assertEquals("Edited during push", row.title)
        assertTrue(row.dirty)
    }

    @Test
    fun `checkpoint advances to max updated_at and feeds the next pull`() = runTest {
        api.remote["categories"] = mutableListOf(
            categoryRow("c1", "Work", "2026-06-12T10:00:00+00:00"),
            categoryRow("c2", "Home", "2026-06-12T11:00:00+00:00"),
        )
        val engine = buildEngine()

        engine.syncOnce()

        assertEquals(null, api.pullArgs.getValue("categories")[0])
        assertEquals("2026-06-12T11:00:00+00:00", checkpoints.map["categories"])

        engine.syncOnce()

        assertEquals("2026-06-12T11:00:00+00:00", api.pullArgs.getValue("categories")[1])
    }

    @Test
    fun `failing table does not block others and keeps its checkpoint`() = runTest {
        api.failPullTables += "tasks"
        api.remote["categories"] = mutableListOf(categoryRow("c1", "Work", "2026-06-12T10:00:00+00:00"))

        val result = buildEngine().syncOnce()

        assertEquals(SyncEngine.SyncResult.PartialFailure(listOf("tasks")), result)
        assertNull(checkpoints.map["tasks"])
        // The healthy table still pulled, applied, and advanced its checkpoint.
        assertNotNull(db.categoryDao().getById("c1"))
        assertEquals("2026-06-12T10:00:00+00:00", checkpoints.map["categories"])
    }

    @Test
    fun `tables are processed in FK order`() = runTest {
        buildEngine().syncOnce()

        assertEquals(
            listOf("categories", "tasks", "habits", "habit_logs", "session_records", "daily_stamps"),
            api.pullOrder,
        )
    }

    // --- natural-key adoption (two devices logging the same habit + day) ---

    @Test
    fun `clean local row adopts the remote row's UUID via the natural key`() = runTest {
        db.habitLogDao().upsert(listOf(localLog("log-local", dirty = false, state = "S")))
        api.remote["habit_logs"] = mutableListOf(
            habitLogRow("log-server", "2026-06-12T10:00:00+00:00", state = "C"),
        )

        val result = buildEngine().syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)
        // The local row was re-keyed onto the server identity and took the
        // remote content (it was clean, so the remote wins).
        assertNull(db.habitLogDao().getById("log-local"))
        val adopted = db.habitLogDao().getById("log-server")
        assertNotNull(adopted)
        assertEquals("C", adopted!!.stateTag)
        assertFalse(adopted.dirty)
    }

    @Test
    fun `dirty local row is re-keyed and its next push targets the adopted server id`() = runTest {
        db.habitLogDao().upsert(listOf(localLog("log-local", dirty = true, state = "S", modifiedAt = 1L)))
        // A user edit lands while the (post-adoption) push is in flight, so
        // markPushed must leave the row dirty for the next pass.
        api.onUpsert = { table ->
            if (table == "habit_logs") {
                db.habitLogDao().getById("log-server")?.let { current ->
                    db.habitLogDao().upsert(listOf(current.copy(locallyModifiedAt = 2L, dirty = true)))
                }
            }
        }
        api.remote["habit_logs"] = mutableListOf(
            habitLogRow("log-server", "2026-06-12T10:00:00+00:00", state = "C"),
        )
        val engine = buildEngine()

        engine.syncOnce()

        // The dirty local content survived, re-keyed onto the server id, and
        // the concurrent edit kept it dirty through the same pass's push.
        assertNull(db.habitLogDao().getById("log-local"))
        val rekeyed = db.habitLogDao().getById("log-server")
        assertNotNull(rekeyed)
        assertEquals("S", rekeyed!!.stateTag)
        assertTrue(rekeyed.dirty)

        api.onUpsert = null
        engine.syncOnce()

        // The second pass pushes the surviving content under the adopted id.
        val lastPush = api.upserts.getValue("habit_logs").last().jsonObject
        assertEquals("log-server", lastPush.getValue("id").jsonPrimitive.content)
        assertEquals("S", lastPush.getValue("state").jsonPrimitive.content)
        assertFalse(db.habitLogDao().getById("log-server")!!.dirty)
    }

    private fun localRecord(
        id: String,
        legacyId: Long,
        taskName: String? = "Guest work",
        dirty: Boolean = true,
        modifiedAt: Long = 1L,
    ) = SessionRecordEntity(
        id = id, legacyId = legacyId, startedAt = 1_000L, endedAt = 2_000L,
        focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15,
        completedFocusCount = 1, totalFocusCount = 1,
        completedShortBreakCount = 0, completedLongBreakCount = 0,
        statusName = "COMPLETED", taskName = taskName, focusTypeName = null,
        dirty = dirty, locallyModifiedAt = modifiedAt, deleted = false,
    )

    private fun sessionRecordRow(
        id: String,
        localId: Long,
        updatedAt: String,
        taskName: String = "Cloud work",
    ): JsonElement = buildJsonObject {
        put("id", id)
        put("user_id", "user-1")
        put("local_id", localId)
        put("started_at", 5_000L)
        put("ended_at", 6_000L)
        put("focus_minutes", 25)
        put("short_break_minutes", 5)
        put("long_break_minutes", 15)
        put("completed_focus_count", 1)
        put("total_focus_count", 1)
        put("completed_short_break_count", 0)
        put("completed_long_break_count", 0)
        put("status", "COMPLETED")
        put("task_name", taskName)
        put("updated_at", updatedAt)
        put("deleted", false)
    }

    @Test
    fun `first claim merge with colliding local_id adopts the server row instead of wedging`() = runTest {
        // Guest data claimed into an account that already has cloud data:
        // both sides independently minted local_id = 1. The fake api enforces
        // the server unique (user_id, local_id) index, so a push-first pass
        // would 409 forever. Every pass pulls first, so the merge re-keys the
        // dirty local row onto the server UUID and the push is an UPDATE.
        db.sessionRecordDao().upsert(listOf(localRecord("rec-local", legacyId = 1)))
        api.remote["session_records"] = mutableListOf(
            sessionRecordRow("rec-server", localId = 1, updatedAt = "2026-06-12T10:00:00+00:00"),
        )

        val result = buildEngine().syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)
        // The dirty local content survived, adopted onto the server identity.
        assertNull(db.sessionRecordDao().getById("rec-local"))
        val adopted = db.sessionRecordDao().getById("rec-server")
        assertNotNull(adopted)
        assertEquals("Guest work", adopted!!.taskName)
        assertFalse("pushed within the same pass", adopted.dirty)
        // The push targeted the adopted id — no colliding insert.
        val pushed = api.upserts.getValue("session_records").single().jsonObject
        assertEquals("rec-server", pushed.getValue("id").jsonPrimitive.content)
        assertEquals("Guest work", pushed.getValue("task_name").jsonPrimitive.content)
    }

    private fun localStamp(
        id: String,
        date: String,
        count: Int,
        dirty: Boolean = true,
        modifiedAt: Long = 1L,
    ) = DailyStampEntity(
        id = id, stampDateIso = date, count = count,
        dirty = dirty, locallyModifiedAt = modifiedAt, deleted = false,
    )

    private fun dailyStampRow(
        id: String,
        date: String,
        count: Int,
        updatedAt: String,
    ): JsonElement = buildJsonObject {
        put("id", id)
        put("user_id", "user-1")
        put("stamp_date", date)
        put("count", count)
        put("updated_at", updatedAt)
        put("deleted", false)
    }

    @Test
    fun `same-account devices minting the same day's stamp converge on a plain delta pass`() = runTest {
        // Another of this account's devices already pushed today's stamp;
        // this device independently created its own row for the same date.
        // The fake api enforces the server unique (user_id, stamp_date), so
        // a push-first pass would 409 forever — an ORDINARY delta pass must
        // heal it: pull first, adopt the server identity, push an update.
        db.dailyStampDao().upsert(listOf(localStamp("stamp-local", "2026-06-12", count = 3)))
        api.remote["daily_stamps"] = mutableListOf(
            dailyStampRow("stamp-server", "2026-06-12", count = 1, updatedAt = "2026-06-12T10:00:00+00:00"),
        )

        val result = buildEngine().syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)
        assertNull(db.dailyStampDao().getById("stamp-local"))
        val adopted = db.dailyStampDao().getById("stamp-server")
        assertNotNull(adopted)
        assertEquals("dirty local count wins and uploads", 3, adopted!!.count)
        assertFalse("pushed within the same pass", adopted.dirty)
        val pushed = api.upserts.getValue("daily_stamps").single().jsonObject
        assertEquals("stamp-server", pushed.getValue("id").jsonPrimitive.content)
        assertEquals(3, pushed.getValue("count").jsonPrimitive.content.toInt())
    }

    @Test
    fun `remote tombstone applied through the engine soft-deletes locally`() = runTest {
        db.habitLogDao().upsert(listOf(localLog("log-1", dirty = false)))
        api.remote["habit_logs"] = mutableListOf(
            habitLogRow("log-1", "2026-06-12T10:00:00+00:00", deleted = true),
        )

        val result = buildEngine().syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)
        val row = db.habitLogDao().getById("log-1")
        assertNotNull(row)
        assertTrue(row!!.deleted)
        assertFalse(row.dirty)
    }

    @Test
    fun `concurrent syncOnce calls collapse into one pass`() = runTest {
        val gate = CompletableDeferred<Unit>()
        api.gateFirstPull = gate
        val engine = buildEngine()

        val first = async { engine.syncOnce() }
        runCurrent()
        val second = async { engine.syncOnce() }
        runCurrent()
        gate.complete(Unit)

        assertEquals(SyncEngine.SyncResult.Success, first.await())
        assertEquals(SyncEngine.SyncResult.Success, second.await())
        // The api saw exactly one pass over the six tables.
        assertEquals(6, api.pullOrder.size)
    }
}

private class FakeSyncApi : SyncApi {
    val remote = mutableMapOf<String, MutableList<JsonElement>>()
    val upserts = mutableMapOf<String, MutableList<JsonElement>>()
    val pullArgs = mutableMapOf<String, MutableList<String?>>()
    val pullOrder = mutableListOf<String>()
    val failPullTables = mutableSetOf<String>()
    var onUpsert: (suspend (String) -> Unit)? = null
    var gateFirstPull: CompletableDeferred<Unit>? = null
    private var firstPullSeen = false

    override suspend fun upsert(table: String, rows: List<JsonElement>, conflictKey: String) {
        uniqueIndexes[table]?.let { enforceUniqueIndex(table, it, rows) }
        upserts.getOrPut(table) { mutableListOf() }.addAll(rows)
        onUpsert?.invoke(table)
    }

    /**
     * Mirrors the server's natural-key unique indexes: an upsert keyed on id
     * that would insert a second row with an already-taken natural key fails
     * like PostgREST's 409.
     */
    private val uniqueIndexes = mapOf(
        "session_records" to listOf("user_id", "local_id"),
        "habit_logs" to listOf("user_id", "habit_id", "log_date"),
        "daily_stamps" to listOf("user_id", "stamp_date"),
    )

    private fun enforceUniqueIndex(table: String, columns: List<String>, rows: List<JsonElement>) {
        fun key(row: JsonElement) = columns.map { row.jsonObject.getValue(it).jsonPrimitive.content }
        fun id(row: JsonElement) = row.jsonObject.getValue("id").jsonPrimitive.content
        for (row in rows) {
            val clash = remote[table].orEmpty().any { id(it) != id(row) && key(it) == key(row) }
            if (clash) {
                throw IOException("409: duplicate key violates unique ${columns.joinToString()} on $table")
            }
        }
    }

    override suspend fun pullSince(table: String, afterIso: String?): List<JsonElement> {
        if (!firstPullSeen) {
            firstPullSeen = true
            gateFirstPull?.await()
        }
        if (table in failPullTables) throw IOException("pull failed for $table")
        pullOrder += table
        pullArgs.getOrPut(table) { mutableListOf() }.add(afterIso)
        return remote[table].orEmpty().filter { row ->
            val updatedAt = row.jsonObject.getValue("updated_at").jsonPrimitive.content
            afterIso == null || updatedAt > afterIso
        }
    }
}

private class FakeCheckpoints : CheckpointStore {
    val map = mutableMapOf<String, String>()
    override suspend fun get(table: String): String? = map[table]
    override suspend fun set(table: String, iso: String) {
        map[table] = iso
    }
}
