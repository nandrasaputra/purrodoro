package com.endiar.purrodoro.model

enum class AuthProvider(val storageTag: String) {
    Google("google");

    companion object {
        fun fromStorageTag(tag: String?): AuthProvider = when (tag) {
            Google.storageTag -> Google
            else -> Google
        }
    }
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
