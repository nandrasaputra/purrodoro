# Auth & Account Identity — Design

_Date: 2026-06-09_

## Summary

Replace Purrodoro's **local auth stub** with real **Supabase Auth**, and define
the **account-identity and local-data-ownership** model that a later sync feature
depends on. Sign-in is the foundation: data only syncs for a signed-in user, so a
stable, server-issued identity must exist before any sync work begins.

This spec covers **authentication + identity + local-data ownership** only. The
actual cross-device **data sync** (schema, RLS-scoped tables, delta sync, conflict
resolution) is a **separate, later spec** — this one makes it possible by
guaranteeing every synced row can be attributed to one stable `user_id`.

**Scope of providers (this pass): Google + local Guest only.** Apple and Email
are deferred (see follow-ups). Because of that, the first shippable target is
**Android** (Google + Guest); iOS sign-in follows once Apple is added — see the
App Store 4.8 constraint below.

> Supabase project: **purrodoro** (`birjkyerhokqdzhdfgpt`, org `Endiar`,
> region `ap-southeast-2`, Postgres 17).
> Canonical identity = `auth.users.id` (per `PRODUCT.md` → "Authentication &
> account sync"). This spec does not introduce any other identity key.

---

## Goals

- Stand up **real Supabase Auth** (Google) on Android, replacing the stub.
- Define the **local-only guest** model: guests never sync, data stays on-device.
- Define a **local-data ownership** model that deterministically answers "whose
  data is on this device?" across guest→login and account-switch scenarios.
- Keep auth **non-destructive until sync ships**, guarded by user-facing warnings.
- Keep the existing `LoginSheet` / `AccountSection` UX on Android; swap only the
  backend.

## Non-Goals (YAGNI / deferred)

- **Apple + Email providers** — deferred to a follow-up. Apple is a hard
  prerequisite for the **iOS** release (4.8); see below.
- **Data sync itself** — tables, RLS, delta/merge, conflict resolution: separate
  spec. This spec stops at "identity + ownership are trustworthy."
- **UUID migration of local entities** — required *for sync*, specified in the
  sync spec, not here.
- **Profile sync / `profiles` table** — created in the sync spec; not needed to
  authenticate.
- Manual multi-email identity linking. Product default stays "one person, one
  Google account." Deferred.
- Billing / premium entitlement changes — unrelated; see the freemium spec.

---

## Identity model

One Supabase user = one `auth.users.id`. That id is the **only** canonical key
for ownership and (later) sync. It is:

- **Stable** across sessions (sign out / sign in on the same account → same id).
- **Stable** across devices and platforms (same Google account → same id).
- **Server-issued**, never client-minted.

This is the critical fix over today's stub, which mints a fresh
`UUID.randomUUID()` on every `signInWithGoogle()` call — a per-sign-in random id
that is unusable as a sync key.

| Scenario | Result |
|---|---|
| Same Google account, two devices | Same `user_id` |
| Different Google accounts | Different `user_id` — never auto-merged |

> Cross-provider auto-linking (e.g. Google + Apple/email sharing one verified
> email) is out of scope until those providers land. Note for later: Apple's
> "Hide My Email" relay address will **not** match a Google email, so the same
> person signing in with Google on one device and Apple on another would get
> **two** `user_id`s. That wrinkle is the follow-up spec's problem, not this one's.

---

## Provider matrix

| Platform | This pass | Later |
|---|---|---|
| **Android** | **Google, Guest** | — |
| **iOS / macOS** | (not shipped this pass) | **Google + Apple** (Apple required by 4.8), Guest |

- **Google** — Credential Manager (Android) produces a Google ID token, exchanged
  via Supabase `signInWithIdToken`.
- **Guest** — **local-only, not an account** (see below).

**App Store 4.8 constraint:** once Google (a third-party login) is offered on
iOS, Apple Sign-In becomes **mandatory** for App Store approval. So iOS cannot
ship Google-only. We therefore lead with **Android (Google)** now and add **iOS +
Apple** in a follow-up. macOS, if shipped, uses the same SwiftUI `AuthStore` as
iOS (not a separate codebase).

---

## Guest model (local-only)

**Decision: guests do not sync. Guest data stays entirely on-device.** A guest is
simply an un-authenticated user of the local app; there is no Supabase session,
anonymous or otherwise. Syncing requires signing in.

Implications:
- No anonymous Supabase auth. A guest has **no `user_id`**.
- Guest-created data is **unclaimed** — owned by no account until a first login
  claims it.
- Sign-out returns the app to the guest state; **local data is kept** (current
  behavior), but its ownership is tracked explicitly (next section).
- **Guests are told their data is local-only and not backed up** until they sign
  in (see Warnings).

---

## Local-data ownership

The device holds exactly one local dataset. The hard question is *whose* it is —
especially when a user signs out and a different account signs in. The device
cannot infer this after the fact, so ownership is **recorded explicitly** at the
moment data is created or claimed.

### The owner marker

One nullable scalar in local storage (DataStore on Android, `KeyValueStore` on
iOS), **independent of the current auth session**:

```
ownerId : String?
    null         → unclaimed (guest data; never signed in)
    "<user_id>"  → owned by that Supabase account
```

It is **not** "who is signed in right now" — it survives sign-out, so the data
keeps its owner while the session is gone. That's the whole point.

### Two rules (plus a no-op)

On a successful login, compare the incoming `user_id` to `ownerId`:

| `ownerId` | Login as | Action |
|---|---|---|
| `null` | A | **Claim** — set `ownerId = A`. Keep all data. |
| A | A | *No-op* — already A's. Keep. |
| A | B | **Drop (with warning)** — confirm, **clear data, then** set `ownerId = B`. |

That's it. The only correctness rule: on **Drop**, clear the data *before*
writing the new `ownerId` — stamp-first-then-crash would leave B inheriting A's
leftovers (a leak). Clear-then-stamp makes a mid-operation crash harmless (it
just re-evaluates to Drop on next launch).

> Pre-sync, **Drop is the only destructive action**, and it never runs without
> the user's confirmation (below). Once sync ships, Drop becomes "clear local,
> then pull B's cloud" — same rule, now safe because A's data is backed up.

### Warnings (user-facing)

Two placements — the at-risk user (A) isn't present when B signs in:

1. **Guest "not backed up" notice** — a low-key hint at the sign-in entry point:
   "Saved on this device only. Sign in to back it up." Sets expectations.
2. **Drop confirmation** — when a **different** account signs in over existing
   data: "This device has another account's data. Signing in will remove it.
   Continue?" The clear runs only on confirm.

### Scenario walk-through

**A. Guest → login (claim):** `ownerId = null` → login as A → **Claim**: tag the
data as A's, keep it. The only path that absorbs guest data into an account.

**B. Login A → logout → login B (drop):** A signs in (`ownerId = A`), signs out
(session gone, `ownerId` stays A, data kept), then B signs in → `B ≠ A` →
**Drop**: confirm, clear, set `ownerId = B`. B never sees A's data.

**C. Login A → logout → guest edits → login A again (keep):** data still tagged
`ownerId = A`, so re-login as A is a no-op and the edits stay. (Had B signed in
instead, Drop applies.)

---

## Edge cases

### What "clear local" includes vs. keeps

On a confirmed **Drop**, **clear** the account-owned domain data and in-flight
runtime:
- tasks, categories, habits + habit logs, session records, daily stamps,
  `TimerSettings`, `completedSessions`, `interruptedFocus`, and the in-flight
  `RuntimeState` / runtime snapshot (stop any running timer first),
- `userName` (it personalizes the greeting from the account — must not linger as
  "Hi, A" for user B).

**Keep** device-local, non-account preferences: appearance mode, language,
`hasSeenOnboarding`, `hasSeenTutorial`. These belong to the device, not the
account.

### Unsynced data at logout (once sync exists)

When sync exists, on **logout** attempt a **best-effort final sync flush** while
online; if unsynced changes exist with no network, warn before completing logout.
The Drop-on-confirmed-different-account rule stays absolute. (Pre-sync there is no
cloud to flush to; the warnings above are the only guard, by design.)

### Display name vs. account identity

`resolvedDisplayName` / `isNameFromAccount` (in `AuthState.kt`) already reconcile
a local name with an account display name. The `ownerId` marker governs *data
ownership*; the display name remains a separate, cosmetic reconciliation.

---

## Security — session storage

The Supabase session contains a long-lived **refresh token** (a bearer
credential). It must **not** sit in plain DataStore / `UserDefaults`.

- **Android:** persist the Supabase session in **Keystore-backed
  EncryptedSharedPreferences** (custom session storage for `supabase-kt`).
- **iOS (later):** persist in the **Keychain**.

`authState` derives from the live Supabase session, not a hand-rolled record.

---

## Architecture — Android (this pass)

Today's `auth/AuthRepository.kt` is a local stub (random-UUID session in its own
DataStore) with built UI (`ui/auth/LoginSheet.kt`, `AccountSection.kt`) and a
`Guest | SignedIn` `AuthState`. Strategy: **keep the UI and `AuthState` shape,
replace the backend.**

- **Add** the Supabase Kotlin SDK (`supabase-kt`: auth module) with
  encrypted session storage (above).
- **Rewrite** `AuthRepository` to delegate to Supabase:
  - `signInWithGoogle` → Credential Manager → Google ID token →
    `signInWithIdToken`. (Remove the fake-email/random-UUID stub.)
  - `signOut` → Supabase sign-out; retain local data per ownership rules.
  - `authState` derives from the Supabase session (`user_id`, email, provider).
- **`AuthProvider`** (`model/AuthState.kt`): keep `Google`, but replace the
  `else → Google` coercion in `fromStorageTag` with an explicit `Unknown`
  fallback so a future Apple/Email tag can't silently decode as Google.
- **Add** `ownerId` storage + the login decision rule (pure function, below) +
  the Claim / Drop side-effects + the Warnings UX.
- `LoginSheet` keeps the existing Google button and "continue as guest"; add the
  guest "not backed up" notice.

## Architecture — iOS / macOS

**Status (2026-07-03):** shipped on iOS — see [`purrodoro-ios/STATUS.md`](../../../purrodoro-ios/STATUS.md).

- **Added** the Supabase Swift SDK (auth) with **Keychain** session storage.
- **`AuthStore`** (`ObservableObject`, injected like other stores) exposes
  `Guest | SignedIn` and delegates sign-in to Supabase.
- **`LoginSheet`** wired at Profile → Account (onboarding sign-in deferred).
- Providers: **Google + Apple + Guest.** Apple via `ASAuthorizationController`
  → `signInWithIdToken` (capture name on first authorization).
- **`ownerId`** in `AccountOwnerStore` (`KeyValueStore`).
- **`AccountManager`** implements Claim / Keep / Drop (same pure rule as Android).

**Still open on iOS:** Supabase sync, logout flush, onboarding sign-in entry,
Google iOS OAuth client ID + URL scheme for device Google sign-in.

## Shared / pure logic (both platforms)

The login decision rule is identical on both platforms — a **pure function**:

```
enum LoginDataAction { Claim, Drop, Keep }   // Keep = same account, no-op

fun resolveLoginAction(ownerId: String?, incomingUserId: String): LoginDataAction =
    when {
        ownerId == null             -> Claim
        ownerId == incomingUserId   -> Keep
        else                        -> Drop
    }
```

Each platform implements it natively (Kotlin / Swift); the truth table is the
contract and is tested identically on both.

---

## Supabase setup (dashboard — not MCP)

Provider configuration lives in the Supabase **dashboard**, not in migrations:

- **Google** OAuth client(s): Android client ID + a web client ID for the
  `signInWithIdToken` exchange.
- **Apple** Sign-In (Service ID + key) — **later**, required before iOS ships.
- **Email provider: off** for now (deferred).
- **Anonymous sign-in: OFF** (guests are local-only by decision).

No custom SMTP is needed this pass (no email auth). No `public` schema tables are
required for auth itself — `auth.users` is managed by Supabase. Domain tables +
RLS arrive in the sync spec.

---

## Testing

Follow the existing conventions: **JUnit4** (`org.junit.Test`, backtick names,
`org.junit.Assert.*`), `kotlinx-coroutines-test` `runTest` for suspend code, and
the `TemporaryFolder` + `PreferenceDataStoreFactory` pattern from
`AuthRepositoryTest` for DataStore-backed tests. Keep all logic-under-test pure or
DataStore-backed so it stays in `src/test` (JVM, no instrumentation).

**Required unit tests (this pass):**

1. **`resolveLoginAction` truth table** — pure, the heart of the model. Put the
   function next to `AuthState` and extend `model/AuthStateTest.kt` (or add
   `model/LoginDataActionTest.kt`):
   - `ownerId = null`, any user → `Claim`
   - `ownerId = "A"`, login `"A"` → `Keep`
   - `ownerId = "A"`, login `"B"` → `Drop`
   - `ownerId = null`, login `""`/blank → still `Claim` (guard against empty id)

2. **`AuthProvider.fromStorageTag` fallback** — in `AuthStateTest.kt`:
   - `"google"` → `Google` (round-trips)
   - unknown / future tag (`"apple"`, `"email"`, `""`, `null`) → `Unknown`
     (the explicit fallback, **not** silently `Google`)

3. **`clearAccountOwnedData` keep/clear sets** — new `data/AppRepositoryClearTest.kt`,
   using the `TemporaryFolder` DataStore pattern. Seed every key, run the clear,
   then assert:
   - **cleared:** tasks, categories, habits + logs, session records, stamps,
     settings keys, `completedSessions`, `interruptedFocus`, runtime snapshot,
     `userName`
   - **kept:** appearance, language, `hasSeenOnboarding`, `hasSeenTutorial`,
     and `ownerId` is updatable separately
   This pins the keep/clear contract so a future key isn't accidentally leaked
   across an account switch.

4. **`ownerId` persistence** — extend `data/AppRepositoryTest.kt`: default is
   `null`; set/read round-trips; survives an independent repo instance on the
   same DataStore.

5. **Sign-in orchestration** — new `AccountManagerTest.kt` (the class that runs
   `onSignedIn`). Inject an in-memory/temp-DataStore repo and a fake confirm
   gate; assert:
   - `null` owner → `Claim` tags `ownerId`, data untouched
   - same owner → `Keep`, nothing changes
   - different owner + **confirm** → data cleared **then** `ownerId = B`
     (assert order: data already empty when the new id is written)
   - different owner + **cancel** → no clear, `ownerId` unchanged

**Not unit-tested (manual / integration):** real Google Credential Manager flow,
encrypted-session round-trip on device, and the four Android scenarios already
listed — these need a device + the live Supabase project, so they stay manual.

---

## Open follow-ups (separate specs)

- **iOS auth polish** — onboarding sign-in entry; Google iOS OAuth client + URL
  scheme; avatar cache. Core auth + Apple shipped 2026-07-03.
- **Email provider** — magic link vs. **email OTP**; on native, OTP avoids
  deep-link/redirect plumbing and is likely simpler. Requires a **custom SMTP**
  provider (Resend / SES / Postmark) — the built-in SMTP only emails org members
  and is not for production.
- **Data sync** — `profiles` + domain tables, RLS (`user_id = auth.uid()`),
  client-generated UUID migration, delta sync + per-entity conflict rules
  (append-only records, LWW for mutable rows, derived stamps). Enables the
  destructive **Drop** to become a safe cloud-restore. Depends on this spec.
- **Manual identity linking** — Profile "Link account" flow for one person with
  multiple providers/emails (Supabase manual linking).
