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
| **Freemium model (design)** | [`docs/superpowers/specs/2026-06-07-freemium-model-design.md`](superpowers/specs/2026-06-07-freemium-model-design.md) — original gating architecture; some limits below supersede it |
| **Freemium rollout (plan)** | [`docs/superpowers/plans/2026-06-07-freemium-model.md`](superpowers/plans/2026-06-07-freemium-model.md) |
| **Premium badge UI** | [`purrodoro-android/docs/premium-badge.md`](../purrodoro-android/docs/premium-badge.md) — Starry Pill lock affordance |
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

## Monetization (in progress)

Purrodoro uses a **freemium** model built around a **one-time unlock**
("Purrodoro Premium"). **No subscriptions.** Play Billing / StoreKit is not wired
yet — the paywall CTA currently flips a local `isPremium` flag; real purchase
flow is a separate, later spec.

**Philosophy:** generous free core loop (timer, cycles, focus mode, Journey,
categories, notifications, light/dark appearance). Premium sells soundscape depth,
higher task/habit headroom, and (planned) extra home-screen widgets.

### Entitlement source

```kotlin
val isPremium = PremiumOverride.overrideValue() ?: appState.isPremium
```

- **`appState.isPremium`** — DataStore flag (`AppRepository.setIsPremium`).
- **`PremiumOverride`** — debug-only toggle in Dev Tools; ignored in release builds.
- Persists across account switch (device-local, not synced). See
  [`premium-badge.md`](../purrodoro-android/docs/premium-badge.md) for lock UI.

Cap logic lives in [`PremiumGate`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/PremiumGate.kt).
Paywall UI: [`PremiumUpsellSheet`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumUpsellSheet.kt),
cap labels: [`PremiumCapUi`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumCapUi.kt).

### Free vs premium (current product)

| Surface | Free | Premium | Android status |
|---|---|---|---|
| Timer / cycles / focus mode | Full | — | Shipped |
| **Journey / stats** | **Full** | — | Shipped (never gated) |
| Task **categories** | Unlimited | Unlimited | Shipped (not capped) |
| **Active tasks** | **10** max | Unlimited | **Shipped** — counts only `!completed` tasks |
| **Habits** | **7** max | Unlimited | **Shipped** |
| Ambience + lo-fi | Rain, Cafe + 2 bundled lo-fi tracks | All locked bundled + cloud `is_premium` tracks | **Shipped** |
| Light / dark appearance | Full | — | Shipped (not a premium perk) |
| Home-screen widgets | **2 basic** (which two TBD) | Remaining 6 | **Not shipped** — all 8 widgets available today |

**Soft-cap rules (tasks & habits):** caps only block **creating new** items once
the limit is reached. Existing tasks and habits stay fully editable and usable.
Completing a task frees an active slot; deleting a habit frees a slot. At the
cap, the add affordance opens the premium paywall instead of the create sheet.

Entry points gated: Tasks **+**, Habits **+**, Start Focus → create new task/habit,
and the persistence layer in `PurrodoroApp.tryCreateTask` / `tryAddHabit`.

**Cap UX (free users):** Tasks and Habits headers show a subtle counter
(e.g. `8/10 active`, `5/7 habits`) via [`PremiumCapUi`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumCapUi.kt).
At the limit the counter uses the premium locked tint, the **+** button switches
to a muted purple fill, and tapping **+** opens a cap-specific paywall (title +
body explain the free limit and how to free a slot). Soundscape locks still use
the generic premium sheet.

### Soundscape (shipped detail)

Free bundled ambience: **Rain**, **Cafe**. Premium bundled: Fire, Forest, Sea
Wave, Winter Wind, plus premium lo-fi (e.g. Midnight Whisper). Cloud catalog rows
use server `is_premium` → `SoundOption.locked` / `MusicOption.locked`. See
[soundscape-admin design](superpowers/specs/2026-06-17-soundscape-admin-design.md).

### Not premium (explicit)

- Journey, calendar, session history, streaks — **free for everyone**
- Themes / mascot scenes — **not** a premium sell; light/dark is free
- Task categories — **uncapped**

### Still to build

- Play Billing one-time purchase + restore
- Widget picker restriction (2 free / 6 premium)
- Paywall copy cleanup (one-time price; remove stale subscription placeholder strings)

### Reference documents

- Design (partially superseded on caps/Journey/widgets): [`superpowers/specs/2026-06-07-freemium-model-design.md`](superpowers/specs/2026-06-07-freemium-model-design.md)
- Implementation plan: [`superpowers/plans/2026-06-07-freemium-model.md`](superpowers/plans/2026-06-07-freemium-model.md)
- Premium badge: [`purrodoro-android/docs/premium-badge.md`](../purrodoro-android/docs/premium-badge.md)

---

## Authentication & account sync

Purrodoro uses **Supabase Auth** as the backend. Login is optional — guests can
use the app locally without an account. Profile and app data sync across
devices only after sign-in.

> **Implementation status (2026-06-09).** Design + ownership model are locked in
> [`docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`](superpowers/specs/2026-06-09-auth-and-account-identity.md).
> Scope for the first pass narrowed to **Google + local Guest on Android** (Apple
> + Email deferred; iOS needs Apple before it can ship per 4.8).
>
> **Done (Android, merged):** the pure ownership layer + tests —
> `resolveLoginAction` (Claim/Keep/Drop) and `AuthProvider.Unknown` in
> `model/AuthState.kt`; `ownerId` marker + `clearAccountOwnedData()` in
> `AppRepository`; `auth/AccountManager` orchestration (clear-before-stamp so a
> mid-switch crash can't leak data). Guest is **local-only** (no anonymous auth);
> account switch **drops** the previous account's local data after a confirm.
>
> **Leftover / next steps (device-dependent, not yet wired):**
> 1. Add `supabase-kt` + ComposeAuth + Credential Manager deps; a `SupabaseClient`
>    configured with the project URL + publishable key.
> 2. Real Google sign-in (`rememberSignInWithGoogle` → `signInWithIdToken`),
>    replacing the random-UUID stub; derive `authState` from `sessionStatus`.
> 3. `EncryptedSessionManager` (Keystore-backed) for the Supabase session.
> 4. Wire `AccountManager.onSignedIn` into the login success path + the Drop
>    confirm dialog and guest "not backed up" notice.
> 5. **Manual (Google Cloud + Supabase dashboard):** create Google OAuth Web +
>    Android client IDs (needs the app SHA-1) and enable the Google provider.
>
> `AccountManager` is intentionally **not** wired to the live login path yet — the
> stub mints a random `user_id` per sign-in, which would make every login look
> like a different account (a destructive Drop). Wire it only once step 2 returns
> a stable `user_id`. Data **sync** itself is a separate, later spec.

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

- **Local** — audio shipped in the APK (`res/raw`); plays instantly.
- **Remote** — audio in Supabase Storage, **downloaded on demand**; a cloud
  icon marks not-downloaded, a progress ring shows downloading, and the file can
  be deleted later. Cloud track ids are `≥ 100`; local ids are `1–99` per kind.

### Unified catalog (local + remote)

Both catalogs are data-driven and editable without an app release:

- Remote tracks live in `public.soundscape_tracks` (unchanged).
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
  catalogs with the Supabase service role key. **Dev-server only** — never built
  or deployed (Vite would inline the key). See `docs/CLOUD_ASSET_MANAGEMENT.md`.

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
