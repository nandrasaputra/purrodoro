# Session History Design

**Date:** 2026-05-14
**Status:** Approved

---

## Overview

Add persistent per-cycle session records to the Records screen. Each completed or abandoned Pomodoro cycle is saved to DataStore and displayed below the calendar heatmap, filtered by the selected date.

---

## Data Model

### `SessionStatus` (enum)
```kotlin
enum class SessionStatus { Completed, Partial }
```

### `SessionRecord` (data class)
```kotlin
data class SessionRecord(
    val id: Long,                      // System.currentTimeMillis() at record-open time
    val startedAt: Long,               // epoch millis
    val endedAt: Long,                 // epoch millis
    val focusMinutes: Int,             // per-focus-block duration from settings
    val shortBreakMinutes: Int,        // per-short-break duration from settings
    val longBreakMinutes: Int,         // per-long-break duration from settings
    val completedFocusCount: Int,      // focus blocks that hit 0 naturally this cycle
    val totalFocusCount: Int,          // focus blocks in the full cycle (e.g. 4)
    val status: SessionStatus,
    val taskName: String?              // snapshot of activeTask?.title at close time
)
```

---

## Recording Logic (in `PurrodoroApp.kt`)

### Open a record
- Trigger: `isStarted` transitions `false → true` (user presses Play for the first time in a fresh session)
- Action: create an in-memory `openRecord` with `id = startedAt = System.currentTimeMillis()`, snapshot `focusMinutes/shortBreakMinutes/longBreakMinutes` from current settings, `totalFocusCount` from `cycleItems.count { it == TimerMode.Focus }`, `completedFocusCount = 0`

### Increment focus count
- Trigger: a Focus block's timer reaches 0 naturally (i.e. auto-advances to the next item)
- Action: `openRecord = openRecord.copy(completedFocusCount = completedFocusCount + 1)`

### Close a record
Two closing paths:

| Path | `status` | Trigger |
|------|----------|---------|
| Natural completion | `Completed` | Last item in `cycleItems` completes (cycle resets) |
| Stop | `Partial` | User confirms "Stop for now" in the stop dialog |

On close:
1. `endedAt = System.currentTimeMillis()`
2. `taskName = activeTask?.title`
3. Append `SessionRecord` to DataStore list
4. Clear `openRecord` (set to null)

---

## Persistence (in `AppRepository.kt`)

- Key: `Preferences.Key<String>("session_records")`
- Serialization: JSON via `kotlinx.serialization` (same pattern as existing todo lists)
- Flow: `sessionRecordsFlow: Flow<List<SessionRecord>>`
- Write: `suspend fun saveSessionRecord(record: SessionRecord)` — appends to list, never purges

---

## UI (in `CalendarScreen.kt`)

### Calendar as date picker
- Default selected date: today
- Tapping a calendar day updates `selectedDate: LocalDate` state
- Selected day cell: accent fill `#FF8B6F` + white border (already shown in mockup)

### Session section (below calendar)
- Section header: `"Today · May 14"` style label (`#ffffff99`, 13sp semi-bold)
- Sessions list: filtered by `selectedDate`, sorted newest-first
- Empty state: small centered text `"No sessions on this day"`

### Session card
```
┌──────────────────────────────────────────┐
│ 2:10 PM – 3:02 PM        [Completed]     │
│ ● ● ● ●  4 / 4 focus · 25 min each      │
│ Break · 5 min short + 15 min long        │
│ [📌 Write unit tests]                    │
└──────────────────────────────────────────┘
```
- Background: glass card `#ffffff14`, stroke `#ffffff26`, cornerRadius 16
- Time range: white 13sp semi-bold
- Badge: green `#4CAF50` / `#4caf5033` for Completed; accent `#FF8B6F` / `#ff8b6f33` for Partial
- Focus dots: filled `●` for completed, empty `○` for incomplete; count `X / Y focus · N min each`
- Break line: `Break · X min short + Y min long` (`#ffffff8c`)
- Task chip (optional): `#ffffff1a` pill, only shown if `taskName != null`

---

## Non-Goals / Out of Scope
- No delete or edit of session records
- No export
- `stamps: Map<LocalDate, Int>` heatmap remains unchanged (session history is additive)
- `Map<LocalDate, FocusStamp>` in `TimerSessionReducer` is disconnected from the main app — ignored

---

## Mockup
Pencil frame: `Fw23P` in `/Users/nandra.saputra/Projects/nandra/pen file/kuro-pen.pen`
