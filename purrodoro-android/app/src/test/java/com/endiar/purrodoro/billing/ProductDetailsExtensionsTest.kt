package com.endiar.purrodoro.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDetailsExtensionsTest {

    @Test
    fun `catalog is ready when price and offer token are present`() {
        val catalog = premiumCatalog(formattedPrice = "Rp 79.000", offerToken = "token")
        assertEquals(BillingCatalogState.Ready, catalog.catalogState)
        assertEquals("Rp 79.000", catalog.formattedPrice)
    }

    @Test
    fun `catalog is unavailable without a purchase token`() {
        val catalog = premiumCatalog(formattedPrice = "Rp 79.000", offerToken = " ")
        assertEquals(BillingCatalogState.Unavailable, catalog.catalogState)
        assertNull(catalog.formattedPrice)
    }

    @Test
    fun `catalog is unavailable without a price`() {
        val catalog = premiumCatalog(formattedPrice = null, offerToken = "token")
        assertEquals(BillingCatalogState.Unavailable, catalog.catalogState)
    }

    @Test
    fun `full price stays on screen until the discount window is active`() {
        val catalog = presentPremiumCatalog(
            fullPrice = "$4.99",
            fullToken = "full",
            discountPrice = "$0.99",
            discountToken = "sale",
            discountWindowActive = false,
        )
        assertEquals(BillingCatalogState.Ready, catalog.catalogState)
        assertEquals("$4.99", catalog.formattedPrice)
        assertNull(catalog.compareAtPrice)
        assertFalse(catalog.discountActive)
        assertTrue(catalog.discountPriceReady)
    }

    @Test
    fun `active window shows the discount and keeps the full price for comparison`() {
        val catalog = presentPremiumCatalog(
            fullPrice = "$4.99",
            fullToken = "full",
            discountPrice = "$0.99",
            discountToken = "sale",
            discountWindowActive = true,
        )
        assertEquals("$0.99", catalog.formattedPrice)
        assertEquals("$4.99", catalog.compareAtPrice)
        assertTrue(catalog.discountActive)
        assertTrue(catalog.isReady)
    }

    @Test
    fun `active window does not sell the full price when the discount is missing`() {
        val catalog = presentPremiumCatalog(
            fullPrice = "$4.99",
            fullToken = "full",
            discountPrice = null,
            discountToken = null,
            discountWindowActive = true,
        )
        assertEquals(BillingCatalogState.Unavailable, catalog.catalogState)
        assertFalse(catalog.discountActive)
        assertFalse(catalog.discountPriceReady)
    }
}
