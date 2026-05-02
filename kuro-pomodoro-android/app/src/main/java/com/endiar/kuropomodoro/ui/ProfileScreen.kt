package com.endiar.kuropomodoro.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.SectionTitle

@Composable
fun ProfileScreen(
    settings: TimerSettings,
    darkModePreview: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onDurationChanged: (TimerMode, Int) -> Unit,
    onMockFeature: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Kuro's room",
            subtitle = "Local settings for your cozy focus companion"
        )

        LocalProfileCard()

        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Timer lengths",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            DurationControl(
                mode = TimerMode.Focus,
                minutes = settings.focusMinutes,
                onDurationChanged = onDurationChanged
            )
            DurationControl(
                mode = TimerMode.ShortBreak,
                minutes = settings.shortBreakMinutes,
                onDurationChanged = onDurationChanged
            )
            DurationControl(
                mode = TimerMode.LongBreak,
                minutes = settings.longBreakMinutes,
                onDurationChanged = onDurationChanged
            )
        }

        DarkModePreviewCard(
            checked = darkModePreview,
            onCheckedChange = onDarkModeChanged
        )

        MockFeatureCard(
            title = "Focus Together",
            description = "Invite a friend into a shared quiet room.",
            actionLabel = "Preview Focus Together",
            onClick = { onMockFeature("Focus Together is a mock social focus room for this prototype.") }
        )
        MockFeatureCard(
            title = "Reminders",
            description = "Nudge your next session at the gentlest time.",
            actionLabel = "Preview Reminders",
            onClick = { onMockFeature("Reminders are mocked here; notifications are not scheduled yet.") }
        )
        MockFeatureCard(
            title = "D-Day Tracker",
            description = "Count down to exams, launches, and tiny victories.",
            actionLabel = "Preview D-Day Tracker",
            onClick = { onMockFeature("D-Day Tracker is a placeholder for future goal countdowns.") }
        )
        MockFeatureCard(
            title = "Premium Pass",
            description = "Unlock extra sounds, rooms, and cat outfits later.",
            actionLabel = "Preview Premium Pass",
            onClick = { onMockFeature("Premium Pass is simulated in this clone and does not start a purchase.") }
        )
    }
}

@Composable
private fun LocalProfileCard() {
    val colorScheme = MaterialTheme.colorScheme

    KuroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colorScheme.primaryContainer
            ) {
                Text(
                    text = "Ku",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Local Pomodoro Keeper",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Saved on this device only. No account or cloud sync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun DurationControl(
    mode: TimerMode,
    minutes: Int,
    onDurationChanged: (TimerMode, Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$minutes minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
        DurationStepButton(
            label = "Decrease ${mode.label} duration",
            text = "-",
            enabled = minutes > 1,
            onClick = { onDurationChanged(mode, minutes - 1) }
        )
        DurationStepButton(
            label = "Increase ${mode.label} duration",
            text = "+",
            enabled = true,
            onClick = { onDurationChanged(mode, minutes + 1) }
        )
    }
}

@Composable
private fun DurationStepButton(label: String, text: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = label }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DarkModePreviewCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    KuroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dark mode preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Try a nighttime palette without changing system settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics { contentDescription = "Dark mode preview" }
            )
        }
    }
}

@Composable
private fun MockFeatureCard(title: String, description: String, actionLabel: String, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    KuroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "Mock",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
