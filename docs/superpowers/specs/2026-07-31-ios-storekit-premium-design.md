# iOS StoreKit Premium — Design

**Date:** 2026-07-31  
**Status:** Approved for planning  
**Platform:** iOS (`purrodoro-ios/`)  
**Android source of truth:** `purrodoro-android/.../billing/*`, `model/PremiumGate.kt`, `ui/PremiumScreen.kt`, `ui/PremiumUpsellSheet.kt`, `ui/PremiumCapUi.kt`, `ui/auth/PremiumLoginRequiredSheet.kt`  
**Product rules:** [`docs/PRODUCT.md`](../../PRODUCT.md) → Monetization  

---

## Goal

Port Android Purrodoro Premium to iOS as a **1:1 behavior mirror**, swapping Play Billing for **StoreKit 2**. This is **sub-project 1** of the larger freemium parity track (Soundscape + home scenes follow once `isPremium` exists).

**Ship in this slice**
- StoreKit 2 subscription purchase / restore / entitlement refresh
- `PremiumResolver` + DEBUG `PremiumOverride`
- `PremiumGate` (10 active tasks / 10 habits) + cap UI + upsell
- Profile discovery (`PremiumView`) + guest login gate
- Premium branding chrome on Profile (aurora / badge language)
- Upsell trigger model including `lockedSound` / `lockedScene` (scaffolded; no Soundscape/scenes UI yet)

**Explicitly out of scope**
- Full Soundscape dual-deck / cloud catalog port
- Home scene picker + art
- Server-side App Store receipt verification / ASN
- App Store Connect product creation (use placeholder IDs + local `.storekit` until Connect is ready)
- RevenueCat or other billing SDKs

---

## Decisions (locked)

| Decision | Choice |
|----------|--------|
| Scope order | StoreKit + gates **first**; Soundscape then scenes after |
| Product IDs | Placeholders mirroring Android; configurable in one file |
| Who can open discovery / subscribe | **Signed-in only** (guest → login gate), same as Android |
| Billing stack | **StoreKit 2 native** (not RevenueCat) |
| Entitlement storage | Device-local cache; restore via App Store — **not** synced as “premium true” |
| Admin grants | Supabase profile `premium_override_*` via `PremiumResolver` |
| Parity style | Copy Android types, surfaces, refresh triggers, copy keys — Swift/StoreKit only where platform requires |

---

## Architecture

```
AuthStore (signed-in?)
        │
        ▼
StoreKitManager ──► Product catalog, purchase, restore
        │              Transaction.updates / currentEntitlements
        ▼
PreferencesStore caches:
  storePremium, premiumOverrideEnabled, premiumOverrideActive
        │
        ▼
isPremium =
  PremiumOverride.overrideValue()   // DEBUG only; nil in Release
  ?? PremiumResolver.resolve(
       overrideEnabled,
       overrideActive,
       storePremium
     )
        │
        ├── PremiumGate → task / habit create caps
        ├── Profile → PremiumView (discovery)
        ├── Tasks / Habits / Start Focus → PremiumUpsellSheet
        └── (later) Soundscape / scenes locks via same triggers
```

### Android → iOS file map

| Android | iOS |
|---------|-----|
| `BillingConfig` | `StoreKitConfig.swift` |
| `BillingManager` | `StoreKitManager.swift` |
| `PremiumEntitlement` | `PremiumEntitlement.swift` |
| `PremiumResolver` | `PremiumResolver.swift` |
| `PremiumOverride` | `PremiumOverride.swift` |
| `PremiumGate` | `PremiumGate.swift` |
| `PremiumPlan` | `PremiumPlan.swift` |
| `SubscriptionOffers` + `BillingCatalogState` | `SubscriptionOffers.swift` |
| `PremiumUpsellReason` / `Trigger` / `PremiumAccessRequest` | `PremiumUpsellModels.swift` |
| `PremiumScreen` | `PremiumView.swift` |
| `PremiumUpsellSheet` | `PremiumUpsellSheet.swift` |
| `PremiumLoginRequiredSheet` | `PremiumLoginRequiredSheet.swift` |
| `FreeTierCapHeaderLabel` (`PremiumCapUi`) | `PremiumCapHeader.swift` |
| DataStore `isPremium` + override keys | `PreferencesStore` (or equivalent `KeyValueStore` keys) |
| Play Console products | `Purrodoro.storekit` (dev) + App Store Connect (later) |

---

## Product IDs

Mirror Android **string IDs** in `StoreKitConfig` (placeholders until App Store Connect exists):

| Constant | Value | Notes |
|----------|-------|-------|
| Group / catalog key | `purrodoro_premium` | Play = single subscription product; App Store = subscription **group** display name / reference — not necessarily a StoreKit product id |
| Monthly | `purrodoro-premium-monthly` | App Store auto-renewable **product id** (and Play base plan id) |
| Yearly | `purrodoro-premium-yearly` | App Store auto-renewable **product id** (and Play base plan id) |
| Yearly trial | `purrodoro-premium-yearly-trial` | Play offer id; on App Store use introductory offer / promotional offer on the yearly product (no separate product id required) |

**Platform mapping (explicit):** Play uses one `ProductDetails` + base plans. StoreKit 2 uses **two** `Product`s (monthly + yearly) in one subscription group. `StoreKitManager` loads both product ids; `PremiumPlan` selects which `Product` to purchase. Trial metadata comes from the yearly product’s subscription offer, not a third product id.

Until Connect exists, local `Purrodoro.storekit` defines monthly + yearly products with those ids for simulator QA.

---

## Entitlement rules

### Effective premium

```swift
let isPremium = PremiumOverride.overrideValue()
    ?? PremiumResolver.resolve(
        overrideEnabled: prefs.premiumOverrideEnabled,
        overrideActive: prefs.premiumOverrideActive,
        storePremium: prefs.storePremium
    )
```

`PremiumResolver` (same as Android):

```text
if overrideEnabled → overrideActive
else → storePremium
```

### Store entitlement

`PremiumEntitlement` grants premium when StoreKit reports an **active** entitlement for **either** monthly or yearly product id in `StoreKitConfig` (including trial / billing-retry states StoreKit treats as entitled — mirror Android’s “purchased / active sub” intent).

Refresh entitlement on:
- cold start
- app foreground
- sign-in
- purchase / restore completion
- `Transaction.updates` stream

Write result to `storePremium` cache after each successful refresh.

### Sync / override

- **Pull** profile DTO `premiumOverrideEnabled` / `premiumOverrideActive` into prefs (Android already applies these from sync).
- **Do not** push StoreKit entitlement as cloud “is premium”.
- Profile push may continue sending override fields as today (server-owned admin flags; client must not invent grants).

### DEBUG override

`PremiumOverride` forces premium (or forced-free) in DEBUG builds only. Release builds: source always nil — identical to Android `PremiumOverride`.

---

## Gates (this slice)

`PremiumGate` constants and helpers match Android exactly:

- `FREE_ACTIVE_TASK_LIMIT = 10` (active = not completed)
- `FREE_HABIT_LIMIT = 10`
- Soft caps: block **creating new** items at limit; existing items remain editable
- Completing a task frees a slot; deleting a habit frees a slot

**Entry points (mirror Android):**
- Tasks **+**
- Habits **+**
- Start Focus → create new task / habit
- Store-level create helpers if present

**Cap UX:** header label `n/10` via `PremiumCapHeader` when `!isPremium`; at cap, muted add affordance → `PremiumUpsellTrigger.activeTaskCap` / `.habitCap`.

---

## UI surfaces

### Profile

- Replace coming-soon premium banner with live entry:
  - **Guest** → `PremiumLoginRequiredSheet` → after sign-in, pending `PremiumAccessRequest.discovery` → `PremiumView`
  - **Signed-in** → `PremiumView`
- Identity card: premium aurora / badge branding when `isPremium` (port tokens from Android premium branding docs)

### `PremiumView` (← `PremiumScreen`)

- Compare free vs premium perks (tasks, habits, sounds, scenes — copy from Android)
- Monthly / yearly plan picker driven by `SubscriptionOffers`
- Subscribe CTA (trial CTA when yearly has trial)
- Restore purchases
- Manage subscription (App Store subscriptions URL)
- Catalog loading / failed / retry states

### `PremiumUpsellSheet`

Reasons (`PremiumUpsellReason`):

| Reason | Used this slice |
|--------|-----------------|
| `ActiveTaskCap` | yes |
| `HabitCap` | yes |
| `Generic` | yes (optional entry) |
| `LockedSound(name)` | scaffold only |
| `LockedScene(name)` | scaffold only |

Guest on upsell: sign-in gate content (Android `PremiumSignInGateContent`), then purchase after login via pending `PremiumAccessRequest.upsell(trigger)`.

### Localization

Port EN + ID strings for paywall, CTAs, cap labels, errors, restore empty — into `Localization.swift` (both `.en` and `.id`).

---

## Purchase / catalog flow

1. `StoreKitManager.refreshCatalog()` → `Product.products(for:)` → fill `SubscriptionOffers` (`Idle` → `Loading` → `Ready` | `Failed`)
2. Signed-in user selects `PremiumPlan` → `purchase(plan)`
3. Finish verified transactions; refresh entitlements; set `storePremium`
4. Cancel → clear in-flight, no error toast
5. Failure → localized error; clear in-flight
6. Pending / Ask to Buy → keep in-flight until updates settle or ~5 minute timeout (match Android)
7. Restore → `AppStore.sync()` + entitlement refresh; “nothing to restore” = info, not error

---

## Error handling summary

| Case | Behavior |
|------|----------|
| Catalog fail / missing products | `Failed` + retry |
| User cancel | silent |
| Purchase fail | short error |
| Pending transaction | in-flight + timeout |
| Guest → discovery | login gate |
| Restore empty | info message |
| Cap hit | upsell sheet |

---

## Testing

**Unit (required)**
- `PremiumGate` — under/at/over limit; premium bypass
- `PremiumResolver` — override on/off matrix
- `PremiumEntitlement` — product id / active vs not
- Offer CTA helper (trial vs subscribe) if extracted

**Manual**
- `.storekit` subscribe monthly / yearly; restore; lose entitlement after cancel + refresh
- Guest → Profile Premium → login → discovery
- Free user at 10 tasks / 10 habits → upsell; complete task frees slot
- DEBUG override toggles gates without StoreKit

**Not in CI:** live App Store sandbox.

---

## Success criteria

- [ ] Effective `isPremium` drives Profile chrome and task/habit caps
- [ ] Signed-in user can load catalog (`.storekit` or Connect) and complete purchase path in simulator
- [ ] Guest cannot open discovery without login
- [ ] EN/ID strings present for all new copy
- [ ] Unit tests for gate / resolver / entitlement pass
- [ ] Soundscape / scenes remain follow-up; triggers exist for later wiring

---

## Follow-ups (separate specs / plans)

1. **Soundscape** — dual deck, catalog/R2, downloads, `lockedSound` upsell
2. **Home scenes** — art + picker, Room free / others premium, `lockedScene` upsell
3. App Store Connect products + sandbox QA matrix
4. Optional server receipt / ASN verification

---

## References

- [`docs/PRODUCT.md`](../../PRODUCT.md) — Monetization
- [`purrodoro-android/docs/premium-badge.md`](../../../purrodoro-android/docs/premium-badge.md)
- [`docs/superpowers/plans/2026-06-07-freemium-model.md`](../plans/2026-06-07-freemium-model.md) (historical; caps now 10/10)
- [`purrodoro-ios/STATUS.md`](../../../purrodoro-ios/STATUS.md)
