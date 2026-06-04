# Purrodoro iOS — Status

A native SwiftUI port of the Purrodoro (Android) focus app. This doc tracks
what's implemented and what's still open.

_Last updated: 2026-06-04_

---

## TL;DR

The core focus loop and all four primary tabs are working: **Focus, Tasks,
Journey, Habits**, plus segment-end local notifications. The app builds and runs
on the iOS 26 simulator. The remaining work is mostly **parity polish** (sounds,
onboarding, settings/appearance, localization) and **platform extras** (widgets,
Live Activities, app icon).

---

## Architecture

- **UI:** SwiftUI, file-system-synchronized Xcode project (drop a `.swift` file in
  `Purrodoro/` and it compiles — no `.pbxproj` editing).
- **State:** plain `ObservableObject` stores injected via `@EnvironmentObject`
  (`TimerEngine`, `TaskStore`, `HabitStore`).
- **Persistence:** `UserDefaults` (JSON for tasks/habits/settings; per-day
  dictionaries for focus stamps/minutes). No Core Data (the template was removed).
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
| `SettingsSheet.swift` | Durations / sessions-per-cycle / auto-start |
| `TaskStore.swift` / `TasksView.swift` | Tasks feature |
| `HabitStore.swift` / `HabitsView.swift` | Habits feature |
| `JourneyModels.swift` / `JourneyView.swift` | Streaks, stats, calendar heatmap |
| `NotificationScheduler.swift` | Local notifications at segment end |
| `PurrodoroTheme.swift` | Brand palette + per-mode accents |

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

### Habits
- [x] CRUD, emoji + color, daily target, per-day log
- [x] 7-day completion strip, per-habit streak, tap-to-log
- [x] "Focus on this" → attaches habit; sessions credit the daily target

### Notifications
- [x] Local notification at segment end (focus done / break over / cycle complete)
- [x] Foreground banner + chime; cancels on pause/stop

### Misc
- [x] Brand palette ported from Android `Color.kt` (light + dark tokens)
- [x] Reused Android cat mascot art as image assets
- [x] Consolidated `.gitignore` at repo root (Android + iOS)

---

## 🚧 Left to do

### Parity with Android (feature gaps)
- [ ] **Ambient focus sounds** (rain, cafe, forest, fire, keys) with looping audio
      + background playback
- [ ] **Onboarding** flow (first-run intro + permission priming)
- [ ] **Profile / full Settings screen**: appearance (Light/Dark/Auto), language
- [ ] **Localization** (Indonesian `id`, currently English-only)
- [ ] **Session history records** (per-session detail; today we only keep daily
      stamp/minute counts — no `SessionRecord` log)
- [ ] **Start Focus sheet** (confirm minutes / pick intent before starting;
      adjust leftover time when resuming an interrupted focus)
- [ ] **Interrupted-focus resume** ("continue where you left off")
- [ ] Task **categories** (with colors) and **due dates**
- [ ] Habit **occurrence** (weekdays) and **time blocks**; skip / rest days
- [ ] Cycle-complete **celebration** (richer than the current alert)

### iOS platform extras
- [ ] **Home Screen widgets** (WidgetKit) — Android has a Glance "Today" widget
- [ ] **Live Activity / Dynamic Island** for the running timer
- [ ] **App icon** (still the default placeholder)
- [ ] True background timing audit (chained notifications when auto-start is on;
      currently only the next boundary is scheduled)
- [ ] Haptics on transitions
- [ ] Accessibility pass (VoiceOver labels exist in places; needs a full sweep)
- [ ] iPad / macOS layout polish (targets are enabled but UI is phone-first)

### Engineering
- [ ] Unit tests (cycle math, streak calc, engine transitions) — none yet
- [ ] Snapshot/UI tests
- [ ] Move persistence behind a protocol (easier testing / future iCloud sync)

---

## Notes / decisions
- Kept the engine **foreground-driven** (no background service like Android's
  `TimerService`); notifications cover the backgrounded case. A Live Activity
  would close the remaining gap.
- Persistence is intentionally simple (`UserDefaults`); revisit if history
  records or sync are added.
- `import Combine` is required in any `ObservableObject` file for `@Published`
  synthesis under this project's Swift settings.
