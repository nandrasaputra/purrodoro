package com.endiar.purrodoro.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest outcome of a sync pass, for profile UI and debug tooling. */
data class SyncSnapshot(
    val lastAttemptAtMillis: Long? = null,
    val eligible: Boolean = false,
    val result: SyncEngine.SyncResult? = null,
)

enum class SyncUiStatus {
    /** Signed in and eligible, but no pass has run yet. */
    NotSyncedYet,
    /** Last pass was ineligible (guest or owner mismatch). */
    Skipped,
    /** Last eligible pass finished cleanly. */
    UpToDate,
    /** Some tables failed; WorkManager will retry. */
    Incomplete,
    /** Eligible pass failed (network/auth) without a structured result. */
    Failed,
}

data class SyncStatusPresentation(
    val status: SyncUiStatus,
    val lastAttemptAtMillis: Long?,
)

object SyncStatusStore {
    private val _snapshot = MutableStateFlow(SyncSnapshot())
    val snapshot: StateFlow<SyncSnapshot> = _snapshot.asStateFlow()

    fun record(eligible: Boolean, result: SyncEngine.SyncResult?) {
        _snapshot.value = SyncSnapshot(
            lastAttemptAtMillis = System.currentTimeMillis(),
            eligible = eligible,
            result = result,
        )
    }
}

fun resolveSyncStatusPresentation(snapshot: SyncSnapshot): SyncStatusPresentation {
    val status = when {
        snapshot.lastAttemptAtMillis == null ->
            SyncUiStatus.NotSyncedYet
        !snapshot.eligible ->
            SyncUiStatus.Skipped
        snapshot.result is SyncEngine.SyncResult.Success ->
            SyncUiStatus.UpToDate
        snapshot.result is SyncEngine.SyncResult.PartialFailure ->
            SyncUiStatus.Incomplete
        else ->
            SyncUiStatus.Failed
    }
    return SyncStatusPresentation(
        status = status,
        lastAttemptAtMillis = snapshot.lastAttemptAtMillis,
    )
}

/** Shared pass-outcome label for profile UI and debug tooling. */
fun resolveSyncPassDetail(eligible: Boolean, result: SyncEngine.SyncResult?): String = when {
    !eligible -> "Skipped — not eligible (guest or owner mismatch)"
    result is SyncEngine.SyncResult.Success -> "Success"
    result is SyncEngine.SyncResult.PartialFailure ->
        "Partial failure: ${result.failedTables.joinToString()}"
    result == null -> "Failed or signed out"
    else -> result.toString()
}
