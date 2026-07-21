package com.endiar.purrodoro.focusshield

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls UsageStats for foreground package while Focus Shield is armed.
 * Runs inside [com.endiar.purrodoro.timer.TimerService] process so FGS keeps it alive.
 */
object FocusShieldMonitor {

    private const val POLL_MS = 3_000L
    private var job: Job? = null

    fun start(scope: CoroutineScope, context: Context) {
        if (job?.isActive == true) return
        val appContext = context.applicationContext
        job = scope.launch {
            while (isActive) {
                evaluate(appContext)
                delay(POLL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        FocusShieldOverlayController.hide()
    }

    private fun evaluate(context: Context) {
        val snap = FocusShieldController.snapshot.value
        if (!snap.armed) {
            if (FocusShieldOverlayController.isShowing()) FocusShieldOverlayController.hide()
            return
        }
        if (FocusShieldOverlayController.isShowing()) return

        val fg = FocusShieldForeground.currentPackage(context) ?: return
        val bypass = FocusShieldController.currentBypass()
        val now = System.currentTimeMillis()
        if (!FocusShield.shouldInterrupt(
                armed = true,
                foregroundPackage = fg,
                blockedPackages = snap.blockedPackages,
                ownPackage = context.packageName,
                bypassPackage = bypass?.packageName,
                bypassUntilMillis = bypass?.untilMillis ?: 0L,
                nowMillis = now,
            )
        ) {
            return
        }
        FocusShieldOverlayController.show(context, fg, snap.remainingSeconds)
    }
}
