package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Flag
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tag
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TaskPriority
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.launch

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

/** Tasks empty-state card. */
@Composable
fun KuroTasksEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroCard(modifier = modifier.fillMaxWidth()) {
        // KuroCard draws its border around this content column, so the content
        // itself must fill the width for the card to read as full-bleed (the
        // text alone would otherwise leave the bordered box wrapping just the
        // text).
        Text(
            text = title,
            color = tokens.textPrimary,
            style = KuroType.headingCard,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            text = message,
            color = tokens.textSecondary,
            style = KuroType.bodySecondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Active in-progress task row.
 *
 * Decoupled from Pomodoro after the redesign: no per-task pomodoro target, no
 * dot row, no `X / Y` counter. The row reads as a calm todo: leading checkbox,
 * a category color dot for scanning, title, optional category subtitle.
 *
 * [subtitle] is null when the surrounding screen has filtered to a single
 * category (printing the category name on every row would be noise).
 *
 * [isLive] paints a 1.5dp accent ring around the color dot — the quiet "this
 * task is currently attached to a running Pomodoro" cue. No timer text, no
 * pill; just enough for users to find the live task at a glance.
 */
@Composable
fun KuroTaskCard(
    task: FocusTask,
    subtitle: String?,
    onTap: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    isLive: Boolean = false,
    categoryColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scope = rememberCoroutineScope()
    // Completing a task moves the row to the "done" list, which would unmount it
    // instantly. We hold the row in place for the ~220ms it takes the check to
    // draw + the circle to fill, so the gesture reads as a satisfying "stamp"
    // before the row departs. `completing` guards against double taps.
    val checkProgress = remember { Animatable(0f) }
    var completing by remember { mutableStateOf(false) }
    val checkboxInteraction = remember { MutableInteractionSource() }
    val fillColor = lerp(tokens.cardFill, tokens.accent, checkProgress.value)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onTap)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md - KuroSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md - KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .pressScale(checkboxInteraction, pressedScale = 0.86f)
                .scale(1f + 0.12f * checkProgress.value)
                .size(KuroSpacing.taskCheckbox)
                .clip(CircleShape)
                .background(fillColor)
                .border(KuroSpacing.hairline + KuroSpacing.hairline / 5, tokens.accent, CircleShape)
                .clickable(
                    role = Role.Checkbox,
                    interactionSource = checkboxInteraction,
                    indication = null,
                ) {
                    if (!completing) {
                        completing = true
                        scope.launch {
                            checkProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 220,
                                    easing = LinearOutSlowInEasing,
                                ),
                            )
                            onToggleComplete()
                        }
                    }
                }
                .semantics { role = Role.Checkbox },
            contentAlignment = Alignment.Center,
        ) {
            if (checkProgress.value > 0f) {
                KuroAnimatedCheck(
                    progress = checkProgress.value,
                    color = tokens.accentOn,
                    modifier = Modifier.size(KuroSpacing.taskCheckbox * 0.62f),
                )
            }
        }
        // The leading dot is now reserved purely for the "live" cue (this task is
        // attached to a running Pomodoro). The category color moved into the meta
        // line's tag icon, so a non-live row no longer carries a redundant dot.
        if (isLive) {
            CategoryColorDot(
                color = categoryColor ?: categoryColorFor(task.categoryId, fallback = tokens.textMuted),
                isLive = true,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
        ) {
            Text(
                text = task.title,
                color = tokens.textPrimary,
                style = KuroType.bodyPrimary,
            )
            TaskRowMeta(
                task = task,
                subtitle = subtitle,
                categoryColor = categoryColor ?: categoryColorFor(task.categoryId, fallback = tokens.textMuted),
                today = today,
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

private val taskRowDueFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/**
 * Quiet single-line metadata under a task title: category (when not already
 * implied by a filter), due date, and a High-priority flag. Mirrors the Next
 * Focus hero's metadata row so list rows and the hero speak the same language.
 *
 * Due date is always surfaced when present — color carries urgency (overdue =
 * destructive, today = accent, otherwise muted) while the label keeps the
 * actual date visible for anything past/future.
 */
@Composable
private fun TaskRowMeta(
    task: FocusTask,
    subtitle: String?,
    categoryColor: Color,
    today: LocalDate,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val due = task.dueDate
    // Only show the High flag inline when there's no date competing for the
    // row's attention; a dated High task already reads as urgent.
    val showHighFlag = task.priority == TaskPriority.High && due == null
    if (subtitle == null && due == null && !showHighFlag) return
    // No "•" separators between groups anymore — they read as cramped on rows
    // that mix a category, a date, and a flag. We lean on spacing instead.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        if (subtitle != null) {
            TaskMetaChip(
                icon = Lucide.Tag,
                iconTint = categoryColor,
                text = subtitle,
                textColor = tokens.textSecondary,
            )
        }
        if (due != null) {
            val dueColor = when {
                due.isBefore(today) -> tokens.destructive
                due == today -> tokens.accent
                else -> tokens.textMuted
            }
            val dueLabel = when {
                due == today -> "Today"
                due == today.plusDays(1) -> "Tomorrow"
                else -> due.format(taskRowDueFormatter)
            }
            TaskMetaChip(
                icon = Lucide.CalendarDays,
                iconTint = dueColor,
                text = dueLabel,
                textColor = dueColor,
            )
        }
        if (showHighFlag) {
            TaskMetaChip(
                icon = Lucide.Flag,
                iconTint = tokens.accent,
                text = "High",
                textColor = tokens.accent,
            )
        }
    }
}

/** A single icon + label metadata group for a task row's secondary line. */
@Composable
private fun TaskMetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String,
    textColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(12.dp),
        )
        Text(text = text, color = textColor, style = KuroType.labelMeta)
    }
}

/**
 * Completed task row.
 *
 * Mint check is the dominant glyph (this row IS a "done" stamp). The category
 * color dot still renders, but at reduced opacity so it doesn't compete with
 * the strikethrough title. Title is single-line strikethrough; subtitle hides
 * when the screen is filtered to a single category.
 */
@Composable
fun KuroCompletedTaskRow(
    task: FocusTask,
    subtitle: String?,
    onTap: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    categoryColor: Color? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onTap)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md - KuroSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md - KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            if (subtitle != null) {
                val tagTint = (categoryColor
                    ?: categoryColorFor(task.categoryId, fallback = tokens.textMuted))
                    .copy(alpha = 0.55f)
                TaskMetaChip(
                    icon = Lucide.Tag,
                    iconTint = tagTint,
                    text = subtitle,
                    textColor = tokens.textFaint,
                )
            }
        }
    }
}

/**
 * Small color dot used to denote a task's category on the list rows.
 *
 * When [isLive] is true, an accent ring is drawn around the dot — the running-
 * task indicator. Scoped to this file so the dot's geometry stays consistent
 * across active and completed cards, but [categoryColorFor] is exported for
 * other callsites that want to surface category colors elsewhere (e.g.
 * inline-add chips, detail sheet preview).
 */
@Composable
private fun CategoryColorDot(color: Color, isLive: Boolean) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier.size(KuroSpacing.taskCategoryDotRing),
        contentAlignment = Alignment.Center,
    ) {
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.taskCategoryDotRing)
                    .clip(CircleShape)
                    .border(KuroSpacing.hairline, tokens.accent, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(KuroSpacing.taskCategoryDot)
                .clip(CircleShape)
                .background(color),
        )
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

/** Today-status for a habit row on the list. */
enum class KuroHabitTodayStatus { Pending, Done, RestDay }

/**
 * Habit list row (hybrid status card). Shows the habit identity, an inline
 * streak chip + state-aware subtitle, and a trailing tap-to-complete circle for
 * habits due today. Rest-day habits show no circle (their subtitle carries the
 * "next due" hint instead), so the eye skips them.
 *
 * Two hit areas: the trailing circle toggles today's completion ([onToggleDone]),
 * the rest of the row opens detail ([onClick]).
 */
@Composable
fun KuroHabitStatusCard(
    name: String,
    emoji: String,
    accentColor: Color,
    streak: Int,
    statusLabel: String,
    status: KuroHabitTodayStatus,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val statusColor = when (status) {
        KuroHabitTodayStatus.Done -> tokens.accent
        KuroHabitTodayStatus.Pending -> tokens.textSecondary
        KuroHabitTodayStatus.RestDay -> tokens.textFaint
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .clickable(onClick = onClick)
            .padding(KuroSpacing.lg),
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        ) {
            Text(name, color = tokens.textPrimary, style = KuroType.headingCard)
            Row(
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (streak > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                            .background(tokens.accent.copy(alpha = 0.12f))
                            .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs / 2),
                    ) {
                        Text("🔥 $streak", color = tokens.accent, style = KuroType.labelMeta)
                    }
                    Text("·", color = tokens.textFaint, style = KuroType.labelMeta)
                }
                Text(statusLabel, color = statusColor, style = KuroType.labelMeta)
            }
        }
        when (status) {
            KuroHabitTodayStatus.Done -> Box(
                modifier = Modifier
                    .size(KuroSpacing.habitCheckCircle)
                    .clip(CircleShape)
                    .background(Mint)
                    .clickable(onClick = onToggleDone)
                    .semantics { role = Role.Checkbox },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = "Mark not done",
                    tint = Color.White,
                    modifier = Modifier.size(KuroSpacing.habitCheckGlyph),
                )
            }
            KuroHabitTodayStatus.Pending -> Box(
                modifier = Modifier
                    .size(KuroSpacing.habitCheckCircle)
                    .clip(CircleShape)
                    .border(KuroSpacing.habitColorSwatchRing, accentColor, CircleShape)
                    .clickable(onClick = onToggleDone)
                    .semantics { role = Role.Checkbox },
            )
            KuroHabitTodayStatus.RestDay -> Unit
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
