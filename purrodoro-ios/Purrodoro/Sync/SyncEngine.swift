//
//  SyncEngine.swift
//  Purrodoro
//
//  Delta sync engine: per-table pull-then-push with LWW + checkpoints.
//  Mirrors Android `sync/SyncEngine.kt`.
//

import Foundation

protocol CheckpointStore: Sendable {
    func get(table: String) async -> String?
    func set(table: String, iso: String) async
}

protocol TableSync: Sendable {
    var table: String { get }
    var conflictKey: String { get }
    func dirtyRows() async -> [SyncRow]
    func markPushed(_ rows: [SyncRow]) async
    func applyRemote(_ rows: [SyncRow]) async
    func maxRemoteUpdatedAt(_ rows: [SyncRow]) async -> String?
}

extension TableSync {
    var conflictKey: String { "id" }
}

final class SyncEngine: @unchecked Sendable {
    enum SyncResult: Equatable, Sendable {
        case success
        case partialFailure(failedTables: [String])
    }

    private let api: any SyncApi
    private let tables: [any TableSync]
    private let checkpoints: any CheckpointStore
    private let isFatal: @Sendable (Error) -> Bool
    private let lock = NSLock()
    private var isRunning = false
    private var waiters: [CheckedContinuation<SyncResult, Error>] = []

    init(
        api: any SyncApi,
        tables: [any TableSync],
        checkpoints: any CheckpointStore,
        isFatal: @escaping @Sendable (Error) -> Bool = { _ in false }
    ) {
        self.api = api
        self.tables = tables
        self.checkpoints = checkpoints
        self.isFatal = isFatal
    }

    func syncOnce() async throws -> SyncResult {
        lock.lock()
        if isRunning {
            lock.unlock()
            return try await withCheckedThrowingContinuation { continuation in
                lock.lock()
                waiters.append(continuation)
                lock.unlock()
            }
        }
        isRunning = true
        lock.unlock()

        do {
            let result = try await performSyncOnce()
            completePass(with: .success(result))
            return result
        } catch {
            completePass(with: .failure(error))
            throw error
        }
    }

    private func completePass(with outcome: Result<SyncResult, Error>) {
        lock.lock()
        isRunning = false
        let pending = waiters
        waiters = []
        lock.unlock()
        for waiter in pending {
            waiter.resume(with: outcome)
        }
    }

    private func performSyncOnce() async throws -> SyncResult {
        var failed: [String] = []
        for tableSync in tables {
            do {
                try await syncTable(tableSync)
            } catch is CancellationError {
                throw CancellationError()
            } catch {
                if isFatal(error) { throw error }
                failed.append(tableSync.table)
            }
        }
        if failed.isEmpty { return .success }
        return .partialFailure(failedTables: failed)
    }

    private func syncTable(_ tableSync: any TableSync) async throws {
        try await pull(tableSync)
        try await push(tableSync)
    }

    private func pull(_ tableSync: any TableSync) async throws {
        let checkpoint = await checkpoints.get(table: tableSync.table)
        let remote = try await api.pullSince(table: tableSync.table, afterIso: checkpoint)
        guard !remote.isEmpty else { return }
        await tableSync.applyRemote(remote)
        if let maxUpdated = await tableSync.maxRemoteUpdatedAt(remote) {
            await checkpoints.set(table: tableSync.table, iso: maxUpdated)
        }
    }

    private func push(_ tableSync: any TableSync) async throws {
        let dirty = await tableSync.dirtyRows()
        guard !dirty.isEmpty else { return }
        try await api.upsert(table: tableSync.table, rows: dirty, conflictKey: tableSync.conflictKey)
        await tableSync.markPushed(dirty)
    }
}
