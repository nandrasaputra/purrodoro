package com.endiar.purrodoro.billing

import com.endiar.purrodoro.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PremiumCatalogTest {

    @Test
    fun `ready catalog exposes the one-time purchase buttons`() {
        val catalog = PremiumCatalog(
            catalogState = BillingCatalogState.Ready,
            formattedPrice = "Rp 79.000",
        )
        assertEquals(R.string.premium_sheet_cta, catalog.ctaResId(forSheet = true))
        assertEquals(R.string.premium_screen_cta_subscribe, catalog.ctaResId(forSheet = false))
    }

    @Test
    fun `catalog without a price is not ready`() {
        val catalog = PremiumCatalog(catalogState = BillingCatalogState.Ready, formattedPrice = null)
        assertFalse(catalog.isReady)
    }
}
