package com.endiar.purrodoro.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.model.FocusTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountManagerTest {

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
            tmpFolder.newFile("am_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    private suspend fun AppRepository.seedTask() =
        setTasks(listOf(FocusTask(id = 1, title = "Keep me", minutes = 25)))

    @Test
    fun `null owner claims - tags account, keeps data`() = runTest {
        val repo = makeRepo()
        repo.seedTask()
        val manager = AccountManager(repo)

        val result = manager.onSignedIn("user-A", confirmDrop = { true })

        assertEquals(AccountManager.Result.Claimed, result)
        assertEquals("user-A", repo.currentOwnerId())
        assertTrue("data untouched on claim", repo.appState.first().tasks.isNotEmpty())
    }

    @Test
    fun `same owner keeps - no change`() = runTest {
        val repo = makeRepo()
        repo.setOwnerId("user-A")
        repo.seedTask()
        val manager = AccountManager(repo)

        var confirmCalled = false
        val result = manager.onSignedIn("user-A", confirmDrop = { confirmCalled = true; true })

        assertEquals(AccountManager.Result.Kept, result)
        assertEquals("user-A", repo.currentOwnerId())
        assertTrue(repo.appState.first().tasks.isNotEmpty())
        assertFalse("confirm not asked on keep", confirmCalled)
    }

    @Test
    fun `different owner with confirm drops - clears then re-tags`() = runTest {
        val repo = makeRepo()
        repo.setOwnerId("user-A")
        repo.seedTask()
        val manager = AccountManager(repo)

        var beforeClearInvoked = false
        val result = manager.onSignedIn(
            incomingUserId = "user-B",
            confirmDrop = { true },
            onBeforeClear = {
                // Ordering proof: owner is still A and data still present here,
                // confirming the clear + re-tag happen *after* this hook.
                beforeClearInvoked = true
                assertEquals("user-A", repo.currentOwnerId())
                assertTrue(repo.appState.first().tasks.isNotEmpty())
            },
        )

        assertEquals(AccountManager.Result.Dropped, result)
        assertTrue(beforeClearInvoked)
        assertTrue("data cleared", repo.appState.first().tasks.isEmpty())
        assertEquals("re-tagged to B", "user-B", repo.currentOwnerId())
    }

    @Test
    fun `different owner with cancel - nothing touched`() = runTest {
        val repo = makeRepo()
        repo.setOwnerId("user-A")
        repo.seedTask()
        val manager = AccountManager(repo)

        var beforeClearInvoked = false
        val result = manager.onSignedIn(
            incomingUserId = "user-B",
            confirmDrop = { false },
            onBeforeClear = { beforeClearInvoked = true },
        )

        assertEquals(AccountManager.Result.DropCancelled, result)
        assertFalse("onBeforeClear must not run on cancel", beforeClearInvoked)
        assertTrue("data intact on cancel", repo.appState.first().tasks.isNotEmpty())
        assertEquals("owner unchanged on cancel", "user-A", repo.currentOwnerId())
    }
}
