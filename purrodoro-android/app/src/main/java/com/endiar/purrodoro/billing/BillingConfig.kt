package com.endiar.purrodoro.billing

/**
 * Play Console subscription product configuration.
 *
 * Create in Play Console → Monetize → Subscriptions:
 * - Product ID: [SUBSCRIPTION_PRODUCT_ID]
 * - Base plans: [BASE_PLAN_MONTHLY], [BASE_PLAN_YEARLY]
 *
 * IDs must match exactly or [BillingManager] will not find offers.
 */
object BillingConfig {
    const val SUBSCRIPTION_PRODUCT_ID = "purrodoro_premium"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"
}
