package com.endiar.purrodoro.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Target
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.ui.designsystem.KuroBackScreenTopBar
import com.endiar.purrodoro.ui.designsystem.KuroBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroElevatedCard
import com.endiar.purrodoro.ui.designsystem.KuroPill
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * History — a pushed full-screen detail surface (entered from the Journey
 * top-bar history icon). Hosts the deliberate "look up a specific day" flow:
 * an interactive monthly calendar heatmap with per-day drill-down into that
 * day's session timeline, where a session can be inspected and deleted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessionRecords: List<SessionRecord>,
    onBack: () -> Unit,
    onDeleteRecord: (Long) -> Unit = {},
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var selectedDate by remember { mutableStateOf(today) }
    var displayMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedRecord by remember { mutableStateOf<SessionRecord?>(null) }

    val intensities = remember(sessionRecords, zone) {
        focusIntensitiesByDate(records = sessionRecords, zone = zone)
    }
    val detail = remember(sessionRecords, selectedDate, zone) {
        historyDayDetail(records = sessionRecords, date = selectedDate, zone = zone)
    }

    Scaffold(
        topBar = {
            KuroBackScreenTopBar(
                title = stringResource(R.string.history_title),
                onBack = onBack,
            )
        },
        containerColor = tokens.pageBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KuroSpacing.lg)
                .padding(top = KuroSpacing.sm, bottom = KuroSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            HistoryCalendarCard(
                displayMonth = displayMonth,
                selectedDate = selectedDate,
                today = today,
                intensities = intensities,
                onPreviousMonth = { displayMonth = displayMonth.minusMonths(1) },
                onNextMonth = { displayMonth = displayMonth.plusMonths(1) },
                onSelectedDate = { selectedDate = it },
            )
            HistoryDayDetailCard(detail = detail, onRowClick = { selectedRecord = it.record })
        }
    }

    selectedRecord?.let { record ->
        HistoryRecordSheet(
            record = record,
            onDismiss = { selectedRecord = null },
            onDelete = {
                onDeleteRecord(it)
                selectedRecord = null
            },
        )
    }
}

@Composable
private fun HistoryCalendarCard(
    displayMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    intensities: Map<LocalDate, Int>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectedDate: (LocalDate) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${displayMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayMonth.year}",
                color = tokens.textPrimary,
                style = KuroType.headingCard,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Lucide.ChevronLeft,
                    contentDescription = stringResource(R.string.history_previous_month),
                    tint = tokens.textSecondary,
                    modifier = Modifier.size(KuroSpacing.lg).clip(CircleShape).clickable { onPreviousMonth() },
                )
                Icon(
                    Lucide.ChevronRight,
                    contentDescription = stringResource(R.string.history_next_month),
                    tint = tokens.textSecondary,
                    modifier = Modifier.size(KuroSpacing.lg).clip(CircleShape).clickable { onNextMonth() },
                )
            }
        }
        Spacer(Modifier.height(KuroSpacing.md))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(day, color = tokens.textSecondary, style = KuroType.labelMeta, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(KuroSpacing.sm))
        val firstDay = displayMonth.atDay(1)
        val firstVisible = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.journeyCalendarRowGap)) {
            repeat(6) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val date = firstVisible.plusDays((row * 7 + col).toLong())
                        HistoryDayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == displayMonth,
                            selected = date == selectedDate,
                            today = date == today,
                            intensity = intensities[date] ?: 0,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectedDate(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    intensity: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val bg = when {
        selected -> tokens.accent
        !inMonth -> Color.Transparent
        intensity > 0 -> historyHeatColor(intensity)
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
                .then(
                    if (today && !selected) Modifier.border(KuroSpacing.xs / 2, tokens.accent, CircleShape) else Modifier
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = textColor,
                style = if (selected || today) KuroType.labelStrong else KuroType.bodySecondary,
            )
        }
    }
}

@Composable
private fun historyHeatColor(level: Int): Color {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    return when (level) {
        1 -> tokens.accent.copy(alpha = 0.22f)
        2 -> tokens.accent.copy(alpha = 0.45f)
        3 -> tokens.accent.copy(alpha = 0.70f)
        else -> tokens.accent
    }
}

private val historyDayHeaderFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

@Composable
private fun HistoryDayDetailCard(detail: HistoryDayDetail, onRowClick: (JourneyTimelineRow) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Text(detail.date.format(historyDayHeaderFormatter), color = tokens.textPrimary, style = KuroType.headingCard)
        Spacer(Modifier.height(KuroSpacing.md))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
            DayStat(Lucide.Clock, detail.focusTimeLabel, stringResource(R.string.journey_stat_focused))
            DayStat(Lucide.Target, detail.focusSessions.toString(), stringResource(R.string.journey_stat_sessions))
            DayStat(Lucide.Coffee, detail.restLabel, stringResource(R.string.journey_stat_rest))
        }
        Spacer(Modifier.height(KuroSpacing.md))
        if (detail.rows.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = KuroSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                Image(
                    painter = painterResource(R.drawable.cat_empty_day),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(KuroSpacing.journeyChartCatWidth),
                )
                Text(
                    stringResource(R.string.history_empty_day),
                    color = tokens.textSecondary,
                    style = KuroType.bodySecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            JourneyTimelineList(
                rows = detail.rows,
                onRowClick = onRowClick,
            )
        }
    }
}

@Composable
private fun DayStat(icon: ImageVector, value: String, label: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
        Icon(icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.lg))
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 4)) {
            Text(value, color = tokens.textPrimary, style = KuroType.labelStrong)
            Text(label, color = tokens.textSecondary, style = KuroType.labelMeta)
        }
    }
}

@Composable
private fun HistoryRecordSheet(record: SessionRecord, onDismiss: () -> Unit, onDelete: (Long) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val start = Instant.ofEpochMilli(record.startedAt).atZone(zone).format(timeFormatter)
    val end = Instant.ofEpochMilli(record.endedAt).atZone(zone).format(timeFormatter)
    val focusMinutes = record.completedFocusCount.coerceAtLeast(0) * record.focusMinutes.coerceAtLeast(0)
    val restMinutes = record.completedShortBreakCount * record.shortBreakMinutes + record.completedLongBreakCount * record.longBreakMinutes

    val completed = record.status == SessionStatus.Completed
    KuroBottomSheet(title = stringResource(R.string.journey_detail), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.journey_time_range, start, end),
                    color = tokens.textSecondary,
                    style = KuroType.labelStrong,
                )
                KuroPill(
                    label = stringResource(
                        if (completed) R.string.journey_status_completed else R.string.journey_status_partial,
                    ),
                    kind = journeyStatusKind(completed),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SheetStat(stringResource(R.string.journey_stat_focused), journeyMinutesLabel(focusMinutes))
                SheetDivider()
                SheetStat(
                    stringResource(R.string.journey_stat_sessions),
                    stringResource(
                        R.string.journey_stat_sessions_value,
                        record.completedFocusCount,
                        record.totalFocusCount,
                    ),
                )
                SheetDivider()
                SheetStat(stringResource(R.string.journey_stat_rest), journeyMinutesLabel(restMinutes))
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
                Text(stringResource(R.string.journey_delete_record), color = tokens.destructive, style = KuroType.bodyPrimary)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.journey_delete_title), color = tokens.textPrimary, style = KuroType.headingCard) },
            text = { Text(stringResource(R.string.journey_delete_body, start, end), color = tokens.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(record.id)
                }) {
                    Text(stringResource(R.string.common_delete), color = tokens.destructive, style = KuroType.labelStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel), color = tokens.textSecondary)
                }
            },
            containerColor = tokens.pageBackground,
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
