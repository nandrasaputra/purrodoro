# Debug Screen UX Redesign

**Date:** 2026-05-14  
**Scope:** `DebugActivity.kt` only — debug source set, no release impact

---

## Problem

The current Dev Tools screen has several UX issues:
1. Clock preset buttons overflow on narrow screens (single `Row`)
2. Restart button is buried mid-page; users must scroll to restart after setting an override
3. No feedback after applying an override
4. Restart button gives no indication overrides are persisted
5. isPremium toggle doesn't show DataStore value vs. override state
6. Section order puts Premium above Clock (Clock is the primary tool)
7. Danger Zone has no visual separation from the rest of the page

---

## Design

### Layout Structure

```
┌─────────────────────────────────┐
│  Kuro Dev Tools        [Close]  │  ← header row (outside scroll)
├─────────────────────────────────┤
│  Clock: 08:00 · Premium: ON     │  ← sticky status banner (outside scroll)
│                    [Restart ↺]  │
├─────────────────────────────────┤
│  ↕ scrollable content           │
│    CLOCK OVERRIDE               │
│    PREMIUM OVERRIDE             │
│    SESSION INJECTION            │
│    PROCESS                      │
│    DANGER ZONE (Card)           │
└─────────────────────────────────┘
```

- `Scaffold` wraps everything
- Header + sticky banner are in a `Column` *outside* the scrollable area
- Scrollable content is a `Column` with `verticalScroll(rememberScrollState())`

### Sticky Status Banner

A `Surface(color = secondaryContainer)` with 8dp corner radius pinned between the header and the scroll area.

**States:**
- Clock override active → `🕗 HH:mm` chip text
- isPremium override ON → `★ Premium` chip text
- Both absent → `"No overrides active"` in muted text (onSurface 50% alpha)
- Restart button always visible on the right; same kill+relaunch logic as the PROCESS section button

**Padding:** 12dp horizontal, 10dp vertical

### Snackbar Feedback

Fires on every override mutation:

| Action | Message | Action Label |
|---|---|---|
| Clock preset tapped | `"Clock set to HH:mm"` | `"Restart"` |
| Clear override | `"Clock cleared"` | — |
| Premium toggle ON | `"Premium override ON"` | `"Restart"` |
| Premium override OFF | `"Premium override OFF"` | — |

- Duration: `SnackbarDuration.Short`
- "Restart" action in snackbar triggers the same process kill+relaunch
- Hosted in `Scaffold`'s `snackbarHost`

### Scrollable Content

**Section order:**
1. CLOCK OVERRIDE
2. PREMIUM OVERRIDE
3. SESSION INJECTION
4. PROCESS
5. DANGER ZONE

**Clock Override:**
- Replace preset `Row` with `FlowRow` (`androidx.compose.foundation.layout.FlowRow`) so buttons wrap on narrow screens
- Presets unchanged: 08:00, 12:00, 17:00, 22:00

**Premium Override:**
- Toggle + label unchanged
- Subtitle updated to show both DataStore value and override state:
  - Override OFF: `"DataStore: free · Override: OFF"` (or `"DataStore: premium · Override: OFF"`)
  - Override ON: `"DataStore: free · Override: ON"`

**Process section:**
- Subtitle: `"Overrides are saved to DataStore — app will relaunch with settings applied."`

**Danger Zone:**
- Wrapped in `Card(colors = CardDefaults.cardColors(containerColor = errorContainer))`
- `Spacer(height = 16.dp)` above the Card
- Section title color stays `error`
- Reset confirmation logic unchanged

---

## Files Changed

- `app/src/debug/java/com/endiar/kuropomodoro/debug/DebugActivity.kt` only

## Out of Scope

- Session injection date picker (separate future task)
- Any changes to `DebugRepository`, `AppClock`, or release source set
