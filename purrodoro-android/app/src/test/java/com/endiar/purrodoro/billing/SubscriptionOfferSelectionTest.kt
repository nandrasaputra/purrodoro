package com.endiar.purrodoro.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionOfferSelectionTest {

    @Test
    fun prefersBasePlanOfferId() {
        val selected = selectOfferIdForBasePlan(
            offers = listOf(
                "monthly" to "intro-monthly",
                "monthly" to "monthly",
            ),
            basePlanId = "monthly",
        )
        assertEquals("monthly", selected)
    }

    @Test
    fun fallsBackToStableMinOfferId() {
        val selected = selectOfferIdForBasePlan(
            offers = listOf(
                "yearly" to "yearly-trial",
                "yearly" to "yearly-promo",
            ),
            basePlanId = "yearly",
        )
        assertEquals("yearly-promo", selected)
    }

    @Test
    fun returnsNullWhenBasePlanMissing() {
        assertNull(
            selectOfferIdForBasePlan(
                offers = listOf("monthly" to "monthly"),
                basePlanId = "yearly",
            ),
        )
    }

    @Test
    fun prefersBlankOfferIdForDefaultBasePlan() {
        val selected = selectOfferIdForBasePlan(
            offers = listOf(
                "monthly" to "intro-monthly",
                "monthly" to "",
            ),
            basePlanId = "monthly",
        )
        assertEquals("", selected)
    }

    @Test
    fun subscriptionOfferIdsMatch_treatsNullAsBlank() {
        assertTrue(subscriptionOfferIdsMatch(offerId = null, selectedOfferId = ""))
        assertTrue(subscriptionOfferIdsMatch(offerId = "monthly", selectedOfferId = "monthly"))
    }
}
