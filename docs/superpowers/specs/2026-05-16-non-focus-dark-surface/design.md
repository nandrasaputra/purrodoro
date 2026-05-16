# Light mode + flat surfaces — Design Spec

> **Status:** approved (mockups in this folder)
> **Mockups:** `01-records-dark.png` … `08-profile-light.png`
> **Author:** Nandra + Kuro (assistant)
> **Date:** 2026-05-16

## Problem

The current UI puts time-of-day cat photographs full-bleed behind every tab.
The cat backdrop is beautiful but actively fights every text-heavy screen:

- Records calendar grid: white numbers on bright sky during day, plus busy
  brush-stroke detail behind the dates → dates blur into the painting.
- Todo cards: thin glass-card overlays only partly opaque the backdrop, so the
  task title and category tag swim on top of unrelated detail.
- Sounds list: the icon-tiles + 5 stacked rows lose hierarchy because the
  whole frame is competing visual noise.
- Profile (Me): settings labels and stepper values are the most fragile —
  small text + low contrast + variable backdrop.

The existing `GlassReadabilityMode` (Adaptive / Clear / Readable) was a
band-aid; even at "Readable" the backdrop is still doing 100% of the
visual work behind the cards. Cards never reach a "quiet workspace" state.

## Approach: Two-surface architecture

Treat the Focus tab and the four content tabs as two visually distinct
contexts:

1. **Focus tab (Timer)** — keeps the cat backdrop. This is the brand moment:
   timer, cat companion, ambient sky. Optimized for a short, lean glance
   ("how much time is left?"). Glass cards / readability work here because
   there's only ~5 short text elements.
2. **Records / Todo / Sounds / Profile** — drop the backdrop entirely. Use
   a solid surface coloured by the theme. Optimized for reading and tapping
   dense content.

This isolates "vibe" from "workspace" instead of asking one canvas to do both.

## Approach: Light + Dark + Auto

The flat-surface tabs now make light mode viable (it wasn't before, because
white text was the only way to win against the cat photo).

- **Dark surface:** `#0A0816` page · `#1A1428` cards · white-ish text.
  Matches the cat backdrop's dark scrim, so transitioning between Focus
  (cat) and other tabs feels continuous.
- **Light surface:** Cream `#FFF8ED` page · CardWhite `#FFFCF7` cards ·
  Cocoa `#5F4638` text. Reuses the existing `Color.kt` palette — no
  net-new tokens needed in code.
- **Auto:** follows the system theme via `isSystemInDarkTheme()`.
  (Alternative considered: tie to time of day. Rejected because it
  conflicts with the user's OS choice and creates an unstable surface
  during the day.)

The Focus tab keeps the cat backdrop in **all three modes**. The cat
photographs already span warm dawn/day → cool night, so they read as
"ambient time of day" rather than "dark mode". Forcing the Focus tab to
go light would lose the time-of-day signal entirely.

## Appearance toggle UI

Lives on the Profile (Me) tab as a new card placed between **Avatar** and
**Timer settings** — see mockup `04-profile-dark.png` / `08-profile-light.png`.

```
┌─ APPEARANCE ──────────────────────────┐
│ Match your time of day                │
│                                       │
│ ┌─────────┬─────────┬─────────┐       │
│ │ ☀ Light │ ☾ Dark  │ ◐ Auto  │       │
│ └─────────┴─────────┴─────────┘       │
└───────────────────────────────────────┘
```

Three-segment control. Active segment uses Coral fill + white label.
Inactive segments are flat (transparent over a faint cocoa track).
Icons: Lucide `sun`, `moon`, `contrast`.

## Surface tokens (per scheme)

| Role | Dark value | Light value | Existing token |
|---|---|---|---|
| Page background | `#0A0816` | `#FFF8ED` | (Dark) NightDeep · Cream |
| Card surface | `#1A1428` | `#FFFCF7` | NightCard · CardWhite |
| Card border | `#2B2440` | `#EBDFCC` | (new) DarkBorder · CreamBorder |
| Card active fill | `#2B1720` | `#FFE9DD` | (new) DarkActive · CreamActive |
| Card active border | `#FF8B6F` 30% | `#FF7F73` 67% | derived from accent |
| Divider | `#231D38` | `#EFE3D0` | (new) DarkDivider · CreamDivider |
| Text primary | `#FFFFFF` | `#5F4638` | white · Cocoa |
| Text secondary | `#B8B0C8` | `#8B6B5A` | (new) NightMuted · CocoaSoft |
| Text muted | `#7A7388` | `#A89486` | (new) NightFaint · CocoaFaint |
| Text faint | `#4A4458` | `#C9B9A8` | (new) |
| Accent | `#FF8B6F` | `#FF7F73` | DockCoral · Coral |
| Accent soft | `#FF8B6F` 18% | `#FFD8DF` | derived · Blush |
| Premium purple | `#7B4FBF` | `#9B7FCC` | LockedPurple · (new) |

Where "(new)" is listed, the colour is added to `Color.kt` rather than
pulled from `MaterialTheme.colorScheme` because the Compose Material
roles don't map cleanly (`onSurfaceVariant` is too generic for the
six-step text hierarchy this design uses).

## Sound icon tiles in light mode

Each sound row's leading icon tile needs a soft pastel fill so the icon
has its own colour pocket on a light card:

| Sound | Light tile fill |
|---|---|
| Rainy Window (active/selected) | Blush `#FFD8DF` |
| Cafe Purr | `#EFE6FF` (soft lavender) |
| Forest Nap | `#DDF1E5` (soft mint) |
| Fireplace | `#FFE2D5` (soft peach) |
| Night Keyboard | `#DDE7F5` (soft sky) |

In dark mode the tiles keep the existing deeper variants
(`#3F2A4A` lavender, `#1D3326` mint, etc.). Both are encoded as a
small `SoundTilePalette` map keyed on sound id.

## Glass treatment, going forward

The `GlassReadabilityMode` (Adaptive / Clear / Readable) **only affects
the Focus tab** after this redesign. The control's copy in Profile
becomes:

> **FOCUS SCREEN READABILITY**
> Only affects the cat backdrop on the Focus tab.

(see mockup `04-profile-dark.png`). The other tabs no longer have a
backdrop, so there's nothing to glass over.

`GlassTokens` keeps existing dark-mode shapes for the Focus tab. We do
**not** add a light-mode variant of `GlassTokens` — light mode never has
a backdrop, so we never reach those code paths.

## Persistence

A new key `appearance_mode` is added to `AppRepository`'s preferences
store, alongside `glass_readability_mode`. Default value is
`AppearanceMode.Auto`. Migration: missing key reads as `Auto`, which is
the safest default for existing users (matches whatever they had before,
since the app currently follows system theme by accident — Material
default).

## Out of scope

- No light-mode variant of the cat backdrop or glass treatment (Focus
  tab keeps the same look in all modes).
- No theme switching animation — instant cutover is fine.
- No high-contrast / accessibility-specific palette — that's a separate
  pass.
- No per-tab theming — Light/Dark applies globally to non-Focus tabs.
