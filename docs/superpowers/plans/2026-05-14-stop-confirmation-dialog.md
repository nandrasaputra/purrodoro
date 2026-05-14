# Stop Confirmation Dialog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a confirmation dialog when the user taps Stop on a paused Focus session, preventing accidental abandonment.

**Architecture:** Add a single boolean `showStopConfirmDialog` state variable local to `FocusDashboard`. Gate the stop button's `onClick` to set this flag instead of calling `onStop` directly. Render an `AlertDialog` conditionally. Break-session stops are unchanged (no dialog).

**Tech Stack:** Kotlin, Jetpack Compose, Material3 `AlertDialog` — no new dependencies.

---

## File Map

| File | Change |
|---|---|
| `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` | Add `showStopConfirmDialog` state; gate stop button; add `AlertDialog` composable in `FocusDashboard` |

---

## Task 1: Add stop confirmation dialog to `FocusDashboard`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add `showStopConfirmDialog` state**

In `FocusDashboard`, after the existing local state declarations (the line `var showModeMenu by remember { mutableStateOf(false) }`), add:

```kotlin
var showStopConfirmDialog by remember { mutableStateOf(false) }
```

- [ ] **Step 2: Gate the stop button to show the dialog instead of calling `onStop` directly**

Find the stop button in the `isStarted` (paused) branch — it is the `Box` with `.clickable(onClick = onStop)` at around line 711. Replace `.clickable(onClick = onStop)` with:

```kotlin
.clickable {
    val currentItemType = cycleItems.getOrNull(cycleIndex)?.type
    val isOnFocus = currentItemType is SessionItemType.Focus
    if (isOnFocus) {
        showStopConfirmDialog = true
    } else {
        onStop()
    }
}
```

- [ ] **Step 3: Add the `AlertDialog` below the controls `Row`**

After the closing brace of the controls `Row(...)` block (before `Spacer(Modifier.weight(1f))`), add:

```kotlin
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
```

Add the `AlertDialog` import at the top of the file if not already present:

```kotlin
import androidx.compose.material3.AlertDialog
```

- [ ] **Step 4: Verify compile**

```bash
cd kuro-pomodoro-android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` (no new errors).

- [ ] **Step 5: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: add stop confirmation dialog for paused focus session"
```

---

## Task 2: Full build verification

- [ ] **Step 1: Run full debug build**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` with APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install on device/emulator**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Manual verification checklist**

  - Start a Focus session → pause it → tap Stop → confirm dialog appears
  - Tap "Stay focused" → dialog dismisses, timer stays paused
  - Tap outside dialog (scrim) → dialog dismisses, timer stays paused
  - Tap "Stop for now" → timer resets to idle, focus mode exits
  - Start a ShortBreak or LongBreak session → pause → tap Stop → no dialog, stops immediately
