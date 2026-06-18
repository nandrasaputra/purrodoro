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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.endiar.purrodoro.R

/**
 * Purrodoro Tasks — pending habit/task chips with a Start Focus CTA (picker preview).
 */
class DailyPlanWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = WidgetDataLoader.load(context)
        provideContent { DailyPlanContent(loaded) }
    }
}

class DailyPlanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyPlanWidget()
}

@Composable
private fun DailyPlanContent(loaded: LoadedWidgetData) {
    val data = loaded.data
    val res = loaded.res
    val cardAction = WidgetActions.action(res, NudgeTarget.OpenApp)
    val chips = data.planChips(max = 3)
    val overflow = (data.pendingHabits.size + data.pendingTasks.size) - chips.size
    KuroWidgetCard(onClick = cardAction, paddingHorizontal = 14, paddingVertical = 12) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = res.getString(R.string.widget_daily_plan_title),
                    style = WidgetTheme.title,
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Image(
                    provider = ImageProvider(R.drawable.cat_peek),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.size(40.dp),
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))

            if (chips.isEmpty()) {
                Box(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = res.getString(R.string.widget_daily_plan_empty),
                        style = WidgetTheme.body,
                        maxLines = 2,
                    )
                }
            } else {
                chips.forEachIndexed { index, chip ->
                    if (index > 0) Spacer(modifier = GlanceModifier.height(6.dp))
                    PlanChip(res = res, chip = chip)
                }
                if (overflow > 0) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = res.getString(R.string.widget_daily_plan_overflow, overflow),
                        style = WidgetTheme.muted,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.height(6.dp))
            PrimaryCta(
                label = res.getString(R.string.widget_daily_plan_start_focus),
                onClick = WidgetActions.action(res, NudgeTarget.StartFocus),
                height = 40,
            )
        }
    }
}

@Composable
private fun PlanChip(res: Context, chip: WidgetChip) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(34.dp)
            .background(WidgetTheme.chipFill)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp)
            .clickable(WidgetActions.action(res, chip.target)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(text = chip.emoji, style = WidgetTheme.chipLabel, maxLines = 1)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(text = chip.label, style = WidgetTheme.chipLabel, maxLines = 1)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier
                .size(16.dp)
                .background(WidgetTheme.accentSoft)
                .cornerRadius(8.dp),
        ) {}
    }
}
