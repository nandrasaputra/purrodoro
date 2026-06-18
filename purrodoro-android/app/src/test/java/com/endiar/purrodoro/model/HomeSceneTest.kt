package com.endiar.purrodoro.model

import com.endiar.purrodoro.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class HomeSceneTest {

    @Test
    fun resolveHomeHeroDrawable_roomDay() {
        assertEquals(
            R.drawable.cat_home_header_day,
            resolveHomeHeroDrawable(HomeScene.Room, isDarkTheme = false, time = LocalTime.of(10, 0)),
        )
    }

    @Test
    fun resolveHomeHeroDrawable_cafeNight() {
        assertEquals(
            R.drawable.cat_home_premium_cafe_night,
            resolveHomeHeroDrawable(HomeScene.Cafe, isDarkTheme = true, time = LocalTime.of(10, 0)),
        )
    }

    @Test
    fun effectiveHomeScene_freeUserWithPremiumSelectionFallsBackToRoom() {
        assertEquals(
            HomeScene.Room,
            effectiveHomeScene(HomeScene.Cafe, isPremium = false),
        )
    }

    @Test
    fun canSelectHomeScene() {
        assertTrue(canSelectHomeScene(HomeScene.Room, isPremium = false))
        assertFalse(canSelectHomeScene(HomeScene.Cafe, isPremium = false))
        assertTrue(canSelectHomeScene(HomeScene.Cafe, isPremium = true))
    }

    @Test
    fun resolveHomeHeroDrawable_libraryDay() {
        assertEquals(
            R.drawable.cat_home_scene_library_day,
            resolveHomeHeroDrawable(HomeScene.Library, isDarkTheme = false, time = LocalTime.of(10, 0)),
        )
    }

    @Test
    fun resolveHomeHeroDrawable_parkNight() {
        assertEquals(
            R.drawable.cat_home_scene_park_night,
            resolveHomeHeroDrawable(HomeScene.Park, isDarkTheme = true, time = LocalTime.of(10, 0)),
        )
    }

    @Test
    fun premiumScenesArePremiumOnly() {
        assertTrue(HomeScene.Cafe.isPremiumOnly)
        assertTrue(HomeScene.Library.isPremiumOnly)
        assertTrue(HomeScene.Park.isPremiumOnly)
        assertFalse(HomeScene.Room.isPremiumOnly)
    }
}
