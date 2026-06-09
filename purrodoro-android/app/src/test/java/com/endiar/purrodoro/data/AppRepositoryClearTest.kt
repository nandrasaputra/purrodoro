package com.endiar.purrodoro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.InterruptedFocus
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

/**
 * Pins the keep/clear contract of [AppRepository.clearAccountOwnedData]. If a
 * future key is added it should be classified here on purpose, so an account
 * switch never silently leaks (cleared too little) or destroys device prefs
 * (cleared too much).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryClearTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("clear_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds)
    }

    private suspend fun seedEverything(repo: AppRepository) {
        // account-owned
        repo.setTasks(listOf(FocusTask(id = 1, title = "Task", minutes = 25)))
        repo.setNextTaskId(9)
        repo.addCategory("Work")
        repo.setSelectedCategoryId(1)
        repo.addHabit(name = "Read", emoji = "📖", colorArgb = 0xFF112233)
        repo.appendSessionRecord(
            SessionRecord(
                id = 1L, startedAt = 0L, endedAt = 1L,
                focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15,
                completedFocusCount = 1, totalFocusCount = 4,
                status = SessionStatus.Completed, taskName = null,
            )
        )
        repo.setStamps(mapOf(LocalDate.of(2026, 6, 9) to 2))
        repo.setCompletedSessions(7)
        repo.setInterruptedFocus(
            InterruptedFocus(FocusType.Task, taskId = 1, habitId = null, title = "x", remainingSeconds = 100)
        )
        repo.setRuntimeSnapshot(
            TimerRuntimeState(
                session = TimerSession(
                    items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
                    currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Idle,
                ),
                endsAtMillis = null, activeTask = null, openRecord = null,
            )
        )
        repo.setPendingCycleCompletionAck(true)
        repo.setUserName("Nandra")
        repo.setSettings(TimerSettings(focusMinutes = 50, shortBreakMinutes = 10, longBreakMinutes = 20))

        // device-local (must survive)
        repo.setAppearanceMode(AppearanceMode.Dark)
        repo.setLanguage(AppLanguage.Indonesian)
        repo.setGlassReadabilityMode(GlassReadabilityMode.Readable)
        repo.setSelectedSoundId(3)
        repo.setNotificationSoundEnabled(false)
        repo.setIsPremium(true)
        repo.setHasSeenOnboarding(true)
        repo.setHasSeenTutorial(true)
        repo.setOwnerId("user-A")
    }

    @Test
    fun `clear removes all account-owned data`() = runTest {
        val repo = makeRepo()
        seedEverything(repo)

        repo.clearAccountOwnedData()

        val state = repo.appState.first()
        assertTrue("tasks cleared", state.tasks.isEmpty())
        assertEquals("nextTaskId reset", 1, state.nextTaskId)
        assertTrue("categories cleared", state.categories.isEmpty())
        assertEquals("nextCategoryId reset", 1, state.nextCategoryId)
        assertNull("selectedCategoryId cleared", state.selectedCategoryId)
        assertTrue("habits cleared", state.habits.isEmpty())
        assertEquals("nextHabitId reset", 1, state.nextHabitId)
        assertTrue("session records cleared", state.sessionRecords.isEmpty())
        assertTrue("stamps cleared", state.stamps.isEmpty())
        assertEquals("completedSessions reset", 0, state.completedSessions)
        assertNull("interruptedFocus cleared", state.interruptedFocus)
        assertEquals("userName cleared", "", state.userName)
        assertEquals("settings reset to defaults", TimerSettings(), state.settings)
        assertNull("runtime snapshot cleared", repo.runtimeSnapshot.first())
        assertEquals("pending ack cleared", false, repo.pendingCycleCompletionAckFlow.first())
    }

    @Test
    fun `clear keeps device-local preferences`() = runTest {
        val repo = makeRepo()
        seedEverything(repo)

        repo.clearAccountOwnedData()

        val state = repo.appState.first()
        assertEquals(AppearanceMode.Dark, state.appearanceMode)
        assertEquals(AppLanguage.Indonesian, state.language)
        assertEquals(GlassReadabilityMode.Readable, state.glassReadabilityMode)
        assertEquals(3, state.selectedSoundId)
        assertEquals(false, state.notificationSoundEnabled)
        assertEquals(true, state.isPremium)
        assertEquals(true, repo.hasSeenOnboardingFlow.first())
        assertEquals(true, repo.hasSeenTutorialFlow.first())
    }

    @Test
    fun `clear does not touch the owner marker`() = runTest {
        val repo = makeRepo()
        seedEverything(repo)

        repo.clearAccountOwnedData()

        // The caller (AccountManager) updates ownerId separately, after the
        // clear commits — clear itself must leave it intact.
        assertEquals("user-A", repo.currentOwnerId())
    }
}
