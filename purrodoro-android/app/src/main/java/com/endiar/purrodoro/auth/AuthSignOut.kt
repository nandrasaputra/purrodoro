package com.endiar.purrodoro.auth

/**
 * Best-effort sign-out: try remote revoke, fall back to clearing the local session.
 *
 * @return `true` when no signed-in user remains afterward.
 */
internal suspend fun performSignOut(
    signOutRemote: suspend () -> Unit,
    clearLocalSession: suspend () -> Unit,
    isSignedIn: () -> Boolean,
): Boolean {
    runCatching { signOutRemote() }
        .onFailure { runCatching { clearLocalSession() } }
    return !isSignedIn()
}
