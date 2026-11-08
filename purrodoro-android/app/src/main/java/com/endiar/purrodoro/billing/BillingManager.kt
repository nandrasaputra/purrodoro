package com.endiar.purrodoro.billing

import android.app.Activity
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
 * Play Billing client for the one-time Purrodoro Premium product.
 *
 * Owns catalog queries, the purchase flow, acknowledgement, and syncing
 * [AppRepository.setPlayPremium] from an owned purchase.
 */
class BillingManager(
    private val repo: AppRepository,
    billingClientFactory: (PurchasesUpdatedListener) -> BillingClient,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectMutex = Mutex()

    private val billingClient: BillingClient = billingClientFactory(this)

    private val _offers = MutableStateFlow(PremiumCatalog())
    val offers: StateFlow<PremiumCatalog> = _offers.asStateFlow()

    private val _purchaseInFlight = MutableStateFlow(false)
    val purchaseInFlight: StateFlow<Boolean> = _purchaseInFlight.asStateFlow()

    private val _uiEvents = MutableSharedFlow<BillingUiEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEvents: SharedFlow<BillingUiEvent> = _uiEvents.asSharedFlow()

    private var fullDetails: ProductDetails? = null
    private var discountDetails: ProductDetails? = null
    private var discountWindowActive: Boolean = false
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
            syncEntitlementFromPlay()
        }
    }

    fun refreshOnForeground() {
        scope.launch {
            if (!billingClient.isReady) return@launch
            syncEntitlementFromPlay()
        }
    }

    /** Re-query Play product details when a premium paywall is shown. */
    fun refreshCatalogForPaywall() {
        scope.launch {
            if (!ensureConnected()) {
                _offers.value = PremiumCatalog(catalogState = BillingCatalogState.Unavailable)
                return@launch
            }
            refreshCatalog(paywallRefresh = true)
        }
    }

    /** Re-query Play after sign-in or account changes on the device. */
    fun refreshEntitlement() {
        scope.launch {
            if (!ensureConnected()) return@launch
            syncEntitlementFromPlay()
        }
    }

    /** The paywall sells the discount product while a 24-hour window is running. */
    fun setDiscountWindowActive(active: Boolean) {
        if (discountWindowActive == active) return
        discountWindowActive = active
        val state = _offers.value.catalogState
        val nothingLoaded = fullDetails == null && discountDetails == null
        if (state == BillingCatalogState.Loading) return
        if (nothingLoaded && state == BillingCatalogState.Idle) return
        publishCatalog()
    }

    fun launchPurchase(activity: Activity?, discounted: Boolean = false) {
        if (activity == null) {
            logBilling("purchase_no_activity")
            emitUiEvent(BillingUiEvent.LaunchFailed)
            return
        }
        scope.launch {
            if (!ensureConnected()) {
                emitUiEvent(BillingUiEvent.ServiceUnavailable)
                return@launch
            }
            if (!_offers.value.isReady) {
                emitUiEvent(BillingUiEvent.ProductsUnavailable)
                return@launch
            }
            refreshCatalog(paywallRefresh = true)
            val details = if (discounted) discountDetails else fullDetails
            val offerToken = details?.oneTimePurchaseOffer()?.offerToken
            if (details == null || offerToken.isNullOrBlank()) {
                logBilling("purchase_missing_offer")
                emitUiEvent(BillingUiEvent.ProductsUnavailable)
                return@launch
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
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

    private suspend fun refreshCatalog(paywallRefresh: Boolean = false) {
        val wasReady = _offers.value.catalogState == BillingCatalogState.Ready
        if (paywallRefresh || !wasReady) {
            _offers.value = _offers.value.copy(catalogState = BillingCatalogState.Loading)
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingConfig.premiumProductIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()

        val result = runCatching { billingClient.queryProductDetails(params) }.getOrNull()
        if (result == null ||
            result.billingResult.responseCode != BillingClient.BillingResponseCode.OK
        ) {
            logBilling(
                "query_product_details_failed code=${result?.billingResult?.responseCode}",
            )
            fullDetails = null
            discountDetails = null
            if (paywallRefresh || !wasReady) {
                _offers.value = PremiumCatalog(catalogState = BillingCatalogState.Unavailable)
            }
            return
        }
        val listed = result.productDetailsList.orEmpty()
        if (listed.isEmpty()) {
            logBilling("query_product_details_empty")
            fullDetails = null
            discountDetails = null
            if (paywallRefresh || !wasReady) {
                _offers.value = PremiumCatalog(catalogState = BillingCatalogState.Unavailable)
            }
            return
        }
        fullDetails = listed.firstOrNull { it.productId == BillingConfig.PRODUCT_ID }
        discountDetails = listed.firstOrNull { it.productId == BillingConfig.DISCOUNT_PRODUCT_ID }
        publishCatalog()
    }

    private fun publishCatalog() {
        val fullOffer = fullDetails?.oneTimePurchaseOffer()
        val discountOffer = discountDetails?.oneTimePurchaseOffer()
        _offers.value = presentPremiumCatalog(
            fullPrice = fullOffer?.formattedPrice,
            fullToken = fullOffer?.offerToken,
            discountPrice = discountOffer?.formattedPrice,
            discountToken = discountOffer?.offerToken,
            discountWindowActive = discountWindowActive,
        )
    }

    /**
     * @return `true`/`false` when entitlement was synced from Play; `null` when the
     * purchases query failed and local premium state was left unchanged.
     */
    private suspend fun syncEntitlementFromPlay(): Boolean? {
        return when (val outcome = queryOneTimePurchases()) {
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

    private suspend fun queryOneTimePurchases(): PurchasesQueryOutcome {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
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
