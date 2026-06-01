# Cycle Completion Dialog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a full Pomodoro cycle finishes (natural tick-down OR final skip), show an in-app non-cancelable `AlertDialog` with a randomized warm-and-affirming copy variant that the user must acknowledge with a single "Done" button before returning to the fresh idle state.

**Architecture:** A persistent boolean flag (`pendingCycleCompletionAck`) is written to DataStore by `TimerService.finishCycleAndStop(...)`. The root Compose function `KuroPomodoroApp` collects the flag as state and renders an `AlertDialog` overlay whenever it's `true`. Tapping "Done" clears the flag. Persistence handles the backgrounded case for free: if the user backgrounded the app when the cycle finished, the dialog appears the next time the composable is composed. The dialog renders one of five copy variants chosen via `remember(showFlag) { pool.random() }` so it is stable for the lifetime of a single display.

**Tech Stack:** Kotlin, Jetpack Compose Material3 (`AlertDialog`), AndroidX DataStore Preferences, JUnit4 + `kotlinx-coroutines-test` for repo tests.

**Spec:** `docs/superpowers/specs/2026-05-15-cycle-completion-dialog-design.md`

---

## File Structure

- `app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt` — add new `KEY_PENDING_CYCLE_ACK` `booleanPreferencesKey`, expose `pendingCycleCompletionAckFlow: Flow<Boolean>`, add `setPendingCycleCompletionAck(value: Boolean)` setter. Mirror existing patterns (e.g. `KEY_IS_PREMIUM` / `setIsPremium`).
- `app/src/main/java/com/endiar/kuropomodoro/timer/TimerService.kt` — inside `finishCycleAndStop(resetState)` (line 368), launch a coroutine on the existing service scope and call `repo.setPendingCycleCompletionAck(true)` before `stopSelf()`.
- `app/src/main/java/com/endiar/kuropomodoro/ui/CycleCompletionCopy.kt` — **new file** holding `data class CycleCompletionCopyVariant(val title: String, val body: String)` and `object CycleCompletionCopy { val variants: List<...>; val buttonLabel = "Done" }`.
- `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` — collect `pendingCycleCompletionAckFlow` at the top of `KuroPomodoroApp` (~line 86), pick a stable variant with `remember(pendingAck) { CycleCompletionCopy.variants.random() }`, render the `AlertDialog` inside the outer `Box` of the root surface (`KuroFlatSurface`) (insert just before line 352, after the `DashboardDock` block) so it overlays every screen. On confirm: launch coroutine that calls `repo.setPendingCycleCompletionAck(false)`.
- `app/src/test/java/com/endiar/kuropomodoro/data/AppRepositoryTest.kt` — add round-trip tests for the new flag (default false, set true persists, set false persists).
- `app/src/test/java/com/endiar/kuropomodoro/ui/CycleCompletionCopyTest.kt` — **new file** asserting the variant pool size, non-blank title/body, and fixed button label.

No changes to `TimerStateMachine`, `TimerReconciler`, `TimerNotification`, `TimerConnection`, or `MainActivity`.

---

## Task 1: Add `pendingCycleCompletionAck` to AppRepository

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt`
- Test: `app/src/test/java/com/endiar/kuropomodoro/data/AppRepositoryTest.kt`

- [ ] **Step 1: Write the failing test (default false + round-trip)**

Add to `AppRepositoryTest.kt`:

```kotlin
@Test
fun `pendingCycleCompletionAck defaults to false`() = runTest {
    val repo = makeRepo()
    assertEquals(false, repo.pendingCycleCompletionAckFlow.first())
}

@Test
fun `setPendingCycleCompletionAck true then false persists`() = runTest {
    val repo = makeRepo()
    repo.setPendingCycleCompletionAck(true)
    assertEquals(true, repo.pendingCycleCompletionAckFlow.first())
    repo.setPendingCycleCompletionAck(false)
    assertEquals(false, repo.pendingCycleCompletionAckFlow.first())
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests com.endiar.kuropomodoro.data.AppRepositoryTest`
Expected: FAIL — `pendingCycleCompletionAckFlow` and `setPendingCycleCompletionAck` unresolved.

- [ ] **Step 3: Implement flag flow + setter**

In `AppRepository.kt`, inside the `companion object` after `KEY_RUNTIME_SNAPSHOT` (line 78), add:

```kotlin
private val KEY_PENDING_CYCLE_ACK = booleanPreferencesKey("pending_cycle_completion_ack")
```

Just below `runtimeSnapshot` (after line 93), add:

```kotlin
val pendingCycleCompletionAckFlow: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_PENDING_CYCLE_ACK] ?: false
}
```

After `setIsPremium` (line 130), add:

```kotlin
suspend fun setPendingCycleCompletionAck(value: Boolean) =
    dataStore.edit { it[KEY_PENDING_CYCLE_ACK] = value }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests com.endiar.kuropomodoro.data.AppRepositoryTest`
Expected: PASS (all existing + 2 new tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt \
        app/src/test/java/com/endiar/kuropomodoro/data/AppRepositoryTest.kt
git commit -m "feat(data): persist pendingCycleCompletionAck flag"
```

---

## Task 2: Set the flag from `TimerService.finishCycleAndStop`

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/timer/TimerService.kt:368-377`

(No new unit test: `TimerService` is a real Android `Service` with `Context` dependencies; existing project tests do not cover it directly. Manual verification in Task 5 covers this.)

- [ ] **Step 1: Locate the helper**

Open `TimerService.kt`, find `private fun finishCycleAndStop(resetState: TimerRuntimeState)` at line 368.

- [ ] **Step 2: Mark flag before tearing down**

Replace the body of `finishCycleAndStop` with:

```kotlin
private suspend fun finishCycleAndStop(resetState: TimerRuntimeState) {
    NotificationManagerCompat.from(applicationContext).notify(
        TimerNotification.NOTIF_ID_COMPLETION + (System.currentTimeMillis() % 1000).toInt(),
        TimerNotification.buildCompletionNotification(applicationContext, TimerNotification.CompletionKind.Cycle)
    )
    repo.setPendingCycleCompletionAck(true)
    _state.value = resetState
    cancelAlarm()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
}
```

Note: the function must be `suspend` and the write must be **awaited** (not fire-and-forget on `scope.launch { ... }`). A launched child coroutine would be cancelled by `serviceJob.cancel()` in `onDestroy()` after `stopSelf()`, killing the DataStore write before it reaches disk. Both call sites (`doSkip` line 186 — already `suspend` — and the tick block line 260 — inside `scope.launch`) are already in suspend/coroutine contexts, so changing the signature requires no caller changes.

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, no regressions.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/timer/TimerService.kt
git commit -m "feat(timer): mark pendingCycleCompletionAck on cycle finish"
```

---

## Task 3: Define the copy pool

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/CycleCompletionCopy.kt`
- Test: `app/src/test/java/com/endiar/kuropomodoro/ui/CycleCompletionCopyTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/endiar/kuropomodoro/ui/CycleCompletionCopyTest.kt`:

```kotlin
package com.endiar.kuropomodoro.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleCompletionCopyTest {

    @Test
    fun `pool has exactly five variants`() {
        assertEquals(5, CycleCompletionCopy.variants.size)
    }

    @Test
    fun `every variant has non-blank title and body`() {
        CycleCompletionCopy.variants.forEach { v ->
            assertTrue("title blank for $v", v.title.isNotBlank())
            assertTrue("body blank for $v", v.body.isNotBlank())
        }
    }

    @Test
    fun `button label is Done`() {
        assertEquals("Done", CycleCompletionCopy.buttonLabel)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.endiar.kuropomodoro.ui.CycleCompletionCopyTest`
Expected: FAIL — `CycleCompletionCopy` unresolved.

- [ ] **Step 3: Create the copy file**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/CycleCompletionCopy.kt`:

```kotlin
package com.endiar.kuropomodoro.ui

data class CycleCompletionCopyVariant(
    val title: String,
    val body: String
)

object CycleCompletionCopy {
    const val buttonLabel: String = "Done"

    val variants: List<CycleCompletionCopyVariant> = listOf(
        CycleCompletionCopyVariant(
            title = "Cycle complete",
            body = "That's a full cycle behind you. Take a moment — you earned it."
        ),
        CycleCompletionCopyVariant(
            title = "Nicely done",
            body = "You finished a full Pomodoro cycle. Rest before the next one."
        ),
        CycleCompletionCopyVariant(
            title = "That's a wrap",
            body = "Four focused sessions, all done. Step away with a clear head."
        ),
        CycleCompletionCopyVariant(
            title = "Good work",
            body = "A complete cycle is no small thing. Take the win."
        ),
        CycleCompletionCopyVariant(
            title = "All done",
            body = "One full cycle behind you. Breathe, then come back when you're ready."
        )
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.endiar.kuropomodoro.ui.CycleCompletionCopyTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/CycleCompletionCopy.kt \
        app/src/test/java/com/endiar/kuropomodoro/ui/CycleCompletionCopyTest.kt
git commit -m "feat(ui): add cycle completion copy variants"
```

---

## Task 4: Render the AlertDialog in `KuroPomodoroApp`

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

(No automated UI test — project does not currently have Compose UI tests. Manual verification in Task 5.)

- [ ] **Step 1: Add imports**

At the top of `KuroPomodoroApp.kt`, ensure these imports exist (add any missing):

```kotlin
import androidx.compose.ui.window.DialogProperties
import com.endiar.kuropomodoro.ui.CycleCompletionCopy
```

`AlertDialog`, `Text`, `TextButton`, `RoundedCornerShape`, `Color`, `FontWeight`, `remember`, `rememberCoroutineScope`, `getValue`, `collectAsStateWithLifecycle`, and `kotlinx.coroutines.launch` are already imported.

- [ ] **Step 2: Collect the flag near the top of `KuroPomodoroApp`**

Right after the `runtimeState` line (currently line 118):

```kotlin
val runtimeState by timerConnection.state.collectAsStateWithLifecycle(initialValue = null)

val pendingCycleAck by repo.pendingCycleCompletionAckFlow
    .collectAsStateWithLifecycle(initialValue = false)
val cycleCompletionVariant = remember(pendingCycleAck) {
    if (pendingCycleAck) CycleCompletionCopy.variants.random() else null
}
val ackScope = rememberCoroutineScope()
```

- [ ] **Step 3: Insert the dialog overlay**

Inside the outer `Box(modifier = Modifier.fillMaxSize())` of the root surface (`KuroFlatSurface`) (the one that closes on line 352), append the dialog **after** the `DashboardDock` `if (...)` block (currently lines 338-351) and **before** the closing `}` on line 352:

```kotlin
            if (pendingCycleAck && cycleCompletionVariant != null) {
                AlertDialog(
                    onDismissRequest = { /* non-cancelable */ },
                    title = {
                        Text(
                            text = cycleCompletionVariant.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Text(
                            text = cycleCompletionVariant.body,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            ackScope.launch { repo.setPendingCycleCompletionAck(false) }
                        }) {
                            Text(
                                text = CycleCompletionCopy.buttonLabel,
                                color = Color(0xFFFF8B6F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    ),
                    containerColor = Color(0xFF2A2035),
                    shape = RoundedCornerShape(20.dp)
                )
            }
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, no regressions.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat(ui): render cycle completion AlertDialog"
```

---

## Task 5: Manual verification on device

**Files:** none

- [ ] **Step 1: Install the debug APK**

Ensure an emulator is running (e.g. `Pixel_7_Pro_API_34_CUST_1`), then:

Run: `./gradlew :app:installDebug`
Expected: `Installed on 1 device.`

- [ ] **Step 2: Verify natural-tick path**

In Profile → Debug (or via dev shortcut), set focus / break durations to the smallest available values so a full cycle completes quickly. Start the timer and let it tick through to the end of the final long break **without** skipping the last item. When the cycle ends:
- Foreground: dialog appears with one of the five title/body variants.
- "Done" is the only button.
- Back press and tapping outside do NOT dismiss the dialog.
- Tap "Done" → dialog disappears, app returns to fresh idle (cycle dots reset, timer ready to start).

- [ ] **Step 3: Verify final-skip path**

Restart the cycle. Skip every item including the final long break. When the final skip completes:
- Same dialog appears with a (possibly different) variant.
- Tap "Done" → dismissal works identically.

- [ ] **Step 4: Verify backgrounded-completion queueing**

Start a fresh cycle, send the app to background (Home button), let the cycle finish (system notification will fire). Re-open the app:
- Dialog appears immediately on the current screen (Timer, Todo, Calendar, or Profile — whichever was last selected).
- Tap "Done" → dismiss works.

- [ ] **Step 5: Verify process-death persistence**

Start a fresh cycle, background the app, force-stop the process via `adb shell am force-stop com.endiar.kuropomodoro` after the cycle would have completed (or trigger completion through normal flow then force-stop without acking). Re-launch:
- Dialog appears on launch.
- Tap "Done" → dismiss works.

- [ ] **Step 6: Confirm no regression to existing notification**

During each verification path above, confirm the system completion notification (`CompletionKind.Cycle`) still fires as before.

- [ ] **Step 7: Commit (only if any code tweaks were needed)**

If steps 2-6 surfaced fixes, commit them with descriptive messages. Otherwise, no commit needed for this task.

---

## Self-Review Checklist

- ✅ Spec section "Trigger" → Task 2 sets flag in `finishCycleAndStop` (covers both natural-completion and skip-to-end).
- ✅ Spec section "Persistence" → Task 1 writes flag to DataStore.
- ✅ Spec section "Display rules: foreground only, queue if backgrounded" → Composable lifecycle naturally satisfies; verified in Task 5 step 4.
- ✅ Spec section "Non-cancelable" → `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)` + empty `onDismissRequest` in Task 4 step 3.
- ✅ Spec section "Single Done button" → `confirmButton` only, no `dismissButton`, label from `CycleCompletionCopy.buttonLabel`.
- ✅ Spec section "5 random copy variants" → Task 3 defines exactly 5; Task 4 picks via `remember(pendingCycleAck) { ... .random() }` so the variant is stable for one display.
- ✅ No placeholders. All code blocks are complete.
- ✅ Type/method names consistent: `pendingCycleCompletionAckFlow`, `setPendingCycleCompletionAck`, `CycleCompletionCopy.variants`, `CycleCompletionCopy.buttonLabel`, `CycleCompletionCopyVariant(title, body)` used identically across all tasks.
