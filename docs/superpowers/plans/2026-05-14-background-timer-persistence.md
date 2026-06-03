# Background Timer Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Pomodoro timer truly run in the background via a foreground service, with a live-updating ongoing notification (Pause / Resume / Stop / Skip actions), `AlarmManager` backstop, and deterministic state reconciliation on app reopen.

**Architecture:** A new `com.endiar.purrodoro.timer/` package introduces a foreground service (`TimerService`) that owns runtime state. The existing-but-unused `TimerSessionReducer` becomes the single source of state transitions. UI binds to the service via `TimerConnection` and observes a `StateFlow<TimerRuntimeState>`. A pure `TimerReconciler` function restores state on app reopen by replaying segment completions against the wall clock. An `AlarmManager` exact alarm acts as a backstop in case the service is killed.

**Tech Stack:** Kotlin, Jetpack Compose, Android `Service` + `BroadcastReceiver`, `AlarmManager`, `NotificationCompat`, AndroidX DataStore Preferences, JUnit 4, kotlinx-coroutines-test, `java.time`.

---

## File Map

**New files:**
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/TimerRuntimeState.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerReconciler.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerStateMachine.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerNotification.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerAlarmReceiver.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerService.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerController.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerConnection.kt`
- `purrodoro-android/app/src/test/java/com/endiar/purrodoro/model/TimerSessionReducerTest.kt`
- `purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerReconcilerTest.kt`
- `purrodoro-android/app/src/test/java/com/endiar/purrodoro/data/AppRepositoryRuntimeSnapshotTest.kt`

**Modified files:**
- `purrodoro-android/app/src/main/AndroidManifest.xml`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/MainActivity.kt`
- `purrodoro-android/app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt`

---

## Task 1: ~~Lock down `TimerSessionReducer` behavior with unit tests~~ — **ALREADY SATISFIED**

> **Status (2026-05-14, post-baseline-review):** Pre-existing `TimerSessionReducerTest.kt` already comprehensively covers the reducer with 19 tests, including all behaviors this task intended to lock down. The plan's exploration was stale — it assumed the test directory was empty. **Skip this task.** No code action; mark complete in the task tracker.

The reducer already exists in `model/TimerSessionReducer.kt` but is not called anywhere. Before wiring it in, write tests so refactors don't silently change behavior.

**Files:**
- ~~Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/model/TimerSessionReducerTest.kt`~~ — already exists.

- [ ] **Step 1: Create the test file with all behavior cases**

```kotlin
package com.endiar.purrodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TimerSessionReducerTest {

    private val defaultSettings = TimerSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        autoStartNextSession = false,
        focusSessionsPerCycle = 2
    )
    private val today: LocalDate = LocalDate.of(2026, 5, 14)
    private val emptyStamps: Map<LocalDate, FocusStamp> = emptyMap()

    private fun freshSession(settings: TimerSettings = defaultSettings) = buildSession(settings)

    @Test
    fun start_transitions_idle_to_playing() {
        val initial = freshSession()
        val result = reduce(initial, TimerAction.Start, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
        assertEquals(initial.currentIndex, result.session.currentIndex)
        assertEquals(initial.remainingSeconds, result.session.remainingSeconds)
        assertEquals(emptyStamps, result.stamps)
    }

    @Test
    fun pause_transitions_playing_to_paused() {
        val playing = freshSession().copy(timerState = TimerState.Playing)
        val result = reduce(playing, TimerAction.Pause, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Paused, result.session.timerState)
    }

    @Test
    fun resume_transitions_paused_to_playing() {
        val paused = freshSession().copy(timerState = TimerState.Paused)
        val result = reduce(paused, TimerAction.Resume, defaultSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
    }

    @Test
    fun stop_during_focus_with_remaining_time_adds_unfinished_stamp() {
        val midFocus = freshSession().copy(
            timerState = TimerState.Playing,
            remainingSeconds = 600 // 10 minutes left in Focus
        )
        val result = reduce(midFocus, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.unfinished)
        assertEquals(0, result.stamps[today]?.completed)
        // Stop also resets to a fresh session
        assertEquals(0, result.session.currentIndex)
        assertEquals(TimerState.Idle, result.session.timerState)
    }

    @Test
    fun stop_during_break_does_not_add_unfinished_stamp() {
        val midBreak = freshSession().copy(
            currentIndex = 1, // ShortBreak position in [F, S, F, L]
            timerState = TimerState.Playing,
            remainingSeconds = 100
        )
        val result = reduce(midBreak, TimerAction.Stop, defaultSettings, emptyStamps, today)
        assertEquals(emptyStamps, result.stamps)
    }

    @Test
    fun skip_during_focus_with_remaining_time_adds_unfinished_stamp_and_advances() {
        val midFocus = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 600)
        val result = reduce(midFocus, TimerAction.Skip, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.unfinished)
        assertEquals(1, result.session.currentIndex)
    }

    @Test
    fun skip_during_break_advances_without_stamp() {
        val midBreak = freshSession().copy(
            currentIndex = 1,
            timerState = TimerState.Playing,
            remainingSeconds = 100
        )
        val result = reduce(midBreak, TimerAction.Skip, defaultSettings, emptyStamps, today)
        assertEquals(2, result.session.currentIndex)
        assertEquals(emptyStamps, result.stamps)
    }

    @Test
    fun timerComplete_on_focus_adds_completed_stamp_and_advances() {
        val endingFocus = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 0)
        val result = reduce(endingFocus, TimerAction.TimerComplete, defaultSettings, emptyStamps, today)
        assertEquals(1, result.stamps[today]?.completed)
        assertEquals(1, result.session.currentIndex)
        // No auto-advance → next state is Paused
        assertEquals(TimerState.Paused, result.session.timerState)
    }

    @Test
    fun timerComplete_with_autoStart_keeps_playing_after_advance() {
        val autoSettings = defaultSettings.copy(autoStartNextSession = true)
        val endingFocus = buildSession(autoSettings).copy(timerState = TimerState.Playing, remainingSeconds = 0)
        val result = reduce(endingFocus, TimerAction.TimerComplete, autoSettings, emptyStamps, today)
        assertEquals(TimerState.Playing, result.session.timerState)
    }

    @Test
    fun timerComplete_on_last_item_resets_to_fresh_session() {
        val lastItemEnding = freshSession().let { initial ->
            initial.copy(
                currentIndex = initial.items.size - 1, // last (LongBreak)
                timerState = TimerState.Playing,
                remainingSeconds = 0
            )
        }
        val result = reduce(lastItemEnding, TimerAction.TimerComplete, defaultSettings, emptyStamps, today)
        assertEquals(0, result.session.currentIndex)
        assertEquals(TimerState.Idle, result.session.timerState)
    }

    @Test
    fun tick_decrements_remaining_seconds() {
        val playing = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 100)
        val result = reduce(playing, TimerAction.Tick(elapsed = 3), defaultSettings, emptyStamps, today)
        assertEquals(97, result.session.remainingSeconds)
    }

    @Test
    fun tick_clamps_remaining_seconds_at_zero() {
        val playing = freshSession().copy(timerState = TimerState.Playing, remainingSeconds = 2)
        val result = reduce(playing, TimerAction.Tick(elapsed = 10), defaultSettings, emptyStamps, today)
        assertEquals(0, result.session.remainingSeconds)
    }
}
```

- [ ] **Step 2: Run the tests to verify they pass against the existing reducer**

```bash
cd purrodoro-android && ./gradlew test --tests "com.endiar.purrodoro.model.TimerSessionReducerTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 11 tests pass.

If any test fails, the reducer has a behavior the spec didn't anticipate. Read the failure, decide whether the test or the reducer is wrong, fix the test (preferred — keep reducer behavior stable), and re-run.

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/test/java/com/endiar/purrodoro/model/TimerSessionReducerTest.kt
git commit -m "test: lock down TimerSessionReducer behavior"
```

---

## Task 2: Add `TimerRuntimeState` runtime wrapper model

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/TimerRuntimeState.kt`

- [ ] **Step 1: Create the runtime state model**

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
    val openRecord: SessionRecord?
) {
    val isActive: Boolean get() = session.timerState != TimerState.Idle
}
```

- [ ] **Step 2: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/TimerRuntimeState.kt
git commit -m "feat: add TimerRuntimeState model"
```

---

## Task 3: Add `TimerReconciler` pure function with unit tests (TDD)

The reconciler is the heart of "the timer feels correct after I reopen the app." It's a pure function; we drive it with tests first.

**Files:**
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerReconcilerTest.kt`
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerReconciler.kt`

- [ ] **Step 1: Write the failing tests first**

```kotlin
package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.buildCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TimerReconcilerTest {

    private val noAutoSettings = TimerSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        autoStartNextSession = false,
        focusSessionsPerCycle = 2
    )
    private val autoSettings = noAutoSettings.copy(autoStartNextSession = true)

    private fun playingSnapshot(
        startMillis: Long,
        settings: TimerSettings,
        atIndex: Int = 0
    ): TimerRuntimeState {
        val items = buildCycle(settings)
        val seconds = items[atIndex].type.let {
            when (it) {
                is SessionItemType.Focus -> it.seconds
                is SessionItemType.ShortBreak -> it.seconds
                is SessionItemType.LongBreak -> it.seconds
            }
        }
        return TimerRuntimeState(
            session = TimerSession(
                items = items,
                currentIndex = atIndex,
                remainingSeconds = seconds,
                timerState = TimerState.Playing
            ),
            endsAtMillis = startMillis + seconds * 1000L,
            activeTask = null,
            openRecord = SessionRecord(
                id = startMillis,
                startedAt = startMillis,
                endedAt = 0L,
                focusMinutes = settings.focusMinutes,
                shortBreakMinutes = settings.shortBreakMinutes,
                longBreakMinutes = settings.longBreakMinutes,
                completedFocusCount = 0,
                totalFocusCount = items.count { it.type is SessionItemType.Focus },
                status = SessionStatus.Partial,
                taskName = null
            )
        )
    }

    private fun zonedDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    @Test
    fun mid_segment_no_advance_returns_state_unchanged() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, noAutoSettings)
        val now = startMillis + 5 * 60 * 1000L // 5 minutes in, 20 left
        val result = TimerReconciler.reconcile(snapshot, now, noAutoSettings)

        assertEquals(snapshot.session.currentIndex, result.state.session.currentIndex)
        assertEquals(snapshot.endsAtMillis, result.state.endsAtMillis)
        assertEquals(TimerState.Playing, result.state.session.timerState)
        assertTrue(result.stampsToIncrement.isEmpty())
        assertTrue(result.closedRecords.isEmpty())
    }

    @Test
    fun after_one_focus_ends_no_autoStart_lands_paused_on_next_segment() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, noAutoSettings) // Focus #1 ends at +25min
        val now = startMillis + 26 * 60 * 1000L // 26 min in — Focus completed 1 min ago

        val result = TimerReconciler.reconcile(snapshot, now, noAutoSettings)

        assertEquals(1, result.state.session.currentIndex) // ShortBreak
        assertEquals(TimerState.Paused, result.state.session.timerState)
        assertNull(result.state.endsAtMillis)
        assertEquals(listOf(zonedDate(snapshot.endsAtMillis!!)), result.stampsToIncrement)
        assertTrue(result.closedRecords.isEmpty())
        assertEquals(1, result.state.openRecord?.completedFocusCount)
    }

    @Test
    fun after_one_focus_ends_with_autoStart_continues_into_break() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, autoSettings)
        val now = startMillis + 26 * 60 * 1000L

        val result = TimerReconciler.reconcile(snapshot, now, autoSettings)

        assertEquals(1, result.state.session.currentIndex)
        assertEquals(TimerState.Playing, result.state.session.timerState)
        assertNotNull(result.state.endsAtMillis)
        // Break ends 25min + 5min after start = at startMillis + 30min
        assertEquals(startMillis + 30 * 60 * 1000L, result.state.endsAtMillis)
    }

    @Test
    fun after_multiple_segments_with_autoStart_advances_through_chain() {
        // 2-focus cycle: [Focus 25m, ShortBreak 5m, Focus 25m, LongBreak 15m] = 70m total
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, autoSettings)
        val now = startMillis + 32 * 60 * 1000L // mid-second-Focus (Focus#1 + break done, 2min into Focus#2)

        val result = TimerReconciler.reconcile(snapshot, now, autoSettings)

        assertEquals(2, result.state.session.currentIndex) // second Focus
        assertEquals(TimerState.Playing, result.state.session.timerState)
        // Focus#2 ends at startMillis + 25m + 5m + 25m = +55m
        assertEquals(startMillis + 55 * 60 * 1000L, result.state.endsAtMillis)
        // One Focus completion only (Focus#1); Focus#2 still in progress
        assertEquals(1, result.stampsToIncrement.size)
        assertEquals(1, result.state.openRecord?.completedFocusCount)
    }

    @Test
    fun after_full_cycle_ends_returns_idle_and_closes_record() {
        val startMillis = 1_700_000_000_000L
        val snapshot = playingSnapshot(startMillis, autoSettings)
        // Cycle total: 25 + 5 + 25 + 15 = 70 minutes
        val now = startMillis + 80 * 60 * 1000L

        val result = TimerReconciler.reconcile(snapshot, now, autoSettings)

        assertEquals(TimerState.Idle, result.state.session.timerState)
        assertNull(result.state.endsAtMillis)
        assertNull(result.state.openRecord)
        assertEquals(2, result.stampsToIncrement.size) // both Focus segments
        assertEquals(1, result.closedRecords.size)
        assertEquals(SessionStatus.Completed, result.closedRecords[0].status)
        assertEquals(2, result.closedRecords[0].completedFocusCount)
    }

    @Test
    fun paused_snapshot_is_returned_unchanged() {
        val snapshot = playingSnapshot(0L, noAutoSettings).let {
            it.copy(
                session = it.session.copy(timerState = TimerState.Paused),
                endsAtMillis = null
            )
        }
        val result = TimerReconciler.reconcile(snapshot, now = 999_999_999L, noAutoSettings)
        assertEquals(snapshot, result.state)
        assertTrue(result.stampsToIncrement.isEmpty())
        assertTrue(result.closedRecords.isEmpty())
    }
}
```

- [ ] **Step 2: Run tests — they should fail (no `TimerReconciler` yet)**

```bash
cd purrodoro-android && ./gradlew test --tests "com.endiar.purrodoro.timer.TimerReconcilerTest" 2>&1 | tail -20
```

Expected: compile error — `Unresolved reference: TimerReconciler`.

- [ ] **Step 3: Implement `TimerReconciler`**

```kotlin
package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReconcileResult(
    val state: TimerRuntimeState,
    val stampsToIncrement: List<LocalDate>,
    val closedRecords: List<SessionRecord>
)

object TimerReconciler {

    /**
     * Replay segment completions against the wall clock to produce the correct
     * [TimerRuntimeState] for "right now". Pure function — no Android dependencies.
     */
    fun reconcile(
        snapshot: TimerRuntimeState,
        now: Long,
        settings: TimerSettings,
        zone: ZoneId = ZoneId.systemDefault()
    ): ReconcileResult {
        if (snapshot.session.timerState != TimerState.Playing || snapshot.endsAtMillis == null) {
            return ReconcileResult(snapshot, emptyList(), emptyList())
        }

        var session = snapshot.session
        var endsAt: Long = snapshot.endsAtMillis
        var openRecord = snapshot.openRecord
        val stamps = mutableListOf<LocalDate>()
        val closed = mutableListOf<SessionRecord>()

        while (now >= endsAt) {
            val currentItem = session.items.getOrNull(session.currentIndex) ?: break

            if (currentItem.type is SessionItemType.Focus) {
                stamps.add(Instant.ofEpochMilli(endsAt).atZone(zone).toLocalDate())
                openRecord = openRecord?.copy(
                    completedFocusCount = openRecord.completedFocusCount + 1
                )
            }

            val nextIndex = session.currentIndex + 1
            if (nextIndex >= session.items.size) {
                // Cycle complete
                openRecord?.let { rec ->
                    closed.add(
                        rec.copy(
                            endedAt = endsAt,
                            status = SessionStatus.Completed
                        )
                    )
                }
                return ReconcileResult(
                    state = snapshot.copy(
                        session = session.copy(
                            currentIndex = 0,
                            remainingSeconds = session.items.first().type.seconds(),
                            timerState = TimerState.Idle
                        ),
                        endsAtMillis = null,
                        openRecord = null
                    ),
                    stampsToIncrement = stamps,
                    closedRecords = closed
                )
            }

            val nextItem = session.items[nextIndex]
            val nextSeconds = nextItem.type.seconds()
            session = session.copy(
                currentIndex = nextIndex,
                remainingSeconds = nextSeconds,
                timerState = if (settings.autoStartNextSession) TimerState.Playing else TimerState.Paused
            )

            if (settings.autoStartNextSession) {
                endsAt += nextSeconds * 1000L
            } else {
                return ReconcileResult(
                    state = snapshot.copy(
                        session = session,
                        endsAtMillis = null,
                        openRecord = openRecord
                    ),
                    stampsToIncrement = stamps,
                    closedRecords = closed
                )
            }
        }

        // Still in current segment; recompute remainingSeconds from endsAt
        val newRemaining = ((endsAt - now) / 1000L).toInt().coerceAtLeast(0)
        return ReconcileResult(
            state = snapshot.copy(
                session = session.copy(remainingSeconds = newRemaining),
                endsAtMillis = endsAt,
                openRecord = openRecord
            ),
            stampsToIncrement = stamps,
            closedRecords = closed
        )
    }

    private fun SessionItemType.seconds(): Int = when (this) {
        is SessionItemType.Focus -> seconds
        is SessionItemType.ShortBreak -> seconds
        is SessionItemType.LongBreak -> seconds
    }
}
```

- [ ] **Step 4: Run tests again — all should pass**

```bash
cd purrodoro-android && ./gradlew test --tests "com.endiar.purrodoro.timer.TimerReconcilerTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerReconcilerTest.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerReconciler.kt
git commit -m "feat: add TimerReconciler pure function with tests"
```

---

## Task 4: Persist `TimerRuntimeState` snapshot in `AppRepository`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt`
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/data/AppRepositoryRuntimeSnapshotTest.kt`

- [ ] **Step 1: Add the encode/decode helpers + round-trip test (TDD)**

Create the test file first:

```kotlin
package com.endiar.purrodoro.data

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppRepositoryRuntimeSnapshotTest {

    @Test
    fun encode_then_decode_roundTrips_a_running_state() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(
                    TimerSessionItem(SessionItemType.Focus(1500)),
                    TimerSessionItem(SessionItemType.ShortBreak(300)),
                    TimerSessionItem(SessionItemType.Focus(1500)),
                    TimerSessionItem(SessionItemType.LongBreak(900))
                ),
                currentIndex = 2,
                remainingSeconds = 1490,
                timerState = TimerState.Playing
            ),
            endsAtMillis = 1_700_000_000_000L,
            activeTask = FocusTask(id = 42, title = "Sketch with cat", minutes = 25),
            openRecord = SessionRecord(
                id = 1_700_000_000_000L,
                startedAt = 1_699_999_000_000L,
                endedAt = 0L,
                focusMinutes = 25,
                shortBreakMinutes = 5,
                longBreakMinutes = 15,
                completedFocusCount = 1,
                totalFocusCount = 2,
                status = SessionStatus.Partial,
                taskName = "Sketch with cat"
            )
        )
        val encoded = encodeRuntimeSnapshot(state)
        val decoded = decodeRuntimeSnapshot(encoded)
        assertEquals(state, decoded)
    }

    @Test
    fun encode_then_decode_roundTrips_a_paused_state_without_endsAtMillis() {
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(
                    TimerSessionItem(SessionItemType.Focus(1500)),
                    TimerSessionItem(SessionItemType.LongBreak(900))
                ),
                currentIndex = 0,
                remainingSeconds = 600,
                timerState = TimerState.Paused
            ),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null
        )
        val encoded = encodeRuntimeSnapshot(state)
        val decoded = decodeRuntimeSnapshot(encoded)
        assertEquals(state, decoded)
    }

    @Test
    fun decode_blank_returns_null() {
        assertNull(decodeRuntimeSnapshot(""))
    }

    @Test
    fun decode_garbage_returns_null() {
        assertNull(decodeRuntimeSnapshot("this is not a valid snapshot"))
    }
}
```

- [ ] **Step 2: Run test — should fail (helpers don't exist)**

```bash
cd purrodoro-android && ./gradlew test --tests "com.endiar.purrodoro.data.AppRepositoryRuntimeSnapshotTest" 2>&1 | tail -20
```

Expected: `Unresolved reference: encodeRuntimeSnapshot`.

- [ ] **Step 3: Add encode/decode helpers + key + getter/setter to `AppRepository.kt`**

In `AppRepository.kt`, add imports at the top:

```kotlin
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.FocusTask
```

Add a new key inside the companion object (after `KEY_SESSION_RECORDS`):

```kotlin
private val KEY_RUNTIME_SNAPSHOT = stringPreferencesKey("timer_runtime_snapshot")
```

Add a new flow on the class (after the existing `appState` flow):

```kotlin
val runtimeSnapshot: Flow<TimerRuntimeState?> = dataStore.data.map { prefs ->
    decodeRuntimeSnapshot(prefs[KEY_RUNTIME_SNAPSHOT] ?: "")
}
```

Add new write methods (after `appendSessionRecord`):

```kotlin
suspend fun setRuntimeSnapshot(snapshot: TimerRuntimeState?) = dataStore.edit { prefs ->
    if (snapshot == null) {
        prefs.remove(KEY_RUNTIME_SNAPSHOT)
    } else {
        prefs[KEY_RUNTIME_SNAPSHOT] = encodeRuntimeSnapshot(snapshot)
    }
}

suspend fun incrementStampFor(date: java.time.LocalDate) = dataStore.edit { prefs ->
    val existing = decodeStamps(prefs[KEY_STAMPS] ?: "")
    val updated = existing + (date to (existing[date] ?: 0) + 1)
    prefs[KEY_STAMPS] = encodeStamps(updated)
}
```

Add encode/decode helpers at the bottom of the file (after `decodeSessionRecords`):

```kotlin
// --- Runtime snapshot encoding ---
// Schema (single line, pipe-separated):
// <timerState>|<currentIndex>|<remainingSeconds>|<endsAtMillisOrEmpty>|<itemsCsv>|<taskOrEmpty>|<openRecordOrEmpty>
// itemsCsv: comma-separated, each item "F:1500" / "S:300" / "L:900"
// taskOrEmpty: "<id>:<urlencodedTitle>:<minutes>:<completed>:<planned>:<doneSessions>"
// openRecordOrEmpty: same fields as the existing session-record encoding, pipe-separated isn't usable inside —
//                    so the openRecord uses ';' as its field separator.

internal fun encodeRuntimeSnapshot(state: TimerRuntimeState): String {
    val itemsCsv = state.session.items.joinToString(",") { item ->
        when (val t = item.type) {
            is SessionItemType.Focus -> "F:${t.seconds}"
            is SessionItemType.ShortBreak -> "S:${t.seconds}"
            is SessionItemType.LongBreak -> "L:${t.seconds}"
        }
    }
    val taskField = state.activeTask?.let { t ->
        val encodedTitle = URLEncoder.encode(t.title, "UTF-8")
        "${t.id}:$encodedTitle:${t.minutes}:${t.completed}:${t.plannedSessions}:${t.completedSessions}"
    } ?: ""
    val recordField = state.openRecord?.let { r ->
        val encodedTask = if (r.taskName != null) URLEncoder.encode(r.taskName, "UTF-8") else ""
        listOf(
            r.id, r.startedAt, r.endedAt,
            r.focusMinutes, r.shortBreakMinutes, r.longBreakMinutes,
            r.completedFocusCount, r.totalFocusCount,
            r.status.name, encodedTask
        ).joinToString(";")
    } ?: ""
    return listOf(
        state.session.timerState.name,
        state.session.currentIndex,
        state.session.remainingSeconds,
        state.endsAtMillis ?: "",
        itemsCsv,
        taskField,
        recordField
    ).joinToString("|")
}

internal fun decodeRuntimeSnapshot(raw: String): TimerRuntimeState? {
    if (raw.isBlank()) return null
    return runCatching {
        val parts = raw.split("|", limit = 7)
        val timerState = TimerState.valueOf(parts[0])
        val currentIndex = parts[1].toInt()
        val remainingSeconds = parts[2].toInt()
        val endsAtMillis = parts[3].takeIf { it.isNotBlank() }?.toLong()
        val items = parts[4].split(",").map { token ->
            val (tag, sec) = token.split(":")
            val seconds = sec.toInt()
            TimerSessionItem(
                when (tag) {
                    "S" -> SessionItemType.ShortBreak(seconds)
                    "L" -> SessionItemType.LongBreak(seconds)
                    else -> SessionItemType.Focus(seconds)
                }
            )
        }
        val task = parts.getOrNull(5)?.takeIf { it.isNotBlank() }?.let { t ->
            val tp = t.split(":", limit = 6)
            FocusTask(
                id = tp[0].toInt(),
                title = URLDecoder.decode(tp[1], "UTF-8"),
                minutes = tp[2].toInt(),
                completed = tp[3].toBoolean(),
                plannedSessions = tp[4].toInt(),
                completedSessions = tp[5].toInt()
            )
        }
        val record = parts.getOrNull(6)?.takeIf { it.isNotBlank() }?.let { r ->
            val rp = r.split(";")
            if (rp.size < 10) return@let null
            com.endiar.purrodoro.model.SessionRecord(
                id = rp[0].toLong(),
                startedAt = rp[1].toLong(),
                endedAt = rp[2].toLong(),
                focusMinutes = rp[3].toInt(),
                shortBreakMinutes = rp[4].toInt(),
                longBreakMinutes = rp[5].toInt(),
                completedFocusCount = rp[6].toInt(),
                totalFocusCount = rp[7].toInt(),
                status = com.endiar.purrodoro.model.SessionStatus.valueOf(rp[8]),
                taskName = if (rp[9].isBlank()) null else URLDecoder.decode(rp[9], "UTF-8")
            )
        }
        TimerRuntimeState(
            session = TimerSession(items, currentIndex, remainingSeconds, timerState),
            endsAtMillis = endsAtMillis,
            activeTask = task,
            openRecord = record
        )
    }.getOrNull()
}
```

- [ ] **Step 4: Run tests — all should pass now**

```bash
cd purrodoro-android && ./gradlew test --tests "com.endiar.purrodoro.data.AppRepositoryRuntimeSnapshotTest" 2>&1 | tail -20
```

Expected: 4 tests pass.

- [ ] **Step 5: Build app to ensure nothing else broke**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt \
        purrodoro-android/app/src/test/java/com/endiar/purrodoro/data/AppRepositoryRuntimeSnapshotTest.kt
git commit -m "feat: persist TimerRuntimeState snapshot in AppRepository"
```

---

## Task 5: Add manifest entries for service, receiver, and permissions

**Files:**
- Modify: `purrodoro-android/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Replace the manifest contents**

Replace the entire `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.KuroPomodoro">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.KuroPomodoro">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".timer.TimerService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="pomodoro_focus_timer" />
        </service>

        <receiver
            android:name=".timer.TimerAlarmReceiver"
            android:exported="false" />

    </application>

</manifest>
```

- [ ] **Step 2: Build — will reference unresolved `.timer.TimerService` and `.timer.TimerAlarmReceiver` but manifest parsing is lazy**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. The manifest references classes that don't yet exist, but AGP only validates components when they're loaded at install time — not at build time. If the build fails citing missing classes, stub the classes with empty `Service()` / `BroadcastReceiver()` shells (these will be replaced in later tasks).

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/AndroidManifest.xml
git commit -m "feat: declare TimerService, alarm receiver, and runtime permissions"
```

---

## Task 6: Implement `TimerNotification` builder + notification channels

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerNotification.kt`

- [ ] **Step 1: Create the notification helper**

```kotlin
package com.endiar.purrodoro.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.endiar.purrodoro.MainActivity
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.formatSeconds

object TimerNotification {

    const val CHANNEL_RUNNING = "timer_running"
    const val CHANNEL_COMPLETION = "timer_completion"

    const val NOTIF_ID_RUNNING = 1001
    const val NOTIF_ID_COMPLETION = 1002

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNNING,
                "Timer running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while a Pomodoro is active"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COMPLETION,
                "Segment complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notification when a Focus or Break segment ends"
            }
        )
    }

    fun buildRunningNotification(
        context: Context,
        state: TimerRuntimeState,
        now: Long
    ): Notification {
        val (title, subtitle) = describe(state, now)
        val remaining = currentRemainingSeconds(state, now)
        val total = state.session.items.getOrNull(state.session.currentIndex)?.type?.seconds() ?: 1

        val builder = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .setProgress(total, total - remaining, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        when (state.session.timerState) {
            TimerState.Playing -> {
                builder.addAction(action(context, "Pause", TimerService.ACTION_PAUSE))
                builder.addAction(action(context, "Stop", TimerService.ACTION_STOP))
                if (isOnBreak(state)) {
                    builder.addAction(action(context, "Skip", TimerService.ACTION_SKIP))
                }
            }
            TimerState.Paused -> {
                builder.addAction(action(context, "Resume", TimerService.ACTION_RESUME))
                builder.addAction(action(context, "Stop", TimerService.ACTION_STOP))
            }
            TimerState.Idle -> Unit
        }
        return builder.build()
    }

    enum class CompletionKind { Focus, Break, Cycle }

    fun buildCompletionNotification(context: Context, kind: CompletionKind): Notification {
        val (title, body) = when (kind) {
            CompletionKind.Focus -> "Focus complete" to "Tap to start your break"
            CompletionKind.Break -> "Break complete" to "Ready for another focus session?"
            CompletionKind.Cycle -> "Cycle complete" to "Nice work — your Pomodoro cycle is done"
        }
        return NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun describe(state: TimerRuntimeState, now: Long): Pair<String, String?> {
        val remaining = currentRemainingSeconds(state, now)
        val timeStr = formatSeconds(remaining)
        val currentItem = state.session.items.getOrNull(state.session.currentIndex)
        val segmentLabel = when (currentItem?.type) {
            is SessionItemType.Focus -> "Focus"
            is SessionItemType.ShortBreak -> "Short Break"
            is SessionItemType.LongBreak -> "Long Break"
            null -> "Timer"
        }
        val title = when (state.session.timerState) {
            TimerState.Playing -> "$segmentLabel · $timeStr"
            TimerState.Paused -> "$segmentLabel · Paused"
            TimerState.Idle -> segmentLabel
        }
        val subtitle = when {
            state.session.timerState == TimerState.Paused -> "$timeStr remaining"
            state.activeTask != null && currentItem?.type is SessionItemType.Focus ->
                "${state.activeTask.title} · ${state.activeTask.completedSessions + 1} / ${state.activeTask.plannedSessions}"
            else -> null
        }
        return title to subtitle
    }

    private fun isOnBreak(state: TimerRuntimeState): Boolean {
        val t = state.session.items.getOrNull(state.session.currentIndex)?.type ?: return false
        return t is SessionItemType.ShortBreak || t is SessionItemType.LongBreak
    }

    private fun currentRemainingSeconds(state: TimerRuntimeState, now: Long): Int {
        return if (state.session.timerState == TimerState.Playing && state.endsAtMillis != null) {
            ((state.endsAtMillis - now) / 1000L).toInt().coerceAtLeast(0)
        } else {
            state.session.remainingSeconds
        }
    }

    private fun SessionItemType.seconds(): Int = when (this) {
        is SessionItemType.Focus -> seconds
        is SessionItemType.ShortBreak -> seconds
        is SessionItemType.LongBreak -> seconds
    }

    private fun action(context: Context, label: String, action: String): NotificationCompat.Action {
        val intent = Intent(context, TimerService::class.java).setAction(action)
        val pi = PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pi).build()
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
```

- [ ] **Step 2: Build — will fail on `TimerService.ACTION_*` until Task 9. Add a placeholder.**

For now, add a tiny placeholder so the file compiles. Create `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerService.kt`:

```kotlin
package com.endiar.purrodoro.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder

// Placeholder — fully implemented in Task 9.
class TimerService : Service() {
    companion object {
        const val ACTION_START = "com.endiar.purrodoro.timer.START"
        const val ACTION_PAUSE = "com.endiar.purrodoro.timer.PAUSE"
        const val ACTION_RESUME = "com.endiar.purrodoro.timer.RESUME"
        const val ACTION_STOP = "com.endiar.purrodoro.timer.STOP"
        const val ACTION_SKIP = "com.endiar.purrodoro.timer.SKIP"
        const val ACTION_ALARM_FIRED = "com.endiar.purrodoro.timer.ALARM_FIRED"

        @Volatile var isRunning: Boolean = false
            internal set
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
```

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerNotification.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerService.kt
git commit -m "feat: add notification channels and TimerNotification builder"
```

---

## Task 7: Implement `TimerStateMachine` shared transition helper

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerStateMachine.kt`

This helper is invoked by both `TimerService` (on natural tick to zero) and `TimerAlarmReceiver` (when the alarm wakes a dead service). Its job: apply a `TimerComplete` transition, persist all side effects, return the new state.

- [ ] **Step 1: Create the helper**

```kotlin
package com.endiar.purrodoro.timer

import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import java.time.Instant
import java.time.ZoneId

object TimerStateMachine {

    /**
     * Apply one TimerComplete transition: advance the cycle, accrue stamps,
     * close records on cycle end, persist runtime snapshot.
     *
     * Returns the new state. The caller decides whether to keep the service running
     * (still active) or stop it (Idle or Paused awaiting user input).
     */
    suspend fun applyTimerComplete(
        snapshot: TimerRuntimeState,
        settings: TimerSettings,
        now: Long,
        repo: AppRepository,
        zone: ZoneId = ZoneId.systemDefault()
    ): TimerRuntimeState {
        if (snapshot.session.timerState != TimerState.Playing) return snapshot

        val currentItem = snapshot.session.items.getOrNull(snapshot.session.currentIndex)
            ?: return snapshot

        var openRecord = snapshot.openRecord

        if (currentItem.type is SessionItemType.Focus) {
            val date = Instant.ofEpochMilli(snapshot.endsAtMillis ?: now).atZone(zone).toLocalDate()
            repo.incrementStampFor(date)
            openRecord = openRecord?.copy(
                completedFocusCount = openRecord.completedFocusCount + 1
            )
        }

        val nextIndex = snapshot.session.currentIndex + 1
        if (nextIndex >= snapshot.session.items.size) {
            openRecord?.let { rec ->
                repo.appendSessionRecord(
                    rec.copy(
                        endedAt = snapshot.endsAtMillis ?: now,
                        status = SessionStatus.Completed
                    )
                )
            }
            val resetState = snapshot.copy(
                session = snapshot.session.copy(
                    currentIndex = 0,
                    remainingSeconds = snapshot.session.items.first().type.seconds(),
                    timerState = TimerState.Idle
                ),
                endsAtMillis = null,
                openRecord = null
            )
            repo.setRuntimeSnapshot(null)
            return resetState
        }

        val nextItem = snapshot.session.items[nextIndex]
        val nextSeconds = nextItem.type.seconds()
        val baseEndsAt = snapshot.endsAtMillis ?: now

        val advanced = if (settings.autoStartNextSession) {
            snapshot.copy(
                session = snapshot.session.copy(
                    currentIndex = nextIndex,
                    remainingSeconds = nextSeconds,
                    timerState = TimerState.Playing
                ),
                endsAtMillis = baseEndsAt + nextSeconds * 1000L,
                openRecord = openRecord
            )
        } else {
            snapshot.copy(
                session = snapshot.session.copy(
                    currentIndex = nextIndex,
                    remainingSeconds = nextSeconds,
                    timerState = TimerState.Paused
                ),
                endsAtMillis = null,
                openRecord = openRecord
            )
        }
        repo.setRuntimeSnapshot(advanced)
        return advanced
    }

    private fun SessionItemType.seconds(): Int = when (this) {
        is SessionItemType.Focus -> seconds
        is SessionItemType.ShortBreak -> seconds
        is SessionItemType.LongBreak -> seconds
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerStateMachine.kt
git commit -m "feat: add TimerStateMachine shared transition helper"
```

---

## Task 8: Implement `TimerAlarmReceiver`

The alarm receiver runs when the exact alarm fires. If the service is alive, it's a near-no-op (the service has already advanced). If the service is dead, the receiver advances state itself, posts a completion notification, and may restart the service for the next segment if auto-advance is on.

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerAlarmReceiver.kt`

- [ ] **Step 1: Create the receiver**

```kotlin
package com.endiar.purrodoro.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        val expectedEndsAtMillis = intent?.getLongExtra(EXTRA_EXPECTED_ENDS_AT, -1L) ?: -1L

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AppRepository.get(appCtx)
                val snapshot = repo.runtimeSnapshot.first() ?: return@launch
                val appState = repo.appState.first()
                val settings = appState.settings

                // If the snapshot has already advanced past the alarm's target time, the service
                // handled it. Do nothing to avoid double-posting.
                if (snapshot.endsAtMillis == null || snapshot.endsAtMillis != expectedEndsAtMillis) {
                    return@launch
                }

                if (snapshot.session.timerState != TimerState.Playing) return@launch

                val completedItemType = snapshot.session.items
                    .getOrNull(snapshot.session.currentIndex)?.type

                val now = System.currentTimeMillis()
                val advanced = TimerStateMachine.applyTimerComplete(
                    snapshot = snapshot,
                    settings = settings,
                    now = now,
                    repo = repo
                )

                val kind = when (completedItemType) {
                    is SessionItemType.Focus ->
                        if (advanced.session.timerState == TimerState.Idle) TimerNotification.CompletionKind.Cycle
                        else TimerNotification.CompletionKind.Focus
                    is SessionItemType.ShortBreak,
                    is SessionItemType.LongBreak ->
                        if (advanced.session.timerState == TimerState.Idle) TimerNotification.CompletionKind.Cycle
                        else TimerNotification.CompletionKind.Break
                    else -> TimerNotification.CompletionKind.Focus
                }

                TimerNotification.ensureChannels(appCtx)
                NotificationManagerCompat.from(appCtx).notify(
                    TimerNotification.NOTIF_ID_COMPLETION + uniqueIdSuffix(),
                    TimerNotification.buildCompletionNotification(appCtx, kind)
                )

                if (advanced.session.timerState == TimerState.Playing && !TimerService.isRunning) {
                    // Auto-advance into next segment but service was killed — restart it.
                    val resumeIntent = Intent(appCtx, TimerService::class.java)
                        .setAction(TimerService.ACTION_ALARM_FIRED)
                    appCtx.startForegroundService(resumeIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun uniqueIdSuffix(): Int =
        (SystemClock.elapsedRealtime() % Int.MAX_VALUE).toInt() and 0x0000_FFFF

    companion object {
        const val EXTRA_EXPECTED_ENDS_AT = "com.endiar.purrodoro.timer.EXPECTED_ENDS_AT"
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerAlarmReceiver.kt
git commit -m "feat: add TimerAlarmReceiver alarm backstop"
```

---

## Task 9: Implement `TimerService` (foreground service with binder)

Full replacement of the Task 6 placeholder. The service owns state, runs the tick loop, dispatches controller actions, schedules alarms, and persists snapshots.

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerService.kt`
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerController.kt`

- [ ] **Step 1: Create `TimerController.kt` interface**

```kotlin
package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.TimerSettings

interface TimerController {
    fun start(settings: TimerSettings, task: FocusTask?)
    fun pause()
    fun resume()
    fun stop()
    fun skip()
}
```

- [ ] **Step 2: Replace `TimerService.kt` placeholder with the full implementation**

```kotlin
package com.endiar.purrodoro.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.buildCycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private val _state = MutableStateFlow<TimerRuntimeState?>(null)
    private var tickJob: Job? = null

    private lateinit var repo: AppRepository
    private lateinit var alarmManager: AlarmManager

    private val controller: TimerController = object : TimerController {
        override fun start(settings: TimerSettings, task: FocusTask?) {
            scope.launch { startCycle(settings, task) }
        }
        override fun pause()  { scope.launch { pause() } }
        override fun resume() { scope.launch { resumePlaying() } }
        override fun stop()   { scope.launch { stopAndExit() } }
        override fun skip()   { scope.launch { skipSegment() } }
    }

    inner class LocalBinder : Binder() {
        val state: StateFlow<TimerRuntimeState?> = _state.asStateFlow()
        val controller: TimerController = this@TimerService.controller
    }
    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        repo = AppRepository.get(applicationContext)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        TimerNotification.ensureChannels(applicationContext)
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        // Promote to foreground IMMEDIATELY using whatever state we have on hand.
        val placeholder = _state.value ?: TimerRuntimeState(
            session = TimerSession(emptyList(), 0, 0, TimerState.Idle),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null
        )
        val initialNotification = TimerNotification.buildRunningNotification(
            applicationContext, placeholder, System.currentTimeMillis()
        )
        ServiceCompat.startForeground(
            this,
            TimerNotification.NOTIF_ID_RUNNING,
            initialNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        scope.launch {
            when (action) {
                ACTION_PAUSE  -> pause()
                ACTION_RESUME -> resumePlaying()
                ACTION_STOP   -> stopAndExit()
                ACTION_SKIP   -> skipSegment()
                ACTION_ALARM_FIRED -> resumeFromAlarm()
                else -> {} // ACTION_START is driven through the controller's start()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        isRunning = false
        tickJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    // --- Transitions ---

    private suspend fun startCycle(settings: TimerSettings, task: FocusTask?) {
        val items = buildCycle(settings)
        val firstSeconds = items.first().type.seconds()
        val now = System.currentTimeMillis()
        val record = SessionRecord(
            id = now,
            startedAt = now,
            endedAt = 0L,
            focusMinutes = settings.focusMinutes,
            shortBreakMinutes = settings.shortBreakMinutes,
            longBreakMinutes = settings.longBreakMinutes,
            completedFocusCount = 0,
            totalFocusCount = items.count { it.type is SessionItemType.Focus },
            status = SessionStatus.Partial,
            taskName = task?.title
        )
        val state = TimerRuntimeState(
            session = TimerSession(items, 0, firstSeconds, TimerState.Playing),
            endsAtMillis = now + firstSeconds * 1000L,
            activeTask = task,
            openRecord = record
        )
        updateState(state)
        scheduleAlarm(state)
        startTicking()
    }

    private suspend fun pause() {
        val current = _state.value ?: repo.runtimeSnapshot.first() ?: return
        if (current.session.timerState != TimerState.Playing) return
        val now = System.currentTimeMillis()
        val remaining = current.endsAtMillis?.let { ((it - now) / 1000L).toInt().coerceAtLeast(0) }
            ?: current.session.remainingSeconds
        val paused = current.copy(
            session = current.session.copy(
                remainingSeconds = remaining,
                timerState = TimerState.Paused
            ),
            endsAtMillis = null
        )
        cancelAlarm()
        stopTicking()
        updateState(paused)
    }

    private suspend fun resumePlaying() {
        val current = _state.value ?: repo.runtimeSnapshot.first() ?: return
        if (current.session.timerState != TimerState.Paused) return
        val now = System.currentTimeMillis()
        val resumed = current.copy(
            session = current.session.copy(timerState = TimerState.Playing),
            endsAtMillis = now + current.session.remainingSeconds * 1000L
        )
        updateState(resumed)
        scheduleAlarm(resumed)
        startTicking()
    }

    private suspend fun skipSegment() {
        val current = _state.value ?: return
        val currentType = current.session.items.getOrNull(current.session.currentIndex)?.type
        if (currentType !is SessionItemType.ShortBreak && currentType !is SessionItemType.LongBreak) {
            return
        }
        cancelAlarm()
        val now = System.currentTimeMillis()
        val nextIndex = current.session.currentIndex + 1
        if (nextIndex >= current.session.items.size) {
            // Skipping last item ends the cycle.
            closeOpenRecord(SessionStatus.Completed, now)
            updateState(idleState(current.session.items))
            cancelAlarm()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val nextSeconds = current.session.items[nextIndex].type.seconds()
        val updated = current.copy(
            session = current.session.copy(
                currentIndex = nextIndex,
                remainingSeconds = nextSeconds,
                timerState = TimerState.Playing
            ),
            endsAtMillis = now + nextSeconds * 1000L
        )
        updateState(updated)
        scheduleAlarm(updated)
        startTicking()
    }

    private suspend fun stopAndExit() {
        val current = _state.value
        if (current != null) {
            val now = System.currentTimeMillis()
            current.openRecord?.let { rec ->
                repo.appendSessionRecord(
                    rec.copy(endedAt = now, status = SessionStatus.Partial)
                )
            }
        }
        cancelAlarm()
        stopTicking()
        repo.setRuntimeSnapshot(null)
        _state.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Called when the service is restarted via ACTION_ALARM_FIRED — the alarm receiver detected
     * the service was dead and an auto-advance is required. The receiver already applied the
     * TimerComplete transition and updated the snapshot; we just restart ticking.
     */
    private suspend fun resumeFromAlarm() {
        val snapshot = repo.runtimeSnapshot.first() ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (snapshot.session.timerState != TimerState.Playing) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        _state.value = snapshot
        scheduleAlarm(snapshot)
        startTicking()
    }

    // --- Tick loop ---

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val current = _state.value ?: break
                if (current.session.timerState != TimerState.Playing) break
                val now = System.currentTimeMillis()
                val endsAt = current.endsAtMillis ?: break

                if (now >= endsAt) {
                    val settings = repo.appState.first().settings
                    val next = TimerStateMachine.applyTimerComplete(current, settings, now, repo)
                    // Completion notification
                    val completedType = current.session.items.getOrNull(current.session.currentIndex)?.type
                    val kind = when {
                        next.session.timerState == TimerState.Idle -> TimerNotification.CompletionKind.Cycle
                        completedType is SessionItemType.Focus -> TimerNotification.CompletionKind.Focus
                        else -> TimerNotification.CompletionKind.Break
                    }
                    NotificationManagerCompat.from(applicationContext).notify(
                        TimerNotification.NOTIF_ID_COMPLETION + (System.currentTimeMillis() % 1000).toInt(),
                        TimerNotification.buildCompletionNotification(applicationContext, kind)
                    )

                    _state.value = next
                    cancelAlarm()
                    if (next.session.timerState == TimerState.Playing) {
                        scheduleAlarm(next)
                    } else {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }
                } else {
                    refreshOngoingNotification(current, now)
                }
                delay(1000L)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun refreshOngoingNotification(state: TimerRuntimeState, now: Long) {
        val notif = TimerNotification.buildRunningNotification(applicationContext, state, now)
        NotificationManagerCompat.from(applicationContext)
            .notify(TimerNotification.NOTIF_ID_RUNNING, notif)
    }

    // --- Alarm scheduling ---

    private fun scheduleAlarm(state: TimerRuntimeState) {
        val endsAt = state.endsAtMillis ?: return
        val intent = Intent(applicationContext, TimerAlarmReceiver::class.java).apply {
            putExtra(TimerAlarmReceiver.EXTRA_EXPECTED_ENDS_AT, endsAt)
        }
        val pi = PendingIntent.getBroadcast(
            applicationContext, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pi)
    }

    private fun cancelAlarm() {
        val intent = Intent(applicationContext, TimerAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            applicationContext, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    // --- Helpers ---

    private suspend fun updateState(state: TimerRuntimeState) {
        _state.value = state
        repo.setRuntimeSnapshot(state)
        refreshOngoingNotification(state, System.currentTimeMillis())
    }

    private suspend fun closeOpenRecord(status: SessionStatus, endedAt: Long) {
        val current = _state.value ?: return
        current.openRecord?.let { rec ->
            repo.appendSessionRecord(rec.copy(endedAt = endedAt, status = status))
        }
    }

    private fun idleState(items: List<com.endiar.purrodoro.model.TimerSessionItem>): TimerRuntimeState {
        val firstSeconds = items.firstOrNull()?.type?.seconds() ?: 0
        return TimerRuntimeState(
            session = TimerSession(items, 0, firstSeconds, TimerState.Idle),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null
        )
    }

    private fun SessionItemType.seconds(): Int = when (this) {
        is SessionItemType.Focus -> seconds
        is SessionItemType.ShortBreak -> seconds
        is SessionItemType.LongBreak -> seconds
    }

    companion object {
        const val ACTION_START = "com.endiar.purrodoro.timer.START"
        const val ACTION_PAUSE = "com.endiar.purrodoro.timer.PAUSE"
        const val ACTION_RESUME = "com.endiar.purrodoro.timer.RESUME"
        const val ACTION_STOP = "com.endiar.purrodoro.timer.STOP"
        const val ACTION_SKIP = "com.endiar.purrodoro.timer.SKIP"
        const val ACTION_ALARM_FIRED = "com.endiar.purrodoro.timer.ALARM_FIRED"

        private const val ALARM_REQUEST_CODE = 9001

        @Volatile var isRunning: Boolean = false
            internal set
    }
}
```

- [ ] **Step 3: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerService.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerController.kt
git commit -m "feat: implement TimerService foreground service with binder"
```

---

## Task 10: Implement `TimerConnection` (UI binding helper)

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerConnection.kt`

- [ ] **Step 1: Create the connection helper**

```kotlin
package com.endiar.purrodoro.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI-side helper that binds to [TimerService] and exposes its state as a [StateFlow].
 *
 * - When the service is running and bound, [state] mirrors the service's internal flow.
 * - When the service is not running, [state] emits the persisted runtime snapshot from
 *   the repository (so the UI can show a paused/reconciled state without the service alive).
 */
class TimerConnection(private val context: Context) {

    private val _state = MutableStateFlow<TimerRuntimeState?>(null)
    val state: StateFlow<TimerRuntimeState?> = _state.asStateFlow()

    private var controllerRef: TimerController? = null
    val controller: TimerController? get() = controllerRef ?: FallbackController

    private var serviceJob: Job? = null
    private var snapshotJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TimerService.LocalBinder ?: return
            controllerRef = binder.controller
            serviceJob?.cancel()
            serviceJob = scope.launch {
                binder.state.collectLatest { _state.value = it }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            controllerRef = null
            serviceJob?.cancel()
            serviceJob = null
        }
    }

    fun bind() {
        TimerNotification.ensureChannels(context)
        snapshotJob?.cancel()
        snapshotJob = scope.launch {
            AppRepository.get(context).runtimeSnapshot.collectLatest { snapshot ->
                if (controllerRef == null) _state.value = snapshot
            }
        }
        if (TimerService.isRunning) {
            val intent = Intent(context, TimerService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        snapshotJob?.cancel()
        snapshotJob = null
        if (controllerRef != null) {
            try { context.unbindService(serviceConnection) } catch (_: IllegalArgumentException) {}
        }
        controllerRef = null
        serviceJob?.cancel()
        serviceJob = null
    }

    /**
     * Start a new cycle. Bootstraps the service if not running, then binds.
     */
    fun start(settings: TimerSettings, task: FocusTask?) {
        if (!TimerService.isRunning) {
            val intent = Intent(context, TimerService::class.java)
                .setAction(TimerService.ACTION_START)
            context.startForegroundService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            // Defer the start call until binder arrives
            scope.launch {
                // Poll for binder up to 2 seconds
                var attempts = 0
                while (controllerRef == null && attempts < 20) {
                    kotlinx.coroutines.delay(100)
                    attempts++
                }
                controllerRef?.start(settings, task)
            }
        } else {
            controllerRef?.start(settings, task)
        }
    }

    /** A no-op fallback used when the service isn't bound yet — callers should usually not hit this. */
    private object FallbackController : TimerController {
        override fun start(settings: TimerSettings, task: FocusTask?) {}
        override fun pause()  {}
        override fun resume() {}
        override fun stop()   {}
        override fun skip()   {}
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerConnection.kt
git commit -m "feat: add TimerConnection UI-side binding helper"
```

---

## Task 11: Wire `TimerConnection` into `MainActivity` + notification permission

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/MainActivity.kt`

- [ ] **Step 1: Replace `MainActivity.kt` contents**

```kotlin
package com.endiar.purrodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.timer.TimerConnection
import com.endiar.purrodoro.timer.TimerNotification
import com.endiar.purrodoro.ui.theme.KuroPomodoroTheme

class MainActivity : ComponentActivity() {

    private lateinit var timerConnection: TimerConnection

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TimerNotification.ensureChannels(applicationContext)
        timerConnection = TimerConnection(applicationContext)

        setContent {
            KuroPomodoroTheme {
                KuroPomodoroApp(
                    repo = AppRepository.get(this),
                    timerConnection = timerConnection,
                    requestNotificationPermission = ::requestNotificationPermissionIfNeeded
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        timerConnection.bind()
    }

    override fun onStop() {
        timerConnection.unbind()
        super.onStop()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

- [ ] **Step 2: Build — will fail because `KuroPomodoroApp` doesn't accept the new params yet. Task 12 fixes this.**

Skip ahead to Task 12 to update `KuroPomodoroApp`'s signature.

- [ ] **Step 3: After Task 12 passes build, commit Task 11 changes together with Task 12 (one combined commit). Continue to Task 12.**

---

## Task 12: Migrate `KuroPomodoroApp.kt` to service-driven state

This is the biggest single change. We:
1. Add new parameters (`timerConnection`, `requestNotificationPermission`).
2. Remove the inlined `LaunchedEffect` timer loop.
3. Remove all duplicated timer state vars.
4. Read state from the service flow.
5. Replace control-flow lambdas to call the controller.

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Update the function signature and imports**

Add imports at the top of `KuroPomodoroApp.kt`:

```kotlin
import com.endiar.purrodoro.timer.TimerConnection
import com.endiar.purrodoro.model.TimerRuntimeState
```

Change the function signature:

```kotlin
@Composable
fun KuroPomodoroApp(
    repo: AppRepository,
    timerConnection: TimerConnection,
    requestNotificationPermission: () -> Unit
) {
```

- [ ] **Step 2: Add state collection from the connection**

Inside `KuroPomodoroApp`, immediately after `val appState by repo.appState.collectAsStateWithLifecycle(...)`, add:

```kotlin
val runtimeState by timerConnection.state.collectAsStateWithLifecycle(initialValue = null)
```

- [ ] **Step 3: Remove duplicated timer state vars**

Delete the following declarations from `KuroPomodoroApp` (currently around lines 125–183):

```kotlin
// DELETE these:
var timerMode by rememberSaveable(...)
var remainingSeconds by rememberSaveable(...)
var activeTotalSeconds by rememberSaveable(...)
var isRunning by rememberSaveable(...)
var isStarted by rememberSaveable(...)
var openRecord by remember(...)
var cycleItems by rememberSaveable(...)
var cycleIndex by rememberSaveable(...)
var timerStartedAtMillis by rememberSaveable(...)
var timerStartedRemainingSeconds by rememberSaveable(...)
```

These move into the service / are derived from `runtimeState`.

Keep:
- `selectedTab`
- `playingSoundId`
- `premiumSheetMessage`
- `activeTask`
- `isFocusMode`

- [ ] **Step 4: Delete the inlined `LaunchedEffect` timer loop**

Delete lines ~252–354 (the entire `LaunchedEffect(isRunning, timerMode, settings) { ... }` block). All of that logic now lives in `TimerService` + `TimerStateMachine`.

- [ ] **Step 5: Compute derived values from `runtimeState`**

Above the root surface (`KuroFlatSurface`) call, add:

```kotlin
val isActive = runtimeState?.isActive == true
val isRunning = runtimeState?.session?.timerState == TimerState.Playing
val isStarted = runtimeState != null && runtimeState?.session?.timerState != TimerState.Idle
val now = System.currentTimeMillis()
val remainingSeconds = runtimeState?.let { rt ->
    if (rt.session.timerState == TimerState.Playing && rt.endsAtMillis != null) {
        ((rt.endsAtMillis - now) / 1000L).toInt().coerceAtLeast(0)
    } else {
        rt.session.remainingSeconds
    }
} ?: settings.secondsFor(TimerMode.Focus)

val currentItemType = runtimeState?.session?.items?.getOrNull(runtimeState?.session?.currentIndex ?: 0)?.type
val timerMode = when (currentItemType) {
    is SessionItemType.Focus -> TimerMode.Focus
    is SessionItemType.ShortBreak -> TimerMode.ShortBreak
    is SessionItemType.LongBreak -> TimerMode.LongBreak
    null -> TimerMode.Focus
}
val cycleItems = runtimeState?.session?.items ?: emptyList()
val cycleIndex = runtimeState?.session?.currentIndex ?: 0
```

Compose only recomposes when an observed state changes. To make the on-screen countdown visibly tick every second, introduce a state-backed clock value and drive `remainingSeconds` from it. Replace the `val now = System.currentTimeMillis()` line with the following block:

```kotlin
var tickNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
LaunchedEffect(isRunning) {
    while (isRunning) {
        tickNow = System.currentTimeMillis()
        delay(250L)
    }
}
val now = tickNow
```

Add this import at the top of `KuroPomodoroApp.kt` if it's not already there:

```kotlin
import androidx.compose.runtime.mutableLongStateOf
```

- [ ] **Step 6: Replace control lambdas to use the controller**

Find the `AppTab.Timer -> FocusDashboard(...)` block. Replace its lambda arguments:

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
    onSkip = { timerConnection.controller?.skip() },
    onStartPause = {
        when {
            !isStarted -> {
                requestNotificationPermission()
                isFocusMode = true
                timerConnection.start(settings, activeTask)
            }
            isRunning  -> {
                isFocusMode = false
                timerConnection.controller?.pause()
            }
            else       -> {
                timerConnection.controller?.resume()
            }
        }
    },
    onStop = {
        timerConnection.controller?.stop()
        isFocusMode = false
        activeTask = null
    },
    onExitFocusMode = { isFocusMode = false },
    onEnterFocusMode = { isFocusMode = true }
)
```

- [ ] **Step 7: Update Profile screen's duration-change handler**

Inside `AppTab.Profile -> ProfileScreen(...)`, change `onDurationChanged`:

```kotlin
onDurationChanged = { mode, minutes ->
    val updatedSettings = settings.update(mode, minutes)
    persistSettings(updatedSettings)
    // No more inline remainingSeconds bump — service rebuilds on next start
},
```

- [ ] **Step 8: Update Todo focus-task tap to start the service**

In the `AppTab.Todo ->` block, change `onFocusTask`:

```kotlin
onFocusTask = { task ->
    activeTask = task
    isFocusMode = true
    selectedTab = AppTab.Timer
    requestNotificationPermission()
    timerConnection.start(settings, task)
},
```

- [ ] **Step 9: Delete obsolete tests in `KuroPomodoroAppTest.kt`**

The pre-existing test file `purrodoro-android/app/src/test/java/com/endiar/purrodoro/KuroPomodoroAppTest.kt` tests two internal helpers (`timerDisplayTotalSeconds`, `timerStartTotalSeconds`) that this task removed from `KuroPomodoroApp.kt`. With those helpers gone, the tests no longer compile. Replace the entire file contents with:

```kotlin
package com.endiar.purrodoro

// Tests removed: timerDisplayTotalSeconds and timerStartTotalSeconds were internal helpers
// for the inline timer math removed during the migration to TimerService.
// New unit coverage for the service-driven path lives in:
//   - timer/TimerReconcilerTest.kt
//   - model/TimerSessionReducerTest.kt
```

If you prefer to delete the file outright instead, that's equivalent — both options are acceptable. Keeping the file with a comment makes the rationale discoverable; deletion makes the test directory cleaner.

- [ ] **Step 10: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`. If you hit errors about removed savers or unused imports, delete those imports (`AppTabSaver`/`TimerModeSaver` may still be used — keep them).

- [ ] **Step 11: Commit Task 11 + Task 12 together**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/MainActivity.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt
git commit -m "feat: migrate KuroPomodoroApp to service-driven timer state"
```

---

## Task 13: Wire reconciliation on app open

When `MainActivity` resumes and the service is not running but a persisted snapshot exists, we run `TimerReconciler.reconcile` and persist the reconciled state. If the result is `Playing` (auto-advance case), we start the service to take over.

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerConnection.kt`

- [ ] **Step 1: Add a `reconcileOnBind` step to `TimerConnection.bind`**

Replace the body of `fun bind()` with:

```kotlin
fun bind() {
    TimerNotification.ensureChannels(context)
    snapshotJob?.cancel()
    snapshotJob = scope.launch {
        val repo = AppRepository.get(context)

        // Reconcile if the service is not running but we have a persisted snapshot.
        if (!TimerService.isRunning) {
            val snapshot = repo.runtimeSnapshot.first()
            if (snapshot != null && snapshot.session.timerState == TimerState.Playing) {
                val settings = repo.appState.first().settings
                val now = System.currentTimeMillis()
                val result = TimerReconciler.reconcile(snapshot, now, settings)

                // Persist stamps + closed records
                result.stampsToIncrement.forEach { date -> repo.incrementStampFor(date) }
                result.closedRecords.forEach { repo.appendSessionRecord(it) }
                repo.setRuntimeSnapshot(
                    if (result.state.session.timerState == TimerState.Idle) null else result.state
                )

                if (result.state.session.timerState == TimerState.Playing) {
                    val intent = Intent(context, TimerService::class.java)
                        .setAction(TimerService.ACTION_ALARM_FIRED)
                    context.startForegroundService(intent)
                }
            }
        }

        // Now subscribe to the persisted snapshot (this also covers the just-reconciled state).
        repo.runtimeSnapshot.collectLatest { snapshot ->
            if (controllerRef == null) _state.value = snapshot
        }
    }
    if (TimerService.isRunning) {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}
```

Add the imports needed:

```kotlin
import com.endiar.purrodoro.model.TimerState
import kotlinx.coroutines.flow.first
```

- [ ] **Step 2: Build to verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run all unit tests to ensure nothing regressed**

```bash
cd purrodoro-android && ./gradlew test 2>&1 | tail -20
```

Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerConnection.kt
git commit -m "feat: reconcile persisted timer state on app open"
```

---

## Task 14: Manual smoke test checklist

This task has no code; it documents the manual tests an engineer should run on a real device or emulator before claiming this is done.

- [ ] **Step 1: Install the debug build on a device or emulator**

```bash
cd purrodoro-android && ./gradlew installDebug
```

Expected: app installs and launches.

- [ ] **Step 2: Manual test checklist — execute each and confirm**

Open the app and follow each scenario in order, marking each ✅ or ❌:

1. **Notification permission prompt fires on first Focus Now tap** (Android 13+).
2. **Live notification appears** showing `Focus · 24:59` and counts down in real time.
3. **Swipe app away from recents** → notification keeps ticking; service stays alive.
4. **Tap Pause in the notification** → title flips to `Focus · Paused`, actions become **Resume** / **Stop**. Reopen app → UI shows paused state with correct remaining time.
5. **Tap Resume in the notification** → countdown continues. Reopen app → UI shows running state.
6. **Set focus to 1 minute** (in Profile → durations). Start a focus, lock device, wait 1+ min → a **Focus complete** notification appears.
7. **Auto-advance off, focus ends while app closed** → reopening app shows paused at ShortBreak with full duration; persisted snapshot reflects this.
8. **Auto-advance on, focus ends while app closed** → reopening app shows ShortBreak running with correct remaining time; service is alive.
9. **Tap Stop in notification** → ongoing notification disappears; reopen app → idle dashboard; a `Partial` SessionRecord is added to Records.
10. **Long-running cycle: kill app process (`adb shell am kill com.endiar.purrodoro`)** while running → wait for segment to end → completion notification still fires (alarm backstop). Reopen app → state is correct.
11. **Skip action on a break** → cycle advances; on a Focus, **Skip** button is hidden.
12. **Cycle complete** → `Cycle complete` notification fires; Records shows a `Completed` SessionRecord.

- [ ] **Step 3: Document any failures and fix them in follow-up commits**

For any ❌ above, file a bug, root-cause it, and fix. No commit needed for this task itself unless fixes are made.

---

## Self-Review Checklist

- [x] **Spec coverage:**
  - Architecture (new `timer/` package) ✓ Task 2, 3, 6–10
  - `TimerRuntimeState` model ✓ Task 2
  - State transitions through reducer ✓ Task 1, 7
  - Stamp boundary handling ✓ Task 7 (service does the increment)
  - Shared transition helper ✓ Task 7
  - Service lifecycle ✓ Task 9
  - AlarmManager backstop ✓ Task 8, 9
  - Notification UX ✓ Task 6
  - UI ↔ Service contract (controller + connection) ✓ Task 9, 10
  - Reconciliation ✓ Task 3, 13
  - Persistence ✓ Task 4
  - Permissions & manifest ✓ Task 5
  - Migration of `KuroPomodoroApp` ✓ Task 12
  - Unit tests for reducer, reconciler, repo encode/decode ✓ Task 1, 3, 4
  - Manual checklist ✓ Task 14

- [x] **Placeholder scan:** All steps have concrete code or concrete commands.

- [x] **Type consistency:**
  - `TimerRuntimeState(session, endsAtMillis, activeTask, openRecord)` consistent across Tasks 2, 3, 4, 7, 9, 10, 13.
  - `TimerController { start, pause, resume, stop, skip }` consistent across Tasks 9, 10, 12.
  - `TimerReconciler.reconcile(snapshot, now, settings)` consistent between Tasks 3 and 13.
  - `TimerStateMachine.applyTimerComplete(snapshot, settings, now, repo)` consistent between Tasks 7, 8, 9.
  - `TimerNotification` IDs / channel names / action constants consistent across Tasks 6, 8, 9.
  - `AppRepository.setRuntimeSnapshot`, `runtimeSnapshot`, `incrementStampFor` consistent across Tasks 4, 7, 9, 13.
