# Purrodoro — Product Reference

A practical reference for how Purrodoro models a Pomodoro **session**, the
state transitions a session goes through, and how each end-path (stop, natural
finish, skip-to-end) differs.

> Source of truth: code in `purrodoro-android/app/src/main/java/com/endiar/purrodoro/`.
> If this doc and the code disagree, the code wins — please update this file.

---

## Related documentation

| Topic | Document |
|---|---|
| **Freemium / Premium (subscriptions)** | This doc → [Monetization](#monetization-in-progress) — monthly & yearly plans; Play Billing not wired yet |
| **Premium badge UI** | [`purrodoro-android/docs/premium-badge.md`](../purrodoro-android/docs/premium-badge.md) — Starry Pill lock affordance |
| **Premium branding (visual)** | This doc → [Premium branding](#premium-branding-visual-identity) — Aurora Ring, profile card, mascot |
| **Soundscape catalog (cloud)** | [`docs/superpowers/specs/2026-06-17-soundscape-admin-design.md`](superpowers/specs/2026-06-17-soundscape-admin-design.md), [`docs/superpowers/plans/2026-06-17-soundscape-admin-and-unified-catalog.md`](superpowers/plans/2026-06-17-soundscape-admin-and-unified-catalog.md) |
| **Cloud assets** | [`docs/CLOUD_ASSET_MANAGEMENT.md`](CLOUD_ASSET_MANAGEMENT.md) |
| **Design system (locked tokens)** | [`docs/DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) |
| **Auth & account identity** | [`docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`](superpowers/specs/2026-06-09-auth-and-account-identity.md) |
| **Android sync blockers / QA** | [`purrodoro-android/docs/pending-actions.md`](../purrodoro-android/docs/pending-actions.md) |

---

## What "session" means

The word "session" is overloaded in the codebase. Two senses, both are real:

### 1. Cycle-sense (the user-facing definition)

A **session** = one full Pomodoro **cycle**: a sequence of focus segments
interleaved with short breaks, capped by one long break.

For `N = focusSessionsPerCycle` (default `4`, range `1..4`), the cycle is:

```
(Focus + ShortBreak) × (N − 1)   then   Focus + LongBreak
```

Example for `N = 4` (8 segments total):

```
Focus  ShortBreak  Focus  ShortBreak  Focus  ShortBreak  Focus  LongBreak
  1                  2                  3                  4
```

Built by [`buildCycle(settings)`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt) in `KuroModels.kt:103`.

This is the unit recorded as a [`SessionRecord`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt) (`KuroModels.kt:215`):
one record per cycle attempt, written when the cycle ends (any path).

### 2. Segment-sense (per-segment terminology)

Internally each item in a cycle is a `TimerSessionItem` of a `SessionItemType`
(`Focus` / `ShortBreak` / `LongBreak`), and the in-flight runtime container is
a `TimerSession` (items + currentIndex + remainingSeconds + timerState).
Some code/UI strings refer to a single Focus segment as a "session" (e.g.
`completedFocusCount`, `totalFocusCount` on `SessionRecord`).

**When in doubt:** "session" in product copy = cycle. "Focus session" or
"focus count" inside a record = a single Focus segment within that cycle.

---

## The runtime state object

`TimerService` owns a `MutableStateFlow<RuntimeState?>`:

- `null` → no active cycle (idle home screen, no progress to show).
- non-null → a cycle is in progress *or* has just ended and the user has not
  yet acknowledged the completion dialog.

Inside `RuntimeState` lives a `TimerSession`:

| Field | Meaning |
|---|---|
| `items` | the cycle plan (list of `TimerSessionItem`) |
| `currentIndex` | which segment we're on |
| `remainingSeconds` | seconds left in the current segment |
| `timerState` | `Idle` / `Playing` / `Paused` |

The pure reducer that advances this is `TimerStateMachine` (timer ticks,
pause/resume, segment completion). `TimerService` glues it to alarms,
notifications, DataStore, and wall-clock reconciliation
(`TimerReconciler` replays elapsed time after process death).

---

## Lifecycle of a single cycle

```
                 start(settings, task)
                        │
                        ▼
                 ┌───────────────┐
                 │   Playing     │ ◀─── resume() ───┐
                 └───────────────┘                  │
                  │   │   │   │                     │
          tick    │   │   │   └── pause() ────► ┌────────┐
                  │   │   │                     │ Paused │
                  │   │   │     ◀── resume() ── └────────┘
                  │   │   │
   segment ends   │   │   └─── skip()  (advance to next segment)
                  │   │
   final segment  │   └─── (natural finish) ──┐
   ends naturally │                            ▼
                  │                   ┌────────────────────┐
                  │                   │  Reset (Idle)      │
                  │                   │  items preserved   │
                  │                   │  currentIndex = 0  │
                  │                   │  status=Completed  │
                  │                   │  pendingCycleAck=T │
                  │                   └────────────────────┘
                  │
   user taps Stop ───────► ┌─────────────────────┐
                           │  Cleared (state=null)│
                           │  status=Partial      │
                           └─────────────────────┘
```

### Three end-paths

There are exactly three ways a cycle can leave its "running" state.

#### A. Stop (user pressed Stop)

`TimerService.doStop()` (`TimerService.kt:204`):

- Persists a `SessionRecord` with `status = SessionStatus.Partial`,
  `endedAt = now`, `completedFocusCount` = focus segments finished so far.
- If the user was mid-Focus with time remaining (Playing or Paused), increments
  today's stamp via `repo.incrementStampFor(today)`. Gated by the pure helper
  `shouldStampStopAsFocus(session)` (`KuroModels.kt`) so we never double-count
  a natural finish (which already stamps inside `TimerStateMachine`).
- Sets `_state.value = null` — the entire runtime is cleared.
- UI effect: cycle dots row reverts to all-outline (no `cycleItems`,
  fallback uses `settings.focusSessionsPerCycle` for total).

#### B. Natural finish (the long break runs to zero)

`TimerStateMachine.applyTimerComplete` (the reset branch, `TimerStateMachine.kt:63`):

- Persists a `SessionRecord` with `status = SessionStatus.Completed`.
- Sets `pendingCycleCompletionAck = true` so the UI can show the completion dialog.
- Returns `state.copy(currentIndex = 0, timerState = TimerState.Idle)`.
  **`items` is intentionally preserved** so the dots row stays populated until
  the user acks the dialog.

#### C. Skip-final (user pressed Skip on the last segment)

`TimerService.buildResetState()` (`TimerService.kt:349`) takes the same path
as B by design: same `SessionRecord` (Completed), same preserved `items`,
same `currentIndex = 0` + `Idle`. The intent (per the in-source comment) is
that the user cannot tell a skip-final apart from a natural finish.

---

### Segment-level skip (not an end-path)

`doSkip()` on a non-final segment simply advances `currentIndex`, keeps the
timer running (or paused/idle, whatever it was), and does not write a record.
Only skipping the final segment terminates the cycle.

---

## Settings that shape the cycle

[`TimerSettings`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt) (`KuroModels.kt:69`) drives `buildCycle`:

| Field | Default | Range | Notes |
|---|---|---|---|
| `focusMinutes` | 25 | 1..180 | duration of every Focus segment |
| `shortBreakMinutes` | 5 | 1..180 | duration of every ShortBreak segment |
| `longBreakMinutes` | 15 | 1..180 | duration of the trailing LongBreak |
| `autoStartNextSession` | false | — | auto-advance into the next segment on completion |
| `focusSessionsPerCycle` | 4 | 1..4 | the `N` in the cycle formula above (cap aligns with home widget dot row) |

Settings are persisted to DataStore via `AppRepository`. Changing
`focusSessionsPerCycle` mid-cycle does **not** rewrite the in-flight cycle —
the running `items` list is fixed at `start()` time. The dots UI accounts
for this (it derives `totalFocusSessions` from `cycleItems` when present,
otherwise falls back to `settings.focusSessionsPerCycle`).

---

## Cycle-progress dots (UI)

Rendered by `CycleDotsRow` (`KuroComponents.kt:553`), driven by the pure
function `cycleDotsState(cycleItems, cycleIndex, timerState, settings)`
(`KuroModels.kt:128`):

| Field | How it's computed |
|---|---|
| `totalFocusSessions` | `cycleItems` Focus count, or `settings.focusSessionsPerCycle` when `cycleItems` is empty |
| `completedFocusSessions` | number of Focus indices that are `< cycleIndex` |
| `isCurrentlyFocus` | `timerState != Idle` **and** the current item is a `Focus` |

The `timerState` gate matters because of end-paths B and C above:
after a natural finish or skip-final, `cycleItems` is preserved and
`cycleIndex` is reset to `0`, but `timerState` is `Idle`. Without the gate,
the first dot would draw as the "active" peach highlight even though no
segment is actually running.

---

## Persistence

| Concern | Location | Persisted via |
|---|---|---|
| Settings | `TimerSettings` | DataStore (`AppRepository`) |
| Records | `List<SessionRecord>` | DataStore (`AppRepository.recordsFlow`) |
| Daily focus stamps | `Map<LocalDate, Int>` | DataStore (`AppRepository.setStamps` / `incrementStampFor`) |
| Pending cycle-ack flag | boolean | DataStore (`pendingCycleCompletionAckFlow`) |
| In-flight runtime | `RuntimeState` | in-memory in `TimerService`; replayed via `TimerReconciler` after process death using saved `endsAtMillis` |

A `SessionRecord` carries enough context to render history without needing
the original `TimerSettings`:

```kotlin
data class SessionRecord(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val completedFocusCount: Int,    // segments completed within this cycle
    val totalFocusCount: Int,        // = settings.focusSessionsPerCycle at start
    val status: SessionStatus,       // Completed | Partial
    val taskName: String?
)
```

---

## Monetization

Purrodoro is **freemium**: the core Pomodoro loop is free; **Purrodoro Premium** is a
**paid subscription** (monthly and yearly plans, priced low). There is **no one-time
lifetime unlock.**

**Billing (Android):** Google Play **subscriptions** via Play Billing Library 8.x
(`ProductType.SUBS`) — subscription product `purrodoro_premium` with base plans
`purrodoro-premium-monthly` / `purrodoro-premium-yearly` and introductory offer
`purrodoro-premium-yearly-trial` (7-day free trial for new subscribers; see
[`BillingConfig`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/billing/BillingConfig.kt)).

**Shipped (client):** [`BillingManager`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/billing/BillingManager.kt)
queries owned subscriptions on cold start, app foreground, sign-in, and purchase
callbacks; acknowledges purchases; drives `AppRepository.setIsPremium` from active
Play subscriptions only. Paywall UI: [`PremiumScreen`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumScreen.kt),
[`PremiumUpsellSheet`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumUpsellSheet.kt).

**Monetization go-live: done.** `purrodoro_premium` is live in Play Console as of
**2026-08-31**. Server-side verification (Play Developer API / RTDN) and private
premium audio URLs remain optional follow-ups — see
[`release-readiness.md`](../purrodoro-android/docs/release-readiness.md).

**Philosophy:** generous free core (timer, cycles, focus mode, Journey, categories,
notifications, light/dark appearance, all home-screen widgets). Premium sells
soundscape depth, higher task/habit headroom, and extra home hero scenes — at a
**low subscription price** rather than a large upfront cost.

### Entitlement

`isPremium` means the user has an **active** Google Play subscription (monthly or
yearly), including any configured free-trial period. When the subscription expires,
is cancelled, or is not renewed → premium gates return after the next billing refresh
(cold start, foreground, sign-in, or purchase callback).

```kotlin
val isPremium = PremiumOverride.overrideValue() ?: appState.isPremium
```

- **`appState.isPremium`** — DataStore cache written by [`BillingManager`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/billing/BillingManager.kt)
  from `queryPurchasesAsync` (`SUBS`, including suspended subscriptions).
- Android has no account sync. Tasks, habits, and session history stay on the device. Android Auto Backup includes the Room database. Profile → Backup & restore still exports a JSON file.
- **`PremiumOverride`** — debug-only toggle in Dev Tools; ignored in release builds.
- Premium entitlement is **device-local** (not synced via Supabase) — same Google
  account on a new device restores via **Play subscription restore**, not cloud sync.
  See [`premium-badge.md`](../purrodoro-android/docs/premium-badge.md) for lock UI.

Cap logic lives in [`PremiumGate`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/PremiumGate.kt).
Paywall UI: [`PremiumUpsellSheet`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumUpsellSheet.kt),
discovery: [`PremiumScreen`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumScreen.kt),
cap labels: [`PremiumCapUi`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumCapUi.kt).

### Free vs premium (current product)

| Surface | Free | Premium | Android status |
|---|---|---|---|
| Timer / cycles / focus mode | Full | — | Shipped |
| **Journey / stats** | **Full** | — | Shipped (never gated) |
| Task **categories** | Unlimited | Unlimited | Shipped (not capped) |
| **Active tasks** | **10** max | Unlimited | **Shipped** — counts only `!completed` tasks |
| **Habits** | **10** max | Unlimited | **Shipped** |
| Ambience + lo-fi | Rain, Cafe + 2 bundled lo-fi tracks | All locked bundled + cloud `is_premium` tracks | **Shipped** |
| Light / dark appearance | Full | — | Shipped (not a premium perk) |
| **Home hero scenes** | **Room only** | Room + Café, Library, Park | **Shipped** — picker + assets; day/night auto-switch |
| Home-screen widgets | All 8 free for everyone | — | Shipped (not gated) |

**Soft-cap rules (tasks & habits):** caps only block **creating new** items once
the limit is reached. Existing tasks and habits stay fully editable and usable.
Completing a task frees an active slot; deleting a habit frees a slot. At the
cap, the add affordance opens the premium paywall instead of the create sheet.

Entry points gated: Tasks **+**, Habits **+**, Start Focus → create new task/habit,
and the persistence layer in `PurrodoroApp.tryCreateTask` / `tryAddHabit`.

**Cap UX (free users):** Tasks and Habits headers show a subtle counter
(e.g. `8/10 active`, `8/10 habits`) via [`PremiumCapUi`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumCapUi.kt).
At the limit the counter uses the premium locked tint, the **+** button switches
to a muted purple fill, and tapping **+** opens a cap-specific paywall (title +
body explain the free limit and how to free a slot). Soundscape locks still use
the generic premium sheet.

### Soundscape (shipped detail)

Free bundled ambience: **Rain**, **Cafe**. Premium bundled: Fire, Forest, Sea
Wave, Winter Wind, plus premium lo-fi (e.g. Midnight Whisper). Cloud catalog rows
use server `is_premium` → `SoundOption.locked` / `MusicOption.locked`. See
[soundscape-admin design](superpowers/specs/2026-06-17-soundscape-admin-design.md).

### Home hero scenes (shipped detail)

Free users get the **Room** backdrop only. Premium unlocks **Café, Library, and
Park**. Each scene is one fixed environment with day/night art that auto-switches
from local time and the app theme (`resolveHomeHeroDrawable`). Entry point: the
circular **layers** button on the top-right of the home hero opens
`HomeScenePickerSheet`. Free users see locked scenes with a Premium badge; tapping
one opens `PremiumUpsellSheet`. Selection persists device-local in DataStore
(`home_scene`) and is preserved for a future upgrade even while Room renders.

### Not premium (explicit)

- Journey, calendar, session history, streaks — **free for everyone**
- Light / dark appearance — free; only **home hero scenes** are a premium sell
- Task categories — **uncapped**
- Home-screen widgets — **free for everyone**

### Still to build

- **Paywall copy cleanup** — remove stale **one-time** strings (`premium_one_time_hint`,
  `premium_screen_price_placeholder`) where still shown in older surfaces
- **Optional:** Real-time developer notifications / server entitlement check (not
  required for v1 client-side subscription query)
- **Premium cloud audio** — private R2 bucket + signed URLs (paywall is UI-only today;
  see `release-readiness.md` §8)

### Reference documents

- Premium badge: [`purrodoro-android/docs/premium-badge.md`](../purrodoro-android/docs/premium-badge.md)
- Android release / billing blockers: [`purrodoro-android/docs/release-readiness.md`](../purrodoro-android/docs/release-readiness.md)

### Premium branding (visual identity)

Premium status is communicated through a **single visual language** — not
separate marketing chrome per screen. Locked content uses the **Starry Pill**
badge; active membership uses the **Aurora Ring** and (on Profile) a
consolidated **identity card** with the same aurora stroke as home.

> Implementation: `ProfileIdentityCard`, `KuroPremiumAvatarFrame`,
> `Modifier.premiumAuroraOutline`, `KuroAccountAvatar`, `AccountAvatarStore`,
> `PremiumProfileCards.kt`, `AccountSection.kt`. Banner drawables:
> `res/drawable/profile_premium_banner.webp`, `profile_free_banner.webp`.
> Mascot reference: `assets/mascot_description.md`, `res/drawable/cat_mascot.webp`.

#### Aurora Ring (home + profile avatar)

The **Aurora Ring** is the primary “you are Premium” signal. It wraps circular
avatars on:

| Surface | Free | Premium |
|---|---|---|
| **Home** top-left profile chip (`CatAvatar`) | Plain circle, `cardBorder` stroke | `KuroPremiumAvatarFrame` |
| **Profile** unified card avatar | Plain circle or Google photo | Same frame + `premium_star_sparkle` accent at bottom-right |

**Avatar photo:** When Google sign-in supplies a profile picture URL
(Supabase `user_metadata.avatar_url` / `picture`), the app downloads and caches
it in `AccountAvatarStore` and shows it on **Home**, **Profile**, and
**onboarding** welcome. Falls back to the display-name initial when no photo
exists. Cache is keyed by auth `userId`; cleared on account switch (Drop).

**Stroke:** 2dp circular sweep gradient — lavender (`PremiumBadgeGradientTop`) →
gold (`PremiumAuroraGold` `#FFD766`) → purple (`PremiumBadgeGradientBottom`) →
gold → lavender. Dark mode uses `PremiumBadgeGradientTopDark` /
`PremiumBadgeGradientBottomDark`.

**Star accent:** `premium_star_sparkle` at bottom-right of the ring (same asset
as Starry Pill). Decorative; meaning is carried by the ring + Premium pill on
Profile.

Home and Profile **must use the same ring** — do not fork a second premium
avatar treatment.

#### Aurora outline (premium profile card border)

Premium members get the **same sweep gradient** as a **1.5dp rounded-rect
stroke** around the consolidated Profile identity card (`premiumAuroraOutline`).
This matches the home avatar ring so membership feels consistent whether the
user is on Home or Profile.

| State | Card border |
|---|---|
| **Premium** | Aurora sweep outline on the unified card (`radiusLg`) |
| **Free** | Standard `cardBorder` — no aurora |

Do **not** use a flat gold or purple solid border for premium cards.

#### Profile — Option A unified identity card (target layout)

Profile merges premium status and account into **one card** (no separate
`PremiumMemberCard` stacked above `AccountSection`).

**Premium (signed in):**

```
┌─ aurora outline (same sweep as home avatar) ─────────────┐
│ [dark starry banner — profile_premium_banner.webp]        │
│ (photo)★  Name · email · Google      │ [sync panel]     │
│ Premium pill (Starry Pill)                                │
│ View benefits ›                    Sign out               │
└───────────────────────────────────────────────────────────┘
```

- Banner: `profile_premium_banner.webp` (starry night + star-eye cat).
- Card: `premiumAuroraOutline` — lavender → gold → purple sweep (matches home).
- Avatar: Google photo when cached, else initial — inside `KuroPremiumAvatarFrame` + star accent.
- **No benefit grid** on premium Profile (benefits live on `PremiumScreen` discovery).
- Footer: **View benefits ›** (left) + **Sign out** (right).

**Free (signed in)** — same card skeleton as premium; only styling and content differ:

```
┌─ cardBorder (no aurora) ─────────────────────────────────┐
│ [soft lavender banner — profile_free_banner.webp]         │
│ (photo)  Name · email · Google       │ [sync panel]       │
│ Free pill (muted)                                         │
│ heading: "Upgrade to Premium to access…"                  │
│ unlock-icon benefit chips 2×2                             │
│ [Explore Premium] (shimmer CTA)    Sign out             │
└───────────────────────────────────────────────────────────┘
```

- Banner: `profile_free_banner.webp` (lavender paw prints + cat peek).
- Card: standard `cardBorder` only.
- Avatar: Google photo when cached, else plain initial circle — no aurora ring, no star.
- Badge: muted **Free** pill (`KuroFreeBadge`) — never `KuroPremiumBadge` on a free account.
- Heading above grid: **"Upgrade to Premium to access…"**
- Grid: static **2×2** unlock-icon chips (sounds, scenes, tasks, habits) — not horizontally scrollable.
- Footer: **Explore Premium** shimmer pill (left) + **Sign out** (right). Subscription
  pricing comes from Play on `PremiumScreen`; Profile does not duplicate plan prices.

**Guest:** same free card pattern; identity row is the guest sign-in CTA instead
of Google account details. Premium discovery still requires sign-in first
(`PremiumLoginRequiredSheet`).

#### Starry Pill (locked content — not membership)

Use `KuroPremiumBadge()` only for **locked** tiles and rows when
`locked && !isPremium`. See [`premium-badge.md`](../purrodoro-android/docs/premium-badge.md).
Do not use Starry Pill to mean “user is already Premium” — that is Aurora Ring +
Premium pill on Profile.

#### Mascot usage

The Purrodoro cat is **cartoonized** — short arms and legs, **mouthless**,
walks **upright on two legs** (not on all fours). Cream body, brown forehead and
cheek stripes, large round black eyes, pink blush, thick striped tail. Source:
`cat_mascot.webp`, `assets/mascot_description.md`.

| Context | Mascot role |
|---|---|
| Premium profile banner | `profile_premium_banner.webp` (starry night + star-eye cat) |
| Free profile banner | `profile_free_banner.webp` (lavender paw prints + cat peek) |
| Signed-in avatar (Home / Profile / onboarding) | Google photo when available, else **initial**; premium adds `KuroPremiumAvatarFrame` on Home + Profile |
| Premium discovery hero | `cat_mascot.webp` on `PremiumScreen` |

Source art: keep lossless PNGs out of git (`assets/profile_*_banner.png`); ship WebP in `res/drawable/`.
Compose drawables: `res/drawable/profile_premium_banner.webp`, `profile_free_banner.webp`.
Benefit chip unlock glyph: `res/drawable/ic_profile_benefit_unlock.xml`.

#### Color tokens (premium branding)

| Token | Hex | Role |
|---|---|---|
| `PremiumBadgeGradientTop` | `#9B7FCC` | Aurora / pill light top |
| `PremiumBadgeGradientBottom` | `#6B43AA` | Aurora / pill light bottom |
| `PremiumAuroraGold` | `#FFD766` | Gold stops in aurora sweep |
| `PremiumMemberCardWashLight` | `#F3EDFF` | Premium card fill (light) |
| `PremiumMemberCardWashDark` | `#1E1830` | Premium card fill (dark) |

#### Premium discovery screen

Full-screen hub (`PremiumScreen`) for benefits, compare table, and **subscribe**
CTA (monthly / yearly plan picker once Play Billing ships). Opened from Profile
“View benefits”, free upsell CTA, and contextual locks. Guests are routed to
`PremiumLoginRequiredSheet` before discovery. Prices and trial terms must come
from Google Play — not hard-coded one-time copy.

---

## Authentication & account sync

Purrodoro uses **Supabase Auth** as the backend. Login is optional — guests can
use the app locally without an account. Profile and app data sync across
devices only after sign-in.

> **Implementation status (updated 2026-06-25).** Design + ownership model are locked in
> [`docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`](superpowers/specs/2026-06-09-auth-and-account-identity.md).
> Scope for the first pass: **Google + local Guest on Android** (Apple + Email
> deferred; iOS needs Apple before it can ship per 4.8).
>
> **Shipped (Android):**
> - `supabase-kt` + ComposeAuth + Credential Manager; `SupabaseConfig` URL,
>   publishable key, and Google Web client ID configured.
> - Real Google sign-in (`rememberSignInWithGoogle` → `signInWithIdToken`);
>   `authState` derived from Supabase `sessionStatus`.
> - `EncryptedSessionManager` (Keystore-backed) for the Supabase refresh token.
> - `LoginSheet` / onboarding → `AuthRepository.resolveSignedInUser` →
>   `AccountManager.onSignedIn` (Claim / Keep / Drop confirm dialog).
> - Guest is **local-only** (no anonymous auth); account switch **drops** prior
>   local data after confirm. Guest "not backed up" notice in login UI.
> - Release Google sign-in verified; upload + Play App Signing SHA-1s registered
>   on the Android OAuth client.
> - Offline-first sync (Room + Supabase) for signed-in users — see sync spec.
>
> **Shipped (iOS, 2026-07-03):**
> - `supabase-swift` + `GoogleSignIn-iOS` (SPM); `SupabaseConfig` URL, publishable
>   key, and Google Web client ID (same project as Android).
> - Sign in with Apple (`AuthStore.signInWithApple` → `signInWithIdToken`); Google
>   UI ready — needs iOS OAuth client ID in `SupabaseConfig.googleIOSClientID`.
> - Keychain session storage (Supabase Swift SDK default).
> - `LoginSheet` (Profile) → `AccountManager.onSignedIn` (Claim / Keep / Drop
>   confirm alert). `ownerId` in `AccountOwnerStore`.
> - Guest local-only; signed-in restore blocked for backup. Sign-out clears session
>   only (no sync flush yet).
>
> **Remaining:** Supabase sync on iOS; StoreKit premium; Google iOS OAuth client
> + onboarding sign-in entry; NAN-65 two-device sync QA matrix (`pending-actions.md`).

### Sign-in options by platform

| Platform | Options | Notes |
|---|---|---|
| **Android** | Google, Email, Guest | No Apple Sign-In on Android |
| **iOS** | Google, Apple, Email, Guest | **Apple is required** when Google is offered (App Store guideline 4.8) |

**Guest** is not a cloud account. The user continues without signing in; data
stays on-device until they authenticate. Optionally upgrade to Supabase
**anonymous auth** so a guest session can later be linked to Google / Apple /
email without losing data.

**Email** can be implemented as magic link (simpler) or email + password.

### Where sign-in appears

| Surface | Behavior |
|---|---|
| Onboarding page 0 (Welcome) | Primary CTA **Sign in** opens login sheet; secondary **Continue as guest** advances to name page |
| Profile → Account | Guest card prompts sign-in; signed-in card shows provider + sign out |

### Identity rules (Supabase)

One Supabase user = one `auth.users.id`. That ID is the canonical key for
profile sync.

| Scenario | Result |
|---|---|
| Same Google account on Android + iOS | **Same user** — profile syncs automatically |
| Same verified email across providers (e.g. Google + magic link) | **Same user** — Supabase auto-links by email |
| Different emails on different devices (e.g. `alice@gmail.com` + `bob@icloud.com`) | **Different users** — not merged automatically |
| User wants two emails on one account | Enable **manual identity linking** in Supabase dashboard + add a Profile “Link account” flow |

**Product default:** one person uses one Google (or Apple) account on all
devices. Do not auto-merge unrelated emails.

### Profile sync (target architecture)

```
Device (local AppState cache)
        ↕ read / write on sign-in & on change
Supabase Auth  →  auth.users.id
        ↕
profiles table (RLS: user_id = auth.uid())
  display_name, timer_settings, language, …
```

On first sign-in, upload local guest data to the signed-in `user_id`. On
subsequent launches, pull remote profile and merge into local storage.

Sign-out clears the auth session only; local app data is kept (current behavior).

---

## Soundscapes & downloads

The Sounds sheet mixes two independent layers — **Ambience** (rain, cafe, …) and
**Lo-fi music** — each with its own volume, surfaced on the timer/focus screens
via a compact transport bar. Every track has one of two delivery types:

- **Local** — audio shipped in the APK (`res/raw`); plays instantly. Bundled
  filenames use kind prefixes: **`l_amb_*`** (ambience), **`l_lofi_*`** (lo-fi).
- **Remote** — audio in **Cloudflare R2**, **downloaded on demand**; objects
  use **`amb_*`** / **`lofi_*`** (no `l_` prefix). Catalog metadata lives in
  Supabase Postgres. A cloud icon marks not-downloaded, a progress ring shows
  downloading, and the file can be deleted later. Cloud track ids are `≥ 100`;
  local ids are `1–99` per kind.

### Unified catalog (local + remote)

Both catalogs are data-driven and editable without an app release:

- Remote tracks live in `public.soundscape_tracks`; audio bytes live in a public
  **Cloudflare R2** bucket (`purrodoro-soundscapes`). The app builds download URLs
  from `SoundscapeConfig.R2_PUBLIC_URL` + `file_path`.
- Local (bundled) track metadata lives in `public.soundscape_bundled_catalog`
  (migration `0005`), keyed by `(kind, local_id)`. It overrides **visibility /
  premium / order / display only** — the audio bytes still ship in the APK.
- On Android, `BundledTrackRegistry` is the source of *existence* (the APK
  defaults). The pure `SoundscapeCatalogBuilder` left-joins the registry with the
  cached DB rows + the remote catalog into the `SoundOption` / `MusicOption`
  lists (`PurrodoroApp.kt` no longer hardcodes them). A missing row → visible
  default; an inactive row → hidden (the app reads all rows, active and
  inactive, so it can tell "hidden" from "not yet seeded"). Offline falls back to
  the registry defaults.
- A debug-only web admin (`tools/soundscape-admin/`, Vite + TS) manages both
  catalogs: Postgres rows via the Supabase service role, audio uploads to R2.
  **Dev-server only** — never built or deployed (Vite would inline secrets). See
  `docs/CLOUD_ASSET_MANAGEMENT.md`.

> Source spec/plan: `docs/superpowers/specs/2026-06-17-soundscape-admin-design.md`
> and `docs/superpowers/plans/2026-06-17-soundscape-admin-and-unified-catalog.md`.

### Storage management

Profile → **Storage** opens a dedicated `StorageScreen` listing downloaded
content (today: cloud soundscapes) with total space used, per-item remove, and a
confirmed "remove all". Generic by design so future downloadable assets fit.
In-progress downloads show as a non-removable progress row. Removing a download
(single or bulk) stops playback of that track first; the user's saved mix is left
intact (the track simply reverts to "needs re-download"). Downloads are
public, re-downloadable cache (internal storage), not account-owned data, so an
account switch leaves them in place. Built on the pure `buildStorageList` /
`formatBytes` helpers (`ui/StorageModels.kt`).

> Source spec: `docs/superpowers/specs/2026-06-17-storage-management-design.md`.

---

## Glossary

- **Cycle** — one full Pomodoro plan from first Focus through the trailing LongBreak. = one `SessionRecord`.
- **Segment / item** — a single `TimerSessionItem` (Focus, ShortBreak, or LongBreak) inside a cycle.
- **Session** — overloaded; defaults to "cycle" in product copy, "Focus segment" inside `SessionRecord` field names.
- **Stamp** — a per-day tally (`completed`, `unfinished`) of Focus segments, used by the calendar/records views.
- **Pending cycle ack** — a persisted boolean indicating the completion dialog has not yet been dismissed; survives process death.
