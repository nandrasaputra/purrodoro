package com.endiar.purrodoro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Target
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.GlassTreatment
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.ui.designsystem.KuroElevatedCard
import com.endiar.purrodoro.ui.designsystem.KuroPillKind
import com.endiar.purrodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.JourneyGreen
import com.endiar.purrodoro.ui.theme.kuroTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal fun journeyStatusKind(completed: Boolean): KuroPillKind =
    if (completed) KuroPillKind.JourneyCompleted else KuroPillKind.JourneyPartial

/**
 * Journey tab — a seamless, read-only "focus story" that zooms out in time:
 * Today → This week → This month → All time. There is no period toggle and no
 * day picker; looking up a specific past day lives on the pushed History screen
 * (opened via the top-bar history icon).
 */
@Composable
fun CalendarScreen(
    sessionRecords: List<SessionRecord> = emptyList(),
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    @Suppress("UNUSED_PARAMETER") overBackdrop: Boolean = true,
) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val displayMonth = remember(today) { YearMonth.from(today) }

    val summary = remember(sessionRecords, today, zone) {
        journeySummary(records = sessionRecords, selectedDate = today, today = today, zone = zone)
    }
    val month = remember(sessionRecords, displayMonth, zone) {
        monthSummary(records = sessionRecords, month = displayMonth, zone = zone)
    }
    val intensities = remember(sessionRecords, zone) {
        focusIntensitiesByDate(records = sessionRecords, zone = zone)
    }
    val monthStreak = remember(intensities, displayMonth) {
        monthBestStreakDays(intensities, displayMonth)
    }
    val allTime = remember(sessionRecords, zone) {
        allTimeSummary(records = sessionRecords, zone = zone)
    }

    KuroScreenScaffold(
        stickyTitle = stringResource(R.string.journey_title),
        modifier = modifier,
        contentPadding = contentPadding,
    ) { scrollState, headerMeasureModifier ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = KuroSpacing.lg)
                .padding(top = KuroSpacing.lg, bottom = KuroSpacing.dockClearance)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg),
        ) {
            Box(headerMeasureModifier) {
                JourneyHeader(onOpenHistory = onOpenHistory)
            }
            JourneySection(label = stringResource(R.string.journey_today)) {
                TodayTimelineCard(summary.todayRows)
            }
            JourneySection(label = stringResource(R.string.journey_period_week)) {
                WeeklyFocusCard(summary)
            }
            JourneySection(label = stringResource(R.string.journey_period_month)) {
                MonthHeatmapCard(displayMonth = displayMonth, intensities = intensities, month = month, bestStreakDays = monthStreak)
            }
            JourneySection(label = stringResource(R.string.journey_section_all_time)) {
                AllTimeCard(allTime)
            }
        }
    }
}

@Composable
private fun JourneySection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
        JourneySectionLabel(label)
        content()
    }
}

@Composable
private fun JourneySectionLabel(text: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Text(
        text = text.uppercase(java.util.Locale.getDefault()),
        color = tokens.textSecondary,
        style = KuroType.labelCaption,
    )
}

@Composable
private fun JourneyHeader(onOpenHistory: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val openHistoryCd = stringResource(R.string.journey_open_history)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.journey_title),
            color = tokens.textPrimary,
            style = KuroType.headingDisplay,
        )
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxxl)
                .clip(CircleShape)
                .clickable(onClick = onOpenHistory)
                .semantics { role = Role.Button; contentDescription = openHistoryCd },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.History,
                contentDescription = null,
                tint = tokens.textPrimary,
                modifier = Modifier.size(KuroSpacing.lg),
            )
        }
    }
}

/** Material 3 emphasized-decelerate — calm arrival for entrance motion. */
private val JourneyEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
private fun WeeklyFocusCard(summary: JourneySummary) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    // One-shot entrance: bars grow up from the baseline in a left-to-right wave.
    // Vertical growth reads as "building up" the week; the per-bar delay stays
    // well under the skill's 500ms stagger budget (6 * 45ms = 270ms).
    var barsShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { barsShown = true }
    val weekNote = summary.weekOverWeekPercent?.let { pct ->
        stringResource(R.string.journey_week_over_week, if (pct >= 0) "+" else "", pct)
    } ?: stringResource(R.string.journey_no_last_week)
    val weekNoteColor = summary.weekOverWeekPercent?.takeIf { it > 0 }?.let { JourneyGreen }
        ?: tokens.textSecondary
    val noneLabel = stringResource(R.string.journey_stat_none)
    val avgLabel = if (summary.dailyAverageMinutes > 0) journeyMinutesLabel(summary.dailyAverageMinutes) else noneLabel
    val bestDayLabel = summary.weekBars
        .maxByOrNull { it.focusMinutes }
        ?.takeIf { it.focusMinutes > 0 }
        ?.dayLabel?.take(3) ?: noneLabel
    KuroElevatedCard {
        Text(stringResource(R.string.journey_focus_time_week), color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(KuroSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            Text(journeyMinutesLabel(summary.weekFocusMinutes), color = tokens.textPrimary, style = KuroType.metricValueLarge)
            Text(weekNote, color = weekNoteColor, style = KuroType.metricDeltaPositive)
        }
        Spacer(Modifier.height(KuroSpacing.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            JourneyStatChip(Lucide.Clock, stringResource(R.string.journey_stat_daily_avg), avgLabel, Modifier.weight(1f))
            JourneyStatChip(Lucide.Star, stringResource(R.string.journey_stat_best_day), bestDayLabel, Modifier.weight(1f))
        }
        Spacer(Modifier.height(KuroSpacing.md))
        val maxDayMinutes = summary.weekBars.maxOfOrNull { it.focusMinutes } ?: 0
        val scale = remember(maxDayMinutes) { journeyChartScale(maxDayMinutes) }
        Row(
            modifier = Modifier.fillMaxWidth().height(KuroSpacing.journeyChartPlotHeight),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChartAxis(scale)
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                val topMinutes = scale.topMinutes.coerceAtLeast(1)
                summary.weekBars.forEachIndexed { index, bar ->
                    // Floor non-zero days to a minimum visible fraction so a tiny
                    // day next to a huge one still reads as "something happened".
                    val ratio = if (bar.focusMinutes <= 0) {
                        0f
                    } else {
                        (bar.focusMinutes.toFloat() / topMinutes.toFloat()).coerceIn(0.04f, 1f)
                    }
                    val grow by animateFloatAsState(
                        targetValue = if (barsShown) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 520,
                            delayMillis = index * 45,
                            easing = JourneyEnterEasing,
                        ),
                        label = "weekBarGrow",
                    )
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        // Bar region: takes all height left above the day label, so a
                        // full-height bar can never push the label out of the plot.
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (bar.focusMinutes > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(KuroSpacing.journeyChartBarWidth)
                                        .fillMaxHeight((ratio * grow).coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(topStart = KuroSpacing.xs, topEnd = KuroSpacing.xs))
                                        .background(if (index == 6) tokens.accentSoft else tokens.accent),
                                )
                            }
                        }
                        Spacer(Modifier.height(KuroSpacing.xs))
                        Text(
                            bar.dayLabel.take(3),
                            color = tokens.textSecondary,
                            style = KuroType.labelMeta,
                            maxLines = 1,
                        )
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.cat_home_sleeping),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = KuroSpacing.journeySelectedDayCatWidth, height = KuroSpacing.journeyChartPlotHeight),
            )
        }
    }
}

/** Small bordered chip: icon + stacked label/value, used on the weekly card header. */
@Composable
private fun JourneyStatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(KuroSpacing.radiusSm),
        color = tokens.cardFill,
        border = BorderStroke(1.dp, tokens.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.md))
            Column {
                Text(label, color = tokens.textSecondary, style = KuroType.labelMeta)
                Text(value, color = tokens.textPrimary, style = KuroType.labelStrong)
            }
        }
    }
}

@Composable
private fun ChartAxis(scale: JourneyChartScale) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier.width(KuroSpacing.journeyChartAxisWidth).fillMaxHeight(),
        horizontalAlignment = Alignment.End,
    ) {
        // Tick region mirrors the bars' weighted plot area so "0" lines up with
        // the bar baseline.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text(journeyMinutesLabel(scale.topMinutes), color = tokens.textSecondary, style = KuroType.labelMeta, maxLines = 1)
            Text(journeyMinutesLabel(scale.midMinutes), color = tokens.textSecondary, style = KuroType.labelMeta, maxLines = 1)
            Text("0", color = tokens.textSecondary, style = KuroType.labelMeta, maxLines = 1)
        }
        // Reserve the same bottom slot the bars use for their day label so the
        // tick region height matches the bar region exactly.
        Spacer(Modifier.height(KuroSpacing.xs))
        Text("0", color = tokens.textSecondary, style = KuroType.labelMeta, modifier = Modifier.alpha(0f))
    }
}

@Composable
private fun MonthHeatmapCard(displayMonth: YearMonth, intensities: Map<LocalDate, Int>, month: JourneyMonth, bestStreakDays: Int) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    // One-shot diagonal wave: cells fade + scale in, delayed by their (row+col)
    // position so the month "develops" from the top-left corner. Diagonal span
    // is 5+6=11 steps * 18ms = ~200ms, keeping the wave under budget.
    var cellsShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cellsShown = true }
    KuroElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1.35f),
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(day, color = tokens.textSecondary, style = KuroType.labelMeta, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                val firstDay = displayMonth.atDay(1)
                val firstVisible = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
                repeat(6) { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                        repeat(7) { col ->
                            val date = firstVisible.plusDays((row * 7 + col).toLong())
                            val inMonth = YearMonth.from(date) == displayMonth
                            val reveal by animateFloatAsState(
                                targetValue = if (cellsShown) 1f else 0f,
                                animationSpec = tween(
                                    durationMillis = 280,
                                    delayMillis = if (inMonth) (row + col) * 18 else 0,
                                    easing = JourneyEnterEasing,
                                ),
                                label = "heatCellReveal",
                            )
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .then(if (inMonth) Modifier.scale(0.7f + 0.3f * reveal).alpha(reveal) else Modifier)
                                        .clip(RoundedCornerShape(KuroSpacing.xs))
                                        .background(if (inMonth) heatCellColor(intensities[date] ?: 0) else Color.Transparent),
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                MonthStat(Lucide.Calendar, month.focusDays.toString(), stringResource(R.string.journey_stat_focus_days))
                MonthStatDivider()
                MonthStat(Lucide.Clock, month.focusTimeLabel, stringResource(R.string.journey_all_time_total))
                MonthStatDivider()
                MonthStat(Lucide.Flame, bestStreakDays.toString(), stringResource(R.string.journey_stat_streak))
            }
        }
    }
}

@Composable
private fun MonthStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.md))
        Column {
            Text(value, color = tokens.textPrimary, style = KuroType.metricValue)
            Text(label, color = tokens.textSecondary, style = KuroType.labelMeta)
        }
    }
}

@Composable
private fun MonthStatDivider() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(tokens.divider),
    )
}

@Composable
private fun heatCellColor(level: Int): Color {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    return when (level) {
        1 -> tokens.accent.copy(alpha = 0.22f)
        2 -> tokens.accent.copy(alpha = 0.45f)
        3 -> tokens.accent.copy(alpha = 0.70f)
        4 -> tokens.accent
        else -> tokens.cardBorder.copy(alpha = 0.45f)
    }
}

@Composable
private fun AllTimeCard(allTime: JourneyAllTime) {
    KuroElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                AllTimeStat(Lucide.Clock, allTime.totalFocusLabel, stringResource(R.string.journey_all_time_total))
                AllTimeStat(Lucide.Flame, allTime.bestStreakDays.toString(), stringResource(R.string.journey_all_time_streak))
                AllTimeStat(Lucide.Target, allTime.totalSessions.toString(), stringResource(R.string.journey_all_time_sessions))
            }
            Image(
                painter = painterResource(R.drawable.cat_celebrate),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = KuroSpacing.journeyChartCatWidth, height = KuroSpacing.journeyChartPlotHeight),
            )
        }
    }
}

@Composable
private fun AllTimeStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Surface(shape = CircleShape, color = tokens.accentSoft, modifier = Modifier.size(KuroSpacing.journeyMetricIconCircle)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.journeyMetricIconGlyph))
            }
        }
        Text(value, color = tokens.textPrimary, style = KuroType.metricValueLarge)
        Text(label, color = tokens.textSecondary, style = KuroType.labelMeta, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TodayTimelineCard(rows: List<JourneyTimelineRow>) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        if (rows.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
                Image(
                    painter = painterResource(R.drawable.cat_today_empty),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(KuroSpacing.journeyChartCatWidth),
                )
                Text(
                    stringResource(R.string.journey_no_records_today),
                    color = tokens.textSecondary,
                    style = KuroType.bodySecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            JourneyTimelineListCollapsible(rows = rows)
        }
    }
}
