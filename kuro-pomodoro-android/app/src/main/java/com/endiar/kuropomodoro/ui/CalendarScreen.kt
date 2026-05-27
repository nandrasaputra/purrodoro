package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Target
import com.composables.icons.lucide.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.ui.designsystem.KuroJourneyMetricCard
import com.endiar.kuropomodoro.ui.designsystem.KuroBottomSheet
import com.endiar.kuropomodoro.ui.designsystem.KuroElevatedCard
import com.endiar.kuropomodoro.ui.designsystem.KuroPill
import com.endiar.kuropomodoro.ui.designsystem.KuroPillKind
import com.endiar.kuropomodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.kuropomodoro.ui.designsystem.KuroSegmentedControl
import com.endiar.kuropomodoro.ui.designsystem.KuroSegmentedControlStyle
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.JourneyGreen
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private enum class JourneyPeriod(val label: String) {
    Week("This week"),
    Month("This month"),
    Year("This year"),
}

private fun journeyStatusKind(label: String): KuroPillKind =
    if (label == "Completed") KuroPillKind.JourneyCompleted else KuroPillKind.JourneyPartial

@Composable
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    sessionRecords: List<SessionRecord> = emptyList(),
    onDeleteRecord: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    @Suppress("UNUSED_PARAMETER") overBackdrop: Boolean = true,
) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    var selectedDate by remember { mutableStateOf(today) }
    var displayMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedPeriod by remember { mutableStateOf(JourneyPeriod.Week) }
    var selectedRecord by remember { mutableStateOf<SessionRecord?>(null) }
    val summary = remember(sessionRecords, selectedDate, today, zone) {
        journeySummary(records = sessionRecords, selectedDate = selectedDate, today = today, zone = zone)
    }

    val tokens = MaterialTheme.colorScheme.kuroTokens

    KuroScreenScaffold(
        stickyTitle = "Journey",
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
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Box(headerMeasureModifier) {
                JourneyHeader()
            }
            KuroSegmentedControl(
                options = JourneyPeriod.entries.map { it.label },
                selectedIndex = JourneyPeriod.entries.indexOf(selectedPeriod),
                onSelect = { selectedPeriod = JourneyPeriod.entries[it] },
                style = KuroSegmentedControlStyle.Outlined,
            )
            val weekNote = summary.weekOverWeekPercent?.let { pct ->
                "${if (pct >= 0) "+" else ""}$pct% vs last week"
            } ?: "No last week yet"
            val weekNoteColor = summary.weekOverWeekPercent?.takeIf { it > 0 }?.let { JourneyGreen }
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
                KuroJourneyMetricCard(
                    title = selectedPeriod.label,
                    value = journeyMinutesLabel(summary.weekFocusMinutes),
                    note = weekNote,
                    icon = Lucide.Clock,
                    iconTint = tokens.accent,
                    iconBackground = tokens.accentSoft,
                    noteColor = weekNoteColor,
                    modifier = Modifier.weight(1f),
                )
                KuroJourneyMetricCard(
                    title = "Focus days",
                    value = "${summary.focusDays} / 7",
                    note = "You focused on ${summary.focusDays} days",
                    icon = Lucide.Calendar,
                    iconTint = JourneyGreen,
                    iconBackground = JourneyGreen.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
                KuroJourneyMetricCard(
                    title = "Daily average",
                    value = journeyMinutesLabel(summary.dailyAverageMinutes),
                    note = "Goal 1h 30m",
                    icon = Lucide.Timer,
                    iconTint = JourneyGreen,
                    iconBackground = JourneyGreen.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f),
                )
                KuroJourneyMetricCard(
                    title = "Weekly goal",
                    value = "${summary.weeklyGoalPercent}%",
                    note = "${journeyMinutesLabel(summary.weekFocusMinutes)} / ${journeyMinutesLabel(summary.weeklyGoalMinutes)}",
                    icon = Lucide.Target,
                    iconTint = tokens.accent,
                    iconBackground = tokens.accentSoft,
                    trailing = { GoalRing(summary.weeklyGoalPercent / 100f) },
                    modifier = Modifier.weight(1f),
                )
            }
            WeeklyFocusCard(summary)
            FocusCalendarCard(
                displayMonth = displayMonth,
                selectedDate = selectedDate,
                today = today,
                stamps = stamps,
                records = sessionRecords,
                summary = summary,
                zone = zone,
                onPreviousMonth = { displayMonth = displayMonth.minusMonths(1) },
                onNextMonth = { displayMonth = displayMonth.plusMonths(1) },
                onSelectedDate = { selectedDate = it },
            )
            TodayTimelineCard(summary.todayRows) { selectedRecord = it.record }
        }
    }

    selectedRecord?.let { record ->
        JourneyRecordSheet(
            record = record,
            onDismiss = { selectedRecord = null },
            onDelete = {
                onDeleteRecord(it)
                selectedRecord = null
            }
        )
    }
}

@Composable
private fun JourneyHeader() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
            Text(
                text = "Journey",
                color = tokens.textPrimary,
                fontFamily = FontFamily.Serif,
                style = KuroType.headingJourney,
            )
            Text(text = "Your focus journey.", color = tokens.textSecondary, style = KuroType.bodySecondary)
        }
        Surface(
            modifier = Modifier.size(KuroSpacing.journeyHeaderAvatar).shadow(KuroSpacing.sm, CircleShape),
            shape = CircleShape,
            color = tokens.accentSoft,
            border = BorderStroke(1.dp, tokens.pageBackground),
        ) {
            Image(
                painter = painterResource(R.drawable.cat_face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.padding(KuroSpacing.xs),
            )
        }
    }
}

@Composable
private fun GoalRing(progress: Float) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Canvas(modifier = Modifier.size(KuroSpacing.journeyGoalRingSize)) {
        val stroke = Stroke(width = KuroSpacing.xs.toPx() + 1f, cap = StrokeCap.Round)
        drawArc(tokens.cardBorder, -90f, 360f, false, style = stroke, size = Size(size.width, size.height))
        drawArc(tokens.accent, -90f, 360f * progress.coerceIn(0f, 1f), false, style = stroke, size = Size(size.width, size.height))
    }
}

@Composable
private fun WeeklyFocusCard(summary: JourneySummary) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Text("Focus time this week", color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(KuroSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth().height(KuroSpacing.journeyChartHeight),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChartAxis()
            Row(
                modifier = Modifier.weight(1f).height(KuroSpacing.journeyChartPlotHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                val maxMinutes = maxOf(180, summary.weekBars.maxOfOrNull { it.focusMinutes } ?: 0)
                summary.weekBars.forEachIndexed { index, bar ->
                    val ratio = if (maxMinutes == 0) 0f else bar.focusMinutes.toFloat() / maxMinutes.toFloat()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        if (bar.focusMinutes > 0) {
                            Box(
                                modifier = Modifier
                                    .width(KuroSpacing.journeyChartBarWidth)
                                    .height(KuroSpacing.journeyChartBarMaxHeight * ratio)
                                    .clip(RoundedCornerShape(topStart = KuroSpacing.xs, topEnd = KuroSpacing.xs))
                                    .background(if (index == 6) tokens.accentSoft else tokens.accent),
                            )
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .width(KuroSpacing.journeyChartBarWidth)
                                    .height(KuroSpacing.xs / 2),
                            )
                        }
                        Spacer(Modifier.height(KuroSpacing.sm))
                        Text(bar.dayLabel.take(3), color = tokens.textSecondary, style = KuroType.labelMeta)
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.cat_home_sleeping),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = KuroSpacing.journeyChartCatWidth, height = KuroSpacing.journeyChartPlotHeight),
            )
        }
    }
}

@Composable
private fun ChartAxis() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier.width(KuroSpacing.journeyChartAxisWidth).height(KuroSpacing.journeyChartAxisHeight),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        Text("3h", color = tokens.textSecondary, style = KuroType.labelMeta)
        Text("1.5h", color = tokens.textSecondary, style = KuroType.labelMeta)
        Text("0", color = tokens.textSecondary, style = KuroType.labelMeta)
    }
}

@Composable
private fun FocusCalendarCard(
    displayMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    stamps: Map<LocalDate, Int>,
    records: List<SessionRecord>,
    summary: JourneySummary,
    zone: ZoneId,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectedDate: (LocalDate) -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Focus calendar", color = tokens.textPrimary, style = KuroType.headingCard)
            Row(
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${displayMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${displayMonth.year}",
                    color = tokens.textPrimary,
                    style = KuroType.labelStrong,
                )
                Text("<", color = tokens.textSecondary, style = KuroType.calendarNav, modifier = Modifier.clickable { onPreviousMonth() })
                Text(">", color = tokens.textSecondary, style = KuroType.calendarNav, modifier = Modifier.clickable { onNextMonth() })
            }
        }
        Spacer(Modifier.height(KuroSpacing.md))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(day, color = tokens.textSecondary, style = KuroType.labelMeta, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(KuroSpacing.sm))
        CalendarGrid(displayMonth, selectedDate, today, stamps, records, zone, onSelectedDate)
        Spacer(Modifier.height(KuroSpacing.md))
        SelectedDayStrip(summary)
    }
}

@Composable
private fun CalendarGrid(
    displayMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    stamps: Map<LocalDate, Int>,
    records: List<SessionRecord>,
    zone: ZoneId,
    onSelectedDate: (LocalDate) -> Unit
) {
    val firstDay = displayMonth.atDay(1)
    val firstVisible = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
    val recordsByDate = remember(records, zone) {
        records.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.journeyCalendarRowGap)) {
        repeat(6) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val date = firstVisible.plusDays((row * 7 + col).toLong())
                    CalendarDayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == displayMonth,
                        selected = date == selectedDate,
                        today = date == today,
                        hasFocus = (stamps[date] ?: 0) > 0 || recordsByDate[date].orEmpty().isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectedDate(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    hasFocus: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val bg = when {
        selected -> tokens.accent
        today -> tokens.activeCardFill
        hasFocus -> tokens.accentSoft
        else -> Color.Transparent
    }
    val textColor = when {
        selected -> tokens.accentOn
        !inMonth -> tokens.textFaint
        else -> tokens.textPrimary
    }
    Box(modifier = modifier.height(KuroSpacing.journeyCalendarDayOuter), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.journeyCalendarDaySize)
                .clip(CircleShape)
                .background(bg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = textColor,
                style = if (selected || today) KuroType.labelStrong else KuroType.bodySecondary
            )
        }
        if (hasFocus && !selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(KuroSpacing.journeyCalendarFocusDot)
                    .clip(CircleShape)
                    .background(if (inMonth) tokens.accent else tokens.textFaint)
            )
        }
    }
}

@Composable
private fun SelectedDayStrip(summary: JourneySummary) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KuroSpacing.radiusMd),
        color = tokens.activeCardFill,
        border = BorderStroke(1.dp, tokens.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = KuroSpacing.journeySelectedDayStripPaddingH,
                vertical = KuroSpacing.journeySelectedDayStripPaddingV,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.cat_home_sleeping),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(
                    width = KuroSpacing.journeySelectedDayCatWidth,
                    height = KuroSpacing.journeySelectedDayCatHeight,
                ),
            )
            Spacer(Modifier.width(KuroSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                Text(
                    "${summary.selectedDay.dateLabel} • ${summary.selectedDay.focusTimeLabel} • ${summary.selectedDay.focusSessions} sessions",
                    color = tokens.textPrimary,
                    style = KuroType.bodyPrimary,
                )
                Text("Tap a day to see details", color = tokens.textSecondary, style = KuroType.bodySecondary)
            }
        }
    }
}

@Composable
private fun TodayTimelineCard(rows: List<JourneyTimelineRow>, onRowClick: (JourneyTimelineRow) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Text("Today", color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(KuroSpacing.md))
        if (rows.isEmpty()) {
            Text("No focus records yet today.", color = tokens.textSecondary, style = KuroType.labelStrong)
        } else {
            rows.forEachIndexed { index, row ->
                TimelineRow(row, first = index == 0, last = index == rows.lastIndex) { onRowClick(row) }
            }
        }
    }
}

@Composable
private fun TimelineRow(row: JourneyTimelineRow, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(KuroSpacing.radiusSm)).clickable(onClick = onClick).padding(vertical = KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineRail(first, last)
        Text(
            row.timeLabel,
            color = tokens.textSecondary,
            style = KuroType.labelStrong,
            modifier = Modifier.width(KuroSpacing.journeyTimelineTimeWidth),
        )
        val taskName = row.taskName?.takeIf { it.isNotBlank() }
        if (taskName == null) {
            Spacer(Modifier.weight(1f))
        } else {
            Text(taskName, color = tokens.textPrimary, style = KuroType.bodyPrimary, modifier = Modifier.weight(1f))
        }
        Text(row.durationLabel, color = tokens.textPrimary, style = KuroType.bodySecondary)
        Spacer(Modifier.width(KuroSpacing.sm))
        KuroPill(label = row.statusLabel, kind = journeyStatusKind(row.statusLabel))
    }
}

@Composable
private fun TimelineRail(first: Boolean, last: Boolean) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .width(KuroSpacing.journeyTimelineRailWidth)
            .height(KuroSpacing.journeyTimelineRailHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (!first) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .width(1.dp)
                    .height(KuroSpacing.journeyTimelineConnector)
                    .background(tokens.textFaint),
            )
        }
        if (!last) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(1.dp)
                    .height(KuroSpacing.journeyTimelineConnector)
                    .background(tokens.textFaint),
            )
        }
        Box(
            Modifier
                .size(KuroSpacing.journeyTimelineDot)
                .clip(CircleShape)
                .background(tokens.cardFill)
                .border(KuroSpacing.xs / 2, tokens.textFaint, CircleShape),
        )
    }
}

@Composable
private fun JourneyRecordSheet(record: SessionRecord, onDismiss: () -> Unit, onDelete: (Long) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val start = Instant.ofEpochMilli(record.startedAt).atZone(zone).format(timeFormatter)
    val end = Instant.ofEpochMilli(record.endedAt).atZone(zone).format(timeFormatter)
    val focusMinutes = record.completedFocusCount.coerceAtLeast(0) * record.focusMinutes.coerceAtLeast(0)
    val restMinutes = record.completedShortBreakCount * record.shortBreakMinutes + record.completedLongBreakCount * record.longBreakMinutes

    KuroBottomSheet(title = "Journey detail", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$start - $end", color = tokens.textSecondary, style = KuroType.labelStrong)
                KuroPill(
                    label = if (record.status == SessionStatus.Completed) "Completed" else "Partial",
                    kind = journeyStatusKind(if (record.status == SessionStatus.Completed) "Completed" else "Partial"),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SheetStat("Focused", journeyMinutesLabel(focusMinutes))
                SheetDivider()
                SheetStat("Sessions", "${record.completedFocusCount}/${record.totalFocusCount}")
                SheetDivider()
                SheetStat("Rest", journeyMinutesLabel(restMinutes))
            }
            record.taskName?.takeIf { it.isNotBlank() }?.let {
                Surface(shape = RoundedCornerShape(KuroSpacing.radiusSm), color = tokens.cardFill, border = BorderStroke(1.dp, tokens.cardBorder)) {
                    Text(it, color = tokens.textPrimary, style = KuroType.bodyPrimary, modifier = Modifier.padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuroSpacing.radiusLg))
                    .background(tokens.destructive.copy(alpha = 0.10f))
                    .border(1.dp, tokens.destructive.copy(alpha = 0.30f), RoundedCornerShape(KuroSpacing.radiusLg))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = KuroSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text("Delete Record", color = tokens.destructive, style = KuroType.bodyPrimary)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this record?", color = tokens.textPrimary, style = KuroType.headingCard) },
            text = { Text("The session from $start to $end will be permanently removed.", color = tokens.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(record.id)
                }) {
                    Text("Delete", color = tokens.destructive, style = KuroType.labelStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = tokens.textSecondary)
                }
            },
            containerColor = tokens.pageBackground
        )
    }
}

@Composable
private fun SheetStat(label: String, value: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
        Text(label, color = tokens.textSecondary, style = KuroType.labelMeta)
        Text(value, color = tokens.textPrimary, style = KuroType.headingCard)
    }
}

@Composable
private fun SheetDivider() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(KuroSpacing.journeySheetStatDividerHeight)
            .background(tokens.cardBorder),
    )
}
