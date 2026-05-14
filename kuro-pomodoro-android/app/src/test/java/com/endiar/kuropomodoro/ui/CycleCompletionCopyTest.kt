package com.endiar.kuropomodoro.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleCompletionCopyTest {

    @Test
    fun `pool has exactly five variants`() {
        assertEquals(5, CycleCompletionCopy.variants.size)
    }

    @Test
    fun `every variant has non-blank title and body`() {
        CycleCompletionCopy.variants.forEach { v ->
            assertTrue("title blank for $v", v.title.isNotBlank())
            assertTrue("body blank for $v", v.body.isNotBlank())
        }
    }

    @Test
    fun `button label is Done`() {
        assertEquals("Done", CycleCompletionCopy.buttonLabel)
    }
}
