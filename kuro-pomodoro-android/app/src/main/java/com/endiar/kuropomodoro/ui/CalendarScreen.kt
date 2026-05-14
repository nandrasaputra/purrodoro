package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.ui.components.GlassCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    sessionRecords: List<SessionRecord> = emptyList(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    val yearMonth = YearMonth.of(today.year, today.month)
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstDay.dayOfWeek.value % 7)

    val todaySessions = stamps[today] ?: 0
    val todayMinutes = todaySessions * 25

    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekSessions = (0..6)
        .map { weekStart.plusDays(it.toLong()) }
        .sumOf { stamps[it] ?: 0 }

    val streak = calculateStreak(stamps, today)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Records",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(label = "Today", value = "${todayMinutes}m", modifier = Modifier.weight(1f), glassTreatment = glassTreatment)
            StatChip(label = "Week", value = "${weekSessions * 25}m", modifier = Modifier.weight(1f), glassTreatment = glassTreatment)
            StatChip(label = "Streak", value = "${streak}d", modifier = Modifier.weight(1f), glassTreatment = glassTreatment)
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), glassTreatment = glassTreatment) {
            val monthLabel = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Text(
                text = "$monthLabel ${today.year}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        color = Color.White.copy(alpha = 0.40f),
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
        }

        Text(
            text = "Total sessions: $completedSessions",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        SessionSection(
            date = selectedDate,
            records = sessionRecords,
            glassTreatment = glassTreatment
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier, glassTreatment: GlassTreatment = GlassTreatment.Clear) {
    GlassCard(
        modifier = modifier,
        glassTreatment = glassTreatment
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = Color.White.copy(alpha = 0.50f), fontSize = 11.sp)
        }
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
    val bg = when {
        isSelected  -> Color(0xFFFF8B6F)
        hasSessions -> Color.White.copy(alpha = 0.12f)
        else        -> Color.Transparent
    }
    val border = when {
        isSelected  -> Color.Transparent
        isToday     -> Color.White.copy(alpha = 0.60f)
        hasSessions -> Color.White.copy(alpha = 0.20f)
        else        -> Color.Transparent
    }
    val textColor = when {
        isSelected  -> Color.White
        hasSessions -> Color.White
        else        -> Color.White.copy(alpha = 0.50f)
    }
    Box(
        modifier = Modifier
            .size(34.dp)
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
    glassTreatment: GlassTreatment
) {
    val dateRecords = records
        .filter { record ->
            val recordDate = java.time.Instant.ofEpochMilli(record.startedAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            recordDate == date
        }
        .sortedByDescending { it.startedAt }

    val dateLabel = if (date == LocalDate.now()) {
        val monthLabel = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "Today · $monthLabel ${date.dayOfMonth}"
    } else {
        val monthLabel = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "$monthLabel ${date.dayOfMonth}, ${date.year}"
    }

    Text(
        text = dateLabel,
        color = Color.White.copy(alpha = 0.60f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )

    if (dateRecords.isEmpty()) {
        Text(
            text = "No sessions on this day",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        dateRecords.forEach { record ->
            SessionCard(record = record, glassTreatment = glassTreatment)
        }
    }
}

@Composable
private fun SessionCard(record: SessionRecord, glassTreatment: GlassTreatment) {
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    val zone = java.time.ZoneId.systemDefault()
    val startTime = java.time.Instant.ofEpochMilli(record.startedAt).atZone(zone)
        .format(timeFormatter)
    val endTime = java.time.Instant.ofEpochMilli(record.endedAt).atZone(zone)
        .format(timeFormatter)

    val badgeText = if (record.status == SessionStatus.Completed) "Completed" else "Partial"
    val badgeColor = if (record.status == SessionStatus.Completed) Color(0xFF4CAF50) else Color(0xFFFF8B6F)
    val badgeBg = if (record.status == SessionStatus.Completed) Color(0x334CAF50) else Color(0x33FF8B6F)

    val dots = buildString {
        repeat(record.completedFocusCount) { append("● ") }
        repeat(record.totalFocusCount - record.completedFocusCount) { append("○ ") }
    }.trimEnd()

    GlassCard(modifier = Modifier.fillMaxWidth(), glassTreatment = glassTreatment) {
        // Header row: time range + badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$startTime – $endTime",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(text = badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        // Focus dots + count
        Text(
            text = "$dots  ${record.completedFocusCount} / ${record.totalFocusCount} focus · ${record.focusMinutes} min each",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        // Break line
        Text(
            text = "Break · ${record.shortBreakMinutes} min short + ${record.longBreakMinutes} min long",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )
        // Optional task chip
        if (record.taskName != null) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "📌 ${record.taskName}",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun calculateStreak(stamps: Map<LocalDate, Int>, today: LocalDate): Int {
    var streak = 0
    var date = today
    while (true) {
        val sessions = stamps[date] ?: 0
        if (sessions > 0) {
            streak++
            date = date.minusDays(1)
        } else break
    }
    return streak
}
