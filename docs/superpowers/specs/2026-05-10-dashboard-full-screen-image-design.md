# Dashboard Full-Screen Image — Design Spec

**Date:** 2026-05-10

## Goal

Replace the Timer tab (the app's main/home screen) with a full-screen, edge-to-edge image that reflects the current time of day. No UI elements are overlaid. The bottom navigation bar is hidden on this tab.

## Approach

Option A: Conditional rendering in `KuroPomodoroApp.kt`. When `selectedTab == AppTab.Timer`, render only the full-screen image. For all other tabs, render the existing layout (content area + bottom nav) unchanged.

## Changes

### `KuroPomodoroApp.kt`

- Wrap the existing `Column` (content + nav bar) in an `else` branch.
- When `selectedTab == AppTab.Timer`, render a `Box` filling the entire screen (`fillMaxSize`) containing an `Image` composable:
  - `painter = painterResource(id = backgroundImageForTime())`
  - `contentScale = ContentScale.Crop` to fill edge-to-edge without letterboxing
  - `modifier = Modifier.fillMaxSize()`
- `DecorativeBackground` is not shown on the dashboard tab (it wraps the existing column layout only).
- All timer state and `TimerScreen` composable remain in place — they are simply not rendered while the dashboard is active.

### No new files

The change is self-contained in `KuroPomodoroApp.kt`. `backgroundImageForTime()` already exists in `KuroModels.kt`.

## Data flow

`backgroundImageForTime(LocalTime.now())` is called at composition time. The image resolves to the correct drawable based on the current hour. It updates naturally when the user switches away and back to the tab (recomposition).

## Time-to-image mapping

| Time range     | Drawable           |
|----------------|--------------------|
| 22:00 – 04:59  | `cat_mid_night`    |
| 05:00 – 07:59  | `cat_dawn`         |
| 08:00 – 10:59  | `cat_morning`      |
| 11:00 – 16:59  | `cat_day`          |
| 17:00 – 19:59  | `cat_evening`      |
| 20:00 – 21:59  | `cat_night`        |

## Out of scope

- Any overlay UI (clock, logo, text) — future work
- Real-time image refresh while the tab is open — future work
- Timer functionality — preserved but hidden; future work to surface it
