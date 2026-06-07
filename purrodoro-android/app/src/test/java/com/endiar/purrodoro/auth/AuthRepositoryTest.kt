package com.endiar.purrodoro.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.endiar.purrodoro.model.AuthProvider
import com.endiar.purrodoro.model.AuthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): AuthRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("auth_${System.nanoTime()}.preferences_pb")
        }
        return AuthRepository(ds)
    }

    @Test
    fun `default auth state is guest`() = runTest {
        val repo = makeRepo()
        assertEquals(AuthState.Guest, repo.authState.first())
    }

    @Test
    fun `signInWithGoogle persists signed-in session`() = runTest {
        val repo = makeRepo()
        val result = repo.signInWithGoogle(fallbackDisplayName = "Nandra")
        assertTrue(result.isSuccess)
        val state = repo.authState.first()
        assertTrue(state is AuthState.SignedIn)
        val user = (state as AuthState.SignedIn).user
        assertEquals("Nandra", user.displayName)
        assertEquals("nandra@purrodoro.app", user.email)
        assertEquals(AuthProvider.Google, user.provider)
    }

    @Test
    fun `signOut clears session`() = runTest {
        val repo = makeRepo()
        repo.signInWithGoogle(fallbackDisplayName = "Nandra")
        repo.signOut()
        assertEquals(AuthState.Guest, repo.authState.first())
    }

    @Test
    fun `decodeSession round-trips encoded payload`() {
        val repo = makeRepo()
        val encoded = repo.encodeSession(
            com.endiar.purrodoro.model.AuthUser(
                userId = "user-1",
                email = "nandra@example.com",
                displayName = "Nandra",
                provider = AuthProvider.Google,
            )
        )
        val decoded = repo.decodeSession(encoded)
        assertEquals("user-1", decoded?.userId)
        assertEquals("nandra@example.com", decoded?.email)
        assertEquals("Nandra", decoded?.displayName)
    }

    @Test
    fun `decodeSession returns null for blank payload`() {
        val repo = makeRepo()
        assertNull(repo.decodeSession(""))
        assertNull(repo.decodeSession(null))
    }
}
