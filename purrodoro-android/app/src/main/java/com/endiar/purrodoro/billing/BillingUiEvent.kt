package com.endiar.purrodoro.billing

import androidx.annotation.StringRes
import com.endiar.purrodoro.R

/** One-shot UI signal from [BillingManager] (snackbar copy). */
sealed class BillingUiEvent {
    data class Message(@StringRes val resId: Int) : BillingUiEvent()

    companion object {
        val ServiceUnavailable = Message(R.string.billing_error_service_unavailable)
        val SubscriptionsUnsupported = Message(R.string.billing_error_subscriptions_unsupported)
        val ProductsUnavailable = Message(R.string.billing_error_products_unavailable)
        val PurchaseFailed = Message(R.string.billing_error_purchase_failed)
        val LaunchFailed = Message(R.string.billing_error_launch_failed)
        val RestoreComplete = Message(R.string.billing_restore_complete)
        val RestoreNothingFound = Message(R.string.billing_restore_nothing_found)
    }
}
