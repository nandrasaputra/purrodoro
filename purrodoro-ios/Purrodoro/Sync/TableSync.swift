//
//  TableSync.swift
//  Purrodoro
//
//  Shared TableSync skeleton with push snapshots and pull dispatch.
//  Mirrors Android `sync/TableSync.kt`.
//
//  DAO hooks are synchronous `@MainActor` closures. One `MainActorGate` hop per
//  operation — never `await` a MainActor-isolated async closure from the main
//  thread (schedule+wait deadlock).
//

import Foundation

/// Generic table adapter — subclasses plug in DAO-style persistence.
final class BaseTableSync<E: SyncEntity>: TableSync, @unchecked Sendable {
    let table: String
    private var pushSnapshot: [String: Int64] = [:]

    private let dirtyEntities: @MainActor @Sendable () -> [E]
    private let entityById: @MainActor @Sendable (String) -> E?
    private let upsertEntities: @MainActor @Sendable ([E]) -> Void
    private let clearDirtyIfUnchanged: @MainActor @Sendable (String, Int64) -> Void
    private let encodeForPush: @Sendable (E, String) -> SyncRow
    private let decodeRemote: @MainActor @Sendable (SyncRow, E?) -> E
    private let entityByNaturalKey: @MainActor @Sendable (SyncRow) -> E?
    private let deleteEntity: @MainActor @Sendable (E) -> Void
    private let withId: @Sendable (E, String) -> E
    private let userId: @Sendable () async -> String

    init(
        table: String,
        userId: @escaping @Sendable () async -> String,
        dirtyEntities: @escaping @MainActor @Sendable () -> [E],
        entityById: @escaping @MainActor @Sendable (String) -> E?,
        upsertEntities: @escaping @MainActor @Sendable ([E]) -> Void,
        clearDirtyIfUnchanged: @escaping @MainActor @Sendable (String, Int64) -> Void,
        encodeForPush: @escaping @Sendable (E, String) -> SyncRow,
        decodeRemote: @escaping @MainActor @Sendable (SyncRow, E?) -> E,
        entityByNaturalKey: (@MainActor @Sendable (SyncRow) -> E?)? = nil,
        deleteEntity: (@MainActor @Sendable (E) -> Void)? = nil,
        withId: (@Sendable (E, String) -> E)? = nil
    ) {
        self.table = table
        self.userId = userId
        self.dirtyEntities = dirtyEntities
        self.entityById = entityById
        self.upsertEntities = upsertEntities
        self.clearDirtyIfUnchanged = clearDirtyIfUnchanged
        self.encodeForPush = encodeForPush
        self.decodeRemote = decodeRemote
        self.entityByNaturalKey = entityByNaturalKey ?? { _ in nil }
        self.deleteEntity = deleteEntity ?? { _ in
            preconditionFailure("\(table) does not support natural-key adoption")
        }
        self.withId = withId ?? { _, _ in
            preconditionFailure("\(table) does not support natural-key adoption")
        }
    }

    func dirtyRows() async -> [SyncRow] {
        let dirty = await MainActorGate.perform { dirtyEntities() }
        pushSnapshot = Dictionary(uniqueKeysWithValues: dirty.map { ($0.syncId, $0.locallyModifiedAt) })
        let uid = await userId()
        return dirty.map { encodeForPush($0, uid) }
    }

    func markPushed(_ rows: [SyncRow]) async {
        for row in rows {
            let id = SyncJSON.rowId(row)
            guard let snapshotModifiedAt = pushSnapshot[id] else { continue }
            await MainActorGate.perform { clearDirtyIfUnchanged(id, snapshotModifiedAt) }
        }
    }

    func applyRemote(_ rows: [SyncRow]) async {
        for row in rows {
            await MainActorGate.perform { applyRemoteRow(row) }
        }
    }

    func maxRemoteUpdatedAt(_ rows: [SyncRow]) async -> String? {
        SyncJSON.maxUpdatedAt(of: rows)
    }

    @MainActor
    private func applyRemoteRow(_ row: SyncRow) {
        let id = SyncJSON.rowId(row)
        var existing = entityById(id)
        var adopted: E?
        if existing == nil, let natural = entityByNaturalKey(row), natural.syncId != id {
            existing = natural
            adopted = natural
        }

        switch resolvePull(localDirty: existing?.isDirty, remoteDeleted: SyncJSON.rowDeleted(row)) {
        case .keepLocal:
            if let adopted {
                deleteEntity(adopted)
                upsertEntities([withId(adopted, id)])
            }
        case .apply:
            if let adopted { deleteEntity(adopted) }
            upsertEntities([decodeRemote(row, existing)])
        }
    }
}
