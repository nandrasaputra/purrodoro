package com.endiar.purrodoro.auth

import android.content.Context
import com.endiar.purrodoro.model.AuthProvider
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.model.AuthUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Real auth backend, backed by Supabase Auth.
 *
 * Sign-in is driven from Compose via [composeAuth] (native Google through
 * Credential Manager). This repository owns the [authState] projection and
 * sign-out; the actual id-token exchange is performed by the ComposeAuth
 * plugin, after which the Supabase session — and therefore [authState] — flips
 * to signed-in. The session itself is persisted by an
 * [EncryptedSessionManager] configured in [SupabaseModule].
 */
class AuthRepository(private val client: SupabaseClient) {

    companion object {
        @Volatile private var INSTANCE: AuthRepository? = null

        fun get(context: Context): AuthRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(SupabaseModule.get(context)).also { INSTANCE = it }
            }
    }

    /** Exposed so [com.endiar.purrodoro.ui.auth.LoginSheet] can start the native flow. */
    val composeAuth: ComposeAuth get() = client.composeAuth

    val authState: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated ->
                (status.session.user ?: client.auth.currentUserOrNull())
                    ?.let { AuthState.SignedIn(it.toAuthUser()) }
                    ?: AuthState.Guest
            else -> AuthState.Guest
        }
    }

    /** The signed-in user right now, or null when guest. Used post-sign-in. */
    fun currentAuthUser(): AuthUser? = client.auth.currentUserOrNull()?.toAuthUser()

    suspend fun signOut() {
        // Clears the Supabase session only; local app data is retained per the
        // ownership rules (see AccountManager).
        client.auth.signOut()
    }
}

/** Maps a Supabase [UserInfo] to the app's provider-agnostic [AuthUser]. */
internal fun UserInfo.toAuthUser(): AuthUser {
    val meta = userMetadata
    fun metaString(key: String): String? =
        (meta?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    val displayName = metaString("full_name") ?: metaString("name")
    val providerTag = appMetadata?.get("provider")?.jsonPrimitive?.contentOrNull

    return AuthUser(
        userId = id,
        email = email?.takeIf { it.isNotBlank() },
        displayName = displayName,
        provider = AuthProvider.fromStorageTag(providerTag),
    )
}
