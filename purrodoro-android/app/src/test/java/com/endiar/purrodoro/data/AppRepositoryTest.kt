package com.endiar.purrodoro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimerSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PurrodoroDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("test_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    @Test
    fun `default AppState has expected values`() = runTest {
        val repo = makeRepo()
        val state = repo.appState.first()
        assertEquals(GlassReadabilityMode.Adaptive, state.glassReadabilityMode)
        assertEquals(1, state.selectedSoundId)
        assertEquals(0, state.completedSessions)
        assertEquals(TimerSettings(), state.settings)
        assertEquals(1, state.nextTaskId)
        assertEquals(emptyMap<LocalDate, Int>(), state.stamps)
    }

    @Test
    fun `setGlassReadabilityMode persists Readable`() = runTest {
        val repo = makeRepo()
        repo.setGlassReadabilityMode(GlassReadabilityMode.Readable)
        assertEquals(GlassReadabilityMode.Readable, repo.appState.first().glassReadabilityMode)
    }

    @Test
    fun `setSelectedSoundId persists value`() = runTest {
        val repo = makeRepo()
        repo.setSelectedSoundId(3)
        assertEquals(3, repo.appState.first().selectedSoundId)
    }

    @Test
    fun `setSoundscapeMix persists ambience and music layers independently`() = runTest {
        val repo = makeRepo()
        repo.setSoundscapeMix(ambienceId = 2, musicId = 1)
        val state = repo.appState.first()
        assertEquals(2, state.soundscapeMixAmbienceId)
        assertEquals(1, state.soundscapeMixMusicId)

        repo.setSoundscapeMix(ambienceId = null, musicId = 3)
        val clearedAmbience = repo.appState.first()
        assertEquals(null, clearedAmbience.soundscapeMixAmbienceId)
        assertEquals(3, clearedAmbience.soundscapeMixMusicId)
    }

    @Test
    fun `setCompletedSessions persists value`() = runTest {
        val repo = makeRepo()
        repo.setCompletedSessions(5)
        assertEquals(5, repo.appState.first().completedSessions)
    }

    @Test
    fun `setSettings persists custom durations`() = runTest {
        val repo = makeRepo()
        repo.setSettings(TimerSettings(focusMinutes = 50, shortBreakMinutes = 10, longBreakMinutes = 20))
        val s = repo.appState.first().settings
        assertEquals(50, s.focusMinutes)
        assertEquals(10, s.shortBreakMinutes)
        assertEquals(20, s.longBreakMinutes)
    }

    @Test
    fun `setSettings persists auto-start next session`() = runTest {
        val repo = makeRepo()
        repo.setSettings(TimerSettings(autoStartNextSession = true))

        assertEquals(true, repo.appState.first().settings.autoStartNextSession)
    }

    @Test
    fun `setNextTaskId persists value`() = runTest {
        val repo = makeRepo()
        repo.setNextTaskId(42)
        assertEquals(42, repo.appState.first().nextTaskId)
    }

    @Test
    fun `setStamps round-trips multiple dates`() = runTest {
        val repo = makeRepo()
        val stamps = mapOf(
            LocalDate.of(2026, 5, 1) to 3,
            LocalDate.of(2026, 5, 2) to 1
        )
        repo.setStamps(stamps)
        assertEquals(stamps, repo.appState.first().stamps)
    }

    @Test
    fun `encodeStamps with emptyMap round-trips to emptyMap`() {
        val encoded = encodeStamps(emptyMap())
        assertEquals(emptyMap<java.time.LocalDate, Int>(), decodeStamps(encoded))
    }

    @Test
    fun `pendingCycleCompletionAck defaults to false`() = runTest {
        val repo = makeRepo()
        assertEquals(false, repo.pendingCycleCompletionAckFlow.first())
    }

    @Test
    fun `setPendingCycleCompletionAck true then false persists`() = runTest {
        val repo = makeRepo()
        repo.setPendingCycleCompletionAck(true)
        assertEquals(true, repo.pendingCycleCompletionAckFlow.first())
        repo.setPendingCycleCompletionAck(false)
        assertEquals(false, repo.pendingCycleCompletionAckFlow.first())
    }

    @Test
    fun `default appearanceMode is Auto`() = runTest {
        val repo = makeRepo()
        assertEquals(AppearanceMode.Auto, repo.appState.first().appearanceMode)
    }

    @Test
    fun `setAppearanceMode persists Light`() = runTest {
        val repo = makeRepo()
        repo.setAppearanceMode(AppearanceMode.Light)
        assertEquals(AppearanceMode.Light, repo.appState.first().appearanceMode)
    }

    @Test
    fun `setAppearanceMode persists Dark`() = runTest {
        val repo = makeRepo()
        repo.setAppearanceMode(AppearanceMode.Dark)
        assertEquals(AppearanceMode.Dark, repo.appState.first().appearanceMode)
    }

    @Test
    fun `task encoding round-trips priority and due date`() {
        val tasks = listOf(
            FocusTask(
                id = 1,
                title = "Design onboarding",
                minutes = 25,
                priority = TaskPriority.High,
                dueDate = LocalDate.of(2026, 5, 27),
            ),
            FocusTask(
                id = 2,
                title = "Tidy desk",
                minutes = 25,
                priority = TaskPriority.Low,
                dueDate = null,
            ),
            FocusTask(id = 3, title = "Default everything", minutes = 25),
        )

        val decoded = decodeTasks(encodeTasks(tasks))

        assertEquals(tasks, decoded)
    }

    @Test
    fun `task decoder accepts legacy v1 rows without priority or due date`() {
        // v1 format has 7 colon-separated fields (no priority, no due date).
        val v1 = "7:Refine%20task%20screen:25:false:1:0:"
        val decoded = decodeTasks(v1)

        assertEquals(1, decoded.size)
        val task = decoded.first()
        assertEquals(7, task.id)
        assertEquals("Refine task screen", task.title)
        assertEquals(TaskPriority.Normal, task.priority)
        assertEquals(null, task.dueDate)
    }

    // --- ownerId (data ownership marker) ---

    @Test
    fun `ownerId defaults to null`() = runTest {
        val repo = makeRepo()
        assertEquals(null, repo.currentOwnerId())
    }

    @Test
    fun `setOwnerId persists and reads back`() = runTest {
        val repo = makeRepo()
        repo.setOwnerId("user-A")
        assertEquals("user-A", repo.currentOwnerId())
    }

    @Test
    fun `setOwnerId null clears the marker`() = runTest {
        val repo = makeRepo()
        repo.setOwnerId("user-A")
        repo.setOwnerId(null)
        assertEquals(null, repo.currentOwnerId())
    }

    @Test
    fun `ownerId survives a fresh repo on the same datastore`() = runTest {
        val file = tmpFolder.newFile("owner_shared.preferences_pb")
        val ds = PreferenceDataStoreFactory.create { file }
        AppRepository(ds).setOwnerId("user-A")
        // A new repository over the same DataStore file sees the persisted owner.
        assertEquals("user-A", AppRepository(ds).currentOwnerId())
    }
}
