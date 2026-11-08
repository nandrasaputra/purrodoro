package com.endiar.purrodoro.billing

import androidx.annotation.StringRes
import com.endiar.purrodoro.R

/** Play prices for the full-price and 24-hour discount products. */
data class PremiumCatalog(
    val catalogState: BillingCatalogState = BillingCatalogState.Idle,
    val formattedPrice: String? = null,
    val fullPrice: String? = null,
    val discountPrice: String? = null,
    val compareAtPrice: String? = null,
    val discountPriceReady: Boolean = false,
    val discountActive: Boolean = false,
) {
    val isReady: Boolean
        get() = catalogState == BillingCatalogState.Ready && !formattedPrice.isNullOrBlank()

    @StringRes
    fun ctaResId(forSheet: Boolean): Int =
        if (forSheet) R.string.premium_sheet_cta else R.string.premium_screen_cta_subscribe
}
