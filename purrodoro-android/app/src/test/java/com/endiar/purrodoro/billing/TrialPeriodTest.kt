package com.endiar.purrodoro.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrialPeriodTest {

    @Test
    fun `parseBillingPeriod reads week duration`() {
        assertEquals(
            TrialPeriod(count = 1, unit = TrialPeriodUnit.Week),
            parseBillingPeriod("P1W"),
        )
    }

    @Test
    fun `parseBillingPeriod reads day duration`() {
        assertEquals(
            TrialPeriod(count = 7, unit = TrialPeriodUnit.Day),
            parseBillingPeriod("P7D"),
        )
    }

    @Test
    fun `parseBillingPeriod multiplies by billing cycle count`() {
        assertEquals(
            TrialPeriod(count = 2, unit = TrialPeriodUnit.Week),
            parseBillingPeriod("P1W", billingCycleCount = 2),
        )
    }

    @Test
    fun `parseBillingPeriod rejects blank and invalid`() {
        assertNull(parseBillingPeriod(""))
        assertNull(parseBillingPeriod("not-iso"))
    }
}
