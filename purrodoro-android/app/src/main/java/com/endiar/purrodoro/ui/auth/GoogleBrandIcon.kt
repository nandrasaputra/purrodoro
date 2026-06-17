package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.theme.kuroTokens

/**
 * Branded Google badge with explicit light/dark assets. Follows Purrodoro's
 * in-app theme ([MaterialTheme] page background), not the OS night mode alone.
 *
 * Use [onColoredButton] for coral CTAs (e.g. onboarding sign-in) so the
 * white-circle variant stays readable on [KuroSurfaceTokens.interactiveFill].
 */
@Composable
fun GoogleBrandIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onColoredButton: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isDarkTheme = tokens.pageBackground.luminance() < 0.5f
    val iconRes = when {
        onColoredButton -> R.drawable.google_icon_light
        isDarkTheme -> R.drawable.google_icon_dark
        else -> R.drawable.google_icon_light
    }

    Image(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
