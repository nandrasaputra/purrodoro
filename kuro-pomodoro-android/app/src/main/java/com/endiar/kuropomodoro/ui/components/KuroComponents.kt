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
import androidx.compose.foundation.layout.width
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.endiar.kuropomodoro.model.AppearanceMode
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.backgroundImageForTime
import java.time.LocalTime
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.Coral
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import com.endiar.kuropomodoro.ui.theme.Lavender
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.Peach

private val DockCoral = Color(0xFFFF8B6F)
private val LockedPurple = Color(0xFF7B4FBF)

internal data class GlassTokens(
    val cardFill: Color,
    val activeCardFill: Color,
    val cardBorder: Color,
    val activeCardBorder: Color,
    val pillFill: Color,
    val pillBorder: Color,
    val dockFill: Color,
    val dockBorder: Color,
    val scrimAlpha: Float,
)

@Composable
internal fun glassTokens(
    treatment: GlassTreatment,
    overBackdrop: Boolean = true,
): GlassTokens {
    if (!overBackdrop) {
        val t = MaterialTheme.colorScheme.kuroTokens
        return GlassTokens(
            cardFill = t.cardFill,
            activeCardFill = t.activeCardFill,
            cardBorder = t.cardBorder,
            activeCardBorder = t.accent.copy(alpha = 0.45f),
            pillFill = t.segmentedTrack,
            pillBorder = t.borderSoft,
            dockFill = t.cardFill,
            dockBorder = t.cardBorder,
            scrimAlpha = 0f,
        )
    }
    return when (treatment) {
        GlassTreatment.Clear -> GlassTokens(
            cardFill = Color(0xFF0F0A1A).copy(alpha = 0.40f),
            activeCardFill = DockCoral.copy(alpha = 0.10f),
            cardBorder = Color.White.copy(alpha = 0.10f),
            activeCardBorder = DockCoral.copy(alpha = 0.30f),
            pillFill = Color.White.copy(alpha = 0.08f),
            pillBorder = Color.White.copy(alpha = 0.15f),
            dockFill = Color(0xFF15101F).copy(alpha = 0.92f),
            dockBorder = Color.White.copy(alpha = 0.18f),
            scrimAlpha = 0.80f,
        )
        GlassTreatment.Readable -> GlassTokens(
            cardFill = Color(0xFF0A0816).copy(alpha = 0.72f),
            activeCardFill = Color(0xFF2B1720).copy(alpha = 0.80f),
            cardBorder = Color.White.copy(alpha = 0.20f),
            activeCardBorder = DockCoral.copy(alpha = 0.45f),
            pillFill = Color(0xFF0A0816).copy(alpha = 0.60f),
            pillBorder = Color.White.copy(alpha = 0.25f),
            dockFill = Color(0xFF0A0816).copy(alpha = 0.95f),
            dockBorder = Color.White.copy(alpha = 0.22f),
            scrimAlpha = 0.90f,
        )
    }
}

@Composable
fun KuroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = tokens.cardFill),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(28.dp))
                .padding(20.dp),
            content = content,
        )
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
    isSoundPlaying: Boolean,
    onNavigate: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AppTab? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
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
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            icon = Icons.Default.List,
            label = "TODO",
            active = selectedTab == AppTab.Todo,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Todo) }
        DockTabItem(
            icon = Icons.Default.DateRange,
            label = "RECORDS",
            active = selectedTab == AppTab.Calendar,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Calendar) }
        DockTabItem(
            icon = if (isRunning) Icons.Default.PlayArrow else Icons.Default.Home,
            label = "HOME",
            active = selectedTab == AppTab.Timer,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Timer) }
        DockTabItem(
            icon = Icons.Default.Notifications,
            label = "SOUNDS",
            active = selectedTab == AppTab.Sounds,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Sounds) }
        DockTabItem(
            icon = Icons.Default.Person,
            label = "ME",
            active = selectedTab == AppTab.Profile,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Profile) }
    }
}

@Composable
private fun DockTabItem(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val kuroTokens = MaterialTheme.colorScheme.kuroTokens
    val iconColor = if (active) kuroTokens.accentOn else kuroTokens.textMuted
    val bgColor = if (active) kuroTokens.accent else Color.Transparent

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(label, color = iconColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
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

/**
 * The non-Focus tabs' background: a flat fill from the active scheme.
 *
 * Unlike [CatBackdrop] this does not paint a photo or scrim — it is the
 * "quiet workspace" surface for content-heavy tabs.
 */
@Composable
fun KuroFlatSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.pageBackground),
        content = content,
    )
}

// ── Glass primitives ─────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
    val fill = if (active) tokens.activeCardFill else tokens.cardFill
    val border = if (active) tokens.activeCardBorder else tokens.cardBorder
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
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
    val kuroT = MaterialTheme.colorScheme.kuroTokens
    val fill = if (selected) kuroT.accent else tokens.pillFill
    val textColor = if (selected) kuroT.accentOn else kuroT.textSecondary
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
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
    val kuroT = MaterialTheme.colorScheme.kuroTokens
    val fill = when (kind) {
        StatusPillKind.Playing  -> kuroT.accent
        StatusPillKind.Selected -> tokens.pillFill
        StatusPillKind.Locked   -> LockedPurple.copy(alpha = 0.25f)
        StatusPillKind.Unlocked -> tokens.pillFill
    }
    val textColor = when (kind) {
        StatusPillKind.Playing  -> kuroT.accentOn
        StatusPillKind.Selected -> kuroT.textPrimary
        StatusPillKind.Locked   -> kuroT.lockedText
        StatusPillKind.Unlocked -> kuroT.textSecondary
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
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
    val kuroT = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(tokens.pillFill)
            .border(1.dp, tokens.pillBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = kuroT.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Light)
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

@Composable
fun AppearanceModeToggle(
    mode: AppearanceMode,
    onModeChanged: (AppearanceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.segmentedTrack)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AppearanceMode.entries.forEach { entry ->
            val selected = entry == mode
            val icon = when (entry) {
                AppearanceMode.Light -> "☀"
                AppearanceMode.Dark -> "☾"
                AppearanceMode.Auto -> "◐"
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) tokens.accent else Color.Transparent)
                    .clickable { onModeChanged(entry) }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = icon,
                    fontSize = 14.sp,
                    color = if (selected) tokens.accentOn else tokens.textSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = entry.label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) tokens.accentOn else tokens.textSecondary,
                )
            }
        }
    }
}

/**
 * A row of dots showing focus session progress within a Pomodoro cycle.
 *
 * @param totalFocusSessions  Total focus sessions in this cycle (= focusSessionsPerCycle)
 * @param completedFocusSessions  Number of focus sessions already completed this cycle
 * @param isCurrentlyFocus  True if the timer is currently on a Focus session (highlights current dot)
 */
@Composable
fun CycleDotsRow(
    totalFocusSessions: Int,
    completedFocusSessions: Int,
    isCurrentlyFocus: Boolean,
    modifier: Modifier = Modifier
) {
    val peach = Color(0xFFFF8B6F)
    val peachMuted = Color(0xFFFF8B6F).copy(alpha = 0.25f)
    val currentDotSize = 12.dp
    val dotSize = 10.dp
    val strokeWidth = 1.5.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalFocusSessions) { index ->
            val isCompleted = index < completedFocusSessions
            val isCurrent = isCurrentlyFocus && index == completedFocusSessions
            val size = if (isCurrent) currentDotSize else dotSize

            Canvas(modifier = Modifier.size(size)) {
                val radius = this.size.minDimension / 2f
                when {
                    isCompleted -> {
                        // Filled peach dot
                        drawCircle(color = peach, radius = radius)
                    }
                    isCurrent -> {
                        // Filled peach dot (slightly larger, acts as highlight)
                        drawCircle(color = peach, radius = radius)
                    }
                    else -> {
                        // Outline dot
                        drawCircle(
                            color = peachMuted,
                            radius = radius - strokeWidth.toPx() / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}
