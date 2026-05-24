package com.endiar.kuropomodoro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// Intentional brand color — no equivalent token (green productivity icon accent)
private val JourneyGreen = Color(0xFF8BA05E)

private enum class JourneyPeriod(val label: String) {
    Week("This week"),
    Month("This month"),
    Year("This year")
}

@Composable
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    sessionRecords: List<SessionRecord> = emptyList(),
    onDeleteRecord: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overBackdrop: Boolean = true,
) {
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val showStickyBar by remember {
        derivedStateOf { scrollState.value > headerHeightPx + with(density) { 8.dp.roundToPx() } }
    }
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
    Box(modifier = modifier.fillMaxSize().background(tokens.pageBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = KuroSpacing.xl)
                .padding(top = KuroSpacing.lg, bottom = 112.dp)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            JourneyHeader(Modifier.onSizeChanged { headerHeightPx = it.height })
            JourneySegmentedControl(selectedPeriod) { selectedPeriod = it }
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
                MetricCard(
                    icon = "T",
                    iconColor = tokens.accent,
                    title = selectedPeriod.label,
                    value = journeyMinutesLabel(summary.weekFocusMinutes),
                    note = summary.weekOverWeekPercent?.let { "${if (it >= 0) "+" else ""}$it% vs last week" } ?: "No last week yet",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = "D",
                    iconColor = JourneyGreen,
                    title = "Focus days",
                    value = "${summary.focusDays} / 7",
                    note = "You focused on ${summary.focusDays} days",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
                MetricCard(
                    icon = "A",
                    iconColor = JourneyGreen,
                    title = "Daily average",
                    value = journeyMinutesLabel(summary.dailyAverageMinutes),
                    note = "Goal 1h 30m",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = "%",
                    iconColor = tokens.accent,
                    title = "Weekly goal",
                    value = "${summary.weeklyGoalPercent}%",
                    note = "${journeyMinutesLabel(summary.weekFocusMinutes)} / ${journeyMinutesLabel(summary.weeklyGoalMinutes)}",
                    trailing = { GoalRing(summary.weeklyGoalPercent / 100f) },
                    modifier = Modifier.weight(1f)
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
                onSelectedDate = { selectedDate = it }
            )
            TodayTimelineCard(summary.todayRows) { selectedRecord = it.record }
        }

        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it },
        ) {
            ScreenStickyBar(title = "Journey")
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
private fun JourneyHeader(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
            // Intentional exception: 42sp Serif display heading — brand personality
            Text("Journey", color = tokens.textPrimary, fontFamily = FontFamily.Serif, fontSize = 42.sp)
            Text("Your focus journey.", color = tokens.textSecondary, style = KuroType.headingCard)
        }
        Surface(
            modifier = Modifier.size(64.dp).shadow(KuroSpacing.sm, CircleShape),
            shape = CircleShape,
            color = tokens.accentSoft,
            border = BorderStroke(1.dp, Color.White)
        ) {
            Image(
                painter = painterResource(R.drawable.cat_face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.padding(KuroSpacing.xs)
            )
        }
    }
}

@Composable
private fun JourneySegmentedControl(selected: JourneyPeriod, onSelected: (JourneyPeriod) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Surface(
        modifier = Modifier.fillMaxWidth().height(46.dp).shadow(KuroSpacing.sm, RoundedCornerShape(KuroSpacing.radiusPill)),
        shape = RoundedCornerShape(KuroSpacing.radiusPill),
        color = tokens.cardFill,
        border = BorderStroke(1.dp, tokens.cardBorder)
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            JourneyPeriod.entries.forEach { period ->
                val active = selected == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                        .background(if (active) tokens.activeCardFill else Color.Transparent)
                        .border(if (active) 1.dp else 0.dp, if (active) tokens.borderSoft else Color.Transparent, RoundedCornerShape(KuroSpacing.radiusMd))
                        .clickable { onSelected(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(period.label, color = if (active) tokens.accent else tokens.textSecondary, style = KuroType.labelStrong)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: String,
    iconColor: Color,
    title: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    JourneyCard(modifier = modifier.height(118.dp), contentPadding = PaddingValues(KuroSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Intentional: large icon letter, 30sp ExtraBold brand accent
            Text(icon, color = iconColor, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(KuroSpacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = tokens.textPrimary, style = KuroType.headingCard)
                // Intentional: Serif large value, brand personality
                Text(value, color = tokens.textPrimary, fontFamily = FontFamily.Serif, fontSize = 30.sp)
                Text(note, color = tokens.textSecondary, style = KuroType.labelMeta)
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun GoalRing(progress: Float) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Canvas(modifier = Modifier.size(46.dp)) {
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        drawArc(tokens.cardBorder, -90f, 360f, false, style = stroke, size = Size(size.width, size.height))
        drawArc(tokens.accent, -90f, 360f * progress.coerceIn(0f, 1f), false, style = stroke, size = Size(size.width, size.height))
    }
}

@Composable
private fun WeeklyFocusCard(summary: JourneySummary) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    JourneyCard {
        Text("Focus time this week", color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth().height(104.dp), verticalAlignment = Alignment.Bottom) {
            ChartAxis()
            Row(
                modifier = Modifier.weight(1f).height(96.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxMinutes = maxOf(180, summary.weekBars.maxOfOrNull { it.focusMinutes } ?: 0)
                summary.weekBars.forEachIndexed { index, bar ->
                    val ratio = if (maxMinutes == 0) 0f else bar.focusMinutes.toFloat() / maxMinutes.toFloat()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((80.dp * ratio).coerceAtLeast(if (bar.focusMinutes > 0) 8.dp else 2.dp))
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(if (index == 6) tokens.accentSoft else tokens.accent)
                        )
                        Spacer(Modifier.height(KuroSpacing.sm))
                        Text(bar.dayLabel.take(3), color = tokens.textSecondary, style = KuroType.labelMeta)
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.cat_home_sleeping),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 78.dp, height = 96.dp)
            )
        }
    }
}

@Composable
private fun ChartAxis() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier.width(34.dp).height(86.dp),
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
    JourneyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Focus calendar", color = tokens.textPrimary, style = KuroType.headingCard)
            Text(
                "${displayMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${displayMonth.year}",
                color = tokens.textPrimary,
                style = KuroType.headingCard
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("<", color = tokens.textSecondary, fontSize = 24.sp, modifier = Modifier.clickable { onPreviousMonth() })
                Text(">", color = tokens.textSecondary, fontSize = 24.sp, modifier = Modifier.clickable { onNextMonth() })
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
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
    Box(modifier = modifier.height(30.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
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
                    .size(4.dp)
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
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.cat_home_sleeping),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 72.dp, height = 48.dp)
            )
            Spacer(Modifier.width(KuroSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${summary.selectedDay.dateLabel} - ${summary.selectedDay.focusTimeLabel} - ${summary.selectedDay.focusSessions} sessions",
                    color = tokens.textPrimary,
                    style = KuroType.bodyPrimary
                )
                Text("Tap a day to see details", color = tokens.textSecondary, style = KuroType.bodySecondary)
            }
        }
    }
}

@Composable
private fun TodayTimelineCard(rows: List<JourneyTimelineRow>, onRowClick: (JourneyTimelineRow) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    JourneyCard {
        Text("Today", color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(14.dp))
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
        Text(row.timeLabel, color = tokens.textSecondary, style = KuroType.labelStrong, modifier = Modifier.width(82.dp))
        val taskName = row.taskName?.takeIf { it.isNotBlank() }
        if (taskName == null) {
            Spacer(Modifier.weight(1f))
        } else {
            Text(taskName, color = tokens.textPrimary, style = KuroType.bodyPrimary, modifier = Modifier.weight(1f))
        }
        Text(row.durationLabel, color = tokens.textPrimary, style = KuroType.bodySecondary)
        Spacer(Modifier.width(KuroSpacing.sm))
        StatusPill(row.statusLabel)
    }
}

@Composable
private fun TimelineRail(first: Boolean, last: Boolean) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(modifier = Modifier.width(28.dp).height(40.dp), contentAlignment = Alignment.Center) {
        if (!first) Box(Modifier.align(Alignment.TopCenter).width(1.dp).height(18.dp).background(tokens.textFaint))
        if (!last) Box(Modifier.align(Alignment.BottomCenter).width(1.dp).height(18.dp).background(tokens.textFaint))
        Box(Modifier.size(11.dp).clip(CircleShape).background(tokens.cardFill).border(2.dp, tokens.textFaint, CircleShape))
    }
}

@Composable
private fun StatusPill(label: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val complete = label == "Completed"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (complete) tokens.accentSoft else tokens.activeCardFill)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (complete) JourneyGreen else tokens.accent, style = KuroType.labelMeta)
    }
}

@Composable
private fun JourneyCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(KuroSpacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Surface(
        modifier = modifier.fillMaxWidth().shadow(11.dp, RoundedCornerShape(KuroSpacing.radiusLg)),
        shape = RoundedCornerShape(KuroSpacing.radiusLg),
        color = tokens.cardFill,
        border = BorderStroke(1.dp, tokens.cardBorder)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourneyRecordSheet(record: SessionRecord, onDismiss: () -> Unit, onDelete: (Long) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val start = Instant.ofEpochMilli(record.startedAt).atZone(zone).format(timeFormatter)
    val end = Instant.ofEpochMilli(record.endedAt).atZone(zone).format(timeFormatter)
    val focusMinutes = record.completedFocusCount.coerceAtLeast(0) * record.focusMinutes.coerceAtLeast(0)
    val restMinutes = record.completedShortBreakCount * record.shortBreakMinutes + record.completedLongBreakCount * record.longBreakMinutes

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = tokens.pageBackground) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = KuroSpacing.xl).padding(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
                    Text("Journey detail", color = tokens.textPrimary, style = KuroType.headingScreen)
                    Text("$start - $end", color = tokens.textSecondary, style = KuroType.labelStrong)
                }
                StatusPill(if (record.status == SessionStatus.Completed) "Completed" else "Partial")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SheetStat("Focused", journeyMinutesLabel(focusMinutes))
                SheetDivider()
                SheetStat("Sessions", "${record.completedFocusCount}/${record.totalFocusCount}")
                SheetDivider()
                SheetStat("Rest", journeyMinutesLabel(restMinutes))
            }
            record.taskName?.takeIf { it.isNotBlank() }?.let {
                Surface(shape = RoundedCornerShape(14.dp), color = tokens.cardFill, border = BorderStroke(1.dp, tokens.cardBorder)) {
                    Text(it, color = tokens.textPrimary, style = KuroType.bodyPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuroSpacing.radiusLg))
                    .background(tokens.destructive.copy(alpha = 0.10f))
                    .border(1.dp, tokens.destructive.copy(alpha = 0.30f), RoundedCornerShape(KuroSpacing.radiusLg))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
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
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete(record.id) }
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = tokens.textSecondary, style = KuroType.labelMeta)
        Text(value, color = tokens.textPrimary, style = KuroType.headingCard)
    }
}

@Composable
private fun SheetDivider() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(tokens.cardBorder))
}
