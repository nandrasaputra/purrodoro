package com.endiar.kuropomodoro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Peach,
    secondary = Lavender,
    tertiary = Mint,
    background = Night,
    surface = NightCard,
    onPrimary = Cocoa,
    onSecondary = Cream,
    onTertiary = Cocoa,
    onBackground = Cream,
    onSurface = Cream
)

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    secondary = Peach,
    tertiary = Mint,
    background = Cream,
    surface = CardWhite,
    onPrimary = CardWhite,
    onSecondary = Cocoa,
    onTertiary = Cocoa,
    onBackground = Cocoa,
    onSurface = Cocoa
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun KuroPomodoroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
