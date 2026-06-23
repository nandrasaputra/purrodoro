package com.endiar.purrodoro.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase

internal sealed class PurchasesQueryOutcome {
    data class Success(val purchases: List<Purchase>) : PurchasesQueryOutcome()
    data object Failed : PurchasesQueryOutcome()
}

/** Maps a Play purchases query into a sync-safe outcome (failed queries must not revoke premium). */
internal fun purchasesQueryOutcome(
    responseCode: Int,
    purchases: List<Purchase>,
): PurchasesQueryOutcome =
    if (responseCode == BillingClient.BillingResponseCode.OK) {
        PurchasesQueryOutcome.Success(purchases)
    } else {
        PurchasesQueryOutcome.Failed
    }
