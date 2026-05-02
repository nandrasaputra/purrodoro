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
import com.endiar.kuropomodoro.ui.theme.Peach
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val month = remember(today) { YearMonth.from(today) }
    val dates = remember(month) { monthCalendarCells(month) }
    val monthLabel = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val context = LocalContext.current

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
                locale = context.resources.configuration.locales[0]
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    dates: List<LocalDate?>,
    stamps: Map<LocalDate, Int>,
    today: LocalDate,
    locale: Locale
) {
    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekDays.forEach { day ->
                Text(
                    text = day,
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
    val background = when {
        hasStamp -> Peach.copy(alpha = 0.78f)
        isToday -> colorScheme.secondary.copy(alpha = 0.42f)
        else -> colorScheme.surface.copy(alpha = 0.52f)
    }
    val outline = if (hasStamp || isToday) {
        colorScheme.primary.copy(alpha = 0.28f)
    } else {
        colorScheme.onSurface.copy(alpha = 0.12f)
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
            .background(background)
            .border(1.dp, outline, shape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (hasStamp) "Paw x$count" else date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface.copy(alpha = if (hasStamp) 0.86f else 0.48f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun monthCalendarCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingEmptyCells = firstDay.dayOfWeek.value - 1
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List(leadingEmptyCells) { null } + days
    val trailingEmptyCells = (7 - cells.size % 7) % 7
    return cells + List(trailingEmptyCells) { null }
}
