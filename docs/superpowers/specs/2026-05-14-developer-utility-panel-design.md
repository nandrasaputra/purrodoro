# Developer Utility Panel — Design Spec

**Date:** 2026-05-14  
**Status:** Approved

---

## Overview

A debug-only developer utility panel for Kuro Pomodoro Android. Accessible via a persistent notification (debug builds only). Tapping the notification opens a `DebugActivity` where the developer can manipulate app state in real time — overriding the clock, speeding up the timer, injecting fake pomodoro sessions, and resetting all data.

Zero debug code is present in release builds.

---

## Entry Point

- On app start (`Application.onCreate()`), debug builds post a persistent low-priority notification: **"Kuro Dev Tools — Tap to open debug panel"**
- Tapping opens `DebugActivity` as a new task
- Notification is `ongoing` (cannot be dismissed by swipe)
- Notification channel id: `kuro_dev`, importance: `LOW`

---

## Architecture

### `AppClock` — main source set (both builds)

```
interface ClockDebugSource {
    val clockOverride: StateFlow<LocalTime?>
    val speedUpTimer: StateFlow<Boolean>
}

object AppClock {
    var source: ClockDebugSource? = null
    fun now(): LocalTime = source?.clockOverride?.value ?: LocalTime.now()
    fun tickIntervalMs(): Long = if (source?.speedUpTimer?.value == true) 25L else 250L
}
```

- `source` is `null` in release builds — `AppClock` behaves identically to `LocalTime.now()`
- `KuroPomodoroApp.kt` replaces `LocalTime.now()` call sites with `AppClock.now()`
- The timer `LaunchedEffect` replaces `delay(250)` with `delay(AppClock.tickIntervalMs())`

### `DebugRepository` — debug source set only

```
class DebugRepository : ClockDebugSource {
    override val clockOverride = MutableStateFlow<LocalTime?>(null)
    override val speedUpTimer  = MutableStateFlow(false)

    fun setClockOverride(time: LocalTime?)
    fun setSpeedUpTimer(enabled: Boolean)

    companion object {
        lateinit var instance: DebugRepository
        fun init(): DebugRepository
    }
}
```

### `KuroDebugApplication` — debug source set only

```
class KuroDebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val debugRepo = DebugRepository.init()
        AppClock.source = debugRepo
        postDevNotification(this)
    }
}
```

Declared in the debug `AndroidManifest.xml` as `android:name=".KuroDebugApplication"`, which merges with the main manifest and overrides the `Application` class for debug builds.

### `DebugActivity` — debug source set only

- Declared in debug `AndroidManifest.xml`: `android:exported="false"`
- Jetpack Compose activity using Material3
- Collects `DebugRepository.clockOverride` and `speedUpTimer` via `collectAsStateWithLifecycle`
- Calls `AppRepository.get(context)` for persistent writes

---

## Debug Actions

### 1. Clock Override (in-memory)

**Purpose:** Preview cat backdrop and glass treatment at different hours without waiting.

**UI:** Four preset time buttons: `08:00` (morning), `12:00` (day), `17:00` (evening), `22:00` (night). Current override shown. `[Clear]` button resets to `null` (real time).

**Behavior:** Sets `DebugRepository.clockOverride`. The main app re-renders immediately on next recomposition tick — `backdropTime = AppClock.now()` in `KuroPomodoroApp.kt` picks up the new value.

**Persistence:** In-memory only. Clears on process death.

---

### 2. Timer Speed-Up (in-memory)

**Purpose:** Complete a full focus/break session in seconds instead of minutes.

**UI:** Toggle switch labeled "Speed up timer". Subtitle: "25 min → ~150 seconds".

**Behavior:** When enabled, `AppClock.tickIntervalMs()` returns `25L` instead of `250L`. The existing timer `LaunchedEffect` ticks 10× faster. All session-complete logic (stamp write, mode transition) fires naturally at countdown end — no special handling.

**Persistence:** In-memory only. Clears on process death.

---

### 3. Session Injection (persistent)

**Purpose:** Populate the Calendar screen with historical data for testing.

**UI:**
- Date picker defaulting to today (tapping opens a date picker dialog)
- Count stepper: `[−] N [+]`, range 1–10
- `[Add Sessions]` button

**Behavior:** On tap:
1. Reads current `stamps` and `completedSessions` from `AppRepository` flow (latest value)
2. Adds `N` to the stamp count for the selected date via `addFocusStamp()` called N times
3. Increments `completedSessions` by `N`
4. Writes both back via `AppRepository.setStamps()` and `AppRepository.setCompletedSessions()`

**Persistence:** Persistent — writes to DataStore exactly as a real completed session would.

---

### 4. Reset All Data (persistent)

**Purpose:** Wipe all persisted state back to seed values for a clean test run.

**UI:** `[Reset All Data]` button in a "Danger Zone" section. On first tap, label changes to "Are you sure? Tap again." Second tap executes. State resets on next tap elsewhere.

**Behavior:** Calls `AppRepository` setters:
- `setStamps(emptyMap())`
- `setCompletedSessions(0)`
- `setTodoLists(emptyList())` — DataStore will fall back to `SEED_TODO_LISTS` on next read

**Persistence:** Persistent — clears DataStore.

---

## File Structure

```
app/src/
  main/
    java/com/endiar/kuropomodoro/
      model/
        AppClock.kt              ← new: ClockDebugSource interface + AppClock object
      KuroPomodoroApp.kt         ← modified: LocalTime.now() → AppClock.now(),
                                              delay(250) → delay(AppClock.tickIntervalMs())
  debug/
    java/com/endiar/kuropomodoro/
      debug/
        DebugRepository.kt       ← new
        KuroDebugApplication.kt  ← new
        DebugActivity.kt         ← new
        DebugNotification.kt     ← new: postDevNotification()
    AndroidManifest.xml          ← new: declares KuroDebugApplication + DebugActivity
```

---

## What is NOT in scope

- No debug tab in the bottom nav
- No shake/gesture entry point
- No debug UI theming (plain Material3 is fine)
- No network/API mocking
- No Logcat / log viewer
- No crash reporting tools

---

## Release build guarantee

`AppClock.source` is never set in release builds — it stays `null`. All debug classes live exclusively in the `debug` source set and are not compiled into the release APK. `BuildConfig.DEBUG` checks are not required anywhere in production code paths.
