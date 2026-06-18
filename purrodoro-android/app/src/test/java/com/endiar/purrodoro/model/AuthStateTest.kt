package com.endiar.purrodoro.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthStateTest {

    private val signedIn = AuthState.SignedIn(
        AuthUser(
            userId = "u1",
            email = "a@b.com",
            displayName = "Google Name",
            provider = AuthProvider.Google,
        )
    )

    @Test
    fun signedIn_prefersAccountDisplayNameOverLocalNickname() {
        val name = resolvedDisplayName(signedIn, localName = "Local Nick")
        assertEquals("Google Name", name)
    }

    @Test
    fun guest_showsLocalNickname() {
        val name = resolvedDisplayName(AuthState.Guest, localName = "Local Nick")
        assertEquals("Local Nick", name)
    }

    @Test
    fun guest_showsLocalNicknameEvenWhenDeviceHasClaimedData() {
        // ownerId persists after sign-out; guest must still show the local profile name.
        val name = resolvedDisplayName(AuthState.Guest, localName = "x")
        assertEquals("x", name)
    }

    @Test
    fun loading_doesNotFlashLocalNickname() {
        assertEquals("", resolvedDisplayName(AuthState.Loading, localName = "Local Nick"))
    }
}
