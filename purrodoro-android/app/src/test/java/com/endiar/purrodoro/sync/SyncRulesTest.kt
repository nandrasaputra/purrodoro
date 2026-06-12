package com.endiar.purrodoro.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRulesTest {

    @Test
    fun `no local row applies remote`() {
        assertEquals(PullAction.Apply, resolvePull(localDirty = null, remoteDeleted = false))
    }

    @Test
    fun `clean local row applies remote`() {
        assertEquals(PullAction.Apply, resolvePull(localDirty = false, remoteDeleted = false))
    }

    @Test
    fun `dirty local row keeps local`() {
        assertEquals(PullAction.KeepLocal, resolvePull(localDirty = true, remoteDeleted = false))
    }

    @Test
    fun `clean local row applies remote tombstone`() {
        assertEquals(PullAction.Apply, resolvePull(localDirty = false, remoteDeleted = true))
    }

    @Test
    fun `dirty local row survives remote tombstone`() {
        assertEquals(PullAction.KeepLocal, resolvePull(localDirty = true, remoteDeleted = true))
    }
}
