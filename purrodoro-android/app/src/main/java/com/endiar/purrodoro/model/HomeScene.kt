package com.endiar.purrodoro.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.endiar.purrodoro.R
import java.time.LocalTime

/**
 * Home hero backdrop theme. Each scene is one fixed environment with day/night
 * variants — same layout, different time-of-day lighting (see [resolveHomeHeroDrawable]).
 */
enum class HomeScene(
    val storageName: String,
    val isPremiumOnly: Boolean,
) {
    Room("room", isPremiumOnly = false),
    Cafe("cafe", isPremiumOnly = true),
    Library("library", isPremiumOnly = true),
    Park("park", isPremiumOnly = true),
    ;

    @DrawableRes
    fun dayDrawable(): Int = when (this) {
        Room -> R.drawable.cat_home_header_day
        Cafe -> R.drawable.cat_home_premium_cafe_day
        Library -> R.drawable.cat_home_scene_library_day
        Park -> R.drawable.cat_home_scene_park_day
    }

    @DrawableRes
    fun nightDrawable(): Int = when (this) {
        Room -> R.drawable.cat_home_header_night
        Cafe -> R.drawable.cat_home_premium_cafe_night
        Library -> R.drawable.cat_home_scene_library_night
        Park -> R.drawable.cat_home_scene_park_night
    }

    @StringRes
    fun titleRes(): Int = when (this) {
        Room -> R.string.home_scene_room_title
        Cafe -> R.string.home_scene_cafe_title
        Library -> R.string.home_scene_library_title
        Park -> R.string.home_scene_park_title
    }

    @StringRes
    fun subtitleRes(): Int = when (this) {
        Room -> R.string.home_scene_room_subtitle
        Cafe -> R.string.home_scene_cafe_subtitle
        Library -> R.string.home_scene_library_subtitle
        Park -> R.string.home_scene_park_subtitle
    }

    companion object {
        fun fromStorageName(name: String?): HomeScene =
            entries.firstOrNull { it.storageName == name } ?: Room
    }
}

/** Night art when the app is in dark mode or local time is outside 6:00–17:59. */
fun isHomeHeroNightTime(isDarkTheme: Boolean, time: LocalTime): Boolean =
    isDarkTheme || time.hour !in 6..17

@DrawableRes
fun resolveHomeHeroDrawable(scene: HomeScene, isDarkTheme: Boolean, time: LocalTime): Int =
    if (isHomeHeroNightTime(isDarkTheme, time)) scene.nightDrawable() else scene.dayDrawable()

/** Free users always render [HomeScene.Room]; selection is kept for a future upgrade. */
fun effectiveHomeScene(selected: HomeScene, isPremium: Boolean): HomeScene =
    if (!isPremium && selected.isPremiumOnly) HomeScene.Room else selected

fun canSelectHomeScene(scene: HomeScene, isPremium: Boolean): Boolean =
    !scene.isPremiumOnly || isPremium
