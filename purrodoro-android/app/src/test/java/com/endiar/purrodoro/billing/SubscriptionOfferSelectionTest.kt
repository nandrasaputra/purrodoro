package com.endiar.purrodoro.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionOfferSelectionTest {

    @Test
    fun prefersBasePlanOfferId() {
        val selected = selectDefaultOfferIdForBasePlan(
            offers = listOf(
                BillingConfig.BASE_PLAN_MONTHLY to "intro-monthly",
                BillingConfig.BASE_PLAN_MONTHLY to BillingConfig.BASE_PLAN_MONTHLY,
            ),
            basePlanId = BillingConfig.BASE_PLAN_MONTHLY,
        )
        assertEquals(BillingConfig.BASE_PLAN_MONTHLY, selected)
    }

    @Test
    fun fallsBackToStableMinOfferIdExcludingTrial() {
        val selected = selectDefaultOfferIdForBasePlan(
            offers = listOf(
                BillingConfig.BASE_PLAN_YEARLY to BillingConfig.OFFER_YEARLY_TRIAL,
                BillingConfig.BASE_PLAN_YEARLY to "yearly-promo",
            ),
            basePlanId = BillingConfig.BASE_PLAN_YEARLY,
        )
        assertEquals("yearly-promo", selected)
    }

    @Test
    fun returnsNullWhenBasePlanMissing() {
        assertNull(
            selectDefaultOfferIdForBasePlan(
                offers = listOf(BillingConfig.BASE_PLAN_MONTHLY to BillingConfig.BASE_PLAN_MONTHLY),
                basePlanId = BillingConfig.BASE_PLAN_YEARLY,
            ),
        )
    }

    @Test
    fun prefersBlankOfferIdForDefaultBasePlan() {
        val selected = selectDefaultOfferIdForBasePlan(
            offers = listOf(
                BillingConfig.BASE_PLAN_MONTHLY to "intro-monthly",
                BillingConfig.BASE_PLAN_MONTHLY to "",
            ),
            basePlanId = BillingConfig.BASE_PLAN_MONTHLY,
        )
        assertEquals("", selected)
    }

    @Test
    fun subscriptionOfferIdsMatch_treatsNullAsBlank() {
        assertTrue(subscriptionOfferIdsMatch(offerId = null, selectedOfferId = ""))
        assertTrue(
            subscriptionOfferIdsMatch(
                offerId = BillingConfig.BASE_PLAN_MONTHLY,
                selectedOfferId = BillingConfig.BASE_PLAN_MONTHLY,
            ),
        )
    }
}
