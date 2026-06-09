package com.endiar.purrodoro.model

enum class AuthProvider(val storageTag: String) {
    Google("google"),

    /**
     * Fallback for any storage tag we don't recognise (e.g. a provider added in
     * a newer build, then read by an older one, or a corrupted value). Must stay
     * last. We deliberately do NOT coerce unknown tags to [Google] — silently
     * mislabelling an account's provider is worse than showing a neutral one.
     */
    Unknown("unknown");

    companion object {
        fun fromStorageTag(tag: String?): AuthProvider = when (tag) {
            Google.storageTag -> Google
            else -> Unknown
        }
    }
}

/**
 * What to do with the on-device data when a user signs in, decided purely from
 * the local data's owner marker vs. the incoming account id. See the
 * auth-and-account-identity spec.
 */
enum class LoginDataAction {
    /** Unclaimed (guest) data → tag it for the new account, keep everything. */
    Claim,

    /** Already this account's data → no-op. */
    Keep,

    /** Belongs to a different account → clear local, then re-tag (with warning). */
    Drop,
}

/**
 * Pure ownership rule. [ownerId] is the persisted marker of who the on-device
 * data belongs to (`null` = guest/unclaimed); [incomingUserId] is the account
 * that just signed in.
 */
fun resolveLoginAction(ownerId: String?, incomingUserId: String): LoginDataAction = when {
    ownerId == null -> LoginDataAction.Claim
    ownerId == incomingUserId -> LoginDataAction.Keep
    else -> LoginDataAction.Drop
}

data class AuthUser(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val provider: AuthProvider,
)

sealed class AuthState {
    data object Guest : AuthState()
    data class SignedIn(val user: AuthUser) : AuthState()
}

fun resolvedDisplayName(authState: AuthState, localName: String): String = when (authState) {
    is AuthState.SignedIn -> authState.user.displayName?.takeIf { it.isNotBlank() }
        ?: localName.trim().takeIf { it.isNotBlank() }
        ?: ""
    AuthState.Guest -> localName.trim()
}

fun isNameFromAccount(authState: AuthState, localName: String): Boolean {
    val accountName = (authState as? AuthState.SignedIn)?.user?.displayName?.trim().orEmpty()
    return accountName.isNotBlank() && accountName == localName.trim()
}
