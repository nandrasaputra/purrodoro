package com.endiar.kuropomodoro.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class DebugRepositoryTest {

    @Test
    fun initialStateHasNoClockOverride() {
        val repo = DebugRepository()
        assertNull(repo.clockOverride.value)
    }

    @Test
    fun initialStateHasSpeedUpDisabled() {
        val repo = DebugRepository()
        assertEquals(false, repo.speedUpTimer.value)
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
    fun setSpeedUpTimerUpdatesFlow() {
        val repo = DebugRepository()
        repo.setSpeedUpTimer(true)
        assertEquals(true, repo.speedUpTimer.value)
    }

    @Test
    fun initCreatesInstanceAndAssignsCompanion() {
        val repo = DebugRepository.init()
        assertEquals(repo, DebugRepository.instance)
    }
}
