package com.endiar.purrodoro.focusshield

import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusShieldTest {

    private val blocked = setOf("com.instagram.android", "com.zhiliaoapp.musically")

    private fun runtime(
        timerState: TimerState,
        item: SessionItemType = SessionItemType.Focus(25 * 60),
    ): TimerRuntimeState {
        val session = TimerSession(
            items = listOf(TimerSessionItem(item)),
            currentIndex = 0,
            remainingSeconds = 60,
            timerState = timerState,
        )
        return TimerRuntimeState(
            session = session,
            endsAtMillis = if (timerState == TimerState.Playing) 1L else null,
            activeTask = null,
            openRecord = null,
            focusType = FocusType.Empty,
        )
    }

    @Test
    fun `armed only during focus segment when pref on list nonempty perms on`() {
        assertTrue(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = blocked,
                usageAccessEnabled = true,
                overlayEnabled = true,
                runtime = runtime(TimerState.Playing),
            ),
        )
        assertTrue(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = blocked,
                usageAccessEnabled = true,
                overlayEnabled = true,
                runtime = runtime(TimerState.Paused),
            ),
        )
    }

    @Test
    fun `not armed on break even if playing`() {
        assertFalse(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = blocked,
                usageAccessEnabled = true,
                overlayEnabled = true,
                runtime = runtime(TimerState.Playing, SessionItemType.ShortBreak(5 * 60)),
            ),
        )
    }

    @Test
    fun `not armed when pref off empty list missing perms or idle`() {
        assertFalse(
            FocusShield.isArmed(
                preferenceEnabled = false,
                blockedPackages = blocked,
                usageAccessEnabled = true,
                overlayEnabled = true,
                runtime = runtime(TimerState.Playing),
            ),
        )
        assertFalse(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = emptySet(),
                usageAccessEnabled = true,
                overlayEnabled = true,
                runtime = runtime(TimerState.Playing),
            ),
        )
        assertFalse(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = blocked,
                usageAccessEnabled = false,
                overlayEnabled = true,
                runtime = runtime(TimerState.Playing),
            ),
        )
        assertFalse(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = blocked,
                usageAccessEnabled = true,
                overlayEnabled = false,
                runtime = runtime(TimerState.Playing),
            ),
        )
        assertFalse(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = blocked,
                usageAccessEnabled = true,
                overlayEnabled = true,
                runtime = null,
            ),
        )
        assertFalse(
            FocusShield.isArmed(
                preferenceEnabled = true,
                blockedPackages = blocked,
                usageAccessEnabled = true,
                overlayEnabled = true,
                runtime = runtime(TimerState.Idle),
            ),
        )
    }

    @Test
    fun `shouldInterrupt when armed and foreground is blocked`() {
        assertTrue(
            FocusShield.shouldInterrupt(
                armed = true,
                foregroundPackage = "com.instagram.android",
                blockedPackages = blocked,
                ownPackage = "com.endiar.purrodoro",
                bypassPackage = null,
                bypassUntilMillis = 0L,
                nowMillis = 100L,
            ),
        )
    }

    @Test
    fun `shouldInterrupt false for own package unblocked or bypassed`() {
        assertFalse(
            FocusShield.shouldInterrupt(
                armed = true,
                foregroundPackage = "com.endiar.purrodoro",
                blockedPackages = blocked,
                ownPackage = "com.endiar.purrodoro",
                bypassPackage = null,
                bypassUntilMillis = 0L,
                nowMillis = 100L,
            ),
        )
        assertFalse(
            FocusShield.shouldInterrupt(
                armed = true,
                foregroundPackage = "com.android.chrome",
                blockedPackages = blocked,
                ownPackage = "com.endiar.purrodoro",
                bypassPackage = null,
                bypassUntilMillis = 0L,
                nowMillis = 100L,
            ),
        )
        assertFalse(
            FocusShield.shouldInterrupt(
                armed = true,
                foregroundPackage = "com.instagram.android",
                blockedPackages = blocked,
                ownPackage = "com.endiar.purrodoro",
                bypassPackage = "com.instagram.android",
                bypassUntilMillis = 200L,
                nowMillis = 100L,
            ),
        )
        assertTrue(
            FocusShield.shouldInterrupt(
                armed = true,
                foregroundPackage = "com.instagram.android",
                blockedPackages = blocked,
                ownPackage = "com.endiar.purrodoro",
                bypassPackage = "com.instagram.android",
                bypassUntilMillis = 50L,
                nowMillis = 100L,
            ),
        )
        assertFalse(
            FocusShield.shouldInterrupt(
                armed = false,
                foregroundPackage = "com.instagram.android",
                blockedPackages = blocked,
                ownPackage = "com.endiar.purrodoro",
                bypassPackage = null,
                bypassUntilMillis = 0L,
                nowMillis = 100L,
            ),
        )
    }
}
