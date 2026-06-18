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
 * Purrodoro Cozy home-screen widget.
 *
 * Full-bleed study-nook illustration with the picker-preview headline and a
 * live "focus today" pill floating over the empty wall up top.
 */
class CozySceneWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(WidgetSizes.SMALL, WidgetSizes.LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = WidgetDataLoader.load(context)
        provideContent { CozySceneContent(loaded) }
    }
}

class CozySceneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CozySceneWidget()
}

@Composable
private fun CozySceneContent(loaded: LoadedWidgetData) {
    val cardAction = WidgetActions.action(loaded.res, loaded.data.nudge.target)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(cardAction),
    ) {
        Image(
            provider = ImageProvider(R.drawable.cozy_scene_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp),
        )

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = loaded.res.getString(R.string.widget_cozy_headline),
                style = WidgetTheme.cozyTitle,
                maxLines = 2,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            TodayBadge(
                res = loaded.res,
                focusToday = loaded.data.focusToday,
            )
        }
    }
}

@Composable
private fun TodayBadge(res: Context, focusToday: Int) {
    Box(
        modifier = GlanceModifier
            .background(WidgetTheme.buttonFill)
            .cornerRadius(20.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = res.getString(R.string.widget_cozy_today_badge, focusToday),
            style = WidgetTheme.buttonLabel,
            maxLines = 1,
        )
    }
}
