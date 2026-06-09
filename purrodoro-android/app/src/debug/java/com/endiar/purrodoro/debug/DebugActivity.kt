package com.endiar.purrodoro.debug

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.PremiumOverride
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
// addFocusStamp removed — use inline stamp increment instead
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DebugScreen(
                    debugRepo = DebugRepository.instance,
                    appRepo = AppRepository.get(this),
                    onClose = { finish() },
                    onRestartApp = {
                        // Kill and relaunch the process so all in-memory state is re-read from DataStore
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugScreen(
    debugRepo: DebugRepository,
    appRepo: AppRepository,
    onClose: () -> Unit,
    onRestartApp: () -> Unit
) {
    val clockOverride by debugRepo.clockOverride.collectAsStateWithLifecycle()
    val isPremiumOverrideEnabled by debugRepo.isPremiumOverrideEnabled.collectAsStateWithLifecycle()
    val premiumOverrideValue by debugRepo.premiumOverrideValue.collectAsStateWithLifecycle()
    var showCatalog by remember { mutableStateOf(false) }

    if (showCatalog) {
        ComponentCatalogScreen()
        return
    }

    val appState by appRepo.appState.collectAsStateWithLifecycle(
        initialValue = com.endiar.purrodoro.data.AppState(
            glassReadabilityMode = com.endiar.purrodoro.model.GlassReadabilityMode.Adaptive,
            selectedSoundId = 1,
            completedSessions = 0,
            settings = com.endiar.purrodoro.model.TimerSettings(),
            nextTaskId = 7,
            stamps = emptyMap()
        )
    )
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Purrodoro Dev Tools", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }

            // Sticky status banner
            StatusBanner(
                clockOverride = clockOverride,
                isPremiumOverrideEnabled = isPremiumOverrideEnabled,
                premiumOverrideValue = premiumOverrideValue,
                onRestart = onRestartApp
            )

            HorizontalDivider()

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- Clock Override ---
                SectionTitle("CLOCK OVERRIDE")
                Text(
                    text = if (clockOverride != null) "Active: $clockOverride" else "Using real time",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (clockOverride != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                // 6 presets cover all 6 background ranges
                val clockPresets = listOf(
                    "05:00 🌅" to java.time.LocalTime.of(5, 0),   // dawn
                    "08:00 ☀️" to java.time.LocalTime.of(8, 0),   // morning
                    "12:00 🌤️" to java.time.LocalTime.of(12, 0),  // day
                    "17:00 🌇" to java.time.LocalTime.of(17, 0),  // evening
                    "20:00 🌆" to java.time.LocalTime.of(20, 0),  // night
                    "22:00 🌙" to java.time.LocalTime.of(22, 0),  // midnight
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    clockPresets.chunked(3).forEach { rowPresets ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPresets.forEach { (label, time) ->
                                OutlinedButton(onClick = {
                                    debugRepo.setClockOverride(time)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Clock set to $label",
                                            actionLabel = "Restart",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onRestartApp()
                                        }
                                    }
                                }) {
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        debugRepo.setClockOverride(null)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Clock cleared",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    enabled = clockOverride != null
                ) {
                    Text("Clear override")
                }

                HorizontalDivider()

                // --- Premium Override ---
                SectionTitle("PREMIUM OVERRIDE")
                val effectivePremium = PremiumOverride.overrideValue() ?: appState.isPremium
                Text(
                    text = "Effective: ${if (effectivePremium) "premium" else "free"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (effectivePremium) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    text = "DataStore: ${if (appState.isPremium) "true" else "false"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Use override",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = isPremiumOverrideEnabled,
                        onCheckedChange = debugRepo::setPremiumOverrideEnabled,
                    )
                }
                if (isPremiumOverrideEnabled) {
                    Text(
                        text = "Forced value",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { debugRepo.setPremiumOverrideValue(false) },
                            modifier = Modifier.weight(1f),
                            colors = if (!premiumOverrideValue) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                        ) {
                            Text("False (free)")
                        }
                        OutlinedButton(
                            onClick = { debugRepo.setPremiumOverrideValue(true) },
                            modifier = Modifier.weight(1f),
                            colors = if (premiumOverrideValue) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                        ) {
                            Text("True (premium)")
                        }
                    }
                }
                Text(
                    text = "DataStore value",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                appRepo.setIsPremium(false)
                                snackbarHostState.showSnackbar(
                                    message = "DataStore premium = false",
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Set false")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                appRepo.setIsPremium(true)
                                snackbarHostState.showSnackbar(
                                    message = "DataStore premium = true",
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Set true")
                    }
                }

                // placeholder for Task 3
                HorizontalDivider()

                // --- Session Injection ---
                SectionTitle("SESSION INJECTION")
                var injectionCount by remember { mutableIntStateOf(1) }

                Text(
                    "Date: ${java.time.LocalDate.now()} (today)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = { if (injectionCount > 1) injectionCount-- }) { Text("−") }
                    Text("$injectionCount", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { if (injectionCount < 10) injectionCount++ }) { Text("+") }
                }
                Button(
                    onClick = {
                        scope.launch {
                            var updatedStamps = appState.stamps
                            repeat(injectionCount) {
                                updatedStamps = updatedStamps + (java.time.LocalDate.now() to (updatedStamps[java.time.LocalDate.now()] ?: 0) + 1)
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

                // --- Day Data Injection ---
                // Seeds full SessionRecords (not just stamps) on a chosen day so
                // every Journey surface lights up: today timeline, weekly focus,
                // month heatmap, all-time totals, and the streak.
                SectionTitle("DAY DATA INJECTION")
                Text(
                    "Inserts completed focus sessions on a chosen day (~25 min each). " +
                        "Where it shows on Journey: Today timeline = today only · Weekly = last 7 days · " +
                        "Heatmap = current month only · All-time + History = any date. " +
                        "Reopen the Journey tab to refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                var targetDate by remember { mutableStateOf(LocalDate.now()) }
                var sessionsForDay by remember { mutableIntStateOf(4) }
                var showDatePicker by remember { mutableStateOf(false) }

                // Date selector — opens a Material date picker so any specific day
                // can be chosen, plus quick presets for the common cases.
                Text("Target day", style = MaterialTheme.typography.labelSmall)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val suffix = if (targetDate == LocalDate.now()) " (today)" else ""
                    Text("📅  $targetDate$suffix")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Today", 1 to "−1d", 7 to "−7d", 30 to "−30d").forEach { (off, label) ->
                        OutlinedButton(onClick = { targetDate = LocalDate.now().minusDays(off.toLong()) }) {
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }

                if (showDatePicker) {
                    val pickerState = rememberDatePickerState(
                        initialSelectedDateMillis = targetDate
                            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                pickerState.selectedDateMillis?.let { millis ->
                                    targetDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneOffset.UTC).toLocalDate()
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = pickerState)
                    }
                }

                // Sessions stepper.
                Column {
                    Text("Sessions", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { if (sessionsForDay > 1) sessionsForDay-- }) { Text("−") }
                        Text("$sessionsForDay", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { if (sessionsForDay < 12) sessionsForDay++ }) { Text("+") }
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            val records = buildDaySessions(targetDate, sessionsForDay, idBase = System.currentTimeMillis())
                            appRepo.appendSessionRecords(records)
                            appRepo.setStamps(appState.stamps + (targetDate to ((appState.stamps[targetDate] ?: 0) + sessionsForDay)))
                            appRepo.setCompletedSessions(appState.completedSessions + sessionsForDay)
                            snackbarHostState.showSnackbar(
                                message = "Seeded $sessionsForDay session${if (sessionsForDay > 1) "s" else ""} on $targetDate",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Insert on $targetDate")
                }
                // Bulk helper to make the heatmap + streak pop instantly.
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            var nextId = System.currentTimeMillis()
                            val all = mutableListOf<SessionRecord>()
                            var stamps = appState.stamps
                            var total = 0
                            repeat(30) { d ->
                                val date = LocalDate.now().minusDays(d.toLong())
                                val n = (1..6).random()
                                all += buildDaySessions(date, n, idBase = nextId)
                                nextId += n
                                stamps = stamps + (date to ((stamps[date] ?: 0) + n))
                                total += n
                            }
                            appRepo.appendSessionRecords(all)
                            appRepo.setStamps(stamps)
                            appRepo.setCompletedSessions(appState.completedSessions + total)
                            snackbarHostState.showSnackbar(
                                message = "Filled last 30 days ($total sessions)",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fill last 30 days (random)")
                }

                HorizontalDivider()

                // --- Cycle Complete Dialog ---
                SectionTitle("CYCLE COMPLETE DIALOG")
                Text(
                    "Flips the persisted pendingCycleCompletionAck flag so the dialog appears next time the home is in foreground.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Button(
                    onClick = {
                        scope.launch {
                            appRepo.setPendingCycleCompletionAck(true)
                            snackbarHostState.showSnackbar(
                                message = "Dialog armed — return to Home tab",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trigger cycle complete")
                }

                HorizontalDivider()

                // --- Onboarding / Tips ---
                SectionTitle("ONBOARDING + TIPS")
                Text(
                    "Resets first-run onboarding and the spotlight tips so both replay. Existing tasks, habits, and history are kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Button(
                    onClick = {
                        scope.launch {
                            appRepo.setHasSeenOnboarding(false)
                            appRepo.setHasSeenTutorial(false)
                            val result = snackbarHostState.showSnackbar(
                                message = "Onboarding and tips reset",
                                actionLabel = "Restart",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                onRestartApp()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Replay onboarding + tips")
                }

                HorizontalDivider()

                // --- Process ---
                SectionTitle("PROCESS")
                Button(
                    onClick = onRestartApp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Restart App")
                }
                Text(
                    "Overrides are saved to DataStore — app will relaunch with settings applied.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                HorizontalDivider()

                // --- Danger Zone ---
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        appRepo.clearSessionRecords()
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
                            "Wipes stamps, session records, and counters back to seed state.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Design System", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = { showCatalog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open Component Catalog")
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatusBanner(
    clockOverride: java.time.LocalTime?,
    isPremiumOverrideEnabled: Boolean,
    premiumOverrideValue: Boolean,
    onRestart: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasOverride = clockOverride != null || isPremiumOverrideEnabled
            if (!hasOverride) {
                Text(
                    "No overrides active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (clockOverride != null) {
                        Text(
                            "🕗 ${clockOverride.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (isPremiumOverrideEnabled) {
                        Text(
                            if (premiumOverrideValue) "★ Premium (override)" else "☆ Free (override)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            TextButton(onClick = onRestart) { Text("Restart ↺") }
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color = Color.Unspecified) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else color
    )
}

private val DEBUG_TASK_NAMES = listOf(
    "Deep Work", "Reading", "Writing", "Study", "Design", "Email", "Planning", "Coding"
)

/**
 * Builds [sessions] completed focus [SessionRecord]s on [date], one per focus
 * block, spaced 45 min apart starting at 09:00. Each block is [focusMinutes]
 * long, so the day's total focus is sessions × focusMinutes — enough to drive
 * the Journey timeline rows, weekly/monthly stats, heatmap intensity, and the
 * all-time totals. IDs are derived from [idBase] so a batch stays unique.
 */
private fun buildDaySessions(
    date: LocalDate,
    sessions: Int,
    idBase: Long,
    focusMinutes: Int = 25,
): List<SessionRecord> {
    val zone = ZoneId.systemDefault()
    return (0 until sessions).map { i ->
        val startTime = LocalTime.of(9, 0).plusMinutes((i * 45).toLong())
        val startMillis = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        val endMillis = startMillis + focusMinutes * 60_000L
        SessionRecord(
            id = idBase + i,
            startedAt = startMillis,
            endedAt = endMillis,
            focusMinutes = focusMinutes,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            completedFocusCount = 1,
            totalFocusCount = 1,
            status = SessionStatus.Completed,
            taskName = DEBUG_TASK_NAMES[i % DEBUG_TASK_NAMES.size],
            completedShortBreakCount = if (i < sessions - 1) 1 else 0,
            completedLongBreakCount = 0,
        )
    }
}

