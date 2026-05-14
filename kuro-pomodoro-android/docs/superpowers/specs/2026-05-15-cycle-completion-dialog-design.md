# Cycle Completion Acknowledgment Dialog

**Date:** 2026-05-15
**Status:** Approved (pending review)

## Summary

Add an in-app `AlertDialog` that appears when a Pomodoro cycle completes (whether by natural tick or user-initiated skip of the long break). The user must tap **Done** to acknowledge; only then does the dialog dismiss. The system notification continues to fire as it does today — this dialog is an additional, in-app surface.

To keep the experience from feeling repetitive, the dialog draws its title and body from a pool of 5 warm, affirming copy variants, selected randomly per appearance. The button label is fixed.

## Goals

- Give the user an explicit moment of closure when a cycle ends.
- Ensure no completion event is silently missed: if the cycle completes while the app is backgrounded, the dialog appears the next time the app is opened.
- Stay consistent with the app's quiet, calm tone.

## Non-Goals

- Per-segment (focus end, break end) dialogs — out of scope.
- Auto-starting the next cycle on acknowledgment — user manually starts the next cycle.
- Showing stats or session summaries in the dialog.
- Avoiding back-to-back repetition of the same copy variant.
- Customizing or adding to the copy pool from settings.

## User Experience

### Trigger

A cycle is "complete" when:

1. The natural tick loop in `TimerService.startTicking()` advances past the final segment of the cycle (long break ends), OR
2. The user invokes skip via `TimerController.skip()` while on the final segment.

Both paths converge on `TimerService.finishCycleAndStop(...)` (added in the prior bug fix). The acknowledgment flag is set there — single source of truth.

### Display rules

- Dialog appears only when the app is in the foreground/resumed.
- If the cycle completes while the app is backgrounded, the dialog is queued (via persistent flag) and appears the next time the app's root composable is rendered with the flag observed `true`.
- Dialog is **non-cancelable**: tapping outside or pressing back does not dismiss it. The user must tap **Done**.
- The system notification (`CompletionKind.Cycle`) is unaffected — it continues to fire on every cycle completion.

### Copy variants

A pool of 5 variants, each with a title and body. Button label is always **Done**.

| # | Title           | Body                                                                |
| - | --------------- | ------------------------------------------------------------------- |
| 1 | Cycle complete  | That's a full cycle behind you. Take a moment — you earned it.      |
| 2 | Nicely done     | You finished a full Pomodoro cycle. Rest before the next one.       |
| 3 | That's a wrap   | Four focused sessions, all done. Step away with a clear head.       |
| 4 | Good work       | A complete cycle is no small thing. Take the win.                   |
| 5 | All done        | One full cycle behind you. Breathe, then come back when you're ready. |

### Selection rule

Pure random per appearance: `pool.random()`. The selected variant is captured in `remember(showFlag)` so a recomposition during the dialog's lifetime does not reroll the copy mid-display.

## Architecture

### Data layer

`AppRepository` gains:

- `val pendingCycleCompletionAckFlow: Flow<Boolean>` — emits the current flag value, hot.
- `suspend fun setPendingCycleCompletionAck(pending: Boolean)` — writes the flag to DataStore.

A new `Preferences.Key<Boolean>` (e.g., `PENDING_CYCLE_COMPLETION_ACK`) is added to the existing DataStore prefs file. Default value is `false`.

### Timer layer

`TimerService.finishCycleAndStop(resetState)` (existing helper, added in prior bug fix) gains one additional side effect:

```
repo.setPendingCycleCompletionAck(true)
```

invoked before `_state.value = resetState`. This guarantees both the natural-completion path (called from `startTicking()` tick loop) and the skip path (called from `doSkip()` end branch) trigger the flag identically.

### UI layer

`KuroPomodoroApp.kt` (root composable):

1. Collect the flag as state: `val pendingAck by repo.pendingCycleCompletionAckFlow.collectAsState(initial = false)`.
2. When `pendingAck == true`, render an `AlertDialog`:
   - `properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`
   - Title and body from a `remember(pendingAck) { CycleCompletionCopy.pool.random() }` pick.
   - Confirm button **Done** — on click: `scope.launch { repo.setPendingCycleCompletionAck(false) }`.
3. The copy pool lives in a small object in the UI module: `object CycleCompletionCopy { val pool: List<Pair<String, String>> = listOf(...) }`.

No changes to `TimerStateMachine`, `TimerReconciler`, or `TimerNotification`.

## Data Flow

```
[ Cycle ends — tick or skip ]
        │
        ▼
TimerService.finishCycleAndStop(resetState)
        │
        ├─► repo.setPendingCycleCompletionAck(true)   ◄── NEW
        ├─► fire CompletionKind.Cycle notification     (existing)
        ├─► _state.value = resetState                  (existing)
        └─► cancelAlarm + stopForeground + stopSelf    (existing)

[ Anytime app is in foreground ]
        │
        ▼
KuroPomodoroApp observes pendingCycleCompletionAckFlow
        │
        ├─► flag == true → render AlertDialog (random copy)
        └─► user taps Done → repo.setPendingCycleCompletionAck(false) → dialog dismisses
```

## Persistence Behavior

- Flag survives app restart, process death, and device reboot via DataStore.
- If the user uninstalls/reinstalls, prefs are wiped — flag defaults to `false`. Acceptable.
- If multiple cycles complete while the app is closed (e.g., notification ignored repeatedly), the flag remains `true` — the user sees a single dialog on next open. This is acceptable; cycle completion is a discrete event, not a counter.

## Error Handling

- Repository writes are suspending and may fail (DataStore I/O). Failures are silently ignored (existing convention in the repo for non-critical writes). Worst case: dialog reappears next launch — harmless.
- Dialog rendering has no failure modes beyond standard Compose.

## Testing

### Required

- **Unit test** in `AppRepositoryTest`: round-trip — set flag `true`, observe flow emits `true`; set `false`, observe flow emits `false`; default initial value is `false`.

### Optional (nice-to-have)

- **Compose UI test**: Given the flag is `true`, the dialog renders with a title from the pool. Tapping the confirm button clears the flag and dismisses.

### Manual verification

1. Start a focus session, fast-forward via skip until long break is current, skip the long break → dialog appears with one of 5 copy variants → tap **Done** → dialog dismisses, UI shows fresh idle cycle.
2. Repeat several times; confirm copy varies (not always the same variant).
3. Start a cycle, advance to long break, background the app, skip via the notification action → reopen app → dialog appears.
4. Tap outside the dialog and press back — dialog should not dismiss.

## Files to Touch

- `app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt` — add flag flow + setter.
- DataStore prefs file (wherever `Preferences.Key<…>` definitions live alongside `AppRepository`) — add new key.
- `app/src/main/java/com/endiar/kuropomodoro/timer/TimerService.kt` — set flag inside `finishCycleAndStop`.
- `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` — collect flag, render dialog, define `CycleCompletionCopy` object.
- `app/src/test/java/com/endiar/kuropomodoro/data/AppRepositoryTest.kt` (or equivalent) — round-trip test.
