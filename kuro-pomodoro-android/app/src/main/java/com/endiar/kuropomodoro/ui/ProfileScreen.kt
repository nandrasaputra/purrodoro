package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.TimerSettings

private val CreamBackground = Color(0xFFFFF8F0)
private val WarmBrown = Color(0xFF8B7355)
private val DividerColor = Color(0xFFE8DDD0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    settings: TimerSettings,
    onBack: () -> Unit,
    onUpdateSettings: (TimerSettings) -> Unit,
    notificationSoundEnabled: Boolean,
    onUpdateNotificationSound: (Boolean) -> Unit,
) {
    var editingField by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF3C2F1E)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF3C2F1E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBackground,
                    scrolledContainerColor = CreamBackground
                )
            )
        },
        containerColor = CreamBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Timer Settings section ──────────────────────────────────
            SectionHeader(title = "TIMER SETTINGS")

            SettingsRow(
                label = "Focus Duration",
                value = "${settings.focusMinutes} min",
                onClick = { editingField = "focus" }
            )
            SettingsRow(
                label = "Short Break",
                value = "${settings.shortBreakMinutes} min",
                onClick = { editingField = "short" }
            )
            SettingsRow(
                label = "Long Break",
                value = "${settings.longBreakMinutes} min",
                onClick = { editingField = "long" }
            )
            SettingsRow(
                label = "Cycle Size",
                value = "${settings.focusSessionsPerCycle} sessions",
                onClick = { editingField = "cycle" }
            )

            // Auto-start toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Auto-start next session",
                    fontSize = 16.sp,
                    color = Color(0xFF3C2F1E)
                )
                Switch(
                    checked = settings.autoStartNextSession,
                    onCheckedChange = { checked ->
                        onUpdateSettings(settings.copy(autoStartNextSession = checked))
                    }
                )
            }

            SectionDivider()

            // ── About section ───────────────────────────────────────────
            SectionHeader(title = "ABOUT")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kuro Pomodoro",
                    fontSize = 16.sp,
                    color = Color(0xFF3C2F1E)
                )
                Text(
                    text = "v1.0",
                    fontSize = 14.sp,
                    color = WarmBrown
                )
            }

            SectionDivider()
        }
    }

    // ── Edit dialogs ────────────────────────────────────────────────────
    when (editingField) {
        "focus" -> NumericEditDialog(
            title = "Focus Duration",
            value = settings.focusMinutes,
            min = 1,
            max = 120,
            unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(focusMinutes = it)) },
            onDismiss = { editingField = null }
        )
        "short" -> NumericEditDialog(
            title = "Short Break",
            value = settings.shortBreakMinutes,
            min = 1,
            max = 120,
            unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(shortBreakMinutes = it)) },
            onDismiss = { editingField = null }
        )
        "long" -> NumericEditDialog(
            title = "Long Break",
            value = settings.longBreakMinutes,
            min = 1,
            max = 120,
            unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(longBreakMinutes = it)) },
            onDismiss = { editingField = null }
        )
        "cycle" -> NumericEditDialog(
            title = "Cycle Size",
            value = settings.focusSessionsPerCycle,
            min = 1,
            max = 10,
            unit = "sessions",
            onConfirm = { onUpdateSettings(settings.copy(focusSessionsPerCycle = it)) },
            onDismiss = { editingField = null }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = WarmBrown,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = DividerColor, thickness = 1.dp)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color(0xFF3C2F1E)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = WarmBrown
        )
    }
}

@Composable
private fun NumericEditDialog(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    unit: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember(value) { mutableIntStateOf(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3C2F1E)
            )
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { if (current > min) current-- },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(
                        text = "−",
                        fontSize = 22.sp,
                        color = if (current > min) WarmBrown else Color(0xFFCCBBAA)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "$current $unit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3C2F1E)
                )
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { if (current < max) current++ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(
                        text = "+",
                        fontSize = 22.sp,
                        color = if (current < max) WarmBrown else Color(0xFFCCBBAA)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(current)
                onDismiss()
            }) {
                Text("OK", color = WarmBrown, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8B7355).copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFFFFF8F0)
    )
}
