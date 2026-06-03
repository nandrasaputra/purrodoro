# Todo ↔ Timer Integration Design

**Date:** 2026-05-12  
**Status:** Approved

---

## Overview

Connect the Todo screen to the Focus timer so users can start a focused Pomodoro session directly from a task. A leading swipe on any task row reveals a "Focus with Purrodoro" action. The user sets how many sessions they want, then the app transitions into a fullscreen Focus Mode. When all planned sessions complete, the task is auto-marked done.

---

## 1. Data Model Changes

`FocusTask` in `KuroModels.kt` gains two new fields:

```kotlin
data class FocusTask(
    val id: String,
    val title: String,
    val minutes: Int,
    val completed: Boolean,
    val plannedSessions: Int = 1,   // set in pre-session sheet
    val pomodorosDone: Int = 0      // incremented per completed focus session
)
```

Both fields are primitive `Int` — compatible with `rememberSaveable`. No persistence layer needed; behavior is consistent with the rest of the app (survives config changes, resets on app kill).

---

## 2. State in KuroPomodoroApp

Two new state variables:

```kotlin
var activeTask by rememberSaveable { mutableStateOf<FocusTask?>(null) }
var isFocusMode by rememberSaveable { mutableStateOf(false) }
```

- `activeTask`: set when "Start Focus" fires; cleared when all planned sessions complete or user ends session. `plannedSessions` is stored on the task itself (copied before assignment), so no separate state variable is needed for it.
- `isFocusMode`: `true` = show fullscreen Focus Mode screen (`VKt6u` design); `false` = show normal timer UI. Set `true` on Start Focus, `false` on tap-anywhere or session end.

Tab switching uses the existing `selectedTab` state — Start Focus sets `selectedTab` to the Timer tab index.

---

## 3. TodoScreen — Leading Swipe Action

`TaskGlassRow` in `TodoScreen.kt` gains a swipe-to-act behavior using Jetpack Compose `SwipeToDismiss` (Material 3):

- **Leading swipe (startToEnd, right):** Large orange slab, "▶ Focus with Purrodoro" label. Fires at ~40% row width threshold; row snaps back after action.
- **Trailing swipe (endToStart, left):** Red slab, ✕ icon, deletes task (existing behavior to be wired up).

The leading swipe action calls `onFocusTask(task)`, which opens the pre-session bottom sheet.

---

## 4. Pre-Session Bottom Sheet

A `ModalBottomSheet` opened after leading swipe fires:

| Element | Detail |
|---|---|
| Drag handle | Top center |
| Task name | Subtitle, dimmed |
| Heading | "How many sessions?" |
| Stepper | `−` / number / `+`, default 1, max 8 |
| Primary button | "▶ Start Focus", full-width, orange |

On "Start Focus":
1. Copy task with `plannedSessions = selectedCount`, update in task list
2. `activeTask = updatedTask`
3. `isFocusMode = true`
4. `selectedTab = timerTabIndex`
5. Start timer (existing `isTimerRunning = true` or equivalent)
6. Dismiss sheet

Swiping the sheet down dismisses without action.

---

## 5. Focus Mode Screen (VKt6u)

Fullscreen state: chrome stripped, massive clock, task name as caption, DND badge, "FOCUS · DND ON" mode label, "tap anywhere to show controls" hint.

Tapping anywhere sets `isFocusMode = false`, returning the user to the Timer tab (Running state).

---

## 6. Running State — Controls Redesign

When `isFocusMode = false` and timer is running, the Timer tab (Running state) shows:

- **Header row:** mode label + task name + sessions pill (e.g. "3 / 4")
- **Ring progress** (existing)
- **"↗ back to focus mode" chip** (orange tint) — sets `isFocusMode = true`
- **3-button control row:**
  - `end` (ghost pill, left)
  - `⏸` (large white button, center)
  - `skip ›` (ghost pill, right)
- **Nav dock home tab:** shows ⏸ PAUSE while timer is running, so user can pause from any tab

---

## 7. Session Complete Logic

Fires inside the existing `LaunchedEffect` timer countdown block in `KuroPomodoroApp.kt` when focus timer reaches 0:

```
activeTask.pomodorosDone++
globalCompletedSessions++

if (pomodorosDone >= plannedSessions):
    tasks = tasks.map { if it.id == activeTask.id → it.copy(completed = true) else it }
    activeTask = null
    isFocusMode = false
    selectedTab = todoTabIndex   // show user the completed task

else:
    // break timer starts (existing behavior)
    isFocusMode = false          // stay on timer tab during break
```

Task list mutation uses `.map { ... .copy(...) }` — no new persistence needed.

---

## Key Files

| File | Change |
|---|---|
| `model/KuroModels.kt` | Add `plannedSessions`, `pomodorosDone` to `FocusTask` |
| `KuroPomodoroApp.kt` | Add `activeTask`, `isFocusMode` state; session complete logic; pass callbacks down |
| `ui/TodoScreen.kt` | Add `SwipeToDismiss` leading swipe to `TaskGlassRow`; add pre-session `ModalBottomSheet` |
| `ui/TimerScreen.kt` | Unused/legacy — no changes |

---

## Pencil Design References

- Flow diagram: frame `TYlgg` (y:952) — A→F steps with real code annotations
- Focus Mode screen: `VKt6u`
- Running state (controls redesign): `bjquU`
- Idle state: `aUoSw`
- Break state: `o3rNk`
