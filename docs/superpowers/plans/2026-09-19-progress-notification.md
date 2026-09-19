# Progress-Centric Timer Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render the running timer notification with Android 16 `Notification.ProgressStyle` (whole-cycle bar, segments, milestone points) on API 36+, keeping the current compat bar below API 36.

**Architecture:** New pure mapper `timer/CycleProgress.kt` converts cycle items + position into total/elapsed/segments/milestones; `TimerNotification` gains an SDK-gated framework builder that reuses `describe()`, actions, channels, and flags. No `TimerService` change — `refreshOngoingNotification` already rebuilds each tick.

**Tech Stack:** Kotlin, framework `android.app.Notification.ProgressStyle` (API 36), JUnit, Robolectric.

---

## File structure

- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/CycleProgress.kt` — pure cycle→progress mapping, no Android dependencies.
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerNotification.kt` — SDK gate + framework `ProgressStyle` builder + action helper.
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerCycleProgressTest.kt` — plain JUnit tests for the mapper.
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerNotificationLegacyTest.kt` — Robolectric test locking the below-36 fallback path.

---

### Task 1: Pure cycle-progress mapper with tests

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/CycleProgress.kt`
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerCycleProgressTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerSessionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerCycleProgressTest {

    private fun items() = listOf(
        TimerSessionItem(SessionItemType.Focus(25 * 60)),
        TimerSessionItem(SessionItemType.ShortBreak(5 * 60)),
        TimerSessionItem(SessionItemType.Focus(25 * 60)),
        TimerSessionItem(SessionItemType.LongBreak(15 * 60)),
    )

    @Test
    fun `whole cycle totals 4200 seconds with focus milestones`() {
        val p = cycleProgress(items(), currentIndex = 0, segmentRemainingSeconds = 25 * 60)
        assertEquals(4200, p.totalSeconds)
        assertEquals(0, p.elapsedSeconds)
        assertEquals(listOf(1500, 3300), p.milestoneSeconds)
        assertEquals(listOf(1500, 1800, 3300, 4200), p.segments.map { it.endSeconds })
        assertEquals(
            listOf(
                CycleSegmentKind.Focus,
                CycleSegmentKind.ShortBreak,
                CycleSegmentKind.Focus,
                CycleSegmentKind.LongBreak,
            ),
            p.segments.map { it.kind },
        )
    }

    @Test
    fun `elapsed accumulates finished segments plus current progress`() {
        // 12:34 (754s) remaining of first focus -> 746s elapsed.
        val p = cycleProgress(items(), currentIndex = 0, segmentRemainingSeconds = 754)
        assertEquals(746, p.elapsedSeconds)
        // Mid short break: full focus plus 108s into the 300s break.
        val q = cycleProgress(items(), currentIndex = 1, segmentRemainingSeconds = 192)
        assertEquals(1608, q.elapsedSeconds)
    }

    @Test
    fun `out of range index clamps elapsed to total`() {
        val p = cycleProgress(items(), currentIndex = 9, segmentRemainingSeconds = 0)
        assertEquals(4200, p.elapsedSeconds)
    }

    @Test
    fun `single focus cycle has no milestones`() {
        val single = listOf(TimerSessionItem(SessionItemType.Focus(25 * 60)))
        val p = cycleProgress(single, currentIndex = 0, segmentRemainingSeconds = 900)
        assertEquals(1500, p.totalSeconds)
        assertEquals(600, p.elapsedSeconds)
        assertEquals(emptyList<Int>(), p.milestoneSeconds)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.timer.TimerCycleProgressTest" --console=plain`
Expected: FAIL — `cycleProgress` is unresolved (compilation failure).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerSessionItem

enum class CycleSegmentKind { Focus, ShortBreak, LongBreak }

data class CycleProgressSegment(
    val kind: CycleSegmentKind,
    /** Cumulative end of this segment in seconds from cycle start. */
    val endSeconds: Int,
)

data class CycleProgress(
    val totalSeconds: Int,
    val elapsedSeconds: Int,
    val segments: List<CycleProgressSegment>,
    /** Cumulative Focus-end boundaries strictly before the cycle end. */
    val milestoneSeconds: List<Int>,
)

/**
 * Maps a cycle to the single 0..[CycleProgress.totalSeconds] scale used by
 * `Notification.ProgressStyle`. [segmentRemainingSeconds] is the current
 * segment's remaining time as already resolved by the caller
 * (wall-clock `endsAtMillis` math while Playing, stored snapshot while Paused).
 */
fun cycleProgress(
    items: List<TimerSessionItem>,
    currentIndex: Int,
    segmentRemainingSeconds: Int,
): CycleProgress {
    var total = 0
    val segments = items.map { item ->
        total += item.type.seconds()
        CycleProgressSegment(kind = item.type.kind(), endSeconds = total)
    }
    var elapsedBefore = 0
    for (i in 0 until currentIndex.coerceIn(0, items.size)) {
        elapsedBefore += items[i].type.seconds()
    }
    val segmentTotal = items.getOrNull(currentIndex)?.type?.seconds() ?: 0
    val elapsed = (elapsedBefore + (segmentTotal - segmentRemainingSeconds)).coerceIn(0, total)
    val milestones = segments
        .filter { it.kind == CycleSegmentKind.Focus && it.endSeconds < total }
        .map { it.endSeconds }
    return CycleProgress(
        totalSeconds = total,
        elapsedSeconds = elapsed,
        segments = segments,
        milestoneSeconds = milestones,
    )
}

private fun SessionItemType.seconds(): Int = when (this) {
    is SessionItemType.Focus -> seconds
    is SessionItemType.ShortBreak -> seconds
    is SessionItemType.LongBreak -> seconds
}

private fun SessionItemType.kind(): CycleSegmentKind = when (this) {
    is SessionItemType.Focus -> CycleSegmentKind.Focus
    is SessionItemType.ShortBreak -> CycleSegmentKind.ShortBreak
    is SessionItemType.LongBreak -> CycleSegmentKind.LongBreak
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.timer.TimerCycleProgressTest" --console=plain`
Expected: PASS (4 tests, 0 failures).

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/CycleProgress.kt purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerCycleProgressTest.kt
git commit -m "android: add pure cycle-to-progress mapper for ProgressStyle notification"
```

---

### Task 2: SDK-gated ProgressStyle builder in TimerNotification

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerNotification.kt`
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerNotificationLegacyTest.kt`

- [ ] **Step 1: Write the failing Robolectric test (locks the below-36 fallback)**

```kotlin
package com.endiar.purrodoro.timer

import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class TimerNotificationLegacyTest {

    @Test
    fun `below API 36 the running notification stays a compat progress bar`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(TimerSessionItem(SessionItemType.Focus(25 * 60))),
                currentIndex = 0,
                remainingSeconds = 754,
                timerState = TimerState.Playing,
            ),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null,
            activeHabitId = null,
            focusType = FocusType.Empty,
        )
        val notification =
            TimerNotification.buildRunningNotification(context, state, System.currentTimeMillis())
        assertTrue((notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0)
        assertEquals(
            NotificationCompat.CATEGORY_PROGRESS,
            notification.category,
        )
    }
}
```

- [ ] **Step 2: Run test to verify it passes before the change (baseline)**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.timer.TimerNotificationLegacyTest" --console=plain`
Expected: PASS — this locks current behavior; the implementation must keep it green.

- [ ] **Step 3: Add the SDK gate and framework builder**

In `TimerNotification.kt`, add imports:

```kotlin
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
```

Change `buildRunningNotification` to delegate on API 36+ (insert at the top of the function body, before `val res = localized(context)`):

```kotlin
if (Build.VERSION.SDK_INT >= 36) {
    return buildProgressRunningNotification(context, state, now)
}
```

Append these private members to the `TimerNotification` object (after `buildCompletionNotification`, before `describe`):

```kotlin
/** Segment colors mirror the Compose tokens (ARGB literals; notification code runs outside Compose). */
private fun segmentColor(kind: CycleSegmentKind): Int = when (kind) {
    CycleSegmentKind.Focus -> 0xFFFF8B6F.toInt()      // DockCoral
    CycleSegmentKind.ShortBreak -> 0xFF8BA05E.toInt() // JourneyGreen
    CycleSegmentKind.LongBreak -> 0xFF9B7FCC.toInt()  // LockedPurpleLight
}

private const val PROGRESS_POINT_COLOR = 0xFFC64432 // DeepCoral

@RequiresApi(36)
private fun buildProgressRunningNotification(
    context: Context,
    state: TimerRuntimeState,
    now: Long
): Notification {
    val res = localized(context)
    val (title, subtitle) = describe(res, state, now)
    val items = state.session.items
    val progress = cycleProgress(items, state.session.currentIndex, currentRemainingSeconds(state, now))
    val style = Notification.ProgressStyle()
        .setStyledByProgress(false)
        .setProgress(progress.elapsedSeconds)
        .setProgressTrackerIcon(
            Icon.createWithResource(context, R.mipmap.ic_launcher_monochrome)
        )
        .setProgressSegments(
            progress.segments.map {
                Notification.ProgressStyle.Segment(it.endSeconds).setColor(segmentColor(it.kind))
            }
        )
        .setProgressPoints(
            progress.milestoneSeconds.map {
                Notification.ProgressStyle.Point(it).setColor(PROGRESS_POINT_COLOR)
            }
        )
    val builder = Notification.Builder(context, CHANNEL_RUNNING)
        .setSmallIcon(R.mipmap.ic_launcher_monochrome)
        .setContentTitle(title)
        .setContentText(subtitle)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setContentIntent(openAppIntent(context))
        .setStyle(style)
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setVisibility(Notification.VISIBILITY_PUBLIC)

    when (state.session.timerState) {
        TimerState.Playing -> {
            builder.addAction(frameworkAction(context, res.getString(R.string.notif_action_pause), TimerService.ACTION_PAUSE))
            builder.addAction(frameworkAction(context, res.getString(R.string.notif_action_stop), TimerService.ACTION_STOP))
            if (isOnBreak(state)) {
                builder.addAction(frameworkAction(context, res.getString(R.string.notif_action_skip), TimerService.ACTION_SKIP))
            }
        }

        TimerState.Paused -> {
            builder.addAction(frameworkAction(context, res.getString(R.string.notif_action_resume), TimerService.ACTION_RESUME))
            builder.addAction(frameworkAction(context, res.getString(R.string.notif_action_stop), TimerService.ACTION_STOP))
        }

        TimerState.Idle -> Unit
    }
    return builder.build()
}

@RequiresApi(36)
private fun frameworkAction(
    context: Context,
    label: String,
    intentAction: String
): Notification.Action {
    // Null icon mirrors the compat path, which builds text actions with icon 0.
    val intent = Intent(context, TimerService::class.java).setAction(intentAction)
    val pi = PendingIntent.getService(
        context,
        intentAction.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return Notification.Action.Builder(null, label, pi).build()
}
```

No `TimerService` change is needed: `refreshOngoingNotification` (`TimerService.kt:394`) already rebuilds via `buildRunningNotification` on every tick, pause, resume, skip, and segment change.

- [ ] **Step 4: Run the timer tests**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.timer.*" --console=plain`
Expected: PASS — mapper tests, legacy fallback test, and all existing reconciler/state-machine tests green.

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/timer/TimerNotification.kt purrodoro-android/app/src/test/java/com/endiar/purrodoro/timer/TimerNotificationLegacyTest.kt
git commit -m "android: render running timer with ProgressStyle on API 36+"
```

---

### Task 3: Full verification

- [ ] **Step 1: Run the full unit suite and a debug build**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL, 0 test failures.

- [ ] **Step 2: Manual QA on a real API 36+ device (not emulated in unit tests)**

  - Start a cycle → shade shows the segmented bar with the tracker advancing; lockscreen shows the same card.
  - Pause → bar freezes; Resume/Stop offered. Skip on a break → segments advance.
  - Finish a Focus → its milestone point fills. Finish the cycle → completion notification unchanged.
  - On an API 28–35 device → old progress bar renders exactly as before.
  - With in-app language set to Indonesian → title/body resolve in Indonesian (existing `localized()` path).

No commit — verification only. Push only when the user asks.
