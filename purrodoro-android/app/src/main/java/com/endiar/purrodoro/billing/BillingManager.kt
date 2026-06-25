package com.endiar.purrodoro.billing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.endiar.purrodoro.data.AppRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

private const val PurchaseInFlightTimeoutMs = 5 * 60_000L

/**
 * Play Billing client for the Purrodoro Premium subscription.
 *
 * Owns product catalog queries, purchase flow launch, acknowledgement, and
 * syncing [AppRepository.setPlayPremium] from owned subscriptions (reconciled
 * with any Supabase admin override on the signed-in user's profile).
 */
class BillingManager(
    private val repo: AppRepository,
    billingClientFactory: (PurchasesUpdatedListener) -> BillingClient,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectMutex = Mutex()

    private val billingClient: BillingClient = billingClientFactory(this)

    private val _offers = MutableStateFlow(SubscriptionOffers())
    val offers: StateFlow<SubscriptionOffers> = _offers.asStateFlow()

    private val _purchaseInFlight = MutableStateFlow(false)
    val purchaseInFlight: StateFlow<Boolean> = _purchaseInFlight.asStateFlow()

    private val _uiEvents = MutableSharedFlow<BillingUiEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEvents: SharedFlow<BillingUiEvent> = _uiEvents.asSharedFlow()

    private var productDetails: ProductDetails? = null
    private var purchaseInFlightTimeoutJob: Job? = null

    private fun setPurchaseInFlight(inFlight: Boolean) {
        _purchaseInFlight.value = inFlight
        purchaseInFlightTimeoutJob?.cancel()
        purchaseInFlightTimeoutJob = null
        if (inFlight) {
            purchaseInFlightTimeoutJob = scope.launch {
                delay(PurchaseInFlightTimeoutMs)
                _purchaseInFlight.value = false
                logBilling("purchase_in_flight_timeout")
            }
        }
    }

    fun start() {
        scope.launch {
            if (!ensureConnected()) return@launch
            refreshCatalog()
            syncEntitlementFromPlay()
        }
    }

    fun refreshOnForeground() {
        scope.launch {
            if (!billingClient.isReady) return@launch
            refreshCatalog()
            syncEntitlementFromPlay()
        }
    }

    /** Re-query Play after sign-in or account changes on the device. */
    fun refreshEntitlement() {
        scope.launch {
            if (!ensureConnected()) return@launch
            syncEntitlementFromPlay()
        }
    }

    fun launchSubscribe(activity: Activity?, plan: PremiumPlan) {
        if (activity == null) {
            emitUiEvent(BillingUiEvent.LaunchFailed)
            return
        }
        scope.launch {
            if (!ensureConnected()) {
                emitUiEvent(BillingUiEvent.ServiceUnavailable)
                return@launch
            }
            if (!subscriptionsSupported()) {
                emitUiEvent(BillingUiEvent.SubscriptionsUnsupported)
                return@launch
            }
            refreshCatalog()
            val details = productDetails
            if (details == null) {
                emitUiEvent(BillingUiEvent.ProductsUnavailable)
                return@launch
            }
            val offer = details.selectOfferForBasePlan(plan.basePlanId)
            if (offer == null) {
                logBilling("subscribe_missing_offer plan=${plan.basePlanId}")
                emitUiEvent(BillingUiEvent.ProductsUnavailable)
                return@launch
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offer.offerToken)
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            setPurchaseInFlight(true)
            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                logBilling("launch_billing_flow code=${result.responseCode}")
                emitUiEvent(BillingUiEvent.LaunchFailed)
                setPurchaseInFlight(false)
            }
        }
    }

    fun restorePurchases() {
        scope.launch {
            setPurchaseInFlight(true)
            try {
                if (!ensureConnected()) {
                    emitUiEvent(BillingUiEvent.ServiceUnavailable)
                    return@launch
                }
                when (syncEntitlementFromPlay()) {
                    null -> emitUiEvent(BillingUiEvent.ServiceUnavailable)
                    true -> emitUiEvent(BillingUiEvent.RestoreComplete)
                    false -> emitUiEvent(BillingUiEvent.RestoreNothingFound)
                }
            } finally {
                setPurchaseInFlight(false)
            }
        }
    }

    fun openSubscriptionManagement(activity: Activity?) {
        if (activity == null) {
            emitUiEvent(BillingUiEvent.LaunchFailed)
            return
        }
        val uri = Uri.parse(
            "https://play.google.com/store/account/subscriptions?package=${activity.packageName}" +
                "&sku=${BillingConfig.SUBSCRIPTION_PRODUCT_ID}",
        )
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        scope.launch {
            try {
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        purchases?.let { acknowledgePurchases(it) }
                        syncEntitlementFromPlay()
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                    else -> {
                        logBilling(
                            "purchases_updated code=${result.responseCode} " +
                                "debug=${result.debugMessage}",
                        )
                        emitUiEvent(BillingUiEvent.PurchaseFailed)
                        syncEntitlementFromPlay()
                    }
                }
            } finally {
                setPurchaseInFlight(false)
            }
        }
    }

    private suspend fun ensureConnected(): Boolean = connectMutex.withLock {
        if (billingClient.isReady) return true
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                        if (!ok) {
                            logBilling(
                                "billing_setup code=${result.responseCode} " +
                                    "debug=${result.debugMessage}",
                            )
                        }
                        cont.resume(ok)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (cont.isActive) cont.resume(false)
                    }
                },
            )
        }
    }

    private fun subscriptionsSupported(): Boolean {
        val result = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private suspend fun refreshCatalog() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConfig.SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()

        val result = runCatching { billingClient.queryProductDetails(params) }.getOrNull()
        if (result == null) {
            logBilling("query_product_details_failed")
            productDetails = null
            return
        }
        val details = result.productDetailsList?.firstOrNull()
        if (details == null) {
            logBilling("query_product_details_empty")
            productDetails = null
            return
        }
        productDetails = details
        _offers.value = SubscriptionOffers(
            monthlyPrice = details.formattedPriceForBasePlan(BillingConfig.BASE_PLAN_MONTHLY),
            yearlyPrice = details.formattedPriceForBasePlan(BillingConfig.BASE_PLAN_YEARLY),
        )
    }

    /**
     * @return `true`/`false` when entitlement was synced from Play; `null` when the
     * purchases query failed and local premium state was left unchanged.
     */
    private suspend fun syncEntitlementFromPlay(): Boolean? {
        return when (val outcome = querySubscriptionPurchases()) {
            PurchasesQueryOutcome.Failed -> {
                logBilling("sync_entitlement_skipped_query_failed")
                null
            }
            is PurchasesQueryOutcome.Success -> {
                acknowledgePurchases(outcome.purchases)
                val premium = PremiumEntitlement.hasActivePremium(outcome.purchases)
                repo.setPlayPremium(premium)
                premium
            }
        }
    }

    private suspend fun querySubscriptionPurchases(): PurchasesQueryOutcome {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .includeSuspendedSubscriptions(true)
            .build()
        val result = runCatching { billingClient.queryPurchasesAsync(params) }.getOrNull()
            ?: return PurchasesQueryOutcome.Failed
        return purchasesQueryOutcome(
            responseCode = result.billingResult.responseCode,
            purchases = result.purchasesList,
        )
    }

    private suspend fun acknowledgePurchases(purchases: List<Purchase>) {
        for (purchase in PremiumEntitlement.purchasesNeedAcknowledgement(purchases)) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val ackResult = runCatching { billingClient.acknowledgePurchase(params) }.getOrNull()
            if (ackResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                logBilling(
                    "acknowledge_failed code=${ackResult?.responseCode} " +
                        "token=${purchase.purchaseToken.take(8)}",
                )
            }
        }
    }

    private fun emitUiEvent(event: BillingUiEvent) {
        _uiEvents.tryEmit(event)
    }

    private fun logBilling(message: String) {
        runCatching { FirebaseCrashlytics.getInstance().log("billing: $message") }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
