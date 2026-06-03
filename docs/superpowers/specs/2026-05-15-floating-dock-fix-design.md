# Design: Floating Dock Fix

**Date:** 2026-05-15  
**Status:** Approved

## Problem

The `DashboardDock` in `PurrodoroApp.kt` is stacked *below* screen content in a `Column`, not overlaid on top of it. Content screens work around this with a hardcoded `bottom = 100.dp` padding. This is fragile — the magic number doesn't respond to actual dock height or system navigation bar insets.

```
Current layout:
Column(fillMaxSize)
  Box(weight=1f)   ← content is clipped here
  DashboardDock    ← sits below, not floating
```

Affected screens: `CalendarScreen`, `TodoScreen`, `SoundsScreen`, `ProfileScreen` — all use `.padding(bottom = 100.dp)` as a workaround.

## Solution: Box Overlay

Replace the outer `Column` with a `Box`. The content area fills the full size; the dock is overlaid at `Alignment.BottomCenter`. The dock's actual measured height is passed to each scrollable screen as a `contentPadding` parameter so scroll content clears the dock naturally.

```
New layout:
Box(fillMaxSize)
  Box(fillMaxSize)        ← content fills everything
  DashboardDock           ← overlaid at BottomCenter, floats above content
```

## Changes

### `PurrodoroApp.kt`
- Replace outer `Column` with `Box`
- Remove `Box(weight=1f)` wrapper around screen content — content fills full size
- Measure dock height via `onSizeChanged`, store as `dockHeightPx` → convert to `Dp`
- Pass `contentPadding = PaddingValues(bottom = dockHeightDp)` to each scrollable screen
- Place `DashboardDock` at `Alignment.BottomCenter` inside the outer `Box`
- `PremiumMessage` banner: keep above dock, place inside the outer `Box` at `Alignment.BottomCenter` with appropriate offset, or pass its height into the padding as well

### `CalendarScreen.kt`
- Add `contentPadding: PaddingValues = PaddingValues(0.dp)` parameter
- Replace `.padding(bottom = 100.dp)` with `.padding(contentPadding)`

### `TodoScreen.kt`
- Same as CalendarScreen

### `SoundsScreen.kt`
- Same as CalendarScreen

### `ProfileScreen.kt`
- Same as CalendarScreen

### `FocusDashboard` (Timer screen)
- No change — does not scroll

## Files Touched

| File | Change |
|------|--------|
| `PurrodoroApp.kt` | Layout refactor, dock height measurement, pass contentPadding |
| `CalendarScreen.kt` | Add `contentPadding` param, remove hardcoded bottom padding |
| `TodoScreen.kt` | Same |
| `SoundsScreen.kt` | Same |
| `ProfileScreen.kt` | Same |

## Non-Goals

- No change to `DashboardDock` internals
- No change to `navigationBarsPadding()` handling (already correct on the dock)
- No Scaffold adoption
