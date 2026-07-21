package com.endiar.purrodoro.focusshield

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Process-wide arming state shared by UI and [FocusShieldMonitor].
 */
object FocusShieldController {

    data class Snapshot(
        val preferenceEnabled: Boolean = false,
        val blockedPackages: Set<String> = emptySet(),
        val usageAccessEnabled: Boolean = false,
        val overlayEnabled: Boolean = false,
        val armed: Boolean = false,
        val remainingSeconds: Int = 0,
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    private val bypass = AtomicReference<Bypass?>(null)

    data class Bypass(val packageName: String, val untilMillis: Long)

    fun publish(snapshot: Snapshot) {
        _snapshot.value = snapshot
    }

    fun bypass(packageName: String, untilMillis: Long) {
        bypass.set(Bypass(packageName, untilMillis))
    }

    fun clearBypass() {
        bypass.set(null)
    }

    fun currentBypass(): Bypass? = bypass.get()
}
