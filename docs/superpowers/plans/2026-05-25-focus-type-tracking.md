# Focus Type Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Store an explicit `FocusType` (Task, Habit, SimpleNote, Empty) on the active timer session so the homescreen can show the correct icon and label instead of the hardcoded `</>` and "Deep Work".

**Architecture:** Add a `FocusType` enum to `KuroModels.kt`, store it in `TimerRuntimeState`, persist it in the snapshot encoding as a new v4 field (no backward compatibility — invalid snapshots return null and the UI starts fresh). Wire the type through `StartFocusRequest` → `TimerService` → `TimerRuntimeState`, then use it in `HomeTimerScreen` to display the right icon and label.

**Tech Stack:** Kotlin, Jetpack Compose, Android Material3 icons

---

## Files

| File | Change |
|------|--------|
| `model/KuroModels.kt` | Add `FocusType` enum |
| `model/TimerRuntimeState.kt` | Add `focusType: FocusType` field |
| `ui/StartFocusSheet.kt` | Add `focusType` to `StartFocusRequest` |
| `timer/TimerService.kt` | Pass `focusType` into `TimerRuntimeState` |
| `timer/TimerController.kt` | Add `focusType` param to `start()` |
| `timer/TimerConnection.kt` | Thread `focusType` through `PendingStart` and `start()` |
| `data/AppRepository.kt` | Encode/decode `focusType` as v4 field |
| `PurrodoroApp.kt` | Pass `focusType` when calling `timerConnection.controller.start()` and pass it to `HomeTimerScreen` |
| `ui/HomeTimerScreen.kt` | Replace `CodeTile` + "Deep Work" with icon + type label derived from `focusType` |
| `test/.../AppRepositoryRuntimeSnapshotTest.kt` | Update existing tests, add focusType round-trip test |

---

### Task 1: Add `FocusType` enum to the model

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt`

- [ ] **Step 1: Add the enum**

Add this block after the existing `GlassTreatment` enum (around line 187):

```kotlin
enum class FocusType { Task, Habit, SimpleNote, Empty }
```

- [ ] **Step 2: Verify it compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt
git commit -m "feat: add FocusType enum to KuroModels"
```

---

### Task 2: Add `focusType` to `TimerRuntimeState`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/TimerRuntimeState.kt`

- [ ] **Step 1: Add the field**

Replace the entire file content with:

```kotlin
package com.endiar.purrodoro.model

/**
 * Runtime wrapper around [TimerSession] that the foreground service owns while a cycle is active.
 *
 * Invariants:
 *  - When [session].timerState == [TimerState.Playing], [endsAtMillis] is non-null.
 *  - While running, the displayed remaining time is derived from `endsAtMillis - now()`.
 *  - While paused, the displayed remaining time is read from [TimerSession.remainingSeconds].
 */
data class TimerRuntimeState(
    val session: TimerSession,
    val endsAtMillis: Long?,
    val activeTask: FocusTask?,
    val openRecord: SessionRecord?,
    val activeHabitId: Int? = null,
    val focusType: FocusType = FocusType.Empty,
) {
    val isActive: Boolean get() = session.timerState != TimerState.Idle
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL (existing callers that don't pass `focusType` will default to `FocusType.Empty`)

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/TimerRuntimeState.kt
git commit -m "feat: add focusType field to TimerRuntimeState"
```

---

### Task 3: Add `focusType` to `StartFocusRequest` and populate it

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/StartFocusSheet.kt`

- [ ] **Step 1: Add `focusType` to `StartFocusRequest`**

Find the existing `StartFocusRequest` data class (around line 77) and replace it:

```kotlin
data class StartFocusRequest(
    val task: FocusTask?,
    val habitId: Int? = null,
    val settings: TimerSettings,
    val focusType: FocusType = FocusType.Empty,
)
```

- [ ] **Step 2: Populate `focusType` in `buildStartFocusRequest`**

Find `buildStartFocusRequest` (around line 284) and replace the return expression:

```kotlin
): StartFocusRequest = when (intent) {
    FocusIntent.Empty -> StartFocusRequest(
        task = null,
        settings = settings,
        focusType = FocusType.Empty,
    )
    FocusIntent.SimpleNote -> StartFocusRequest(
        task = FocusTask(id = -1, title = noteText.trim(), minutes = 0),
        settings = settings,
        focusType = FocusType.SimpleNote,
    )
    FocusIntent.Task -> StartFocusRequest(
        task = tasks.first { it.id == selectedTaskId },
        settings = settings,
        focusType = FocusType.Task,
    )
    FocusIntent.Habit -> {
        val habit = todayHabits.first { it.id == selectedHabitId }
        val remaining = habit.effectiveTarget() - habit.progressOn(today)
        val displaySessions = remaining.coerceAtLeast(1)
        StartFocusRequest(
            task = FocusTask(
                id = -habit.id,
                title = "${habit.emoji} ${habit.name}",
                minutes = 0,
                plannedSessions = displaySessions,
                completedSessions = habit.progressOn(today),
            ),
            habitId = habit.id,
            settings = settings,
            focusType = FocusType.Habit,
        )
    }
}
```

- [ ] **Step 3: Add the missing import**

At the top of `StartFocusSheet.kt`, add:

```kotlin
import com.endiar.purrodoro.model.FocusType
```

- [ ] **Step 4: Verify it compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/StartFocusSheet.kt
git commit -m "feat: add focusType to StartFocusRequest"
```

---

### Task 4: Thread `focusType` through `TimerController` → `TimerConnection` → `TimerService`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerController.kt`
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerConnection.kt`
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerService.kt`

- [ ] **Step 1: Update `TimerController.kt`**

Replace the entire file:

```kotlin
package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.TimerSettings

interface TimerController {
    fun start(settings: TimerSettings, task: FocusTask?, activeHabitId: Int? = null, focusType: FocusType = FocusType.Empty)
    fun pause()
    fun resume()
    fun stop()
    fun skip()
}
```

- [ ] **Step 2: Update `TimerConnection.kt`**

Find the `PendingStart` data class and the `start()` override inside `TimerConnection`. Update both:

```kotlin
private data class PendingStart(
    val settings: TimerSettings,
    val task: FocusTask?,
    val activeHabitId: Int?,
    val focusType: FocusType,
)
```

```kotlin
override fun start(settings: TimerSettings, task: FocusTask?, activeHabitId: Int?, focusType: FocusType) {
    if (bound != null) {
        bound.start(settings, task, activeHabitId, focusType)
    } else {
        pendingStart = PendingStart(settings, task, activeHabitId, focusType)
    }
}
```

Also find the line where `pendingStart` is consumed (the `lb.controller.start(ps.settings, ps.task, ps.activeHabitId)` call) and update it:

```kotlin
lb.controller.start(ps.settings, ps.task, ps.activeHabitId, ps.focusType)
```

Add the missing import at the top of `TimerConnection.kt`:

```kotlin
import com.endiar.purrodoro.model.FocusType
```

- [ ] **Step 3: Update `TimerService.kt`**

Find the `start()` override inside `TimerService` (around line 49):

```kotlin
override fun start(settings: TimerSettings, task: FocusTask?, activeHabitId: Int?, focusType: FocusType) {
    scope.launch { startCycle(settings, task, activeHabitId, focusType) }
}
```

Find `startCycle` (around line 119) and update its signature and the `TimerRuntimeState` construction inside it:

```kotlin
private suspend fun startCycle(
    settings: TimerSettings,
    task: FocusTask?,
    activeHabitId: Int? = null,
    focusType: FocusType = FocusType.Empty,
) {
```

Inside `startCycle`, find where `TimerRuntimeState(...)` is constructed and add `focusType`:

```kotlin
val state = TimerRuntimeState(
    session = ...,         // keep existing fields
    endsAtMillis = ...,
    activeTask = task,
    openRecord = ...,
    activeHabitId = activeHabitId,
    focusType = focusType,
)
```

Add the missing import at the top of `TimerService.kt`:

```kotlin
import com.endiar.purrodoro.model.FocusType
```

- [ ] **Step 4: Verify it compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/
git commit -m "feat: thread focusType through TimerController/Connection/Service"
```

---

### Task 5: Update snapshot encode/decode (v4)

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt`
- Modify: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/data/AppRepositoryRuntimeSnapshotTest.kt`

- [ ] **Step 1: Write the failing test**

Open `AppRepositoryRuntimeSnapshotTest.kt`. Add a new test for the focusType round-trip:

```kotlin
@Test
fun encode_then_decode_roundTrips_focusType_habit() {
    val state = TimerRuntimeState(
        session = TimerSession(
            items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
            currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Playing
        ),
        endsAtMillis = 1_700_000_000_000L,
        activeTask = null, openRecord = null,
        activeHabitId = 3,
        focusType = FocusType.Habit,
    )
    val decoded = decodeRuntimeSnapshot(encodeRuntimeSnapshot(state))
    assertEquals(FocusType.Habit, decoded?.focusType)
}

@Test
fun encode_then_decode_roundTrips_focusType_task() {
    val state = TimerRuntimeState(
        session = TimerSession(
            items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
            currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Playing
        ),
        endsAtMillis = 1_700_000_000_000L,
        activeTask = FocusTask(id = 5, title = "Read docs", minutes = 25),
        openRecord = null,
        focusType = FocusType.Task,
    )
    val decoded = decodeRuntimeSnapshot(encodeRuntimeSnapshot(state))
    assertEquals(FocusType.Task, decoded?.focusType)
}
```

Also add the import at the top:

```kotlin
import com.endiar.purrodoro.model.FocusType
```

- [ ] **Step 2: Run the tests to confirm they fail**

```bash
cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests "*.AppRepositoryRuntimeSnapshotTest.encode_then_decode_roundTrips_focusType*"
```

Expected: FAIL (encode/decode don't know about focusType yet)

- [ ] **Step 3: Update `encodeRuntimeSnapshot` in `AppRepository.kt`**

Update the comment block above it:

```kotlin
// Schema (single line, pipe-separated):
//   v4 (9 fields): <timerState>|<currentIndex>|<remainingSeconds>|<endsAtMillisOrEmpty>|<itemsCsv>|<taskOrEmpty>|<openRecordOrEmpty>|<activeHabitIdOrEmpty>|<focusType>
//
// itemsCsv: comma-separated, each token is "F:<sec>", "S:<sec>", or "L:<sec>"
// taskOrEmpty: "<id>:<urlTitle>:<minutes>:<completed>:<plannedSessions>:<completedSessions>"
// openRecordOrEmpty: fields joined by ";" to avoid conflict with the outer "|" separator
// focusType: one of Task, Habit, SimpleNote, Empty
```

At the end of `encodeRuntimeSnapshot`, add `focusType` to the list:

```kotlin
return listOf(
    state.session.timerState.name,
    state.session.currentIndex,
    state.session.remainingSeconds,
    state.endsAtMillis ?: "",
    itemsCsv,
    taskField,
    recordField,
    state.activeHabitId?.toString() ?: "",
    state.focusType.name,
).joinToString("|")
```

- [ ] **Step 4: Update `decodeRuntimeSnapshot` in `AppRepository.kt`**

Change the `split` limit from 8 to 9:

```kotlin
val parts = raw.split("|", limit = 9)
```

Add focusType decoding before the `TimerRuntimeState(...)` construction:

```kotlin
val focusType = parts.getOrNull(8)
    ?.takeIf { it.isNotBlank() }
    ?.let { runCatching { FocusType.valueOf(it) }.getOrNull() }
    ?: FocusType.Empty
```

Add `focusType` to the `TimerRuntimeState(...)` construction:

```kotlin
TimerRuntimeState(
    session = TimerSession(items, currentIndex, remainingSeconds, timerState),
    endsAtMillis = endsAtMillis,
    activeTask = task,
    openRecord = record,
    activeHabitId = activeHabitId,
    focusType = focusType,
)
```

Add the import at the top of `AppRepository.kt`:

```kotlin
import com.endiar.purrodoro.model.FocusType
```

- [ ] **Step 5: Update existing snapshot round-trip tests that construct `TimerRuntimeState` directly**

In `AppRepositoryRuntimeSnapshotTest.kt`, the two existing round-trip tests construct `TimerRuntimeState` without `focusType`. Since `focusType` defaults to `FocusType.Empty`, the decoded state will have `focusType = FocusType.Empty` which should match. No change needed for those tests — but update the `activeHabitId` round-trip test to also assert focusType:

```kotlin
@Test
fun encode_then_decode_roundTrips_activeHabitId() {
    val state = TimerRuntimeState(
        session = TimerSession(
            items = listOf(TimerSessionItem(SessionItemType.Focus(1500))),
            currentIndex = 0, remainingSeconds = 1500, timerState = TimerState.Playing
        ),
        endsAtMillis = 1_700_000_000_000L,
        activeTask = null, openRecord = null,
        activeHabitId = 7,
        focusType = FocusType.Habit,
    )
    val decoded = decodeRuntimeSnapshot(encodeRuntimeSnapshot(state))
    assertEquals(7, decoded?.activeHabitId)
    assertEquals(FocusType.Habit, decoded?.focusType)
}
```

Also **remove** the `decode_v2_snapshot_without_activeHabitId_defaults_to_null` test — no backward compatibility needed.

- [ ] **Step 6: Run all snapshot tests**

```bash
cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests "*.AppRepositoryRuntimeSnapshotTest"
```

Expected: All pass

- [ ] **Step 7: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt
git add purrodoro-android/app/src/test/java/com/endiar/purrodoro/data/AppRepositoryRuntimeSnapshotTest.kt
git commit -m "feat: persist focusType in runtime snapshot (v4)"
```

---

### Task 6: Pass `focusType` from `PurrodoroApp` when starting the timer

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

- [ ] **Step 1: Find where `timerConnection.controller.start(...)` is called**

There are two call sites: one for a regular start focus request (around line 415) and one for a synthetic task start (around line 486). Both come from `onStartFocus`.

Find the `onStartFocus` lambda. It receives a `StartFocusRequest`. Update the `controller.start(...)` calls to pass `request.focusType`:

```kotlin
timerConnection.controller.start(
    settings = request.settings,
    task = request.task,
    activeHabitId = request.habitId,
    focusType = request.focusType,
)
```

Apply this pattern to **every** call site that calls `controller.start(...)` in `PurrodoroApp.kt`.

- [ ] **Step 2: Expose `focusType` to `HomeTimerScreen`**

Find where the runtime state is observed (look for `runtimeState` or `timerConnection.state`). The `focusType` is already in `TimerRuntimeState` — just pass it to `HomeTimerScreen`.

Find the `HomeTimerScreen(...)` call (around line 346) and add:

```kotlin
activeFocusType = runtimeState?.focusType ?: FocusType.Empty,
```

Add the import at the top of `PurrodoroApp.kt`:

```kotlin
import com.endiar.purrodoro.model.FocusType
```

- [ ] **Step 3: Verify it compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL (HomeTimerScreen will error until Task 7)

- [ ] **Step 4: Commit after Task 7 is done** (defer — HomeTimerScreen not updated yet)

---

### Task 7: Update `HomeTimerScreen` to show icon + focus type label

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/HomeTimerScreen.kt`

- [ ] **Step 1: Add `activeFocusType` parameter to `HomeTimerScreen`**

Add the parameter to the function signature (around line 72):

```kotlin
activeFocusType: FocusType = FocusType.Empty,
```

- [ ] **Step 2: Thread `activeFocusType` down to `ActiveHero` and `ActiveTimerCard`**

In `HomeTimerScreen`, pass it to `ActiveHero`:

```kotlin
ActiveHero(
    ...,
    activeFocusType = activeFocusType,
    ...
)
```

In `ActiveHero`, add the parameter and pass it to `ActiveTimerCard`:

```kotlin
@Composable
private fun ActiveHero(
    ...,
    activeFocusType: FocusType,
    ...
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HeroImage(modifier = Modifier.fillMaxWidth(), time = backdropTime)
        ActiveTimerCard(
            ...,
            activeFocusType = activeFocusType,
            ...
        )
    }
}
```

In `ActiveTimerCard`, add the parameter:

```kotlin
@Composable
private fun ActiveTimerCard(
    ...,
    activeFocusType: FocusType,
    ...
)
```

- [ ] **Step 3: Replace `CodeTile()` + "Deep Work" with `FocusTypeIcon` + focus type label**

Inside `ActiveTimerCard`, find the `Row` block that has `CodeTile()` (around line 300). Replace:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    FocusTypeIcon(focusType = activeFocusType)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = activeTask?.title ?: timerMode.label,
            color = tokens.textPrimary,
            style = KuroType.headingCard,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = focusTypeLabel(activeFocusType),
            color = tokens.textSecondary,
            style = KuroType.labelStrong
        )
    }
    Text(
        text = "✎ Edit",
        color = tokens.accent,
        style = KuroType.labelStrong
    )
}
```

- [ ] **Step 4: Add `FocusTypeIcon` composable**

Replace the existing `CodeTile` composable (around line 609) with `FocusTypeIcon`:

```kotlin
@Composable
private fun FocusTypeIcon(focusType: FocusType, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val icon = when (focusType) {
        FocusType.Task -> Icons.Outlined.TaskAlt
        FocusType.Habit -> Icons.Outlined.Spa
        FocusType.SimpleNote -> Icons.Outlined.EditNote
        FocusType.Empty -> Icons.Outlined.CropSquare
    }
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(22.dp)
        )
    }
}
```

- [ ] **Step 5: Add `focusTypeLabel` helper**

Add near `FocusTypeIcon`:

```kotlin
private fun focusTypeLabel(focusType: FocusType): String = when (focusType) {
    FocusType.Task -> "Task"
    FocusType.Habit -> "Habit"
    FocusType.SimpleNote -> "Simple Note"
    FocusType.Empty -> "Focus"
}
```

- [ ] **Step 6: Add missing imports to `HomeTimerScreen.kt`**

```kotlin
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TaskAlt
import com.endiar.purrodoro.model.FocusType
```

Remove the now-unused `import androidx.compose.foundation.Image` if `CodeTile` was the only user (check first).

- [ ] **Step 7: Verify it compiles**

```bash
cd purrodoro-android && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit Tasks 6 + 7 together**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/HomeTimerScreen.kt
git commit -m "feat: show focus type icon and label in active timer card"
```

---

### Task 8: Run all unit tests

- [ ] **Step 1: Run the full test suite**

```bash
cd purrodoro-android && ./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, 0 failures

- [ ] **Step 2: If any failures, fix them before continuing**

Common failures to look for:
- Tests that construct `TimerRuntimeState` without `focusType` — these will still compile (defaults to `FocusType.Empty`) but assertEquals checks may fail if the encoded snapshot no longer round-trips for old tests
- Tests that construct `StartFocusRequest` without `focusType` — will default to `FocusType.Empty`, should be fine

- [ ] **Step 3: Commit any test fixes**

```bash
git add -A
git commit -m "fix: update tests for focusType changes"
```
