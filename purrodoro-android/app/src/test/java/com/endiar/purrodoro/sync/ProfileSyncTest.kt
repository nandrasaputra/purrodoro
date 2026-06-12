package com.endiar.purrodoro.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.model.TimerSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Engine-level coverage for [ProfileTableSync]: the singleton profile row
 * pushes keyed by `user_id` and a pulled remote profile lands in the
 * DataStore-backed name/settings values.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProfileSyncTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PurrodoroDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val api = RecordingSyncApi()
    private val checkpoints = mutableMapOf<String, String>()

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("psync_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    private fun buildEngine(repo: AppRepository): SyncEngine = SyncEngine(
        api = api,
        tables = listOf(
            ProfileTableSync(
                db.profileDao(),
                { "user-1" },
                { block -> db.withTransaction { block() } },
                repo::applySyncedProfilePrefs,
            ),
        ),
        checkpoints = object : CheckpointStore {
            override suspend fun get(table: String): String? = checkpoints[table]
            override suspend fun set(table: String, iso: String) {
                checkpoints[table] = iso
            }
        },
    )

    private fun profileRow(
        displayName: String,
        focusMinutes: Int,
        updatedAt: String,
    ): JsonElement = buildJsonObject {
        put("user_id", "user-1")
        put("display_name", displayName)
        putJsonObject("timer_settings") {
            put("focus_minutes", focusMinutes)
        }
        put("updated_at", updatedAt)
        put("deleted", false)
    }

    @Test
    fun `dirty profile pushes keyed by user_id and lands clean`() = runTest {
        val repo = makeRepo()
        repo.setUserName("Nandra")
        repo.setSettings(TimerSettings(focusMinutes = 50))

        val result = buildEngine(repo).syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)
        assertEquals("user_id", api.conflictKeys["profiles"])
        val pushed = api.upserts.getValue("profiles").single().jsonObject
        assertEquals("user-1", pushed.getValue("user_id").jsonPrimitive.content)
        assertEquals("Nandra", pushed.getValue("display_name").jsonPrimitive.content)
        assertEquals(
            50,
            pushed.getValue("timer_settings").jsonObject
                .getValue("focus_minutes").jsonPrimitive.content.toInt(),
        )
        assertNull("server-owned column stripped from the push", pushed["updated_at"])
        assertFalse(db.profileDao().get()!!.dirty)
    }

    @Test
    fun `pulled remote profile lands in prefs and the row is clean`() = runTest {
        val repo = makeRepo()
        api.remote["profiles"] = mutableListOf(
            profileRow("Cloud Name", focusMinutes = 30, updatedAt = "2026-06-12T10:00:00+00:00"),
        )

        val result = buildEngine(repo).syncOnce()

        assertEquals(SyncEngine.SyncResult.Success, result)
        val state = repo.appState.first()
        assertEquals("Cloud Name", state.userName)
        assertEquals(30, state.settings.focusMinutes)
        val row = db.profileDao().get()!!
        assertFalse(row.dirty)
        assertEquals("Cloud Name", row.displayName)
        assertEquals("2026-06-12T10:00:00+00:00", checkpoints["profiles"])
    }

    @Test
    fun `dirty local profile survives a concurrent edit during the push`() = runTest {
        val repo = makeRepo()
        repo.setUserName("Pushed Name")
        // A user edit lands while the push is in flight: markPushed must keep
        // the row dirty, and the pull must not clobber the newer local value.
        api.onUpsert = { table ->
            if (table == "profiles") repo.setUserName("Edited During Push")
        }
        api.remote["profiles"] = mutableListOf(
            profileRow("Remote Name", focusMinutes = 30, updatedAt = "2026-06-12T10:00:00+00:00"),
        )

        buildEngine(repo).syncOnce()

        val row = db.profileDao().get()!!
        assertTrue("concurrent edit stays dirty", row.dirty)
        assertEquals("Edited During Push", row.displayName)
        assertEquals("Edited During Push", repo.appState.first().userName)
    }
}

private class RecordingSyncApi : SyncApi {
    val remote = mutableMapOf<String, MutableList<JsonElement>>()
    val upserts = mutableMapOf<String, MutableList<JsonElement>>()
    val conflictKeys = mutableMapOf<String, String>()
    var onUpsert: (suspend (String) -> Unit)? = null

    override suspend fun upsert(table: String, rows: List<JsonElement>, conflictKey: String) {
        upserts.getOrPut(table) { mutableListOf() }.addAll(rows)
        conflictKeys[table] = conflictKey
        onUpsert?.invoke(table)
    }

    override suspend fun pullSince(table: String, afterIso: String?): List<JsonElement> =
        remote[table].orEmpty().filter { row ->
            val updatedAt = row.jsonObject.getValue("updated_at").jsonPrimitive.content
            afterIso == null || updatedAt > afterIso
        }
}
