package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

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
    val tokens = MaterialTheme.colorScheme.kuroTokens

    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var displayMonth by remember { mutableStateOf(YearMonth.of(today.year, today.month)) }
    val yearMonth = displayMonth
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstDay.dayOfWeek.value % 7)

    fun minutesForDate(date: LocalDate): Int =
        sessionRecords
            .filter { r ->
                java.time.Instant.ofEpochMilli(r.startedAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate() == date
            }
            .sumOf { r -> r.completedFocusCount * r.focusMinutes }

    val selectedMinutes = minutesForDate(selectedDate)
    val todayMinutes = minutesForDate(today)

    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekMinutes = (0..6)
        .map { weekStart.plusDays(it.toLong()) }
        .sumOf { minutesForDate(it) }

    val streak = com.endiar.kuropomodoro.model.calculateStreak(stamps, today)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Records",
                color = tokens.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "your focus history with kuro 🐱",
                color = tokens.textSecondary,
                fontSize = 13.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isToday = selectedDate == today
            val todayLabel = if (isToday) "Today" else selectedDate.month.getDisplayName(
                TextStyle.SHORT,
                Locale.getDefault()
            ).replaceFirstChar { it.uppercase() } + " ${selectedDate.dayOfMonth}"
            InlineStatItem(label = todayLabel, value = "${selectedMinutes} min")
            Box(
                Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(tokens.textFaint)
            )
            InlineStatItem(label = "This Week", value = "${weekMinutes} min")
            Box(
                Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(tokens.textFaint)
            )
            val streakLabel = if (streak > 0) "🔥Streak" else "No Streak Yet"
            val streakAlpha = if (streak > 0) 1f else 0.35f
            InlineStatItem(label = streakLabel, value = "${streak} day", valueAlpha = streakAlpha)
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glassTreatment = glassTreatment,
            overBackdrop = overBackdrop,
        ) {
            val monthLabel = displayMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tokens.textFaint)
                        .clickable { displayMonth = displayMonth.minusMonths(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Light)
                }
                Text(
                    text = "$monthLabel ${displayMonth.year}",
                    color = tokens.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val isCurrentMonth = displayMonth == YearMonth.of(today.year, today.month)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentMonth) tokens.textFaint.copy(alpha = 0.4f)
                            else tokens.textFaint
                        )
                        .clickable(enabled = !isCurrentMonth) {
                            displayMonth = displayMonth.plusMonths(1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "›",
                        color = if (isCurrentMonth) tokens.textMuted else tokens.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val cells = startOffset + daysInMonth
            val rows = (cells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayNum)
                                val isToday = date == today
                                val hasSessions = (stamps[date] ?: 0) > 0
                                DayCell(
                                    day = dayNum,
                                    isToday = isToday,
                                    isSelected = date == selectedDate,
                                    hasSessions = hasSessions,
                                    onClick = { selectedDate = date }
                                )
                            }
                        }
                    }
                }
                if (row < rows - 1) Spacer(Modifier.height(4.dp))
            }

            val selectedCycles = sessionRecords
                .filter { r ->
                    java.time.Instant.ofEpochMilli(r.startedAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate() == selectedDate
                }
                .sumOf { r -> r.completedFocusCount }
            val selectedMonthName =
                selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.divider)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${selectedDate.dayOfMonth} $selectedMonthName ${selectedDate.year} · $selectedCycles cycles · ${selectedMinutes} min focused",
                color = tokens.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        SessionSection(
            date = selectedDate,
            records = sessionRecords,
            onDeleteRecord = onDeleteRecord,
            glassTreatment = glassTreatment,
            overBackdrop = overBackdrop,
        )
    }
}

@Composable
private fun InlineStatItem(
    label: String,
    value: String,
    valueAlpha: Float = 1f
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = tokens.textSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = tokens.textPrimary.copy(alpha = valueAlpha),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasSessions: Boolean,
    onClick: () -> Unit = {}
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val bg = when {
        isSelected -> tokens.accent
        hasSessions -> tokens.textFaint
        else -> Color.Transparent
    }
    val border = when {
        isSelected -> Color.Transparent
        isToday -> tokens.textSecondary
        hasSessions -> tokens.textMuted
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> tokens.accentOn
        hasSessions -> tokens.textPrimary
        else -> tokens.textMuted
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SessionSection(
    date: LocalDate,
    records: List<SessionRecord>,
    onDeleteRecord: (Long) -> Unit,
    glassTreatment: GlassTreatment,
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val dateRecords = records
        .filter { record ->
            val recordDate = java.time.Instant.ofEpochMilli(record.startedAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            recordDate == date
        }
        .sortedByDescending { it.startedAt }

    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.uppercase() }
    val monthLabel = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val fullDate = "${date.dayOfMonth} $monthLabel ${date.year}"

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$dayName,",
            color = tokens.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = fullDate,
            color = tokens.textSecondary,
            fontSize = 13.sp
        )
    }

    if (dateRecords.isEmpty()) {
        val isToday = date == LocalDate.now()
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isToday) "kuro hasn't seen you today yet 🐱" else "nothing here",
                color = tokens.textSecondary,
                fontSize = 14.sp
            )
            Text(
                text = if (isToday) "start a session to begin your streak" else "kuro was probably napping 🐱",
                color = tokens.textMuted,
                fontSize = 13.sp
            )
        }
    } else {
        dateRecords.forEach { record ->
            SessionCard(
                record = record,
                onDelete = onDeleteRecord,
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    record: SessionRecord,
    onDelete: (Long) -> Unit,
    glassTreatment: GlassTreatment,
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    val zone = java.time.ZoneId.systemDefault()
    val startTime = java.time.Instant.ofEpochMilli(record.startedAt).atZone(zone)
        .format(timeFormatter)
    val endTime = java.time.Instant.ofEpochMilli(record.endedAt).atZone(zone)
        .format(timeFormatter)

    var showDetail by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetail = true },
        glassTreatment = glassTreatment,
        overBackdrop = overBackdrop,
    ) {
        val totalMinutes = record.completedFocusCount * record.focusMinutes
        val breakTimeTotal = record.completedShortBreakCount * record.shortBreakMinutes +
            record.completedLongBreakCount * record.longBreakMinutes
        val heroText =
            "${totalMinutes} min focused" + if (breakTimeTotal > 0) "\n${breakTimeTotal} min rest" else ""
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = heroText,
                color = tokens.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$startTime – $endTime",
                color = tokens.textSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(16.dp))
        com.endiar.kuropomodoro.ui.components.CycleDotsRow(
            totalFocusSessions = record.totalFocusCount,
            completedFocusSessions = record.completedFocusCount,
            isCurrentlyFocus = false
        )

        if (record.taskName != null) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.segmentedTrack)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "📌 ${record.taskName}",
                    color = tokens.textPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showDetail) {
        SessionDetailSheet(
            record = record,
            onDismiss = { showDetail = false },
            onDelete = { id ->
                onDelete(id)
                showDetail = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailSheet(
    record: SessionRecord,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isLight = tokens.cardFill.luminance() > 0.5f
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    val zone = java.time.ZoneId.systemDefault()
    val startTime = java.time.Instant.ofEpochMilli(record.startedAt).atZone(zone)
        .format(timeFormatter)
    val endTime = java.time.Instant.ofEpochMilli(record.endedAt).atZone(zone)
        .format(timeFormatter)

    val totalFocusMinutes = record.completedFocusCount * record.focusMinutes
    val breakTimeTotal = record.completedShortBreakCount * record.shortBreakMinutes +
        record.completedLongBreakCount * record.longBreakMinutes
    val isCompleted = record.status == SessionStatus.Completed

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardFill
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session Details",
                    color = tokens.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCompleted) Color(0xFF00C853).copy(alpha = 0.18f) else Color(0xFFFF9500).copy(alpha = 0.18f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCompleted) "✓ Completed" else "Partial",
                        color = if (isCompleted)
                            (if (isLight) Color(0xFF006B47) else Color(0xFF7EEAA0))
                        else
                            (if (isLight) Color(0xFFA04E00) else Color(0xFFFFB067)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "$startTime – $endTime",
                color = tokens.textSecondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailStatItem(label = "Focused", value = "${totalFocusMinutes} min")
                Box(
                    Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(tokens.divider)
                )
                DetailStatItem(
                    label = "Cycles",
                    value = "${record.completedFocusCount}/${record.totalFocusCount}"
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(tokens.divider)
                )
                DetailStatItem(
                    label = "Rest",
                    value = if (breakTimeTotal > 0) "${breakTimeTotal} min" else "—"
                )
            }

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.divider)
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Timer settings",
                color = tokens.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingChip(label = "Focus", value = "${record.focusMinutes} min")
                SettingChip(label = "Short break", value = "${record.shortBreakMinutes} min")
                SettingChip(label = "Long break", value = "${record.longBreakMinutes} min")
            }

            Spacer(Modifier.height(20.dp))
            com.endiar.kuropomodoro.ui.components.CycleDotsRow(
                totalFocusSessions = record.totalFocusCount,
                completedFocusSessions = record.completedFocusCount,
                isCurrentlyFocus = false
            )

            if (record.taskName != null) {
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.segmentedTrack)
                        .border(1.dp, tokens.borderSoft, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📌 ${record.taskName}",
                        color = tokens.textPrimary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF3B30).copy(alpha = 0.10f))
                    .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Delete Record",
                    color = if (isLight) Color(0xFFCC2222) else Color(0xFFFF6B6B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete this record?",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "The session from $startTime – $endTime will be permanently removed. This cannot be undone.",
                    color = tokens.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete(record.id) }
                }) {
                    Text("Delete", color = if (isLight) Color(0xFFCC2222) else Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = tokens.textSecondary)
                }
            },
            containerColor = tokens.cardFill,
            titleContentColor = tokens.textPrimary,
            textContentColor = tokens.textSecondary,
        )
    }
}

@Composable
private fun DetailStatItem(label: String, value: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = tokens.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = tokens.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingChip(label: String, value: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tokens.segmentedTrack)
            .border(1.dp, tokens.borderSoft, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = tokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                color = tokens.textMuted,
                fontSize = 10.sp
            )
        }
    }
}
