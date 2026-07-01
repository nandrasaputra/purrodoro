package com.endiar.purrodoro.billing

import androidx.annotation.StringRes
import com.endiar.purrodoro.R

/**
 * Localized subscription prices and trial metadata from Play [ProductDetails].
 */
data class PlanOfferDetails(
    val priceLine: String? = null,
    val secondaryPriceLine: String? = null,
    val hasTrial: Boolean = false,
    /** Parsed from Play trial phase `billingPeriod`; null when unknown. */
    val trialPeriod: TrialPeriod? = null,
    val hasOffer: Boolean = false,
)

data class SubscriptionOffers(
    val catalogState: BillingCatalogState = BillingCatalogState.Idle,
    val monthly: PlanOfferDetails = PlanOfferDetails(),
    val yearly: PlanOfferDetails = PlanOfferDetails(),
    /** Approximate savings vs paying monthly × 12; null when prices unknown. */
    val savingsPercent: Int? = null,
) {
    fun forPlan(plan: PremiumPlan): PlanOfferDetails = when (plan) {
        PremiumPlan.Monthly -> monthly
        PremiumPlan.Yearly -> yearly
    }

    val isReady: Boolean get() = catalogState == BillingCatalogState.Ready

    @StringRes
    fun ctaResId(plan: PremiumPlan, forSheet: Boolean): Int =
        if (plan == PremiumPlan.Yearly && yearly.hasTrial) {
            R.string.premium_cta_start_trial
        } else if (forSheet) {
            R.string.premium_sheet_cta
        } else {
            R.string.premium_screen_cta_subscribe
        }
}
