package com.endiar.purrodoro.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.AppState
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.HomeScene
import com.endiar.purrodoro.model.TimerSettings
import java.io.ByteArrayInputStream
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PurrodoroDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() = db.close()

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("bm_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    private fun manager(repo: AppRepository) =
        BackupManager(repo, appVersionName = "test", safetyDir = tmpFolder.newFolder("backups_${System.nanoTime()}"))

    private fun sampleState(): AppState = AppState(
        glassReadabilityMode = GlassReadabilityMode.Adaptive,
        appearanceMode = AppearanceMode.Light,
        language = AppLanguage.System,
        userName = "Tester",
        selectedSoundId = 1,
        completedSessions = 0,
        settings = TimerSettings(),
        nextTaskId = 2,
        stamps = mapOf(LocalDate.of(2026, 6, 18) to 2),
        categories = listOf(Category(1, "Work", null)),
        tasks = listOf(FocusTask(1, "Imported task", 25)),
        homeScene = HomeScene.Room,
    )

    // ── readCapped ──────────────────────────────────────────────────────────

    @Test
    fun readCapped_returnsTextWhenUnderCap() {
        val result = BackupManager.readCapped(ByteArrayInputStream("hello".toByteArray()), max = 1024)
        assertEquals(BackupManager.ReadResult.Ok("hello"), result)
    }

    @Test
    fun readCapped_tooLargeWhenOverCap() {
        val big = ByteArrayInputStream(ByteArray(100))
        assertEquals(BackupManager.ReadResult.TooLarge, BackupManager.readCapped(big, max = 10))
    }

    // ── applyDecoded outcome mapping ──────────────────────────────────────────

    @Test
    fun applyDecoded_malformedJson() = runTest {
        val outcome = manager(makeRepo()).applyDecoded("definitely not json")
        assertEquals(RestoreOutcome.Malformed, outcome)
    }

    @Test
    fun applyDecoded_unsupportedVersion() = runTest {
        val outcome = manager(makeRepo()).applyDecoded("""{"schema_version": 9999, "data": {}}""")
        assertEquals(RestoreOutcome.UnsupportedVersion(9999), outcome)
    }

    @Test
    fun applyDecoded_successAppliesDataAndWritesSafetyBackup() = runTest {
        val repo = makeRepo()
        val safetyDir = tmpFolder.newFolder("backups_apply")
        val mgr = BackupManager(repo, "test", safetyDir)
        val json = BackupCodec.encode(sampleState(), "test", 1L)

        val outcome = mgr.applyDecoded(json)

        assertEquals(RestoreOutcome.Success, outcome)
        val state = repo.appState.first()
        assertEquals(listOf("Imported task"), state.tasks.map { it.title })
        assertEquals(2, state.stamps[LocalDate.of(2026, 6, 18)])
        // a safety copy of the (empty) prior state was written first
        val safetyFiles = safetyDir.listFiles { f -> f.name.startsWith("autobackup-") }
        assertTrue("expected a safety backup file", (safetyFiles?.size ?: 0) >= 1)
    }

    @Test
    fun applyDecoded_keepsAtMostThreeSafetyBackups() = runTest {
        val repo = makeRepo()
        val safetyDir = tmpFolder.newFolder("backups_prune")
        val mgr = BackupManager(repo, "test", safetyDir)
        val json = BackupCodec.encode(sampleState(), "test", 1L)

        repeat(5) {
            mgr.applyDecoded(json)
            Thread.sleep(3) // distinct millisecond-based filenames
        }

        val safetyFiles = safetyDir.listFiles { f -> f.name.startsWith("autobackup-") } ?: emptyArray()
        assertTrue("expected at most 3 safety backups, got ${safetyFiles.size}", safetyFiles.size <= 3)
    }
}
