package com.endiar.purrodoro.focusshield

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerState

/**
 * Pure rules for Focus Shield — session-bound distraction interrupt.
 * Detection = UsageStats; interrupt = overlay (no Accessibility).
 */
object FocusShield {

    fun isArmed(
        preferenceEnabled: Boolean,
        blockedPackages: Set<String>,
        usageAccessEnabled: Boolean,
        overlayEnabled: Boolean,
        runtime: TimerRuntimeState?,
    ): Boolean {
        if (!preferenceEnabled || blockedPackages.isEmpty()) return false
        if (!usageAccessEnabled || !overlayEnabled) return false
        val session = runtime?.session ?: return false
        if (session.timerState == TimerState.Idle) return false
        return session.currentItem?.type is SessionItemType.Focus
    }

    fun shouldInterrupt(
        armed: Boolean,
        foregroundPackage: String?,
        blockedPackages: Set<String>,
        ownPackage: String,
        bypassPackage: String?,
        bypassUntilMillis: Long,
        nowMillis: Long,
    ): Boolean {
        if (!armed) return false
        val pkg = foregroundPackage ?: return false
        if (pkg == ownPackage) return false
        if (pkg == bypassPackage && nowMillis < bypassUntilMillis) return false
        return pkg in blockedPackages
    }
}
