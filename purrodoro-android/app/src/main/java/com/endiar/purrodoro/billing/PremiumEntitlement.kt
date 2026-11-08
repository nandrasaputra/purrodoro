package com.endiar.purrodoro.billing

import com.android.billingclient.api.Purchase

/**
 * Pure helpers for deciding whether Play purchases grant Premium.
 */
object PremiumEntitlement {

    fun purchaseGrantsPremium(
        purchase: Purchase,
        productId: String = BillingConfig.PRODUCT_ID,
    ): Boolean = grantsPremium(
        products = purchase.products,
        purchaseState = purchase.purchaseState,
        productId = productId,
    )

    internal fun grantsPremium(
        products: Collection<String>,
        purchaseState: Int,
        productId: String = BillingConfig.PRODUCT_ID,
    ): Boolean {
        if (productId !in products) return false
        return purchaseState == Purchase.PurchaseState.PURCHASED
    }

    fun hasActivePremium(purchases: List<Purchase>): Boolean =
        purchases.any { purchase ->
            BillingConfig.premiumProductIds.any { productId ->
                purchaseGrantsPremium(purchase, productId)
            }
        }

    fun purchasesNeedAcknowledgement(purchases: List<Purchase>): List<Purchase> =
        purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
        }
}
