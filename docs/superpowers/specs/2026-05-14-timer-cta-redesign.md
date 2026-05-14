# Timer CTA Redesign

**Date:** 2026-05-14

## Goal

Remove the manual mode selector from the timer screen and replace the small idle play button with a prominent pill-shaped "Focus Now" CTA. Modes only change automatically through cycle progression — never manually.

## Decisions

- **Mode switching:** Removed entirely from the UI. `TimerMode` only changes as a side-effect of cycle advancement (automatic). No manual override.
- **CTA style:** Pill-shaped button, full-width with `horizontal: 48dp` padding, height `56dp`, accent fill `#FF8B6F`, white bold text `16sp "Focus Now"`, corner radius `28dp`.
- **Idle layout:** Cycle dots → timer countdown → spacer → "Focus Now" pill button.
- **Running/paused states:** Unchanged (pause circle, resume circle + stop circle).

## What Changes

### Removed
- `showModeMenu` state variable in `FocusDashboard`
- `DropdownMenu` and `DropdownMenuItem` imports/usage
- The mode selector `Box` + `DropdownMenu` in the idle button row
- `onModeSelected` callback parameter from `FocusDashboard` composable signature
- The small circular ▶ play button in idle state (replaced by pill)

### Added
- Pill-shaped `Box` ("Focus Now") replacing the idle play + dropdown row
- Calls existing `onStartPause` on tap — no new callbacks needed

### Unchanged
- Running state: pause circle button
- Paused state: resume circle + stop circle (+ skip during breaks)
- Cycle dots row
- Timer countdown display
- `onModeSelected` call site in `KuroPomodoroApp` removed (no longer passed to `FocusDashboard`)

## Layout Detail (Idle State)

```
[Cycle dots row — centered]
[Timer countdown — 80sp, centered]
[Spacer weight(1f)]
[  Focus Now pill — fillMaxWidth, horizontal padding 48dp, height 56dp  ]
[Spacer 40dp]
```

## Constraints

- App visual style: dark `#1A1425` bg, glass cards, accent `#FF8B6F`
- No emoji in button text
- Pill button uses same `#FF8B6F` fill as existing accent; white text `FontWeight.Bold`
