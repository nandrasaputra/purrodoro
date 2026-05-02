package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.SectionTitle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val month = remember(today) { YearMonth.from(today) }
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val dates = remember(month, firstDayOfWeek) { monthCalendarCells(month, firstDayOfWeek) }
    val weekDays = remember(firstDayOfWeek) { localizedWeekDays(firstDayOfWeek) }
    val monthLabel = month.month.getDisplayName(TextStyle.FULL, locale)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Stamp calendar",
            subtitle = "Kuro keeps a paw mark for every finished focus session"
        )

        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$monthLabel ${month.year}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$completedSessions completed focus sessions total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            Spacer(Modifier.height(18.dp))
            CalendarGrid(
                dates = dates,
                stamps = stamps,
                today = today,
                locale = locale,
                weekDays = weekDays
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    dates: List<LocalDate?>,
    stamps: Map<LocalDate, Int>,
    today: LocalDate,
    locale: Locale,
    weekDays: List<DayOfWeek>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center
                )
            }
        }

        dates.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        count = date?.let { stamps[it] } ?: 0,
                        isToday = date == today,
                        locale = locale,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    count: Int,
    isToday: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val hasStamp = count > 0
    val containerColor = when {
        hasStamp -> colorScheme.primaryContainer
        isToday -> colorScheme.secondaryContainer
        else -> colorScheme.surfaceVariant
    }
    val contentColor = when {
        hasStamp -> colorScheme.onPrimaryContainer
        isToday -> colorScheme.onSecondaryContainer
        else -> colorScheme.onSurfaceVariant
    }
    val outline = if (hasStamp || isToday) {
        contentColor.copy(alpha = 0.32f)
    } else {
        colorScheme.outlineVariant
    }
    val label = when {
        date == null -> "Empty calendar cell"
        hasStamp -> "${date.dayOfMonth}, $count completed focus sessions"
        else -> "${date.dayOfMonth}, no completed focus sessions"
    }

    Box(
        modifier = modifier
            .aspectRatio(0.86f)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, outline, shape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (hasStamp) "Paw x$count" else date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = if (hasStamp) 0.88f else 0.72f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun monthCalendarCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingEmptyCells = Math.floorMod(firstDay.dayOfWeek.value - firstDayOfWeek.value, 7)
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List(leadingEmptyCells) { null } + days
    val trailingEmptyCells = (7 - cells.size % 7) % 7
    return cells + List(trailingEmptyCells) { null }
}

private fun localizedWeekDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> {
    return (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }
}
