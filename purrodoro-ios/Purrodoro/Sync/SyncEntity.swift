//
//  SyncEntity.swift
//  Purrodoro
//
//  Persisted rows for the seven Supabase-synced tables. Mirrors Android
//  `data/local/SyncedEntities.kt`.
//

import Foundation

protocol SyncEntity: Sendable {
    var syncId: String { get }
    var locallyModifiedAt: Int64 { get }
    var isDirty: Bool { get }
}

struct CategoryRecord: Codable, Equatable, SyncEntity, Identifiable {
    var id: String
    var legacyId: Int
    var name: String
    var colorArgb: Int64?
    var dirty: Bool
    var locallyModifiedAt: Int64
    var deleted: Bool

    var syncId: String { id }
    var isDirty: Bool { dirty }
}

struct TaskRecord: Codable, Equatable, SyncEntity, Identifiable {
    var id: String
    var legacyId: Int
    var title: String
    var completed: Bool
    var minutes: Int
    var plannedSessions: Int
    var completedSessions: Int
    var categoryId: String?
    var priorityTag: String
    var dueDateIso: String?
    var dirty: Bool
    var locallyModifiedAt: Int64
    var deleted: Bool

    var syncId: String { id }
    var isDirty: Bool { dirty }
}

struct HabitRecord: Codable, Equatable, SyncEntity, Identifiable {
    var id: String
    var legacyId: Int
    var name: String
    var emoji: String
    var colorArgb: Int64
    var createdAtMillis: Int64
    var occurrenceTag: String
    var occurrenceDays: String
    var timeBlockTag: String
    var dirty: Bool
    var locallyModifiedAt: Int64
    var deleted: Bool

    var syncId: String { id }
    var isDirty: Bool { dirty }
}

struct HabitLogRecord: Codable, Equatable, SyncEntity, Identifiable {
    var id: String
    var habitId: String
    var logDateIso: String
    var stateTag: String
    var progress: Int
    var dirty: Bool
    var locallyModifiedAt: Int64
    var deleted: Bool

    var syncId: String { id }
    var isDirty: Bool { dirty }
}

struct SessionRecordRow: Codable, Equatable, SyncEntity, Identifiable {
    var id: String
    var legacyId: Int64
    var startedAt: Int64
    var endedAt: Int64
    var focusMinutes: Int
    var shortBreakMinutes: Int
    var longBreakMinutes: Int
    var completedFocusCount: Int
    var totalFocusCount: Int
    var completedShortBreakCount: Int
    var completedLongBreakCount: Int
    var statusName: String
    var taskName: String?
    var focusTypeName: String?
    var dirty: Bool
    var locallyModifiedAt: Int64
    var deleted: Bool

    var syncId: String { id }
    var isDirty: Bool { dirty }
}

struct DailyStampRecord: Codable, Equatable, SyncEntity, Identifiable {
    var id: String
    var stampDateIso: String
    var count: Int
    var dirty: Bool
    var locallyModifiedAt: Int64
    var deleted: Bool

    var syncId: String { id }
    var isDirty: Bool { dirty }
}

/// Singleton profile sync row (display name + timer settings JSON).
struct ProfileRecord: Codable, Equatable, SyncEntity {
    var displayName: String
    var timerSettingsJson: String
    var dirty: Bool
    var locallyModifiedAt: Int64

    var syncId: String { "profile" }
    var isDirty: Bool { dirty }
}

enum SyncEntityKeys {
    static let categories = "purrodoro.sync.categories"
    static let tasks = "purrodoro.sync.tasks"
    static let habits = "purrodoro.sync.habits"
    static let habitLogs = "purrodoro.sync.habit_logs"
    static let sessionRecords = "purrodoro.sync.session_records"
    static let dailyStamps = "purrodoro.sync.daily_stamps"
    static let profile = "purrodoro.sync.profile"
    static let checkpoints = "purrodoro.sync.checkpoints"
    static let migrated = "purrodoro.sync.migrated_v1"

    static let nextTaskId = "purrodoro.sync.next_task_id"
    static let nextCategoryId = "purrodoro.sync.next_category_id"
    static let nextHabitId = "purrodoro.sync.next_habit_id"
    static let nextSessionId = "purrodoro.sync.next_session_id"

    static let legacyTaskId = "purrodoro.sync.legacy_task_id"
    static let legacyCategoryId = "purrodoro.sync.legacy_category_id"
    static let legacyHabitId = "purrodoro.sync.legacy_habit_id"

    static let legacyIdFloor = 1_000_000
}
