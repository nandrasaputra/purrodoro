package com.endiar.kuropomodoro.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Kuro-specific surface tokens that are not covered by Material's [ColorScheme].
 *
 * The flat (non-Focus) tabs read everything through this object; the Focus
 * tab still uses [com.endiar.kuropomodoro.ui.components.glassTokens] for
 * its over-photo glass cards.
 */
data class KuroSurfaceTokens(
    val pageBackground: Color,
    val cardFill: Color,
    val activeCardFill: Color,
    val cardBorder: Color,
    val divider: Color,
    val borderSoft: Color,
    val segmentedTrack: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentOn: Color,
    val lockedFill: Color,
    val lockedBorder: Color,
    val lockedText: Color,
)

private val DarkTokens = KuroSurfaceTokens(
    pageBackground = NightDeep,
    cardFill = NightCard,
    activeCardFill = NightCardActive,
    cardBorder = NightBorder,
    divider = NightDivider,
    borderSoft = Color.White.copy(alpha = 0.10f),
    segmentedTrack = Color.White.copy(alpha = 0.06f),
    textPrimary = Color.White,
    textSecondary = NightMuted,
    textMuted = NightFaint,
    textFaint = NightFainter,
    accent = DockCoral,
    accentSoft = DockCoral.copy(alpha = 0.18f),
    accentOn = Color.White,
    lockedFill = LockedPurple.copy(alpha = 0.25f),
    lockedBorder = LockedPurple.copy(alpha = 0.45f),
    lockedText = LockedPurpleText,
)

private val LightTokens = KuroSurfaceTokens(
    pageBackground = Cream,
    cardFill = CardWhite,
    activeCardFill = CreamCardActive,
    cardBorder = CreamBorder,
    divider = CreamDivider,
    borderSoft = LightBorderSoft,
    segmentedTrack = LightBorderSoft,
    textPrimary = Cocoa,
    textSecondary = CocoaSoft,
    textMuted = CocoaFaint,
    textFaint = CocoaFainter,
    accent = Coral,
    accentSoft = Blush,
    accentOn = Color.White,
    lockedFill = LockedPurpleLight.copy(alpha = 0.15f),
    lockedBorder = LockedPurpleLight,
    lockedText = LockedPurpleTextDark,
)

/**
 * Resolves the kuro tokens for the current theme by inferring light vs dark
 * from the scheme's background luminance.
 */
val ColorScheme.kuroTokens: KuroSurfaceTokens
    get() = if (background.luminance() > 0.5f) LightTokens else DarkTokens

/** Convenience accessor for composables. */
object KuroTokens {
    val current: KuroSurfaceTokens
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.kuroTokens
}

internal fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
