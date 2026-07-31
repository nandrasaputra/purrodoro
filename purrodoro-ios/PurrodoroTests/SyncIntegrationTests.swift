//
//  SyncIntegrationTests.swift
//  PurrodoroTests
//
//  Repo-backed sync tests: natural-key adoption, profile row, FK order.
//  Ports Android `SyncEngineTest.kt` adoption cases + `ProfileSyncTest.kt`.
//

import Testing
import Foundation
import Helpers
@testable import Purrodoro

// MARK: - Fakes

private actor IntegrationFakeSyncApi: SyncApi {
    var remote: [String: [SyncRow]] = [:]
    var upserts: [String: [SyncRow]] = [:]
    var pullArgs: [String: [String?]] = [:]
    var pullOrder: [String] = []
    var conflictKeys: [String: String] = [:]
    var onUpsert: (@Sendable (String) async -> Void)?

    private let uniqueIndexes: [String: [String]] = [
        "session_records": ["user_id", "local_id"],
        "habit_logs": ["user_id", "habit_id", "log_date"],
        "daily_stamps": ["user_id", "stamp_date"],
    ]

    func setRemote(_ rows: [SyncRow], table: String) { remote[table] = rows }
    func setOnUpsert(_ handler: (@Sendable (String) async -> Void)?) { onUpsert = handler }

    func upsert(table: String, rows: [SyncRow], conflictKey: String) async throws {
        conflictKeys[table] = conflictKey
        try enforceUniqueIndex(table: table, rows: rows)
        upserts[table, default: []].append(contentsOf: rows)
        await onUpsert?(table)
    }

    func pullSince(table: String, afterIso: String?) async throws -> [SyncRow] {
        pullOrder.append(table)
        pullArgs[table, default: []].append(afterIso)
        return (remote[table] ?? []).filter { row in
            guard let afterIso, let updated = SyncJSON.stringField(row, "updated_at") else { return true }
            return updated > afterIso
        }
    }

    func pushedRows(table: String) -> [SyncRow] { upserts[table] ?? [] }

    private func enforceUniqueIndex(table: String, rows: [SyncRow]) throws {
        guard let columns = uniqueIndexes[table] else { return }
        func naturalKey(_ row: SyncRow) -> [String] {
            columns.map { col in
                switch row[col] {
                case .string(let s): return s
                case .integer(let i): return String(i)
                case .double(let d): return String(Int(d))
                default: return ""
                }
            }
        }
        func rowId(_ row: SyncRow) -> String { SyncJSON.stringField(row, "id") ?? "" }
        for row in rows {
            let clash = (remote[table] ?? []).contains { existing in
                rowId(existing) != rowId(row) && naturalKey(existing) == naturalKey(row)
            }
            if clash { throw URLError(.cannotCreateFile) }
        }
    }
}

// MARK: - Row builders

private let testUserId = "user-1"
private let testHabitId = "habit-uuid"

private func habitLogRow(
    id: String,
    updatedAt: String,
    habitId: String = testHabitId,
    logDate: String = "2026-06-12",
    state: String = "C",
    deleted: Bool = false
) -> SyncRow {
    [
        "id": .string(id),
        "user_id": .string(testUserId),
        "habit_id": .string(habitId),
        "log_date": .string(logDate),
        "state": .string(state),
        "progress": .integer(0),
        "updated_at": .string(updatedAt),
        "deleted": .bool(deleted),
    ]
}

private func localHabitLog(
    id: String,
    dirty: Bool,
    state: String,
    modifiedAt: Int64 = 1
) -> HabitLogRecord {
    HabitLogRecord(
        id: id,
        habitId: testHabitId,
        logDateIso: "2026-06-12",
        stateTag: state,
        progress: 0,
        dirty: dirty,
        locallyModifiedAt: modifiedAt,
        deleted: false
    )
}

private func sessionRecordRow(
    id: String,
    localId: Int64,
    updatedAt: String,
    taskName: String = "Cloud work"
) -> SyncRow {
    [
        "id": .string(id),
        "user_id": .string(testUserId),
        "local_id": .integer(Int(localId)),
        "started_at": .integer(5_000),
        "ended_at": .integer(6_000),
        "focus_minutes": .integer(25),
        "short_break_minutes": .integer(5),
        "long_break_minutes": .integer(15),
        "completed_focus_count": .integer(1),
        "total_focus_count": .integer(1),
        "completed_short_break_count": .integer(0),
        "completed_long_break_count": .integer(0),
        "status": .string("COMPLETED"),
        "task_name": .string(taskName),
        "updated_at": .string(updatedAt),
        "deleted": .bool(false),
    ]
}

private func localSessionRecord(
    id: String,
    legacyId: Int64,
    taskName: String = "Guest work",
    dirty: Bool = true,
    modifiedAt: Int64 = 1
) -> SessionRecordRow {
    SessionRecordRow(
        id: id,
        legacyId: legacyId,
        startedAt: 1_000,
        endedAt: 2_000,
        focusMinutes: 25,
        shortBreakMinutes: 5,
        longBreakMinutes: 15,
        completedFocusCount: 1,
        totalFocusCount: 1,
        completedShortBreakCount: 0,
        completedLongBreakCount: 0,
        statusName: "COMPLETED",
        taskName: taskName,
        focusTypeName: nil,
        dirty: dirty,
        locallyModifiedAt: modifiedAt,
        deleted: false
    )
}

private func dailyStampRow(
    id: String,
    date: String,
    count: Int,
    updatedAt: String
) -> SyncRow {
    [
        "id": .string(id),
        "user_id": .string(testUserId),
        "stamp_date": .string(date),
        "count": .integer(count),
        "updated_at": .string(updatedAt),
        "deleted": .bool(false),
    ]
}

private func localDailyStamp(
    id: String,
    date: String,
    count: Int,
    dirty: Bool = true,
    modifiedAt: Int64 = 1
) -> DailyStampRecord {
    DailyStampRecord(
        id: id,
        stampDateIso: date,
        count: count,
        dirty: dirty,
        locallyModifiedAt: modifiedAt,
        deleted: false
    )
}

private func profileRow(
    displayName: String,
    focusMinutes: Int,
    premiumOverrideEnabled: Bool = false,
    premiumOverrideActive: Bool = false,
    updatedAt: String
) -> SyncRow {
    [
        "user_id": .string(testUserId),
        "display_name": .string(displayName),
        "timer_settings": .object([
            "focus_minutes": .integer(focusMinutes),
            "short_break_minutes": .integer(5),
            "long_break_minutes": .integer(15),
            "auto_start_next_session": .bool(false),
            "focus_sessions_per_cycle": .integer(4),
        ]),
        "premium_override_enabled": .bool(premiumOverrideEnabled),
        "premium_override_active": .bool(premiumOverrideActive),
        "updated_at": .string(updatedAt),
        "deleted": .bool(false),
    ]
}

// MARK: - Harness

@MainActor
private func makeIntegrationHarness(
    repo: SyncRepository,
    prefs: PreferencesStore,
    engine: TimerEngine
) -> (IntegrationFakeSyncApi, SyncEngine) {
    let api = IntegrationFakeSyncApi()
    let userId: @Sendable () async -> String = { testUserId }
    let tables = TableSyncs.makeAll(
        repo: repo,
        userId: userId,
        applySyncedPrefs: { displayName, settingsJson, overrideEnabled, overrideActive in
            prefs.applySyncedProfile(displayName: displayName)
            prefs.applyPremiumOverride(enabled: overrideEnabled, active: overrideActive)
            engine.applySyncedSettings(fromJson: settingsJson)
        }
    )
    let syncEngine = SyncEngine(api: api, tables: tables, checkpoints: repo)
    return (api, syncEngine)
}

@MainActor
private func makeTestStack() -> (
    SyncRepository, PreferencesStore, TimerEngine, IntegrationFakeSyncApi, SyncEngine
) {
    let store = InMemoryKeyValueStore()
    store.set(true, forKey: SyncEntityKeys.migrated)
    let repo = SyncRepository(store: store)
    let prefs = PreferencesStore(syncRepo: repo, store: store)
    let engine = TimerEngine(syncRepo: repo, store: store)
    let (api, syncEngine) = makeIntegrationHarness(repo: repo, prefs: prefs, engine: engine)
    return (repo, prefs, engine, api, syncEngine)
}

// MARK: - Startup ownership

@Suite(.serialized)
struct StartupOwnershipTests {
    @Test func signedOutDoesNothing() {
        #expect(resolveStartupOwnership(userId: nil, ownerId: nil, hasLocalData: false) == .none)
        #expect(resolveStartupOwnership(userId: nil, ownerId: "user-A", hasLocalData: true) == .none)
    }

    @Test func ownerMatchesUser() {
        #expect(resolveStartupOwnership(userId: "user-A", ownerId: "user-A", hasLocalData: true) == .none)
    }

    @Test func unclaimedDataClaims() {
        #expect(resolveStartupOwnership(userId: "user-A", ownerId: nil, hasLocalData: true) == .claim)
        #expect(resolveStartupOwnership(userId: "user-A", ownerId: nil, hasLocalData: false) == .claim)
    }

    @Test func foreignOwnerEmptyDataCompletesDrop() {
        #expect(resolveStartupOwnership(userId: "user-B", ownerId: "user-A", hasLocalData: false) == .completeDrop)
    }

    @Test func foreignOwnerWithDataSignsOut() {
        #expect(resolveStartupOwnership(userId: "user-B", ownerId: "user-A", hasLocalData: true) == .signOut)
    }

    @Test func claimRemnantOrchestration() async {
        var calls: [String] = []
        var taggedOwner: String?
        await reconcileStartupOwnership(
            userId: "user-A",
            ownerId: nil,
            hasLocalData: true,
            setOwnerId: { taggedOwner = $0; calls.append("setOwnerId") },
            claimMerge: { calls.append("claimMerge") },
            restoreFromCloud: { calls.append("restoreFromCloud") },
            signOut: { calls.append("signOut") }
        )
        #expect(calls == ["setOwnerId", "claimMerge"])
        #expect(taggedOwner == "user-A")
    }

    @Test func interruptedDropOrchestration() async {
        var calls: [String] = []
        var taggedOwner: String?
        await reconcileStartupOwnership(
            userId: "user-B",
            ownerId: "user-A",
            hasLocalData: false,
            setOwnerId: { taggedOwner = $0; calls.append("setOwnerId") },
            claimMerge: { calls.append("claimMerge") },
            restoreFromCloud: { calls.append("restoreFromCloud") },
            signOut: { calls.append("signOut") }
        )
        #expect(calls == ["setOwnerId", "restoreFromCloud"])
        #expect(taggedOwner == "user-B")
    }
}

// MARK: - Natural-key adoption

@Suite(.serialized)
struct NaturalKeyAdoptionTests {

    @Test func cleanHabitLogAdoptsServerUUID() async throws {
        let (repo, _, _, api, syncEngine) = await MainActor.run { makeTestStack() }
        await MainActor.run {
            repo.syncUpsertHabitLogs([localHabitLog(id: "log-local", dirty: false, state: "S")])
        }
        await api.setRemote(
            [habitLogRow(id: "log-server", updatedAt: "2026-06-12T10:00:00+00:00", state: "C")],
            table: "habit_logs"
        )

        let result = try await syncEngine.syncOnce()
        #expect(result == .success)
        await MainActor.run {
            #expect(repo.syncHabitLogById("log-local") == nil)
            let adopted = repo.syncHabitLogById("log-server")
            #expect(adopted?.stateTag == "C")
            #expect(adopted?.dirty == false)
        }
    }

    @Test func dirtyHabitLogRekeysAndPushesOnSecondPass() async throws {
        let (repo, _, _, api, syncEngine) = await MainActor.run { makeTestStack() }
        await MainActor.run {
            repo.syncUpsertHabitLogs([localHabitLog(id: "log-local", dirty: true, state: "S", modifiedAt: 1)])
        }
        await api.setOnUpsert { table in
            guard table == "habit_logs" else { return }
            await MainActor.run {
                guard var current = repo.syncHabitLogById("log-server") else { return }
                current.locallyModifiedAt = 2
                current.dirty = true
                repo.syncUpsertHabitLogs([current])
            }
        }
        await api.setRemote(
            [habitLogRow(id: "log-server", updatedAt: "2026-06-12T10:00:00+00:00", state: "C")],
            table: "habit_logs"
        )

        _ = try await syncEngine.syncOnce()
        await MainActor.run {
            let rekeyed = repo.syncHabitLogById("log-server")
            #expect(rekeyed?.stateTag == "S")
            #expect(rekeyed?.dirty == true)
        }

        await api.setOnUpsert(nil)
        _ = try await syncEngine.syncOnce()
        let pushed = await api.pushedRows(table: "habit_logs")
        #expect(SyncJSON.stringField(pushed.last!, "id") == "log-server")
        #expect(SyncJSON.stringField(pushed.last!, "state") == "S")
        await MainActor.run {
            #expect(repo.syncHabitLogById("log-server")?.dirty == false)
        }
    }

    @Test func claimMergeCollidingLocalIdAdoptsServerSession() async throws {
        let (repo, _, _, api, syncEngine) = await MainActor.run { makeTestStack() }
        await MainActor.run {
            repo.syncUpsertSessionRecords([localSessionRecord(id: "rec-local", legacyId: 1)])
        }
        await api.setRemote(
            [sessionRecordRow(id: "rec-server", localId: 1, updatedAt: "2026-06-12T10:00:00+00:00")],
            table: "session_records"
        )

        let result = try await syncEngine.syncOnce()
        #expect(result == .success)
        await MainActor.run {
            #expect(repo.syncSessionById("rec-local") == nil)
            let adopted = repo.syncSessionById("rec-server")
            #expect(adopted?.taskName == "Guest work")
            #expect(adopted?.dirty == false)
        }
        let pushed = await api.pushedRows(table: "session_records")
        #expect(SyncJSON.stringField(pushed[0], "id") == "rec-server")
        #expect(SyncJSON.stringField(pushed[0], "task_name") == "Guest work")
    }

    @Test func sameDayStampConvergesOnDeltaPass() async throws {
        let (repo, _, _, api, syncEngine) = await MainActor.run { makeTestStack() }
        await MainActor.run {
            repo.syncUpsertDailyStamps([localDailyStamp(id: "stamp-local", date: "2026-06-12", count: 3)])
        }
        await api.setRemote(
            [dailyStampRow(id: "stamp-server", date: "2026-06-12", count: 1, updatedAt: "2026-06-12T10:00:00+00:00")],
            table: "daily_stamps"
        )

        let result = try await syncEngine.syncOnce()
        #expect(result == .success)
        await MainActor.run {
            #expect(repo.syncDailyStampById("stamp-local") == nil)
            let adopted = repo.syncDailyStampById("stamp-server")
            #expect(adopted?.count == 3)
            #expect(adopted?.dirty == false)
        }
    }

    @Test func remoteTombstoneSoftDeletesHabitLog() async throws {
        let (repo, _, _, api, syncEngine) = await MainActor.run { makeTestStack() }
        await MainActor.run {
            repo.syncUpsertHabitLogs([localHabitLog(id: "log-1", dirty: false, state: "C")])
        }
        await api.setRemote(
            [habitLogRow(id: "log-1", updatedAt: "2026-06-12T10:00:00+00:00", deleted: true)],
            table: "habit_logs"
        )

        let result = try await syncEngine.syncOnce()
        #expect(result == .success)
        await MainActor.run {
            let row = repo.syncHabitLogById("log-1")
            #expect(row?.deleted == true)
            #expect(row?.dirty == false)
        }
    }

    @Test func tablesProcessedInFKOrder() async throws {
        let (_, _, _, api, syncEngine) = await MainActor.run { makeTestStack() }

        _ = try await syncEngine.syncOnce()
        let order = await api.pullOrder
        #expect(order == [
            "categories", "tasks", "habits", "habit_logs",
            "session_records", "daily_stamps", "profiles",
        ])
    }
}

// MARK: - Profile

@Suite(.serialized)
struct ProfileSyncIntegrationTests {

    @Test func dirtyProfilePushesOnUserIdConflictKey() async throws {
        let (repo, prefs, engine, api, syncEngine) = await MainActor.run { makeTestStack() }
        await MainActor.run {
            prefs.userName = "Nandra"
            engine.settings = TimerSettings(focusMinutes: 50)
            repo.updateProfile(displayName: "Nandra", timerSettings: engine.settings)
        }

        let result = try await syncEngine.syncOnce()
        #expect(result == .success)
        let conflictKey = await api.conflictKeys["profiles"]
        #expect(conflictKey == "user_id")
        let pushed = await api.pushedRows(table: "profiles")
        #expect(pushed.count == 1)
        #expect(pushed[0]["updated_at"] == nil)
        await MainActor.run {
            #expect(repo.syncProfileRecord()?.dirty == false)
        }
    }

    @Test func pulledProfileLandsInPrefsAndRow() async throws {
        let (repo, prefs, engine, api, syncEngine) = await MainActor.run { makeTestStack() }
        await api.setRemote(
            [profileRow(
                displayName: "Cloud Name",
                focusMinutes: 30,
                premiumOverrideEnabled: true,
                premiumOverrideActive: true,
                updatedAt: "2026-06-12T10:00:00+00:00"
            )],
            table: "profiles"
        )

        let result = try await syncEngine.syncOnce()
        #expect(result == .success)
        await MainActor.run {
            #expect(prefs.trimmedName == "Cloud Name")
            #expect(engine.settings.focusMinutes == 30)
            #expect(prefs.premiumOverrideEnabled)
            #expect(prefs.premiumOverrideActive)
            #expect(prefs.isPremium)
            #expect(repo.syncProfileRecord()?.displayName == "Cloud Name")
            #expect(repo.syncProfileRecord()?.dirty == false)
        }
    }
}
