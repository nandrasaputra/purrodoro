package com.endiar.purrodoro.billing

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.endiar.purrodoro.data.AppRepository

/**
 * Process-wide billing singleton. Mirrors [com.endiar.purrodoro.sync.SyncModule].
 */
class BillingModule private constructor(
    private val manager: BillingManager,
) {

    val billingManager: BillingManager get() = manager

    companion object {
        @Volatile
        private var instance: BillingModule? = null

        fun get(context: Context): BillingModule =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(appContext: Context): BillingModule {
            val repo = AppRepository.get(appContext)
            val manager = BillingManager(
                repo = repo,
                billingClientFactory = { listener ->
                    BillingClient.newBuilder(appContext)
                        .setListener(listener)
                        .enablePendingPurchases(
                            PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build(),
                        )
                        .enableAutoServiceReconnection()
                        .build()
                },
            )
            val module = BillingModule(manager)
            manager.start()
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        manager.refreshOnForeground()
                    }
                },
            )
            return module
        }
    }
}
