package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.formatSeconds
import com.endiar.kuropomodoro.ui.components.CatCompanion
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.components.ModeChip
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.components.TimerRing

@Composable
fun TimerScreen(
    mode: TimerMode,
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    completedSessionsToday: Int,
    currentTask: FocusTask?,
    onModeSelected: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val progress = if (totalSeconds > 0) {
        remainingSeconds.toFloat() / totalSeconds.toFloat()
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Kuro focus time",
            subtitle = "A soft timer for one cozy session at a time"
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerMode.entries.forEach { timerMode ->
                ModeChip(
                    mode = timerMode,
                    selected = timerMode == mode,
                    onClick = { onModeSelected(timerMode) }
                )
            }
        }

        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CatCompanion()
                TimerRing(
                    timeText = formatSeconds(remainingSeconds),
                    progress = progress
                )
                Text(
                    text = "$completedSessionsToday focus sessions today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KuroPrimaryButton(
                        text = if (isRunning) "Pause" else "Start",
                        onClick = onStartPause,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onReset) {
                        Text("Reset", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Current task",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (currentTask == null) {
                Text(
                    text = "Pick a task from your list when you are ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
            } else {
                Text(
                    text = currentTask.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${currentTask.minutes} min focus buddy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        }
    }
}
