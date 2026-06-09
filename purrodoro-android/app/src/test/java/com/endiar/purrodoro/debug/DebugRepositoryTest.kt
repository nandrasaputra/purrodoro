package com.endiar.purrodoro.debug

import com.endiar.purrodoro.model.PremiumOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DebugRepositoryTest {

    @Test
    fun initialStateHasNoClockOverride() {
        val repo = DebugRepository()
        assertNull(repo.clockOverride.value)
    }

    @Test
    fun setClockOverrideUpdatesFlow() {
        val repo = DebugRepository()
        val time = LocalTime.of(22, 0)
        repo.setClockOverride(time)
        assertEquals(time, repo.clockOverride.value)
    }

    @Test
    fun clearClockOverrideSetsNull() {
        val repo = DebugRepository()
        repo.setClockOverride(LocalTime.of(8, 0))
        repo.setClockOverride(null)
        assertNull(repo.clockOverride.value)
    }

    @Test
    fun initCreatesInstanceAndAssignsCompanion() {
        val repo = DebugRepository.init()
        assertEquals(repo, DebugRepository.instance)
    }

    @Test
    fun premiumOverrideCanForceTrueOrFalse() {
        val repo = DebugRepository.init()
        PremiumOverride.source = repo

        assertNull(PremiumOverride.overrideValue())

        repo.setPremiumOverrideEnabled(true)
        repo.setPremiumOverrideValue(true)
        assertTrue(PremiumOverride.overrideValue()!!)

        repo.setPremiumOverrideValue(false)
        assertFalse(PremiumOverride.overrideValue()!!)

        repo.setPremiumOverrideEnabled(false)
        assertNull(PremiumOverride.overrideValue())
    }
}
