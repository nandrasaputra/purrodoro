# Freemium Model — Design

_Date: 2026-06-07_

## Summary

Introduce a **freemium** model to Purrodoro built around a **one-time unlock**
("Purrodoro Premium"). The free tier keeps the entire core daily loop unblocked;
Premium sells **polish and depth**. This spec defines the **gating model and lock
UX** only — actual in-app-purchase billing (Play Billing / StoreKit) is a separate,
later spec. The whole experience is validated through the existing
`PremiumOverride` debug toggle.

> Source of truth for the entitlement flag stays:
> `val isPremium = PremiumOverride.overrideValue() ?: appState.isPremium`
> (`PurrodoroApp.kt`). This spec does not change how entitlement is sourced.

---

## Goals

- Define exactly what is free vs premium.
- Define a **centralized gating registry** so premium checks stop being ad-hoc.
- Define the **lock UX** for each kind of gated surface.
- Stay shippable and testable today via the debug premium override.

## Non-Goals (YAGNI / deferred)

- No billing SDK, receipt validation, or restore-purchases flow — later spec.
- No new premium *content* authored here (no new sounds/themes/widgets). We gate
  what already exists and leave hooks for more.
- iOS gating mirrors this model conceptually but gets its own spec.
- The in-progress ambient-sound feature continues on its own track; this spec
  only defines how its premium tier is gated, not the audio work.

---

## Monetization model

- **One-time unlock**, single product: "Purrodoro Premium." No subscriptions,
  no recurring server costs to justify.
- **Philosophy:** generous free, polish-and-depth paid. The core loop — timer,
  cycles, focus mode, unlimited tasks, basic stats, onboarding, notifications,
  light/dark themes — is never blocked.
- Locks are never dead ends: every locked affordance explains what Premium adds.

---

## What's free vs premium

| Surface | Free | Premium |
|---|---|---|
| Timer / cycles / focus mode | Full | — |
| Tasks (count) | Unlimited | Unlimited |
| Task categories | **3** (soft cap) | Unlimited |
| Habits | **5** (soft cap) | Unlimited |
| Ambient sounds | 2 (Rain, Cafe) | All + future packs |
| Themes / appearance | Light, dark, default palette | Extra color themes, mascot scenes |
| Home-screen widgets | 2 basic | All 8 (incl. Cozy Scene, Streak Hero, Stats Grid) |
| Stats / Journey | Today + current streak | Full history heatmap, long-range trends, per-category / per-habit breakdowns |

**Soft caps are loose on purpose.** Free categories = 3, free habits = 5. Most
casual users never hit them; invested users do, at the natural "I'm committed,
unlocking is worth it" moment. Tasks themselves stay unlimited and free — only
the *organizational containers* (categories) are capped. Existing items always
remain fully editable and usable, even if a user somehow exceeds a cap (e.g. a
former-premium state); caps only gate **adding new** beyond the limit.

### Free sound selection

Free = **Rain** and **Cafe**. Premium = **Forest**, **Fire**, **Keys**.

Note: today `SoundOption` flags only Forest and Fire as premium (`true`); Rain,
Cafe, and Keys are `false`. To reach a 2-free split, **Keys must be re-flagged to
premium** as part of implementation. New sounds default to premium unless
explicitly added to the free set.

---

## Gating architecture

Centralized registry, specified for Android, conceptually platform-agnostic.

### `PremiumFeature` enum

A single enum is the source of truth for "what is gated." Each entry carries its
paywall copy via string resources (localized EN/ID, consistent with the app's
runtime localization).

```
enum class PremiumFeature {
    PremiumSounds,
    Themes,
    Widgets,
    AdvancedStats,
    UnlimitedCategories,
    UnlimitedHabits,
}
```

Each entry maps to:
- a paywall **title** string resource,
- a paywall **body** string resource.

(Copy lives in `strings.xml` / `values-in/strings.xml`, not inline — replacing
the current inline sound message string.)

### Entitlement source (unchanged)

`isPremium` continues to come from
`PremiumOverride.overrideValue() ?: appState.isPremium`. The registry is a pure
consumer of this boolean — it never reads DataStore directly.

### `PremiumGate` helper

A small, pure object that centralizes the lock logic and copy lookup:

- `PremiumGate.isLocked(feature: PremiumFeature, isPremium: Boolean): Boolean`
  — returns `!isPremium` for every feature today (the indirection lets a feature
  become always-free later without touching call sites).
- `PremiumGate.copyFor(feature: PremiumFeature): PremiumCopy` — returns the
  title/body string resource ids for the paywall.
- Cap helpers:
  - `PremiumGate.categoryLimit(isPremium): Int` → `FREE_CATEGORY_LIMIT` (3) or `Int.MAX_VALUE`
  - `PremiumGate.habitLimit(isPremium): Int` → `FREE_HABIT_LIMIT` (5) or `Int.MAX_VALUE`
- Constants: `FREE_CATEGORY_LIMIT = 3`, `FREE_HABIT_LIMIT = 5`.

The helper is pure (no Android/Compose deps beyond string resource ids) so it is
unit-testable in isolation.

### Presentation stays per-feature

Each gate decides *how* to present its lock, but draws from shared design-system
primitives and routes copy through `PremiumGate`:
- shared tokens `lockedFill` / `lockedBorder` / `lockedText`, the `Locked` pill kind,
- one reusable paywall sheet (generalized from the current `PremiumMessage`).

This matches the design-system rule that reusable presentation lives in
`ui/designsystem/` and respects that a sound row, a capped add-button, and a
premium widget genuinely need different affordances.

---

## Lock UX patterns

1. **Locked item** (sounds, themes, widgets): the item is **visible** but badged
   "Premium" (`lockedFill` badge / `Locked` pill). Tapping opens the paywall
   sheet with that feature's copy. Never hidden — seeing it is the temptation.

2. **Soft cap** (categories, habits): free users add freely up to the limit. The
   "add" affordance **past** the limit opens a gentle premium nudge sheet instead
   of silently failing or disappearing. Existing items stay fully usable/editable.

3. **Locked section** (advanced stats / Journey depth): premium portions render as
   a **soft-locked preview** (blurred / teaser) with an unlock CTA, rather than
   being hidden — this shows the value the user would unlock.

4. **One paywall sheet**: generalize the current `PremiumMessage` into a reusable
   paywall component that accepts a `PremiumFeature`, renders its localized copy,
   and exposes a single unlock action. The unlock action is **stubbed** in this
   spec (e.g. flips the debug override / shows "coming soon"); real purchase
   wiring lands in the billing spec.

---

## Components touched (Android)

- **New:** `PremiumFeature` enum + `PremiumGate` helper (pure logic, `model/`).
- **New / generalized:** reusable paywall sheet in `ui/designsystem/` (from the
  existing `PremiumMessage` in `PurrodoroApp.kt`).
- **Strings:** premium paywall copy per feature in `values/strings.xml` and
  `values-in/strings.xml`.
- **Sounds:** replace the inline `if (sound.locked && !isPremium) { … inline string … }`
  check in `PurrodoroApp.kt` with a `PremiumGate` call + paywall sheet.
- **Tasks/Habits:** apply soft caps via `PremiumGate.categoryLimit` /
  `habitLimit` at the add-category / add-habit entry points.
- **Themes / Widgets / Journey:** apply locks/teasers using the same registry +
  paywall sheet. (Authoring of additional premium content is out of scope.)

No change to the entitlement source, DataStore schema, or timer logic.

---

## Testing

- **Unit (pure `PremiumGate`):**
  - `isLocked(feature, isPremium=false)` is true and `isPremium=true` is false,
    for every `PremiumFeature` entry.
  - `categoryLimit` / `habitLimit` return the free constant when not premium and
    `Int.MAX_VALUE` when premium.
  - Boundary checks around the caps (e.g. 2/3/4 categories, 4/5/6 habits) at the
    call-site logic that decides "can add another".
- **Manual:** flip the `PremiumOverride` debug switch and confirm every gate
  (sounds, category cap, habit cap, themes, widgets, advanced stats) reacts
  correctly in both states.

---

## Open follow-ups (separate specs)

- Billing integration: Play Billing (Android) + StoreKit (iOS), restore
  purchases, the real unlock action behind the paywall CTA.
- iOS gating model mirroring this registry.
- Any newly authored premium content (sound packs, themes, widgets).
