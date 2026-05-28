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
import java.time.Instant
import java.time.ZoneId

/**
 * Today With Kuro home-screen widget.
 *
 * Mirrors the Variation A mockup:
 *  - Warm cream card, big coral focus count, a row of progress dots
 *  - Kuro mouthless cat face peeking from the bottom-right
 *  - Encouragement copy that softens for 0 focus and warms up at the goal
 *
 * Tapping the widget just opens the app. No action callbacks needed.
 */
class TodayProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) {
            val repo = AppRepository.get(context)
            val app = repo.appState.first()
            val today = Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val focusCount = app.stamps[today] ?: 0
            WidgetState.today(focusCountToday = focusCount, settings = app.settings)
        }
        provideContent {
            TodayContent(context = context, state = state)
        }
    }
}

class TodayProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayProgressWidget()
}

@Composable
private fun TodayContent(context: Context, state: TodayWidgetState) {
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            // Kuro peeking from the bottom-right.
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.cat_face),
                    contentDescription = null,
                    modifier = GlanceModifier.size(48.dp),
                )
            }

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start,
            ) {
                Text(
                    text = "Today with Kuro",
                    style = WidgetTheme.title,
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Row(verticalAlignment = Alignment.Vertical.Bottom) {
                    Text(
                        text = state.focusCount.toString(),
                        style = WidgetTheme.metricNumber,
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "focus",
                        style = WidgetTheme.metricUnit,
                    )
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
                DotRow(filled = state.filledDots, total = state.targetDots)
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = state.encouragement,
                    style = WidgetTheme.body,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DotRow(filled: Int, total: Int) {
    val safeTotal = total.coerceAtLeast(1)
    val safeFilled = filled.coerceIn(0, safeTotal)
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        repeat(safeTotal) { index ->
            val isFilled = index < safeFilled
            Image(
                provider = ImageProvider(
                    if (isFilled) R.drawable.widget_dot_filled else R.drawable.widget_dot_outline,
                ),
                contentDescription = null,
                modifier = GlanceModifier.size(14.dp),
            )
            if (index < safeTotal - 1) {
                Spacer(modifier = GlanceModifier.width(6.dp))
            }
        }
    }
}
