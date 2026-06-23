package com.endiar.purrodoro.billing

/**
 * Localized subscription prices from Play [ProductDetails], keyed by base plan.
 */
data class SubscriptionOffers(
    val monthlyPrice: String? = null,
    val yearlyPrice: String? = null,
) {
    fun priceFor(plan: PremiumPlan): String? = when (plan) {
        PremiumPlan.Monthly -> monthlyPrice
        PremiumPlan.Yearly -> yearlyPrice
    }

    val priceHint: String?
        get() = when {
            monthlyPrice != null && yearlyPrice != null ->
                "$monthlyPrice / month · $yearlyPrice / year"
            monthlyPrice != null -> monthlyPrice
            yearlyPrice != null -> yearlyPrice
            else -> null
        }
}
