package com.endiar.purrodoro.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

/**
 * Centralised Glance color + text styles for the Purrodoro widgets.
 *
 * Glance's resource-Int `ColorProvider` is library-internal and not safe to
 * use from app code (it can resolve in the wrong process when the launcher
 * hosts the widget — see Glance's color guidance). We instead pin literal
 * day/night values that mirror the `@color/widget_*` resources, keeping the
 * widget look identical across light and dark while staying on the public
 * Glance API.
 */
object WidgetTheme {

    val cardFill = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A1428))
    val cardBorder = ColorProvider(day = Color(0xFFEBDFCC), night = Color(0xFF2B2440))
    val pageBg = ColorProvider(day = Color(0xFFFAF5F2), night = Color(0xFF0A0816))
    val textPrimary = ColorProvider(day = Color(0xFF5F4638), night = Color(0xFFFFFFFF))
    val textSecondary = ColorProvider(day = Color(0xFF8B6B5A), night = Color(0xFFB8B0C8))
    val textMuted = ColorProvider(day = Color(0xFFA89486), night = Color(0xFF7D768B))
    val accent = ColorProvider(day = Color(0xFFFF7F73), night = Color(0xFFFF8B6F))
    val buttonFill = ColorProvider(day = Color(0xFFFF7F73), night = Color(0xFFFF8B6F))
    val buttonText = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A1428))

    /** Soft coral tint used for ghost CTAs and accented chips. */
    val accentSoft = ColorProvider(day = Color(0xFFFFF1EE), night = Color(0xFF2B2440))

    /**
     * Number overlaid on the painterly flame. Fixed across themes (not day/night)
     * because the flame art is always light-centered, so a deep warm coral keeps
     * the digit legible on the glow in both light and dark mode.
     */
    val flameOverlay = ColorProvider(day = Color(0xFF8A3D2F), night = Color(0xFF8A3D2F))

    /** Neutral chip / tile fill on the cream card. */
    val chipFill = ColorProvider(day = Color(0xFFF6EFE7), night = Color(0xFF241D33))

    val title: TextStyle = TextStyle(
        color = textPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )

    val titleLg: TextStyle = TextStyle(
        color = textPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
    )

    val metricNumber: TextStyle = TextStyle(
        color = accent,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
    )

    val metricUnit: TextStyle = TextStyle(
        color = textPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )

    val body: TextStyle = TextStyle(
        color = textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
    )

    val muted: TextStyle = TextStyle(
        color = textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )

    val buttonLabel: TextStyle = TextStyle(
        color = buttonText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )

    /** CTA label for the outlined / ghost button (coral on a soft tint). */
    val ghostLabel: TextStyle = TextStyle(
        color = accent,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )

    /** Prominent one-line nudge headline on the 4x4 widgets. */
    val nudgeTitle: TextStyle = TextStyle(
        color = textPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    )

    /**
     * Headline overlaid on the cozy scene illustration. Fixed deep brown across
     * themes (the art is always a warm light scene), so the digit/title stays
     * legible whether the launcher is in light or dark mode.
     */
    val cozyTitle: TextStyle = TextStyle(
        color = ColorProvider(day = Color(0xFF4A3525), night = Color(0xFF4A3525)),
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
    )

    /**
     * Centered nudge line overlaid on the calm scene illustration. Fixed deep
     * brown across themes so it stays legible on the warm light art.
     */
    val calmTitle: TextStyle = TextStyle(
        color = ColorProvider(day = Color(0xFF4A3525), night = Color(0xFF4A3525)),
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )

    private val onImageDark = ColorProvider(day = Color(0xFF4A3525), night = Color(0xFF4A3525))
    private val onImageSoft = ColorProvider(day = Color(0xFF6B5244), night = Color(0xFF6B5244))

    /** Greeting headline overlaid on the dashboard sky art (fixed dark brown). */
    val onImageTitle: TextStyle = TextStyle(
        color = onImageDark,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
    )

    /** Subtitle / nudge line over the dashboard sky art. */
    val onImageSubtitle: TextStyle = TextStyle(
        color = onImageSoft,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )

    /** Focus / streak stat line over the dashboard sky art. */
    val onImageStat: TextStyle = TextStyle(
        color = onImageSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )

    /** Big white streak number composited over the coral flame scene. */
    val streakSceneNumber: TextStyle = TextStyle(
        color = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFFFFFFFF)),
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
    )

    /** A stat tile's big number (e.g. focus count, streak). */
    val statNumber: TextStyle = TextStyle(
        color = textPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
    )

    /** A stat tile's small caption under the number. */
    val statLabel: TextStyle = TextStyle(
        color = textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )

    /** Label inside a habit / task chip. */
    val chipLabel: TextStyle = TextStyle(
        color = textPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
    )

    /** Big streak number rendered on the coral flame. */
    val flameNumber: TextStyle = TextStyle(
        color = buttonText,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
    )

    /** Streak number overlaid inside the painterly flame (large layout). */
    val flameOverlayNumber: TextStyle = TextStyle(
        color = flameOverlay,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
    )

    /** Streak number overlaid inside the painterly flame (compact 2x2 layout). */
    val flameOverlayNumberSmall: TextStyle = TextStyle(
        color = flameOverlay,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
}
