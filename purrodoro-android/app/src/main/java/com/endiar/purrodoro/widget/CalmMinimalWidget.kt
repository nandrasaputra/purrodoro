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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.endiar.purrodoro.R

/**
 * Concept 8 — "Calm Minimal".
 *
 * A quiet, near-square tile: the soft full-bleed Purrodoro illustration is the whole
 * card, with one gentle nudge line floating over the top and a single ghost CTA
 * pinned at the bottom (matching the widget picker preview).
 */
class CalmMinimalWidget : GlanceAppWidget() {

    // Single near-square layout — the art is the card.
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = WidgetDataLoader.load(context)
        provideContent { CalmMinimalContent(loaded) }
    }
}

class CalmMinimalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalmMinimalWidget()
}

@Composable
private fun CalmMinimalContent(loaded: LoadedWidgetData) {
    val data = loaded.data
    val res = loaded.res
    val cardAction = WidgetActions.action(res, data.nudge.target)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(cardAction),
    ) {
        // Full-bleed calm Purrodoro art as the card itself.
        Image(
            provider = ImageProvider(R.drawable.calm_scene_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp),
        )

        // The headline + mascot are baked into the art; only the CTA is live,
        // pinned to the bottom over the clear space.
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            GhostCta(label = data.nudge.ctaLabel, onClick = cardAction)
        }
    }
}
