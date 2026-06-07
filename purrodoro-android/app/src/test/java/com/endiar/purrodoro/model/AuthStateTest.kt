package com.endiar.purrodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthStateTest {

    @Test
    fun `resolvedDisplayName prefers account name`() {
        val auth = AuthState.SignedIn(
            AuthUser(
                userId = "1",
                email = null,
                displayName = "Nandra",
                provider = AuthProvider.Google,
            )
        )
        assertEquals("Nandra", resolvedDisplayName(auth, localName = "Local"))
    }

    @Test
    fun `resolvedDisplayName falls back to local name`() {
        val auth = AuthState.SignedIn(
            AuthUser(
                userId = "1",
                email = null,
                displayName = null,
                provider = AuthProvider.Google,
            )
        )
        assertEquals("Local", resolvedDisplayName(auth, localName = "Local"))
    }

    @Test
    fun `resolvedDisplayName uses local name for guest`() {
        assertEquals("Local", resolvedDisplayName(AuthState.Guest, localName = "Local"))
    }

    @Test
    fun `isNameFromAccount true when names match`() {
        val auth = AuthState.SignedIn(
            AuthUser("1", null, "Nandra", AuthProvider.Google)
        )
        assertTrue(isNameFromAccount(auth, localName = "Nandra"))
    }

    @Test
    fun `isNameFromAccount false for guest`() {
        assertFalse(isNameFromAccount(AuthState.Guest, localName = "Nandra"))
    }
}
