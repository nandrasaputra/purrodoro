# Sounds Bottom Sheet Design

**Date:** 2026-05-18
**Status:** Approved

## Overview

Move the Sounds feature from a dedicated dock tab into a `ModalBottomSheet`, accessible via a music icon button on the home (Focus) screen. The entry point is always visible below the center timer component. Remove `AppTab.Sounds` from the dock, leaving 4 tabs.

---

## Goals

- Surface the ambient sounds picker directly from the Focus screen without leaving it
- Reduce dock clutter from 5 tabs to 4
- Preserve all existing sounds functionality (selection, premium gating, simulated playback)
- Keep `SoundsScreen.kt` composable untouched — reuse it inside the sheet

---

## Scope

### In scope
- Remove `AppTab.Sounds` from `KuroModels.kt`
- Remove the `AppTab.Sounds` branch from `PurrodoroApp`'s tab switch
- Add `showSoundsSheet` state and `ModalBottomSheet` in `PurrodoroApp`
- Add `MusicButton` composable inside `FocusDashboard`
- Rotating vinyl animation on the icon when a sound is playing

### Out of scope
- Actual audio playback (remains simulated as before)
- Any changes to `SoundsScreen.kt`
- Any changes to sound data, `AppRepository`, or `AppState`

---

## Architecture

### `KuroModels.kt` — `AppTab` enum

Remove `Sounds` entry. Final order:

```kotlin
enum class AppTab(val label: String, val symbol: String) {
    Timer("Focus", "Home"),
    Todo("Todo", "Todo"),
    Calendar("Records", "Records"),
    Profile("Me", "Me")
}
```

### `PurrodoroApp.kt` — sheet state

Add alongside existing sheet state:

```kotlin
var showSoundsSheet by rememberSaveable { mutableStateOf(false) }
```

Remove the `AppTab.Sounds -> SoundsScreen(...)` branch from the `when (selectedTab)` block.

Add a `ModalBottomSheet` at the top level of the composable (same level as existing `premiumSheetMessage` overlay and `CycleCompletionDialog`), rendered when `showSoundsSheet == true`:

```kotlin
if (showSoundsSheet) {
    ModalBottomSheet(
        onDismissRequest = { showSoundsSheet = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = tokens.cardFill,
        contentColor = tokens.textPrimary,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        SoundsScreen(
            sounds = sounds,
            selectedSoundId = selectedSoundId,
            playingSoundId = playingSoundId,
            onSoundSelected = { sound -> /* existing logic */ },
            onToggleSound = { sound -> /* existing logic */ },
            glassTreatment = GlassTreatment.Clear,
            overBackdrop = false,
        )
    }
}
```

Sound callbacks (`onSoundSelected`, `onToggleSound`, `premiumSheetMessage`) are moved from the removed tab branch and wired into the sheet. Closing the sheet does **not** stop playback — `playingSoundId` is untouched on dismiss.

The `PremiumMessage` surface (shown when a locked sound is tapped) is rendered **inside** the `ModalBottomSheet` content, above `SoundsScreen`, so it stays contextually within the sheet rather than appearing outside/below the modal overlay.

### `FocusDashboard` — `MusicButton`

A new private `MusicButton` composable added to `PurrodoroApp.kt`. Placed inside `FocusDashboard`, centered horizontally, below the controls row (pause/play/stop/skip or "Focus Now"), always rendered regardless of timer state.

#### Idle state (no sound playing)
- Circular icon button, Material `MusicNote` icon
- Low-opacity white (e.g. `Color.White.copy(alpha = 0.5f)`)
- Static, no animation

#### Playing state (`playingSoundId != null`)
- Same circular button
- Full-opacity white (or a tinted accent color)
- Continuous rotation animation via `rememberInfiniteTransition`:
  - `animateFloat` from `0f` to `360f`
  - `LinearEasing`, period ~4 seconds
  - Applied as `graphicsLayer { rotationZ = rotation }`
- Icon: `MusicNote` or a disc/vinyl symbol on a circular background

#### Interaction
- Tap → `onOpenSoundsSheet()` callback → sets `showSoundsSheet = true` in parent
- Visible in both normal and focus mode

---

## Data Flow

No data flow changes. `playingSoundId` remains a `rememberSaveable` in `PurrodoroApp`. `selectedSoundId` persists via `AppRepository` as before. The sheet's open/close state (`showSoundsSheet`) is ephemeral UI state.

```
MusicButton tap
    → showSoundsSheet = true
    → ModalBottomSheet renders SoundsScreen

SoundsScreen row tap (unlocked)
    → persistSelectedSoundId(sound.id)   [AppRepository]
    → playingSoundId = sound.id          [UI state]

SoundsScreen row tap (playing)
    → playingSoundId = null              [UI state]

Sheet dismiss (swipe / scrim tap)
    → showSoundsSheet = false
    → playingSoundId unchanged
```

---

## Dock Changes

`DashboardDock` iterates `AppTab.entries`. Removing `AppTab.Sounds` automatically reduces the dock to 4 tabs. No other dock changes required. The `isSoundPlaying` prop on `DashboardDock` can be removed if it was only used to highlight the Sounds tab; otherwise retain if it drives other visual feedback.

Final dock order: **Focus → Todo → Records → Me**

---

## Error Handling / Edge Cases

- **Sheet open during focus mode:** The sheet opens on top of the focus mode scrim. No special handling needed — `ModalBottomSheet` overlays everything. Dismissing the sheet returns to focus mode.
- **Premium sound tapped inside sheet:** `premiumSheetMessage` is set as before; the `PremiumMessage` surface renders inside the sheet content (above `SoundsScreen`), keeping the message contextually tied to the interaction.
- **App killed / process death:** `showSoundsSheet` is `rememberSaveable` so it survives config changes but not process death — acceptable, the sheet simply closes on restart.

---

## Testing

- `MusicButton` renders below controls in all timer states (idle, running, paused)
- `MusicButton` is static when `playingSoundId == null`
- `MusicButton` rotates continuously when `playingSoundId != null`
- Tapping `MusicButton` opens the bottom sheet
- `SoundsScreen` inside sheet works identically to previous tab behavior
- Dismissing sheet does not change `playingSoundId`
- Dock shows 4 tabs; `AppTab.Sounds` no longer exists
- No regression on Todo/Calendar bottom sheets
