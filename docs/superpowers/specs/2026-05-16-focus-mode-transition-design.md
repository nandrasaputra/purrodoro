# Design: Focus Mode Transition

**Date:** 2026-05-16  
**Status:** Approved

## Problem

Entering and exiting Focus Mode in `KuroPomodoroApp.kt` is currently an instantaneous swap. The `FocusDashboard` composable short-circuits with `if (isFocusMode && isRunning) { ... return }`, and the dock is conditionally removed from the layout (`if (!(isFocusMode && isRunning)) DashboardDock(...)`). The result: the dock pops, the cycle dots and controls vanish, and the timer text snaps to a new size — all in a single frame.

This is functionally correct but undersells the moment. Focus Mode is the emotional centerpiece of a Pomodoro app: the user is committing to a block of attention. The transition should *feel* like the room dimming and the world receding, not like a screen change.

## Goal

Animate the enter/exit of Focus Mode with a calm, cinematic motion that matches the cat-on-windowsill backdrop and the meditative tone of the app. The transition should feel intentional on entry (slightly longer, decelerating into stillness) and responsive on exit (shorter, accelerating away).

## Solution: Single-Source Transition with Layered Choreography

Drive the entire transition from one `updateTransition(targetState = isFocusMode)` at the `KuroPomodoroApp` level so the dock, the dim scrim, and the dashboard interior animate in perfect lockstep. The timer text never unmounts — its style (size, weight, position) is animated rather than swapped — so the countdown stays smooth across the transition.

```
KuroPomodoroApp
  Transition(focusActive = isFocusMode && isRunning)
    ├─ DashboardDock (AnimatedVisibility: slide + fade)
    ├─ DimScrim       (animateFloat alpha; always mounted)
    └─ FocusDashboard
         ├─ Timer text (animated style: size, weight, position)
         ├─ "FOCUS" label    (AnimatedVisibility: fade + translate)
         ├─ Cycle dots       (AnimatedVisibility: fade)
         ├─ Control buttons  (AnimatedVisibility: fade)
         ├─ "enter focus mode" pill (AnimatedVisibility: fade)
         └─ "tap anywhere…" hint    (AnimatedVisibility: delayed fade)
```

## Motion Choreography

### Enter Focus Mode (~700ms total)

| Stage     | Timing      | What animates                                                                  |
| --------- | ----------- | ------------------------------------------------------------------------------ |
| 0–500ms   | Dock        | Slides down its full height + fades to 0 (`EmphasizedAccelerate`)              |
| 0–400ms   | Chrome      | Cycle dots, control buttons, pill, pause label fade out + 4dp downward drift   |
| 100–700ms | Scrim       | Alpha 0 → 0.45 black overlay (deepens the room)                                |
| 200–700ms | Timer text  | Size 80sp → 96sp, weight 400 → 200, letter-spacing −1 → −2; re-centers         |
| 350–700ms | FOCUS label | Fades in above the timer with 8dp upward translate-in                          |
| 350–700ms | Task title  | Restyles to focus position (color, size)                                       |
| 600–1000ms| Hint        | "tap anywhere…" fades in last (never feels like a CTA)                         |
| 800–2000ms| Breath pulse| Timer text scales 1.0 → 1.02 → 1.0 over 1.2s (one-shot, signals "you're in")   |

### Exit Focus Mode (~400ms total)

| Stage     | Timing    | What animates                                              |
| --------- | --------- | ---------------------------------------------------------- |
| 0–250ms   | Label/hint| FOCUS and hint fade out fast                               |
| 0–400ms   | Scrim     | Alpha 0.45 → 0                                             |
| 0–400ms   | Timer     | Morphs back to dashboard size/weight                       |
| 150–400ms | Chrome    | Dock, dots, controls slide + fade in (`EmphasizedDecelerate`) |

Exit is intentionally shorter per Material 3 guidance — leaving feels responsive, arriving feels deliberate.

## Motion Constants

New file `ui/components/FocusModeMotion.kt`:

```kotlin
object FocusModeMotion {
    const val EnterDurationMs = 700
    const val ExitDurationMs = 400
    const val BreathDelayMs = 800L
    const val BreathDurationMs = 1200

    // Material 3 expressive easing
    val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)   // emphasized-decelerate
    val ExitEasing  = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)   // emphasized-accelerate

    const val ScrimAlphaFocus = 0.45f
    const val BreathScalePeak = 1.02f
}
```

## Architecture

### Single Transition Source

In `KuroPomodoroApp`, lift `isFocusMode && isRunning` into a single derived boolean and feed it to `updateTransition` so all dependent animations share one clock:

```kotlin
val focusActive = isFocusMode && isRunning
val focusTransition = updateTransition(targetState = focusActive, label = "focusMode")

val scrimAlpha by focusTransition.animateFloat(
    transitionSpec = {
        if (targetState) tween(FocusModeMotion.EnterDurationMs, easing = FocusModeMotion.EnterEasing)
        else             tween(FocusModeMotion.ExitDurationMs,  easing = FocusModeMotion.ExitEasing)
    },
    label = "scrimAlpha"
) { active -> if (active) FocusModeMotion.ScrimAlphaFocus else 0f }
```

The dock uses `AnimatedVisibility(visible = !focusActive)` with a slide + fade combo. The dim scrim is a new `Box` overlay always mounted between the backdrop and the screen content; its alpha is driven by the transition, so it fades in/out naturally and contributes zero visual weight at rest.

### `FocusDashboard` Refactor

Remove the early-return. Instead:

- Render a single layout where the timer and task text always exist.
- Animate their *style* (size, color, weight, position) using `animateFloatAsState`/`animateDpAsState` driven by `isFocusMode && isRunning`.
- Wrap auxiliary chrome (cycle dots, control row, "enter focus mode" pill, paused label) in `AnimatedVisibility` blocks that hide when focus is active.
- Wrap the FOCUS caps label and "tap anywhere…" hint in `AnimatedVisibility` blocks that show only when focus is active, with a per-element delay.
- Replace the full-screen `Modifier.clickable(onClick = onExitFocusMode)` with a `pointerInput` layer that only consumes taps when `focusActive` is `true` (the underlying boolean — not the animation progress — so taps work immediately on enter and stop being consumed the instant the user dismisses).

### Process Death / Initial State

`isFocusMode` is already `rememberSaveable`. On process restart, we want to *snap* to the current state rather than animate from `false` → `true`. `updateTransition` handles this naturally: its initial value equals the target on the first composition, so no animation runs.

The breath pulse must NOT fire on process restore. Gate it on a `LaunchedEffect(focusActive)` that skips the first invocation when `focusActive` is already true at composition. Concretely:

```kotlin
var hasComposed by remember { mutableStateOf(false) }
LaunchedEffect(focusActive) {
    if (focusActive && hasComposed) {
        delay(FocusModeMotion.BreathDelayMs)
        // run breath animation
    }
    hasComposed = true
}
```

This way the pulse only runs when the user actively *enters* focus mode in the foreground session.

## Files Touched

| File | Change |
| ---- | ------ |
| `KuroPomodoroApp.kt` | Wrap dock in `AnimatedVisibility`; add dim-scrim layer; introduce `updateTransition` for focus mode; refactor `FocusDashboard` to use animated style instead of early-return |
| `ui/components/FocusModeMotion.kt` | **NEW** — motion constants (durations, easings, alpha/scale targets) |

## Edge Cases

- **Pause during focus** — existing logic flips `isFocusMode = false`; transition runs naturally.
- **Stop during focus** — sets `isFocusMode = false` and clears `activeTask`; transition runs.
- **Cycle completion dialog** — appears above both states; transition runs underneath. No interaction.
- **Tab switch from Todo via `onFocusTask`** — sets `isFocusMode = true` + switches to Timer tab; transition runs on the next composition.
- **Process death / restore** — initial transition state equals target, so it snaps without animation. Breath pulse is gated and doesn't fire.
- **Rapid toggle** — `updateTransition` interrupts and reverses gracefully; no manual handling needed.

## Non-Goals

- No haptic feedback on enter/exit
- No system bar color animation (edge-to-edge already handles it)
- No backdrop image scale/parallax — would compete with the calm feel
- No sound effects on transition
- No change to the underlying focus-mode trigger logic (start, pause, stop, "enter focus mode" pill, "tap anywhere" exit)
- No change to `TimerService` / state machine
