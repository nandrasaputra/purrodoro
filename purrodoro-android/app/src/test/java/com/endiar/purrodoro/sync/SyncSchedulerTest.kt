package com.endiar.purrodoro.sync

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSchedulerTest {

    @Test
    fun `burst of local writes debounces into exactly one sync`() = runTest {
        var syncCount = 0
        val scheduler = SyncScheduler(
            scope = backgroundScope,
            debounceMs = 5_000L,
            sync = { syncCount++ },
        )
        runCurrent() // let the collector subscribe before the first write

        // 10 writes spread over ~1s, well inside the 5s debounce window.
        repeat(10) {
            scheduler.onLocalWrite()
            advanceTimeBy(100L)
        }
        assertEquals("no sync while the burst is still inside the window", 0, syncCount)

        advanceTimeBy(6_000L)
        assertEquals(1, syncCount)
    }

    @Test
    fun `quiet period after a sync allows the next write to sync again`() = runTest {
        var syncCount = 0
        val scheduler = SyncScheduler(
            scope = backgroundScope,
            debounceMs = 5_000L,
            sync = { syncCount++ },
        )
        runCurrent()

        scheduler.onLocalWrite()
        advanceTimeBy(6_000L)
        assertEquals(1, syncCount)

        scheduler.onLocalWrite()
        advanceTimeBy(6_000L)
        assertEquals(2, syncCount)
    }

    @Test
    fun `requestImmediate syncs without waiting for the debounce`() = runTest {
        var syncCount = 0
        val scheduler = SyncScheduler(
            scope = backgroundScope,
            debounceMs = 5_000L,
            sync = { syncCount++ },
        )
        runCurrent()

        scheduler.requestImmediate()
        runCurrent()
        assertEquals(1, syncCount)
    }
}
