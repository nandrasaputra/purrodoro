package com.endiar.purrodoro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.backup.RestoreBundle
import com.endiar.purrodoro.data.backup.RestorePrefs
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.HabitLog
import com.endiar.purrodoro.model.HabitLogState
import com.endiar.purrodoro.model.HomeScene
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerSettings
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins the restore semantics of [AppRepository.applyRestore]: current-state
 * collections (tasks/categories/habits) **replace** (rows absent from the
 * bundle become tombstones), while history collections (records/stamps/habit
 * logs) **union** (existing rows survive). See the data-backup-and-restore spec.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppRepositoryRestoreTest {

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
            tmpFolder.newFile("restore_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    private fun bundle(
        displayName: String = "Imported",
        settings: TimerSettings = TimerSettings(),
        categories: List<Category> = emptyList(),
        tasks: List<FocusTask> = emptyList(),
        habits: List<Habit> = emptyList(),
        records: List<SessionRecord> = emptyList(),
        stamps: Map<LocalDate, Int> = emptyMap(),
    ) = RestoreBundle(
        displayName = displayName,
        timerSettings = settings,
        prefs = RestorePrefs(
            appearance = AppearanceMode.Dark,
            language = AppLanguage.Indonesian,
            glass = GlassReadabilityMode.Clear,
            homeScene = HomeScene.Room,
            selectedSoundId = 2,
            notificationSound = false,
        ),
        categories = categories,
        tasks = tasks,
        habits = habits,
        records = records,
        stamps = stamps,
    )

    @Test
    fun restore_replacesTasksAndCategories() = runTest {
        val repo = makeRepo()
        repo.setTasks(listOf(FocusTask(id = 1, title = "Old task", minutes = 25)))
        repo.addCategory("Old category")

        repo.applyRestore(
            bundle(
                categories = listOf(Category(id = 5, name = "New category")),
                tasks = listOf(FocusTask(id = 7, title = "New task", minutes = 30)),
            )
        )

        val state = repo.appState.first()
        assertEquals(listOf("New task"), state.tasks.map { it.title })
        assertEquals(listOf("New category"), state.categories.map { it.name })
    }

    @Test
    fun restore_unionsRecordsAndStamps() = runTest {
        val repo = makeRepo()
        repo.appendSessionRecord(
            SessionRecord(
                id = 1L, startedAt = 0, endedAt = 1, focusMinutes = 25,
                shortBreakMinutes = 5, longBreakMinutes = 15, completedFocusCount = 1,
                totalFocusCount = 4, status = SessionStatus.Completed, taskName = "Existing",
            )
        )
        repo.setStamps(mapOf(LocalDate.of(2026, 6, 1) to 2))

        repo.applyRestore(
            bundle(
                records = listOf(
                    SessionRecord(
                        id = 2L, startedAt = 10, endedAt = 20, focusMinutes = 25,
                        shortBreakMinutes = 5, longBreakMinutes = 15, completedFocusCount = 4,
                        totalFocusCount = 4, status = SessionStatus.Completed, taskName = "Imported",
                    )
                ),
                // same date as existing with a smaller count + a new date
                stamps = mapOf(LocalDate.of(2026, 6, 1) to 1, LocalDate.of(2026, 6, 2) to 3),
            )
        )

        val state = repo.appState.first()
        // history preserved AND merged
        assertEquals(setOf(1L, 2L), state.sessionRecords.map { it.id }.toSet())
        // existing date keeps the larger count (max union), new date added
        assertEquals(2, state.stamps[LocalDate.of(2026, 6, 1)])
        assertEquals(3, state.stamps[LocalDate.of(2026, 6, 2)])
    }

    @Test
    fun restore_replacesHabitsButUnionsTheirLogs() = runTest {
        val repo = makeRepo()
        // Habit id 1 exists with a log on Jun 1; habit id 2 will be dropped.
        repo.addHabit(name = "Keep", emoji = "📖", colorArgb = 0xFF112233)
        repo.addHabit(name = "Drop", emoji = "🏃", colorArgb = 0xFF445566)
        repo.toggleHabitBinaryCompletion(1, LocalDate.of(2026, 6, 1))

        // Bundle keeps habit 1 (with a different-day log) and omits habit 2.
        repo.applyRestore(
            bundle(
                habits = listOf(
                    Habit(
                        id = 1, name = "Keep", emoji = "📖", colorArgb = 0xFF112233,
                        createdAt = 0L,
                        logs = mapOf(LocalDate.of(2026, 6, 2) to HabitLog(HabitLogState.Completed)),
                    )
                ),
            )
        )

        val state = repo.appState.first()
        assertEquals(listOf("Keep"), state.habits.map { it.name }) // habit 2 dropped
        val keep = state.habits.single()
        // union of logs: the pre-existing Jun 1 log survives alongside the imported Jun 2.
        assertEquals(
            setOf(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2)),
            keep.logs.keys,
        )
    }

    @Test
    fun restore_appliesPrefsProfileAndHealsCounters() = runTest {
        val repo = makeRepo()
        repo.applyRestore(
            bundle(
                displayName = "Nandra",
                settings = TimerSettings(40, 8, 25, autoStartNextSession = true, focusSessionsPerCycle = 2),
                categories = listOf(Category(id = 10, name = "C")),
                tasks = listOf(FocusTask(id = 20, title = "T", minutes = 25)),
                habits = listOf(Habit(id = 30, name = "H", emoji = "x", colorArgb = 1L, createdAt = 0L)),
            )
        )

        val state = repo.appState.first()
        assertEquals("Nandra", state.userName)
        assertEquals(40, state.settings.focusMinutes)
        assertEquals(2, state.settings.focusSessionsPerCycle)
        assertEquals(AppearanceMode.Dark, state.appearanceMode)
        assertEquals(AppLanguage.Indonesian, state.language)
        // counters heal above the imported max ids
        assertEquals(21, state.nextTaskId)
        assertEquals(11, state.nextCategoryId)
        assertEquals(31, state.nextHabitId)
    }

    @Test
    fun restore_clearsInFlightRuntime() = runTest {
        val repo = makeRepo()
        repo.setPendingCycleCompletionAck(true)

        repo.applyRestore(bundle())

        assertNull(repo.runtimeSnapshot.first())
        assertTrue(!repo.pendingCycleCompletionAckFlow.first())
    }

    @Test
    fun restore_doesNotTouchOwnerId() = runTest {
        val repo = makeRepo()
        repo.setOwnerId("user-A")

        repo.applyRestore(bundle(tasks = listOf(FocusTask(id = 1, title = "T", minutes = 25))))

        assertEquals("user-A", repo.currentOwnerId())
    }
}
