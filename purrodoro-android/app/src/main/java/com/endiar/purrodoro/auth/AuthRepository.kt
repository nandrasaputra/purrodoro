package com.endiar.purrodoro.auth

import android.content.Context
import com.endiar.purrodoro.model.AuthProvider
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.model.AuthUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

    /**
     * Application-lived scope for sign-in resolution. Resolving the signed-in
     * user (see [resolveSignedInUser]) must not be tied to the LoginSheet's
     * composition: the sheet can be dismissed — cancelling its
     * `rememberCoroutineScope` — while [awaitSignedInUser] is still polling,
     * which would otherwise skip the post-sign-in handoff to AccountManager.
     */
    private val resolverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Reads the current session synchronously for first-frame UI (e.g. home greeting). */
    fun snapshotAuthState(): AuthState =
        currentAuthUser()?.let { AuthState.SignedIn(it) } ?: AuthState.Guest

    /** Blocks until Supabase finishes reading the persisted session (cold start). */
    suspend fun awaitInitialization(timeoutMs: Long = 2_000) {
        withTimeoutOrNull(timeoutMs) {
            client.auth.awaitInitialization()
        }
    }

    val authState: Flow<AuthState> = client.auth.sessionStatus.map(::resolveAuthState)

    private fun resolveAuthState(status: SessionStatus): AuthState = when (status) {
        is SessionStatus.Authenticated ->
            (status.session.user ?: client.auth.currentUserOrNull())
                ?.let { AuthState.SignedIn(it.toAuthUser()) }
                ?: snapshotAuthState()
        is SessionStatus.NotAuthenticated -> AuthState.Guest
        SessionStatus.Initializing -> hydratingAuthState()
        is SessionStatus.RefreshFailure -> hydratingAuthState()
    }

    /** Session is still restoring; avoid treating the user as signed-out guest. */
    private fun hydratingAuthState(): AuthState =
        when (val snap = snapshotAuthState()) {
            is AuthState.SignedIn -> snap
            else -> AuthState.Loading
        }

    /** The signed-in user right now, or null when guest. Used post-sign-in. */
    fun currentAuthUser(): AuthUser? = client.auth.currentUserOrNull()?.toAuthUser()

    /**
     * Resolves the signed-in user after native Google sign-in. The ComposeAuth
     * success callback can fire a tick before the Supabase session is readable
     * via [currentAuthUser], so we poll briefly then fall back to [authState].
     */
    suspend fun awaitSignedInUser(): AuthUser? {
        repeat(20) {
            currentAuthUser()?.let { return it }
            delay(100)
        }
        return withTimeoutOrNull(3_000L) {
            authState.filterIsInstance<AuthState.SignedIn>().map { it.user }.first()
        }
    }

    /**
     * Resolves the signed-in user on an application-lived scope and delivers the
     * result on the main thread. Survives the LoginSheet leaving composition so
     * the post-sign-in handoff (AccountManager) always runs once the native
     * Google flow reports success.
     */
    fun resolveSignedInUser(onResolved: (AuthUser?) -> Unit) {
        resolverScope.launch {
            onResolved(awaitSignedInUser())
        }
    }

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
