package com.endiar.purrodoro.billing

import com.android.billingclient.api.ProductDetails

/**
 * Picks a stable subscription offer for [basePlanId].
 *
 * When Play returns multiple offers (e.g. base + free-trial), prefer the
 * default base-plan offer (`offerId` blank or equal to [basePlanId]); otherwise
 * fall back to the lexicographically smallest `offerId` so selection is
 * deterministic across devices.
 */
internal fun selectOfferIdForBasePlan(
    offers: List<Pair<String, String>>,
    basePlanId: String,
): String? {
    val matching = offers.filter { it.first == basePlanId }.map { it.second }
    if (matching.isEmpty()) return null
    return matching.firstOrNull { it.isBlank() || it == basePlanId }
        ?: matching.minOrNull()
}

internal fun ProductDetails.selectOfferForBasePlan(
    basePlanId: String,
): ProductDetails.SubscriptionOfferDetails? {
    val matching = subscriptionOfferDetails?.filter { it.basePlanId == basePlanId }.orEmpty()
    if (matching.isEmpty()) return null
    val offerId = selectOfferIdForBasePlan(
        matching.map { it.basePlanId to it.offerId.orEmpty() },
        basePlanId,
    ) ?: return null
    return matching.firstOrNull { subscriptionOfferIdsMatch(it.offerId, offerId) }
}

internal fun subscriptionOfferIdsMatch(offerId: String?, selectedOfferId: String): Boolean =
    offerId.orEmpty() == selectedOfferId

internal fun ProductDetails.formattedPriceForBasePlan(basePlanId: String): String? =
    selectOfferForBasePlan(basePlanId)
        ?.pricingPhases
        ?.pricingPhaseList
        ?.firstOrNull()
        ?.formattedPrice
