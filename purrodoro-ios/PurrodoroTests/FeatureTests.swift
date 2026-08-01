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
            "journey.title", "journey.recentSessions", "journey.currentStreak",
            "journey.openHistory", "history.title", "history.emptyDay",
            "journey.deleteRecord", "journey.statFocused",
            "onboard.welcome.title", "onboard.enableNotif", "date.today",
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

private struct RecordingSyncHooks: AccountManager.SyncHooks {
    let onClaimMerge: () async -> Void
    let onDelta: () async -> Void
    let onReset: () async -> Void

    func claimMergeSync() async { await onClaimMerge() }
    func deltaSync() async { await onDelta() }
    func resetCheckpointsAndPull() async { await onReset() }
}
