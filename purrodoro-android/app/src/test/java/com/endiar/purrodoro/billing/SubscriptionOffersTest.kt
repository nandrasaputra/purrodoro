package com.endiar.purrodoro.billing

import com.endiar.purrodoro.R
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionOffersTest {

    @Test
    fun `ctaResId uses trial copy for yearly with trial`() {
        val offers = SubscriptionOffers(
            catalogState = BillingCatalogState.Ready,
            yearly = PlanOfferDetails(hasTrial = true, hasOffer = true),
        )
        assertEquals(
            R.string.premium_cta_start_trial,
            offers.ctaResId(PremiumPlan.Yearly, forSheet = true),
        )
    }

    @Test
    fun `ctaResId uses sheet copy for monthly`() {
        val offers = SubscriptionOffers(catalogState = BillingCatalogState.Ready)
        assertEquals(
            R.string.premium_sheet_cta,
            offers.ctaResId(PremiumPlan.Monthly, forSheet = true),
        )
    }

    @Test
    fun `ctaResId uses screen copy for yearly without trial`() {
        val offers = SubscriptionOffers(catalogState = BillingCatalogState.Ready)
        assertEquals(
            R.string.premium_screen_cta_subscribe,
            offers.ctaResId(PremiumPlan.Yearly, forSheet = false),
        )
    }
}
