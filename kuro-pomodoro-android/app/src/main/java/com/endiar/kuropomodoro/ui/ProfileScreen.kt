package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.GlassReadabilityMode
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.GlassPill
import com.endiar.kuropomodoro.ui.components.MockBadge
import com.endiar.kuropomodoro.ui.components.StepperButton
import com.endiar.kuropomodoro.ui.theme.kuroTokens

@Composable
fun ProfileScreen(
    settings: TimerSettings,
    glassReadabilityMode: GlassReadabilityMode,
    glassTreatment: GlassTreatment,
    onGlassReadabilityModeChanged: (GlassReadabilityMode) -> Unit,
    onDurationChanged: (TimerMode, Int) -> Unit,
    onCycleSizeChanged: (Int) -> Unit,
    onResetSettings: () -> Unit,
    onMockFeature: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var showResetConfirm by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Me",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.textPrimary
        )

        // Profile / avatar card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glassTreatment = glassTreatment,
            overBackdrop = overBackdrop,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Nandra",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Stay focused with kuro",
                        fontSize = 13.sp,
                        color = tokens.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(tokens.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NS",
                        color = tokens.accentOn,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Timer settings card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glassTreatment = glassTreatment,
            overBackdrop = overBackdrop,
        ) {
            Text(
                text = "TIMER SETTINGS",
                fontSize = 13.sp,
                color = tokens.textMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(8.dp))
            DurationRow(
                label = TimerMode.Focus.label,
                minutes = settings.focusMinutes,
                onMinus = { onDurationChanged(TimerMode.Focus, (settings.focusMinutes - 1).coerceIn(1, 60)) },
                onPlus = { onDurationChanged(TimerMode.Focus, (settings.focusMinutes + 1).coerceIn(1, 60)) },
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
            Spacer(Modifier.height(8.dp))
            DurationRow(
                label = TimerMode.ShortBreak.label,
                minutes = settings.shortBreakMinutes,
                onMinus = { onDurationChanged(TimerMode.ShortBreak, (settings.shortBreakMinutes - 1).coerceIn(1, 60)) },
                onPlus = { onDurationChanged(TimerMode.ShortBreak, (settings.shortBreakMinutes + 1).coerceIn(1, 60)) },
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
            Spacer(Modifier.height(8.dp))
            DurationRow(
                label = TimerMode.LongBreak.label,
                minutes = settings.longBreakMinutes,
                onMinus = { onDurationChanged(TimerMode.LongBreak, (settings.longBreakMinutes - 1).coerceIn(1, 60)) },
                onPlus = { onDurationChanged(TimerMode.LongBreak, (settings.longBreakMinutes + 1).coerceIn(1, 60)) },
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
            Spacer(Modifier.height(8.dp))
            DurationRow(
                label = "Focus sessions / cycle",
                minutes = settings.focusSessionsPerCycle,
                unit = "sessions",
                onMinus = { onCycleSizeChanged((settings.focusSessionsPerCycle - 1).coerceIn(1, 10)) },
                onPlus = { onCycleSizeChanged((settings.focusSessionsPerCycle + 1).coerceIn(1, 10)) },
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glassTreatment = glassTreatment,
            overBackdrop = overBackdrop,
        ) {
            Text(
                text = "FOCUS SCREEN READABILITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = tokens.textMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Only affects the cat backdrop on the Focus tab.",
                fontSize = 12.sp,
                color = tokens.textSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassReadabilityMode.values().toList().forEach { mode ->
                    GlassPill(
                        text = mode.label,
                        selected = mode == glassReadabilityMode,
                        onClick = { onGlassReadabilityModeChanged(mode) },
                        glassTreatment = glassTreatment,
                        overBackdrop = overBackdrop,
                    )
                }
            }
        }

        // Reset button
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = { showResetConfirm = true }),
            glassTreatment = glassTreatment,
            overBackdrop = overBackdrop,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Reset to defaults",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Restore default timer settings",
                        fontSize = 12.sp,
                        color = tokens.textSecondary
                    )
                }
                Text("↺", fontSize = 22.sp, color = tokens.textSecondary)
            }
        }

        // Mock features section label
        Text(
            text = "FEATURES",
            fontSize = 10.sp,
            color = tokens.textFaint,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MockFeatureSquare(
                name = "Focus Together",
                icon = "\uD83D\uDC65",
                description = "Co-focus with friends",
                onClick = { onMockFeature("Focus Together is a mock social focus room for this prototype.") },
                modifier = Modifier.weight(1f),
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
            MockFeatureSquare(
                name = "Reminders",
                icon = "\uD83D\uDD14",
                description = "Smart break reminders",
                onClick = { onMockFeature("Reminders are mocked here; notifications are not scheduled yet.") },
                modifier = Modifier.weight(1f),
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MockFeatureSquare(
                name = "D-Day Tracker",
                icon = "\uD83D\uDCC5",
                description = "Track important dates",
                onClick = { onMockFeature("D-Day Tracker is a placeholder for future goal countdowns.") },
                modifier = Modifier.weight(1f),
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
            MockFeatureSquare(
                name = "Premium Sounds",
                icon = "\uD83C\uDFB5",
                description = "Unlock all sound packs",
                onClick = { onMockFeature("Premium Sounds is simulated in this clone and does not start a purchase.") },
                modifier = Modifier.weight(1f),
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text(
                    text = "Reset timer settings?",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "This will restore Focus to 25 min, Short break to 5 min, Long break to 15 min, and Sessions/cycle to 4.",
                    color = tokens.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    onResetSettings()
                }) {
                    Text("Reset", color = tokens.accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = tokens.textSecondary)
                }
            },
            containerColor = tokens.cardFill,
            titleContentColor = tokens.textPrimary,
            textContentColor = tokens.textSecondary,
        )
    }
}

@Composable
private fun DurationRow(
    label: String,
    minutes: Int,
    unit: String = "min",
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepperButton(
                label = "−",
                onClick = onMinus,
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
            Text(
                text = "$minutes $unit",
                color = tokens.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 54.dp),
                textAlign = TextAlign.Center
            )
            StepperButton(
                label = "+",
                onClick = onPlus,
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            )
        }
    }
}

@Composable
private fun MockFeatureSquare(
    name: String,
    icon: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    GlassCard(
        modifier = modifier.clickable(role = Role.Button, onClickLabel = name, onClick = onClick),
        glassTreatment = glassTreatment,
        overBackdrop = overBackdrop,
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            MockBadge()
        }
        Spacer(Modifier.height(2.dp))
        Text(description, color = tokens.textSecondary, fontSize = 11.sp)
    }
}
