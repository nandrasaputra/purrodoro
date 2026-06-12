package com.endiar.purrodoro.sync

/**
 * What app start should do about the ownership marker. The interesting cases
 * are crash remnants of the sign-in flow's Claim/Drop windows (see
 * [com.endiar.purrodoro.auth.AccountManager]): without this reconciliation, a
 * crash between "clear data" and "re-tag owner" would leave a signed-in user
 * whose data never syncs — silently, forever.
 */
enum class StartupAction {
    /** Signed out, or the owner already matches the user. */
    None,

    /** Unclaimed data under a signed-in user: re-run the Claim (tag + merge). */
    Claim,

    /**
     * Foreign owner but ALL local data is gone — the destructive half of a
     * Drop already happened, so finish it: re-tag and pull the user's cloud.
     * No confirmation needed; there is nothing left to lose.
     */
    CompleteDrop,

    /**
     * Foreign owner with data still present. This state should be unreachable
     * (the sign-in flow resolves it), so be conservative: never touch the
     * data, sign out, and let the next interactive sign-in re-evaluate.
     */
    SignOut,
}

/** Pure decision behind the startup ownership reconciliation. */
fun resolveStartupOwnership(
    userId: String?,
    ownerId: String?,
    hasLocalData: Boolean,
): StartupAction = when {
    userId == null -> StartupAction.None
    userId == ownerId -> StartupAction.None
    ownerId == null -> StartupAction.Claim
    !hasLocalData -> StartupAction.CompleteDrop
    else -> StartupAction.SignOut
}

/**
 * Orchestrates [resolveStartupOwnership] over injected side-effects, so the
 * sequencing is unit-testable without the real [SyncModule]. Production
 * wiring is [SyncModule.reconcileOwnershipOnStartup].
 */
suspend fun reconcileStartupOwnership(
    userId: String?,
    ownerId: String?,
    hasLocalData: Boolean,
    setOwnerId: suspend (String) -> Unit,
    claimMerge: suspend () -> Unit,
    restoreFromCloud: suspend () -> Unit,
    signOut: suspend () -> Unit,
) {
    when (resolveStartupOwnership(userId, ownerId, hasLocalData)) {
        StartupAction.None -> Unit
        StartupAction.Claim -> {
            setOwnerId(checkNotNull(userId))
            claimMerge()
        }
        StartupAction.CompleteDrop -> {
            setOwnerId(checkNotNull(userId))
            restoreFromCloud()
        }
        StartupAction.SignOut -> signOut()
    }
}
