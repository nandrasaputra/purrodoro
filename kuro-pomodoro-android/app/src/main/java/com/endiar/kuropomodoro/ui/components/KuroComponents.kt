package com.endiar.kuropomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.backgroundImageForTime
import java.time.LocalTime
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.Coral
import com.endiar.kuropomodoro.ui.theme.Lavender
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.Peach

private val DockCoral = Color(0xFFFF8B6F)

@Composable
fun KuroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = colorScheme.onBackground)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
fun ModeChip(mode: TimerMode, selected: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedColor = when (mode) {
        TimerMode.Focus -> colorScheme.secondary
        TimerMode.ShortBreak -> colorScheme.tertiary
        TimerMode.LongBreak -> Lavender
    }
    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) selectedColor else colorScheme.surface
    ) {
        Text(
            text = mode.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (selected) Cocoa else colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun KuroPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TimerRing(timeText: String, progress: Float, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(230.dp)) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = colorScheme.onSurface.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
            drawArc(
                color = colorScheme.primary,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, style = MaterialTheme.typography.displaySmall, color = colorScheme.onBackground)
            Text("stay with kuro", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface.copy(alpha = 0.72f))
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
fun BottomTabBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .selectable(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        role = Role.Tab
                    )
                    .heightIn(min = 48.dp)
                    .widthIn(min = 48.dp)
                    .background(if (selected) colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(tab.symbol, color = if (selected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.72f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(tab.label, color = colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DashboardModeChip(mode: TimerMode, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.15f)
    val textColor = if (selected) Color(0xFF241E2B) else Color.White.copy(alpha = 0.85f)
    Surface(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Text(
            text = mode.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = textColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun DashboardDock(
    isRunning: Boolean,
    onNavigate: (AppTab) -> Unit,
    onStartPause: () -> Unit,
    selectedTab: AppTab? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 21.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(62.dp)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xFF15101F).copy(alpha = 0.65f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            icon = "\u2611",
            label = "TODO",
            active = selectedTab == AppTab.Todo
        ) { onNavigate(AppTab.Todo) }
        DockTabItem(
            icon = "\u229e",
            label = "RECORDS",
            active = selectedTab == AppTab.Calendar
        ) { onNavigate(AppTab.Calendar) }
        DockHomeButton(isRunning = isRunning, onClick = onStartPause)
        DockTabItem(
            icon = "\u266a",
            label = "SOUNDS",
            active = selectedTab == AppTab.Sounds
        ) { onNavigate(AppTab.Sounds) }
        DockTabItem(
            icon = "\u25cb",
            label = "ME",
            active = selectedTab == AppTab.Profile
        ) { onNavigate(AppTab.Profile) }
    }
}

@Composable
private fun DockTabItem(icon: String, label: String, active: Boolean = false, onClick: () -> Unit) {
    val iconColor = if (active) DockCoral else Color.White.copy(alpha = 0.7f)
    val bgColor = if (active) DockCoral.copy(alpha = 0.2f) else Color.Transparent
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(icon, color = iconColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(label, color = iconColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun DockHomeButton(isRunning: Boolean, onClick: () -> Unit) {
    val bgColor = if (isRunning) Color.White.copy(alpha = 0.92f) else DockCoral
    val contentColor = if (isRunning) Color(0xFF241E2B) else Color.White
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = if (isRunning) "\u23f8" else "\u25b6",
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (isRunning) "PAUSE" else "START",
            color = contentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun DecorativeBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier.background(colorScheme.background)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(colorScheme.secondary.copy(alpha = 0.42f), radius = 120.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.16f))
            drawCircle(colorScheme.tertiary.copy(alpha = 0.38f), radius = 90.dp.toPx(), center = Offset(size.width * 0.92f, size.height * 0.28f))
            drawCircle(colorScheme.primary.copy(alpha = 0.28f), radius = 110.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.82f))
        }
        content()
    }
}

@Composable
fun CatBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val darkBase = Color(0xFF0A0816)
    Box(modifier = modifier.fillMaxSize().background(darkBase)) {
        Image(
            painter = painterResource(id = backgroundImageForTime(LocalTime.now())),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Top scrim — legibility for page title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(darkBase.copy(alpha = 0.80f), Color.Transparent)
                    )
                )
        )
        // Bottom scrim — legibility for dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, darkBase.copy(alpha = 0.80f))
                    )
                )
        )
        content()
    }
}
