# Design System — Kuro Pomodoro

**Date:** 2026-05-24
**Scope:** `ui/designsystem/` package inside `:app` module
**Out of scope:** Timer logic, data layer, models, `Theme.kt`, `Color.kt` structure, `CalendarScreen`, `ProfileScreen` migration

---

## Goal

Establish a single source of truth for all visual decisions in the app — colors, spacing, typography, components, and screen patterns — so every screen feels like it belongs to the same "cozy productivity" brand without developers manually coordinating values.

The north star is the warm brand personality visible in the Home, Tasks, and Habits screens: coral/cream/cocoa palette, soft rounded corners, calm hierarchy.

---

## 1. Token Layer

### 1.1 Color tokens

`Color.kt` is already well-structured. No changes to its content or location. It is the canonical source of all raw color values.

**Rule:** No `Color(0xFF...)` literals in screen files. All colors must come from `KuroTokens.current`, `MaterialTheme.colorScheme`, or named constants from `Color.kt`.

### 1.2 Surface tokens

`KuroSurfaceTokens` in `SurfaceTokens.kt` is the correct abstraction. No structural changes. `KuroTokens.current` is the accessor composables must use.

**Rule:** Screens must not define private color palettes (`private val ReferenceCoral`, `private val HomeCream`, etc.). All such blocks must be deleted and replaced with `KuroTokens.current` references.

### 1.3 Spacing scale

New file: `ui/designsystem/KuroSpacing.kt`

```kotlin
object KuroSpacing {
    val xs     = 4.dp
    val sm     = 8.dp
    val md     = 16.dp
    val lg     = 20.dp
    val xl     = 24.dp
    val xxl    = 32.dp
    val xxxl   = 40.dp

    // Corner radii
    val radiusSm   = 12.dp
    val radiusMd   = 20.dp
    val radiusLg   = 28.dp
    val radiusPill = 34.dp
}
```

**Rule:** No raw `N.dp` spacing literals in screen files except inside component-internal layout. Screen-level layout uses `KuroSpacing` constants.

### 1.4 Typography scale

New file: `ui/designsystem/KuroType.kt`

Roles and their values:

| Role | Size | Weight | Usage |
|---|---|---|---|
| `displayTimer` | 48sp | Bold | Timer countdown digit |
| `headingScreen` | 28sp | Bold | Screen title (e.g. "Tasks") |
| `headingCard` | 18sp | SemiBold | Card section title |
| `bodyPrimary` | 15sp | Regular | Main list item text |
| `bodySecondary` | 13sp | Regular | Supporting / subtitle text |
| `labelStrong` | 13sp | SemiBold | Chip and pill labels |
| `labelMeta` | 11sp | SemiBold | Dock tab labels, badges |

**Rule:** No `fontSize = N.sp` or `fontWeight = FontWeight.X` literals in screen files. All typography via `KuroType` or `MaterialTheme.typography`.

---

## 2. Component Library

All new components live in `ui/designsystem/`. Existing `KuroComponents.kt` is refactored to import from there rather than hardcode values. Components are organized into three tiers.

### 2.1 Atoms

Single-purpose primitives. Each has a well-defined visual contract and no internal state.

| Component | Replaces / Notes |
|---|---|
| `KuroCard` | Existing — adopts `KuroSpacing.radiusLg` + `KuroSpacing.lg` padding |
| `KuroPrimaryButton` | Existing — adopts spacing tokens |
| `KuroChip` | Replaces `GlassPill` for flat-surface filter chips (Todo, Habits) |
| `KuroPill` | Replaces `StatusPill`; semantic kinds: `Playing`, `Locked`, `Default` |
| `KuroStepper` | Replaces `StepperButton`; adopts spacing tokens |
| `KuroDivider` | 1dp line using `tokens.divider`; used between list rows |
| `KuroBadge` | Replaces `MockBadge`; accepts any label text + fill/text color pair |

### 2.2 Molecules

Composed from atoms. May have simple local state (e.g. expansion).

| Component | Description |
|---|---|
| `KuroListRow` | Leading slot + title + optional subtitle + trailing slot. Standard row for settings, tasks, habit items. |
| `KuroSectionHeader` | Label text + optional trailing action (e.g. "Add", "See all") |
| `KuroFilterBar` | Horizontal scrolling row of `KuroChip`s with a selected index |
| `KuroBottomSheet` | Wraps `ModalBottomSheet` with standard drag handle, title row, and content padding |
| `KuroInputField` | Wraps `TextField`; Kuro styling: warm border, no underline, `radiusMd` corners |
| `KuroSegmentedControl` | Generalizes `AppearanceModeToggle` for any N string options |

### 2.3 Organisms

Screen-level assembled blocks.

| Component | Description |
|---|---|
| `KuroScreenScaffold` | Wraps `KuroFlatSurface` + `ScreenStickyBar` + scroll state wiring. Root of every flat-surface tab screen. |
| `KuroDock` | Existing `DashboardDock` — adopts spacing + type tokens, removes hardcoded colors |
| `KuroCycleDotsRow` | Existing `CycleDotsRow` — adopts tokens, removes hardcoded `Color(0xFFFF8B6F)` |

---

## 3. Screen Patterns

Patterns document how screens are assembled. They are not code — they are rules.

### Pattern: List Screen
**Used by:** Todo, Habits

```
KuroScreenScaffold
  └─ sticky bar (ScreenStickyBar)
  └─ KuroFilterBar
  └─ vertically scrolling list
       └─ KuroSectionHeader (per group)
       └─ KuroListRow (per item)
            └─ KuroDivider (between rows)
  └─ FAB (KuroPrimaryButton, anchored above dock)
```

Rule: No inline color or spacing. Every value from tokens or named components.

### Pattern: Settings Screen
**Used by:** Profile, Sounds sheet, future settings screens

```
KuroScreenScaffold
  └─ screen title
  └─ KuroCard (per settings group)
       └─ KuroListRow (per setting)
       └─ KuroDivider (between rows)
  └─ KuroCard (destructive actions, tinted accentSoft)
```

Rule: Actions that open sub-sheets use `KuroBottomSheet`. No full-screen navigation pushes from settings.

### Pattern: Detail Bottom Sheet
**Used by:** Add Task, Add Habit, Edit Habit, sound detail

```
KuroBottomSheet
  └─ title row
  └─ KuroInputField(s)
  └─ KuroListRow option rows
  └─ KuroPrimaryButton (commit action, bottom)
```

Rule: Sheets own their local state and emit a single commit callback. They do not call repository directly.

### Pattern: Dashboard Screen
**Used by:** Home / Timer tab only

```
KuroFlatSurface
  └─ glass card stack (GlassCard, glassTokens)
  └─ KuroDock
```

Rule: `glassTokens()` and `GlassCard` are **only** used in this pattern. All other patterns use flat surface tokens (`KuroTokens.current`). This keeps the glass visual grammar exclusive to the Dashboard.

---

## 4. Component Catalog (Debug Only)

Lives in `src/debug/`. Never shipped in release. Accessible from `DebugActivity` as a "Design System" entry.

### Structure

A single `ComponentCatalogScreen` Composable with a `KuroFilterBar` at the top navigating 4 sections:

**1. Tokens**
- Color swatches grid: every named constant from `Color.kt` as a labeled tile showing name + hex
- Surface token pairs: light + dark side-by-side for every `KuroSurfaceTokens` field
- Spacing scale: visual ruler — each `KuroSpacing` value as a colored bar with name + dp label
- Typography scale: each `KuroType` role rendered at its actual size with role name beside it

**2. Components**
- Every atom and molecule in isolation
- Both light and dark theme side-by-side per component
- All meaningful states shown: default, active/selected, disabled, locked (where applicable)
- Grouped: Atoms → Molecules → Organisms

**3. Patterns**
- Full-height mock of each screen pattern: List Screen, Settings Screen, Detail Bottom Sheet, Dashboard Screen
- Populated with representative static fake data (no repository calls)
- Light/dark toggle at the top of this section

**4. Audit**
- Checklist that scans for known drift signals at launch
- Initial checks: screens importing `Reference*` or `Home*` private color constants trigger a warning row
- Section grows as new checks are added over time

---

## 5. Migration Plan

### Order of operations

1. Create `ui/designsystem/` package with `KuroSpacing.kt`, `KuroType.kt`, and stub files for all new components
2. Refactor `KuroComponents.kt` to import spacing + type tokens — no behavior changes
3. Delete all private `Reference*` / `Home*` color blocks from `TodoScreen`, `HabitsScreen`, `HomeTimerScreen` — replace with `KuroTokens.current`
4. Implement new molecules: `KuroListRow`, `KuroSectionHeader`, `KuroFilterBar`, `KuroBottomSheet`, `KuroInputField`, `KuroSegmentedControl`
5. Refactor `TodoScreen` and `HabitsScreen` to use new molecules
6. Implement `KuroScreenScaffold` and migrate all flat-surface screens to use it
7. Build `ComponentCatalogScreen` in `src/debug/`, wire into `DebugActivity`

### Enforcement rules (living in `docs/DESIGN_SYSTEM.md`)

- No `Color(0xFF...)` literals in screen files
- No `fontSize = N.sp` or `fontWeight = FontWeight.X` literals in screen files
- No raw `N.dp` spacing in screen-level layout — use `KuroSpacing`
- Glass tokens only in Dashboard pattern
- New components go in `ui/designsystem/` before being used in screens
- No automatic commits — all git operations are done manually by the developer

### Out of scope for this pass

- `CalendarScreen` migration (separate cycle)
- `ProfileScreen` migration (separate cycle, in-progress redesign spec exists)
- Changes to `Theme.kt`, `Color.kt` structure, `SurfaceTokens.kt`
- Timer logic, data layer, models

---

## File Map

```
ui/
  designsystem/
    KuroSpacing.kt          ← new
    KuroType.kt             ← new
    KuroAtoms.kt            ← new (KuroChip, KuroPill, KuroStepper, KuroDivider, KuroBadge)
    KuroMolecules.kt        ← new (KuroListRow, KuroSectionHeader, KuroFilterBar,
                                    KuroBottomSheet, KuroInputField, KuroSegmentedControl)
    KuroOrganisms.kt        ← new (KuroScreenScaffold)
  components/
    KuroComponents.kt       ← refactored (imports from designsystem/, removes hardcoded values)
    FocusModeMotion.kt      ← unchanged
  theme/
    Color.kt                ← unchanged
    SurfaceTokens.kt        ← unchanged
    Theme.kt                ← unchanged

src/debug/
  ComponentCatalogScreen.kt ← new
  DebugActivity.kt          ← updated (adds Design System entry)

docs/
  DESIGN_SYSTEM.md          ← new (enforcement rules reference doc)
```
