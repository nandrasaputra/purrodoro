package com.endiar.purrodoro.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.local.PurrodoroDatabase
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
        return listOf(
            CategoryTableSync(db.categoryDao(), userId) { nextCategory++ },
            TaskTableSync(db.taskDao(), userId) { nextTask++ },
            HabitTableSync(db.habitDao(), userId) { nextHabit++ },
            HabitLogTableSync(db.habitLogDao(), userId),
            SessionRecordTableSync(db.sessionRecordDao(), userId),
            DailyStampTableSync(db.dailyStampDao(), userId),
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
        upserts.getOrPut(table) { mutableListOf() }.addAll(rows)
        onUpsert?.invoke(table)
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
