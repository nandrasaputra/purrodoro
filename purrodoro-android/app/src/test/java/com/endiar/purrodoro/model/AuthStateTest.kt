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

    // --- AuthProvider.fromStorageTag ---

    @Test
    fun `fromStorageTag round-trips google`() {
        assertEquals(AuthProvider.Google, AuthProvider.fromStorageTag("google"))
    }

    @Test
    fun `fromStorageTag maps unknown tags to Unknown not Google`() {
        assertEquals(AuthProvider.Unknown, AuthProvider.fromStorageTag("apple"))
        assertEquals(AuthProvider.Unknown, AuthProvider.fromStorageTag("email"))
        assertEquals(AuthProvider.Unknown, AuthProvider.fromStorageTag(""))
        assertEquals(AuthProvider.Unknown, AuthProvider.fromStorageTag(null))
    }

    // --- resolveLoginAction (ownership rule) ---

    @Test
    fun `resolveLoginAction null owner claims`() {
        assertEquals(LoginDataAction.Claim, resolveLoginAction(ownerId = null, incomingUserId = "A"))
    }

    @Test
    fun `resolveLoginAction null owner with blank id still claims`() {
        assertEquals(LoginDataAction.Claim, resolveLoginAction(ownerId = null, incomingUserId = ""))
    }

    @Test
    fun `resolveLoginAction same owner keeps`() {
        assertEquals(LoginDataAction.Keep, resolveLoginAction(ownerId = "A", incomingUserId = "A"))
    }

    @Test
    fun `resolveLoginAction different owner drops`() {
        assertEquals(LoginDataAction.Drop, resolveLoginAction(ownerId = "A", incomingUserId = "B"))
    }
}
