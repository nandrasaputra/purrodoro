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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.endiar.purrodoro.R

/**
 * Concept 6 — "Streak Hero".
 *
 * A single, fully immersive 2x2 tile: the painterly flame fills the whole card
 * and the live day-streak number sits in its glowing belly. Tapping starts a
 * focus session to keep (or begin) the streak.
 */
class StreakHeroWidget : GlanceAppWidget() {

    // Fixed 2x2 tile — no responsive variants.
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent { StreakHeroContent(context, data) }
    }
}

class StreakHeroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakHeroWidget()
}

@Composable
private fun StreakHeroContent(context: Context, data: KuroWidgetData) {
    val cardAction = WidgetActions.action(context, NudgeTarget.StartFocus)
    val height = LocalSize.current.height

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(cardAction),
    ) {
        // Full-bleed cat + flame art; the streak number rides on the flame body.
        Image(
            provider = ImageProvider(R.drawable.streak_scene_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp),
        )
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = GlanceModifier.padding(top = (height.value * 0.20f).dp)) {
                Text(
                    text = data.streak.toString(),
                    style = WidgetTheme.streakSceneNumber,
                    maxLines = 1,
                )
            }
        }
    }
}
