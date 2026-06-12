package com.endiar.purrodoro.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutFlushTest {

    @Test
    fun `clean flush signs out`() {
        assertEquals(
            LogoutDecision.SignOut,
            logoutDecision(flushSucceeded = true, dirtyRemaining = false),
        )
    }

    @Test
    fun `failed flush with dirty rows warns about unsynced data`() {
        assertEquals(
            LogoutDecision.WarnUnsynced,
            logoutDecision(flushSucceeded = false, dirtyRemaining = true),
        )
    }

    @Test
    fun `failed flush with nothing dirty signs out`() {
        assertEquals(
            LogoutDecision.SignOut,
            logoutDecision(flushSucceeded = false, dirtyRemaining = false),
        )
    }
}
