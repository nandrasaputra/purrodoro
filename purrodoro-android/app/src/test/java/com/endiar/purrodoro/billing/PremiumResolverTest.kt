package com.endiar.purrodoro.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumResolverTest {

    @Test
    fun overrideEnabled_usesOverrideActive() {
        assertTrue(PremiumResolver.resolve(overrideEnabled = true, overrideActive = true, playPremium = false))
        assertFalse(PremiumResolver.resolve(overrideEnabled = true, overrideActive = false, playPremium = true))
    }

    @Test
    fun overrideDisabled_usesPlayPremium() {
        assertTrue(PremiumResolver.resolve(overrideEnabled = false, overrideActive = false, playPremium = true))
        assertFalse(PremiumResolver.resolve(overrideEnabled = false, overrideActive = true, playPremium = false))
    }
}
