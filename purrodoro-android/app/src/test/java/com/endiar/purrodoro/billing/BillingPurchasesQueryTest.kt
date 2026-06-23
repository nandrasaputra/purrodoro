package com.endiar.purrodoro.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingPurchasesQueryTest {

    @Test
    fun successWhenResponseOk() {
        val outcome = purchasesQueryOutcome(
            responseCode = BillingClient.BillingResponseCode.OK,
            purchases = emptyList(),
        )
        assertTrue(outcome is PurchasesQueryOutcome.Success)
        assertEquals(emptyList<Purchase>(), (outcome as PurchasesQueryOutcome.Success).purchases)
    }

    @Test
    fun failedWhenResponseNotOk() {
        val outcome = purchasesQueryOutcome(
            responseCode = BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            purchases = emptyList(),
        )
        assertEquals(PurchasesQueryOutcome.Failed, outcome)
    }
}
