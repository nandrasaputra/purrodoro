package com.endiar.purrodoro.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDetailsExtensionsTest {

    @Test
    fun `formatPlanOfferFromPhases captures trial period from Play billing period`() {
        val phases = listOf(
            PricingPhaseSnapshot(
                formattedPrice = "$0.00",
                priceAmountMicros = 0L,
                billingPeriod = "P1W",
                billingCycleCount = 1,
            ),
            PricingPhaseSnapshot(
                formattedPrice = "$29.99",
                priceAmountMicros = 29_990_000L,
                billingPeriod = "P1Y",
            ),
        )

        val details = formatPlanOfferFromPhases(phases, isYearly = true)

        assertTrue(details.hasTrial)
        assertEquals(TrialPeriod(count = 1, unit = TrialPeriodUnit.Week), details.trialPeriod)
        assertEquals("$29.99", details.secondaryPriceLine)
        assertNull(details.priceLine)
    }

    @Test
    fun `formatPlanOfferFromPhases leaves trial period null when not yearly trial`() {
        val phases = listOf(
            PricingPhaseSnapshot(
                formattedPrice = "$4.99",
                priceAmountMicros = 4_990_000L,
                billingPeriod = "P1M",
            ),
        )

        val details = formatPlanOfferFromPhases(phases, isYearly = false)

        assertEquals("$4.99", details.priceLine)
        assertNull(details.trialPeriod)
    }
}
