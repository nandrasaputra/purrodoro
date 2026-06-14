package com.endiar.purrodoro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.sync.SyncStatusPresentation
import com.endiar.purrodoro.sync.SyncUiStatus
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun SyncStatusSection(
    presentation: SyncStatusPresentation,
    modifier: Modifier = Modifier,
) {
    KuroCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SyncStatusRowContent(presentation = presentation)
        }
    }
}

/** Compact stacked sync readout for the account card split panel. */
@Composable
fun SyncStatusMiniPanel(
    presentation: SyncStatusPresentation,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val labels = rememberSyncStatusLabels(presentation)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Lucide.Cloud,
            contentDescription = null,
            tint = labels.accentColor,
            modifier = Modifier.size(KuroSpacing.lg),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.xs))
        Text(
            text = labels.title,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        labels.relativeTime?.let { relativeTime ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = relativeTime,
                style = KuroType.labelMeta,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SyncStatusRowContent(
    presentation: SyncStatusPresentation,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val labels = rememberSyncStatusLabels(presentation)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.Cloud,
            contentDescription = null,
            tint = labels.accentColor,
            modifier = Modifier.size(KuroSpacing.lg),
        )
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = labels.title,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.textPrimary,
            )
            labels.lastSyncLabel?.let { subtitle ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = KuroType.labelMeta,
                    color = tokens.textSecondary,
                )
            }
        }
    }
}

private data class SyncStatusLabels(
    val title: String,
    val accentColor: Color,
    val lastSyncLabel: String?,
    val relativeTime: String?,
)

@Composable
private fun rememberSyncStatusLabels(presentation: SyncStatusPresentation): SyncStatusLabels {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(presentation.lastAttemptAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    val title = when (presentation.status) {
        SyncUiStatus.NotSyncedYet -> stringResource(R.string.sync_status_not_yet)
        SyncUiStatus.Skipped -> stringResource(R.string.sync_status_skipped)
        SyncUiStatus.UpToDate -> stringResource(R.string.sync_status_up_to_date)
        SyncUiStatus.Incomplete -> stringResource(R.string.sync_status_incomplete)
        SyncUiStatus.Failed -> stringResource(R.string.sync_status_failed)
    }
    val accentColor = when (presentation.status) {
        SyncUiStatus.UpToDate, SyncUiStatus.Incomplete -> tokens.accent
        SyncUiStatus.NotSyncedYet, SyncUiStatus.Skipped -> tokens.textSecondary
        SyncUiStatus.Failed -> tokens.destructive
    }
    val relativeTime = when (presentation.status) {
        SyncUiStatus.Skipped, SyncUiStatus.NotSyncedYet -> null
        else -> presentation.lastAttemptAtMillis?.let { last ->
            formatLastSyncLabel(last, nowMillis)
        }
    }
    val lastSyncLabel = when (presentation.status) {
        SyncUiStatus.Skipped, SyncUiStatus.NotSyncedYet -> null
        SyncUiStatus.Incomplete, SyncUiStatus.Failed ->
            relativeTime?.let { stringResource(R.string.sync_status_last_tried, it) }
        SyncUiStatus.UpToDate ->
            relativeTime?.let { stringResource(R.string.sync_status_last_sync, it) }
    }
    return SyncStatusLabels(
        title = title,
        accentColor = accentColor,
        lastSyncLabel = lastSyncLabel,
        relativeTime = relativeTime,
    )
}

@Composable
private fun formatLastSyncLabel(lastAttemptAtMillis: Long, nowMillis: Long): String {
    val elapsed = Duration.ofMillis((nowMillis - lastAttemptAtMillis).coerceAtLeast(0))
    return when {
        elapsed.seconds < 45 -> stringResource(R.string.sync_time_just_now)
        elapsed.toMinutes() < 60 ->
            stringResource(R.string.sync_time_minutes_ago, elapsed.toMinutes())
        elapsed.toHours() < 24 ->
            stringResource(R.string.sync_time_hours_ago, elapsed.toHours())
        else -> DateTimeFormatter.ofPattern("MMM d, HH:mm")
            .format(Instant.ofEpochMilli(lastAttemptAtMillis).atZone(ZoneId.systemDefault()))
    }
}
