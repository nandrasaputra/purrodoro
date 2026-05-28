package com.endiar.kuropomodoro.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

/**
 * Centralised Glance color + text styles for the Kuro widgets.
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
}
