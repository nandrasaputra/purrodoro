package com.endiar.purrodoro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.endiar.purrodoro.model.AppearanceMode

private val DarkColorScheme = darkColorScheme(
    primary = DockCoral,
    onPrimary = Cocoa,
    secondary = Lavender,
    tertiary = Mint,
    background = NightDeep,
    onBackground = NightMuted,
    surface = NightCard,
    onSurface = NightMuted,
    surfaceVariant = NightCardActive,
    onSurfaceVariant = NightFaint,
    outline = NightBorder,
)

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = CardWhite,
    secondary = Peach,
    tertiary = Mint,
    background = Cream,
    onBackground = CocoaSoft,
    surface = CardWhite,
    onSurface = CocoaSoft,
    surfaceVariant = CreamCardActive,
    onSurfaceVariant = CocoaFaint,
    outline = CreamBorder,
)

/**
 * Wraps [content] in a [MaterialTheme] using the scheme implied by
 * [appearanceMode]. `Auto` defers to [isSystemInDarkTheme].
 */
@Composable
fun PurrodoroTheme(
    appearanceMode: AppearanceMode = AppearanceMode.Light,
    systemInDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val isDark = appearanceMode.isDark(systemInDark)
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        // Force Nunito as the default family for every Text, so styles that
        // don't name a family (e.g. KuroType.*) render in the brand font
        // instead of falling back to the platform default.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = NunitoFamily),
            content = content,
        )
    }
}
