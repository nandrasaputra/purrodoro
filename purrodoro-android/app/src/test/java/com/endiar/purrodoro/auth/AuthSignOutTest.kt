package com.endiar.purrodoro.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSignOutTest {

    @Test
    fun performSignOut_succeedsWhenRemoteSignOutClearsSession() = runTest {
        var signedIn = true
        val result = performSignOut(
            signOutRemote = { signedIn = false },
            clearLocalSession = { error("should not run") },
            isSignedIn = { signedIn },
        )
        assertTrue(result)
    }

    @Test
    fun performSignOut_fallsBackToClearLocalWhenRemoteFails() = runTest {
        var signedIn = true
        val result = performSignOut(
            signOutRemote = { error("network down") },
            clearLocalSession = { signedIn = false },
            isSignedIn = { signedIn },
        )
        assertTrue(result)
    }

    @Test
    fun performSignOut_returnsFalseWhenSessionPersists() = runTest {
        val result = performSignOut(
            signOutRemote = { error("network down") },
            clearLocalSession = { error("clear failed") },
            isSignedIn = { true },
        )
        assertFalse(result)
    }
}
