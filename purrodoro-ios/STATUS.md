# Purrodoro iOS — Status

Native SwiftUI port of Purrodoro (Android). Tracks what's shipped on iOS vs what
Android has on `main`.

_Android source of truth:_ `purrodoro-android/app/src/main/java/com/endiar/purrodoro/`
· _Product reference:_ [`docs/PRODUCT.md`](../docs/PRODUCT.md) · _Android release
checklist:_ [`purrodoro-android/docs/release-readiness.md`](../purrodoro-android/docs/release-readiness.md)

_Last updated: 2026-07-31 (StoreKit premium foundation + free-tier caps)_

---

## Recent progress

| Slice | Status | Key files |
|-------|--------|-----------|
| Profile shell | ✅ | `ProfileView.swift` |
| Backup & restore | ✅ | `BackupCodec.swift`, `BackupManager.swift`, `BackupRestoreView.swift` |
| Auth (Google + Apple + guest) | 🔶 | `AuthStore`, `LoginSheet`, `AccountManager` — Apple live; Google needs iOS client ID |
| Supabase sync | ✅ wired | `Purrodoro/Sync/*`, `SyncCoordinator`, Profile status; NAN-65 two-device QA pending |
| StoreKit premium | 🔶 | `StoreKitManager` + gates + Profile discovery; App Store Connect products still placeholders |
| Free-tier caps (10 tasks / 10 habits) | ✅ | `PremiumGate` + Tasks/Habits upsell |
| **Android audit 2026-07-31** | docs | Android added **Focus Shield**; billing / Soundscape / 7 widgets confirmed shipped |

---

## TL;DR

**Shipped on iOS:** local-first core product — four tabs (Focus / Tasks / Journey /
Habits), full Pomodoro loop, Start Focus sheet, interrupted-focus resume,
celebration, task categories & due dates, habit scheduling & rest days, procedural
ambient sounds, EN/ID localization, onboarding, **Profile hub**, **backup &
restore** (Android-compatible JSON v1), **auth** (Supabase + Apple + Google UI,
Claim/Keep/Drop ownership), **Supabase sync** (seven tables, Profile status,
logout flush), Live Activity, one Today widget, **StoreKit premium foundation**
(discovery, upsell, 10/10 caps), unit tests (all
passing). Builds on iOS 26 simulator.

**Not on iOS yet:** **App Store Connect products** (local `.storekit` only), **cloud Soundscape**
(dual-deck + lo-fi catalog), **home scenes**, **focus-mode overlay**,
**post-launch tutorial**, **History screen**, **6 of 7 Android Glance widgets**,
**Focus Shield** (Android distraction blocker — iOS would need Screen Time / FamilyControls).
StoreKit foundation + task/habit caps shipped; **NAN-65 two-device QA** still pending.

**Next recommended port slices:** Soundscape → home scenes. Finish App Store Connect product setup + purchase QA. Run NAN-65 two-device sync QA on iOS.

---

## Parity matrix (Android → iOS)

Legend: ✅ done · 🔶 partial / substitute · ❌ missing · — N/A on iOS

### Navigation & overlays

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| 4-tab dock (Focus / Tasks / Habits / Journey) | ✅ | ✅ | `PurrodoroApp.kt` → `RootView.swift` |
| Profile / Settings screen | ✅ `ProfileScreen.kt` | 🔶 | `ProfileView.swift` — settings hub + live auth; premium + storage stub |
| Start Focus sheet | ✅ | ✅ | iOS: none / task / habit; no **simple note** intent |
| Sounds / Soundscape sheet | ✅ `SoundsScreen.kt` | 🔶 | iOS: single ambient list (`SoundsSheet.swift`) |
| Premium discovery + upsell sheets | ✅ | 🔶 | `PremiumView` + `PremiumUpsellSheet`; StoreKit placeholders |
| Login / logout / account switch | ✅ | 🔶 | `AuthStore` + `AccountManager` — Claim/Keep/Drop + sync hooks |
| History (day drill-down) | ✅ `HistoryScreen.kt` | ❌ | iOS: 8 recent rows on Journey only |
| Storage (downloaded tracks) | ✅ `StorageScreen.kt` | ❌ | |
| Backup & restore | ✅ `BackupRestoreScreen.kt` | ✅ | `BackupRestoreView.swift` — schema v1, guest-only restore, Files picker |
| Home scene picker | ✅ `HomeScenePickerSheet.kt` | ❌ | Room / Café / Library / Park |
| Focus mode overlay | ✅ `FocusModeOverlay.kt` | ❌ | Immersive zen timer surface |
| Cycle complete UI | ✅ `CycleCompleteDialog.kt` | ✅ | iOS `CelebrationView.swift` (full-screen, not dialog) |
| Post-launch spotlight tutorial (5 steps) | ✅ `Tutorial.kt` | ❌ | |
| Focus-busy dialog (block 2nd Pomodoro) | ✅ | ❌ | |
| Focus Shield (distraction app blocker) | ✅ `focusshield/*` · `FocusShieldScreen.kt` | ❌ | Android UsageStats + overlay; iOS needs different API (Screen Time / FamilyControls) |

### Timer & focus

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| Cycle builder | ✅ `buildCycle` | ✅ | `PurrodoroModels.swift` |
| Foreground timer service + exact alarms | ✅ `TimerService.kt` | — | iOS: foreground `TimerEngine` by design |
| Process-death reconcile | ✅ `TimerReconciler` | ✅ | `TimerEngine.reconcile()` |
| Auto-start next segment | ✅ | ✅ | |
| Interrupted-focus resume | ✅ | ✅ | |
| Focus types: Task / Habit / SimpleNote / Empty | ✅ | 🔶 | iOS: task / habit / none only |
| Per-day stamps + minutes | ✅ | ✅ | |
| Session records | ✅ per-cycle `SessionRecord` | 🔶 | iOS: per focus segment (`SessionHistoryStore`) |
| Habit credit on focus complete | ✅ | ✅ | via `PurrodoroApp` hook |
| Home hero scenes (day/night art) | ✅ `HomeScene.kt` | ❌ | Static mascot art only |
| Segment-end notifications | ✅ | ✅ | `NotificationScheduler.swift` |
| Chained notifications (auto-start) | ✅ | ✅ | `TimerEngine.notificationChain` |
| Notification sound toggle (completion chime) | ✅ Profile | ❌ | Chime always on when foregrounded |
| Notification permission UX + settings deep link | ✅ | 🔶 | Profile shows status + Open Settings; onboarding priming |
| Widget deep link → Start Focus / tab | ✅ `WidgetActions.kt` | ❌ | |

### Tasks

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| CRUD, priority, planned sessions | ✅ | ✅ | |
| Categories (create / filter / colors) | ✅ | ✅ | |
| Due dates + urgency badges + sort | ✅ | ✅ | |
| Archived filter | 🔶 placeholder | — | Android `TaskUiFilter.Archived` always empty |
| Live focus indicator on task row | ✅ | ❌ | |
| Free-tier cap (10 active tasks) | ✅ `PremiumGate` | ✅ | |
| Cloud sync | ✅ `TaskTableSync` | ✅ `TableSyncs` | |

### Habits

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| CRUD, emoji, color, daily target | ✅ | ✅ | |
| Occurrence (weekdays) | ✅ | ✅ | |
| Time blocks (morning / afternoon / evening) | ✅ | ✅ | |
| Rest / skip days + streak logic | ✅ | ✅ | |
| Monthly dot grid + detail sheets | ✅ | 🔶 | iOS: 7-day strip |
| Free-tier cap (10 habits) | ✅ | ✅ | |
| Cloud sync | ✅ `HabitTableSync` | ✅ `TableSyncs` | |

### Journey / stats

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| Today / week / month / all-time sections | ✅ `CalendarScreen.kt` | 🔶 | Android: stacked period cards (not a zoom toggle); iOS: week stats + month heatmap |
| Session timeline per period | ✅ `JourneyTimeline.kt` | 🔶 | Recent feed only |
| Streak (daily stamps) | ✅ | ✅ | Ported `calculateStreak` |
| Delete session record | ✅ History | ❌ | |
| Cloud sync (records + stamps) | ✅ | ✅ `TableSyncs` | |

### Auth & sync

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| Google Sign-In | ✅ `AuthRepository.kt` | 🔶 | `AuthStore` — needs `googleIOSClientID` in `SupabaseConfig.swift` |
| Sign in with Apple | — | ✅ | `AuthStore.signInWithApple()`; entitlement + Supabase Apple provider |
| Supabase session (encrypted) | ✅ Keystore | ✅ | Keychain via `supabase-swift` default storage |
| `ownerId` ownership marker | ✅ DataStore | ✅ | `AccountOwnerStore` |
| Claim / Keep / Drop on sign-in | ✅ `AccountManager.kt` | ✅ | `AccountManager.swift` + switch confirm alert |
| Offline-first LWW sync + tombstones | ✅ `SyncEngine.kt` | ✅ `Sync/` | |
| Sync status in Profile | ✅ | ✅ | `SyncStatusMiniPanel` |
| Logout flush + unsynced warning | ✅ | ✅ | `SyncCoordinator.flushBeforeLogout` |
| Avatar cache | ✅ | 🔶 | `AsyncImage` from provider URL when present |

### Premium & billing

| Feature | Android | iOS |
|---------|---------|-----|
| Play Billing 8.x / subscriptions | ✅ `BillingManager.kt` | 🔶 `StoreKitManager` | Local `.storekit`; Connect TBD |
| Monthly + yearly + trial offers | ✅ | 🔶 | Trial when yearly product offers intro |
| Premium gates (sounds, scenes, caps) | ✅ `PremiumGate.kt` | 🔶 | Caps ✅; sounds/scenes wait on those features |
| Premium branding (avatar frame, badge) | ✅ | 🔶 | Profile banner purple when premium |
| StoreKit | — | 🔶 | Foundation shipped |

### Soundscape

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| Dual deck (ambience + lo-fi) | ✅ | ❌ | |
| Bundled APK tracks (`l_amb_*`, `l_lofi_*`) | ✅ | — | |
| Cloud catalog (Postgres + R2) | ✅ `SoundscapeCatalogApi.kt` | ❌ | |
| Track downloads + storage UI | ✅ `TrackDownloadManager.kt` | ❌ | |
| Thumbnails | ✅ | ❌ | |
| Dock / compact transport on Home | ✅ | ❌ | Music button → sheet only |
| Saved mix persistence | ✅ | 🔶 | Selected ambient sound only |
| Procedural ambient (rain, cafe, …) | — | ✅ | `AmbientAudio.swift` — intentional substitute |
| Premium-locked tracks | ✅ UI gate | ❌ | `SoundOption.locked` unused |

### Backup & restore

| Feature | Android | iOS |
|---------|---------|-----|
| JSON export (Files) | ✅ `BackupManager.kt` | ✅ `BackupManager.swift` |
| Guest-only restore + safety backup | ✅ | ✅ |
| Signed-in restore blocked | ✅ | ✅ | `BackupRestoreApplicator.isGuest` |

### Onboarding

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| First-run flow | ✅ | ✅ | |
| Welcome + name + notifications | ✅ | ✅ | |
| Google sign-in or guest | ✅ | 🔶 | Profile → Sign in (`LoginSheet`); not in onboarding yet |
| Language picker in onboarding | ✅ | 🔶 | Settings only on iOS |
| Post-launch tutorial | ✅ | ❌ | |

### Settings / profile detail

| Feature | Android | iOS |
|---------|---------|-----|
| Timer durations + sessions/cycle + auto-start | ✅ | ✅ |
| Appearance (Light / Dark / Auto) | ✅ | ✅ |
| Language (System / EN / ID) | ✅ | 🔶 EN/ID only (no System) |
| Profile name | ✅ | ✅ |
| Notification sound toggle | ✅ | ❌ |
| Account section (live sign-in) | ✅ | 🔶 | `ProfileView` + `LoginSheet`; Google needs iOS OAuth client |
| About + privacy link | ✅ | ✅ | `ProfileView` about card + `AppLinks.swift` |
| Links to Storage / Backup | ✅ | 🔶 | Backup ✅ (`BackupRestoreView`); Storage stub |

### Widgets & Live Activity

| Widget / surface | Android | iOS |
|------------------|---------|-----|
| Today (sessions / minutes / streak) | ✅ `TodayProgressWidget.kt` | ✅ `TodayWidget.swift` |
| Dashboard / nudge | ✅ `NudgeDashboardWidget.kt` | ❌ |
| Daily plan (tasks + habits) | ✅ `DailyPlanWidget.kt` | ❌ |
| Streak hero | ✅ `StreakHeroWidget.kt` | ❌ |
| Stats grid | ✅ `StatsGridWidget.kt` | ❌ |
| Calm minimal | ✅ `CalmMinimalWidget.kt` | ❌ |
| Cozy scene | ✅ `CozySceneWidget.kt` | ❌ |
| Live Activity / Dynamic Island | — | ✅ `FocusLiveActivity.swift` |
| Widget → app deep links | ✅ | ❌ |

### Localization

| Feature | Android | iOS |
|---------|---------|-----|
| English + Indonesian | ✅ `values` / `values-in` | ✅ `Localization.swift` |
| Runtime switch (no relaunch) | ✅ | ✅ |
| System locale follow | ✅ `AppLanguage.System` | ❌ |
| Widget + notification localized | ✅ | 🔶 notifications yes; widget strings hardcoded EN |

### Engineering & polish

| Item | Android | iOS |
|------|---------|-----|
| Unit tests | ✅ 311+ (Gradle) | ✅ Swift Testing — all passing; includes `BackupCodecTests`, auth ownership tests |
| UI / snapshot tests | — | ❌ templates only |
| Persistence | Room + DataStore | `UserDefaults` via `KeyValueStore` |
| iPad / macOS layout | tablet-aware | ❌ phone-first |
| Accessibility sweep | broad | 🔶 Focus screen only |
| Crashlytics | ✅ release | ❌ |

---

## Architecture (iOS)

- **UI:** SwiftUI, file-system-synchronized Xcode project (drop a `.swift` file in
  `Purrodoro/` and it compiles). `PurrodoroWidgets/` extension +
  `PurrodoroShared/` (app + extension).
- **State:** `ObservableObject` stores via `@EnvironmentObject` (`TimerEngine`,
  `TaskStore`, `HabitStore`, `PreferencesStore`, `Localization`, `SoundsStore`,
  `SessionHistoryStore`, `AuthStore`, `AccountOwnerStore`).
- **Auth:** Supabase Swift SDK + Google Sign-In-iOS (SPM). Session in Keychain.
  `AuthStore` projects `Guest | SignedIn`; `AccountManager` runs Claim/Keep/Drop.
- **Persistence:** `KeyValueStore` protocol (`Persistence.swift`); `UserDefaults`
  in production, `InMemoryKeyValueStore` in tests. JSON blobs for domain data;
  per-day dictionaries for stamps/minutes. No Core Data / SQLite.
- **Timing:** foreground `Timer` + `endsAt` target; reconcile on foreground;
  chained local notifications when auto-start on; Live Activity for Lock Screen
  countdown.
- **Deployment target:** iOS 26.2 · **Bundle id:** `com.endiar.Purrodoro` ·
  **App Group:** `group.com.endiar.Purrodoro`

### Source files

| File | Responsibility |
|------|----------------|
| `PurrodoroApp.swift` | App entry, owns stores, focus-credit hook |
| `RootView.swift` | Tab shell + bottom dock + onboarding cover |
| `PurrodoroModels.swift` | Domain models (cycle, settings, dots, greetings) |
| `TimerEngine.swift` | Countdown, segment advance, reconcile, stamps, widget snapshot |
| `HomeView.swift` | Focus screen (dial, dots, controls, intent card, resume card) |
| `ProfileView.swift` | Profile hub: identity, timer, appearance, language, notifications, account, backup nav |
| `AuthModels.swift` | `AuthState`, `AuthUser`, `resolveLoginAction` |
| `SupabaseConfig.swift` | Supabase URL, publishable key, Google OAuth client IDs |
| `AuthStore.swift` | Supabase auth, Google + Apple sign-in, sign-out |
| `AccountOwnerStore.swift` | `ownerId` persistence |
| `AccountManager.swift` | Claim / Keep / Drop on sign-in |
| `AccountDataClearer.swift` | Wipe account-owned data on Drop |
| `LoginSheet.swift` | Sign-in sheet (Google, Apple, guest) |
| `AppLinks.swift` | External URLs (privacy policy, etc.) |
| `BackupModels.swift` | Backup JSON DTOs (schema v1, matches Android) |
| `BackupCodec.swift` | Encode/decode + untrusted-input validation |
| `BackupManager.swift` | Export, restore, safety snapshots (`Application Support/backups/`) |
| `BackupRestoreView.swift` | Export/import UI (Files picker, confirm dialog) |
| `PreferencesStore.swift` | Appearance, name, onboarding flag |
| `OnboardingView.swift` | First-run welcome / name / notification priming |
| `StartFocusSheet.swift` | Pre-start intent + one-shot session config |
| `CelebrationView.swift` | Cycle-complete overlay + confetti |
| `TaskStore.swift` / `TasksView.swift` | Tasks + categories + due dates |
| `HabitStore.swift` / `HabitsView.swift` | Habits + scheduling + rest days |
| `JourneyModels.swift` / `JourneyView.swift` | Streaks, heatmap, recent sessions |
| `SessionHistoryStore.swift` | Per-focus-segment history (cap 500) |
| `NotificationScheduler.swift` | Local notifications + foreground delegate |
| `SoundsStore.swift` / `SoundsSheet.swift` / `AmbientAudio.swift` | Procedural ambient audio |
| `Localization.swift` | Runtime EN/ID string table |
| `Persistence.swift` | `KeyValueStore` protocol |
| `LiveActivityController.swift` | ActivityKit lifecycle |
| `PurrodoroTheme.swift` | Brand palette (ported from Android `Color.kt`) |
| `PurrodoroShared/FocusActivityAttributes.swift` | Live Activity shared types |
| `PurrodoroShared/SharedStore.swift` | App Group `TodaySnapshot` |
| `PurrodoroWidgets/*` | Today widget + Live Activity UI |

### Android → iOS file mapping (core)

| Android | iOS |
|---------|-----|
| `timer/TimerService.kt` + `TimerStateMachine.kt` | `TimerEngine.swift` |
| `model/KuroModels.kt` | `PurrodoroModels.swift` |
| `ui/HomeTimerScreen.kt` | `HomeView.swift` |
| `ui/TodoScreen.kt` | `TasksView.swift` + `TaskStore.swift` |
| `ui/HabitsScreen.kt` | `HabitsView.swift` + `HabitStore.swift` |
| `ui/CalendarScreen.kt` + `JourneyModels.kt` | `JourneyView.swift` + `JourneyModels.swift` |
| `ui/ProfileScreen.kt` | `ProfileView.swift` + `LoginSheet.swift` |
| `ui/SoundsScreen.kt` + `soundscape/*` | `SoundsSheet.swift` + `AmbientAudio.swift` (partial) |
| `auth/AuthRepository.kt` + `AccountManager.kt` | `AuthStore.swift` + `AccountManager.swift` + sync hooks |
| `sync/*` | `Purrodoro/Sync/*` + `SyncCoordinator` |
| `billing/*` | — |
| `focusshield/*` + `FocusShieldScreen.kt` | — (needs iOS Screen Time / FamilyControls design) |
| `data/backup/*` | `BackupCodec.swift` + `BackupManager.swift` + `BackupRestoreView.swift` |
| `widget/*` (7 Glance widgets) | `PurrodoroWidgets/TodayWidget.swift` |

---

## ✅ Done (iOS)

### Core timer & focus
- [x] Cycle `(Focus + ShortBreak) × (N−1) + Focus + LongBreak`
- [x] Start / pause / resume / skip / stop
- [x] One-shot start with session overrides (`StartFocusSheet`)
- [x] Auto-start next segment (persisted)
- [x] Background reconcile on foreground return
- [x] Interrupted-focus resume card
- [x] Per-day focus stamps + minutes
- [x] Cycle-complete celebration overlay (5 copy variants)

### Four tabs
- [x] Focus home — dial, dots, mascot, greeting, today stats, intent card
- [x] Tasks — CRUD, priority, planned sessions, categories, due dates, focus attach
- [x] Habits — CRUD, occurrence, time blocks, rest days, streaks, focus attach
- [x] Journey — streaks, week stats, month heatmap, recent sessions

### Platform
- [x] Local notifications + chained schedule when auto-start on
- [x] Live Activity / Dynamic Island (`FocusLiveActivity.swift`)
- [x] Today Home Screen widget (small + medium) via App Group
- [x] Custom app icon
- [x] Haptics on timer + tab transitions
- [x] Focus-screen accessibility (VoiceOver dial, dots, button labels)

### Profile, backup & auth
- [x] Profile hub (`ProfileView.swift`) — identity card, timer, appearance, language, notifications status, about/privacy
- [x] **Auth (PR A)** — `AuthStore`, `LoginSheet`, `AccountManager`, `AccountOwnerStore`
- [x] Supabase session (Keychain) + Sign in with Apple
- [x] Google sign-in UI (`signInWithIdToken`) — paste iOS OAuth client into `SupabaseConfig.googleIOSClientID`
- [x] Claim / Keep / Drop + account-switch confirm dialog
- [x] Sign-out (session cleared; local data + `ownerId` kept)
- [x] Restore blocked while signed in
- [x] Auth unit tests (`resolveLoginAction`, `AuthProvider`, `AccountManager`)
- [x] Premium stub banner (coming-soon until StoreKit)
- [x] Storage stub row (coming soon)
- [x] **Backup & restore** — `BackupCodec` / `BackupManager` / `BackupRestoreView`
- [x] Android-compatible JSON schema v1 (cross-platform export/import)
- [x] Guest-only restore; safety copy before apply; 10 MiB read cap
- [x] Restore semantics: replace tasks/habits/settings; union stamps + session history
- [x] `BackupCodecTests` (round-trip, malformed, unsupported version)

### Sounds, onboarding, i18n
- [x] Procedural ambient sounds (rain, cafe, forest, fireplace, waves) + background audio
- [x] Onboarding (welcome → name → notifications), skippable
- [x] Runtime EN/ID localization with in-app language picker
- [x] Appearance Light / Dark / Auto

### Engineering
- [x] `KeyValueStore` persistence protocol + in-memory test conformer
- [x] Unit tests: models, Journey math, engine transitions, parity features (all passing)
- [x] Brand palette + cat mascot assets from Android

---

## 🚧 Left to do

### P0 — identity & cloud (largest Android delta)

- [x] **Profile shell** — `ProfileView.swift` replaces `SettingsSheet`
- [x] **Auth PR A** — `AuthStore`, `LoginSheet`, `AccountManager`, ownership (`ownerId`)
- [ ] **Google on device** — create iOS OAuth client; set `SupabaseConfig.googleIOSClientID` + URL scheme
- [ ] **Onboarding sign-in** — wire `LoginSheet` into `OnboardingView` (Profile only today)
- [x] **Supabase sync** — `SyncEngine`, `TableSyncs`, `SyncCoordinator`, Profile status
- [x] **Logout flush** — unsynced warning on sign-out
- [x] **Backup & restore** — schema v1; cross-platform JSON
- [x] **StoreKit premium foundation** — `StoreKitManager`, `PremiumGate` caps, Profile discovery/upsell; App Store Connect products still placeholders

### P1 — product parity

- [ ] **Soundscape** — dual deck, bundled + cloud catalog, downloads, thumbnails, premium locks
- [ ] **Home scenes** — `HomeScene.kt` art + picker + premium gates
- [ ] **Focus mode overlay** — `FocusModeOverlay.kt` immersive surface
- [ ] **History screen** — day drill-down, delete records (`HistoryScreen.kt`)
- [ ] **Start Focus: simple note** intent (`FocusType.SimpleNote`)
- [ ] **Post-launch tutorial** — 5-step spotlight (`Tutorial.kt`)
- [x] **Free-tier caps** — 10 tasks / 10 habits with upsell (`PremiumGate`)
- [ ] **Settings gaps** — notification sound toggle, System locale
- [ ] **Focus-busy guard** — block second concurrent Pomodoro
- [ ] **Focus Shield** — Android `focusshield/*` shipped; iOS port needs Screen Time / FamilyControls design (not a 1:1 UsageStats port)

### P2 — widgets & platform

- [ ] Extra widgets (dashboard, daily plan, streak, stats grid, calm, cozy)
- [ ] Widget → app deep links (open tab, auto Start Focus)
- [ ] iPad / macOS layout polish (targets enabled, phone-first UI)
- [ ] Full accessibility sweep (Tasks, Habits, Settings)
- [ ] Snapshot / UI tests (replace XCTest templates)
- [ ] Remove misleading **"Coming soon"** badge on empty states (`RootView.swift` `PlaceholderScreen`)

### P3 — nice-to-have

- [ ] Live focus indicator on task rows
- [ ] Habit monthly dot grid + detail sheets (Android parity)
- [ ] Journey period summaries (today / all-time — Android uses stacked cards)
- [ ] Glass readability mode (Android: persist + Adaptive default; **no Profile picker yet**)
- [ ] Crashlytics or equivalent
- [ ] Consider SQLite/Core Data if sync lands (volume + query needs)

---

## Suggested port order

```
1. ProfileScreen shell ✅ — `ProfileView.swift`
2. Backup export/import ✅ — `BackupRestoreView` + Android schema v1
3. Auth (Google + Apple) + encrypted session ✅ — `AuthStore.swift`, `LoginSheet.swift`, `AccountManager.swift`
4. Supabase sync (offline-first, same tables as Android) ✅ — `SyncCoordinator` + `Sync/`
5. StoreKit subscriptions + PremiumGate caps  ✅ foundation (Connect TBD)
6. Soundscape (catalog API shared with Android; AVPlayer loops)  ← next
7. Home scenes + focus overlay
8. History + tutorial + widget deep links
9. Extra widgets + iPad polish + tests
10. Focus Shield (platform-specific; design before port)
```

Specs to read before each slice:
- Auth: [`docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`](../docs/superpowers/specs/2026-06-09-auth-and-account-identity.md)
- Backup: [`docs/superpowers/specs/2026-06-19-data-backup-and-restore-design.md`](../docs/superpowers/specs/2026-06-19-data-backup-and-restore-design.md)
- **StoreKit premium (next):** [`docs/superpowers/specs/2026-07-31-ios-storekit-premium-design.md`](../docs/superpowers/specs/2026-07-31-ios-storekit-premium-design.md)
- Soundscape: [`docs/superpowers/specs/2026-06-17-soundscape-admin-design.md`](../docs/superpowers/specs/2026-06-17-soundscape-admin-design.md)
- Cloud assets: [`docs/CLOUD_ASSET_MANAGEMENT.md`](../docs/CLOUD_ASSET_MANAGEMENT.md)
- Freemium (historical): [`docs/superpowers/plans/2026-06-07-freemium-model.md`](../docs/superpowers/plans/2026-06-07-freemium-model.md)

---

## Notes / decisions

- **Foreground engine:** no `TimerService` equivalent; `TimerEngine` + chained
  notifications + Live Activity cover backgrounded timing. Android exact alarms
  are the reference for edge-case QA, not a port target.
- **Session records:** Android logs one `SessionRecord` per cycle attempt; iOS
  logs each completed **focus segment** — Journey UI reflects that difference.
- **Sounds:** procedural synthesis is an intentional iOS substitute for bundled
  `l_amb_*` tracks; full parity means cloud Soundscape + lo-fi deck, not more
  procedural presets.
- **Persistence:** `UserDefaults` + `SyncRepository` for sync metadata; revisit
  SQLite/Core Data if volume/query needs grow
  if entity count or query patterns outgrow JSON blobs.
- **`import Combine`** required in `ObservableObject` files for `@Published`.
- **Localization:** in-app table (`Localization.swift`), not `.lproj` — add both
  `.en` and `.id` for every new string. Android also supports **System** locale;
  iOS does not yet.
- **Premium scaffolding:** `SoundOption.locked` and `sounds.premium` string exist
  but are unused — wire when StoreKit lands.
- **Backup files:** schema v1 matches Android `BackupCodec.kt` field names
  (`schema_version`, snake_case keys). iOS uses UUIDs internally but exports
  stable int ids for interchange. Safety copies live in
  `Application Support/backups/` (keeps last 3). Restore blocked while timer
  running. Spec: [`docs/superpowers/specs/2026-06-19-data-backup-and-restore-design.md`](../docs/superpowers/specs/2026-06-19-data-backup-and-restore-design.md).
- **Profile entry:** Focus screen person icon → `ProfileView` sheet; sign-in at
  Profile → Account; backup at Profile → Data → Backup & restore.
- **Auth setup (one-time):** Google Cloud → OAuth **iOS** client for
  `com.endiar.Purrodoro` → paste into `SupabaseConfig.googleIOSClientID`; add
  reversed client ID URL scheme to Info.plist. Supabase dashboard → enable **Apple**
  provider (Google already enabled for Android). Spec:
  [`docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`](../docs/superpowers/specs/2026-06-09-auth-and-account-identity.md).
- **Auth behaviour:** guests are local-only (`ownerId = null`). Sign-in runs
  Claim/Keep/Drop via `AccountManager`; Drop clears account-owned data after
  confirm, then sets `ownerId`. Sign-out flushes sync when eligible, then clears
  Supabase session — data stays locally. Sync hooks drive claim-merge / delta /
  checkpoint reset through `SyncCoordinator`.
- **Sync:** offline-first LWW + tombstones for seven tables; triggers on
  foreground + debounced local writes. Profile shows sync status; logout warns
  if dirty rows remain after flush. Manual two-device QA (NAN-65) still open.
  Plan: [`docs/superpowers/plans/2026-07-03-ios-supabase-sync.md`](../docs/superpowers/plans/2026-07-03-ios-supabase-sync.md).
- **Focus Shield (Android, 2026-07-31):** session-bound distraction interrupt —
  UsageStats detect + overlay block while a Focus segment is playing
  (`focusshield/FocusShield.kt`, Profile → Focus Shield). Not in iOS yet; do
  **not** copy Android APIs — design against Screen Time / FamilyControls (or
  defer). Commit `735ab7f`.
- **Android stubs / partials (don't over-port):** `TaskUiFilter.Archived` always
  empty; Glass readability persists Adaptive default but has **no Profile
  picker** yet (`persistGlass*` unused from UI).
- **2026-07-31 audit:** Android billing, dual-deck Soundscape, History, Storage,
  scenes, focus overlay, tutorial, PremiumGate, and all 7 Glance widgets remain
  fully shipped. iOS gap list above still accurate.

---

## Verify locally

```bash
cd purrodoro-ios
xcodebuild -scheme Purrodoro -destination 'platform=iOS Simulator,name=iPhone 17' build
xcodebuild test -scheme Purrodoro -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:PurrodoroTests
```

Open `Purrodoro.xcodeproj` in Xcode → **⌘R** to run on simulator.

### Manual backup test (iOS)

1. Create tasks/habits in the app.
2. Profile → **Backup & restore** → **Export backup** → save JSON to Files.
3. (Optional) import that file on Android guest Profile → Restore to verify cross-platform.
4. On iOS: **Restore from file** → pick JSON → confirm. Tasks/habits/settings replace; stamps/history union.
5. Stop timer before restore or you'll see "Stop the timer before restoring."

### Manual auth test (iOS)

1. Profile → **Sign in** → **Sign in with Apple** (or Google once iOS client ID is set).
2. Guest with tasks → sign in → data **claimed** (`ownerId` tagged).
3. Sign out → sign in same account → **keep** (data intact).
4. Sign out → sign in different account → **switch confirm** → Drop clears local data.
5. While signed in, Backup → Restore should be disabled.

- **Android QA open (not iOS blockers):** NAN-65 two-device sync matrix per
  [`purrodoro-android/docs/pending-actions.md`](../purrodoro-android/docs/pending-actions.md).
