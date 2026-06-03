package com.endiar.purrodoro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.endiar.purrodoro.R

/**
 * Concept 9 — "Stats Grid".
 *
 * A 2x2 glance of focus / streak / habits / tasks with a soft nudge footer.
 * The whole card deep-links via the current nudge target.
 */
class StatsGridWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent { StatsGridContent(context, data) }
    }
}

class StatsGridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsGridWidget()
}

@Composable
private fun StatsGridContent(context: Context, data: KuroWidgetData) {
    val cardAction = WidgetActions.action(context, data.nudge.target)
    KuroWidgetCard(onClick = cardAction, paddingHorizontal = 14, paddingVertical = 14) {
      Box(modifier = GlanceModifier.fillMaxSize()) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                StatTile(
                    emoji = "\uD83C\uDFAF",
                    value = data.focusToday.toString(),
                    label = "focus",
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                StatTile(
                    emoji = "\uD83D\uDD25",
                    value = data.streak.toString(),
                    label = "streak",
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                StatTile(
                    emoji = "\uD83C\uDF31",
                    value = "${data.habitsDoneToday}/${data.habitsScheduledToday}",
                    label = "habits",
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                StatTile(
                    emoji = "\u2705",
                    value = data.pendingTasksCount.toString(),
                    label = "tasks",
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
            }
        }

        // Purrodoro's face sticker overlaps the center seam of the 2x2 grid.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.stats_cat),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(64.dp),
            )
        }
      }
    }
}

@Composable
private fun StatTile(
    emoji: String,
    value: String,
    label: String,
    modifier: GlanceModifier,
) {
    Box(
        modifier = modifier
            .background(WidgetTheme.chipFill)
            .cornerRadius(18.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Text(text = emoji, style = WidgetTheme.statLabel, maxLines = 1)
            Text(text = value, style = WidgetTheme.statNumber, maxLines = 1)
            Text(text = label, style = WidgetTheme.statLabel, maxLines = 1)
        }
    }
}
