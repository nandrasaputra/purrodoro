package com.endiar.kuropomodoro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassReadabilityMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TodoListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("test_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds)
    }

    @Test
    fun `default AppState has expected values`() = runTest {
        val repo = makeRepo()
        val state = repo.appState.first()
        assertEquals(GlassReadabilityMode.Adaptive, state.glassReadabilityMode)
        assertEquals(1, state.selectedSoundId)
        assertEquals(0, state.completedSessions)
        assertEquals(TimerSettings(), state.settings)
        assertEquals(7, state.nextTaskId)
        assertEquals(1, state.selectedTodoListId)
        assertEquals(emptyMap<LocalDate, Int>(), state.stamps)
        assertEquals(3, state.todoLists.size) // seed data
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
    fun `setNextTaskId persists value`() = runTest {
        val repo = makeRepo()
        repo.setNextTaskId(42)
        assertEquals(42, repo.appState.first().nextTaskId)
    }

    @Test
    fun `setSelectedTodoListId persists value`() = runTest {
        val repo = makeRepo()
        repo.setSelectedTodoListId(2)
        assertEquals(2, repo.appState.first().selectedTodoListId)
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
    fun `setTodoLists round-trips lists with tasks`() = runTest {
        val repo = makeRepo()
        val lists = listOf(
            TodoListState(1, "Work", listOf(FocusTask(1, "Task A", 25, false))),
            TodoListState(2, "Home", emptyList())
        )
        repo.setTodoLists(lists)
        val result = repo.appState.first().todoLists
        assertEquals(2, result.size)
        assertEquals("Work", result[0].name)
        assertEquals(1, result[0].tasks.size)
        assertEquals("Task A", result[0].tasks[0].title)
        assertEquals("Home", result[1].name)
        assertEquals(0, result[1].tasks.size)
    }

    @Test
    fun `setTodoLists round-trips completed flag`() = runTest {
        val repo = makeRepo()
        val lists = listOf(
            TodoListState(1, "Study", listOf(FocusTask(1, "Done task", 10, true)))
        )
        repo.setTodoLists(lists)
        val task = repo.appState.first().todoLists[0].tasks[0]
        assertEquals(true, task.completed)
    }

    @Test
    fun `encodeStamps with emptyMap round-trips to emptyMap`() {
        val encoded = encodeStamps(emptyMap())
        assertEquals(emptyMap<java.time.LocalDate, Int>(), decodeStamps(encoded))
    }

    @Test
    fun `task title containing colon survives encode-decode round-trip`() {
        val lists = listOf(
            TodoListState(1, "Work", listOf(FocusTask(1, "Read: Chapter 1", 25, false)))
        )
        val decoded = decodeTodoLists(encodeTodoLists(lists))
        assertEquals("Read: Chapter 1", decoded[0].tasks[0].title)
    }

    @Test
    fun `list name containing pipe survives encode-decode round-trip`() {
        val lists = listOf(
            TodoListState(1, "Work|Home", listOf(FocusTask(1, "Task", 25, false)))
        )
        val decoded = decodeTodoLists(encodeTodoLists(lists))
        assertEquals("Work|Home", decoded[0].name)
    }
}
