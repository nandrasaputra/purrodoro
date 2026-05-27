package com.endiar.kuropomodoro.widget

import androidx.compose.ui.unit.sp
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.endiar.kuropomodoro.R

/**
 * Centralised Glance color + text styles for the Kuro widgets.
 *
 * Color providers point at `@color/widget_*` resources, which have a
 * `values-night/` override, so light and dark schemes are handled by the
 * Android resource framework without per-composable branching.
 */
object WidgetTheme {

    val cardFill = ColorProvider(R.color.widget_card_fill)
    val cardBorder = ColorProvider(R.color.widget_card_border)
    val pageBg = ColorProvider(R.color.widget_page_bg)
    val textPrimary = ColorProvider(R.color.widget_text_primary)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val textMuted = ColorProvider(R.color.widget_text_muted)
    val accent = ColorProvider(R.color.widget_accent)
    val buttonFill = ColorProvider(R.color.widget_button_fill)
    val buttonText = ColorProvider(R.color.widget_button_text)

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
