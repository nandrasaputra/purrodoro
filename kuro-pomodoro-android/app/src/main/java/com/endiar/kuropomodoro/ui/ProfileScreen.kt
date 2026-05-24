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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.TimerSettings

private val CreamBg       = Color(0xFFFFF8F0)
private val CardWhite     = Color(0xFFFFFFFF)
private val SectionBrown  = Color(0xFFC07850)
private val LabelDark     = Color(0xFF3C2F1E)
private val ValueBrown    = Color(0xFF8B7355)
private val SubtitleGray  = Color(0xFF9E9083)
private val DividerColor  = Color(0xFFEDE5D8)
private val IconBrown     = Color(0xFFC07850)
private val SwitchThumb   = Color(0xFFFFFFFF)
private val SwitchTrackOn = Color(0xFFC07850)
private val SwitchTrackOff= Color(0xFFD9CFC4)

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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LabelDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBg,
                    scrolledContainerColor = CreamBg
                )
            )
        },
        containerColor = CreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Settings",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = LabelDark
            )
            Text(
                text = "Customize your focus experience.",
                fontSize = 14.sp,
                color = SubtitleGray,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(title = "TIMER", icon = Icons.Outlined.Settings)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                TappableRow(
                    icon = Icons.Outlined.DateRange,
                    label = "Focus time",
                    value = "${settings.focusMinutes} min",
                    onClick = { editingField = "focus" }
                )
                CardDivider()
                TappableRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "Short break",
                    value = "${settings.shortBreakMinutes} min",
                    onClick = { editingField = "short" }
                )
                CardDivider()
                TappableRow(
                    icon = Icons.Outlined.Favorite,
                    label = "Long break",
                    value = "${settings.longBreakMinutes} min",
                    onClick = { editingField = "long" }
                )
                CardDivider()
                TappableRow(
                    icon = Icons.Outlined.Refresh,
                    label = "Long break after",
                    value = "${settings.focusSessionsPerCycle} sessions",
                    onClick = { editingField = "cycle" }
                )
                CardDivider()
                ToggleRow(
                    icon = Icons.Outlined.PlayArrow,
                    label = "Auto-start next session",
                    subtitle = "Automatically continue to the next session",
                    checked = settings.autoStartNextSession,
                    onCheckedChange = { onUpdateSettings(settings.copy(autoStartNextSession = it)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(title = "NOTIFICATIONS", icon = Icons.Outlined.Notifications)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                ToggleRow(
                    icon = Icons.Outlined.Notifications,
                    label = "Enable notification sound",
                    subtitle = "Play sound when session ends",
                    checked = notificationSoundEnabled,
                    onCheckedChange = onUpdateNotificationSound
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(title = "ABOUT", icon = Icons.Outlined.Info)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = IconBrown,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Kuro Pomodoro", fontSize = 15.sp, color = LabelDark)
                    }
                    Text(text = "v1.0", fontSize = 13.sp, color = ValueBrown)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
private fun SectionLabel(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = SectionBrown, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, color = SectionBrown)
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), color = DividerColor, thickness = 0.5.dp)
}

@Composable
private fun TappableRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = IconBrown, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 15.sp, color = LabelDark, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 13.sp, color = ValueBrown)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "›", fontSize = 18.sp, color = ValueBrown)
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = IconBrown, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 15.sp, color = LabelDark)
            Text(text = subtitle, fontSize = 12.sp, color = SubtitleGray, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SwitchThumb,
                checkedTrackColor = SwitchTrackOn,
                uncheckedThumbColor = SwitchThumb,
                uncheckedTrackColor = SwitchTrackOff,
                uncheckedBorderColor = SwitchTrackOff
            )
        )
    }
}

@Composable
private fun NumericEditDialog(
    title: String, value: Int, min: Int, max: Int, unit: String,
    onConfirm: (Int) -> Unit, onDismiss: () -> Unit
) {
    var current by remember(value) { mutableIntStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CreamBg,
        title = { Text(text = title, fontWeight = FontWeight.SemiBold, color = LabelDark) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { if (current > min) current-- }, modifier = Modifier.size(40.dp)) {
                    Text(text = "−", fontSize = 22.sp, color = if (current > min) SwitchTrackOn else Color(0xFFCCBBAA))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "$current $unit", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LabelDark)
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { if (current < max) current++ }, modifier = Modifier.size(40.dp)) {
                    Text(text = "+", fontSize = 22.sp, color = if (current < max) SwitchTrackOn else Color(0xFFCCBBAA))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) {
                Text("OK", color = SwitchTrackOn, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ValueBrown.copy(alpha = 0.6f))
            }
        }
    )
}
