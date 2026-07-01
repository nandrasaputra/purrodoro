package com.endiar.purrodoro.billing

import com.android.billingclient.api.ProductDetails

/**
 * Picks the default (non-promotional) subscription offer for [basePlanId].
 *
 * Prefers `offerId` blank or equal to [basePlanId]; otherwise lexicographically
 * smallest, excluding [BillingConfig.OFFER_YEARLY_TRIAL].
 */
internal fun selectDefaultOfferIdForBasePlan(
    offers: List<Pair<String, String>>,
    basePlanId: String,
): String? {
    val matching = offers.filter { it.first == basePlanId }.map { it.second }
    if (matching.isEmpty()) return null
    return matching.firstOrNull { it.isBlank() || it == basePlanId }
        ?: matching
            .filter { it != BillingConfig.OFFER_YEARLY_TRIAL }
            .minOrNull()
        ?: matching.minOrNull()
}

internal fun subscriptionOfferIdsMatch(offerId: String?, selectedOfferId: String): Boolean =
    offerId.orEmpty() == selectedOfferId

internal fun ProductDetails.selectDefaultOfferForBasePlan(
    basePlanId: String,
): ProductDetails.SubscriptionOfferDetails? {
    val matching = subscriptionOfferDetails?.filter { it.basePlanId == basePlanId }.orEmpty()
    if (matching.isEmpty()) return null
    val offerId = selectDefaultOfferIdForBasePlan(
        matching.map { it.basePlanId to it.offerId.orEmpty() },
        basePlanId,
    ) ?: return null
    return matching.firstOrNull { subscriptionOfferIdsMatch(it.offerId, offerId) }
}

internal fun ProductDetails.selectTrialOfferForYearly(): ProductDetails.SubscriptionOfferDetails? =
    subscriptionOfferDetails
        ?.firstOrNull {
            it.basePlanId == BillingConfig.BASE_PLAN_YEARLY &&
                it.offerId == BillingConfig.OFFER_YEARLY_TRIAL
        }

internal fun ProductDetails.selectPurchaseOfferForPlan(
    plan: PremiumPlan,
): ProductDetails.SubscriptionOfferDetails? = when (plan) {
    PremiumPlan.Monthly -> selectDefaultOfferForBasePlan(BillingConfig.BASE_PLAN_MONTHLY)
    PremiumPlan.Yearly -> selectTrialOfferForYearly()
        ?: selectDefaultOfferForBasePlan(BillingConfig.BASE_PLAN_YEARLY)
}

internal data class PricingPhaseSnapshot(
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val billingPeriod: String = "",
    val billingCycleCount: Int = 1,
)

internal fun ProductDetails.SubscriptionOfferDetails.pricingPhaseSnapshots(): List<PricingPhaseSnapshot> =
    pricingPhases
        ?.pricingPhaseList
        .orEmpty()
        .map { phase ->
            PricingPhaseSnapshot(
                formattedPrice = phase.formattedPrice,
                priceAmountMicros = phase.priceAmountMicros,
                billingPeriod = phase.billingPeriod,
                billingCycleCount = phase.billingCycleCount,
            )
        }

/** Free-trial offers: phase 0 is $0; recurring price comes from the last phase. */
internal fun formatPlanOfferFromPhases(
    phases: List<PricingPhaseSnapshot>,
    isYearly: Boolean,
): PlanOfferDetails {
    if (phases.isEmpty()) return PlanOfferDetails(hasOffer = false)
    val recurringPhase = phases.last()
    val isFreeTrial = phases.size > 1 && phases.first().priceAmountMicros == 0L
    val trialPeriod = if (isFreeTrial) {
        val trialPhase = phases.first()
        parseBillingPeriod(trialPhase.billingPeriod, trialPhase.billingCycleCount)
    } else {
        null
    }
    return if (isFreeTrial && isYearly) {
        PlanOfferDetails(
            priceLine = null,
            secondaryPriceLine = recurringPhase.formattedPrice,
            hasTrial = true,
            trialPeriod = trialPeriod,
            hasOffer = true,
        )
    } else {
        PlanOfferDetails(
            priceLine = recurringPhase.formattedPrice,
            hasTrial = false,
            trialPeriod = null,
            hasOffer = true,
        )
    }
}

internal fun computeSavingsPercent(monthlyMicros: Long, yearlyMicros: Long): Int? {
    if (monthlyMicros <= 0L || yearlyMicros <= 0L) return null
    val annualizedMonthly = monthlyMicros * 12
    if (yearlyMicros >= annualizedMonthly) return null
    return ((annualizedMonthly - yearlyMicros) * 100 / annualizedMonthly).toInt()
}

internal fun ProductDetails.toSubscriptionOffers(): SubscriptionOffers {
    val monthlyOffer = selectDefaultOfferForBasePlan(BillingConfig.BASE_PLAN_MONTHLY)
    val yearlyTrialOffer = selectTrialOfferForYearly()
    val yearlyDefaultOffer = selectDefaultOfferForBasePlan(BillingConfig.BASE_PLAN_YEARLY)
    val yearlyDisplayOffer = yearlyTrialOffer ?: yearlyDefaultOffer

    val monthlyPhases = monthlyOffer?.pricingPhaseSnapshots().orEmpty()
    val yearlyPhases = yearlyDisplayOffer?.pricingPhaseSnapshots().orEmpty()

    val monthlyDetails = formatPlanOfferFromPhases(monthlyPhases, isYearly = false)
    val yearlyDetails = formatPlanOfferFromPhases(yearlyPhases, isYearly = true)

    val monthlyMicros = monthlyPhases.lastOrNull()?.priceAmountMicros
    val yearlyRecurringMicros = yearlyPhases.lastOrNull()?.priceAmountMicros
    val savings = if (monthlyMicros != null && yearlyRecurringMicros != null) {
        computeSavingsPercent(monthlyMicros, yearlyRecurringMicros)
    } else {
        null
    }

    val hasAnyOffer = monthlyDetails.hasOffer || yearlyDetails.hasOffer
    return SubscriptionOffers(
        catalogState = if (hasAnyOffer) BillingCatalogState.Ready else BillingCatalogState.Unavailable,
        monthly = monthlyDetails,
        yearly = yearlyDetails,
        savingsPercent = savings,
    )
}
