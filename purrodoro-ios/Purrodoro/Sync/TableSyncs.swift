//
//  TableSyncs.swift
//  Purrodoro
//
//  Production TableSync bindings over SyncRepository. Mirrors Android
//  `sync/TableSyncs.kt`.
//

import Foundation
import Helpers

enum TableSyncs {
    static func makeAll(
        repo: SyncRepository,
        userId: @escaping @Sendable () async -> String,
        applySyncedPrefs: @escaping @MainActor @Sendable (String, String) -> Void
    ) -> [any TableSync] {
        // Unsafe: closures are `@MainActor` and only invoked via MainActorGate.
        nonisolated(unsafe) let repoRef = repo

        let categorySync = BaseTableSync<CategoryRecord>(
            table: "categories",
            userId: userId,
            dirtyEntities: { repoRef.syncDirtyCategories() },
            entityById: { repoRef.syncCategoryById($0) },
            upsertEntities: { repoRef.syncUpsertCategories($0) },
            clearDirtyIfUnchanged: { repoRef.syncClearCategoryDirtyIfUnchanged(id: $0, snapshotModifiedAt: $1) },
            encodeForPush: { record, uid in
                (try? SyncDtoCodec.encodePush(record.toDto(userId: uid))) ?? [:]
            },
            decodeRemote: { row, existing in
                guard let dto = try? SyncDtoCodec.decode(CategoryDto.self, from: row) else {
                    preconditionFailure("invalid category row")
                }
                let legacy = repoRef.allocateSyncLegacyCategoryId()
                return CategoryRecord.fromDto(dto, existing: existing, legacyId: legacy)
            }
        )

        let taskSync = BaseTableSync<TaskRecord>(
            table: "tasks",
            userId: userId,
            dirtyEntities: { repoRef.syncDirtyTasks() },
            entityById: { repoRef.syncTaskById($0) },
            upsertEntities: { repoRef.syncUpsertTasks($0) },
            clearDirtyIfUnchanged: { repoRef.syncClearTaskDirtyIfUnchanged(id: $0, snapshotModifiedAt: $1) },
            encodeForPush: { record, uid in
                (try? SyncDtoCodec.encodePush(record.toDto(userId: uid))) ?? [:]
            },
            decodeRemote: { row, existing in
                guard let dto = try? SyncDtoCodec.decode(TaskDto.self, from: row) else {
                    preconditionFailure("invalid task row")
                }
                let legacy = repoRef.allocateSyncLegacyTaskId()
                return TaskRecord.fromDto(dto, existing: existing, legacyId: legacy)
            }
        )

        let habitSync = BaseTableSync<HabitRecord>(
            table: "habits",
            userId: userId,
            dirtyEntities: { repoRef.syncDirtyHabits() },
            entityById: { repoRef.syncHabitById($0) },
            upsertEntities: { repoRef.syncUpsertHabits($0) },
            clearDirtyIfUnchanged: { repoRef.syncClearHabitDirtyIfUnchanged(id: $0, snapshotModifiedAt: $1) },
            encodeForPush: { record, uid in
                (try? SyncDtoCodec.encodePush(record.toDto(userId: uid))) ?? [:]
            },
            decodeRemote: { row, existing in
                guard let dto = try? SyncDtoCodec.decode(HabitDto.self, from: row) else {
                    preconditionFailure("invalid habit row")
                }
                let legacy = repoRef.allocateSyncLegacyHabitId()
                return HabitRecord.fromDto(dto, existing: existing, legacyId: legacy)
            }
        )

        let habitLogSync = BaseTableSync<HabitLogRecord>(
            table: "habit_logs",
            userId: userId,
            dirtyEntities: { repoRef.syncDirtyHabitLogs() },
            entityById: { repoRef.syncHabitLogById($0) },
            upsertEntities: { repoRef.syncUpsertHabitLogs($0) },
            clearDirtyIfUnchanged: { repoRef.syncClearHabitLogDirtyIfUnchanged(id: $0, snapshotModifiedAt: $1) },
            encodeForPush: { record, uid in
                (try? SyncDtoCodec.encodePush(record.toDto(userId: uid))) ?? [:]
            },
            decodeRemote: { row, existing in
                guard let dto = try? SyncDtoCodec.decode(HabitLogDto.self, from: row) else {
                    preconditionFailure("invalid habit log row")
                }
                return HabitLogRecord.fromDto(dto, existing: existing)
            },
            entityByNaturalKey: { row in
                guard let habitId = SyncJSON.stringField(row, "habit_id"),
                      let logDate = SyncJSON.stringField(row, "log_date") else { return nil }
                return repoRef.syncHabitLogByHabitAndDate(habitId: habitId, logDate: logDate)
            },
            deleteEntity: { repoRef.syncDeleteHabitLogById($0.id) },
            withId: { entity, newId in
                HabitLogRecord(
                    id: newId,
                    habitId: entity.habitId,
                    logDateIso: entity.logDateIso,
                    stateTag: entity.stateTag,
                    progress: entity.progress,
                    dirty: entity.dirty,
                    locallyModifiedAt: entity.locallyModifiedAt,
                    deleted: entity.deleted
                )
            }
        )

        let sessionSync = BaseTableSync<SessionRecordRow>(
            table: "session_records",
            userId: userId,
            dirtyEntities: { repoRef.syncDirtySessionRecords() },
            entityById: { repoRef.syncSessionById($0) },
            upsertEntities: { repoRef.syncUpsertSessionRecords($0) },
            clearDirtyIfUnchanged: { repoRef.syncClearSessionDirtyIfUnchanged(id: $0, snapshotModifiedAt: $1) },
            encodeForPush: { record, uid in
                (try? SyncDtoCodec.encodePush(record.toDto(userId: uid))) ?? [:]
            },
            decodeRemote: { row, existing in
                guard let dto = try? SyncDtoCodec.decode(SessionRecordDto.self, from: row) else {
                    preconditionFailure("invalid session row")
                }
                return SessionRecordRow.fromDto(dto, existing: existing)
            },
            entityByNaturalKey: { row in
                guard let localId = Self.legacyId(from: row) else { return nil }
                return repoRef.syncSessionByLegacyId(localId)
            },
            deleteEntity: { repoRef.syncDeleteSessionById($0.id) },
            withId: { entity, newId in
                SessionRecordRow(
                    id: newId,
                    legacyId: entity.legacyId,
                    startedAt: entity.startedAt,
                    endedAt: entity.endedAt,
                    focusMinutes: entity.focusMinutes,
                    shortBreakMinutes: entity.shortBreakMinutes,
                    longBreakMinutes: entity.longBreakMinutes,
                    completedFocusCount: entity.completedFocusCount,
                    totalFocusCount: entity.totalFocusCount,
                    completedShortBreakCount: entity.completedShortBreakCount,
                    completedLongBreakCount: entity.completedLongBreakCount,
                    statusName: entity.statusName,
                    taskName: entity.taskName,
                    focusTypeName: entity.focusTypeName,
                    dirty: entity.dirty,
                    locallyModifiedAt: entity.locallyModifiedAt,
                    deleted: entity.deleted
                )
            }
        )

        let stampSync = BaseTableSync<DailyStampRecord>(
            table: "daily_stamps",
            userId: userId,
            dirtyEntities: { repoRef.syncDirtyDailyStamps() },
            entityById: { repoRef.syncDailyStampById($0) },
            upsertEntities: { repoRef.syncUpsertDailyStamps($0) },
            clearDirtyIfUnchanged: { repoRef.syncClearDailyStampDirtyIfUnchanged(id: $0, snapshotModifiedAt: $1) },
            encodeForPush: { record, uid in
                (try? SyncDtoCodec.encodePush(record.toDto(userId: uid))) ?? [:]
            },
            decodeRemote: { row, existing in
                guard let dto = try? SyncDtoCodec.decode(DailyStampDto.self, from: row) else {
                    preconditionFailure("invalid stamp row")
                }
                return DailyStampRecord.fromDto(dto, existing: existing)
            },
            entityByNaturalKey: { row in
                guard let stampDate = SyncJSON.stringField(row, "stamp_date") else { return nil }
                return repoRef.syncDailyStampByDate(stampDate)
            },
            deleteEntity: { repoRef.syncDeleteDailyStampById($0.id) },
            withId: { entity, newId in
                DailyStampRecord(
                    id: newId,
                    stampDateIso: entity.stampDateIso,
                    count: entity.count,
                    dirty: entity.dirty,
                    locallyModifiedAt: entity.locallyModifiedAt,
                    deleted: entity.deleted
                )
            }
        )

        let profileSync = ProfileTableSync(
            repo: repoRef,
            userId: userId,
            applySyncedPrefs: applySyncedPrefs
        )

        return [categorySync, taskSync, habitSync, habitLogSync, sessionSync, stampSync, profileSync]
    }

    private static func legacyId(from row: SyncRow) -> Int64? {
        switch row["local_id"] {
        case .integer(let value):
            return Int64(value)
        case .double(let value):
            return Int64(value)
        default:
            return nil
        }
    }
}

// MARK: - Profile (user_id keyed)

final class ProfileTableSync: TableSync, @unchecked Sendable {
    let table = "profiles"
    let conflictKey = "user_id"

    // Held unsafely so MainActorGate bodies stay callable from nonisolated sync.
    nonisolated(unsafe) private let repo: SyncRepository
    private let userId: @Sendable () async -> String
    private let applySyncedPrefs: @MainActor @Sendable (String, String) -> Void
    private var pushSnapshotModifiedAt: Int64?

    init(
        repo: SyncRepository,
        userId: @escaping @Sendable () async -> String,
        applySyncedPrefs: @escaping @MainActor @Sendable (String, String) -> Void
    ) {
        self.repo = repo
        self.userId = userId
        self.applySyncedPrefs = applySyncedPrefs
    }

    func dirtyRows() async -> [SyncRow] {
        let row = await MainActorGate.perform { repo.syncProfileRecord()?.dirty == true ? repo.syncProfileRecord() : nil }
        pushSnapshotModifiedAt = row?.locallyModifiedAt
        guard let row else { return [] }
        let uid = await userId()
        let dto = ProfilePushDto(
            userId: uid,
            displayName: row.displayName,
            timerSettings: SyncDtoCodec.timerSettingsObject(from: row.timerSettingsJson),
            premiumOverrideEnabled: false,
            premiumOverrideActive: false
        )
        return [(try? SyncDtoCodec.encodePush(dto)) ?? [:]]
    }

    func markPushed(_ rows: [SyncRow]) async {
        guard let snapshot = pushSnapshotModifiedAt else { return }
        await MainActorGate.perform { repo.syncClearProfileDirtyIfUnchanged(snapshotModifiedAt: snapshot) }
    }

    func applyRemote(_ rows: [SyncRow]) async {
        guard let row = rows.max(by: { lhs, rhs in
            let left = SyncJSON.stringField(lhs, "updated_at").flatMap(parseUpdatedAt) ?? .distantPast
            let right = SyncJSON.stringField(rhs, "updated_at").flatMap(parseUpdatedAt) ?? .distantPast
            return left < right
        }) else { return }

        let displayName = SyncJSON.stringField(row, "display_name")?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let settingsJson = SyncDtoCodec.timerSettingsJson(from: row["timer_settings"] ?? .object([:]))

        await MainActorGate.perform {
            let local = repo.syncProfileRecord()
            guard resolvePull(localDirty: local?.dirty, remoteDeleted: false) == .apply else { return }
            repo.syncUpsertProfile(
                ProfileRecord(
                    displayName: displayName,
                    timerSettingsJson: settingsJson,
                    dirty: false,
                    locallyModifiedAt: local?.locallyModifiedAt ?? 0
                )
            )
            guard repo.syncProfileRecord()?.dirty == false else { return }
            applySyncedPrefs(displayName, settingsJson)
        }
    }

    func maxRemoteUpdatedAt(_ rows: [SyncRow]) async -> String? {
        SyncJSON.maxUpdatedAt(of: rows)
    }

    private func parseUpdatedAt(_ iso: String) -> Date? {
        ISO8601DateFormatter.sync.date(from: iso)
            ?? ISO8601DateFormatter.syncNoFraction.date(from: iso)
    }
}

private extension ISO8601DateFormatter {
    static let sync: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    static let syncNoFraction: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()
}
