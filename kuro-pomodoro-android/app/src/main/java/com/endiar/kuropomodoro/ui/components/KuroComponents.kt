package com.endiar.kuropomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.ui.theme.Blush
import com.endiar.kuropomodoro.ui.theme.CardWhite
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.CocoaSoft
import com.endiar.kuropomodoro.ui.theme.Coral
import com.endiar.kuropomodoro.ui.theme.Cream
import com.endiar.kuropomodoro.ui.theme.Lavender
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.Peach
import com.endiar.kuropomodoro.ui.theme.Sky

@Composable
fun KuroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Cocoa)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = CocoaSoft)
        }
    }
}

@Composable
fun ModeChip(mode: TimerMode, selected: Boolean, onClick: () -> Unit) {
    val color = when (mode) {
        TimerMode.Focus -> Peach
        TimerMode.ShortBreak -> Mint
        TimerMode.LongBreak -> Lavender
    }
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color else Cream
    ) {
        Text(
            text = mode.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = Cocoa,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun KuroPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = CardWhite)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TimerRing(timeText: String, progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(230.dp)) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Blush,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
            drawArc(
                color = Coral,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, style = MaterialTheme.typography.displaySmall, color = Cocoa)
            Text("stay with kuro", style = MaterialTheme.typography.bodyMedium, color = CocoaSoft)
        }
    }
}

@Composable
fun CatCompanion(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(180.dp)) {
        drawCircle(color = Peach, radius = 62.dp.toPx(), center = Offset(size.width / 2, size.height / 2 + 12.dp.toPx()))
        drawCircle(color = Peach, radius = 44.dp.toPx(), center = Offset(size.width / 2, size.height / 2 - 34.dp.toPx()))
        drawCircle(color = Cocoa, radius = 5.dp.toPx(), center = Offset(size.width / 2 - 16.dp.toPx(), size.height / 2 - 42.dp.toPx()))
        drawCircle(color = Cocoa, radius = 5.dp.toPx(), center = Offset(size.width / 2 + 16.dp.toPx(), size.height / 2 - 42.dp.toPx()))
        drawCircle(color = Coral, radius = 5.dp.toPx(), center = Offset(size.width / 2, size.height / 2 - 28.dp.toPx()))
        drawCircle(color = Cocoa, radius = 12.dp.toPx(), center = Offset(size.width / 2 - 42.dp.toPx(), size.height / 2 + 28.dp.toPx()))
        drawCircle(color = Cocoa, radius = 12.dp.toPx(), center = Offset(size.width / 2 + 42.dp.toPx(), size.height / 2 + 28.dp.toPx()))
    }
}

@Composable
fun BottomTabBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(CardWhite)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(tab) }
                    .background(if (selected) Blush else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(tab.symbol, color = if (selected) Coral else CocoaSoft, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(tab.label, color = Cocoa, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DecorativeBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.background(Cream)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Sky, radius = 120.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.16f))
            drawCircle(Mint, radius = 90.dp.toPx(), center = Offset(size.width * 0.92f, size.height * 0.28f))
            drawCircle(Blush, radius = 110.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.82f))
        }
        content()
    }
}
