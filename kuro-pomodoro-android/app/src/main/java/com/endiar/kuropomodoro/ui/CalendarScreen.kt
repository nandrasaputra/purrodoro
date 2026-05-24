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
import androidx.compose.material3.MaterialTheme
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private val JourneyPaper = Color(0xFFFFFBF7)
private val JourneyInk = Color(0xFF292625)
private val JourneyMuted = Color(0xFF77716C)
private val JourneyFaint = Color(0xFFA7A09A)
private val JourneyLine = Color(0xFFEDE3DA)
private val JourneyCard = Color(0xFFFFFEFC)
private val JourneyCoral = Color(0xFFE97852)
private val JourneyOrange = Color(0xFFF5A33B)
private val JourneyGreen = Color(0xFF8BA05E)
private val JourneyChip = Color(0xFFF0F4E8)

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
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 112.dp)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            JourneyHeader(Modifier.onSizeChanged { headerHeightPx = it.height })
            JourneySegmentedControl(selectedPeriod) { selectedPeriod = it }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    icon = "T",
                    iconColor = JourneyCoral,
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    iconColor = JourneyCoral,
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Journey", color = JourneyInk, fontFamily = FontFamily.Serif, fontSize = 42.sp)
            Text("Your focus journey.", color = JourneyMuted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Surface(
            modifier = Modifier.size(64.dp).shadow(8.dp, CircleShape),
            shape = CircleShape,
            color = Color(0xFFFFEFE5),
            border = BorderStroke(1.dp, Color.White)
        ) {
            Image(
                painter = painterResource(R.drawable.cat_face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun JourneySegmentedControl(selected: JourneyPeriod, onSelected: (JourneyPeriod) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(46.dp).shadow(8.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, JourneyLine)
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            JourneyPeriod.entries.forEach { period ->
                val active = selected == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) Color(0xFFFFFBF9) else Color.Transparent)
                        .border(if (active) 1.dp else 0.dp, if (active) Color(0xFFFFC9B6) else Color.Transparent, RoundedCornerShape(20.dp))
                        .clickable { onSelected(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(period.label, color = if (active) JourneyCoral else JourneyMuted, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
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
    ReferenceCard(modifier = modifier.height(118.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = iconColor, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = JourneyInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = JourneyInk, fontFamily = FontFamily.Serif, fontSize = 30.sp)
                Text(note, color = JourneyMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun GoalRing(progress: Float) {
    Canvas(modifier = Modifier.size(46.dp)) {
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        drawArc(JourneyLine, -90f, 360f, false, style = stroke, size = Size(size.width, size.height))
        drawArc(JourneyOrange, -90f, 360f * progress.coerceIn(0f, 1f), false, style = stroke, size = Size(size.width, size.height))
    }
}

@Composable
private fun WeeklyFocusCard(summary: JourneySummary) {
    ReferenceCard {
        Text("Focus time this week", color = JourneyInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
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
                                .background(if (index == 6) Color(0xFFF3B798) else JourneyCoral)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(bar.dayLabel.take(3), color = JourneyMuted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
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
    Column(
        modifier = Modifier.width(34.dp).height(86.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        Text("3h", color = JourneyMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("1.5h", color = JourneyMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("0", color = JourneyMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    ReferenceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Focus calendar", color = JourneyInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "${displayMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${displayMonth.year}",
                color = JourneyInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("<", color = JourneyMuted, fontSize = 24.sp, modifier = Modifier.clickable { onPreviousMonth() })
                Text(">", color = JourneyMuted, fontSize = 24.sp, modifier = Modifier.clickable { onNextMonth() })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(day, color = JourneyMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        CalendarGrid(displayMonth, selectedDate, today, stamps, records, zone, onSelectedDate)
        Spacer(Modifier.height(12.dp))
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
    val bg = when {
        selected -> JourneyCoral
        today -> Color(0xFFF2EBDD)
        hasFocus -> Color(0xFFFFF1E8)
        else -> Color.Transparent
    }
    val textColor = when {
        selected -> Color.White
        !inMonth -> JourneyFaint
        else -> JourneyInk
    }
    Box(modifier = modifier.height(30.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(date.dayOfMonth.toString(), color = textColor, fontSize = 13.sp, fontWeight = if (selected || today) FontWeight.ExtraBold else FontWeight.SemiBold)
        }
        if (hasFocus && !selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (inMonth) JourneyOrange else JourneyFaint)
            )
        }
    }
}

@Composable
private fun SelectedDayStrip(summary: JourneySummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFFBF4),
        border = BorderStroke(1.dp, Color(0xFFF4E9DB))
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.cat_home_sleeping),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 72.dp, height = 48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${summary.selectedDay.dateLabel} - ${summary.selectedDay.focusTimeLabel} - ${summary.selectedDay.focusSessions} sessions",
                    color = JourneyInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("Tap a day to see details", color = JourneyMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TodayTimelineCard(rows: List<JourneyTimelineRow>, onRowClick: (JourneyTimelineRow) -> Unit) {
    ReferenceCard {
        Text("Today", color = JourneyInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        if (rows.isEmpty()) {
            Text("No focus records yet today.", color = JourneyMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        } else {
            rows.forEachIndexed { index, row ->
                TimelineRow(row, first = index == 0, last = index == rows.lastIndex) { onRowClick(row) }
            }
        }
    }
}

@Composable
private fun TimelineRow(row: JourneyTimelineRow, first: Boolean, last: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineRail(first, last)
        Text(row.timeLabel, color = JourneyMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(82.dp))
        val taskName = row.taskName?.takeIf { it.isNotBlank() }
        if (taskName == null) {
            Spacer(Modifier.weight(1f))
        } else {
            Text(taskName, color = JourneyInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        }
        Text(row.durationLabel, color = JourneyInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        StatusPill(row.statusLabel)
    }
}

@Composable
private fun TimelineRail(first: Boolean, last: Boolean) {
    Box(modifier = Modifier.width(28.dp).height(40.dp), contentAlignment = Alignment.Center) {
        if (!first) Box(Modifier.align(Alignment.TopCenter).width(1.dp).height(18.dp).background(JourneyFaint))
        if (!last) Box(Modifier.align(Alignment.BottomCenter).width(1.dp).height(18.dp).background(JourneyFaint))
        Box(Modifier.size(11.dp).clip(CircleShape).background(Color.White).border(2.dp, JourneyFaint, CircleShape))
    }
}

@Composable
private fun StatusPill(label: String) {
    val complete = label == "Completed"
    Box(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (complete) JourneyChip else Color(0xFFFFF0E4)).padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (complete) Color(0xFF6F8351) else JourneyCoral, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ReferenceCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth().shadow(11.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = JourneyCard,
        border = BorderStroke(1.dp, JourneyLine)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourneyRecordSheet(record: SessionRecord, onDismiss: () -> Unit, onDelete: (Long) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val start = Instant.ofEpochMilli(record.startedAt).atZone(zone).format(timeFormatter)
    val end = Instant.ofEpochMilli(record.endedAt).atZone(zone).format(timeFormatter)
    val focusMinutes = record.completedFocusCount.coerceAtLeast(0) * record.focusMinutes.coerceAtLeast(0)
    val restMinutes = record.completedShortBreakCount * record.shortBreakMinutes + record.completedLongBreakCount * record.longBreakMinutes

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = JourneyPaper) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Journey detail", color = JourneyInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("$start - $end", color = JourneyMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, JourneyLine)) {
                    Text(it, color = JourneyInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFEFEA))
                    .border(1.dp, Color(0xFFFFC6B8), RoundedCornerShape(18.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Delete Record", color = Color(0xFFBF4C35), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this record?", color = JourneyInk, fontWeight = FontWeight.ExtraBold) },
            text = { Text("The session from $start to $end will be permanently removed.", color = JourneyMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete(record.id) }
                }) {
                    Text("Delete", color = Color(0xFFBF4C35), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = JourneyMuted)
                }
            },
            containerColor = JourneyPaper
        )
    }
}

@Composable
private fun SheetStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = JourneyMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = JourneyInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SheetDivider() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(JourneyLine))
}
