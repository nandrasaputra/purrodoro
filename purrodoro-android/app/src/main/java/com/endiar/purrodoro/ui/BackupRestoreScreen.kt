package com.endiar.purrodoro.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Upload
import com.endiar.purrodoro.R
import com.endiar.purrodoro.data.backup.RestoreOutcome
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroBackScreenTopBar
import com.endiar.purrodoro.ui.designsystem.KuroElevatedCard
import com.endiar.purrodoro.ui.designsystem.KuroSectionHeader
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.launch

/**
 * Backup & Restore — a pushed full-screen surface (entered from Profile).
 * Export is always available; **restore is gated to guest / signed-out** to
 * avoid overwriting an account's cloud data (see the data-backup spec).
 *
 * The screen owns the Storage Access Framework launchers; the actual file IO +
 * decode + apply runs in the [onExport] / [onRestore] suspend callbacks
 * provided by the host (dispatched off the main thread there).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    isGuest: Boolean,
    defaultFileName: String,
    onBack: () -> Unit,
    onExport: suspend (android.net.Uri) -> Boolean,
    onRestore: suspend (android.net.Uri) -> RestoreOutcome,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var working by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportError = stringResource(R.string.backup_export_error)
    val exportSuccess = stringResource(R.string.backup_export_success)
    val restoreSuccess = stringResource(R.string.backup_restore_success)
    val errMalformed = stringResource(R.string.backup_restore_error_malformed)
    val errVersion = stringResource(R.string.backup_restore_error_version)
    val errTooLarge = stringResource(R.string.backup_restore_error_too_large)
    val errRead = stringResource(R.string.backup_restore_error_read)
    val errApply = stringResource(R.string.backup_restore_error_apply)

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        working = true
        statusMessage = null
        scope.launch {
            val ok = onExport(uri)
            working = false
            statusMessage = if (ok) exportSuccess else exportError
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        // Defer the destructive work behind an explicit confirm.
        if (uri != null) pendingRestoreUri = uri
    }

    Scaffold(
        topBar = {
            KuroBackScreenTopBar(title = stringResource(R.string.backup_title), onBack = onBack)
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
            SummaryCard()

            KuroSectionHeader(title = stringResource(R.string.backup_section_backup))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                ActionRow(
                    icon = Lucide.Upload,
                    label = stringResource(R.string.backup_export_label),
                    hint = stringResource(R.string.backup_export_hint),
                    enabled = !working,
                    onClick = {
                        statusMessage = null
                        createLauncher.launch(defaultFileName)
                    },
                )
            }

            KuroSectionHeader(title = stringResource(R.string.backup_section_restore))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                ActionRow(
                    icon = Lucide.Download,
                    label = stringResource(R.string.backup_import_label),
                    hint = if (isGuest) {
                        stringResource(R.string.backup_import_hint)
                    } else {
                        stringResource(R.string.backup_restore_blocked_signed_in)
                    },
                    enabled = !working && isGuest,
                    onClick = {
                        statusMessage = null
                        openLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*"))
                    },
                )
            }

            if (working) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(KuroSpacing.lg), color = tokens.accent)
                    Text(stringResource(R.string.backup_working), color = tokens.textSecondary, style = KuroType.bodySecondary)
                }
            }

            statusMessage?.let { msg ->
                Text(text = msg, color = tokens.textSecondary, style = KuroType.bodySecondary)
            }
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = {
                Text(stringResource(R.string.backup_confirm_title), color = tokens.textPrimary, style = KuroType.headingCard)
            },
            text = {
                Text(stringResource(R.string.backup_confirm_body), color = tokens.textSecondary)
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    working = true
                    statusMessage = null
                    scope.launch {
                        val outcome = onRestore(uri)
                        working = false
                        statusMessage = when (outcome) {
                            RestoreOutcome.Success -> restoreSuccess
                            RestoreOutcome.Malformed -> errMalformed
                            is RestoreOutcome.UnsupportedVersion -> errVersion
                            RestoreOutcome.TooLarge -> errTooLarge
                            RestoreOutcome.ReadError -> errRead
                            RestoreOutcome.ApplyError -> errApply
                        }
                    }
                }) {
                    Text(stringResource(R.string.backup_confirm_action), color = tokens.destructive, style = KuroType.labelStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text(stringResource(R.string.common_cancel), color = tokens.textSecondary)
                }
            },
            containerColor = tokens.pageBackground,
        )
    }
}

@Composable
private fun SummaryCard() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Icon(
                imageVector = Lucide.ShieldCheck,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(KuroSpacing.lg),
            )
            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                Text(stringResource(R.string.backup_summary_title), color = tokens.textPrimary, style = KuroType.headingCard)
                Text(stringResource(R.string.backup_summary_body), color = tokens.textSecondary, style = KuroType.labelMeta)
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    hint: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val contentTint = if (enabled) tokens.textPrimary else tokens.textSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(vertical = KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tokens.accent else tokens.textSecondary,
            modifier = Modifier.size(KuroSpacing.lg),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
            Text(text = label, color = contentTint, style = KuroType.bodyPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = hint, color = tokens.textSecondary, style = KuroType.labelMeta)
        }
    }
}
