package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.FocusTask
import java.time.LocalDate
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.kuroTokens

/**
 * Root scaffold for all flat-surface tab screens (Todo, Habits, Journey, etc.).
 */
@Composable
fun KuroScreenScaffold(
    stickyTitle: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.(scrollState: androidx.compose.foundation.ScrollState, headerMeasureModifier: Modifier) -> Unit,
) {
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topPaddingPx = with(density) { KuroSpacing.sm.roundToPx() }
    val showStickyBar by remember {
        derivedStateOf { scrollState.value > headerHeightPx + topPaddingPx }
    }
    val headerMeasureModifier = Modifier.onSizeChanged { headerHeightPx = it.height }

    KuroFlatSurface(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it },
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            ScreenStickyBar(title = stickyTitle, modifier = Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content(scrollState, headerMeasureModifier)
        }
    }
}

/** Shadowed surface card for the Journey tab (metrics, charts, calendar). */
@Composable
fun KuroElevatedCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(KuroSpacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(KuroSpacing.elevationSoft, RoundedCornerShape(KuroSpacing.radiusLg)),
        shape = RoundedCornerShape(KuroSpacing.radiusLg),
        color = tokens.cardFill,
        border = BorderStroke(1.dp, tokens.cardBorder),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Journey 2×2 stat card — icon badge, label, serif value, footnote. */
@Composable
fun KuroJourneyMetricCard(
    title: String,
    value: String,
    note: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    noteColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard(
        modifier = modifier.height(KuroSpacing.journeyMetricCardHeight),
        contentPadding = PaddingValues(KuroSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = iconBackground,
                modifier = Modifier.size(KuroSpacing.journeyMetricIconCircle),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(KuroSpacing.journeyMetricIconGlyph),
                    )
                }
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(KuroSpacing.sm))
        Text(title, color = tokens.textSecondary, style = KuroType.labelMetric)
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            value,
            color = tokens.textPrimary,
            fontFamily = FontFamily.Serif,
            style = KuroType.metricValueSerif,
        )
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            note,
            color = noteColor ?: tokens.textSecondary,
            style = if (noteColor != null) KuroType.metricDeltaPositive else KuroType.labelMeta,
        )
    }
}

/** Tasks overview metrics card. */
@Composable
fun KuroTasksOverviewCard(
    total: Int,
    inProgress: Int,
    completed: Int,
    estimatedFocusTimeLabel: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroCard(modifier = modifier) {
        Text(text = "Overview", color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(KuroSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            KuroTasksOverviewMetric("📋", total.toString(), "Total", Modifier.weight(1f))
            KuroTasksOverviewMetric("🕐", inProgress.toString(), "In Progress", Modifier.weight(1f))
            KuroTasksOverviewMetric("✅", completed.toString(), "Completed", Modifier.weight(1f))
            KuroTasksOverviewMetric("🕐", estimatedFocusTimeLabel, "Est. Focus Time", Modifier.weight(1f))
        }
    }
}

@Composable
private fun KuroTasksOverviewMetric(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Text(text = icon, style = KuroType.metricValue)
        Text(text = value, color = tokens.textPrimary, style = KuroType.metricValue)
        Text(text = label, color = tokens.textSecondary, style = KuroType.labelMeta, textAlign = TextAlign.Center)
    }
}

/** Tasks empty-state card. */
@Composable
fun KuroTasksEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroCard(modifier = modifier) {
        Text(text = title, color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(text = message, color = tokens.textSecondary, style = KuroType.bodySecondary)
    }
}

/** Active in-progress task card. */
@Composable
fun KuroTaskCard(
    task: FocusTask,
    subtitle: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .clickable(onClick = onTap)
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(KuroSpacing.taskLeadingTile)
                        .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                        .background(tokens.activeCardFill)
                        .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusSm)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = task.title.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        color = tokens.accent,
                        style = KuroType.metricValue,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
                    Text(text = task.title, color = tokens.textPrimary, style = KuroType.headingCard)
                    Text(text = subtitle, color = tokens.textSecondary, style = KuroType.bodySecondary)
                }
            }
            Text(
                text = "⋮",
                color = tokens.textFaint,
                style = KuroType.metricValue,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onTap)
                    .padding(
                        horizontal = KuroSpacing.taskMenuTapPaddingH,
                        vertical = KuroSpacing.taskMenuTapPaddingV,
                    ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(task.plannedSessions.coerceAtLeast(1)) { index ->
                    val filled = index < task.completedSessions
                    Box(
                        modifier = Modifier
                            .size(KuroSpacing.taskCycleDot)
                            .clip(CircleShape)
                            .background(if (filled) tokens.accent else tokens.cardFill)
                            .border(
                                KuroSpacing.hairline,
                                if (filled) tokens.accent else tokens.cardBorder,
                                CircleShape,
                            ),
                    )
                }
            }
            Text(
                text = "${task.completedSessions.coerceAtMost(task.plannedSessions)} / ${task.plannedSessions.coerceAtLeast(1)}",
                color = tokens.accent,
                style = KuroType.labelStrong,
            )
        }
    }
}

/** Completed task row. */
@Composable
fun KuroCompletedTaskRow(
    task: FocusTask,
    subtitle: String,
    onTap: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onTap)
            .padding(KuroSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.taskCompletedLeadingTile)
                .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                .background(tokens.activeCardFill)
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusSm)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = task.title.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                color = tokens.textSecondary,
                style = KuroType.headingCard,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
        ) {
            Text(
                text = task.title,
                color = tokens.textSecondary,
                style = KuroType.bodyPrimary,
                textDecoration = TextDecoration.LineThrough,
            )
            Text(text = subtitle, color = tokens.textFaint, style = KuroType.labelMeta)
        }
        Box(
            modifier = Modifier
                .size(KuroSpacing.taskCompletedCheck)
                .clip(CircleShape)
                .background(Mint)
                .clickable(role = Role.Checkbox) { onToggleComplete() }
                .semantics { role = Role.Checkbox },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", color = tokens.accentOn, style = KuroType.labelStrong)
        }
    }
}

/** Habits empty-state card with CTA. */
@Composable
fun KuroHabitsEmptyState(
    title: String,
    message: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusPill))
            .clickable(onClick = onAdd)
            .padding(KuroSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        leading?.invoke()
        Text(title, color = tokens.textPrimary, style = KuroType.headingCard)
        Text(message, color = tokens.textSecondary, style = KuroType.bodySecondary, textAlign = TextAlign.Center)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                .background(tokens.accent)
                .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text("Add habit", color = tokens.accentOn, style = KuroType.labelStrong)
        }
    }
}

/** Week dot row on a habit card. */
data class KuroHabitWeekDotUi(
    val label: String,
    val isScheduled: Boolean,
    val isCompleted: Boolean,
    val isToday: Boolean,
)

/** Calendar dot in the monthly habits progress card. */
data class KuroHabitMonthDotUi(
    val isCompleted: Boolean,
    val isToday: Boolean,
    val isPlaceholder: Boolean = false,
)

/** Single habit card with week progress dots. */
@Composable
fun KuroHabitCard(
    name: String,
    emoji: String,
    accentColor: Color,
    scheduleLabel: String,
    streak: Int,
    streakUnitLabel: String,
    weekDots: List<KuroHabitWeekDotUi>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .clickable(onClick = onClick)
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(KuroSpacing.habitEmojiTile)
                        .clip(RoundedCornerShape(KuroSpacing.md))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, style = KuroType.habitEmoji)
                }
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
                    Text(name, color = tokens.textPrimary, style = KuroType.headingCard)
                    Text(scheduleLabel, color = tokens.textSecondary, style = KuroType.labelMeta)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 4)) {
                Text(streak.toString(), color = tokens.accent, style = KuroType.habitStreakValue)
                Text(streakUnitLabel, color = tokens.textFaint, style = KuroType.labelMeta)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            weekDots.forEach { dot ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
                ) {
                    Text(dot.label, color = tokens.textFaint, style = KuroType.labelMeta)
                    Box(
                        modifier = Modifier
                            .size(KuroSpacing.habitWeekDot)
                            .clip(CircleShape)
                            .background(
                                when {
                                    dot.isCompleted -> Mint
                                    dot.isScheduled -> Mint.copy(alpha = 0.25f)
                                    else -> tokens.cardBorder.copy(alpha = 0.6f)
                                },
                            )
                            .border(
                                1.dp,
                                if (dot.isToday) tokens.textPrimary.copy(alpha = 0.20f) else Color.Transparent,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

/** Monthly habit completion heatmap card. */
@Composable
fun KuroMonthlyHabitProgressCard(
    monthLabel: String,
    rows: List<List<KuroHabitMonthDotUi>>,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Monthly Progress", color = tokens.textPrimary, style = KuroType.headingCard)
            Text(monthLabel, color = tokens.textSecondary, style = KuroType.labelMeta)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    day,
                    color = tokens.textFaint,
                    style = KuroType.labelMeta,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        rows.forEach { rowDots ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowDots.forEach { dot ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (!dot.isPlaceholder) {
                            Box(
                                modifier = Modifier
                                    .size(KuroSpacing.habitMonthDot)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            dot.isCompleted -> Mint
                                            dot.isToday -> Mint.copy(alpha = 0.4f)
                                            else -> tokens.cardBorder
                                        },
                                    ),
                            )
                        }
                    }
                }
                repeat(7 - rowDots.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Motivational quote card with cat illustration. */
@Composable
fun KuroHabitMotivationCard(
    quote: String,
    encouragement: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.activeCardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .padding(KuroSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.cat_face),
            contentDescription = null,
            modifier = Modifier.size(KuroSpacing.habitMotivationCat),
            contentScale = ContentScale.Fit,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            Text(quote, color = tokens.textPrimary, style = KuroType.bodyPrimary)
            Text(encouragement, color = tokens.accent, style = KuroType.labelStrong)
        }
    }
}
