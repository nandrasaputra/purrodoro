# Background Timer Persistence Design

**Date:** 2026-05-14
**Status:** Approved

---

## Overview

The timer currently lives entirely inside `PurrodoroApp.kt` Compose state. When the user closes the app (task-swipe or process death), the running timer is lost: state is not preserved, the cycle does not advance, and no notification is shown when a Focus or Break segment ends.

This spec makes the Pomodoro timer **truly run in the background**, with:

- A foreground service that owns timer state for the lifetime of an active cycle.
- A live-updating ongoing notification with **Pause / Resume / Stop / Skip** actions and a separate completion notification when a segment ends.
- An `AlarmManager` backstop to guarantee the user is notified at the exact second a segment ends, even if the service is killed by the OS.
- Deterministic state reconciliation when the app is reopened: the displayed timer matches what the user expects based on real wall-clock time, even after process death.

The existing `TimerSessionReducer` (currently dead code) becomes the single source of truth for state transitions — both the service and reconciliation logic call it.

---

## Architecture

New package `com.endiar.purrodoro.timer/` is introduced. Compose UI becomes a thin observer of service state.

```
com.endiar.purrodoro/
├── timer/                          ← NEW package
│   ├── TimerService.kt             ← Foreground service, owns runtime state
│   ├── TimerController.kt          ← Bound API: start/pause/resume/stop/skip
│   ├── TimerConnection.kt          ← UI-side bind helper, exposes StateFlow
│   ├── TimerNotification.kt        ← Notification builder + action intents
│   ├── TimerReconciler.kt          ← Pure: (snapshot, now) → restored state
│   └── TimerAlarmReceiver.kt       ← AlarmManager backstop receiver
├── model/
│   ├── TimerSessionReducer.kt      ← Existing; finally wired
│   ├── TimerRuntimeState.kt        ← NEW: runtime wrapper around TimerSession
│   └── KuroModels.kt               ← Unchanged
├── data/
│   └── AppRepository.kt            ← Extended with runtime-snapshot persistence
└── PurrodoroApp.kt              ← Slims down ~150 lines; timer state moves out
```

---

## Data Model

### `TimerRuntimeState` (new, in `model/TimerRuntimeState.kt`)

```kotlin
data class TimerRuntimeState(
    val session: TimerSession,          // existing: items, currentIndex, remainingSeconds, timerState
    val endsAtMillis: Long?,            // wall-clock end of current segment; null when paused/idle
    val activeTask: FocusTask?,         // task driving the current focus (if any)
    val openRecord: SessionRecord?      // moved from PurrodoroApp
)
```

**Invariants:**

- `timerState == Playing` ⇒ `endsAtMillis != null`.
- `remainingSeconds` is **derived** from `endsAtMillis − now()` while running; **stored** while paused.
- After every transition, the service writes a `TimerRuntimeState` snapshot to DataStore.

---

## State Transitions

All state changes go through the existing reducer `model/TimerSessionReducer.kt`. No new transition logic is introduced; the reducer is wired into the service.

| User action | Reducer action | Service side effect |
|---|---|---|
| Tap **Focus Now** (idle) | `Start` | `startForeground()`, post notification, schedule alarm |
| Tap **Pause** | `Pause` | Cancel alarm; snapshot `remainingSeconds`; update notification |
| Tap **Resume** | `Resume` | Compute new `endsAtMillis`; schedule alarm; update notification |
| Tap **Skip** (break only) | `Skip` | Cancel old alarm; schedule alarm for next segment |
| Tap **Stop** | `Stop` | Persist `SessionRecord` (Partial); `stopForeground`; `stopSelf` |
| Segment timer hits 0 | `TimerComplete` | Persist stamp/record updates; if `autoStartNextSession` → continue; else → `Paused`; reschedule or stop alarm |

### Stamp handling at the reducer boundary

The reducer's `ReducerResult` returns `stamps: Map<LocalDate, FocusStamp>` (a richer counted shape with `completed` and `unfinished`), but the app's persisted heatmap uses `Map<LocalDate, Int>` (a flat completed-counter). The service does **not** use the reducer's stamps return value. Instead:

- The service calls `reduce(...)` purely for its `session` output.
- When the reducer applies `TimerComplete` on a Focus segment, the service itself increments the persisted `Int` stamp for today (matching the existing `PurrodoroApp` behavior).
- The reducer's `FocusStamp` model remains internal-to-reducer dead code for now. Unifying the stamp model is explicitly out of scope (see Out of Scope).

### Shared state-transition logic between service and alarm receiver

Both `TimerService` and `TimerAlarmReceiver` need to apply `TimerComplete` and persist results. To avoid duplication, a small helper object `TimerStateMachine` (in the `timer/` package) wraps:

```kotlin
suspend fun applyTimerComplete(
    snapshot: TimerRuntimeState,
    settings: TimerSettings,
    now: Long,
    repo: AppRepository
): TimerRuntimeState
```

It runs the reducer, updates the persisted `Map<LocalDate, Int>` stamp, appends any closed `SessionRecord`, persists the new runtime snapshot, and returns the new state. Both the service's tick loop and the alarm receiver call this single function.

---

## Service Lifecycle

`TimerService` is a started + bound foreground service.

**Started state machine:**

```
NOT_RUNNING ──"Focus Now"──▶ RUNNING (Playing) ──"Pause"──▶ RUNNING (Paused)
     ▲                            │                              │
     │                            │                              │
     │                            │"Stop" or cycle complete      │"Resume"
     │                            │                              ▼
     └────────────────────────────┴───────────────  RUNNING (Playing)
```

- **Created:** by `startForegroundService(intent)` when the user taps Focus Now (or resumes from a reconciled paused state).
- **Foreground promotion:** `startForeground(id, notification)` is called within the first few ms of `onStartCommand`. Android's 5-second budget is comfortably met.
- **Bound:** the UI binds via `bindService(...)` to observe `StateFlow<TimerRuntimeState>` and dispatch controller actions. Service stays foreground even when no UI is bound.
- **Stopped:** when (a) user explicitly stops, (b) full cycle ends and `autoStartNextSession == false`, (c) full cycle ends and the final long break completes.
- **Task removal (swipe):** service stays alive in foreground. Notification keeps ticking.

The service runs a single coroutine that loops `delay(1000)` solely to refresh the notification countdown. **All time math reads `endsAtMillis − now()`**, so coroutine pauses or dropped frames cannot cause drift.

---

## AlarmManager Backstop

Foreground services can still be killed on aggressive OEM ROMs and under low memory. An exact alarm is scheduled in parallel as a guarantee.

- **On every Play / Resume / Skip / segment-advance:** `alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, endsAtMillis, pendingIntent)` targeting `TimerAlarmReceiver`.
- **On every Pause / Stop:** cancel the pending alarm.
- **When alarm fires** (`TimerAlarmReceiver.onReceive`):
  1. Load `TimerRuntimeState` snapshot from DataStore.
  2. Apply `reduce(state, TimerComplete, settings, stamps, today)`.
  3. Persist the new snapshot, stamps, and any closed `SessionRecord`.
  4. Post a completion notification (separate channel, importance `DEFAULT`).
  5. If `autoStartNextSession == true` and there is a next segment, schedule the next alarm.
  6. If the service is alive, broadcast the new state; if dead, the next app open triggers reconciliation.

We use `USE_EXACT_ALARM` (auto-granted, Android 13+) rather than `SCHEDULE_EXACT_ALARM` (user-grantable). Exact timing is core to a Pomodoro app's function, satisfying Google's policy for `USE_EXACT_ALARM`.

---

## Notification UX

A single ongoing notification (channel `timer_running`, importance `LOW` — silent, ticks). Content varies by state:

| State | Title | Subtitle / Body | Actions |
|---|---|---|---|
| Focus running | `Focus · 12:34` | task name + progress (e.g. *"Review Compose layout · 1 / 4"*) | **Pause**, **Stop** |
| Short break running | `Short Break · 03:21` | *"Up next: Focus"* | **Pause**, **Stop**, **Skip** |
| Long break running | `Long Break · 14:55` | *"Up next: new cycle"* | **Pause**, **Stop**, **Skip** |
| Paused | `Focus · Paused` | *"12:34 remaining"* | **Resume**, **Stop** |

A second channel `timer_completion` (importance `DEFAULT`) is used for transient pings:

| Trigger | Title | Subtitle | Action |
|---|---|---|---|
| Segment ends, auto-advance off | `Focus complete` | *"Tap to start your break"* | **Start break** |
| Cycle ends | `Cycle complete` | *"4 focus sessions today"* | (dismiss) |

- Tap the notification body → opens `MainActivity`.
- Each action button targets `TimerService` via a `PendingIntent` carrying an action extra (`ACTION_PAUSE`, `ACTION_RESUME`, `ACTION_STOP`, `ACTION_SKIP`).
- The expanded view shows a progress bar (`setProgress(total, elapsed, false)`).

---

## UI ↔ Service Contract

### `TimerConnection` (UI-side helper)

```kotlin
class TimerConnection(private val context: Context) {
    val state: StateFlow<TimerRuntimeState?>   // null when service not bound
    val controller: TimerController?           // null when not bound

    fun bind(lifecycle: Lifecycle)
    fun unbind()
}
```

- Created once at `MainActivity` scope, bound for the activity's lifetime.
- `state` emits `null` when the service is not running and there is no persisted open session.
- Underneath: a `ServiceConnection` plus a `Binder` exposed by `TimerService` that returns the service's internal `StateFlow`.

### `TimerController` (bound API)

```kotlin
interface TimerController {
    fun start(settings: TimerSettings, task: FocusTask?)
    fun pause()
    fun resume()
    fun stop()
    fun skip()
}
```

All methods dispatch to the service; the service is the only object that mutates state.

### Composable side

```kotlin
@Composable
fun PurrodoroApp(repo: AppRepository, connection: TimerConnection) {
    val runtimeState by connection.state.collectAsStateWithLifecycle(null)
    // when runtimeState != null → render running/paused dashboard
    // when null → render idle dashboard with "Focus Now" CTA
}
```

---

## State Reconciliation on App Open

The critical correctness path. When `MainActivity` resumes:

```
on Activity resume:
    1. attempt to bind TimerService
    2. if service is alive:
        → state flow emits current TimerRuntimeState; remaining is computed from endsAtMillis − now()
        → done
    3. if service is dead:
        load TimerRuntimeState snapshot from DataStore
        if snapshot is null OR snapshot.session.timerState == Idle:
            → emit null; done
        else:
            reconciled = TimerReconciler.reconcile(snapshot, now, settings)
            persist reconciled
            if reconciled.session.timerState == Playing:
                → startForegroundService() with intent flag RESUME_FROM_RECONCILED
            else (Paused or cycle complete):
                → emit reconciled; no service start until user action
```

### `TimerReconciler.reconcile` (pure function)

```kotlin
fun reconcile(
    snapshot: TimerRuntimeState,
    now: Long,                          // epoch millis; today is derived as Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    settings: TimerSettings
): ReconcileResult                      // (state, stampsToIncrement, closedRecords)
```

`stampsToIncrement: List<LocalDate>` is a flat list of dates (one entry per Focus segment that completed during the reconciliation walk). The caller (service or alarm receiver) folds these into the persisted `Map<LocalDate, Int>`. Keeping the reconciler's output as a list of dates — rather than a map — keeps it pure and decoupled from the persisted shape.

Algorithm:

```
state = snapshot
stampsToIncrement = empty list
closedRecords = empty list
while state.session.timerState == Playing
      && state.endsAtMillis != null
      && now >= state.endsAtMillis:

    completionMoment = state.endsAtMillis
    dateForStamp = LocalDate from completionMoment in system zone

    if state.session.currentItem.type is Focus:
        stampsToIncrement.append(dateForStamp)
        state.openRecord = state.openRecord?.copy(
            completedFocusCount = openRecord.completedFocusCount + 1
        )

    apply reduce(state.session, TimerComplete) → newSession (ignore stamps return)

    if newSession.currentIndex >= newSession.items.size:
        // end-of-cycle
        closedRecords.append(state.openRecord.copy(
            endedAt = completionMoment,
            status = Completed
        ))
        state = state.copy(session = Idle session, endsAtMillis = null, openRecord = null)
        break

    if settings.autoStartNextSession:
        state.endsAtMillis = completionMoment + duration(newSession.currentItem) * 1000
        state.session = newSession with timerState = Playing
    else:
        state.session = newSession with timerState = Paused
        state.endsAtMillis = null
        break

return ReconcileResult(state, stampsToIncrement, closedRecords)
```

This is a pure function. No Android dependencies. Fully unit-testable.

---

## Persistence (in `AppRepository.kt`)

A new preference key:

```kotlin
private val KEY_RUNTIME_SNAPSHOT = stringPreferencesKey("timer_runtime_snapshot")
```

Snapshot encoding follows the existing repo convention (pipe-separated, URL-encoded fields). The full encoding for `TimerRuntimeState`:

```
<timerState>|<currentIndex>|<remainingSeconds>|<endsAtMillis>|<activeTaskOrEmpty>|<openRecordOrEmpty>|<itemsCsv>
```

`itemsCsv` reuses the existing cycle-item encoding (`F:1500,S:300,F:1500,L:900`).

`appendSessionRecord` and `setStamps` already exist; the service uses them on `TimerComplete` and `Stop`.

---

## Permissions & Manifest

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

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
```

**Runtime permission flow:** request `POST_NOTIFICATIONS` on first "Focus Now" tap (Android 13+). On denial, show a one-time rationale. The timer still functions if denied — Android promotes the foreground service notification automatically, but the notification will be system-managed (less rich).

---

## Migration of `PurrodoroApp.kt`

**Removed:**

- `LaunchedEffect(isRunning, timerMode, settings) { ... }` timer loop (lines ~252–354).
- `remember`/`rememberSaveable` for `remainingSeconds`, `cycleItems`, `cycleIndex`, `timerStartedAtMillis`, `timerStartedRemainingSeconds`, `activeTotalSeconds`, `isRunning`, `isStarted`.
- The cycle-advance, stamp-update, and record-update logic inside `onStartPause`, `onStop`, and `onSkip` lambdas.

**Replaced with:**

- `val runtimeState by connection.state.collectAsStateWithLifecycle(null)` — drives the dashboard.
- `onStartPause = { connection.controller?.start(settings, activeTask) ?: ... }` (or `pause()` / `resume()` based on current state).
- `onStop = { connection.controller?.stop() }`.
- `onSkip = { connection.controller?.skip() }`.

**Stays in the composable** (pure UI state): `activeTask`, `isFocusMode`, `selectedTab`, `premiumSheetMessage`, `playingSoundId`, the saver definitions.

**Net change:** `PurrodoroApp.kt` drops from ~930 lines to ~400. The duplicated state between `PurrodoroApp` and DataStore disappears.

---

## Testing Strategy

### Unit tests (`app/src/test/java/...`, currently empty)

- `TimerSessionReducerTest` — every `TimerAction` × every `TimerState` combination, including stamp accumulation on Focus completion.
- `TimerReconcilerTest` — pure function tests:
  - Reopened mid-segment, no auto-advance → state unchanged, recompute remaining.
  - Reopened after one segment ended, auto-advance on → advanced by one segment.
  - Reopened after multiple segments ended → advanced through the chain.
  - Reopened after full cycle ended → idle with Completed record persisted.
  - Reopened after segment end, auto-advance off → Paused at the next segment with full duration.
- `AppRepositoryTest` — round-trip encode/decode of `TimerRuntimeState`.

### Manual checklist (in implementation plan, not this spec)

- Start focus → close app → reopen → state matches.
- Start focus → swipe app away → notification ticks for 25 min → completion notification fires.
- Pause from notification → reopen app → UI shows Paused.
- Resume from notification → reopen app → UI shows Running.
- Decline `POST_NOTIFICATIONS` → timer still functions; system-managed notification appears.
- Notification action **Skip** disabled during Focus, enabled during breaks.
- Lock screen during running timer → notification visible and updating.

---

## Out of Scope

- **Ambient audio playback while in background.** Separate spec; will hook into this same service later.
- **Survive device reboot.** Would require a `BOOT_COMPLETED` receiver to re-schedule alarms. Deferred to v2.
- **Rich lock-screen media controls (`MediaSession`).** Deferred.
- **Widget / Tile / Wearable integrations.** Deferred.
- **OEM battery-whitelist guidance** (Xiaomi/Oppo auto-start permissions). Document in README only.
- **Removing the existing `Map<LocalDate, Int>` stamps in favor of `FocusStamp`.** Reducer's richer `FocusStamp` model is used internally for reconciliation, but the persisted heatmap stays on the current `Int`-keyed shape to avoid migration churn in this spec.
