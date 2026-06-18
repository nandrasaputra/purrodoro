package com.endiar.purrodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumGateTest {

    @Test
    fun `activeTaskCount ignores completed tasks`() {
        val tasks = listOf(
            FocusTask(id = 1, title = "A", minutes = 25, completed = false),
            FocusTask(id = 2, title = "B", minutes = 25, completed = true),
            FocusTask(id = 3, title = "C", minutes = 25, completed = false),
        )
        assertEquals(2, PremiumGate.activeTaskCount(tasks))
    }

    @Test
    fun `canAddActiveTask allows up to limit for free users`() {
        assertTrue(PremiumGate.canAddActiveTask(9, isPremium = false))
        assertFalse(PremiumGate.canAddActiveTask(10, isPremium = false))
        assertFalse(PremiumGate.canAddActiveTask(11, isPremium = false))
    }

    @Test
    fun `canAddActiveTask is unlimited for premium`() {
        assertTrue(PremiumGate.canAddActiveTask(10, isPremium = true))
        assertTrue(PremiumGate.canAddActiveTask(100, isPremium = true))
    }

    @Test
    fun `canAddHabit allows up to limit for free users`() {
        assertTrue(PremiumGate.canAddHabit(9, isPremium = false))
        assertFalse(PremiumGate.canAddHabit(10, isPremium = false))
        assertFalse(PremiumGate.canAddHabit(11, isPremium = false))
    }

    @Test
    fun `canAddHabit is unlimited for premium`() {
        assertTrue(PremiumGate.canAddHabit(7, isPremium = true))
        assertTrue(PremiumGate.canAddHabit(50, isPremium = true))
    }
}
