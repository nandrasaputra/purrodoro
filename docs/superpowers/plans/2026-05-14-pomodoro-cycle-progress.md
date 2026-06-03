# Pomodoro Cycle Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable Pomodoro cycle (N focus sessions before long break) with dot progress display above the timer and a skip button for breaks.

**Architecture:** Extend `TimerSettings` with `focusSessionsPerCycle`, add `buildCycle()` to construct the full session item list, wire cycle reset on completion in `PurrodoroApp`, add `CycleDotsRow` composable above the timer digits, and show a Skip button during breaks.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX DataStore, no new dependencies.

---

## File Map

| File | Change |
|---|---|
| `model/KuroModels.kt` | Add `focusSessionsPerCycle` to `TimerSettings`, add `buildCycle()`, add `buildSession()` |
| `data/AppRepository.kt` | Persist `focusSessionsPerCycle` via DataStore |
| `PurrodoroApp.kt` | Replace ad-hoc timer completion logic with cycle-aware logic using `buildCycle`; add skip handler; thread `cycleItems`/`cycleIndex` state; add `onSkip` to `FocusDashboard` |
| `ui/ProfileScreen.kt` | Add `DurationRow`-style stepper for `focusSessionsPerCycle` |
| `PurrodoroApp.kt` (FocusDashboard) | Pass `cycleItems`, `cycleIndex`, `isBreak` to new `CycleDotsRow`; show Skip button when on a break |
| `ui/components/KuroComponents.kt` | Add `CycleDotsRow` composable |

---

## Task 1: Add `focusSessionsPerCycle` to `TimerSettings` and add `buildCycle()`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt`

- [ ] **Step 1: Add `focusSessionsPerCycle` field to `TimerSettings`**

Replace the existing `TimerSettings` data class (lines 69–89) with:

```kotlin
data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val autoStartNextSession: Boolean = false,
    val focusSessionsPerCycle: Int = 4
) {
    fun secondsFor(mode: TimerMode): Int = when (mode) {
        TimerMode.Focus -> focusMinutes * 60
        TimerMode.ShortBreak -> shortBreakMinutes * 60
        TimerMode.LongBreak -> longBreakMinutes * 60
    }

    fun update(mode: TimerMode, minutes: Int): TimerSettings {
        val safeMinutes = minutes.coerceIn(1, 180)
        return when (mode) {
            TimerMode.Focus -> copy(focusMinutes = safeMinutes)
            TimerMode.ShortBreak -> copy(shortBreakMinutes = safeMinutes)
            TimerMode.LongBreak -> copy(longBreakMinutes = safeMinutes)
        }
    }

    fun updateCycleSize(n: Int): TimerSettings = copy(focusSessionsPerCycle = n.coerceIn(1, 10))
}
```

- [ ] **Step 2: Add `buildCycle()` function after `TimerSettings`**

Add this function after the closing brace of `TimerSettings`:

```kotlin
/**
 * Builds the full Pomodoro cycle item list from settings.
 *
 * Pattern for N = focusSessionsPerCycle:
 *   (Focus + ShortBreak) × (N-1), then Focus + LongBreak
 *
 * Example for N=4:
 *   [Focus, ShortBreak, Focus, ShortBreak, Focus, ShortBreak, Focus, LongBreak]
 */
fun buildCycle(settings: TimerSettings): List<TimerSessionItem> {
    val items = mutableListOf<TimerSessionItem>()
    val n = settings.focusSessionsPerCycle.coerceAtLeast(1)
    repeat(n - 1) {
        items += TimerSessionItem(SessionItemType.Focus(settings.focusMinutes * 60))
        items += TimerSessionItem(SessionItemType.ShortBreak(settings.shortBreakMinutes * 60))
    }
    items += TimerSessionItem(SessionItemType.Focus(settings.focusMinutes * 60))
    items += TimerSessionItem(SessionItemType.LongBreak(settings.longBreakMinutes * 60))
    return items
}
```

- [ ] **Step 3: Verify the file compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` (or only pre-existing warnings, no new errors).

- [ ] **Step 4: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt
git commit -m "feat: add focusSessionsPerCycle to TimerSettings and buildCycle()"
```

---

## Task 2: Persist `focusSessionsPerCycle` in `AppRepository`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt`

- [ ] **Step 1: Add DataStore key**

In the `companion object`, after `KEY_LONG_MIN`, add:

```kotlin
private val KEY_CYCLE_SIZE = intPreferencesKey("focus_sessions_per_cycle")
```

- [ ] **Step 2: Read `focusSessionsPerCycle` in `appState` flow**

In the `appState` flow mapping, replace:

```kotlin
settings = TimerSettings(
    focusMinutes = prefs[KEY_FOCUS_MIN] ?: 25,
    shortBreakMinutes = prefs[KEY_SHORT_MIN] ?: 5,
    longBreakMinutes = prefs[KEY_LONG_MIN] ?: 15
),
```

with:

```kotlin
settings = TimerSettings(
    focusMinutes = prefs[KEY_FOCUS_MIN] ?: 25,
    shortBreakMinutes = prefs[KEY_SHORT_MIN] ?: 5,
    longBreakMinutes = prefs[KEY_LONG_MIN] ?: 15,
    focusSessionsPerCycle = prefs[KEY_CYCLE_SIZE] ?: 4
),
```

- [ ] **Step 3: Persist `focusSessionsPerCycle` in `setSettings`**

Replace the existing `setSettings` function:

```kotlin
suspend fun setSettings(value: TimerSettings) = dataStore.edit { prefs ->
    prefs[KEY_FOCUS_MIN] = value.focusMinutes
    prefs[KEY_SHORT_MIN] = value.shortBreakMinutes
    prefs[KEY_LONG_MIN] = value.longBreakMinutes
    prefs[KEY_CYCLE_SIZE] = value.focusSessionsPerCycle
}
```

- [ ] **Step 4: Verify the file compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt
git commit -m "feat: persist focusSessionsPerCycle in AppRepository"
```

---

## Task 3: Add cycle stepper to `ProfileScreen`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/ProfileScreen.kt`

- [ ] **Step 1: Add `onCycleSizeChanged` parameter to `ProfileScreen`**

Replace the existing `ProfileScreen` signature:

```kotlin
@Composable
fun ProfileScreen(
    settings: TimerSettings,
    darkModePreview: Boolean,
    glassReadabilityMode: GlassReadabilityMode,
    glassTreatment: GlassTreatment,
    onDarkModeChanged: (Boolean) -> Unit,
    onGlassReadabilityModeChanged: (GlassReadabilityMode) -> Unit,
    onDurationChanged: (TimerMode, Int) -> Unit,
    onCycleSizeChanged: (Int) -> Unit,
    onMockFeature: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Add cycle size stepper row inside the Timer Settings card**

After the last `DurationRow` call (for `LongBreak`) and its preceding `Spacer`, add:

```kotlin
Spacer(Modifier.height(8.dp))
DurationRow(
    label = "Focus sessions / cycle",
    minutes = settings.focusSessionsPerCycle,
    unit = "sessions",
    onMinus = { onCycleSizeChanged((settings.focusSessionsPerCycle - 1).coerceIn(1, 10)) },
    onPlus = { onCycleSizeChanged((settings.focusSessionsPerCycle + 1).coerceIn(1, 10)) },
    glassTreatment = glassTreatment
)
```

- [ ] **Step 3: Update `DurationRow` to accept a `unit` parameter**

Replace the existing `DurationRow` private composable with:

```kotlin
@Composable
private fun DurationRow(
    label: String,
    minutes: Int,
    unit: String = "min",
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepperButton(label = "−", onClick = onMinus, glassTreatment = glassTreatment)
            Text(
                text = "$minutes $unit",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 54.dp),
                textAlign = TextAlign.Center
            )
            StepperButton(label = "+", onClick = onPlus, glassTreatment = glassTreatment)
        }
    }
}
```

- [ ] **Step 4: Verify compile**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: errors only from `PurrodoroApp.kt` (missing `onCycleSizeChanged` argument) — that's fine, fixed in Task 4.

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/ProfileScreen.kt
git commit -m "feat: add cycle size stepper to ProfileScreen"
```

---

## Task 4: Add `CycleDotsRow` composable to `KuroComponents.kt`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt`

- [ ] **Step 1: Read the current KuroComponents.kt to find insertion point**

Read the file first to understand its structure and find where to add the new composable.

- [ ] **Step 2: Add imports if not present**

Ensure these imports are present at the top of the file (add any missing ones):

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
```

- [ ] **Step 3: Add `CycleDotsRow` at the end of the file**

```kotlin
/**
 * A row of dots showing focus session progress within a Pomodoro cycle.
 *
 * @param totalFocusSessions  Total focus sessions in this cycle (= focusSessionsPerCycle)
 * @param completedFocusSessions  Number of focus sessions already completed this cycle
 * @param isCurrentlyFocus  True if the timer is currently on a Focus session (highlights current dot)
 */
@Composable
fun CycleDotsRow(
    totalFocusSessions: Int,
    completedFocusSessions: Int,
    isCurrentlyFocus: Boolean,
    modifier: Modifier = Modifier
) {
    val peach = Color(0xFFFF8B6F)
    val peachMuted = Color(0xFFFF8B6F).copy(alpha = 0.25f)
    val currentDotSize = 12.dp
    val dotSize = 10.dp
    val strokeWidth = 1.5.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalFocusSessions) { index ->
            val isCompleted = index < completedFocusSessions
            val isCurrent = isCurrentlyFocus && index == completedFocusSessions
            val size = if (isCurrent) currentDotSize else dotSize

            Canvas(modifier = Modifier.size(size)) {
                val radius = this.size.minDimension / 2f
                when {
                    isCompleted -> {
                        // Filled peach dot
                        drawCircle(color = peach, radius = radius)
                    }
                    isCurrent -> {
                        // Filled peach dot (slightly larger, acts as highlight)
                        drawCircle(color = peach, radius = radius)
                    }
                    else -> {
                        // Outline dot
                        drawCircle(
                            color = peachMuted,
                            radius = radius - strokeWidth.toPx() / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Verify compile**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` (aside from the still-unresolved `onCycleSizeChanged` in PurrodoroApp from Task 3).

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt
git commit -m "feat: add CycleDotsRow composable"
```

---

## Task 5: Wire cycle state and skip into `PurrodoroApp`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

This is the largest task. Read the full file before starting.

- [ ] **Step 1: Add cycle state variables**

After the existing `var isFocusMode by rememberSaveable { ... }` line (around line 130), add:

```kotlin
// Cycle state: items list and current position
var cycleItems by rememberSaveable(
    stateSaver = Saver(
        save = { items: List<TimerSessionItem> ->
            items.map { item ->
                when (val t = item.type) {
                    is SessionItemType.Focus -> "F:${t.seconds}"
                    is SessionItemType.ShortBreak -> "S:${t.seconds}"
                    is SessionItemType.LongBreak -> "L:${t.seconds}"
                }
            }
        },
        restore = { saved: List<String> ->
            saved.map { s ->
                val (tag, sec) = s.split(":")
                val seconds = sec.toInt()
                TimerSessionItem(when (tag) {
                    "S" -> SessionItemType.ShortBreak(seconds)
                    "L" -> SessionItemType.LongBreak(seconds)
                    else -> SessionItemType.Focus(seconds)
                })
            }
        }
    )
) { mutableStateOf(buildCycle(settings)) }
var cycleIndex by rememberSaveable { mutableIntStateOf(0) }
```

Also add the required imports at the top of the file (if not present):

```kotlin
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.buildCycle
```

- [ ] **Step 2: Replace the timer completion logic in `LaunchedEffect`**

Find the existing timer completion block (around lines 203–237):

```kotlin
if (updatedRemainingSeconds == 0) {
    isRunning = false
    val nextMode = if (timerMode == TimerMode.Focus) {
        ...
        TimerMode.ShortBreak
    } else {
        timerMode
    }
    timerMode = nextMode
    remainingSeconds = settings.secondsFor(nextMode)
    timerStartedAtMillis = null
    timerStartedRemainingSeconds = remainingSeconds
    break
}
```

Replace that entire `if (updatedRemainingSeconds == 0)` block with:

```kotlin
if (updatedRemainingSeconds == 0) {
    isRunning = false

    // Stamps and task tracking on Focus completion
    val currentItem = cycleItems.getOrNull(cycleIndex)
    if (currentItem?.type is SessionItemType.Focus) {
        persistCompletedSessions(completedSessions + 1)
        persistStamps(addFocusStamp(stamps, LocalDate.now()))

        val currentTask = activeTask
        if (currentTask != null) {
            val updatedTask = currentTask.copy(
                completedSessions = currentTask.completedSessions + 1
            )
            activeTask = updatedTask
            if (updatedTask.completedSessions >= updatedTask.plannedSessions) {
                persistTodoLists(todoLists.map { list ->
                    list.copy(tasks = list.tasks.map { t ->
                        if (t.id == updatedTask.id) t.copy(completed = true) else t
                    })
                })
                activeTask = null
                isFocusMode = false
                selectedTab = AppTab.Todo
            }
        }
    }

    // Advance cycle
    val nextIndex = cycleIndex + 1
    if (nextIndex >= cycleItems.size) {
        // Full cycle complete — reset
        cycleItems = buildCycle(settings)
        cycleIndex = 0
    } else {
        cycleIndex = nextIndex
    }

    val nextItem = cycleItems[cycleIndex]
    val nextSeconds = when (val t = nextItem.type) {
        is SessionItemType.Focus -> t.seconds
        is SessionItemType.ShortBreak -> t.seconds
        is SessionItemType.LongBreak -> t.seconds
    }
    remainingSeconds = nextSeconds
    activeTotalSeconds = nextSeconds
    timerMode = when (cycleItems[cycleIndex].type) {
        is SessionItemType.Focus -> TimerMode.Focus
        is SessionItemType.ShortBreak -> TimerMode.ShortBreak
        is SessionItemType.LongBreak -> TimerMode.LongBreak
    }
    timerStartedAtMillis = null
    timerStartedRemainingSeconds = remainingSeconds

    if (settings.autoStartNextSession) {
        isRunning = true
    }
    break
}
```

- [ ] **Step 3: Add `onSkip` handler**

Add this function near the other event handlers (after `fun persistTodoLists`, before the `addTask` lambda, around line 142):

```kotlin
val onSkip: () -> Unit = {
    val currentItem = cycleItems.getOrNull(cycleIndex)
    // Skip is only valid during breaks
    if (currentItem?.type is SessionItemType.ShortBreak || currentItem?.type is SessionItemType.LongBreak) {
        isRunning = false
        val nextIndex = cycleIndex + 1
        if (nextIndex >= cycleItems.size) {
            // Skipping last item (LongBreak) resets cycle
            cycleItems = buildCycle(settings)
            cycleIndex = 0
        } else {
            cycleIndex = nextIndex
        }
        val nextItem = cycleItems[cycleIndex]
        val nextSeconds = when (val t = nextItem.type) {
            is SessionItemType.Focus -> t.seconds
            is SessionItemType.ShortBreak -> t.seconds
            is SessionItemType.LongBreak -> t.seconds
        }
        remainingSeconds = nextSeconds
        activeTotalSeconds = nextSeconds
        timerMode = when (cycleItems[cycleIndex].type) {
            is SessionItemType.Focus -> TimerMode.Focus
            is SessionItemType.ShortBreak -> TimerMode.ShortBreak
            is SessionItemType.LongBreak -> TimerMode.LongBreak
        }
        timerStartedAtMillis = null
        timerStartedRemainingSeconds = remainingSeconds
    }
}
```

- [ ] **Step 4: Pass cycle state and `onSkip` to `FocusDashboard`**

Find the `FocusDashboard(...)` call (around line 256) and add these parameters:

```kotlin
AppTab.Timer -> FocusDashboard(
    timerMode = timerMode,
    remainingSeconds = remainingSeconds,
    isRunning = isRunning,
    isStarted = remainingSeconds < settings.secondsFor(timerMode),
    isFocusMode = isFocusMode,
    activeTask = activeTask,
    cycleItems = cycleItems,
    cycleIndex = cycleIndex,
    onSkip = onSkip,
    onModeSelected = { ... },  // unchanged
    onStartPause = { ... },    // unchanged
    onStop = { ... },          // unchanged
    onExitFocusMode = { isFocusMode = false },
    onEnterFocusMode = { isFocusMode = true }
)
```

(Keep all existing parameters; just add `cycleItems`, `cycleIndex`, `onSkip`.)

- [ ] **Step 5: Update `ProfileScreen` call to pass `onCycleSizeChanged`**

Find the `ProfileScreen(...)` call (around line 334) and add:

```kotlin
onCycleSizeChanged = { n ->
    val updatedSettings = settings.updateCycleSize(n)
    persistSettings(updatedSettings)
    // Rebuild cycle on next start (don't interrupt current cycle)
},
```

- [ ] **Step 6: Update `FocusDashboard` signature and body**

Find `private fun FocusDashboard(` (around line 392) and update its signature to include the new parameters:

```kotlin
@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    isFocusMode: Boolean,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    onSkip: () -> Unit,
    onModeSelected: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
)
```

- [ ] **Step 7: Add `CycleDotsRow` above the timer digits in `FocusDashboard`**

Compute dots data at the top of the composable body (after the `if (isFocusMode && isRunning) { ... return }` early-return block):

```kotlin
// Cycle dot computation
val focusItemIndices = cycleItems.mapIndexedNotNull { i, item ->
    if (item.type is SessionItemType.Focus) i else null
}
val totalFocusSessions = focusItemIndices.size
val completedFocusSessions = focusItemIndices.count { it < cycleIndex }
val isCurrentlyFocus = cycleItems.getOrNull(cycleIndex)?.type is SessionItemType.Focus
```

Then in the main `Column`, add `CycleDotsRow` **above** the timer digits text. Find the `Text(formatSeconds(...))` call (around line 488 in the original) and insert before it:

```kotlin
Spacer(Modifier.height(8.dp))
CycleDotsRow(
    totalFocusSessions = totalFocusSessions,
    completedFocusSessions = completedFocusSessions,
    isCurrentlyFocus = isCurrentlyFocus,
    modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center)
)
Spacer(Modifier.height(4.dp))
```

Add the import at the top of the file:
```kotlin
import com.endiar.purrodoro.ui.components.CycleDotsRow
```

- [ ] **Step 8: Add Skip button in `FocusDashboard` break state**

The current item type determines whether skip is shown. In the controls `Row` (the one with the pause/resume/stop buttons), add skip button logic:

Find the `if (isRunning)` / `else if (isStarted)` / `else` block in the controls Row. In **both** the `isRunning` and `isStarted` branches, after the stop/pause button, add a skip button that is only visible when on a break:

```kotlin
val currentItemType = cycleItems.getOrNull(cycleIndex)?.type
val isOnBreak = currentItemType is SessionItemType.ShortBreak ||
                currentItemType is SessionItemType.LongBreak

// Add this after the existing buttons in both isRunning and isStarted branches:
if (isOnBreak) {
    Spacer(Modifier.width(8.dp))
    Box(
        modifier = Modifier
            .height(56.dp)
            .border(
                1.5.dp,
                Color.White.copy(alpha = 0.20f),
                RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onSkip)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Skip",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
```

- [ ] **Step 9: Verify compile**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: wire cycle state, dots, and skip into PurrodoroApp"
```

---

## Task 6: Full build verification

- [ ] **Step 1: Run full debug build**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with an APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Commit build success (if any last-minute fixes needed)**

```bash
git add -A && git commit -m "fix: resolve any remaining compile issues"
```

Only needed if Step 1 required fixes.
