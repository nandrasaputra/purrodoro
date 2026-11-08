package com.endiar.purrodoro.billing

import com.android.billingclient.api.ProductDetails

internal fun ProductDetails.oneTimePurchaseOffer(): ProductDetails.OneTimePurchaseOfferDetails? {
    val listed = oneTimePurchaseOfferDetailsList.orEmpty()
        .firstOrNull { !it.offerToken.isNullOrBlank() }
    return listed ?: oneTimePurchaseOfferDetails?.takeIf { !it.offerToken.isNullOrBlank() }
}

/** Ready only when Play returned both a display price and a purchase token. */
internal fun premiumCatalog(formattedPrice: String?, offerToken: String?): PremiumCatalog {
    val price = formattedPrice?.trim()?.takeIf { it.isNotEmpty() }
    val token = offerToken?.trim()?.takeIf { it.isNotEmpty() }
    return if (price != null && token != null) {
        PremiumCatalog(catalogState = BillingCatalogState.Ready, formattedPrice = price)
    } else {
        PremiumCatalog(catalogState = BillingCatalogState.Unavailable)
    }
}

internal fun ProductDetails.toPremiumCatalog(): PremiumCatalog {
    val offer = oneTimePurchaseOffer()
    return premiumCatalog(offer?.formattedPrice, offer?.offerToken)
}

/**
 * Picks the price the paywall sells. An active window sells the discount only
 * when that product loaded. Otherwise the paywall stays on the full price, and
 * [PremiumCatalog.discountPriceReady] still reports whether a leave can start the clock.
 */
internal fun presentPremiumCatalog(
    fullPrice: String?,
    fullToken: String?,
    discountPrice: String?,
    discountToken: String?,
    discountWindowActive: Boolean,
): PremiumCatalog {
    val full = premiumCatalog(fullPrice, fullToken)
    val discount = premiumCatalog(discountPrice, discountToken)
    if (discountWindowActive) {
        if (!discount.isReady) {
            return PremiumCatalog(catalogState = BillingCatalogState.Unavailable)
        }
        return discount.copy(
            fullPrice = full.formattedPrice,
            discountPrice = discount.formattedPrice,
            compareAtPrice = full.formattedPrice,
            discountPriceReady = true,
            discountActive = true,
        )
    }
    if (!full.isReady) {
        return PremiumCatalog(
            catalogState = BillingCatalogState.Unavailable,
            discountPrice = discount.formattedPrice,
            discountPriceReady = discount.isReady,
        )
    }
    return full.copy(
        fullPrice = full.formattedPrice,
        discountPrice = discount.formattedPrice,
        discountPriceReady = discount.isReady,
    )
}
