package com.endiar.purrodoro.data.backup

import android.content.Context
import android.net.Uri
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
import java.io.ByteArrayOutputStream
import java.io.OutputStream
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
import org.robolectric.Shadows.shadowOf

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

    @Test
    fun export_writesJsonAndReturnsFalseWhenTheStreamIsMissingOrFails() = runTest {
        val repo = makeRepo()
        repo.setUserName("Exported")
        val mgr = manager(repo)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://purrodoro.test/export")
        val out = ByteArrayOutputStream()
        shadowOf(context.contentResolver).registerOutputStream(uri, out)

        assertTrue(mgr.export(context.contentResolver, uri))
        val parsed = BackupCodec.decode(out.toString(Charsets.UTF_8.name()))
        assertTrue(parsed is BackupParseResult.Success)
        assertEquals("Exported", (parsed as BackupParseResult.Success).bundle.displayName)

        val missing = Uri.parse("content://purrodoro.test/missing")
        assertEquals(false, mgr.export(context.contentResolver, missing))

        val broken = Uri.parse("content://purrodoro.test/broken")
        shadowOf(context.contentResolver).registerOutputStream(broken, ThrowingOutputStream())
        assertEquals(false, mgr.export(context.contentResolver, broken))
    }

    @Test
    fun restore_roundTripsAFileAndMapsReadFailures() = runTest {
        val source = makeRepo()
        source.setUserName("Round trip")
        val exported = BackupCodec.encode(source.appState.first(), "test", 1L)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://purrodoro.test/restore")
        shadowOf(context.contentResolver).registerInputStream(
            uri,
            ByteArrayInputStream(exported.toByteArray()),
        )

        val target = makeRepo()
        val outcome = manager(target).restore(context.contentResolver, uri)
        assertEquals(RestoreOutcome.Success, outcome)
        assertEquals("Round trip", target.appState.first().userName)

        val missing = Uri.parse("content://purrodoro.test/no-such-file")
        assertEquals(RestoreOutcome.ReadError, manager(target).restore(context.contentResolver, missing))

        val huge = Uri.parse("content://purrodoro.test/huge")
        shadowOf(context.contentResolver).registerInputStream(huge, OversizedInputStream())
        assertEquals(RestoreOutcome.TooLarge, manager(target).restore(context.contentResolver, huge))
    }

    /** Reports more bytes than [BackupManager.MAX_FILE_BYTES] without allocating them. */
    private class OversizedInputStream : java.io.InputStream() {
        private var remaining = BackupManager.MAX_FILE_BYTES + 1

        override fun read(): Int {
            if (remaining <= 0) return -1
            remaining--
            return 'x'.code
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val n = minOf(len, remaining)
            remaining -= n
            return n
        }
    }

    private class ThrowingOutputStream : OutputStream() {
        override fun write(b: Int) = throw java.io.IOException("disk full")
    }
}
