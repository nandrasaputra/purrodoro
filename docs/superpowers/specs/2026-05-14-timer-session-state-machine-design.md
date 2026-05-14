# Timer Session State Machine — Design Spec

**Date:** 2026-05-14
**Status:** Approved

---

## Background

The current timer implementation uses scattered `rememberSaveable` variables (`isRunning`, `timerMode`, `remainingSeconds`, etc.) with no formal state machine. There is no distinction between "paused" and "stopped", the sequence only partially auto-advances (Focus → ShortBreak only), and the UI provides no way for the user to see where they are in the Pomodoro cycle.

This spec defines a clean session state machine, explicit sequence list, and the UI changes needed to surface the cycle to the user.

---

## Goals

- Explicit `Idle / Playing / Paused` timer states
- Automatic sequence progression through a full 4-round Pomodoro cycle
- User can see their position in the cycle at all times
- Stop exits the whole cycle; Skip advances to the next item
- Unfinished focus sessions are recorded separately from completed ones
- Auto-start next session is configurable (default off)

---

## Section 1 — Data Model

### Session item types

```kotlin
sealed class SessionItemType {
    data class Focus(val seconds: Int) : SessionItemType()
    data class ShortBreak(val seconds: Int) : SessionItemType()
    data class LongBreak(val seconds: Int) : SessionItemType()
}

data class TimerSessionItem(val type: SessionItemType)
```

### Timer state

```kotlin
enum class TimerState { Idle, Playing, Paused }
```

### Session

```kotlin
data class TimerSession(
    val items: List<TimerSessionItem>,  // full ordered schedule for the cycle
    val currentIndex: Int,              // active item; equals items.size when cycle is done
    val remainingSeconds: Int,          // countdown for the current item
    val timerState: TimerState,         // Idle = not started or fully stopped
)
```

### Derived properties

- `isActive = timerState != Idle`
- `isComplete = currentIndex >= items.size`
- `currentItem = items.getOrNull(currentIndex)`

### Standard cycle (built from TimerSettings)

```
Focus(focusSeconds)
ShortBreak(shortBreakSeconds)
Focus(focusSeconds)
ShortBreak(shortBreakSeconds)
Focus(focusSeconds)
ShortBreak(shortBreakSeconds)
Focus(focusSeconds)
LongBreak(longBreakSeconds)
```

8 items total. Built by a pure `buildSession(settings: TimerSettings): TimerSession` function. `TimerSession` is not persisted — rebuilt on each app start or after Stop.

---

## Section 2 — State Transitions

### Transition table

| Current state | Action | Next state | Notes |
|---|---|---|---|
| `Idle` | Start | `Playing` | currentIndex = 0 |
| `Playing` | Pause | `Paused` | remainingSeconds preserved |
| `Paused` | Resume | `Playing` | continues from remainingSeconds |
| `Playing` | timer hits 0 | `Paused` (next item) | auto-start if configured; record stamp |
| `Playing` | Skip | `Paused` (next item) | auto-start if configured; record stamp if Focus |
| `Paused` | Skip | `Paused` (next item) | record stamp if Focus |
| `Playing` | Stop | `Idle` | currentIndex = 0; record stamp if mid-Focus |
| `Paused` | Stop | `Idle` | currentIndex = 0; record stamp if mid-Focus |
| `Playing` | timer hits 0 on last item | `Idle` | cycle complete; record completed stamp |

### Stamp side effects

| Trigger | Stamp recorded |
|---|---|
| Focus timer hits 0 naturally | `completed += 1` |
| Stop mid-Focus (remainingSeconds > 0) | `unfinished += 1` |
| Skip mid-Focus (remainingSeconds > 0) | `unfinished += 1` |
| Skip Focus that already hit 0 | already recorded, no duplicate |
| Stop or Skip mid-Break | no stamp |
| Cycle fully completes | completed stamp already recorded when Focus hit 0 |

---

## Section 3 — UI Changes

### Sequence indicator

Replaces the manual mode chips (`Focus / Short Break / Long Break`).

- 4 dots representing the 4 Focus sessions in the cycle
- Filled `●` = completed focus session
- Current `◉` = active item (Focus or Break)
- Empty `○` = not yet reached
- Label beside dots shows current item name and position: `Focus 3 of 4`, `Short Break`, `Long Break`

### Timer screen layout

```
Kuro focus time
A soft timer for one cozy session at a time

● ◉ ○ ○  Focus 2 of 4          ← sequence indicator

┌─────────────────────────────┐
│     🐱 CatCompanion         │
│                             │
│          23:41              │
│       stay with kuro        │
│                             │
│   5 done · 2 unfinished     │  ← today's stamp summary
│                             │
│       [Pause / Resume]      │  ← primary button
│  [Skip]          [Stop]     │  ← secondary, hidden when Idle
└─────────────────────────────┘

Current task card
```

### Button states

| `timerState` | Primary button | Secondary controls |
|---|---|---|
| `Idle` | **Start** | hidden |
| `Playing` | **Pause** | Skip · Stop |
| `Paused` | **Resume** | Skip · Stop |
| Cycle complete | **Start** (new cycle) | hidden |

### Settings screen addition

New toggle in ProfileScreen:

```
Auto-start next session    [OFF]
```

Default: off. When on, advancing to the next item (timer hits 0 or Skip) automatically starts Playing instead of stopping at Paused.

---

## Section 4 — Data Persistence

### FocusStamp

Replace `Map<LocalDate, Int>` with:

```kotlin
data class FocusStamp(
    val completed: Int,
    val unfinished: Int
)

// stored as Map<LocalDate, FocusStamp>
```

### Migration

Existing DataStore entries encoded as plain int counts decode as:
```kotlin
FocusStamp(completed = oldCount, unfinished = 0)
```

### TimerSettings extension

```kotlin
data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val autoStartNextSession: Boolean = false   // new
)
```

### TimerSession persistence

`TimerSession` is **not persisted**. It is rebuilt from `TimerSettings` on app start. Mid-session state is lost on app backgrounding — consistent with current behaviour.

---

## Section 5 — Testing

### Session builder
- `buildSession(settings)` produces correct 8-item list in correct order
- Focus durations match `settings.focusMinutes * 60`
- Break durations match their respective settings fields

### State machine transitions
- All transitions are pure functions: `(TimerSession, Action) → TimerSession`
- Cover every row in the transition table
- Cover stamp side effects: natural completion, stop mid-Focus, skip mid-Focus, skip/stop mid-Break
- Cover auto-start config: next item starts Playing vs Paused

### Stamp persistence
- `encodeStamps` / `decodeStamps` round-trips `Map<LocalDate, FocusStamp>`
- Old format (plain int) decodes correctly as migration path

### Obsolete tests
- `KuroPomodoroAppTest.kt` — `timerDisplayTotalSeconds` and `timerStartTotalSeconds` are replaced by state machine tests above

---

## Out of Scope

- Persisting mid-session state across app backgrounding
- Custom cycle length (number of rounds) — fixed at 4 for now
- Per-task session tracking changes (handled by existing `FocusTask.plannedSessions` / `completedSessions`)
