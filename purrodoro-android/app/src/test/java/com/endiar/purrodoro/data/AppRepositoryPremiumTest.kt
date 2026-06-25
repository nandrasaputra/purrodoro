package com.endiar.purrodoro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppRepositoryPremiumTest {

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
            tmpFolder.newFile("premium_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    @Test
    fun `override enabled grants premium without play`() = runTest {
        val repo = makeRepo()
        repo.setPlayPremium(false)
        repo.applySyncedProfilePrefs(
            displayName = "Reviewer",
            timerSettingsJson = "{}",
            premiumOverrideEnabled = true,
            premiumOverrideActive = true,
        )
        assertTrue(repo.appState.first().isPremium)
    }

    @Test
    fun `override enabled can force free despite play`() = runTest {
        val repo = makeRepo()
        repo.setPlayPremium(true)
        repo.applySyncedProfilePrefs(
            displayName = "Tester",
            timerSettingsJson = "{}",
            premiumOverrideEnabled = true,
            premiumOverrideActive = false,
        )
        assertFalse(repo.appState.first().isPremium)
    }

    @Test
    fun `override disabled uses play billing`() = runTest {
        val repo = makeRepo()
        repo.applySyncedProfilePrefs(
            displayName = "User",
            timerSettingsJson = "{}",
            premiumOverrideEnabled = false,
            premiumOverrideActive = false,
        )
        repo.setPlayPremium(true)
        assertTrue(repo.appState.first().isPremium)
    }

    @Test
    fun `clearPremiumOverride falls back to play`() = runTest {
        val repo = makeRepo()
        repo.setPlayPremium(true)
        repo.applySyncedProfilePrefs(
            displayName = "User",
            timerSettingsJson = "{}",
            premiumOverrideEnabled = true,
            premiumOverrideActive = false,
        )
        assertFalse(repo.appState.first().isPremium)
        repo.clearPremiumOverride()
        assertTrue(repo.appState.first().isPremium)
    }
}
