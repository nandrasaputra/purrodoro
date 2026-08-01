//
//  FeatureTests.swift
//  PurrodoroTests
//
//  Unit tests for the features added in the iOS parity pass: interrupted-focus
//  resume, task categories/due dates, habit scheduling (occurrence + rest
//  days), and the runtime localization table.
//

import Testing
import Foundation
@testable import Purrodoro

// MARK: - Interrupted focus

struct InterruptedFocusTests {

    @Test func leftoverMinutesClampToAtLeastOne() {
        let zero = InterruptedFocus(leftoverMinutes: 0, intentName: nil)
        #expect(zero.leftoverMinutes == 1)
        let negative = InterruptedFocus(leftoverMinutes: -5, intentName: "Write")
        #expect(negative.leftoverMinutes == 1)
        #expect(negative.intentName == "Write")
    }

    @Test func codableRoundTrips() throws {
        let value = InterruptedFocus(leftoverMinutes: 12, intentName: "Read", stoppedAt: Date(timeIntervalSince1970: 100))
        let data = try JSONEncoder().encode(value)
        let decoded = try JSONDecoder().decode(InterruptedFocus.self, from: data)
        #expect(decoded == value)
    }
}

// MARK: - Task categories & due dates

struct TaskItemTests {

    private var cal: Calendar { Calendar.current }

    @Test func trimmedCategoryNormalisesBlankToNil() {
        var task = TaskItem(title: "A")
        task.category = "   "
        #expect(task.trimmedCategory == nil)
        task.category = "  Work "
        #expect(task.trimmedCategory == "Work")
    }

    @Test func overdueOnlyWhenPastAndIncomplete() {
        let yesterday = cal.date(byAdding: .day, value: -1, to: Date())!
        var task = TaskItem(title: "A")
        task.dueDate = yesterday
        #expect(task.isOverdue)
        task.completed = true
        #expect(!task.isOverdue)
    }

    @Test func dueTodayDetected() {
        var task = TaskItem(title: "A")
        task.dueDate = Date()
        #expect(task.isDueToday)
        #expect(!task.isOverdue)
    }

    @Test func noDueDateIsNeverOverdueOrDueToday() {
        let task = TaskItem(title: "A")
        #expect(!task.isOverdue)
        #expect(!task.isDueToday)
    }

    @Test func decodesLegacyTaskWithoutNewFields() throws {
        // Older persisted tasks have no category/dueDate keys.
        let legacy = """
        {"id":"\(UUID().uuidString)","title":"Legacy","priority":"normal","plannedSessions":2,"completedSessions":0,"completed":false,"createdAt":0}
        """.data(using: .utf8)!
        let task = try JSONDecoder().decode(TaskItem.self, from: legacy)
        #expect(task.title == "Legacy")
        #expect(task.category == nil)
        #expect(task.dueDate == nil)
    }
}

// MARK: - Habit scheduling

struct HabitSchedulingTests {

    private var cal: Calendar { Calendar.current }

    private func date(weekday: Int, base: Date = Date()) -> Date {
        var d = cal.startOfDay(for: base)
        for _ in 0..<7 {
            if cal.component(.weekday, from: d) == weekday { return d }
            d = cal.date(byAdding: .day, value: 1, to: d)!
        }
        return d
    }

    @Test func emptyWeekdaysMeansEveryDay() {
        let habit = HabitItem(name: "Read")
        #expect(habit.isScheduled(on: date(weekday: 2)))
        #expect(habit.isScheduled(on: date(weekday: 6)))
    }

    @Test func specificWeekdaysGateScheduling() {
        var habit = HabitItem(name: "Gym")
        habit.scheduledWeekdays = [2, 4] // Mon, Wed (1=Sun)
        #expect(habit.isScheduled(on: date(weekday: 2)))
        #expect(!habit.isScheduled(on: date(weekday: 3)))
        #expect(habit.isScheduled(on: date(weekday: 4)))
    }

    @Test func restDayOverridesSchedule() {
        var habit = HabitItem(name: "Walk")
        let today = Date()
        habit.restDays = [Journey.dayKey(today)]
        #expect(!habit.isScheduled(on: today))
        #expect(habit.isRestDay(today))
    }

    @Test func streakSkipsUnscheduledDaysWithoutBreaking() {
        // Schedule only on the weekday of "today" and the same weekday last week;
        // both met. The 6 unscheduled days in between must not break the streak.
        var habit = HabitItem(name: "Weekly")
        let today = cal.startOfDay(for: Date())
        let weekday = cal.component(.weekday, from: today)
        habit.scheduledWeekdays = [weekday]
        habit.targetPerDay = 1
        let lastWeek = cal.date(byAdding: .day, value: -7, to: today)!
        habit.log[Journey.dayKey(today)] = 1
        habit.log[Journey.dayKey(lastWeek)] = 1
        #expect(habit.streak == 2)
    }
}

// MARK: - Localization

@MainActor
struct LocalizationTests {

    private func manager() -> Localization {
        let suite = UserDefaults(suiteName: "loc.test.\(UUID().uuidString)")!
        return Localization(store: suite)
    }

    @Test func returnsEnglishAndIndonesian() {
        let loc = manager()
        loc.language = .en
        #expect(loc.t("home.startFocus") == "Start Focus")
        loc.language = .id
        #expect(loc.t("home.startFocus") == "Mulai Fokus")
    }

    @Test func unknownKeyFallsBackToKey() {
        let loc = manager()
        #expect(loc.t("nonexistent.key") == "nonexistent.key")
    }

    @Test func formatsIntegerArguments() {
        let loc = manager()
        loc.language = .en
        #expect(loc.t("resume.subtitle", 7) == "7 min of focus left")
    }

    @Test func everyKeyHasBothLanguages() {
        // Guard against a half-translated key slipping in.
        let loc = manager()
        for key in [
            "tab.focus", "home.startFocus", "settings.language", "sounds.title",
            "focusBusy.title", "settings.notifSound", "settings.languageSystem",
            "journey.title", "journey.recentSessions", "journey.currentStreak",
            "journey.openHistory", "history.title", "history.emptyDay",
            "journey.deleteRecord", "journey.statFocused",
            "tutorial.startFocus.title", "tutorial.done", "start.note",
            "onboard.welcome.title", "onboard.enableNotif", "onboard.signIn",
            "onboard.continueGuest", "tasks.nowFocusing", "date.today",
            "tasks.markDoneA11y", "habits.focusA11y", "habits.logTodayA11y",
            "journey.streakA11y", "appearance.light",
        ] {
            loc.language = .en
            #expect(loc.t(key) != key)
            loc.language = .id
            #expect(loc.t(key) != key)
        }
    }
}

@Suite struct BackupCodecTests {
    @Test func decodeEmptyIsMalformed() {
        if case .malformed = BackupCodec.decode("") { } else {
            Issue.record("expected malformed")
        }
    }

    @Test func decodeUnsupportedVersion() {
        let raw = #"{"schema_version":9999,"data":{}}"#
        if case .unsupportedVersion(9999) = BackupCodec.decode(raw) { } else {
            Issue.record("expected unsupported version")
        }
    }

    @Test func roundTripPreservesCoreFields() throws {
        let task = TaskItem(
            title: "Write spec",
            priority: .high,
            plannedSessions: 2,
            completedSessions: 1,
            category: "Work"
        )
        let habit = HabitItem(
            name: "Read",
            emoji: "📖",
            colorHex: 0xAABBCC,
            targetPerDay: 1,
            scheduledWeekdays: [2, 6],
            timeBlock: .morning
        )
        let json = try BackupCodec.encode(
            displayName: "Nandra",
            timerSettings: TimerSettings(focusMinutes: 30, shortBreakMinutes: 6, longBreakMinutes: 20, autoStartNextSession: true, focusSessionsPerCycle: 3),
            appearance: .dark,
            language: .id,
            selectedSoundId: 2,
            homeScene: .cafe,
            tasks: [task],
            habits: [habit],
            sessionRecords: [FocusSessionRecord(minutes: 25, intentName: "Write spec")],
            stamps: ["2026-06-18": 4],
            appVersionName: "1.0",
            exportedAt: 123
        )
        guard case .success(let bundle) = BackupCodec.decode(json) else {
            Issue.record("decode failed")
            return
        }
        #expect(bundle.displayName == "Nandra")
        #expect(bundle.timerSettings.focusMinutes == 30)
        #expect(bundle.prefs.homeScene == .cafe)
        #expect(bundle.tasks.count == 1)
        #expect(bundle.tasks[0].title == "Write spec")
        #expect(bundle.tasks[0].trimmedCategory == "Work")
        #expect(bundle.habits.count == 1)
        #expect(bundle.stamps["2026-06-18"] == 4)
    }

    @Test func systemLanguageAndNotificationSoundRoundTrip() throws {
        let json = try BackupCodec.encode(
            displayName: "",
            timerSettings: TimerSettings(),
            appearance: .auto,
            language: .system,
            selectedSoundId: 1,
            notificationSound: false,
            tasks: [],
            habits: [],
            sessionRecords: [],
            stamps: [:],
            appVersionName: "1.0",
            exportedAt: 1
        )
        guard case .success(let bundle) = BackupCodec.decode(json) else {
            Issue.record("decode failed")
            return
        }
        #expect(bundle.prefs.language == .system)
        #expect(bundle.prefs.notificationSound == false)
    }
}

// MARK: - Auth ownership

struct ResolveLoginActionTests {

    @Test func nullOwnerClaims() {
        #expect(resolveLoginAction(ownerId: nil, incomingUserId: "user-A") == .claim)
        #expect(resolveLoginAction(ownerId: nil, incomingUserId: "") == .claim)
    }

    @Test func sameOwnerKeeps() {
        #expect(resolveLoginAction(ownerId: "user-A", incomingUserId: "user-A") == .keep)
    }

    @Test func differentOwnerDrops() {
        #expect(resolveLoginAction(ownerId: "user-A", incomingUserId: "user-B") == .drop)
    }
}

struct AuthProviderTests {

    @Test func storageTagRoundTrip() {
        #expect(AuthProvider.fromStorageTag("google") == .google)
        #expect(AuthProvider.fromStorageTag("apple") == .apple)
    }

    @Test func unknownProviderFallback() {
        #expect(AuthProvider.fromStorageTag("email") == .unknown)
        #expect(AuthProvider.fromStorageTag(nil) == .unknown)
        #expect(AuthProvider.fromStorageTag("") == .unknown)
    }
}

@MainActor
struct AccountManagerTests {

    private func makeStores(store: InMemoryKeyValueStore) -> (
        SyncRepository, TimerEngine, TaskStore, HabitStore, SessionHistoryStore, PreferencesStore
    ) {
        let sync = SyncRepository(store: store)
        let engine = TimerEngine(syncRepo: sync, store: store)
        let tasks = TaskStore(syncRepo: sync, store: store)
        let habits = HabitStore(syncRepo: sync, store: store)
        let history = SessionHistoryStore(syncRepo: sync)
        let prefs = PreferencesStore(syncRepo: sync, store: store)
        tasks.focusMinutesProvider = { engine.settings.focusMinutes }
        history.timerSettingsProvider = { engine.settings }
        prefs.timerSettingsProvider = { engine.settings }
        return (sync, engine, tasks, habits, history, prefs)
    }

    @Test func claimTagsOwnerKeepsData() async {
        let store = InMemoryKeyValueStore()
        let ownerStore = AccountOwnerStore(store: store)
        let (sync, engine, tasks, habits, history, prefs) = makeStores(store: store)
        tasks.add(title: "Keep me", priority: .normal, plannedSessions: 1)
        let manager = AccountManager(
            ownerStore: ownerStore,
            clearer: AccountDataClearer(
                syncRepo: sync,
                engine: engine,
                tasks: tasks,
                habits: habits,
                history: history,
                prefs: prefs
            )
        )

        let result = await manager.onSignedIn(incomingUserId: "user-A", confirmDrop: { true })
        #expect(result == .claimed)
        #expect(ownerStore.ownerId == "user-A")
        #expect(!tasks.tasks.isEmpty)
    }

    @Test func dropClearsThenRetags() async {
        let store = InMemoryKeyValueStore()
        let ownerStore = AccountOwnerStore(store: store)
        ownerStore.setOwnerId("user-A")
        let (sync, engine, tasks, habits, history, prefs) = makeStores(store: store)
        tasks.add(title: "Old data", priority: .normal, plannedSessions: 1)
        prefs.userName = "Alice"
        let manager = AccountManager(
            ownerStore: ownerStore,
            clearer: AccountDataClearer(
                syncRepo: sync,
                engine: engine,
                tasks: tasks,
                habits: habits,
                history: history,
                prefs: prefs
            )
        )

        let result = await manager.onSignedIn(incomingUserId: "user-B", confirmDrop: { true })
        #expect(result == .dropped)
        #expect(ownerStore.ownerId == "user-B")
        #expect(tasks.tasks.isEmpty)
        #expect(prefs.userName.isEmpty)
    }

    @Test func dropCancelledLeavesData() async {
        let store = InMemoryKeyValueStore()
        let ownerStore = AccountOwnerStore(store: store)
        ownerStore.setOwnerId("user-A")
        let (sync, engine, tasks, habits, history, prefs) = makeStores(store: store)
        tasks.add(title: "Stay", priority: .normal, plannedSessions: 1)
        let manager = AccountManager(
            ownerStore: ownerStore,
            clearer: AccountDataClearer(
                syncRepo: sync,
                engine: engine,
                tasks: tasks,
                habits: habits,
                history: history,
                prefs: prefs
            )
        )

        let result = await manager.onSignedIn(incomingUserId: "user-B", confirmDrop: { false })
        #expect(result == .dropCancelled)
        #expect(ownerStore.ownerId == "user-A")
        #expect(!tasks.tasks.isEmpty)
    }

    @Test func claimTriggersClaimMergeSync() async {
        let store = InMemoryKeyValueStore()
        let ownerStore = AccountOwnerStore(store: store)
        let (sync, engine, tasks, habits, history, prefs) = makeStores(store: store)
        var hookCalls: [String] = []
        let hooks = RecordingSyncHooks(
            onClaimMerge: { hookCalls.append("claimMerge") },
            onDelta: { hookCalls.append("deltaSync") },
            onReset: { hookCalls.append("resetCheckpointsAndPull") }
        )
        let manager = AccountManager(
            ownerStore: ownerStore,
            clearer: AccountDataClearer(
                syncRepo: sync, engine: engine, tasks: tasks, habits: habits, history: history, prefs: prefs
            ),
            syncHooks: hooks
        )

        _ = await manager.onSignedIn(incomingUserId: "user-A", confirmDrop: { true })
        #expect(hookCalls == ["claimMerge"])
    }

    @Test func keepTriggersDeltaSync() async {
        let store = InMemoryKeyValueStore()
        let ownerStore = AccountOwnerStore(store: store)
        ownerStore.setOwnerId("user-A")
        let (sync, engine, tasks, habits, history, prefs) = makeStores(store: store)
        var hookCalls: [String] = []
        let hooks = RecordingSyncHooks(
            onClaimMerge: { hookCalls.append("claimMerge") },
            onDelta: { hookCalls.append("deltaSync") },
            onReset: { hookCalls.append("resetCheckpointsAndPull") }
        )
        let manager = AccountManager(
            ownerStore: ownerStore,
            clearer: AccountDataClearer(
                syncRepo: sync, engine: engine, tasks: tasks, habits: habits, history: history, prefs: prefs
            ),
            syncHooks: hooks
        )

        _ = await manager.onSignedIn(incomingUserId: "user-A", confirmDrop: { true })
        #expect(hookCalls == ["deltaSync"])
    }
}

struct FocusBusyGateTests {

    @Test func idleProceeds() {
        #expect(FocusBusyGate.evaluate(isStarted: false, liveMatchesSelection: false) == .proceed)
        #expect(FocusBusyGate.evaluate(isStarted: false, liveMatchesSelection: true) == .proceed)
    }

    @Test func sameIntentOpensFocus() {
        #expect(FocusBusyGate.evaluate(isStarted: true, liveMatchesSelection: true) == .openFocus)
    }

    @Test func otherIntentIsBusy() {
        #expect(FocusBusyGate.evaluate(isStarted: true, liveMatchesSelection: false) == .busy)
    }
}

struct AppLanguageSystemTests {

    @Test func systemResolvesToDeviceLanguage() {
        let resolved = AppLanguage.system.resolved
        #expect(resolved == .en || resolved == .id)
        #expect(resolved != .system)
    }

    @Test func concreteLanguagesResolveToThemselves() {
        #expect(AppLanguage.en.resolved == .en)
        #expect(AppLanguage.id.resolved == .id)
    }

    @Test func localizationDefaultsToSystem() {
        let loc = Localization(store: InMemoryKeyValueStore())
        #expect(loc.language == .system)
    }
}

@MainActor
struct TutorialControllerTests {

    @Test func advancesAndFinishes() {
        let controller = TutorialController()
        var finished = false
        controller.start()
        #expect(controller.visible)
        #expect(controller.stepIndex == 0)
        #expect(controller.steps.count == 5)

        for _ in 0..<(controller.steps.count - 1) {
            controller.next { finished = true }
            #expect(!finished)
        }
        controller.next { finished = true }
        #expect(finished)
        #expect(!controller.visible)
    }

    @Test func skipFinishesEarly() {
        let controller = TutorialController()
        var finished = false
        controller.start()
        controller.skip { finished = true }
        #expect(finished)
        #expect(!controller.visible)
    }
}

struct DeepLinkTests {

    @Test func parsesStartFocusAndTabs() {
        #expect(DeepLink.parse(URL(string: "purrodoro://start-focus")!) == .startFocus)
        #expect(DeepLink.parse(URL(string: "purrodoro://tab/tasks")!) == .openTab(.tasks))
        #expect(DeepLink.parse(URL(string: "purrodoro://tab/habits")!) == .openTab(.habits))
        #expect(DeepLink.parse(URL(string: "purrodoro://tab/journey")!) == .openTab(.journey))
        #expect(DeepLink.parse(URL(string: "purrodoro://tab/focus")!) == .openTab(.focus))
        #expect(DeepLink.parse(URL(string: "purrodoro://")!) == .openApp)
        #expect(DeepLink.parse(URL(string: "https://example.com")!) == nil)
    }

    @Test func urlBuildersRoundTrip() {
        #expect(DeepLink.parse(DeepLink.url(for: .startFocus)) == .startFocus)
        #expect(DeepLink.parse(DeepLink.url(for: .openTab(.habits))) == .openTab(.habits))
    }
}

@MainActor
struct NotificationSoundPreferenceTests {

    @Test func defaultsEnabledAndPersistsOff() {
        let store = InMemoryKeyValueStore()
        let prefs = PreferencesStore(store: store)
        #expect(prefs.notificationSoundEnabled == true)
        prefs.notificationSoundEnabled = false
        #expect(store.bool(forKey: "purrodoro.notificationSoundEnabled") == false)
        let reloaded = PreferencesStore(store: store)
        #expect(reloaded.notificationSoundEnabled == false)
    }
}

@MainActor
struct SessionHistoryDeleteTests {

    @Test func deleteSoftDeletesAndHidesRecord() {
        let store = InMemoryKeyValueStore()
        let sync = SyncRepository(store: store)
        let history = SessionHistoryStore(syncRepo: sync)
        history.timerSettingsProvider = { TimerSettings() }

        history.record(minutes: 25, intentName: "Write")
        history.record(minutes: 30, intentName: "Read")
        #expect(history.records.count == 2)

        let doomed = history.records[0]
        history.delete(doomed.id)

        #expect(history.records.count == 1)
        #expect(history.records.contains { $0.id == doomed.id } == false)
        #expect(sync.syncDirtySessionRecords().contains { $0.id == doomed.id.uuidString.lowercased() && $0.deleted })
    }
}

@MainActor
struct SimpleNotePreferenceTests {

    @Test func setAndClearSimpleNote() {
        let store = InMemoryKeyValueStore()
        let prefs = PreferencesStore(store: store)
        prefs.setSimpleNote("  Draft blog  ")
        #expect(prefs.activeSimpleNote == "Draft blog")
        #expect(store.string(forKey: "purrodoro.activeSimpleNote") == "Draft blog")
        prefs.clearSimpleNote()
        #expect(prefs.activeSimpleNote == nil)
    }
}

struct WidgetSnapshotBuilderTests {

    @Test func freshFocusWhenIdleEmpty() {
        let snap = WidgetSnapshotBuilder.build(
            stamps: [:],
            minutesToday: 0,
            settings: TimerSettings(focusSessionsPerCycle: 4),
            habits: [],
            tasks: [],
            timerState: .idle,
            pendingCycleAck: false,
            today: date("2026-08-01")
        )
        #expect(snap.nudge.kind == .freshFocus)
        #expect(snap.nudge.target == .startFocus)
        #expect(snap.focusToday == 0)
        #expect(snap.focusTarget == 4)
    }

    @Test func streakAtRiskWhenStreakAliveAndNoFocusToday() {
        let today = date("2026-08-01")
        let stamps = ["2026-07-31": 1, "2026-07-30": 1]
        let snap = WidgetSnapshotBuilder.build(
            stamps: stamps,
            minutesToday: 0,
            settings: TimerSettings(focusSessionsPerCycle: 4),
            habits: [],
            tasks: [],
            timerState: .idle,
            pendingCycleAck: false,
            today: today
        )
        #expect(snap.streak == 2)
        #expect(snap.streakAtRisk)
        #expect(snap.nudge.kind == .streakAtRisk)
        #expect(snap.nudge.target == .startFocus)
    }

    @Test func playingTimerWinsNudgePriority() {
        let snap = WidgetSnapshotBuilder.build(
            stamps: ["2026-08-01": 0],
            minutesToday: 0,
            settings: TimerSettings(),
            habits: [],
            tasks: [],
            timerState: .playing,
            pendingCycleAck: false,
            today: date("2026-08-01")
        )
        #expect(snap.nudge.kind == .focusing)
        #expect(snap.nudge.target == .openTimer)
    }

    @Test func planChipsPreferHabitsThenTasks() {
        let habit = HabitItem(name: "Stretch", emoji: "🧘")
        let task = TaskItem(title: "Draft", dueDate: date("2026-08-01"))
        let snap = WidgetSnapshotBuilder.build(
            stamps: [:],
            minutesToday: 0,
            settings: TimerSettings(),
            habits: [habit],
            tasks: [task],
            timerState: .idle,
            pendingCycleAck: false,
            today: date("2026-08-01")
        )
        let chips = snap.planChips(max: 3)
        #expect(chips.count == 2)
        #expect(chips[0].label == "Stretch")
        #expect(chips[1].label == "Draft")
        #expect(snap.pendingTasksCount == 1)
    }

    @Test func nudgeTargetURLsMatchDeepLinkScheme() {
        #expect(WidgetNudgeTarget.startFocus.url.absoluteString == "purrodoro://start-focus")
        #expect(WidgetNudgeTarget.openHabits.url.absoluteString == "purrodoro://tab/habits")
        #expect(WidgetNudgeTarget.openTasks.url.absoluteString == "purrodoro://tab/tasks")
        #expect(WidgetNudgeTarget.openTimer.url.absoluteString == "purrodoro://tab/focus")
    }

    private func date(_ key: String) -> Date {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.date(from: key)!
    }
}

private struct RecordingSyncHooks: AccountManager.SyncHooks {
    let onClaimMerge: () async -> Void
    let onDelta: () async -> Void
    let onReset: () async -> Void

    func claimMergeSync() async { await onClaimMerge() }
    func deltaSync() async { await onDelta() }
    func resetCheckpointsAndPull() async { await onReset() }
}
