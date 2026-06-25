package com.endiar.purrodoro.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/**
 * Keystore-backed [SessionManager] for the Supabase auth session.
 *
 * The Supabase session carries a long-lived refresh token (a bearer
 * credential), so it must not sit in plain DataStore / SharedPreferences. We
 * persist it in [EncryptedSharedPreferences], whose encryption key is sealed in
 * the Android Keystore. See the auth-and-account-identity spec ("Security —
 * session storage").
 *
 * If encrypted prefs cannot be read (e.g. after an Android backup restore onto
 * a new device where the Keystore key was not restored), stored session data is
 * cleared and the user is treated as signed-out rather than crashing on launch.
 */
class EncryptedSessionManager(context: Context) : SessionManager {

    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    private var prefsCache: SharedPreferences? = null

    override suspend fun saveSession(session: UserSession) {
        runCatching {
            sharedPrefs().edit()
                .putString(KEY_SESSION, json.encodeToString(UserSession.serializer(), session))
                .apply()
        }.onFailure {
            resetSessionStorage()
            throw it
        }
    }

    override suspend fun loadSession(): UserSession? {
        val raw = runCatching { sharedPrefs().getString(KEY_SESSION, null) }
            .getOrElse {
                resetSessionStorage()
                return null
            }
            ?: return null
        return runCatching { json.decodeFromString(UserSession.serializer(), raw) }
            .getOrElse {
                deleteSession()
                null
            }
    }

    override suspend fun deleteSession() {
        runCatching { sharedPrefs().edit().remove(KEY_SESSION).apply() }
            .onFailure { resetSessionStorage() }
    }

    private fun sharedPrefs(): SharedPreferences {
        prefsCache?.let { return it }
        return synchronized(this) {
            prefsCache ?: openEncryptedPrefs().also { prefsCache = it }
        }
    }

    private fun openEncryptedPrefs(): SharedPreferences {
        return runCatching { createEncryptedPrefs() }.getOrElse { firstFailure ->
            resetSessionStorage()
            runCatching { createEncryptedPrefs() }.getOrElse {
                AuthDiagnostics.log("auth_session_storage_unrecoverable", firstFailure.message)
                throw firstFailure
            }
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Drops unreadable session files after backup restore or Keystore mismatch. */
    private fun resetSessionStorage() {
        prefsCache = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { appContext.deleteSharedPreferences(PREFS_NAME) }
        }
        AuthDiagnostics.log("auth_session_storage_reset")
    }

    internal companion object {
        const val PREFS_NAME = "purrodoro_auth_session"
        const val KEY_SESSION = "supabase_session"
    }
}
