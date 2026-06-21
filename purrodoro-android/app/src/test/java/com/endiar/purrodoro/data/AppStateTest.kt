package com.endiar.purrodoro.data

import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.HomeScene
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateTest {

    @Test
    fun loadingPlaceholder_matchesComposeFallbackDefaults() {
        val placeholder = AppState.loadingPlaceholder()
        assertEquals(GlassReadabilityMode.Adaptive, placeholder.glassReadabilityMode)
        assertEquals(AppearanceMode.Light, placeholder.appearanceMode)
        assertEquals(AppLanguage.System, placeholder.language)
        assertEquals(HomeScene.Room, placeholder.homeScene)
    }
}
