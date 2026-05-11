# Secondary Tabs over Cat Background — Design Spec

**Date:** 2026-05-12

## Goal

Unify the visual language across all five tabs of the app. The Home (Focus Dashboard) tab already uses a cinematic, time-of-day cat photo as its background. The other four tabs — Todo, Records, Sounds, Me (Profile) — currently use a separate light/material card layout with the old `BottomTabBar`. They look and feel disconnected from Home.

The goal is for every tab to share the same cat photo background and the same glass dock navigation, with content shown in semi-transparent glass cards that let the photo show through.

This spec also retires the old `BottomTabBar` and the old screen scaffolding (`DecorativeBackground`, `KuroCard`, etc.) for these tabs.

## Approach (Soft scrim + glass cards)

For every tab:

1. **Cat photo background** — `cat_<time-of-day>.webp` from `app/src/main/res/drawable/`, full-bleed via `Image(contentScale = ContentScale.Crop)`. The image resolves at composition time from `backgroundImageForTime(LocalTime.now())`, the existing helper in `KuroModels.kt`.
2. **Top scrim** — vertical linear gradient from `#0a0816CC` (top) → `#0a081600` (bottom), 240dp tall, anchored to the top edge. Provides legibility for the page title.
3. **Bottom scrim** — vertical linear gradient from `#0a081600` (top) → `#0a0816CC` (bottom), 260dp tall, anchored to the bottom edge. Provides legibility for the dock.
4. **Glass content cards** sit between the scrims.
5. **Glass dock** (existing `DashboardDock` in `KuroComponents.kt`) is shared by Home and all secondary tabs. Each tab highlights its own dock item.

The old `BottomTabBar`, the `DecorativeBackground` wrapper, the `KuroCard` shape, and the per-tab use of `MaterialTheme.colorScheme.surface` are all removed for these tabs. Existing screen logic (state, callbacks) stays — only the presentation changes.

## Shared design system

These tokens are used by every secondary tab.

### Colors (over photo)

| Token | Value | Use |
|---|---|---|
| Background fallback | `#0a0816` | Behind the photo (in case it fails to load) |
| Primary text | `#FFFFFF` | Titles, active labels |
| Body text | `#FFFFFFCC` (white@80%) | Task names, primary card text |
| Secondary text | `#FFFFFF8C` (white@55%) | Subtitles, descriptions |
| Tertiary text | `#FFFFFF66` (white@40%) | Section labels, weekday headers |
| Disabled text | `#FFFFFF4D` (white@30%) | Future calendar dates |
| Coral / accent | `#FF8B6F` | Active states, primary buttons |
| Coral tint | `#FF8B6F1A` (coral@10%) | Active card fills |
| Coral border | `#FF8B6F40` (coral@25%) | Active card borders |
| Lavender | `#D8C8FF` | Premium / mock indicator |
| Lavender bg | `#7B4FBF40` (purple@25%) | Premium / mock pill bg |

### Card / pill primitives

| Primitive | Specs |
|---|---|
| Glass card (default) | `cornerRadius: 22dp`, `fill: #0F0A1A66` (deep navy@40%), `stroke: 1dp #FFFFFF1A`, `padding: 16–18dp` |
| Glass card (active) | `cornerRadius: 18–22dp`, `fill: #FF8B6F1A`, `stroke: 1dp #FF8B6F40` |
| Pill / chip (default) | `cornerRadius: 20dp`, `fill: #FFFFFF14`, `stroke: 1dp #FFFFFF26`, `padding: 8×16dp` |
| Pill / chip (selected) | `cornerRadius: 20dp`, `fill: #FF8B6F`, white text |
| Status pill (Premium) | `cornerRadius: 12dp`, `fill: #7B4FBF40`, `stroke: 1dp #D8C8FF66`, lavender lock icon + `Premium` |
| Status pill (Playing) | `cornerRadius: 12dp`, `fill: #FF8B6F`, white play icon + `Playing` |
| Status pill (Selected) | `cornerRadius: 12dp`, `fill: #FFFFFF1A`, `stroke: 1dp #FFFFFF26`, `Selected` |
| Mock badge | `cornerRadius: 8dp`, `fill: #7B4FBF40`, lavender `Mock` text, 9sp |

### Typography

| Use | Size / Weight |
|---|---|
| Page title (`To-Do`, `Records`, `Sounds`, `Me`) | 28sp / W700 / white |
| Page subtitle | 13sp / W400 / `#FFFFFF8C` |
| Card title | 14–15sp / W600–W700 / white |
| Card body | 11–13sp / W400 / `#FFFFFF8C` |
| Section label (`UPCOMING`) | 10sp / W600 / `#FFFFFF4D` / letter-spacing 1.5 |
| Big numeric (stats) | 22sp / W700 / white |
| Calendar day | 12sp / W400 (active: W600) |

### Layout structure (every secondary tab)

```
Box(fillMaxSize):
  Image(cat_<time-of-day>, ContentScale.Crop, fillMaxSize)
  Box(top, height=240dp, vertical gradient #0a0816CC → transparent)
  Box(bottom, height=260dp, vertical gradient transparent → #0a0816CC)
  Column(fillMaxSize, statusBarsPadding):
    [Page header: title + subtitle, padding 20dp]
    [Scrollable content column, padding 20dp horizontal, vertical spacing]
    Spacer(weight = 1f)
    DashboardDock(highlightedTab = <this tab>, navigationBarsPadding)
```

## Per-screen layouts

### Todo tab (existing screen, restyled)

Order top to bottom inside the content column:

1. **Header row** — `To-Do` title + `Mon, May 12` (today, formatted) on the left; circular coral `+` button (40dp) on the right.
2. **List switcher** — horizontal pill row of all todo lists. Selected list = coral fill + white text. Others = glass pill.
3. **Section label** — `UPCOMING`.
4. **Task list** — vertical column of task cards. Each card:
   - Circular checkbox (22dp). Unchecked = `#FFFFFF1A` fill + white border. Checked = `#FF8B6F` fill + white check icon.
   - Title (15sp / W600) + minutes subtitle (12sp).
   - Active task (the next incomplete) uses the active glass card style. Others use default glass.
5. **Add-task affordance** — the orange `+` button in the header is the only entry point. Tapping it reveals an inline glass input row beneath the chips with a single text field and a submit chip. Submitting calls the existing `onAddTask` and collapses the input. This is a behavior change from the current always-visible card with `OutlinedTextField` + `KuroPrimaryButton`; existing `onAddTask` callback is reused unchanged.

### Records tab (replaces Calendar tab)

1. **Header** — `Records` title + `<X> focus sessions this month` subtitle.
2. **Stats row** — three glass mini-cards filling the row equally:
   - `TODAY` / count
   - `WEEK` / count
   - `STREAK` / `<n>d` (uses the active card style)
3. **Calendar card** — one large default glass card containing:
   - Month nav row: `‹  May 2026  ›`.
   - Weekday header row (S M T W T F S, 10sp tertiary text).
   - 5–6 week rows. Each cell is a 34dp tall fill_container frame.
     - Future date: muted text only, no chip.
     - Past date with no stamp: tertiary text, no chip.
     - Past date with stamps: coral tint chip (`#FF8B6F1A` fill, `#FF8B6F40` border, white W600 text).
     - Today: glass chip with brighter white border (`#FFFFFFB3`, 1.5dp).

### Sounds tab

1. **Header** — `Sounds` title + `Pick a cozy loop for Kuro` subtitle.
2. **Sound list** — vertical column of sound cards. Each card:
   - 44dp icon tile (lucide icon — `cloud-rain`, `coffee`, `trees`, `flame`, `keyboard`, etc.). Unlocked sounds use white@10% tile + white@80% icon. Premium sounds use lavender tile + lavender icon.
   - Name (15sp / W600) + description (12sp).
   - Status pill on the right.
3. Currently playing sound uses the active card style. Premium sounds dim their text to `#FFFFFFCC` / `#FFFFFF66`.

### Me tab (replaces Profile tab)

1. **Header** — `Me` title + `Local · stays on this device` subtitle.
2. **Profile card** — glass card with 52dp coral avatar (`Ku` initials, white W700) + `Pomodoro Keeper` name + `No account, no cloud sync` line.
3. **Timer lengths card** — glass card containing the title `Timer lengths` and three rows. Each row: mode label + `<n> minutes` line on the left; `−` (glass chip) and `+` (coral-tinted chip) buttons on the right.
4. **Dark mode preview row** — full-width glass card row with description + iOS-style toggle (coral when on, white knob).
5. **Mock feature grid** — 2×2 grid of square glass cards. Each card: lucide icon + `Mock` badge in the top row; title + one-line description below. Premium Pass uses the active card style; the other three use default glass.

## Component changes in code

### Reuse / unchanged

- `DashboardDock` (already in `KuroComponents.kt`) — the same component is used by Home and all secondary tabs. Today it has no notion of an "active" tab. It needs:
  - A new `selectedTab: AppTab?` parameter (`null` on Home so nothing is highlighted there, or the matching `AppTab` on a secondary tab).
  - The active dock item gets coral icon + coral label (`#FF8B6F`) on a `#FF8B6F33` rounded background.
  - Inactive items keep the current `#FFFFFFB3` icon + label and no background.
  - The center `START / PAUSE` home button is unaffected and always shows the play/pause state.
- `backgroundImageForTime(LocalTime.now())` — already in `KuroModels.kt`, drives the photo for every screen.
- `AppTab` enum — already has the right user-facing labels ("Records", "Me"). No model change needed.
- All existing screen state (todoLists, stamps, settings, sounds, etc.) and callbacks (addTask, toggleTask, onSoundSelected, onDurationChanged…) — unchanged.

### New shared composables (proposed location: `ui/components/KuroComponents.kt`)

- `CatBackdrop(content: @Composable BoxScope.() -> Unit)` — fillMaxSize Box that paints the cat image + top + bottom scrims, then renders `content`.
- `GlassCard(modifier, active = false, content)` — replaces `KuroCard` for these tabs. Inactive vs active style.
- `GlassPill(text, selected, onClick)` — replaces the chip variants.
- `StatusPill(label, kind = Default | Playing | Selected | Premium)`.
- `MockBadge()`.

### Removed / retired (for these tabs)

- `BottomTabBar` — fully removed; its callsite in `KuroPomodoroApp.kt` is replaced with `DashboardDock`.
- `DecorativeBackground` — no longer wraps these screens.
- `KuroCard` — not used by these tabs (left in place if other code still depends on it, but the four tab screens migrate to `GlassCard`).
- The old `if (selectedTab == AppTab.Timer) … else …` split — collapses into a single Column with `CatBackdrop` always behind, and a `when (selectedTab)` choosing the per-tab content column above the dock.

### Per-screen file changes

- `TodoScreen.kt` — restyled with header row + list switcher pill + UPCOMING label + task cards. Add-task remains, restyled as a glass pill input.
- `CalendarScreen.kt` — file name unchanged; the user-facing label changes to **Records**. Stats row added at the top (Today / Week / Streak). Calendar grid restyled with chip cells.
- `SoundsScreen.kt` — restyled with icon tiles and status pills; introduces lucide icon dependency or substitutes simple symbol text.
- `ProfileScreen.kt` — file name unchanged; user-facing label is **Me**. Restyled. Mock cards become a 2×2 grid instead of stacked rows. Profile name shortened from "Local Pomodoro Keeper" to "Pomodoro Keeper", subtitle to "No account, no cloud sync".

### `KuroPomodoroApp.kt` changes

- Wrap the entire main UI in `CatBackdrop` (or directly in a `Box` doing the same thing).
- Always render `DashboardDock` at the bottom (not just on the Home tab).
- Pass `selectedTab` to `DashboardDock` so it can highlight the active item.
- Replace the `BottomTabBar` callsite with the dock.
- The per-tab content for Home stays inside `FocusDashboard`, but `FocusDashboard` no longer paints its own background image or its own dock — those move up to the parent.

## Time-of-day photo mapping (unchanged from earlier spec)

| Time range | Drawable |
|---|---|
| 22:00 – 04:59 | `cat_mid_night` |
| 05:00 – 07:59 | `cat_dawn` |
| 08:00 – 10:59 | `cat_morning` |
| 11:00 – 16:59 | `cat_day` |
| 17:00 – 19:59 | `cat_evening` |
| 20:00 – 21:59 | `cat_night` |

## Out of scope

- Real-time photo refresh while a tab is open (the photo updates only on recomposition / tab switch).
- Backdrop blur on the glass cards (not needed; the dark scrim + low-opacity navy fill provide enough separation).
- Task editing, drag-reorder, or per-task duration changes beyond what the existing screen supports.
- Calendar interaction (tapping a day, scrolling months) — visual only for now.
- Mock feature card actions — they continue to invoke the existing `onMockFeature` callback that opens the premium sheet.
- Any change to the Home (Focus Dashboard) tab itself beyond moving the dock to be shared and removing its now-duplicated background painting.
- Settings persistence — still local-only / Compose-saver-based.

## References

- Pencil design file: `/Users/nandra.saputra/Projects/nandra/pen file/kuro-pen.pen`
- Designed frames:
  - `u8QNW` — Todo Screen
  - `g6mGv` — Records Screen
  - `bdHNM` — Sounds Screen
  - `jJaiM` — Me Screen
  - `Z9jCY` — Bottom Nav / Pill (reusable component)
  - `SJcY1` — Status Bar (reusable component)
- Earlier spec: `docs/superpowers/specs/2026-05-10-dashboard-full-screen-image-design.md`
