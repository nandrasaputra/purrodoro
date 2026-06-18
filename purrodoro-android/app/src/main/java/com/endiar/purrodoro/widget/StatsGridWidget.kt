package com.endiar.purrodoro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.endiar.purrodoro.R

/**
 * Purrodoro Daily Starts — a 2×2 glance of today's focus, streak, eligible
 * habits, and open tasks (picker preview).
 */
class StatsGridWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = WidgetDataLoader.load(context)
        provideContent { StatsGridContent(loaded) }
    }
}

class StatsGridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsGridWidget()
}

@Composable
private fun StatsGridContent(loaded: LoadedWidgetData) {
    val data = loaded.data
    val res = loaded.res
    val cardAction = WidgetActions.action(res, data.nudge.target)
    KuroWidgetCard(onClick = cardAction, paddingHorizontal = 14, paddingVertical = 14) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = res.getString(R.string.widget_stats_grid_title),
                style = WidgetTheme.title,
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                StatTile(
                    emoji = "\uD83C\uDFAF",
                    value = data.focusToday.toString(),
                    label = res.getString(R.string.widget_stat_focus),
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                StatTile(
                    emoji = "\uD83D\uDD25",
                    value = data.streak.toString(),
                    label = res.getString(R.string.widget_stat_streak),
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                StatTile(
                    emoji = "\uD83C\uDF31",
                    value = data.pendingHabits.size.toString(),
                    label = res.getString(R.string.widget_stat_habits),
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                StatTile(
                    emoji = "\u2705",
                    value = data.pendingTasksCount.toString(),
                    label = res.getString(R.string.widget_stat_tasks),
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
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
