package com.endiar.purrodoro.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupOwnershipTest {

    // --- resolveStartupOwnership truth table ---

    @Test
    fun `signed out - nothing to reconcile`() {
        assertEquals(StartupAction.None, resolveStartupOwnership(null, null, hasLocalData = false))
        assertEquals(StartupAction.None, resolveStartupOwnership(null, null, hasLocalData = true))
        assertEquals(StartupAction.None, resolveStartupOwnership(null, "user-A", hasLocalData = true))
    }

    @Test
    fun `owner matches the signed-in user - nothing to reconcile`() {
        assertEquals(StartupAction.None, resolveStartupOwnership("user-A", "user-A", hasLocalData = true))
        assertEquals(StartupAction.None, resolveStartupOwnership("user-A", "user-A", hasLocalData = false))
    }

    @Test
    fun `signed in over unclaimed data - re-run the claim`() {
        assertEquals(StartupAction.Claim, resolveStartupOwnership("user-A", null, hasLocalData = true))
        assertEquals(StartupAction.Claim, resolveStartupOwnership("user-A", null, hasLocalData = false))
    }

    @Test
    fun `foreign owner with empty data - complete the interrupted drop`() {
        assertEquals(
            StartupAction.CompleteDrop,
            resolveStartupOwnership("user-B", "user-A", hasLocalData = false),
        )
    }

    @Test
    fun `foreign owner with data present - sign out, never touch the data`() {
        assertEquals(
            StartupAction.SignOut,
            resolveStartupOwnership("user-B", "user-A", hasLocalData = true),
        )
    }

    // --- orchestration ---

    private class Recorder {
        val calls = mutableListOf<String>()
        var taggedOwner: String? = null

        suspend fun run(userId: String?, ownerId: String?, hasLocalData: Boolean) =
            reconcileStartupOwnership(
                userId = userId,
                ownerId = ownerId,
                hasLocalData = hasLocalData,
                setOwnerId = { taggedOwner = it; calls += "setOwnerId" },
                claimMerge = { calls += "claimMerge" },
                restoreFromCloud = { calls += "restoreFromCloud" },
                signOut = { calls += "signOut" },
            )
    }

    @Test
    fun `claim remnant - tags the owner then runs the claim merge`() = runTest {
        val recorder = Recorder()

        recorder.run(userId = "user-A", ownerId = null, hasLocalData = true)

        assertEquals(listOf("setOwnerId", "claimMerge"), recorder.calls)
        assertEquals("user-A", recorder.taggedOwner)
    }

    @Test
    fun `interrupted drop - re-tags then restores from cloud, no confirmation`() = runTest {
        val recorder = Recorder()

        recorder.run(userId = "user-B", ownerId = "user-A", hasLocalData = false)

        assertEquals(listOf("setOwnerId", "restoreFromCloud"), recorder.calls)
        assertEquals("user-B", recorder.taggedOwner)
    }

    @Test
    fun `impossible state with data - signs out without touching anything`() = runTest {
        val recorder = Recorder()

        recorder.run(userId = "user-B", ownerId = "user-A", hasLocalData = true)

        assertEquals(listOf("signOut"), recorder.calls)
    }

    @Test
    fun `healthy states - no side effects`() = runTest {
        val recorder = Recorder()

        recorder.run(userId = null, ownerId = "user-A", hasLocalData = true)
        recorder.run(userId = "user-A", ownerId = "user-A", hasLocalData = true)

        assertEquals(emptyList<String>(), recorder.calls)
    }
}
