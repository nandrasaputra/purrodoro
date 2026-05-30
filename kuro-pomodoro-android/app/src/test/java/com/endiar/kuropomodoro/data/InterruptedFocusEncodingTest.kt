package com.endiar.kuropomodoro.data

import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.InterruptedFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterruptedFocusEncodingTest {

    @Test
    fun roundTripsTaskInterruption() {
        val value = InterruptedFocus(
            focusType = FocusType.Task,
            taskId = 7,
            habitId = null,
            title = "Write product spec | draft",
            remainingSeconds = 12 * 60 + 5,
        )

        assertEquals(value, decodeInterruptedFocus(encodeInterruptedFocus(value)))
    }

    @Test
    fun roundTripsHabitInterruption() {
        val value = InterruptedFocus(
            focusType = FocusType.Habit,
            taskId = null,
            habitId = 3,
            title = "📚 Read 10 pages",
            remainingSeconds = 8 * 60,
        )

        assertEquals(value, decodeInterruptedFocus(encodeInterruptedFocus(value)))
    }

    @Test
    fun decodesBlankAndGarbageToNull() {
        assertNull(decodeInterruptedFocus(""))
        assertNull(decodeInterruptedFocus("not-a-valid-record"))
    }
}
