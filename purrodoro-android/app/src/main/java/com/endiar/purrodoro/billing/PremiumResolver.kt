package com.endiar.purrodoro.billing

/**
 * Combines Supabase admin override with store billing.
 *
 * When [overrideEnabled], [overrideActive] is the premium flag (review grants,
 * forced free tests). Otherwise [playPremium] from Google Play applies.
 */
object PremiumResolver {

    fun resolve(
        overrideEnabled: Boolean,
        overrideActive: Boolean,
        playPremium: Boolean,
    ): Boolean = if (overrideEnabled) overrideActive else playPremium
}
