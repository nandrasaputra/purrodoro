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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.backgroundImageForTime
import java.time.LocalTime
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.Coral
import com.endiar.kuropomodoro.ui.theme.Lavender
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.Peach

private val DockCoral = Color(0xFFFF8B6F)
private val LockedPurple = Color(0xFF7B4FBF)

private data class GlassTokens(
    val cardFill: Color,
    val activeCardFill: Color,
    val cardBorder: Color,
    val pillFill: Color,
    val pillBorder: Color,
    val dockFill: Color,
    val dockBorder: Color,
    val scrimAlpha: Float
)

private fun glassTokens(treatment: GlassTreatment): GlassTokens {
    return when (treatment) {
        GlassTreatment.Clear -> GlassTokens(
            cardFill = Color(0xFF0F0A1A).copy(alpha = 0.40f),
            activeCardFill = DockCoral.copy(alpha = 0.10f),
            cardBorder = Color.White.copy(alpha = 0.10f),
            pillFill = Color.White.copy(alpha = 0.08f),
            pillBorder = Color.White.copy(alpha = 0.15f),
            dockFill = Color(0xFF15101F).copy(alpha = 0.65f),
            dockBorder = Color.White.copy(alpha = 0.15f),
            scrimAlpha = 0.80f
        )
        GlassTreatment.Readable -> GlassTokens(
            cardFill = Color(0xFF0A0816).copy(alpha = 0.72f),
            activeCardFill = Color(0xFF2B1720).copy(alpha = 0.80f),
            cardBorder = Color.White.copy(alpha = 0.20f),
            pillFill = Color(0xFF0A0816).copy(alpha = 0.60f),
            pillBorder = Color.White.copy(alpha = 0.25f),
            dockFill = Color(0xFF0A0816).copy(alpha = 0.85f),
            dockBorder = Color.White.copy(alpha = 0.20f),
            scrimAlpha = 0.90f
        )
    }
}

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
private fun PulseWrapper(active: Boolean, content: @Composable () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.15f else 1.0f,
        animationSpec = if (active) {
            infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "pulseScale"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun DashboardDock(
    isRunning: Boolean,
    isSoundPlaying: Boolean,
    onNavigate: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AppTab? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
) {
    val tokens = glassTokens(glassTreatment)
    Row(
        modifier = modifier
            .padding(horizontal = 21.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(62.dp)
            .border(1.dp, tokens.dockBorder, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(tokens.dockFill)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            icon = "\u2611",
            label = "TODO",
            active = selectedTab == AppTab.Todo,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Todo) }
        DockTabItem(
            icon = "\u229e",
            label = "RECORDS",
            active = selectedTab == AppTab.Calendar,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Calendar) }
        DockHomeButton(
            isRunning = isRunning,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(AppTab.Timer) }
        )
        DockTabItem(
            icon = if (isSoundPlaying) "\u266b" else "\u266a",
            label = "SOUNDS",
            active = selectedTab == AppTab.Sounds,
            isSoundPlaying = isSoundPlaying,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Sounds) }
        DockTabItem(
            icon = "\u25cb",
            label = "ME",
            active = selectedTab == AppTab.Profile,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Profile) }
    }
}

@Composable
private fun DockTabItem(
    icon: String,
    label: String,
    active: Boolean = false,
    isSoundPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val iconColor = if (active) DockCoral else Color.White.copy(alpha = 0.7f)
    val bgColor = if (active) DockCoral.copy(alpha = 0.2f) else Color.Transparent
    val shouldPulse = active && isSoundPlaying
    PulseWrapper(active = shouldPulse) {
        Column(
            modifier = modifier
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
}

@Composable
private fun DockHomeButton(isRunning: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icon = if (isRunning) "\u25b6" else "\u2302"
    val bgColor = DockCoral
    val contentColor = Color.White
    PulseWrapper(active = isRunning) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = icon,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "HOME",
                color = contentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
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
fun CatBackdrop(
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    time: LocalTime = LocalTime.now(),
    content: @Composable BoxScope.() -> Unit
) {
    val darkBase = Color(0xFF0A0816)
    val tokens = glassTokens(glassTreatment)
    Box(modifier = modifier.fillMaxSize().background(darkBase)) {
        Image(
            painter = painterResource(id = backgroundImageForTime(time)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(darkBase.copy(alpha = tokens.scrimAlpha), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, darkBase.copy(alpha = tokens.scrimAlpha))
                    )
                )
        )
        content()
    }
}

// ── Glass primitives ─────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = glassTokens(glassTreatment)
    val fill = if (active) tokens.activeCardFill else tokens.cardFill
    val border = if (active) DockCoral.copy(alpha = 0.30f) else tokens.cardBorder
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    val tokens = glassTokens(glassTreatment)
    val fill = if (selected) DockCoral else tokens.pillFill
    val textColor = if (selected) Color.White else Color.White.copy(alpha = 0.72f)
    val borderColor = if (selected) Color.Transparent else tokens.pillBorder
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

enum class StatusPillKind { Playing, Selected, Locked, Unlocked }

@Composable
fun StatusPill(
    label: String,
    kind: StatusPillKind,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    val tokens = glassTokens(glassTreatment)
    val fill = when (kind) {
        StatusPillKind.Playing  -> DockCoral
        StatusPillKind.Selected -> tokens.pillFill
        StatusPillKind.Locked   -> LockedPurple.copy(alpha = 0.25f)
        StatusPillKind.Unlocked -> tokens.pillFill
    }
    val textColor = when (kind) {
        StatusPillKind.Playing  -> Color.White
        StatusPillKind.Selected -> Color.White.copy(alpha = 0.80f)
        StatusPillKind.Locked   -> Color(0xFFD8C8FF)
        StatusPillKind.Unlocked -> Color.White.copy(alpha = 0.55f)
    }
    val borderColor = when (kind) {
        StatusPillKind.Locked   -> Color(0xFFD8C8FF).copy(alpha = 0.40f)
        else                    -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StepperButton(
    label: String,
    onClick: () -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    val tokens = glassTokens(glassTreatment)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(tokens.pillFill)
            .border(1.dp, tokens.pillBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun MockBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LockedPurple.copy(alpha = 0.25f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = "Mock", color = Color(0xFFD8C8FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
