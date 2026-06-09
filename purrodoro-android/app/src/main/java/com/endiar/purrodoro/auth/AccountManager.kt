package com.endiar.purrodoro.auth

import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.LoginDataAction
import com.endiar.purrodoro.model.resolveLoginAction

/**
 * Applies the local-data ownership rule when a user signs in.
 *
 * Pure decision lives in [resolveLoginAction]; this class wires the resulting
 * [LoginDataAction] to the side-effects (tag / clear) and the user-facing
 * confirmation for the destructive [LoginDataAction.Drop] case.
 *
 * Kept free of Supabase / Android UI types so it is unit-testable against an
 * in-memory [AppRepository]. See the auth-and-account-identity spec.
 */
class AccountManager(
    private val repo: AppRepository,
) {

    /** Outcome of an [onSignedIn] call, for the caller to react to (UI/logging). */
    enum class Result {
        /** Unclaimed guest data was tagged for the signing-in account. */
        Claimed,

        /** Data already belonged to this account; nothing changed. */
        Kept,

        /** A different account's data was cleared and re-tagged after confirmation. */
        Dropped,

        /** A switch was needed but the user declined the confirmation. */
        DropCancelled,
    }

    /**
     * Resolve what to do with on-device data for the account [incomingUserId]
     * that just signed in.
     *
     * For the destructive switch case, [confirmDrop] is invoked and must return
     * `true` to proceed with clearing. [onBeforeClear] runs first on confirm so
     * the caller can stop any in-flight timer before the persisted runtime
     * snapshot is wiped.
     *
     * Ordering guarantee: on a confirmed drop we **clear data, then** write the
     * new owner id. A crash between the two leaves the old owner in place, so the
     * next sign-in simply re-evaluates to Drop — never a leak of the old data to
     * the new account.
     */
    suspend fun onSignedIn(
        incomingUserId: String,
        confirmDrop: suspend () -> Boolean,
        onBeforeClear: suspend () -> Unit = {},
    ): Result {
        return when (resolveLoginAction(repo.currentOwnerId(), incomingUserId)) {
            LoginDataAction.Claim -> {
                repo.setOwnerId(incomingUserId)
                Result.Claimed
            }
            LoginDataAction.Keep -> Result.Kept
            LoginDataAction.Drop -> {
                if (confirmDrop()) {
                    onBeforeClear()
                    repo.clearAccountOwnedData()
                    repo.setOwnerId(incomingUserId) // only after the clear commits
                    Result.Dropped
                } else {
                    Result.DropCancelled
                }
            }
        }
    }
}
