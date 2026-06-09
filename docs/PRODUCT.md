# Purrodoro — Product Reference

A practical reference for how Purrodoro models a Pomodoro **session**, the
state transitions a session goes through, and how each end-path (stop, natural
finish, skip-to-end) differs.

> Source of truth: code in `purrodoro-android/app/src/main/java/com/endiar/purrodoro/`.
> If this doc and the code disagree, the code wins — please update this file.

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

Purrodoro is moving to a **freemium** model. This work is **in progress** — the
design and implementation plan are written; gating is being built behind the
existing debug `PremiumOverride` toggle, and in-app-purchase billing is not yet
wired.

- **Model:** one-time unlock ("Purrodoro Premium"). No subscriptions.
- **Philosophy:** generous free — the core loop (timer, cycles, unlimited tasks,
  basic stats, notifications, light/dark) is never blocked. Premium sells polish
  and depth.
- **Free vs premium (planned):** premium sounds (free: Rain + Cafe), extra
  themes/mascot scenes, the full widget set, advanced Journey insights, and
  removal of the soft caps on task categories (free: 3) and habits (free: 5).
- **Gating:** a centralized `PremiumFeature` registry + pure `PremiumGate` helper
  consume the existing `isPremium` source
  (`PremiumOverride.overrideValue() ?: appState.isPremium`); a reusable paywall
  sheet surfaces locks. Billing integration is a separate, later spec.

Reference documents:
- Spec: [`docs/superpowers/specs/2026-06-07-freemium-model-design.md`](superpowers/specs/2026-06-07-freemium-model-design.md)
- Plan: [`docs/superpowers/plans/2026-06-07-freemium-model.md`](superpowers/plans/2026-06-07-freemium-model.md)

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

## Glossary

- **Cycle** — one full Pomodoro plan from first Focus through the trailing LongBreak. = one `SessionRecord`.
- **Segment / item** — a single `TimerSessionItem` (Focus, ShortBreak, or LongBreak) inside a cycle.
- **Session** — overloaded; defaults to "cycle" in product copy, "Focus segment" inside `SessionRecord` field names.
- **Stamp** — a per-day tally (`completed`, `unfinished`) of Focus segments, used by the calendar/records views.
- **Pending cycle ack** — a persisted boolean indicating the completion dialog has not yet been dismissed; survives process death.
