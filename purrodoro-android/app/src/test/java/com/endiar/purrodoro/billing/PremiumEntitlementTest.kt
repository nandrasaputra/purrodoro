package com.endiar.purrodoro.billing

import com.android.billingclient.api.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PremiumEntitlementTest {

    @Test
    fun grantsPremium_whenPurchasedAndProductMatches() {
        assertTrue(
            PremiumEntitlement.grantsPremium(
                products = listOf(BillingConfig.SUBSCRIPTION_PRODUCT_ID),
                purchaseState = Purchase.PurchaseState.PURCHASED,
            ),
        )
    }

    @Test
    fun grantsPremium_falseForWrongProduct() {
        assertFalse(
            PremiumEntitlement.grantsPremium(
                products = listOf("other_product"),
                purchaseState = Purchase.PurchaseState.PURCHASED,
            ),
        )
    }

    @Test
    fun grantsPremium_falseForNonPurchasedState() {
        assertFalse(
            PremiumEntitlement.grantsPremium(
                products = listOf(BillingConfig.SUBSCRIPTION_PRODUCT_ID),
                purchaseState = Purchase.PurchaseState.PENDING,
            ),
        )
    }

    @Test
    fun purchaseGrantsPremium_readsLivePurchaseObject() {
        val purchase = Purchase(
            """
            {
              "orderId": "order",
              "packageName": "com.endiar.purrodoro",
              "productId": "${BillingConfig.SUBSCRIPTION_PRODUCT_ID}",
              "products": ["${BillingConfig.SUBSCRIPTION_PRODUCT_ID}"],
              "purchaseTime": 1,
              "purchaseState": ${Purchase.PurchaseState.PURCHASED},
              "purchaseToken": "token",
              "quantity": 1,
              "acknowledged": true
            }
            """.trimIndent(),
            "signature",
        )
        assertTrue(PremiumEntitlement.purchaseGrantsPremium(purchase))
    }

    @Test
    fun purchasesNeedAcknowledgement_filtersUnacknowledgedPurchased() {
        val purchases = listOf(
            purchase(acknowledged = false),
            purchase(acknowledged = true),
        )
        assertEquals(1, PremiumEntitlement.purchasesNeedAcknowledgement(purchases).size)
    }

    @Test
    fun hasActivePremium_trueWhenAnyPurchaseGrants() {
        val purchases = listOf(
            purchase(acknowledged = true, productId = "other_product"),
            purchase(acknowledged = true),
        )
        assertTrue(PremiumEntitlement.hasActivePremium(purchases))
    }

    @Test
    fun hasActivePremium_falseWhenNoMatchingPurchase() {
        val purchases = listOf(
            purchase(acknowledged = true, productId = "other_product"),
        )
        assertFalse(PremiumEntitlement.hasActivePremium(purchases))
    }

    private fun purchase(
        acknowledged: Boolean,
        productId: String = BillingConfig.SUBSCRIPTION_PRODUCT_ID,
    ): Purchase = Purchase(
        """
        {
          "orderId": "order",
          "packageName": "com.endiar.purrodoro",
          "productId": "$productId",
          "products": ["$productId"],
          "purchaseTime": 1,
          "purchaseState": ${Purchase.PurchaseState.PURCHASED},
          "purchaseToken": "token-$acknowledged-$productId",
          "quantity": 1,
          "acknowledged": $acknowledged
        }
        """.trimIndent(),
        "signature",
    )
}
