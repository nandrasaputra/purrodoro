package com.endiar.purrodoro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.model.TimerSettings
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

/**
 * Pins the local profile row bookkeeping: user-facing name/settings writes
 * mark the singleton row dirty for the next push, while the sync-pull apply
 * path writes prefs without re-dirtying it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryTest {

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
            tmpFolder.newFile("profile_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    @Test
    fun `setUserName writes the pref and marks the profile row dirty`() = runTest {
        val repo = makeRepo()

        repo.setUserName("Nandra")

        val row = db.profileDao().get()!!
        assertTrue("profile dirty after a name change", row.dirty)
        assertEquals("Nandra", row.displayName)
        assertEquals("UI still reads the pref", "Nandra", repo.appState.first().userName)
    }

    @Test
    fun `setSettings marks the profile row dirty with the encoded settings`() = runTest {
        val repo = makeRepo()
        val settings = TimerSettings(focusMinutes = 50, autoStartNextSession = true)

        repo.setSettings(settings)

        val row = db.profileDao().get()!!
        assertTrue(row.dirty)
        assertEquals(settings, decodeTimerSettings(row.timerSettingsJson))
        assertEquals("UI still reads the prefs", settings, repo.appState.first().settings)
    }

    @Test
    fun `ensureProfileRowDirty creates the missing row from current prefs`() = runTest {
        val repo = makeRepo()
        repo.setUserName("Nandra")
        repo.setSettings(TimerSettings(focusMinutes = 50))
        // Simulate pre-profile data (e.g. a database upgraded from v1): prefs
        // exist but the row does not.
        db.profileDao().clearAll()

        repo.ensureProfileRowDirty(now = 7L)

        val row = db.profileDao().get()!!
        assertTrue(row.dirty)
        assertEquals(7L, row.locallyModifiedAt)
        assertEquals("Nandra", row.displayName)
        assertEquals(50, decodeTimerSettings(row.timerSettingsJson).focusMinutes)
    }

    @Test
    fun `applySyncedProfile writes prefs and stores the row clean`() = runTest {
        val repo = makeRepo()

        repo.applySyncedProfile(
            displayName = "Cloud Name",
            timerSettingsJson = encodeTimerSettings(TimerSettings(focusMinutes = 30)),
        )

        val row = db.profileDao().get()!!
        assertFalse("pull apply must not re-dirty the row", row.dirty)
        assertEquals("Cloud Name", row.displayName)
        val state = repo.appState.first()
        assertEquals("Cloud Name", state.userName)
        assertEquals(30, state.settings.focusMinutes)
    }
}
