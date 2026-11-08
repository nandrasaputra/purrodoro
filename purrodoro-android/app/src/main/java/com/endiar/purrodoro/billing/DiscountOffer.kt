package com.endiar.purrodoro.billing

/**
 * Device-local discount window. Leaving the full-price paywall starts 24 hours
 * at the discount price. The same leave can start it again only after 7 days
 * from that start.
 */
object DiscountOffer {
    const val ACTIVE_WINDOW_MS = 24L * 60L * 60L * 1000L
    const val CYCLE_MS = 7L * 24L * 60L * 60L * 1000L

    enum class Phase {
        Idle,
        Active,
        Cooldown,
    }

    fun phase(offeredAtMillis: Long?, nowMillis: Long): Phase {
        if (offeredAtMillis == null) return Phase.Idle
        val elapsed = nowMillis - offeredAtMillis
        return when {
            elapsed < ACTIVE_WINDOW_MS -> Phase.Active
            elapsed < CYCLE_MS -> Phase.Cooldown
            else -> Phase.Idle
        }
    }

    /**
     * @return the offer start to keep. A new timestamp means a window just started.
     * The clock stays put when the discount price is missing, or while a window
     * or its following week is still running.
     */
    fun onPaywallDismissed(
        offeredAtMillis: Long?,
        nowMillis: Long,
        discountPriceReady: Boolean,
    ): Long? {
        if (!discountPriceReady) return offeredAtMillis
        if (phase(offeredAtMillis, nowMillis) != Phase.Idle) return offeredAtMillis
        return nowMillis
    }

    fun formatRemaining(remainingMs: Long): String {
        val totalSeconds = remainingMs.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
