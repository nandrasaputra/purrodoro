package com.endiar.kuropomodoro.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.endiar.kuropomodoro.MainActivity
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Quick Start Kuro home-screen widget.
 *
 * Mirrors the Variation A mockup:
 *  - Warm cream card with cocoa text and a coral pill CTA
 *  - Kuro mouthless cat face peeking from the top-right
 *  - Title + supporting copy + primary action
 *
 * The CTA + the whole card both launch [MainActivity]. For the "Ready" mood,
 * the launch intent carries [EXTRA_AUTO_OPEN_START_FOCUS] so the host can
 * surface the existing Start Focus sheet automatically; the other moods just
 * open the app so the user lands on whatever they were doing.
 */
class QuickStartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (state, language) = withContext(Dispatchers.IO) {
            val repo = AppRepository.get(context)
            val runtime = repo.runtimeSnapshot.first()
            val pendingAck = repo.pendingCycleCompletionAckFlow.first()
            val app = repo.appState.first()
            WidgetState.quickStart(runtime, pendingAck) to app.language
        }
        val localized = localizedWidgetContext(context, language)
        provideContent {
            QuickStartContent(context = context, res = localized, state = state)
        }
    }

    companion object {
        const val EXTRA_AUTO_OPEN_START_FOCUS = "com.endiar.kuropomodoro.widget.AUTO_OPEN_START_FOCUS"
    }
}

class QuickStartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickStartWidget()
}

@Composable
private fun QuickStartContent(context: Context, res: Context, state: QuickStartWidgetState) {
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (state.mood == QuickStartMood.Ready) {
            putExtra(QuickStartWidget.EXTRA_AUTO_OPEN_START_FOCUS, true)
        }
    }
    val openAction = actionStartActivity(openIntent)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(openAction),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetTheme.cardFill)
                .cornerRadius(28.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // Kuro peeking from the top-right.
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.cat_face),
                    contentDescription = null,
                    modifier = GlanceModifier.size(54.dp),
                )
            }

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start,
            ) {
                Text(
                    text = res.quickStartTitle(state.mood),
                    style = WidgetTheme.titleLg,
                    maxLines = 2,
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = res.quickStartSubtitle(state.mood),
                    style = WidgetTheme.body,
                    maxLines = 2,
                )
                // Push the CTA to the bottom of the card so the content spans
                // the full height instead of clustering at the top.
                Spacer(modifier = GlanceModifier.defaultWeight())
                StartFocusButton(
                    label = res.quickStartAction(state.mood),
                    onClick = openAction,
                )
            }
        }
    }
}

@Composable
private fun StartFocusButton(label: String, onClick: androidx.glance.action.Action) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(48.dp)
            .background(WidgetTheme.buttonFill)
            .cornerRadius(34.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = WidgetTheme.buttonLabel,
        )
    }
}
