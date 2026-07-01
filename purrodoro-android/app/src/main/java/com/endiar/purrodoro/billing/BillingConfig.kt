package com.endiar.purrodoro.billing

/**
 * Play Console subscription product configuration.
 *
 * Create in Play Console → Monetize → Subscriptions:
 * - Product ID: [SUBSCRIPTION_PRODUCT_ID]
 * - Base plans: [BASE_PLAN_MONTHLY], [BASE_PLAN_YEARLY]
 * - Offer on yearly: [OFFER_YEARLY_TRIAL] (7-day free trial, new subscribers only)
 *
 * IDs must match exactly or [BillingManager] will not find offers.
 */
object BillingConfig {
    const val SUBSCRIPTION_PRODUCT_ID = "purrodoro_premium"
    const val BASE_PLAN_MONTHLY = "purrodoro-premium-monthly"
    const val BASE_PLAN_YEARLY = "purrodoro-premium-yearly"
    const val OFFER_YEARLY_TRIAL = "purrodoro-premium-yearly-trial"
}
