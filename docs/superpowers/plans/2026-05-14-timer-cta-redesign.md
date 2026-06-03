# Timer CTA Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the manual mode selector from the idle timer screen and replace the small play button with a prominent full-width "Focus Now" pill CTA.

**Architecture:** All changes are confined to `PurrodoroApp.kt`. The `FocusDashboard` composable's idle branch loses the mode dropdown and small circle play button; both are replaced by a single pill `Box`. The `onModeSelected` callback is removed from `FocusDashboard`'s signature and its call site in `PurrodoroApp`.

**Tech Stack:** Kotlin, Jetpack Compose

---

### Task 1: Remove mode selector and replace idle CTA in `FocusDashboard`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

- [ ] **Step 1: Remove `onModeSelected` from `FocusDashboard` signature**

Find the `FocusDashboard` composable signature (around line 502) and remove the `onModeSelected` parameter:

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
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
) {
```

- [ ] **Step 2: Remove `showModeMenu` state and `timerMode` parameter usage in idle branch**

Inside `FocusDashboard`, remove this line:

```kotlin
var showModeMenu by remember { mutableStateOf(false) }
```

- [ ] **Step 3: Replace the idle button row with the "Focus Now" pill**

Find the idle `else` branch (the block starting with `// Idle state: play button + mode selector`). Replace the entire content of that `else` block with the pill button:

```kotlin
} else {
    // Idle state: Focus Now pill CTA
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFFF8B6F))
            .clickable(onClick = onStartPause),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Focus Now",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
```

- [ ] **Step 4: Remove unused `DropdownMenu`/`DropdownMenuItem` imports if they become unused**

Check the import block at the top of the file. If `DropdownMenu` and `DropdownMenuItem` are no longer referenced anywhere, remove these two lines:

```kotlin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
```

Also remove `MenuDefaults` if unused:
```kotlin
import androidx.compose.material3.MenuDefaults
```

- [ ] **Step 5: Remove `onModeSelected` from the `FocusDashboard` call site in `PurrodoroApp`**

Find the `FocusDashboard(...)` call (inside `AppTab.Timer ->`) and remove the `onModeSelected` argument:

```kotlin
AppTab.Timer -> FocusDashboard(
    timerMode = timerMode,
    remainingSeconds = remainingSeconds,
    isRunning = isRunning,
    isStarted = isStarted,
    isFocusMode = isFocusMode,
    activeTask = activeTask,
    cycleItems = cycleItems,
    cycleIndex = cycleIndex,
    onSkip = onSkip,
    onStartPause = {
        if (!isRunning) {
            activeTotalSeconds = timerStartTotalSeconds(
                settings = settings,
                mode = timerMode,
                remainingSeconds = remainingSeconds,
                activeTotalSeconds = activeTotalSeconds
            )
            isFocusMode = true
            isStarted = true
            if (openRecord == null) {
                val totalFocus = cycleItems.count { it.type is SessionItemType.Focus }
                openRecord = SessionRecord(
                    id = System.currentTimeMillis(),
                    startedAt = System.currentTimeMillis(),
                    endedAt = 0L,
                    focusMinutes = settings.focusMinutes,
                    shortBreakMinutes = settings.shortBreakMinutes,
                    longBreakMinutes = settings.longBreakMinutes,
                    completedFocusCount = 0,
                    totalFocusCount = totalFocus,
                    status = SessionStatus.Partial,
                    taskName = activeTask?.title
                )
            }
        } else {
            isFocusMode = false
        }
        isRunning = !isRunning
    },
    onStop = {
        openRecord?.let { rec ->
            persistSessionRecord(
                rec.copy(
                    endedAt = System.currentTimeMillis(),
                    status = SessionStatus.Partial,
                    taskName = activeTask?.title
                )
            )
            openRecord = null
        }
        isRunning = false
        isStarted = false
        isFocusMode = false
        activeTask = null
        remainingSeconds = settings.secondsFor(timerMode)
        activeTotalSeconds = settings.secondsFor(timerMode)
        timerStartedAtMillis = null
        timerStartedRemainingSeconds = settings.secondsFor(timerMode)
    },
    onExitFocusMode = { isFocusMode = false },
    onEnterFocusMode = { isFocusMode = true }
)
```

- [ ] **Step 6: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: replace idle mode selector with Focus Now pill CTA"
```

---

## Self-Review Checklist

- **Spec coverage:**
  - Remove mode selector dropdown ✓ (Step 2 + 3)
  - Remove `onModeSelected` callback ✓ (Step 1 + 5)
  - Pill CTA "Focus Now", `#FF8B6F`, `56dp`, pill shape, `16sp Bold` ✓ (Step 3)
  - Idle layout: cycle dots → timer → pill ✓ (existing layout unchanged except button row)
  - Running/paused states unchanged ✓ (only `else` idle branch touched)
- **No placeholders:** All steps have complete code
- **Type consistency:** `onStartPause`, `RoundedCornerShape`, `Color(0xFFFF8B6F)` all consistent with existing usage
