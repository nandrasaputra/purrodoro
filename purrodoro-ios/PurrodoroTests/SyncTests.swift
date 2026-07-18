//
//  SyncTests.swift
//  PurrodoroTests
//
//  Port of Android `SyncEngineTest.kt` core cases.
//

import Testing
import Foundation
import Helpers
@testable import Purrodoro

// MARK: - Fakes

private struct SyncTestTask: SyncEntity, Equatable {
    var syncId: String
    var legacyId: Int
    var title: String
    var dirty: Bool
    var locallyModifiedAt: Int64
    var deleted: Bool

    var isDirty: Bool { dirty }
}

private actor FakeCheckpoints: CheckpointStore {
    var map: [String: String] = [:]

    func get(table: String) async -> String? { map[table] }
    func set(table: String, iso: String) async { map[table] = iso }

    func value(for table: String) async -> String? { map[table] }
}

private actor FakeSyncApi: SyncApi {
    var remote: [String: [SyncRow]] = [:]
    var upserts: [String: [SyncRow]] = [:]
    var pullArgs: [String: [String?]] = [:]
    var pullOrder: [String] = []
    var failPullTables: Set<String> = []
    var onUpsert: (@Sendable (String) async -> Void)?

    func setRemote(_ rows: [SyncRow], table: String) { remote[table] = rows }
    func failPull(for table: String) { failPullTables.insert(table) }
    func setOnUpsert(_ handler: @escaping @Sendable (String) async -> Void) { onUpsert = handler }

    func upsert(table: String, rows: [SyncRow], conflictKey: String) async throws {
        upserts[table, default: []].append(contentsOf: rows)
        await onUpsert?(table)
    }

    func pullSince(table: String, afterIso: String?) async throws -> [SyncRow] {
        pullOrder.append(table)
        pullArgs[table, default: []].append(afterIso)
        if failPullTables.contains(table) {
            throw URLError(.cannotConnectToHost)
        }
        return remote[table] ?? []
    }

    func pushedRows(table: String) -> [SyncRow] { upserts[table] ?? [] }
    func pullArgs(for table: String) -> [String?] { pullArgs[table] ?? [] }
}

private final class SyncTestStore: @unchecked Sendable {
    private let lock = NSLock()
    private var tasks: [String: SyncTestTask] = [:]
    private var nextLegacyId = 100

    func upsert(_ items: [SyncTestTask]) {
        lock.lock(); defer { lock.unlock() }
        for item in items { tasks[item.syncId] = item }
    }

    func get(_ id: String) -> SyncTestTask? {
        lock.lock(); defer { lock.unlock() }
        return tasks[id]
    }

    func dirtyTasks() -> [SyncTestTask] {
        lock.lock(); defer { lock.unlock() }
        return tasks.values.filter(\.dirty).sorted { $0.syncId < $1.syncId }
    }

    func clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Int64) {
        lock.lock(); defer { lock.unlock() }
        guard var task = tasks[id], task.locallyModifiedAt == snapshotModifiedAt else { return }
        task.dirty = false
        tasks[id] = task
    }

    func allocateLegacyId() -> Int {
        lock.lock(); defer { lock.unlock() }
        defer { nextLegacyId += 1 }
        return nextLegacyId
    }
}

private func makeTaskTableSync(store: SyncTestStore) -> BaseTableSync<SyncTestTask> {
    BaseTableSync(
        table: "tasks",
        userId: { "user-1" },
        dirtyEntities: { store.dirtyTasks() },
        entityById: { store.get($0) },
        upsertEntities: { store.upsert($0) },
        clearDirtyIfUnchanged: { store.clearDirtyIfUnchanged(id: $0, snapshotModifiedAt: $1) },
        encodeForPush: { task, _ in
            [
                "id": .string(task.syncId),
                "user_id": .string("user-1"),
                "title": .string(task.title),
                "completed": .bool(false),
                "minutes": .integer(25),
                "planned_sessions": .integer(1),
                "completed_sessions": .integer(0),
                "category_id": .null,
                "priority": .string("N"),
                "due_date": .null,
                "deleted": .bool(task.deleted),
            ]
        },
        decodeRemote: { row, existing in
            let title = SyncJSON.stringField(row, "title") ?? ""
            let legacy = existing?.legacyId ?? store.allocateLegacyId()
            return SyncTestTask(
                syncId: SyncJSON.rowId(row),
                legacyId: legacy,
                title: title,
                dirty: false,
                locallyModifiedAt: existing?.locallyModifiedAt ?? 0,
                deleted: SyncJSON.rowDeleted(row)
            )
        }
    )
}

private func makeCategoryTableSync(store: SyncTestStore) -> BaseTableSync<SyncTestTask> {
    BaseTableSync(
        table: "categories",
        userId: { "user-1" },
        dirtyEntities: { [] },
        entityById: { store.get($0) },
        upsertEntities: { store.upsert($0) },
        clearDirtyIfUnchanged: { _, _ in },
        encodeForPush: { _, _ in [:] },
        decodeRemote: { row, existing in
            SyncTestTask(
                syncId: SyncJSON.rowId(row),
                legacyId: existing?.legacyId ?? 1,
                title: SyncJSON.stringField(row, "name") ?? "",
                dirty: false,
                locallyModifiedAt: 0,
                deleted: false
            )
        }
    )
}

private func taskRow(
    id: String,
    title: String,
    updatedAt: String,
    deleted: Bool = false
) -> SyncRow {
    [
        "id": .string(id),
        "user_id": .string("user-1"),
        "title": .string(title),
        "completed": .bool(false),
        "minutes": .integer(25),
        "planned_sessions": .integer(1),
        "completed_sessions": .integer(0),
        "category_id": .null,
        "priority": .string("N"),
        "due_date": .null,
        "updated_at": .string(updatedAt),
        "deleted": .bool(deleted),
    ]
}

private func categoryRow(id: String, name: String, updatedAt: String) -> SyncRow {
    [
        "id": .string(id),
        "user_id": .string("user-1"),
        "name": .string(name),
        "color_argb": .null,
        "updated_at": .string(updatedAt),
        "deleted": .bool(false),
    ]
}

private func makeEngine(api: FakeSyncApi, checkpoints: FakeCheckpoints, store: SyncTestStore) -> SyncEngine {
    SyncEngine(
        api: api,
        tables: [makeTaskTableSync(store: store)],
        checkpoints: checkpoints
    )
}

// MARK: - Tests

struct SyncRulesTests {
    @Test func dirtyLocalKeeps() {
        #expect(resolvePull(localDirty: true, remoteDeleted: false) == .keepLocal)
    }

    @Test func cleanLocalApplies() {
        #expect(resolvePull(localDirty: false, remoteDeleted: false) == .apply)
        #expect(resolvePull(localDirty: nil, remoteDeleted: true) == .apply)
    }
}

@Suite(.serialized)
struct SyncEngineTests {

    @Test func pushClearsDirtyAndPullAppliesRemote() async throws {
        let store = SyncTestStore()
        let api = FakeSyncApi()
        let checkpoints = FakeCheckpoints()
        store.upsert([
            SyncTestTask(syncId: "local-1", legacyId: 1, title: "Local task", dirty: true, locallyModifiedAt: 1, deleted: false),
        ])
        await api.setRemote([taskRow(id: "remote-1", title: "Remote task", updatedAt: "2026-06-12T10:00:00+00:00")], table: "tasks")

        let result = try await makeEngine(api: api, checkpoints: checkpoints, store: store).syncOnce()
        #expect(result == .success)

        let pushed = await api.pushedRows(table: "tasks")
        #expect(pushed.count == 1)
        #expect(SyncJSON.rowId(pushed[0]) == "local-1")
        #expect(pushed[0]["updated_at"] == nil)

        let local = store.get("local-1")
        #expect(local?.dirty == false)

        let applied = store.get("remote-1")
        #expect(applied?.title == "Remote task")
        #expect(applied?.legacyId == 100)
        #expect(applied?.dirty == false)
    }

    @Test func dirtyLocalSurvivesPull() async throws {
        let store = SyncTestStore()
        let api = FakeSyncApi()
        let checkpoints = FakeCheckpoints()
        store.upsert([
            SyncTestTask(syncId: "local-1", legacyId: 1, title: "Pushed title", dirty: true, locallyModifiedAt: 1, deleted: false),
        ])
        await api.setOnUpsert { table in
            guard table == "tasks" else { return }
            store.upsert([
                SyncTestTask(syncId: "local-1", legacyId: 1, title: "Edited during push", dirty: true, locallyModifiedAt: 2, deleted: false),
            ])
        }
        await api.setRemote([taskRow(id: "local-1", title: "Remote title", updatedAt: "2026-06-12T10:00:00+00:00")], table: "tasks")

        _ = try await makeEngine(api: api, checkpoints: checkpoints, store: store).syncOnce()

        let row = store.get("local-1")
        #expect(row?.title == "Edited during push")
        #expect(row?.dirty == true)
    }

    @Test func checkpointAdvancesToMaxUpdatedAt() async throws {
        let store = SyncTestStore()
        let api = FakeSyncApi()
        let checkpoints = FakeCheckpoints()
        let categorySync = makeCategoryTableSync(store: store)
        await api.setRemote([
            categoryRow(id: "c1", name: "Work", updatedAt: "2026-06-12T10:00:00+00:00"),
            categoryRow(id: "c2", name: "Home", updatedAt: "2026-06-12T11:00:00+00:00"),
        ], table: "categories")

        let engine = SyncEngine(api: api, tables: [categorySync], checkpoints: checkpoints)
        _ = try await engine.syncOnce()
        let checkpointAfterFirst = await checkpoints.value(for: "categories")
        #expect(checkpointAfterFirst == "2026-06-12T11:00:00+00:00")

        _ = try await engine.syncOnce()
        let args = await api.pullArgs(for: "categories")
        #expect(args.count == 2)
        #expect(args[1] == "2026-06-12T11:00:00+00:00")
    }

    @Test func failingTableDoesNotBlockOthers() async throws {
        let store = SyncTestStore()
        let api = FakeSyncApi()
        let checkpoints = FakeCheckpoints()
        await api.failPull(for: "tasks")
        await api.setRemote([categoryRow(id: "c1", name: "Work", updatedAt: "2026-06-12T10:00:00+00:00")], table: "categories")

        let engine = SyncEngine(
            api: api,
            tables: [
                makeTaskTableSync(store: store),
                makeCategoryTableSync(store: store),
            ],
            checkpoints: checkpoints
        )
        let result = try await engine.syncOnce()
        #expect(result == .partialFailure(failedTables: ["tasks"]))
        let tasksCheckpoint = await checkpoints.value(for: "tasks")
        let categoryRow = store.get("c1")
        let categoriesCheckpoint = await checkpoints.value(for: "categories")
        #expect(tasksCheckpoint == nil)
        #expect(categoryRow != nil)
        #expect(categoriesCheckpoint == "2026-06-12T10:00:00+00:00")
    }

    @Test func concurrentSyncOnceCallsCollapse() async throws {
        let store = SyncTestStore()
        let api = FakeSyncApi()
        let checkpoints = FakeCheckpoints()
        let engine = makeEngine(api: api, checkpoints: checkpoints, store: store)

        async let first = engine.syncOnce()
        async let second = engine.syncOnce()
        let results = try await [first, second]
        #expect(results[0] == results[1])
        let pullCount = await api.pullArgs(for: "tasks").count
        #expect(pullCount == 1)
    }
}

struct SyncStatusTests {
    @Test func upToDateWhenEligibleSuccess() {
        let snapshot = SyncSnapshot(lastAttemptAtMillis: 1, eligible: true, result: .success)
        #expect(resolveSyncStatusPresentation(snapshot).status == .upToDate)
    }

    @Test func skippedWhenIneligible() {
        let snapshot = SyncSnapshot(lastAttemptAtMillis: 1, eligible: false, result: nil)
        #expect(resolveSyncStatusPresentation(snapshot).status == .skipped)
    }
}

struct SyncLogoutTests {
    @Test func warnOnlyWhenFlushFailedAndDirtyRemain() {
        #expect(logoutDecision(flushSucceeded: false, dirtyRemaining: true) == .warnUnsynced)
        #expect(logoutDecision(flushSucceeded: true, dirtyRemaining: true) == .signOut)
        #expect(logoutDecision(flushSucceeded: false, dirtyRemaining: false) == .signOut)
    }
}
