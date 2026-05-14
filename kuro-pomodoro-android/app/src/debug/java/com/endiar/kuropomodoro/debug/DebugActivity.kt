package com.endiar.kuropomodoro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.addFocusStamp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DebugScreen(
                    debugRepo = DebugRepository.instance,
                    appRepo = AppRepository.get(this),
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
private fun DebugScreen(
    debugRepo: DebugRepository,
    appRepo: AppRepository,
    onClose: () -> Unit
) {
    val clockOverride by debugRepo.clockOverride.collectAsStateWithLifecycle()
    val speedUpTimer by debugRepo.speedUpTimer.collectAsStateWithLifecycle()
    val appState by appRepo.appState.collectAsStateWithLifecycle(
        initialValue = com.endiar.kuropomodoro.data.AppState(
            darkModePreview = false,
            glassReadabilityMode = com.endiar.kuropomodoro.model.GlassReadabilityMode.Adaptive,
            selectedSoundId = 1,
            completedSessions = 0,
            settings = com.endiar.kuropomodoro.model.TimerSettings(),
            nextTaskId = 7,
            selectedTodoListId = 1,
            stamps = emptyMap(),
            todoLists = emptyList()
        )
    )
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kuro Dev Tools", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }

            HorizontalDivider()

            // --- Clock Override ---
            SectionTitle("CLOCK OVERRIDE")
            Text(
                text = if (clockOverride != null) "Active: ${clockOverride}" else "Using real time",
                style = MaterialTheme.typography.bodySmall,
                color = if (clockOverride != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "08:00" to LocalTime.of(8, 0),
                    "12:00" to LocalTime.of(12, 0),
                    "17:00" to LocalTime.of(17, 0),
                    "22:00" to LocalTime.of(22, 0)
                ).forEach { (label, time) ->
                    OutlinedButton(onClick = { debugRepo.setClockOverride(time) }) {
                        Text(label)
                    }
                }
            }
            TextButton(
                onClick = { debugRepo.setClockOverride(null) },
                enabled = clockOverride != null
            ) {
                Text("Clear override")
            }

            HorizontalDivider()

            // --- Timer Speed-Up ---
            SectionTitle("TIMER")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Speed up timer", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "25 min → ~150 seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = speedUpTimer,
                    onCheckedChange = { debugRepo.setSpeedUpTimer(it) }
                )
            }

            HorizontalDivider()

            // --- Session Injection ---
            SectionTitle("SESSION INJECTION")
            var injectionCount by remember { mutableIntStateOf(1) }
            var injectionDate by remember { mutableStateOf(LocalDate.now()) }

            Text(
                "Date: $injectionDate (today)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { if (injectionCount > 1) injectionCount-- }
                ) { Text("−") }
                Text("$injectionCount", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { if (injectionCount < 10) injectionCount++ }
                ) { Text("+") }
            }
            Button(
                onClick = {
                    scope.launch {
                        var updatedStamps = appState.stamps
                        repeat(injectionCount) {
                            updatedStamps = addFocusStamp(updatedStamps, injectionDate)
                        }
                        appRepo.setStamps(updatedStamps)
                        appRepo.setCompletedSessions(appState.completedSessions + injectionCount)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add $injectionCount Session${if (injectionCount > 1) "s" else ""}")
            }

            HorizontalDivider()

            // --- Danger Zone ---
            SectionTitle("DANGER ZONE", color = MaterialTheme.colorScheme.error)
            var resetConfirming by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (!resetConfirming) {
                        resetConfirming = true
                    } else {
                        scope.launch {
                            appRepo.setStamps(emptyMap())
                            appRepo.setCompletedSessions(0)
                            appRepo.setTodoLists(emptyList())
                        }
                        resetConfirming = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (resetConfirming) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (resetConfirming) MaterialTheme.colorScheme.onError
                                   else MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (resetConfirming) "Are you sure? Tap again to confirm"
                    else "Reset All Data"
                )
            }
            if (resetConfirming) {
                TextButton(onClick = { resetConfirming = false }) {
                    Text("Cancel")
                }
            }
            Text(
                "Wipes stamps, sessions, and todos back to seed state.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: androidx.compose.ui.graphics.Color = Color.Unspecified) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else color
    )
}
