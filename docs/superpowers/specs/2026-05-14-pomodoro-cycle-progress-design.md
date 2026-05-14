# Pomodoro Cycle Progress — Design Spec

**Date:** 2026-05-14
**Status:** Approved

---

## Overview

Introduce a configurable Pomodoro cycle (N focus sessions before a long break) with visible dot progress on the timer screen and a skip button for breaks.

---

## Data Model

### `TimerSettings` — new field

```kotlin
val focusSessionsPerCycle: Int = 4
```

Default is 4, matching standard Pomodoro. Valid range: 1–10 (coerced on update).

### `buildCycle(settings: TimerSettings): List<TimerSessionItem>`

New top-level function in `KuroModels.kt`. Builds the full cycle sequence from settings.

For `focusSessionsPerCycle = N`, the pattern is:
- Repeat `(Focus, ShortBreak)` × (N − 1)
- Then `Focus, LongBreak`

Example for N = 4:
```
[Focus, ShortBreak, Focus, ShortBreak, Focus, ShortBreak, Focus, LongBreak]
```

### `TimerSession` — no structural changes

A cycle is a `TimerSession` whose `items` were produced by `buildCycle`. The existing `currentIndex` tracks position.

**Cycle reset on completion:** when `isComplete` becomes true, the session rebuilds via `buildCycle` and resets `currentIndex = 0`. Timer auto-continues if `autoStartNextSession = true`, otherwise goes `Idle`.

### Dot progress derivation

No extra state. Derived from `TimerSession`:
- All `SessionItemType.Focus` items in `items` = the dots (count = `focusSessionsPerCycle`)
- Focus dot index `i` is **completed** if `currentIndex > position of that focus item in items`
- Focus dot index `i` is **current** if `currentItem` is a Focus and it is the i-th focus
- Otherwise **upcoming**

---

## Timer Screen UI

### Cycle dot row

- Positioned **above the timer digits** as a cycle header
- One dot per focus session in the cycle
- Always visible during an active session (including focus mode)

Dot states:
| State | Visual |
|---|---|
| Completed focus | Filled circle, peach/tomato color |
| Current focus | Filled circle, slightly larger or glowing |
| Upcoming focus | Outline circle, muted color |
| During a break | Dots show completed count so far; no dot is "active" |

### Skip button

- Visible **only** when `currentItem` is `ShortBreak` or `LongBreak`
- Hidden during Focus sessions
- Positioned alongside existing stop/pause controls

**Skip behavior:**
- Advances `currentIndex` to the next item
- If the skipped item is `LongBreak` (last item), cycle resets — same as natural completion
- Respects `autoStartNextSession`: if true, timer continues playing; otherwise goes `Idle`

---

## Settings

`focusSessionsPerCycle` is user-configurable from the Profile/Settings screen. Existing `TimerSettings.update()` pattern extended to handle this field. Range coerced to 1–10.

Changing `focusSessionsPerCycle` takes effect on the **next cycle start** — the current in-progress cycle is not interrupted.

---

## Out of Scope

- Different dot shapes per phase type (decided: dots only for focus sessions, breaks implied)
- Rearranging cycle order (always F → SB × (N-1) → F → LB)
- Skipping Focus sessions

