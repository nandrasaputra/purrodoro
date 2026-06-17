package com.endiar.purrodoro.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CloudRain
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Trash2
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroBackScreenTopBar
import com.endiar.purrodoro.ui.designsystem.KuroDivider
import com.endiar.purrodoro.ui.designsystem.KuroElevatedCard
import com.endiar.purrodoro.ui.designsystem.KuroSectionHeader
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlin.math.roundToInt

/**
 * Storage — a pushed full-screen surface (entered from Profile → Storage). Lists
 * downloaded content (today: cloud soundscapes) with per-item remove and a
 * confirmed "remove all". Generic by design so future downloadable assets fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    summary: StorageSummary,
    onBack: () -> Unit,
    onRemove: (Int) -> Unit,
    onRemoveAll: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var showRemoveAllConfirm by remember { mutableStateOf(false) }

    val removableCount = summary.items.count { it.isRemovable }

    Scaffold(
        topBar = {
            KuroBackScreenTopBar(
                title = stringResource(R.string.storage_title),
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
            StorageSummaryCard(totalBytes = summary.totalBytes)

            if (summary.items.isEmpty()) {
                StorageEmptyState()
            } else {
                KuroSectionHeader(title = stringResource(R.string.storage_section_downloads))
                KuroCard(modifier = Modifier.fillMaxWidth()) {
                    summary.items.forEachIndexed { index, item ->
                        if (index > 0) KuroDivider()
                        StorageRow(item = item, onRemove = { onRemove(item.id) })
                    }
                }

                if (removableCount > 0) {
                    RemoveAllButton(onClick = { showRemoveAllConfirm = true })
                }
            }
        }
    }

    if (showRemoveAllConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveAllConfirm = false },
            title = {
                Text(
                    stringResource(R.string.storage_remove_all_title),
                    color = tokens.textPrimary,
                    style = KuroType.headingCard,
                )
            },
            text = {
                Text(
                    stringResource(R.string.storage_remove_all_body, formatBytes(summary.totalBytes)),
                    color = tokens.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveAllConfirm = false
                    onRemoveAll()
                }) {
                    Text(
                        stringResource(R.string.storage_remove_all_confirm),
                        color = tokens.destructive,
                        style = KuroType.labelStrong,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAllConfirm = false }) {
                    Text(stringResource(R.string.common_cancel), color = tokens.textSecondary)
                }
            },
            containerColor = tokens.pageBackground,
        )
    }
}

@Composable
private fun StorageSummaryCard(totalBytes: Long) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.xxl)
                    .clip(CircleShape)
                    .background(tokens.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.HardDrive,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(KuroSpacing.lg),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                Text(
                    text = formatBytes(totalBytes),
                    color = tokens.textPrimary,
                    style = KuroType.headingCard,
                )
                Text(
                    text = stringResource(R.string.storage_summary_subtitle),
                    color = tokens.textSecondary,
                    style = KuroType.labelMeta,
                )
            }
        }
    }
}

@Composable
private fun StorageRow(item: StorageItem, onRemove: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val icon = if (item.kind == "music") Lucide.Music else Lucide.CloudRain
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(KuroSpacing.lg),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
            Text(
                text = item.name,
                color = tokens.textPrimary,
                style = KuroType.bodyPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.isRemovable) {
                    formatBytes(item.sizeBytes)
                } else {
                    stringResource(R.string.storage_downloading, ((item.progress ?: 0f) * 100).roundToInt())
                },
                color = tokens.textSecondary,
                style = KuroType.labelMeta,
            )
        }
        if (item.isRemovable) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = stringResource(R.string.storage_remove_one_cd, item.name),
                    tint = tokens.destructive,
                    modifier = Modifier.size(KuroSpacing.lg),
                )
            }
        } else {
            CircularProgressIndicator(
                progress = { item.progress ?: 0f },
                modifier = Modifier.size(KuroSpacing.lg),
                color = tokens.accent,
            )
        }
    }
}

@Composable
private fun RemoveAllButton(onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.destructive.copy(alpha = 0.10f))
            .border(1.dp, tokens.destructive.copy(alpha = 0.30f), RoundedCornerShape(KuroSpacing.radiusLg))
            .clickable(onClick = onClick)
            .padding(vertical = KuroSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.storage_remove_all),
            color = tokens.destructive,
            style = KuroType.bodyPrimary,
        )
    }
}

@Composable
private fun StorageEmptyState() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = KuroSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            Image(
                painter = painterResource(R.drawable.cat_today_empty),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(KuroSpacing.journeyChartCatWidth),
            )
            Text(
                text = stringResource(R.string.storage_empty),
                color = tokens.textPrimary,
                style = KuroType.bodyPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.storage_empty_hint),
                color = tokens.textSecondary,
                style = KuroType.bodySecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(KuroSpacing.xs))
        }
    }
}
