package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerSessionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerCycleProgressTest {

    private fun items() = listOf(
        TimerSessionItem(SessionItemType.Focus(25 * 60)),
        TimerSessionItem(SessionItemType.ShortBreak(5 * 60)),
        TimerSessionItem(SessionItemType.Focus(25 * 60)),
        TimerSessionItem(SessionItemType.LongBreak(15 * 60)),
    )

    @Test
    fun `whole cycle totals 4200 seconds`() {
        val p = cycleProgress(items(), currentIndex = 0, segmentRemainingSeconds = 25 * 60)
        assertEquals(4200, p.totalSeconds)
        assertEquals(0, p.elapsedSeconds)
    }

    @Test
    fun `elapsed accumulates finished segments plus current progress`() {
        // 12:34 (754s) remaining of first focus -> 746s elapsed.
        val p = cycleProgress(items(), currentIndex = 0, segmentRemainingSeconds = 754)
        assertEquals(746, p.elapsedSeconds)
        // Mid short break: full focus plus 108s into the 300s break.
        val q = cycleProgress(items(), currentIndex = 1, segmentRemainingSeconds = 192)
        assertEquals(1608, q.elapsedSeconds)
    }

    @Test
    fun `out of range index clamps elapsed to total`() {
        val p = cycleProgress(items(), currentIndex = 9, segmentRemainingSeconds = 0)
        assertEquals(4200, p.elapsedSeconds)
    }

    @Test
    fun `single focus cycle totals its own length`() {
        val single = listOf(TimerSessionItem(SessionItemType.Focus(25 * 60)))
        val p = cycleProgress(single, currentIndex = 0, segmentRemainingSeconds = 900)
        assertEquals(1500, p.totalSeconds)
        assertEquals(600, p.elapsedSeconds)
    }
}
