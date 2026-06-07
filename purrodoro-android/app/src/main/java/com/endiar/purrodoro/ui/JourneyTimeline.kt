package com.endiar.purrodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.JourneyGreen
import com.endiar.purrodoro.ui.theme.kuroTokens

/**
 * Vertical session timeline for Journey (Today) and History day detail.
 * Newest entry first; rail connects dot-to-dot; card-style rows.
 */
internal const val JourneyTimelineCollapsedCount = 3

@Composable
internal fun JourneyTimelineListCollapsible(
    rows: List<JourneyTimelineRow>,
    modifier: Modifier = Modifier,
    collapsedCount: Int = JourneyTimelineCollapsedCount,
) {
    var expanded by remember { mutableStateOf(false) }
    val canCollapse = rows.size > collapsedCount
    val visibleRows = if (!canCollapse || expanded) rows else rows.take(collapsedCount)

    Column(modifier = modifier.fillMaxWidth()) {
        JourneyTimelineList(rows = visibleRows)
        if (canCollapse) {
            JourneyTimelineExpandToggle(
                expanded = expanded,
                hiddenCount = rows.size - collapsedCount,
                onToggle = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun JourneyTimelineExpandToggle(
    expanded: Boolean,
    hiddenCount: Int,
    onToggle: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val label = if (expanded) {
        stringResource(R.string.journey_timeline_show_less)
    } else {
        pluralStringResource(R.plurals.journey_timeline_more, hiddenCount, hiddenCount)
    }
    val chevron = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.md)
                .height(1.dp)
                .background(tokens.cardBorder.copy(alpha = 0.65f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                .clickable(onClick = onToggle)
                .semantics { role = Role.Button; contentDescription = label }
                .padding(vertical = KuroSpacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = tokens.accent,
                style = KuroType.labelStrong,
            )
            Spacer(Modifier.width(KuroSpacing.xs))
            Icon(
                imageVector = chevron,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(KuroSpacing.md),
            )
        }
    }
}

@Composable
internal fun JourneyTimelineList(
    rows: List<JourneyTimelineRow>,
    modifier: Modifier = Modifier,
    onRowClick: ((JourneyTimelineRow) -> Unit)? = null,
) {
    if (rows.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            JourneyTimelineEntry(
                row = row,
                isLatest = index == 0,
                isFirst = index == 0,
                isLast = index == rows.lastIndex,
                railWidth = KuroSpacing.journeyTimelineRailWidth,
                onClick = onRowClick?.let { handler -> { handler(row) } },
            )
        }
    }
}

@Composable
private fun JourneyTimelineEntry(
    row: JourneyTimelineRow,
    isLatest: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    railWidth: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)?,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val title = row.taskName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_metric_focus_time)
    val meta = stringResource(R.string.journey_timeline_meta, row.timeLabel, row.durationLabel)
    val cardShape = RoundedCornerShape(KuroSpacing.radiusSm)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JourneyTimelineRail(
            isFirst = isFirst,
            isLast = isLast,
            isLatest = isLatest,
            railWidth = railWidth,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = KuroSpacing.xs)
                .then(
                    if (onClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                            .clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(cardShape)
                    .background(tokens.pageBackground.copy(alpha = 0.55f), cardShape)
                    .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = title,
                        color = tokens.textPrimary,
                        style = KuroType.bodyPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    JourneyTimelineStatusIcon(completed = row.completed)
                }
                Spacer(Modifier.height(KuroSpacing.xs / 2))
                Text(
                    text = meta,
                    color = tokens.textFaint,
                    style = KuroType.labelMeta,
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = tokens.textFaint,
                    modifier = Modifier
                        .padding(start = KuroSpacing.xs)
                        .size(KuroSpacing.md),
                )
            }
        }
    }
}

@Composable
private fun JourneyTimelineRail(
    isFirst: Boolean,
    isLast: Boolean,
    isLatest: Boolean,
    railWidth: androidx.compose.ui.unit.Dp,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val lineColor = tokens.textFaint.copy(alpha = 0.40f)

    Box(
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(2.dp)
                    .fillMaxHeight(0.5f)
                    .background(lineColor),
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(2.dp)
                    .fillMaxHeight(0.5f)
                    .background(lineColor),
            )
        }
        JourneyTimelineDot(isLatest = isLatest)
    }
}

@Composable
private fun JourneyTimelineDot(isLatest: Boolean) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val latestLabel = stringResource(R.string.journey_latest)
    val dotSize = if (isLatest) KuroSpacing.journeyTimelineDot + 2.dp else KuroSpacing.journeyTimelineDot

    Box(
        modifier = Modifier
            .size(dotSize)
            .semantics {
                if (isLatest) contentDescription = latestLabel
            }
            .clip(CircleShape)
            .then(
                if (isLatest) {
                    Modifier.background(tokens.accent)
                } else {
                    Modifier
                        .background(tokens.cardFill)
                        .border(KuroSpacing.xs / 2, tokens.textFaint.copy(alpha = 0.55f), CircleShape)
                },
            ),
    )
}

@Composable
private fun JourneyTimelineStatusIcon(completed: Boolean) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val statusLabel = stringResource(
        if (completed) R.string.journey_status_completed else R.string.journey_status_partial,
    )

    Box(
        modifier = Modifier
            .size(KuroSpacing.lg)
            .semantics { contentDescription = statusLabel }
            .clip(CircleShape)
            .background(
                if (completed) JourneyGreen.copy(alpha = 0.14f) else tokens.accentSoft.copy(alpha = 0.45f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (completed) Lucide.Check else Lucide.Circle,
            contentDescription = null,
            tint = if (completed) JourneyGreen else tokens.accent,
            modifier = Modifier.size(KuroSpacing.md),
        )
    }
}
