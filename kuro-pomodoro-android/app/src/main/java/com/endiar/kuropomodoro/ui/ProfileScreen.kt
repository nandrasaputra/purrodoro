package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.endiar.kuropomodoro.model.AppearanceMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.AppearanceModeToggle
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.designsystem.KuroDivider
import com.endiar.kuropomodoro.ui.designsystem.KuroSectionHeader
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroStepper
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.kuroTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    settings: TimerSettings,
    onBack: () -> Unit,
    onUpdateSettings: (TimerSettings) -> Unit,
    notificationSoundEnabled: Boolean,
    onUpdateNotificationSound: (Boolean) -> Unit,
    appearanceMode: AppearanceMode = AppearanceMode.Auto,
    onUpdateAppearanceMode: (AppearanceMode) -> Unit = {},
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var editingField by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = tokens.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.pageBackground,
                    scrolledContainerColor = tokens.pageBackground
                )
            )
        },
        containerColor = tokens.pageBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KuroSpacing.lg)
        ) {
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = "Settings",
                style = KuroType.headingSettings,
                color = tokens.textPrimary,
            )
            Text(
                text = "Customize your focus experience.",
                style = KuroType.labelStrong,
                color = tokens.textSecondary,
                modifier = Modifier.padding(top = KuroSpacing.xs)
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = "TIMER")
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                TappableRow(
                    icon = Icons.Outlined.DateRange,
                    label = "Focus time",
                    value = "${settings.focusMinutes} min",
                    onClick = { editingField = "focus" },
                )
                KuroDivider()
                TappableRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "Short break",
                    value = "${settings.shortBreakMinutes} min",
                    onClick = { editingField = "short" },
                )
                KuroDivider()
                TappableRow(
                    icon = Icons.Outlined.Favorite,
                    label = "Long break",
                    value = "${settings.longBreakMinutes} min",
                    onClick = { editingField = "long" },
                )
                KuroDivider()
                TappableRow(
                    icon = Icons.Outlined.Refresh,
                    label = "Long break after",
                    value = "${settings.focusSessionsPerCycle} sessions",
                    onClick = { editingField = "cycle" },
                )
                KuroDivider()
                ToggleRow(
                    icon = Icons.Outlined.PlayArrow,
                    label = "Auto-start next session",
                    subtitle = "Automatically continue to the next session",
                    checked = settings.autoStartNextSession,
                    onCheckedChange = { onUpdateSettings(settings.copy(autoStartNextSession = it)) },
                )
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = "NOTIFICATIONS")
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                ToggleRow(
                    icon = Icons.Outlined.Notifications,
                    label = "Enable notification sound",
                    subtitle = "Play sound when session ends",
                    checked = notificationSoundEnabled,
                    onCheckedChange = onUpdateNotificationSound,
                )
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = "APPEARANCE")
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = tokens.accent,
                            modifier = Modifier.size(KuroSpacing.lg),
                        )
                        Spacer(modifier = Modifier.width(KuroSpacing.md))
                        Column {
                            Text(
                                text = "Theme",
                                style = KuroType.bodyPrimary,
                                color = tokens.textPrimary,
                            )
                            Text(
                                text = "Choose light, dark, or follow system",
                                style = KuroType.labelMeta,
                                color = tokens.textSecondary,
                            )
                        }
                    }
                    AppearanceModeToggle(
                        mode = appearanceMode,
                        onModeChanged = onUpdateAppearanceMode,
                    )
                }
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = "ABOUT")
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = tokens.accent,
                            modifier = Modifier.size(KuroSpacing.lg),
                        )
                        Spacer(modifier = Modifier.width(KuroSpacing.md))
                        Text(text = "Kuro Pomodoro", style = KuroType.bodyPrimary, color = tokens.textPrimary)
                    }
                    Text(text = "v1.0", style = KuroType.bodySecondary, color = tokens.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xxl))
        }
    }

    when (editingField) {
        "focus" -> NumericEditDialog(
            title = "Focus time",
            value = settings.focusMinutes,
            min = 1, max = 120, unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(focusMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "short" -> NumericEditDialog(
            title = "Short break",
            value = settings.shortBreakMinutes,
            min = 1, max = 120, unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(shortBreakMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "long" -> NumericEditDialog(
            title = "Long break",
            value = settings.longBreakMinutes,
            min = 1, max = 120, unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(longBreakMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "cycle" -> NumericEditDialog(
            title = "Long break after",
            value = settings.focusSessionsPerCycle,
            min = 1, max = 10, unit = "sessions",
            onConfirm = { onUpdateSettings(settings.copy(focusSessionsPerCycle = it)); editingField = null },
            onDismiss = { editingField = null }
        )
    }
}

@Composable
private fun TappableRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.lg))
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Text(text = label, style = KuroType.bodyPrimary, color = tokens.textPrimary, modifier = Modifier.weight(1f))
        Text(text = value, style = KuroType.bodySecondary, color = tokens.textSecondary)
        Spacer(modifier = Modifier.width(KuroSpacing.xs))
        Text(text = "›", style = KuroType.chevron, color = tokens.textSecondary)
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.lg))
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = KuroType.bodyPrimary, color = tokens.textPrimary)
            Text(text = subtitle, style = KuroType.labelMeta, color = tokens.textSecondary)
        }
        Spacer(modifier = Modifier.width(KuroSpacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.accentOn,
                checkedTrackColor = tokens.accent,
                uncheckedThumbColor = tokens.accentOn,
                uncheckedTrackColor = tokens.cardBorder,
                uncheckedBorderColor = tokens.cardBorder,
            ),
        )
    }
}

@Composable
private fun NumericEditDialog(
    title: String, value: Int, min: Int, max: Int, unit: String,
    onConfirm: (Int) -> Unit, onDismiss: () -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var current by remember(value) { mutableIntStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.pageBackground,
        title = { Text(text = title, style = KuroType.headingCard, color = tokens.textPrimary) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                KuroStepper(label = "−", enabled = current > min, large = true, onClick = { if (current > min) current-- })
                Spacer(modifier = Modifier.width(KuroSpacing.md))
                Text(text = "$current $unit", style = KuroType.headingCard, color = tokens.textPrimary)
                Spacer(modifier = Modifier.width(KuroSpacing.md))
                KuroStepper(label = "+", enabled = current < max, large = true, onClick = { if (current < max) current++ })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) {
                Text("OK", color = tokens.accent, style = KuroType.labelStrong)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = tokens.textSecondary)
            }
        }
    )
}
