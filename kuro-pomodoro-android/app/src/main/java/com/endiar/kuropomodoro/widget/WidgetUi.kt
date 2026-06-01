package com.endiar.kuropomodoro.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text

/**
 * Shared Glance building blocks for the reactive Kuro widgets so each widget
 * file stays declarative. All visuals come from [WidgetTheme].
 */

/**
 * Responsive breakpoints for the adaptive widgets (Streak / Calm / Cozy).
 *
 * Glance picks the largest declared size that fits the host cell, so a 2x2 slot
 * resolves to [SMALL] and anything larger resolves to [LARGE]. Widgets compare
 * `LocalSize.current.width` against [COMPACT_THRESHOLD] to choose a layout.
 */
object WidgetSizes {
    val SMALL = DpSize(120.dp, 120.dp)
    val LARGE = DpSize(250.dp, 250.dp)
    val COMPACT_THRESHOLD = 180.dp
}

/** The standard cream rounded card every widget sits inside. */
@Composable
fun KuroWidgetCard(
    onClick: Action,
    paddingHorizontal: Int = 18,
    paddingVertical: Int = 16,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(onClick),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetTheme.cardFill)
                .cornerRadius(28.dp)
                .padding(horizontal = paddingHorizontal.dp, vertical = paddingVertical.dp),
        ) {
            content()
        }
    }
}

/** Filled coral pill — the primary call to action. */
@Composable
fun PrimaryCta(label: String, onClick: Action, height: Int = 48) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height.dp)
            .background(WidgetTheme.buttonFill)
            .cornerRadius(34.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = WidgetTheme.buttonLabel, maxLines = 1)
    }
}

/** Soft-tinted pill with coral text — the calmer / secondary CTA. */
@Composable
fun GhostCta(label: String, onClick: Action) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(46.dp)
            .background(WidgetTheme.accentSoft)
            .cornerRadius(34.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = WidgetTheme.ghostLabel, maxLines = 1)
    }
}
