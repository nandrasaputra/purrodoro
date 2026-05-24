package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.Assert.assertEquals

class KuroSpacingTest {
    @Test fun spacingValues() {
        assertEquals(4.dp,  KuroSpacing.xs)
        assertEquals(8.dp,  KuroSpacing.sm)
        assertEquals(16.dp, KuroSpacing.md)
        assertEquals(20.dp, KuroSpacing.lg)
        assertEquals(24.dp, KuroSpacing.xl)
        assertEquals(32.dp, KuroSpacing.xxl)
        assertEquals(40.dp, KuroSpacing.xxxl)
    }
    @Test fun radiusValues() {
        assertEquals(12.dp, KuroSpacing.radiusSm)
        assertEquals(20.dp, KuroSpacing.radiusMd)
        assertEquals(28.dp, KuroSpacing.radiusLg)
        assertEquals(34.dp, KuroSpacing.radiusPill)
    }
}
