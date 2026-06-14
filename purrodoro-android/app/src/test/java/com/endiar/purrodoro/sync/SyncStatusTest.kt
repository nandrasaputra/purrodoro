package com.endiar.purrodoro.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusTest {

    @Test
    fun `not synced yet when no attempt recorded`() {
        val presentation = resolveSyncStatusPresentation(SyncSnapshot())
        assertEquals(SyncUiStatus.NotSyncedYet, presentation.status)
        assertEquals(null, presentation.lastAttemptAtMillis)
    }

    @Test
    fun `up to date on success`() {
        val at = 1_700_000_000_000L
        val presentation = resolveSyncStatusPresentation(
            SyncSnapshot(
                lastAttemptAtMillis = at,
                eligible = true,
                result = SyncEngine.SyncResult.Success,
            ),
        )
        assertEquals(SyncUiStatus.UpToDate, presentation.status)
        assertEquals(at, presentation.lastAttemptAtMillis)
    }

    @Test
    fun `incomplete on partial failure`() {
        val presentation = resolveSyncStatusPresentation(
            SyncSnapshot(
                lastAttemptAtMillis = 42L,
                eligible = true,
                result = SyncEngine.SyncResult.PartialFailure(listOf("tasks")),
            ),
        )
        assertEquals(SyncUiStatus.Incomplete, presentation.status)
    }

    @Test
    fun `skipped when ineligible pass recorded`() {
        val at = 42L
        val presentation = resolveSyncStatusPresentation(
            SyncSnapshot(
                lastAttemptAtMillis = at,
                eligible = false,
                result = null,
            ),
        )
        assertEquals(SyncUiStatus.Skipped, presentation.status)
        assertEquals(at, presentation.lastAttemptAtMillis)
        assertEquals(
            "Skipped — not eligible (guest or owner mismatch)",
            resolveSyncPassDetail(eligible = false, result = null),
        )
    }

    @Test
    fun `failed when eligible but no structured result`() {
        val presentation = resolveSyncStatusPresentation(
            SyncSnapshot(
                lastAttemptAtMillis = 42L,
                eligible = true,
                result = null,
            ),
        )
        assertEquals(SyncUiStatus.Failed, presentation.status)
    }
}
