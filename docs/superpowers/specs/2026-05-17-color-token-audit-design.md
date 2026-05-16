# Color Token Audit — Design Spec
**Date:** 2026-05-17  
**Approach:** Interactive / Display Split (Approach 2)  
**Scope:** WCAG AA compliance + broad UX quality audit  
**Files affected:** `Color.kt`, `SurfaceTokens.kt`, `KuroComponents.kt`

---

## Background

An audit of the Kuro Pomodoro Android app's light and dark color tokens against WCAG 2.1 AA standards and Material Design 3 best practices revealed six contrast failures.

The root cause is structural: `Coral (#FF7F73, L=0.376)` and `DockCoral (#FF8B6F, L=0.408)` are mid-tone colors. No single text color can achieve the required 4.5:1 ratio against a mid-tone background, so these colors cannot serve as both decorative accent and interactive button background.

The chosen approach — **Interactive / Display Split** — resolves all failures by:
1. Keeping the existing coral tokens for **decorative** uses (progress ring, cycle dots, backdrop tints, glass card accents).
2. Adding a new `interactiveFill`/`interactiveText` token pair that buttons and active tab indicators use exclusively.
3. Nudging one near-miss dark token (`NightFaint`) to clear the threshold.
4. Promoting a more legible text token for the `textMuted` role in light mode.

---

## Standards Applied

| Standard | Requirement |
|---|---|
| WCAG 2.1 AA — Normal text | ≥ 4.5:1 contrast ratio |
| WCAG 2.1 AA — Large text (≥ 18sp or ≥ 14sp bold) | ≥ 3:1 contrast ratio |
| WCAG 2.1 — Incidental/decorative text | No requirement |
| Apple HIG Dark Mode | ≥ 4.5:1 minimum; strive for 7:1 in small text |
| Material Design 3 | Primary/onPrimary tone pairing via HCT tonal palettes |

---

## Audit Findings

### Failures

| Token Pair | Contrast | WCAG Status | Severity |
|---|---|---|---|
| `CardWhite` on `Coral` (light button text) | 2.42:1 | ✗ AA | Critical |
| `White` on `Coral` (light active dock label 9sp) | 2.46:1 | ✗ AA | High |
| `CocoaFaint` on `Cream` (light textMuted) | 2.75:1 | ✗ AA | High |
| `White` on `DockCoral` (dark active dock label 9sp) | 2.29:1 | ✗ AA | High |
| `Cocoa` on `DockCoral` (dark button text) | 3.79:1 | ✗ AA | High |
| `NightFaint` on `NightDeep` (dark textMuted) | 4.37:1 | ✗ AA | Medium |

### Passing

| Token Pair | Contrast | Notes |
|---|---|---|
| `White` on `NightDeep` | 19.83:1 | ✓✓✓ |
| `NightMuted` on `NightDeep` | 9.52:1 | ✓✓✓ |
| `DockCoral` on `NightDeep` | 8.66:1 | ✓✓✓ — accent on bg |
| `Cocoa` on `Cream` | 8.22:1 | ✓✓✓ |
| `Cocoa` on `Peach` | 5.10:1 | ✓ — Mode chip |
| `CocoaSoft` on `Cream` | 4.57:1 | ✓ — barely |
| `NightMuted` on `NightCard` | 8.59:1 | ✓✓✓ |

### Intentionally Low (WCAG exempt — decorative/incidental)

| Token Pair | Contrast | Context |
|---|---|---|
| `NightFainter` on `NightDeep` | 2.14:1 | Calendar empty cell text — non-functional |
| `CocoaFainter` on `Cream` | 1.81:1 | Calendar empty cell text — non-functional |

---

## Design Decisions

### Decision 1: Interactive / Display Split

**Problem:** Using `Coral`/`DockCoral` for both decorative tints and interactive button fills creates an impossible contrast constraint — the color is too light for dark text and too saturated for white text.

**Solution:** Introduce `interactiveFill` and `interactiveText` as new fields on `KuroSurfaceTokens`. All interactive components (buttons, active dock tabs, selected pills, mode toggles) use these tokens. Decorative components (ring arc, dots, backdrop fills, glass card borders) continue using `accent = Coral/DockCoral`.

This preserves the warm coral brand aesthetic in the places users see it most (the focus timer backdrop, progress ring, ambient decorations) while ensuring interactive affordances are fully accessible.

### Decision 2: Reuse `Peach` for dark mode interactive

`Peach (#FFB59E)` is already defined in the palette and `Cocoa` text on it yields **5.10:1**. No new color is needed for dark mode. The token reuses an existing palette value in a new role.

### Decision 3: New `DeepCoral` for light mode interactive

A new `DeepCoral (#C64432)` — a deep terracotta in the same warm hue family as `Coral` — provides **4.91:1** contrast with white text. It reads as a pressed/action variant of coral rather than a foreign color.

### Decision 4: Nudge `NightFaint` not replace it

`NightFaint` misses AA by a margin of 0.13 (4.37:1 vs 4.5:1 required). Rather than adding a new token, the existing value is shifted from `#7A7388` to `#7D768B` — a ~4-unit brightness increase imperceptible to the eye but sufficient to clear the threshold (new ratio: **4.56:1**).

### Decision 5: Promote `CocoaSoft` to `textMuted` in light mode

`CocoaFaint (#A89486)` at 2.75:1 is too low for secondary readable text (it was introduced as a muted label color but falls short). `CocoaSoft (#8B6B5A)` at 4.57:1 already passes and has the right semantic weight for muted text. `CocoaFaint` is demoted to `textFaint` (purely decorative, calendar cells), and `CocoaFainter` moves to a comment-only alias.

### Decision 6: Replace `.copy(alpha=0.72f)` with `textSecondary` token

Two components used `colorScheme.onSurface.copy(alpha = 0.72f)` for subtitle/sub-label text. This alpha blending produces sub-threshold contrast (~2.8:1 in light mode). Direct use of `kuroTokens.textSecondary` resolves this while also making the intent explicit in code.

---

## Spec

### `Color.kt`

**Add:**
```kotlin
val DeepCoral = Color(0xFFC64432)   // deep terracotta; White on this = 4.91:1 ✓
```

**Change:**
```kotlin
// was: val NightFaint = Color(0xFF7A7388)
val NightFaint = Color(0xFF7D768B)  // +4 brightness units; contrast 4.37→4.56:1 ✓
```

All other palette values unchanged.

---

### `SurfaceTokens.kt`

**Add 2 fields to `KuroSurfaceTokens`:**
```kotlin
data class KuroSurfaceTokens(
    // … existing fields …
    val interactiveFill: Color,   // button container + active tab/pill bg
    val interactiveText: Color,   // text/icon on interactiveFill
)
```

**Dark tokens additions:**
```kotlin
interactiveFill = Peach,      // #FFB59E — Cocoa on Peach = 5.10:1 ✓
interactiveText = Cocoa,
```

**Light tokens changes:**
```kotlin
interactiveFill = DeepCoral,  // #C64432 — White on DeepCoral = 4.91:1 ✓
interactiveText = Color.White,
textMuted       = CocoaSoft,  // was CocoaFaint (2.75:1→4.57:1) ✓
textFaint       = CocoaFaint, // demoted from textMuted; used for calendar empty cells
```

---

### `KuroComponents.kt`

#### `KuroPrimaryButton`
```kotlin
// Before
colors = ButtonDefaults.buttonColors(
    containerColor = colorScheme.primary,
    contentColor   = colorScheme.onPrimary
)

// After
val tokens = MaterialTheme.colorScheme.kuroTokens
colors = ButtonDefaults.buttonColors(
    containerColor = tokens.interactiveFill,
    contentColor   = tokens.interactiveText
)
```

#### `DockTabItem`
```kotlin
// Before
val iconColor = if (active) kuroTokens.accentOn else kuroTokens.textMuted
val bgColor   = if (active) kuroTokens.accent   else Color.Transparent

// After
val iconColor = if (active) kuroTokens.interactiveText else kuroTokens.textMuted
val bgColor   = if (active) kuroTokens.interactiveFill else Color.Transparent
```

#### `AppearanceModeToggle`
```kotlin
// Before
.background(if (selected) tokens.accent else Color.Transparent)
color = if (selected) tokens.accentOn else tokens.textSecondary

// After
.background(if (selected) tokens.interactiveFill else Color.Transparent)
color = if (selected) tokens.interactiveText else tokens.textSecondary
```

#### `GlassPill`
```kotlin
// Before
val fill      = if (selected) kuroT.accent    else tokens.pillFill
val textColor = if (selected) kuroT.accentOn  else kuroT.textSecondary

// After
val fill      = if (selected) kuroT.interactiveFill else tokens.pillFill
val textColor = if (selected) kuroT.interactiveText else kuroT.textSecondary
```

#### `SectionTitle` subtitle
```kotlin
// Before
color = colorScheme.onSurface.copy(alpha = 0.72f)

// After
color = MaterialTheme.colorScheme.kuroTokens.textSecondary
```

#### `TimerRing` sub-label ("stay with kuro")
```kotlin
// Before
color = colorScheme.onSurface.copy(alpha = 0.72f)

// After
color = kuroTokens.textSecondary
```

---

## What Does Not Change

- `Coral`, `DockCoral` values in `Color.kt` — preserved exactly
- `accent = Coral/DockCoral` in `KuroSurfaceTokens` — preserved; used by timer ring, dots, glass card borders
- `colorScheme.primary = Coral/DockCoral` in `Theme.kt` — preserved; used for progress arc fill, decorative blobs
- `accentOn = Color.White` — preserved for non-interactive glass overlay text on accent-tinted surfaces
- Glass tokens (`glassTokens()`) — unchanged
- `NightFainter` / `CocoaFainter` — unchanged; decorative-only (calendar empty cells, WCAG exempt)

---

## Contrast Summary After Changes

| Token Pair | Before | After |
|---|---|---|
| Light mode button text | 2.42:1 ✗ | **4.91:1 ✓** |
| Dark mode button text | 3.79:1 ✗ | **5.10:1 ✓** |
| Light active dock tab (9sp) | 2.46:1 ✗ | **4.91:1 ✓** |
| Dark active dock tab (9sp) | 2.29:1 ✗ | **5.10:1 ✓** |
| Light textMuted | 2.75:1 ✗ | **4.57:1 ✓** |
| Dark textMuted | 4.37:1 ✗ | **4.56:1 ✓** |
