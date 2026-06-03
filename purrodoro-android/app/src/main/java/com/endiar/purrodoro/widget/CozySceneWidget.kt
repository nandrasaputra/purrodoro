package com.endiar.purrodoro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.endiar.purrodoro.R

/**
 * Concept 10 — "Cozy Scene".
 *
 * A fully immersive card: the warm painted study-nook illustration is the
 * background edge-to-edge (matching the widget picker preview), with only the
 * live nudge headline and a "today" pill floating over the empty wall area up
 * top. Tapping anywhere follows the current nudge target.
 *
 * The scene art (`cozy_scene_bg`) is pre-baked with a clear upper wall so the
 * dynamic text stays legible; everything below stays purely illustrative.
 */
class CozySceneWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(WidgetSizes.SMALL, WidgetSizes.LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent { CozySceneContent(context, data) }
    }
}

class CozySceneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CozySceneWidget()
}

@Composable
private fun CozySceneContent(context: Context, data: KuroWidgetData) {
    val cardAction = WidgetActions.action(context, data.nudge.target)
    val compact = LocalSize.current.width < WidgetSizes.COMPACT_THRESHOLD
    val pad = if (compact) 12 else 16

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(cardAction),
    ) {
        // Full-bleed cozy scene art as the card itself.
        Image(
            provider = ImageProvider(R.drawable.cozy_scene_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp),
        )

        // Live headline + today badge float over the empty wall up top.
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = pad.dp, vertical = pad.dp)) {
            if (!compact) {
                Text(text = data.nudge.title, style = WidgetTheme.cozyTitle, maxLines = 2)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
            TodayBadge(focusToday = data.focusToday)
        }
    }
}

@Composable
private fun TodayBadge(focusToday: Int) {
    Box(
        modifier = GlanceModifier
            .background(WidgetTheme.buttonFill)
            .cornerRadius(20.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "\uD83D\uDD25 $focusToday today",
            style = WidgetTheme.buttonLabel,
            maxLines = 1,
        )
    }
}
