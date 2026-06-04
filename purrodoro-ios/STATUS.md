# Purrodoro iOS — Status

A native SwiftUI port of the Purrodoro (Android) focus app. This doc tracks
what's implemented and what's still open.

_Last updated: 2026-06-04_

---

## TL;DR

The core focus loop and all four primary tabs are working: **Focus, Tasks,
Journey, Habits**, plus segment-end local notifications. The app builds and runs
on the iOS 26 simulator. A recent parity pass added the **Start Focus sheet** +
**interrupted-focus resume**, a **cycle-complete celebration**, **task
categories & due dates**, **habit occurrence / time blocks / rest days**,
**ambient focus sounds** (real, procedurally-synthesised audio with background
playback), and **runtime English/Indonesian localization**. A follow-up
platform pass added a **Live Activity / Dynamic Island** for the running timer
and a **"Today" Home Screen widget** (WidgetKit), backed by an App Group. A
final polish pass shipped a **custom app icon**, **chained background
notifications** for auto-start, full **onboarding + Journey localization**, an
**accessibility pass** on the Focus screen, **engine transition tests**, and
moved persistence **behind a `KeyValueStore` protocol**. The main remaining work
is **iPad/macOS layout polish** and **snapshot/UI tests**.

---

## Architecture

- **UI:** SwiftUI, file-system-synchronized Xcode project (drop a `.swift` file in
  `Purrodoro/` and it compiles — no `.pbxproj` editing). The `PurrodoroWidgets/`
  extension is likewise file-system-synchronized; `PurrodoroShared/` holds code
  compiled into *both* the app and the extension.
- **State:** plain `ObservableObject` stores injected via `@EnvironmentObject`
  (`TimerEngine`, `TaskStore`, `HabitStore`).
- **Persistence:** behind a `KeyValueStore` protocol (`Persistence.swift`);
  `UserDefaults` is the production conformer, `InMemoryKeyValueStore` backs
  tests. JSON for tasks/habits/settings/history; per-day dictionaries for focus
  stamps/minutes. No Core Data (the template was removed).
- **Timing:** foreground `Timer` publisher driving a countdown derived from a
  target `endsAt` date, with reconcile-on-foreground for time elapsed while
  backgrounded.
- **Deployment target:** iOS 26.2 · **Bundle id:** `com.endiar.Purrodoro`.

### Source files

| File | Responsibility |
|------|----------------|
| `PurrodoroApp.swift` | App entry, owns the stores, wires the focus-credit hook |
| `RootView.swift` | Tab shell + custom bottom dock, routes the 4 tabs |
| `PurrodoroModels.swift` | Pure domain models (cycle, settings, dots, greetings) |
| `TimerEngine.swift` | Countdown, segment advance, auto-start, reconcile, stamps |
| `HomeView.swift` | Focus screen (dial, dots, controls, active-intent card) |
| `SettingsSheet.swift` | Durations / sessions-per-cycle / auto-start / appearance |
| `PreferencesStore.swift` | App-wide prefs (appearance, name, onboarding flag), persisted |
| `OnboardingView.swift` | First-run intro: welcome / name / notification priming |
| `TaskStore.swift` / `TasksView.swift` | Tasks feature |
| `HabitStore.swift` / `HabitsView.swift` | Habits feature |
| `JourneyModels.swift` / `JourneyView.swift` | Streaks, stats, calendar heatmap, recent sessions |
| `SessionHistoryStore.swift` | Persisted log of completed focus sessions |
| `NotificationScheduler.swift` | Local notifications at segment end |
| `PurrodoroTheme.swift` | Brand palette + per-mode accents |
| `StartFocusSheet.swift` | Pre-start sheet: pick intent + adjust this session; powers resume |
| `CelebrationView.swift` | Full-screen cycle-complete celebration + confetti |
| `SoundsStore.swift` / `SoundsSheet.swift` | Ambient-sound selection/playback state + picker sheet |
| `AmbientAudio.swift` | Procedural (asset-free) ambient audio engine (AVAudioEngine) |
| `Localization.swift` | Runtime-switchable EN/ID string table (`@EnvironmentObject`) |
| `Persistence.swift` | `KeyValueStore` protocol + `UserDefaults`/in-memory conformers |
| `LiveActivityController.swift` | Starts/updates/ends the running-timer Live Activity (ActivityKit) |
| `PurrodoroShared/FocusActivityAttributes.swift` | Live Activity attributes/state, shared app↔extension |
| `PurrodoroShared/SharedStore.swift` | App Group store for the "Today" widget snapshot |
| `PurrodoroWidgets/PurrodoroWidgetsBundle.swift` | Widget extension entry (`WidgetBundle`) |
| `PurrodoroWidgets/FocusLiveActivity.swift` | Lock Screen + Dynamic Island UI for the timer |
| `PurrodoroWidgets/TodayWidget.swift` | Home Screen "Today" widget (sessions / minutes / streak) |
| `PurrodoroWidgets/WidgetTheme.swift` | Brand palette mirror for the extension |

---

## ✅ Done

### Core timer
- [x] Cycle construction `(Focus + ShortBreak) ×(N-1) + Focus + LongBreak`
- [x] Countdown with start / pause / resume / skip / stop
- [x] Auto-start-next-segment option
- [x] Background reconcile (advances elapsed segments on return)
- [x] Per-day focus **stamps** + **minutes** recorded
- [x] Cycle-complete acknowledgement (alert)

### Focus home
- [x] Greeting by time of day, today's focus count + minutes
- [x] Circular progress dial + mode chip + mascot art
- [x] Cycle progress dots (active/completed)
- [x] Active-intent card (task **or** habit) with detach

### Settings
- [x] Focus / short / long durations, sessions-per-cycle, auto-start
- [x] Persisted; idle preview refreshes live
- [x] Appearance mode (Light / Dark / Auto), persisted, applied app-wide
- [x] Editable profile name (persisted) — personalizes the home greeting

### Navigation
- [x] Custom warm bottom dock (Focus / Tasks / Journey / Habits)

### Tasks
- [x] CRUD, priority (Low/Normal/High), planned sessions, complete, delete
- [x] "Focus on this" → attaches task; sessions credit toward planned
- [x] Auto-complete when planned sessions met; active task persists

### Journey
- [x] Current streak + best streak (ported from Android `calculateStreak`)
- [x] This-week minutes / total sessions stats
- [x] Navigable month calendar heatmap, today ringed
- [x] Recent sessions feed (per-session history with intent name + timestamp)

### Habits
- [x] CRUD, emoji + color, daily target, per-day log
- [x] 7-day completion strip, per-habit streak, tap-to-log
- [x] "Focus on this" → attaches habit; sessions credit the daily target

### Notifications
- [x] Local notification at segment end (focus done / break over / cycle complete)
- [x] Foreground banner + chime; cancels on pause/stop

### Onboarding
- [x] First-run flow (welcome → name → notification priming), skippable
- [x] Persisted completion flag; greeting + notifications wired in

### Start Focus & resume
- [x] Start Focus sheet: choose intent (none / task / habit) and tune this
      session's durations + sessions-per-cycle without changing the saved
      defaults (one-shot `engine.start(using:)`)
- [x] Interrupted-focus resume: stopping mid-focus remembers the leftover
      minutes and offers a "continue where you left off" card on the Focus screen

### Celebration
- [x] Cycle-complete celebration overlay (mascot, confetti, 5 rotating copy
      variants) replacing the old system alert

### Sounds
- [x] Ambient focus sounds (rain, cafe, forest, fireplace, waves) — **real**
      procedurally-synthesised audio (coloured noise + filtering), no shipped
      audio assets
- [x] Looping + background playback (`AVAudioSession .playback`,
      `UIBackgroundModes = audio`); music button on the Focus screen opens the
      sounds sheet; playback continues after the sheet closes

### Misc
- [x] Brand palette ported from Android `Color.kt` (light + dark tokens)
- [x] Reused Android cat mascot art as image assets
- [x] Consolidated `.gitignore` at repo root (Android + iOS)
- [x] Runtime localization (English + Indonesian) with an in-app string table;
      language picker in Settings switches the whole UI live (no relaunch)

---

## 🚧 Left to do

### Parity with Android (feature gaps)
- [x] **Ambient focus sounds** with looping audio + background playback — done
      (procedural synthesis instead of bundled files; waves replaces the
      Android "keys" texture)
- [x] **Onboarding** flow (first-run intro + permission priming) — done
- [x] **Profile / full Settings screen**: appearance ✅ + profile name ✅ +
      language ✅ — done
- [x] **Localization** (Indonesian `id`) — all flows localized with a runtime
      language switch, now including onboarding and the full Journey tab.
- [x] **Session history records** — per completed focus session (timestamp,
      minutes, intent name), persisted + shown on Journey. (Android logs full
      per-cycle records; iOS logs per focus segment, which is the useful unit.)
- [x] **Start Focus sheet** (confirm minutes / pick intent before starting;
      adjust leftover time when resuming an interrupted focus) — done
- [x] **Interrupted-focus resume** ("continue where you left off") — done
- [x] Task **categories** (free-text, deterministic colors) and **due dates**
      (overdue / due-today badges, due-aware sorting) — done
- [x] Habit **occurrence** (weekdays) and **time blocks**; **rest days**
      (one-off skip via context menu; streak skips unscheduled days) — done
- [x] Cycle-complete **celebration** (richer than the old alert) — done

### iOS platform extras
- [x] **Home Screen widgets** (WidgetKit) — "Today" widget (small + medium)
      mirrors focus sessions / minutes / streak from an App Group snapshot,
      matching the Android Glance "Today" widget
- [x] **Live Activity / Dynamic Island** for the running timer — Lock Screen
      banner + Dynamic Island (compact / expanded / minimal) with a live
      countdown, mode accent, cycle dots, and attached intent name. Driven from
      the engine's transition hooks via `LiveActivityController`
- [x] **App icon** — custom cat-on-a-pomodoro mascot (single 1024² asset,
      `Assets.xcassets/AppIcon.appiconset`)
- [x] True background timing audit — when auto-start is on the engine now
      pre-schedules the **whole chain** of upcoming segment boundaries
      (`TimerEngine.notificationChain` → `NotificationScheduler.schedule([…])`),
      so each transition still notifies while backgrounded
- [x] Haptics on transitions (timer start/pause/resume, segment change, cycle
      complete, tab switch) via SwiftUI `sensoryFeedback`
- [x] Accessibility pass on the Focus screen — the timer dial is a single
      VoiceOver element with a spoken countdown (`a11y.*` strings), the cycle
      dots announce progress, and all icon buttons carry labels. _Remaining:_ a
      sweep of the Tasks / Habits / Settings forms.
- [ ] iPad / macOS layout polish (targets are enabled but UI is phone-first)

### Engineering
- [x] Unit tests for pure logic (cycle build, settings clamp, dots, formatting,
      day-part, Journey streaks/aggregation, record codable) plus the new
      parity features (interrupted focus, task category/due-date + legacy decode,
      habit scheduling/streak, localization table) via Swift Testing, all passing
- [x] Engine transition tests (start / pause / resume / skip / stop / cycle
      completion) on a `@MainActor` harness, plus a pure test of the
      auto-start notification chain — `EngineTests.swift`
- [ ] Snapshot/UI tests
- [x] Move persistence behind a protocol (`KeyValueStore` in `Persistence.swift`);
      every store now injects it, defaulting to `UserDefaults.standard`

---

## Notes / decisions
- Kept the engine **foreground-driven** (no background service like Android's
  `TimerService`); notifications cover the backgrounded case. A Live Activity
  would close the remaining gap.
- Persistence is intentionally simple (`UserDefaults`); revisit if history
  records or sync are added.
- `import Combine` is required in any `ObservableObject` file for `@Published`
  synthesis under this project's Swift settings.
- Ambient sounds are **synthesised at runtime** (`AVAudioSourceNode` + coloured
  noise) rather than shipped as audio files — keeps the app asset-free and gives
  gapless looping. `UIBackgroundModes = audio` is set via `INFOPLIST_KEY_*` in
  the build settings (the project uses a generated Info.plist).
- The **Live Activity** is the recommended way to close the background-timing
  gap noted above: it uses `Text(timerInterval:)` so the Lock Screen / Dynamic
  Island countdown stays correct without push updates, and the engine pushes a
  fresh `ContentState` on every transition (start / pause / resume / skip /
  segment change), ending it on stop / cycle complete. Requires
  `NSSupportsLiveActivities = YES` (set via `INFOPLIST_KEY_*`).
- **Chained notifications:** with auto-start on, the app may be backgrounded
  while segments auto-advance, so `syncNotification` now pre-schedules a
  notification for *every* remaining boundary (pure builder
  `TimerEngine.notificationChain`); with auto-start off only the current
  boundary fires, since the next segment waits for a manual start.
- Widget ↔ app data sharing uses an **App Group** (`group.com.endiar.Purrodoro`,
  declared in `Purrodoro.entitlements` + `PurrodoroWidgets.entitlements`). The
  engine writes a `TodaySnapshot` (count / minutes / streak) after every focus
  credit and calls `WidgetCenter.reloadAllTimelines()`.
- Localization is an **in-app table** (`Localization.swift`), not `.lproj`
  bundles, so the language can switch live like the Android app. New user-facing
  strings should be added there with both `.en` and `.id` values.
