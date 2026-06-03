package com.endiar.purrodoro.ui

import com.endiar.purrodoro.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleCompletionCopyTest {

    @Test
    fun `pool has exactly five variants`() {
        assertEquals(5, CycleCompletionCopy.variants.size)
    }

    @Test
    fun `every variant references real string resources`() {
        CycleCompletionCopy.variants.forEach { v ->
            assertNotEquals("title res unset for $v", 0, v.titleRes)
            assertNotEquals("body res unset for $v", 0, v.bodyRes)
        }
    }

    @Test
    fun `variant title and body resources are distinct`() {
        CycleCompletionCopy.variants.forEach { v ->
            assertTrue("title and body identical for $v", v.titleRes != v.bodyRes)
        }
    }

    @Test
    fun `button label points at the done string`() {
        assertEquals(R.string.cycle_done, CycleCompletionCopy.buttonLabelRes)
    }
}
