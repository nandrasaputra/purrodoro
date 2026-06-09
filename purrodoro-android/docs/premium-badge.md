# Purrodoro Premium Badge

The **Starry Pill** premium badge (Design **A**) marks locked premium features across
the Android app. It pairs a golden sparkle star with a purple pill and white
“Premium” label — the same visual language as the premium upsell sheet header.

_Last updated: 2026-06-09_

---

## What it is

| Property | Value |
|----------|-------|
| **Name** | Starry Pill |
| **Design ID** | A (from premium badge exploration) |
| **Purpose** | Signal “this item is premium-locked” in a cute, tempting way — not a harsh lock |
| **Compose API** | `KuroPremiumBadge()` |
| **Star asset** | `R.drawable.premium_star_sparkle` |
| **Label string** | `R.string.sound_pill_premium` → `"Premium"` / `"Premium"` (ID) |

### Anatomy

```
┌─────────────────────┐
│  ✦   Premium        │   ← 9dp star + 5dp gap + SemiBold labelMeta
└─────────────────────┘
     purple gradient pill, 12dp corner radius, 10×6dp padding
```

### Brand tokens (`Color.kt`)

| Token | Hex | Usage |
|-------|-----|-------|
| `PremiumBadgeGradientTop` | `#9B7FCC` | Pill gradient top (light) |
| `PremiumBadgeGradientBottom` | `#6B43AA` | Pill gradient bottom (light) |
| `PremiumBadgeGradientTopDark` | `#A894D8` | Pill gradient top (dark) |
| `PremiumBadgeGradientBottomDark` | `#7B4FBF` | Pill gradient bottom (dark) |
| `PremiumBadgeText` | `#FFFFFF` | Label text |
| Star (asset) | golden sparkle | `premium_star_sparkle.webp` |

The pill uses a **vertical linear gradient** (lighter lavender → deeper purple) plus a
soft purple drop shadow — matching `assets/premium_badge_starry_pill.png`.

### Motion (Premium personality)

| Layer | Effect | Timing |
|-------|--------|--------|
| **Ambient** | Horizontal shimmer sweep across the pill | 2400ms loop, `FastOutSlowInEasing` |
| **Secondary** | Star scale 1.0→1.18 + opacity pulse | 1600ms reverse (offset phase) |
| **Reduced motion** | Static badge (no shimmer / twinkle) | `ANIMATOR_DURATION_SCALE == 0` |

Shimmer is subtle (peak white α ≈ 0.32) so locked tiles stay calm, not flashy.

**Related but different:** `LockedPurple` / `tokens.lockedFill` are for generic
locked pills (`KuroPillKind.Locked`). The premium badge uses a **solid** purple
fill and the dedicated star asset — do not reuse `KuroBadge` for premium.

---

## When to use

| ✅ Use `KuroPremiumBadge` | ❌ Do not use |
|--------------------------|---------------|
| Locked soundscape / lo-fi tiles | Free content |
| Future locked widgets, themes, etc. | Generic “locked” states (use `KuroPill`) |
| Only when `locked && !isPremium` | Profile “you are premium” status |

### Placement

- **Corner overlay** on tiles: `Modifier.align(Alignment.TopEnd).padding(KuroSpacing.xs)`
- **Inline** in list rows: leading or trailing slot
- Badge is **not** tappable itself — the parent row/tile handles the tap → premium upsell sheet

---

## How to use in Compose

```kotlin
import com.endiar.purrodoro.ui.designsystem.KuroPremiumBadge

// Default label ("Premium")
KuroPremiumBadge(
    modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(KuroSpacing.xs),
)

// Custom label (rare — prefer sound_pill_premium for consistency)
KuroPremiumBadge(label = stringResource(R.string.sound_pill_premium))
```

### Example: locked sound tile (`SoundsScreen.kt`)

```kotlin
if (sound.locked) {
    KuroPremiumBadge(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(KuroSpacing.xs),
    )
}
```

### Preview in dev tools

Open **Debug → Component Catalog → KuroPremiumBadge** to inspect the badge in isolation.

---

## Assets

### Star sparkle (`premium_star_sparkle`)

| Path | Role |
|------|------|
| `app/src/main/res/drawable/premium_star_sparkle.webp` | Production drawable |
| `assets/premium_star_sparkle.webp` | Repo reference copy |
| `assets/premium_star_sparkle.png` | Source / design export |

**Spec:** Four-point rounded star, golden yellow, kawaii sparkle. Displayed at **9dp**
inside the badge via `ContentScale.Fit`.

### How to regenerate the star asset

1. Generate or illustrate a **single** four-point star on transparent or purple background
   (reference: `assets/premium_badge_starry_pill.png`, Design A).
2. Export PNG (≥512px wide recommended).
3. Convert to WebP:
   ```bash
   cwebp -q 95 premium_star_sparkle.png \
     -o app/src/main/res/drawable/premium_star_sparkle.webp
   ```
4. Copy reference PNG/WebP to `assets/` at repo root.
5. Rebuild — `KuroPremiumBadge` picks up the drawable automatically.

### Design reference files

| File | Contents |
|------|----------|
| `assets/premium-badge-design-sheet.png` | Variants A–F comparison |
| `assets/premium_badge_starry_pill.png` | Finalized badge mock (A) |
| `assets/premium_sheet_header.webp` | Premium upsell hero (same star language) |

---

## How to create a new premium badge variant

If Design A needs a sibling (e.g. icon-only for tiny tiles):

1. Add exploration mock to `assets/`.
2. Add tokens to `Color.kt` (never hard-code hex in composables).
3. Create a new composable in `ui/designsystem/` (e.g. `KuroPremiumBadgeCompact`).
4. Document the variant in this file under a new heading.
5. Add a row to **Component Catalog** for visual QA.

**Do not** fork `KuroBadge` for premium — keep `KuroPremiumBadge` as the single
source of truth so sound tiles, widgets, and upsell stay consistent.

---

## Accessibility

- Badge sets `contentDescription` via `R.string.premium_badge_cd` → `"Premium badge"` / `"Lencana Premium"` (ID).
- Star image uses `contentDescription = null` (decorative; meaning carried by parent badge CD).
- Parent tile should expose `R.string.sound_unlock_cd` as `onClickLabel` when locked.

---

## Related

- Premium upsell sheet: `PremiumUpsellSheet.kt`, `premium_sheet_header.webp`
- Premium gating: `PurrodoroApp.kt` → `showPremiumSheet` / `PremiumUpsellTrigger`
- Debug premium override: Dev Tools → **Premium Override**
