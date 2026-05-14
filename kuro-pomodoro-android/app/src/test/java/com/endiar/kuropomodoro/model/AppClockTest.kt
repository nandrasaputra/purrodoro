package com.endiar.kuropomodoro.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class AppClockTest {

    @After
    fun tearDown() {
        AppClock.source = null
    }

    @Test
    fun nowReturnsRealTimeWhenSourceIsNull() {
        AppClock.source = null
        val before = LocalTime.now()
        val result = AppClock.now()
        val after = LocalTime.now()
        assert(!result.isBefore(before) && !result.isAfter(after))
    }

    @Test
    fun nowReturnsOverrideWhenSourceHasValue() {
        val fixed = LocalTime.of(22, 0)
        AppClock.source = fakeSource(time = fixed)
        assertEquals(fixed, AppClock.now())
    }

    @Test
    fun nowReturnsRealTimeWhenOverrideIsNull() {
        AppClock.source = fakeSource()
        val before = LocalTime.now()
        val result = AppClock.now()
        val after = LocalTime.now()
        assert(!result.isBefore(before) && !result.isAfter(after))
    }

    @Test
    fun tickIntervalMsReturns250WhenSourceIsNull() {
        AppClock.source = null
        assertEquals(250L, AppClock.tickIntervalMs())
    }

    @Test
    fun tickIntervalMsReturns250WhenSpeedUpIsFalse() {
        AppClock.source = fakeSource()
        assertEquals(250L, AppClock.tickIntervalMs())
    }

    @Test
    fun tickIntervalMsReturns25WhenSpeedUpIsTrue() {
        AppClock.source = fakeSource(speedUp = true)
        assertEquals(25L, AppClock.tickIntervalMs())
    }

    private fun fakeSource(time: LocalTime? = null, speedUp: Boolean = false): ClockDebugSource =
        object : ClockDebugSource {
            override val clockOverride = MutableStateFlow(time)
            override val speedUpTimer = MutableStateFlow(speedUp)
        }
}
