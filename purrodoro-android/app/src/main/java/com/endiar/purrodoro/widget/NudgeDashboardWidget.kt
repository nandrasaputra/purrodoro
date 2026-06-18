package com.endiar.purrodoro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.endiar.purrodoro.R
import java.time.LocalTime

/**
 * Concept 1 — "Purrodoro Dashboard".
 *
 * Greeting + today's focus count and streak + standing Purrodoro + a Start Focus CTA.
 * The whole card and the CTA deep-link via the current nudge target.
 */
class NudgeDashboardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = WidgetDataLoader.load(context)
        provideContent { DashboardContent(loaded) }
    }
}

class NudgeDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NudgeDashboardWidget()
}

private fun greetingFor(hour: Int): String = when {
    hour < 5 -> "Still up?"
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    hour < 21 -> "Good evening"
    else -> "Winding down?"
}

@Composable
private fun DashboardContent(loaded: LoadedWidgetData) {
    val data = loaded.data
    val res = loaded.res
    val cardAction = WidgetActions.action(res, data.nudge.target)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(cardAction),
    ) {
        // Full-bleed sunrise sky + standing Purrodoro as the card itself.
        Image(
            provider = ImageProvider(R.drawable.dashboard_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp),
        )

        // Greeting + stats float over the open sky; the CTA pins to the bottom.
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = greetingFor(LocalTime.now().hour),
                style = WidgetTheme.onImageTitle,
                maxLines = 1,
            )
            Text(text = data.nudge.line, style = WidgetTheme.onImageSubtitle, maxLines = 1)
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(text = "\uD83C\uDFAF ${data.focusToday} focus", style = WidgetTheme.onImageStat, maxLines = 1)
                Spacer(modifier = GlanceModifier.width(12.dp))
                Text(text = "\uD83D\uDD25 ${data.streak} day streak", style = WidgetTheme.onImageStat, maxLines = 1)
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            PrimaryCta(label = data.nudge.ctaLabel, onClick = cardAction)
        }
    }
}
