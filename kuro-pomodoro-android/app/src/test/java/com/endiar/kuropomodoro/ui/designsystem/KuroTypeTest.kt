package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import org.junit.Test
import org.junit.Assert.assertEquals

class KuroTypeTest {
    @Test fun displayTimerSpec() {
        assertEquals(48.sp, KuroType.displayTimer.fontSize)
        assertEquals(FontWeight.Bold, KuroType.displayTimer.fontWeight)
    }
    @Test fun headingScreenSpec() {
        assertEquals(28.sp, KuroType.headingScreen.fontSize)
        assertEquals(FontWeight.Bold, KuroType.headingScreen.fontWeight)
    }
    @Test fun labelMetaSpec() {
        assertEquals(11.sp, KuroType.labelMeta.fontSize)
        assertEquals(FontWeight.SemiBold, KuroType.labelMeta.fontWeight)
    }
}
