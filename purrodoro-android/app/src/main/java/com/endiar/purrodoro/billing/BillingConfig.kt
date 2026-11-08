package com.endiar.purrodoro.billing

/**
 * Play Console one-time product. ID must match Monetize → One-time products
 * or [BillingManager] will not find a price.
 */
object BillingConfig {
    const val PRODUCT_ID = "purrodoro_premium_one_time"
    const val DISCOUNT_PRODUCT_ID = "purrodoro_premium_discount"

    val premiumProductIds = setOf(PRODUCT_ID, DISCOUNT_PRODUCT_ID)
}
