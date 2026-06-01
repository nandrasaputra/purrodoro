# Home Timer UI Revamp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current Timer tab dashboard with a near-1:1 Compose implementation of the provided warm, card-based Home/Timer reference.

**Architecture:** Keep `TimerService`, `TimerConnection`, `TimerStateMachine`, DataStore, records, stamps, habits, and notification behavior unchanged. Add pure Home display helpers for greeting, metrics, and Pomodoro labels, then create a focused `HomeTimerScreen` composable that reads the existing runtime inputs and invokes the existing timer callbacks. Wire `KuroPomodoroApp` to use the new Home composable in the `AppTab.Timer` branch.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, existing app models and timer controller.

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerModels.kt` | Create pure display helpers for greeting, today metrics, task selection, and Pomodoro progress |
| `app/src/test/java/com/endiar/kuropomodoro/ui/HomeTimerModelsTest.kt` | Create unit tests for the new pure helpers |
| `app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerScreen.kt` | Create the new Home/Timer Compose screen and visual subcomponents |
| `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` | Replace the `FocusDashboard` Timer branch with `HomeTimerScreen`; keep existing callbacks and completion dialog |
| `app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt` | No planned change unless import cleanup is required by the wiring swap |

## Task 1: Add Home display model helpers

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerModels.kt`
- Create: `app/src/test/java/com/endiar/kuropomodoro/ui/HomeTimerModelsTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/endiar/kuropomodoro/ui/HomeTimerModelsTest.kt`:

```kotlin
package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeTimerModelsTest {
    @Test
    fun greetingCopyUsesTimeOfDay() {
        assertEquals(
            HomeGreeting("Good morning,", "Let's make this morning meaningful."),
            homeGreetingFor(LocalTime.of(8, 0))
        )
        assertEquals(
            HomeGreeting("Good afternoon,", "Keep the afternoon moving gently."),
            homeGreetingFor(LocalTime.of(14, 0))
        )
        assertEquals(
            HomeGreeting("Good evening,", "Let's make this evening meaningful."),
            homeGreetingFor(LocalTime.of(20, 0))
        )
    }

    @Test
    fun todayFocusMetricsCountsOnlyRecordsEndedToday() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 5, 23)
        val yesterday = today.minusDays(1)
        val records = listOf(
            record(
                endedAt = millisFor(today, 9, 0, zone),
                focusMinutes = 25,
                completedFocusCount = 2,
                status = SessionStatus.Completed
            ),
            record(
                endedAt = millisFor(today, 11, 0, zone),
                focusMinutes = 25,
                completedFocusCount = 1,
                status = SessionStatus.Partial
            ),
            record(
                endedAt = millisFor(yesterday, 11, 0, zone),
                focusMinutes = 25,
                completedFocusCount = 4,
                status = SessionStatus.Completed
            )
        )

        val metrics = todayFocusMetrics(records, today, zone)

        assertEquals("1h 15m", metrics.focusTimeLabel)
        assertEquals("3", metrics.sessionsLabel)
        assertEquals("1", metrics.completedLabel)
    }

    @Test
    fun continueTaskPrefersActiveThenIncompleteTaskWithProgress() {
        val active = FocusTask(
            id = 2,
            title = "Compose Migration",
            minutes = 25,
            plannedSessions = 5,
            completedSessions = 2
        )
        val tasks = listOf(
            FocusTask(
                id = 1,
                title = "Inbox cleanup",
                minutes = 25,
                completed = false,
                plannedSessions = 3,
                completedSessions = 1
            )
        )

        assertEquals(active, continueTask(active, tasks))
        assertEquals(tasks.first(), continueTask(null, tasks))
    }

    @Test
    fun continueTaskReturnsNullWhenOnlyCompletedTasksExist() {
        val tasks = listOf(
            FocusTask(id = 1, title = "Done", minutes = 25, completed = true)
        )

        assertNull(continueTask(null, tasks))
    }

    @Test
    fun pomodoroProgressLabelUsesCurrentFocusPositionDuringCycle() {
        val settings = TimerSettings(focusSessionsPerCycle = 5)

        assertEquals(
            PomodoroProgress(2, 5, "Pomodoro 2 of 5", "2 / 5 Pomodoros completed"),
            pomodoroProgress(
                completedFocusSessions = 1,
                totalFocusSessions = 5,
                isStarted = true,
                task = null,
                settings = settings
            )
        )
        assertEquals(
            PomodoroProgress(2, 5, "Pomodoro 2 of 5", "2 / 5 Pomodoros completed"),
            pomodoroProgress(
                completedFocusSessions = 2,
                totalFocusSessions = 5,
                isStarted = false,
                task = FocusTask(
                    id = 7,
                    title = "Compose Migration",
                    minutes = 25,
                    plannedSessions = 5,
                    completedSessions = 2
                ),
                settings = settings
            )
        )
    }

    private fun record(
        endedAt: Long,
        focusMinutes: Int,
        completedFocusCount: Int,
        status: SessionStatus
    ): SessionRecord = SessionRecord(
        id = endedAt,
        startedAt = endedAt - 1_000L,
        endedAt = endedAt,
        focusMinutes = focusMinutes,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        completedFocusCount = completedFocusCount,
        totalFocusCount = 4,
        status = status,
        taskName = null
    )

    private fun millisFor(date: LocalDate, hour: Int, minute: Int, zone: ZoneId): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

Run from `kuro-pomodoro-android/`:

```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.kuropomodoro.ui.HomeTimerModelsTest"
```

Expected: FAIL because `HomeGreeting`, `homeGreetingFor`, `todayFocusMetrics`, `continueTask`, `PomodoroProgress`, and `pomodoroProgress` do not exist yet.

- [ ] **Step 3: Add the display model helpers**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerModels.kt`:

```kotlin
package com.endiar.kuropomodoro.ui

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class HomeGreeting(
    val salutation: String,
    val encouragement: String
)

data class TodayFocusMetrics(
    val focusTimeLabel: String,
    val sessionsLabel: String,
    val completedLabel: String
)

data class PomodoroProgress(
    val current: Int,
    val total: Int,
    val activeLabel: String,
    val completedLabel: String
)

fun homeGreetingFor(time: LocalTime): HomeGreeting {
    val hour = time.hour
    return when {
        hour < 12 -> HomeGreeting(
            salutation = "Good morning,",
            encouragement = "Let's make this morning meaningful."
        )
        hour < 17 -> HomeGreeting(
            salutation = "Good afternoon,",
            encouragement = "Keep the afternoon moving gently."
        )
        else -> HomeGreeting(
            salutation = "Good evening,",
            encouragement = "Let's make this evening meaningful."
        )
    }
}

fun todayFocusMetrics(
    sessionRecords: List<SessionRecord>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): TodayFocusMetrics {
    val todayRecords = sessionRecords.filter { record ->
        record.endedAt > 0L &&
            Instant.ofEpochMilli(record.endedAt).atZone(zone).toLocalDate() == today
    }
    val completedFocusSegments = todayRecords.sumOf { it.completedFocusCount.coerceAtLeast(0) }
    val focusMinutes = todayRecords.sumOf { record ->
        record.completedFocusCount.coerceAtLeast(0) * record.focusMinutes.coerceAtLeast(0)
    }
    val completedCycles = todayRecords.count { it.status == SessionStatus.Completed }
    return TodayFocusMetrics(
        focusTimeLabel = formatFocusMinutes(focusMinutes),
        sessionsLabel = completedFocusSegments.toString(),
        completedLabel = completedCycles.toString()
    )
}

fun continueTask(activeTask: FocusTask?, tasks: List<FocusTask>): FocusTask? {
    return activeTask ?: tasks.firstOrNull { !it.completed && it.completedSessions < it.plannedSessions }
        ?: tasks.firstOrNull { !it.completed }
}

fun pomodoroProgress(
    completedFocusSessions: Int,
    totalFocusSessions: Int,
    isStarted: Boolean,
    task: FocusTask?,
    settings: TimerSettings
): PomodoroProgress {
    val total = when {
        task != null -> task.plannedSessions.coerceAtLeast(1)
        totalFocusSessions > 0 -> totalFocusSessions
        else -> settings.focusSessionsPerCycle.coerceAtLeast(1)
    }
    val current = if (isStarted) {
        (completedFocusSessions + 1).coerceIn(1, total)
    } else {
        ((task?.completedSessions ?: completedFocusSessions) + 0).coerceIn(0, total)
    }
    val displayCurrent = if (isStarted) current else current.coerceAtLeast(0)
    return PomodoroProgress(
        current = displayCurrent,
        total = total,
        activeLabel = "Pomodoro ${displayCurrent.coerceAtLeast(1)} of $total",
        completedLabel = "$displayCurrent / $total Pomodoros completed"
    )
}

private fun formatFocusMinutes(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
```

- [ ] **Step 4: Run the focused tests to confirm they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.kuropomodoro.ui.HomeTimerModelsTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerModels.kt \
        app/src/test/java/com/endiar/kuropomodoro/ui/HomeTimerModelsTest.kt
git commit -m "feat: add home timer display helpers"
```

## Task 2: Create the Home/Timer Compose screen

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerScreen.kt`

- [ ] **Step 1: Create the new screen file**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerScreen.kt`:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.cycleDotsState
import com.endiar.kuropomodoro.model.formatSeconds
import java.time.LocalTime

private val HomeBackground = Color(0xFFFBF6EF)
private val HomeCard = Color(0xFFFFFBF6)
private val HomeCardSoft = Color(0xFFFDF4EA)
private val HomeBorder = Color(0xFFEFE4D8)
private val HomeText = Color(0xFF16181C)
private val HomeMuted = Color(0xFF66707A)
private val HomeCoral = Color(0xFFD76343)
private val HomeCoralDark = Color(0xFFC85436)

@Composable
fun HomeTimerScreen(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    activeTask: FocusTask?,
    continueTask: FocusTask?,
    greeting: HomeGreeting,
    todayMetrics: TodayFocusMetrics,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    pomodoroProgress: PomodoroProgress,
    isSoundPlaying: Boolean,
    backdropTime: LocalTime,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onOpenSoundsSheet: () -> Unit,
    onEnterFocusMode: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    val currentItemType = cycleItems.getOrNull(cycleIndex)?.type
    val isOnBreak = currentItemType is SessionItemType.ShortBreak || currentItemType is SessionItemType.LongBreak
    val taskForTimer = activeTask ?: continueTask

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeHeader(greeting = greeting)

            if (isStarted) {
                ActiveHeroSection(
                    timerMode = timerMode,
                    remainingSeconds = remainingSeconds,
                    isRunning = isRunning,
                    isOnBreak = isOnBreak,
                    task = taskForTimer,
                    cycleItems = cycleItems,
                    cycleIndex = cycleIndex,
                    currentTimerState = currentTimerState,
                    timerSettings = timerSettings,
                    pomodoroProgress = pomodoroProgress,
                    backdropTime = backdropTime,
                    onPrimaryClick = onStartPause,
                    onStopClick = {
                        if (currentItemType is SessionItemType.Focus) {
                            showStopConfirmDialog = true
                        } else {
                            onStop()
                        }
                    },
                    onSkip = onSkip,
                    onEnterFocusMode = onEnterFocusMode
                )
            } else {
                IdleHeroSection(
                    backdropTime = backdropTime,
                    onStart = onStartPause
                )
            }

            ContinueTaskCard(task = continueTask, progress = pomodoroProgress)
            TodayFocusCard(metrics = todayMetrics)
            QuoteCard()
            SoundChip(isPlaying = isSoundPlaying, onClick = onOpenSoundsSheet)
            Spacer(Modifier.height(8.dp))
        }

        if (showStopConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showStopConfirmDialog = false },
                title = {
                    Text(
                        text = "Taking a break?",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "Your focus time so far will still be counted.",
                        color = Color.White.copy(alpha = 0.75f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showStopConfirmDialog = false
                        onStop()
                    }) {
                        Text(
                            text = "Stop for now",
                            color = Color(0xFFFF8B6F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopConfirmDialog = false }) {
                        Text(
                            text = "Stay focused",
                            color = Color.White.copy(alpha = 0.70f)
                        )
                    }
                },
                containerColor = Color(0xFF2A2035),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader(greeting: HomeGreeting) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = greeting.salutation,
                color = HomeMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nandra",
                    color = HomeText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "  \uD83C\uDF3F",
                    color = Color(0xFF7F9A54),
                    fontSize = 21.sp
                )
            }
            Text(
                text = greeting.encouragement,
                color = HomeMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Box(modifier = Modifier.size(64.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .align(Alignment.Center)
                    .shadow(8.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEFE3))
                    .border(1.dp, Color(0xFFEACCB9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MiniCatFace()
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 8.dp)
                    .clip(CircleShape)
                    .background(HomeCoral)
                    .border(2.dp, HomeBackground, CircleShape)
            )
        }
    }
}

@Composable
private fun IdleHeroSection(
    backdropTime: LocalTime,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        CatHeroImage(backdropTime = backdropTime, modifier = Modifier.fillMaxWidth())
        CoralButton(
            label = "Start Focus",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp)
                .offset(y = 26.dp),
            onClick = onStart
        )
    }
    Spacer(Modifier.height(26.dp))
}

@Composable
private fun ActiveHeroSection(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isOnBreak: Boolean,
    task: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    pomodoroProgress: PomodoroProgress,
    backdropTime: LocalTime,
    onPrimaryClick: () -> Unit,
    onStopClick: () -> Unit,
    onSkip: () -> Unit,
    onEnterFocusMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        CatHeroImage(backdropTime = backdropTime, modifier = Modifier.fillMaxWidth())
        TimerOverlayCard(
            timerMode = timerMode,
            remainingSeconds = remainingSeconds,
            isRunning = isRunning,
            isOnBreak = isOnBreak,
            task = task,
            cycleItems = cycleItems,
            cycleIndex = cycleIndex,
            currentTimerState = currentTimerState,
            timerSettings = timerSettings,
            pomodoroProgress = pomodoroProgress,
            onPrimaryClick = onPrimaryClick,
            onStopClick = onStopClick,
            onSkip = onSkip,
            onEnterFocusMode = onEnterFocusMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp)
                .offset(y = 156.dp)
        )
    }
    Spacer(Modifier.height(156.dp))
}

@Composable
private fun CatHeroImage(
    backdropTime: LocalTime,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.cat_home_header_day),
        contentDescription = null,
        modifier = modifier
            .aspectRatio(1.48f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFEEDCC9)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TimerOverlayCard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isOnBreak: Boolean,
    task: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    pomodoroProgress: PomodoroProgress,
    onPrimaryClick: () -> Unit,
    onStopClick: () -> Unit,
    onSkip: () -> Unit,
    onEnterFocusMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, timerSettings)
    SoftCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CodeTile()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = task?.title ?: timerMode.label,
                        color = HomeText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Deep Work",
                        color = HomeMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "\u270E Edit",
                color = HomeCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatSeconds(remainingSeconds),
                    color = HomeText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.sp
                )
                Text(
                    text = "remaining",
                    color = HomeMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 30.dp)
                )
            }
            PomodoroRing(progress = pomodoroProgress)
        }
        Spacer(Modifier.height(18.dp))
        HomeCycleDots(
            total = dots.totalFocusSessions,
            completed = dots.completedFocusSessions,
            active = dots.isCurrentlyFocus
        )
        Spacer(Modifier.height(22.dp))
        CoralButton(
            label = if (isRunning) "Pause Focus" else "Resume Focus",
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onStopClick) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = HomeMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Stop", color = HomeMuted, fontSize = 12.sp)
            }
            if (isOnBreak) {
                TextButton(onClick = onSkip) {
                    Text("Skip break", color = HomeCoral, fontSize = 12.sp)
                }
            }
            if (isRunning) {
                TextButton(onClick = onEnterFocusMode) {
                    Text("Focus mode", color = HomeCoral, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ContinueTaskCard(
    task: FocusTask?,
    progress: PomodoroProgress
) {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\uD83D\uDCC5",
                color = HomeCoral,
                fontSize = 15.sp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Continue where you left off",
                color = HomeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.62f))
                .border(1.dp, HomeBorder.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodeTile()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task?.title ?: "Choose a focus task",
                    color = HomeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (task == null) "Start with one gentle Pomodoro" else progress.completedLabel,
                    color = HomeMuted,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = HomeCoral
            )
        }
    }
}

@Composable
private fun TodayFocusCard(metrics: TodayFocusMetrics) {
    SoftCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Focus",
                color = HomeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "View all",
                color = HomeCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricColumn("Focus time", metrics.focusTimeLabel, Modifier.weight(1f))
            DividerLine()
            MetricColumn("Sessions", metrics.sessionsLabel, Modifier.weight(1f))
            DividerLine()
            MetricColumn("Completed", metrics.completedLabel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = HomeMuted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, color = HomeText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(42.dp)
            .background(HomeBorder)
    )
}

@Composable
private fun QuoteCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(HomeCardSoft)
            .border(1.dp, HomeBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Little by little,\na lot becomes a lot. \u2661",
            color = HomeText,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        )
        SleepingCat()
    }
}

@Composable
private fun SoftCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(HomeCard.copy(alpha = 0.96f))
            .border(1.dp, HomeBorder.copy(alpha = 0.78f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun CoralButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .shadow(10.dp, RoundedCornerShape(29.dp), clip = false)
            .clip(RoundedCornerShape(29.dp))
            .background(HomeCoral)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CodeTile() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFEEE7)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            tint = HomeCoral,
            modifier = Modifier.size(27.dp)
        )
    }
}

@Composable
private fun PomodoroRing(progress: PomodoroProgress) {
    Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color(0xFFE7DED6),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = HomeCoral,
                startAngle = -90f,
                sweepAngle = 360f * (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f),
                useCenter = false,
                style = stroke
            )
        }
        Text(
            text = "Pomodoro\n${progress.current.coerceAtLeast(1)} of ${progress.total}",
            color = HomeText,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun HomeCycleDots(
    total: Int,
    completed: Int,
    active: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(total.coerceAtLeast(1)) { index ->
            val isCompleted = index < completed
            val isActive = active && index == completed
            Box(
                modifier = Modifier
                    .size(if (isActive) 17.dp else 15.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted || isActive -> HomeCoral
                            else -> Color(0xFFE6DED6)
                        }
                    )
            )
        }
    }
}

@Composable
private fun SoundChip(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.48f))
            .border(1.dp, HomeBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPlaying) "\u266B Ambient sound playing" else "\u266B Open focus sounds",
            color = if (isPlaying) HomeCoralDark else HomeMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniCatFace() {
    Canvas(modifier = Modifier.size(44.dp)) {
        drawCircle(color = Color(0xFFEFD1B5), radius = 19.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
        drawCircle(color = Color(0xFF6B5548), radius = 5.dp.toPx(), center = Offset(size.width / 2 - 7.dp.toPx(), size.height / 2 - 3.dp.toPx()))
        drawCircle(color = Color(0xFF6B5548), radius = 5.dp.toPx(), center = Offset(size.width / 2 + 7.dp.toPx(), size.height / 2 - 3.dp.toPx()))
        drawCircle(color = Color(0xFF2B2524), radius = 2.dp.toPx(), center = Offset(size.width / 2 - 7.dp.toPx(), size.height / 2 - 4.dp.toPx()))
        drawCircle(color = Color(0xFF2B2524), radius = 2.dp.toPx(), center = Offset(size.width / 2 + 7.dp.toPx(), size.height / 2 - 4.dp.toPx()))
        drawCircle(color = Color(0xFFD87964), radius = 2.2.dp.toPx(), center = Offset(size.width / 2, size.height / 2 + 3.dp.toPx()))
    }
}

@Composable
private fun SleepingCat() {
    Canvas(modifier = Modifier.size(width = 126.dp, height = 58.dp)) {
        val body = Color(0xFF7B6254)
        val cream = Color(0xFFEAD6C5)
        drawOval(color = Color(0xFFEAD8C8), topLeft = Offset(18.dp.toPx(), 34.dp.toPx()), size = androidx.compose.ui.geometry.Size(84.dp.toPx(), 15.dp.toPx()))
        drawOval(color = body, topLeft = Offset(42.dp.toPx(), 12.dp.toPx()), size = androidx.compose.ui.geometry.Size(58.dp.toPx(), 34.dp.toPx()))
        drawCircle(color = body, radius = 18.dp.toPx(), center = Offset(42.dp.toPx(), 29.dp.toPx()))
        drawCircle(color = cream, radius = 9.dp.toPx(), center = Offset(39.dp.toPx(), 33.dp.toPx()))
        drawCircle(color = Color(0xFF2B2524), radius = 1.5.dp.toPx(), center = Offset(36.dp.toPx(), 29.dp.toPx()))
        drawCircle(color = Color(0xFF2B2524), radius = 1.5.dp.toPx(), center = Offset(45.dp.toPx(), 29.dp.toPx()))
        drawCircle(color = HomeCoral.copy(alpha = 0.65f), radius = 2.dp.toPx(), center = Offset(41.dp.toPx(), 35.dp.toPx()))
        drawCircle(color = Color(0xFF9BA660), radius = 4.dp.toPx(), center = Offset(17.dp.toPx(), 42.dp.toPx()))
        drawCircle(color = Color(0xFF9BA660), radius = 3.dp.toPx(), center = Offset(109.dp.toPx(), 42.dp.toPx()))
        drawCircle(color = Color(0xFFE8A99B), radius = 3.dp.toPx(), center = Offset(116.dp.toPx(), 34.dp.toPx()))
    }
}
```

- [ ] **Step 2: Compile the new file to catch missing imports**

Run from `kuro-pomodoro-android/`:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: PASS. If it fails for missing imports or type errors in `HomeTimerScreen.kt`, fix only those issues in this task and rerun until `HomeTimerScreen.kt` compiles.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerScreen.kt
git commit -m "feat: add home timer screen UI"
```

## Task 3: Wire HomeTimerScreen into KuroPomodoroApp

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add imports**

In `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`, add these imports near the other `com.endiar.kuropomodoro.ui` imports:

```kotlin
import com.endiar.kuropomodoro.ui.HomeTimerScreen
import com.endiar.kuropomodoro.ui.continueTask
import com.endiar.kuropomodoro.ui.homeGreetingFor
import com.endiar.kuropomodoro.ui.pomodoroProgress
import com.endiar.kuropomodoro.ui.todayFocusMetrics
```

- [ ] **Step 2: Derive Home display values**

Inside `KuroPomodoroApp`, after `val currentTimerState = runtimeState?.session?.timerState ?: TimerState.Idle`, add:

```kotlin
    val homeGreeting = homeGreetingFor(backdropTime)
    val homeContinueTask = continueTask(activeTask, tasks)
    val homeMetrics = todayFocusMetrics(
        sessionRecords = sessionRecords,
        today = LocalDate.now()
    )
    val homeDots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, settings)
    val homePomodoroProgress = pomodoroProgress(
        completedFocusSessions = homeDots.completedFocusSessions,
        totalFocusSessions = homeDots.totalFocusSessions,
        isStarted = isStarted,
        task = activeTask ?: homeContinueTask,
        settings = settings
    )
```

If `backdropTime` is currently declared after this block, move `val backdropTime = AppClock.now()` above these derived values so `homeGreetingFor(backdropTime)` can compile.

- [ ] **Step 3: Replace the Timer branch call**

In the `when (selectedTab)` block, replace the current `AppTab.Timer -> FocusDashboard` call with:

```kotlin
                    AppTab.Timer -> HomeTimerScreen(
                        timerMode = timerMode,
                        remainingSeconds = remainingSeconds,
                        isRunning = isRunning,
                        isStarted = isStarted,
                        activeTask = activeTask,
                        continueTask = homeContinueTask,
                        greeting = homeGreeting,
                        todayMetrics = homeMetrics,
                        cycleItems = cycleItems,
                        cycleIndex = cycleIndex,
                        currentTimerState = currentTimerState,
                        timerSettings = settings,
                        pomodoroProgress = homePomodoroProgress,
                        isSoundPlaying = isSoundPlaying,
                        backdropTime = backdropTime,
                        onSkip = { timerConnection.controller.skip() },
                        onStartPause = {
                            when {
                                !isStarted -> {
                                    requestNotificationPermission()
                                    isFocusMode = true
                                    timerConnection.controller.start(settings, activeTask)
                                }

                                isRunning -> {
                                    isFocusMode = false
                                    timerConnection.controller.pause()
                                }

                                else -> {
                                    timerConnection.controller.resume()
                                }
                            }
                        },
                        onStop = {
                            timerConnection.controller.stop()
                            isFocusMode = false
                            activeTask = null
                        },
                        onOpenSoundsSheet = { showSoundsSheet = true },
                        onEnterFocusMode = { isFocusMode = true },
                        contentPadding = screenContentPadding
                    )
```

- [ ] **Step 4: Remove unused imports and dead FocusDashboard code**

After replacing the Timer branch, remove the old private `FocusDashboard` composable and the old private `MusicButton` composable from `KuroPomodoroApp.kt`.

Remove imports from `KuroPomodoroApp.kt` that become unused because those old composables are gone. The expected removable imports include:

```kotlin
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.formatSeconds
import com.endiar.kuropomodoro.ui.components.CycleDotsRow
```

Keep any import from that list if the compiler still needs it elsewhere in the file.

- [ ] **Step 5: Compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: show revamped home timer screen"
```

## Task 4: Validate behavior and polish obvious layout issues

**Files:**
- Modify only files changed in Tasks 1-3 if verification reveals compile, test, or layout issues.

- [ ] **Step 1: Run focused model tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.kuropomodoro.ui.HomeTimerModelsTest"
```

Expected: PASS.

- [ ] **Step 2: Run the broader JVM test suite**

```bash
./gradlew test
```

Expected: PASS.

- [ ] **Step 3: Build debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Manual UI smoke check**

Install/run the debug build in Android Studio or on a local emulator and verify:

- Idle Home shows the greeting, cat hero, `Start Focus`, continue card, metrics card, quote card, sound chip, and existing dock.
- Tapping `Start Focus` starts the timer and shows the overlapping active timer card.
- Playing card shows a decreasing countdown and `Pause Focus`.
- Tapping `Pause Focus` pauses and changes the primary CTA to `Resume Focus`.
- Tapping `Resume Focus` resumes the countdown.
- Tapping `Stop` during a focus segment shows the stop confirmation dialog.
- Break segments expose `Skip break`.
- Sound chip opens the existing sounds bottom sheet.
- Completion dialog still appears at cycle completion.
- Tasks, Habits, Calendar/Records, and Profile tabs still open unchanged.

- [ ] **Step 5: Commit any verification fixes**

If verification required fixes, commit only the touched files:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerModels.kt \
        app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerScreen.kt \
        app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt \
        app/src/test/java/com/endiar/kuropomodoro/ui/HomeTimerModelsTest.kt
git commit -m "fix: polish home timer revamp"
```

If no fixes were needed, do not create an empty commit.

## Task 5: Final review

**Files:**
- Read-only unless issues are found.

- [ ] **Step 1: Check git status**

```bash
git status --short
```

Expected: only unrelated pre-existing `.idea` changes may remain.

- [ ] **Step 2: Review the implementation diff**

```bash
git diff HEAD~3..HEAD -- app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerModels.kt \
                         app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerScreen.kt \
                         app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt \
                         app/src/test/java/com/endiar/kuropomodoro/ui/HomeTimerModelsTest.kt
```

Expected: diff is limited to Home/Timer presentation, helper tests, and wiring.

- [ ] **Step 3: Confirm acceptance criteria**

Confirm each item before handoff:

- Home/Timer is warm, cream, card-based, and structurally close to the reference.
- Idle Home includes header, hero, start CTA, continue task card, today metrics, and quote card.
- Active Home includes overlapping timer card, remaining time, Pomodoro progress, cycle dots, and primary CTA.
- Existing timer lifecycle behavior is unchanged.
- No DataStore schema changes were introduced.
- Secondary tabs remain unchanged.
- Tests/builds either pass or any environment blocker is documented.
