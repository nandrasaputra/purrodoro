package com.endiar.purrodoro.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.endiar.purrodoro.model.AuthProvider
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.model.AuthUser
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the signed-in account locally. Google Sign-In is stubbed until a
 * Web client ID is configured — [signInWithGoogle] still produces a durable
 * session so the login UI can be exercised end-to-end.
 */
class AuthRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_AUTH_SESSION = stringPreferencesKey("auth_session")

        private val Context.authDataStore by preferencesDataStore(name = "purrodoro_auth")

        @Volatile private var INSTANCE: AuthRepository? = null

        fun get(context: Context): AuthRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context.applicationContext.authDataStore).also {
                    INSTANCE = it
                }
            }
    }

    val authState: Flow<AuthState> = dataStore.data.map { prefs ->
        decodeSession(prefs[KEY_AUTH_SESSION])?.let { AuthState.SignedIn(it) } ?: AuthState.Guest
    }

    suspend fun signInWithGoogle(fallbackDisplayName: String = ""): Result<AuthUser> {
        val trimmedName = fallbackDisplayName.trim()
        val email = trimmedName.takeIf { it.isNotBlank() }?.let { name ->
            "${name.lowercase().replace(Regex("\\s+"), ".")}@purrodoro.app"
        }
        val user = AuthUser(
            userId = UUID.randomUUID().toString(),
            email = email,
            displayName = trimmedName.takeIf { it.isNotBlank() },
            provider = AuthProvider.Google,
        )
        persistSession(user)
        return Result.success(user)
    }

    suspend fun signOut() {
        dataStore.edit { prefs -> prefs.remove(KEY_AUTH_SESSION) }
    }

    private suspend fun persistSession(user: AuthUser) {
        dataStore.edit { prefs -> prefs[KEY_AUTH_SESSION] = encodeSession(user) }
    }

    internal fun encodeSession(user: AuthUser): String {
        val email = user.email.orEmpty()
        val displayName = user.displayName.orEmpty()
        return listOf(
            user.userId,
            URLEncoder.encode(email, Charsets.UTF_8.name()),
            URLEncoder.encode(displayName, Charsets.UTF_8.name()),
            user.provider.storageTag,
        ).joinToString("|")
    }

    internal fun decodeSession(raw: String?): AuthUser? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split("|")
        if (parts.size < 4) return null
        val userId = parts[0].trim()
        if (userId.isBlank()) return null
        return AuthUser(
            userId = userId,
            email = URLDecoder.decode(parts[1], Charsets.UTF_8.name()).takeIf { it.isNotBlank() },
            displayName = URLDecoder.decode(parts[2], Charsets.UTF_8.name()).takeIf { it.isNotBlank() },
            provider = AuthProvider.fromStorageTag(parts[3]),
        )
    }
}
