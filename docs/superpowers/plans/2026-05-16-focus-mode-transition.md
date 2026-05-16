# Focus Mode Transition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the instant focus-mode swap with a calm, cinematic transition: dim scrim fades in, dock slides + fades out, chrome (cycle dots, controls) collapses, the timer text morphs to a larger weight-200 hero style with a one-shot "breath" pulse — all driven by a single `updateTransition`. Exit is the same motion in reverse, slightly faster.

**Architecture:** One `updateTransition(focusActive)` at the `KuroPomodoroApp` level drives the dock's `AnimatedVisibility`, a new dim-scrim layer, and the `FocusDashboard` interior. The timer text never unmounts; its size, weight, and letter-spacing are animated. Auxiliary chrome (cycle dots, control buttons, "enter focus mode" pill) is wrapped in `AnimatedVisibility` blocks gated on `!focusActive`; focus-only chrome (FOCUS label, "tap anywhere…" hint) is gated on `focusActive`.

**Tech Stack:** Kotlin, Jetpack Compose (already in BOM `2024.09.00`), JUnit 4 (already configured). No new dependencies.

**Spec:** [`docs/superpowers/specs/2026-05-16-focus-mode-transition-design.md`](../specs/2026-05-16-focus-mode-transition-design.md)

---

## File Map

| File | Change |
| ---- | ------ |
| `app/src/main/java/com/endiar/kuropomodoro/ui/components/FocusModeMotion.kt` | **NEW** — motion constants (durations, easings, alpha/scale targets) |
| `app/src/test/java/com/endiar/kuropomodoro/ui/components/FocusModeMotionTest.kt` | **NEW** — JVM unit test pinning the motion constants |
| `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` | Introduce `focusActive` + `updateTransition`; wrap dock in `AnimatedVisibility`; add dim-scrim overlay; refactor `FocusDashboard` (remove early-return, animate timer style, wrap chrome in `AnimatedVisibility`, add breath pulse) |

---

## Task 1: Add `FocusModeMotion` constants with a pinning unit test

A small constants object is the right home for shared motion values so dock, scrim, timer, and chrome stay in lockstep. A pinning test makes accidental drift loud.

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/components/FocusModeMotion.kt`
- Test:   `app/src/test/java/com/endiar/kuropomodoro/ui/components/FocusModeMotionTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/endiar/kuropomodoro/ui/components/FocusModeMotionTest.kt`:

```kotlin
package com.endiar.kuropomodoro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusModeMotionTest {

    @Test
    fun `enter is longer than exit so arriving feels deliberate`() {
        assertTrue(
            "enter must be > exit",
            FocusModeMotion.EnterDurationMs > FocusModeMotion.ExitDurationMs
        )
    }

    @Test
    fun `durations are within calm-cinematic range`() {
        assertEquals(700, FocusModeMotion.EnterDurationMs)
        assertEquals(400, FocusModeMotion.ExitDurationMs)
    }

    @Test
    fun `breath pulse fires after enter completes`() {
        assertTrue(
            "breath must wait until after enter finishes",
            FocusModeMotion.BreathDelayMs >= FocusModeMotion.EnterDurationMs
        )
        assertEquals(1200, FocusModeMotion.BreathDurationMs)
        assertEquals(1.02f, FocusModeMotion.BreathScalePeak, 0.0001f)
    }

    @Test
    fun `scrim alpha is darker than mid-grey but not opaque`() {
        assertEquals(0.45f, FocusModeMotion.ScrimAlphaFocus, 0.0001f)
    }

    @Test
    fun `easings exist`() {
        assertNotNull(FocusModeMotion.EnterEasing)
        assertNotNull(FocusModeMotion.ExitEasing)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd kuro-pomodoro-android && ./gradlew :app:testDebugUnitTest --tests "com.endiar.kuropomodoro.ui.components.FocusModeMotionTest"
```

Expected: FAIL with `Unresolved reference: FocusModeMotion`.

- [ ] **Step 3: Create the `FocusModeMotion` file**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/components/FocusModeMotion.kt`:

```kotlin
package com.endiar.kuropomodoro.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Shared motion constants for the focus-mode enter/exit transition.
 *
 * Calm & cinematic: enter is decelerated and slightly longer (deliberate
 * arrival); exit is accelerated and shorter (responsive departure). A
 * one-shot breath pulse fires after enter completes to signal "you're in".
 *
 * See: docs/superpowers/specs/2026-05-16-focus-mode-transition-design.md
 */
object FocusModeMotion {
    const val EnterDurationMs = 700
    const val ExitDurationMs = 400

    const val BreathDelayMs = 800L
    const val BreathDurationMs = 1200
    const val BreathScalePeak = 1.02f

    const val ScrimAlphaFocus = 0.45f

    /** Material 3 emphasized-decelerate — for entering content. */
    val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Material 3 emphasized-accelerate — for exiting content. */
    val ExitEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd kuro-pomodoro-android && ./gradlew :app:testDebugUnitTest --tests "com.endiar.kuropomodoro.ui.components.FocusModeMotionTest"
```

Expected: `BUILD SUCCESSFUL`, all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/FocusModeMotion.kt \
        kuro-pomodoro-android/app/src/test/java/com/endiar/kuropomodoro/ui/components/FocusModeMotionTest.kt
git commit -m "feat(ui): add FocusModeMotion constants for focus transition"
```

---

## Task 2: Drive the dock and scrim from a shared `updateTransition`

Hoist `focusActive = isFocusMode && isRunning` into an `updateTransition` so the dock fade/slide and the dim scrim animate from the same clock.

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add the new imports to `KuroPomodoroApp.kt`**

Add these imports alongside the existing `androidx.compose.*` imports near the top of the file:

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.alpha
import com.endiar.kuropomodoro.ui.components.FocusModeMotion
```

- [ ] **Step 2: Introduce `focusActive` and the shared transition**

In `KuroPomodoroApp`, immediately after the existing line:

```kotlin
val isStarted = runtimeState != null && runtimeState?.session?.timerState != TimerState.Idle
```

add:

```kotlin
val focusActive = isFocusMode && isRunning
val focusTransition = updateTransition(targetState = focusActive, label = "focusMode")

val scrimAlpha by focusTransition.animateFloat(
    transitionSpec = {
        if (targetState) {
            tween(
                durationMillis = FocusModeMotion.EnterDurationMs,
                easing = FocusModeMotion.EnterEasing
            )
        } else {
            tween(
                durationMillis = FocusModeMotion.ExitDurationMs,
                easing = FocusModeMotion.ExitEasing
            )
        }
    },
    label = "scrimAlpha"
) { active -> if (active) FocusModeMotion.ScrimAlphaFocus else 0f }
```

- [ ] **Step 3: Add the dim-scrim overlay**

The scrim sits *between* the backdrop and screen content, so it deepens the room without dimming the dock or dialogs. Inside the outer `Box(modifier = Modifier.fillMaxSize())` of `surfaceContent` (the one that already contains the screen content `Box`, `PremiumMessage`, and `DashboardDock`), insert the scrim as the **first child** so it sits behind everything else:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // NEW — dim scrim, always mounted, alpha driven by transition
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(scrimAlpha)
            .background(Color.Black)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when (selectedTab) { /* ... unchanged ... */ }
    }
    // ... rest unchanged ...
}
```

- [ ] **Step 4: Replace the dock's `if` gate with `AnimatedVisibility`**

Find this block:

```kotlin
if (!(isFocusMode && isRunning)) {
    DashboardDock(
        isRunning = isRunning,
        isSoundPlaying = isSoundPlaying,
        onNavigate = { selectedTab = it },
        selectedTab = selectedTab,
        glassTreatment = glassTreatment,
        overBackdrop = true,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .onSizeChanged { dockHeightPx = it.height }
    )
}
```

Replace it with:

```kotlin
AnimatedVisibility(
    visible = !focusActive,
    enter = slideInVertically(
        animationSpec = tween(
            durationMillis = FocusModeMotion.ExitDurationMs,
            easing = FocusModeMotion.ExitEasing
        )
    ) { fullHeight -> fullHeight } + fadeIn(
        animationSpec = tween(
            durationMillis = FocusModeMotion.ExitDurationMs,
            easing = FocusModeMotion.ExitEasing
        )
    ),
    exit = slideOutVertically(
        animationSpec = tween(
            durationMillis = FocusModeMotion.EnterDurationMs,
            easing = FocusModeMotion.EnterEasing
        )
    ) { fullHeight -> fullHeight } + fadeOut(
        animationSpec = tween(
            durationMillis = FocusModeMotion.EnterDurationMs,
            easing = FocusModeMotion.EnterEasing
        )
    ),
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .navigationBarsPadding()
) {
    DashboardDock(
        isRunning = isRunning,
        isSoundPlaying = isSoundPlaying,
        onNavigate = { selectedTab = it },
        selectedTab = selectedTab,
        glassTreatment = glassTreatment,
        overBackdrop = true,
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { dockHeightPx = it.height }
    )
}
```

Note: `navigationBarsPadding()` moves to the `AnimatedVisibility` modifier so the slide-out distance equals the dock + inset height. `onSizeChanged` stays on the inner dock so `dockHeightPx` continues to reflect the actual dock height.

- [ ] **Step 5: Verify the project compiles**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. If there are compile errors (likely missing imports) fix them before committing.

- [ ] **Step 6: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat(ui): animate dock and dim scrim via shared focus transition"
```

---

## Task 3: Refactor `FocusDashboard` to animate timer style and chrome instead of swapping screens

Remove the early-return. Render one layout where the timer always exists and its style animates between dashboard and focus presentations. Wrap dashboard chrome in `AnimatedVisibility(!focusActive)` and focus-only chrome in `AnimatedVisibility(focusActive)` — `AnimatedVisibility` inside a `Column` collapses the layout space smoothly, so the timer drifts toward center naturally as chrome disappears.

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add the new imports needed by `FocusDashboard`**

Append to the existing imports in `KuroPomodoroApp.kt`. `FontWeight` is already imported (used in the existing dashboard) — only add the ones that aren't already present:

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
```

(`slideInVertically`, `slideOutVertically`, `fadeIn`, `fadeOut`, `tween`, `updateTransition`, `animateFloat`, and `AnimatedVisibility` were already added in Task 2 Step 1.)

- [ ] **Step 2: Update `FocusDashboard` signature to receive the shared transition**

Change the signature from:

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
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
)
```

to:

```kotlin
@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    focusActive: Boolean,
    focusTransition: Transition<Boolean>,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
)
```

And update the call site in `surfaceContent` (the existing `AppTab.Timer -> FocusDashboard(...)` block). Replace the entire `FocusDashboard(...)` call with:

```kotlin
AppTab.Timer -> FocusDashboard(
    timerMode = timerMode,
    remainingSeconds = remainingSeconds,
    isRunning = isRunning,
    isStarted = isStarted,
    focusActive = focusActive,
    focusTransition = focusTransition,
    activeTask = activeTask,
    cycleItems = cycleItems,
    cycleIndex = cycleIndex,
    currentTimerState = currentTimerState,
    timerSettings = settings,
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
    onExitFocusMode = { isFocusMode = false },
    onEnterFocusMode = { isFocusMode = true },
    contentPadding = screenContentPadding
)
```

Two things change vs the original call site:
- The `isFocusMode = isFocusMode` argument is removed (deleted from the signature).
- The new `focusActive = focusActive` and `focusTransition = focusTransition` arguments are added.

All three lambda bodies (`onStartPause`, `onStop`, `onSkip`) are byte-identical to today's code.

- [ ] **Step 3: Replace the entire `FocusDashboard` body with the animated single-layout version**

Delete everything inside `FocusDashboard` from the opening brace to the closing brace (lines ~483–797 in the current file) and replace with:

```kotlin
{
    val dots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, timerSettings)
    val totalFocusSessions = dots.totalFocusSessions
    val completedFocusSessions = dots.completedFocusSessions
    val isCurrentlyFocus = dots.isCurrentlyFocus

    val buttonBg = if (isRunning) Color.White.copy(alpha = 0.92f) else Color(0xFFFF8B6F)
    val buttonContentColor = if (isRunning) Color(0xFF241E2B) else Color.White
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    // ── Animated timer text style ─────────────────────────────────────────
    val timerSize by focusTransition.animateFloat(
        transitionSpec = {
            if (targetState) tween(FocusModeMotion.EnterDurationMs, easing = FocusModeMotion.EnterEasing)
            else             tween(FocusModeMotion.ExitDurationMs,  easing = FocusModeMotion.ExitEasing)
        },
        label = "timerSize"
    ) { active -> if (active) 96f else 80f }

    val timerWeight by focusTransition.animateInt(
        transitionSpec = {
            if (targetState) tween(FocusModeMotion.EnterDurationMs, easing = FocusModeMotion.EnterEasing)
            else             tween(FocusModeMotion.ExitDurationMs,  easing = FocusModeMotion.ExitEasing)
        },
        label = "timerWeight"
    ) { active -> if (active) 200 else 400 }

    val timerLetterSpacing by focusTransition.animateFloat(
        transitionSpec = {
            if (targetState) tween(FocusModeMotion.EnterDurationMs, easing = FocusModeMotion.EnterEasing)
            else             tween(FocusModeMotion.ExitDurationMs,  easing = FocusModeMotion.ExitEasing)
        },
        label = "timerLetterSpacing"
    ) { active -> if (active) -2f else -1f }

    val timerAlpha by focusTransition.animateFloat(
        transitionSpec = {
            if (targetState) tween(FocusModeMotion.EnterDurationMs, easing = FocusModeMotion.EnterEasing)
            else             tween(FocusModeMotion.ExitDurationMs,  easing = FocusModeMotion.ExitEasing)
        },
        label = "timerAlpha"
    ) { active -> if (active) 0.95f else 0.9f }

    // ── Breath pulse (one-shot, only on user-initiated enter) ─────────────
    val breathScale = remember { Animatable(1f) }
    var hasComposed by remember { mutableStateOf(false) }
    LaunchedEffect(focusActive) {
        if (focusActive && hasComposed) {
            delay(FocusModeMotion.BreathDelayMs)
            breathScale.animateTo(
                targetValue = FocusModeMotion.BreathScalePeak,
                animationSpec = tween(
                    durationMillis = FocusModeMotion.BreathDurationMs / 2,
                    easing = FocusModeMotion.EnterEasing
                )
            )
            breathScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = FocusModeMotion.BreathDurationMs / 2,
                    easing = FocusModeMotion.ExitEasing
                )
            )
        } else if (!focusActive) {
            breathScale.snapTo(1f)
        }
        hasComposed = true
    }

    // ── Layout ─────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .then(
                // Full-screen tap layer for exit, gated on the underlying boolean
                // (not animation progress) so taps work the instant focus is active.
                if (focusActive) {
                    Modifier.pointerInput(focusActive) {
                        detectTapGestures(onTap = { onExitFocusMode() })
                    }
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // FOCUS caps label — focus-only, delayed fade so it settles last
        AnimatedVisibility(
            visible = focusActive,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.EnterDurationMs - 350,
                    delayMillis = 350,
                    easing = FocusModeMotion.EnterEasing
                )
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.EnterDurationMs - 350,
                    delayMillis = 350,
                    easing = FocusModeMotion.EnterEasing
                )
            ) { -it / 2 },
            exit = fadeOut(
                animationSpec = tween(durationMillis = 250, easing = FocusModeMotion.ExitEasing)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FOCUS",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // Dashboard task block + cycle dots — collapse on enter focus
        AnimatedVisibility(
            visible = !focusActive,
            enter = fadeIn(
                animationSpec = tween(FocusModeMotion.ExitDurationMs, delayMillis = 150, easing = FocusModeMotion.ExitEasing)
            ) + expandVertically(
                animationSpec = tween(FocusModeMotion.ExitDurationMs, easing = FocusModeMotion.ExitEasing)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 400, easing = FocusModeMotion.EnterEasing)
            ) + shrinkVertically(
                animationSpec = tween(durationMillis = 400, easing = FocusModeMotion.EnterEasing)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isRunning && activeTask != null) {
                    Text(
                        text = activeTask.title,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${activeTask.completedSessions + 1} / ${activeTask.plannedSessions}",
                        color = Color(0xFFFF8B6F).copy(alpha = 0.80f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                CycleDotsRow(
                    totalFocusSessions = totalFocusSessions,
                    completedFocusSessions = completedFocusSessions,
                    isCurrentlyFocus = isCurrentlyFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        // Timer text — ALWAYS rendered, style animated, breath-scaled
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = breathScale.value
                    scaleY = breathScale.value
                },
            textAlign = TextAlign.Center,
            text = formatSeconds(remainingSeconds),
            color = Color.White.copy(alpha = timerAlpha),
            fontSize = timerSize.sp,
            fontWeight = FontWeight(timerWeight),
            letterSpacing = timerLetterSpacing.sp
        )

        // Focus-mode task block — sits below the timer in focus mode
        AnimatedVisibility(
            visible = focusActive && activeTask != null,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.EnterDurationMs - 350,
                    delayMillis = 350,
                    easing = FocusModeMotion.EnterEasing
                )
            ) + expandVertically(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.EnterDurationMs - 200,
                    delayMillis = 200,
                    easing = FocusModeMotion.EnterEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FocusModeMotion.ExitEasing)
            ) + shrinkVertically(
                animationSpec = tween(durationMillis = 250, easing = FocusModeMotion.ExitEasing)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                activeTask?.let { task ->
                    Text(
                        text = task.title,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${task.completedSessions + 1} / ${task.plannedSessions}",
                        color = Color(0xFFFF8B6F).copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Paused label — dashboard only
        AnimatedVisibility(
            visible = !focusActive && isStarted && !isRunning,
            enter = fadeIn(animationSpec = tween(durationMillis = 250)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "${timerMode.label} · paused",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Control buttons row — dashboard only
        AnimatedVisibility(
            visible = !focusActive,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.ExitDurationMs,
                    delayMillis = 150,
                    easing = FocusModeMotion.ExitEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 350, easing = FocusModeMotion.EnterEasing)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    val currentItemType = cycleItems.getOrNull(cycleIndex)?.type
                    val isOnBreak = currentItemType is SessionItemType.ShortBreak ||
                            currentItemType is SessionItemType.LongBreak
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(buttonBg)
                            .clickable(onClick = onStartPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u23f8",
                            color = buttonContentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isOnBreak) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .border(1.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
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
                } else if (isStarted) {
                    val currentItemType = cycleItems.getOrNull(cycleIndex)?.type
                    val isOnBreak = currentItemType is SessionItemType.ShortBreak ||
                            currentItemType is SessionItemType.LongBreak
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(buttonBg)
                            .clickable(onClick = onStartPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u25b6",
                            color = buttonContentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                val currentItemType2 = cycleItems.getOrNull(cycleIndex)?.type
                                if (currentItemType2 is SessionItemType.Focus) {
                                    showStopConfirmDialog = true
                                } else {
                                    onStop()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u25a0",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 18.sp
                        )
                    }
                    if (isOnBreak) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .border(1.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
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
                } else {
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
            }
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

        Spacer(Modifier.weight(1f))

        // "enter focus mode" pill — dashboard only while running
        AnimatedVisibility(
            visible = !focusActive && isRunning,
            enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFF8B6F).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFFFF8B6F).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onEnterFocusMode)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "↗  enter focus mode",
                        color = Color(0xFFFF8B6F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(40.dp))
            }
        }

        // "tap anywhere…" hint — focus mode, fades in last
        AnimatedVisibility(
            visible = focusActive,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 400,
                    delayMillis = 600,
                    easing = FocusModeMotion.EnterEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 200)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "tap anywhere to show controls",
                    color = Color.White.copy(alpha = 0.22f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
```

- [ ] **Step 4: Verify the project compiles**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. If there are compile errors (likely missing imports, unused imports, or type mismatches), fix them before committing.

- [ ] **Step 5: Run the unit test suite to confirm nothing logical regressed**

```bash
cd kuro-pomodoro-android && ./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` — all existing tests still pass (we only touched UI composables).

- [ ] **Step 6: Manual smoke-test plan (do this on a device or emulator)**

Install the debug APK and verify each transition feels right:

```bash
cd kuro-pomodoro-android && ./gradlew installDebug
```

Then manually verify:

1. **Cold enter:** Tap "Focus Now" from idle dashboard → dock slides down + fades; cycle dots fade; timer grows and thins to weight-200; "FOCUS" appears above; "tap anywhere…" fades in last; after ~0.8s the timer does one subtle scale pulse.
2. **Tap to exit:** Tap anywhere on the focus screen → "FOCUS" + hint fade fast; timer shrinks back; dock slides up; dashboard chrome fades in. Total exit feels faster than enter.
3. **Pause auto-exits:** While in focus mode, pause via notification or service → focus mode exits with the same calm animation.
4. **Stop auto-exits:** Tap stop while in focus → animates out cleanly, returns to idle dashboard.
5. **Re-enter via "enter focus mode" pill:** Pause, resume, tap the coral pill → enters focus mode with full animation (and breath pulse).
6. **From Todo → focus task:** Open Todo, focus a task → tab switches to Timer and focus mode transitions in (no breath pulse expected on first composition? Actually `hasComposed` becomes true after first effect run; second time entering this session, pulse should fire — verify both cases at least once).
7. **Process restore:** While in focus mode, kill the app from recents and reopen → the focus state restores; the transition should snap (no animation), and the breath pulse should NOT fire.
8. **Cycle completion dialog:** Let a focus cycle complete naturally → the completion dialog appears above the focus view; transition runs underneath when dismissed.

If any of these feel off (jank, mis-timed stagger, jumpy layout), tweak `FocusModeMotion` constants or the per-`AnimatedVisibility` `delayMillis` and re-run.

- [ ] **Step 7: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat(ui): animate focus mode transition with shared timer style and chrome stagger"
```

---

## Done

At this point:
- Entering focus mode is a 700ms staggered choreography: dock slides + fades, scrim deepens, timer morphs to hero, chrome collapses, FOCUS label settles in, hint fades in last, breath pulse signals "you're in".
- Exiting is a 400ms responsive reverse: hint and FOCUS fade fast, timer shrinks, dock slides back, chrome reappears.
- The countdown text never unmounts — its size, weight, color, and letter-spacing animate instead — so the timer stays smooth across the transition.
- Process restore snaps to the current state without animating, and the breath pulse only fires on user-initiated entries.
- All motion comes from one `FocusModeMotion` source of truth, pinned by unit tests.
