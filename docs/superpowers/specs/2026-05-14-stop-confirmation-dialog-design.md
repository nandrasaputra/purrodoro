# Stop Confirmation Dialog — Design Spec

**Date:** 2026-05-14

---

## Goal

Prevent accidental session abandonment by requiring confirmation before stopping a paused timer. Reassure the user that committed focus time still counts.

---

## Trigger Condition

The confirmation dialog appears **only when the user taps Stop during a paused Focus session**.

- Paused **Focus** → show dialog ✓
- Paused **ShortBreak** or **LongBreak** → stop immediately, no dialog (no focus work at stake)

The stop button is only visible in the paused (`isStarted && !isRunning`) state, so there is no running-state case to handle.

---

## Dialog Content

| Element | Value |
|---|---|
| Title | **Taking a break?** |
| Body | Your focus time so far will still be counted. |
| Confirm button | **Stop for now** |
| Dismiss button | **Stay focused** |

---

## Behavior

| Action | Result |
|---|---|
| Tap "Stop for now" | Executes existing `onStop` logic: reset timer, clear active task, exit focus mode |
| Tap "Stay focused" | Dismiss dialog; timer remains paused, no state change |
| Tap scrim (outside dialog) | Same as "Stay focused" — dismiss only |

---

## Implementation

- **Component:** Compose `AlertDialog` (Material3) — no new dependencies
- **State:** Add `var showStopConfirmDialog by remember { mutableStateOf(false) }` local to `FocusDashboard`
- **Stop button tap:** Instead of calling `onStop` directly, set `showStopConfirmDialog = true`
- **Dialog placement:** Inside `FocusDashboard`, rendered conditionally when `showStopConfirmDialog == true`
- **No changes to `onStop` logic itself** — dialog is purely a UI gate

---

## Scope Exclusions

- No changes to session record / stamp logic (tracked as a separate future spec)
- No changes to running-state behavior (stop button not shown while running)
- No animation beyond default `AlertDialog` enter/exit

---

## Files Affected

| File | Change |
|---|---|
| `KuroPomodoroApp.kt` (`FocusDashboard`) | Add `showStopConfirmDialog` state, gate stop button, add `AlertDialog` composable |
