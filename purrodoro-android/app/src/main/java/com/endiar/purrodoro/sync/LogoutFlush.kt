package com.endiar.purrodoro.sync

/** What the sign-out flow should do after the pre-logout flush attempt. */
enum class LogoutDecision { SignOut, WarnUnsynced }

/**
 * Pure rule behind [SyncModule.flushBeforeLogout]: warn the user only when the
 * flush failed AND dirty rows remain — that is the one case where signing out
 * leaves changes that exist nowhere but on this device. A failed flush with
 * nothing dirty lost nothing, and a clean flush already uploaded everything.
 */
fun logoutDecision(flushSucceeded: Boolean, dirtyRemaining: Boolean): LogoutDecision =
    if (!flushSucceeded && dirtyRemaining) LogoutDecision.WarnUnsynced else LogoutDecision.SignOut
