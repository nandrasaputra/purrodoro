//
//  BackupModels.swift
//  Purrodoro
//
//  On-disk JSON shape for Backup & Restore — mirrors Android
//  `data/backup/BackupModels.kt` (schema version 1).
//

import Foundation

// MARK: - Envelope

struct BackupEnvelope: Codable {
    var schemaVersion: Int
    var appVersionName: String
    var exportedAt: Int64
    var data: BackupData

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case appVersionName = "app_version_name"
        case exportedAt = "exported_at"
        case data
    }
}

struct BackupData: Codable {
    var profile: BackupProfile
    var prefs: BackupPrefs
    var categories: [BackupCategory]
    var tasks: [BackupTask]
    var habits: [BackupHabit]
    var habitLogs: [BackupHabitLog]
    var records: [BackupRecord]
    var stamps: [BackupStamp]

    enum CodingKeys: String, CodingKey {
        case profile, prefs, categories, tasks, habits, records, stamps
        case habitLogs = "habit_logs"
    }
}

struct BackupProfile: Codable {
    var displayName: String
    var timerSettings: BackupTimerSettings

    enum CodingKeys: String, CodingKey {
        case displayName = "display_name"
        case timerSettings = "timer_settings"
    }
}

struct BackupTimerSettings: Codable {
    var focusMinutes: Int
    var shortBreakMinutes: Int
    var longBreakMinutes: Int
    var autoStartNextSession: Bool
    var focusSessionsPerCycle: Int

    enum CodingKeys: String, CodingKey {
        case focusMinutes = "focus_minutes"
        case shortBreakMinutes = "short_break_minutes"
        case longBreakMinutes = "long_break_minutes"
        case autoStartNextSession = "auto_start_next_session"
        case focusSessionsPerCycle = "focus_sessions_per_cycle"
    }

    static let stock = BackupTimerSettings(
        focusMinutes: 25,
        shortBreakMinutes: 5,
        longBreakMinutes: 15,
        autoStartNextSession: false,
        focusSessionsPerCycle: 4
    )
}

struct BackupPrefs: Codable {
    var appearance: String
    var language: String
    var glass: String
    var homeScene: String
    var selectedSoundId: Int
    var notificationSound: Bool

    enum CodingKeys: String, CodingKey {
        case appearance, language, glass
        case homeScene = "home_scene"
        case selectedSoundId = "selected_sound_id"
        case notificationSound = "notification_sound"
    }
}

struct BackupCategory: Codable {
    var id: Int
    var name: String
    var colorArgb: Int64?

    enum CodingKeys: String, CodingKey {
        case id, name
        case colorArgb = "color_argb"
    }
}

struct BackupTask: Codable {
    var id: Int
    var title: String
    var minutes: Int
    var completed: Bool
    var plannedSessions: Int
    var completedSessions: Int
    var categoryId: Int?
    var priority: String
    var dueDate: String?

    enum CodingKeys: String, CodingKey {
        case id, title, minutes, completed, priority
        case plannedSessions = "planned_sessions"
        case completedSessions = "completed_sessions"
        case categoryId = "category_id"
        case dueDate = "due_date"
    }
}

struct BackupHabit: Codable {
    var id: Int
    var name: String
    var emoji: String
    var colorArgb: Int64
    var createdAt: Int64
    var occurrence: String
    var occurrenceDays: String
    var timeBlock: String

    enum CodingKeys: String, CodingKey {
        case id, name, emoji, occurrence
        case colorArgb = "color_argb"
        case createdAt = "created_at"
        case occurrenceDays = "occurrence_days"
        case timeBlock = "time_block"
    }
}

struct BackupHabitLog: Codable {
    var habitId: Int
    var date: String
    var state: String
    var progress: Int

    enum CodingKeys: String, CodingKey {
        case date, state, progress
        case habitId = "habit_id"
    }
}

struct BackupRecord: Codable {
    var id: Int64
    var startedAt: Int64
    var endedAt: Int64
    var focusMinutes: Int
    var shortBreakMinutes: Int
    var longBreakMinutes: Int
    var completedFocusCount: Int
    var totalFocusCount: Int
    var completedShortBreakCount: Int
    var completedLongBreakCount: Int
    var status: String
    var taskName: String?
    var focusType: String?

    enum CodingKeys: String, CodingKey {
        case id, status
        case startedAt = "started_at"
        case endedAt = "ended_at"
        case focusMinutes = "focus_minutes"
        case shortBreakMinutes = "short_break_minutes"
        case longBreakMinutes = "long_break_minutes"
        case completedFocusCount = "completed_focus_count"
        case totalFocusCount = "total_focus_count"
        case completedShortBreakCount = "completed_short_break_count"
        case completedLongBreakCount = "completed_long_break_count"
        case taskName = "task_name"
        case focusType = "focus_type"
    }
}

struct BackupStamp: Codable {
    var date: String
    var count: Int
}

// MARK: - Restore bundle (validated, trusted)

struct RestorePrefs {
    var appearance: AppearanceMode
    var language: AppLanguage
    var selectedSoundId: Int
}

struct RestoreBundle {
    var displayName: String
    var timerSettings: TimerSettings
    var prefs: RestorePrefs
    var tasks: [TaskItem]
    var habits: [HabitItem]
    var sessionRecords: [FocusSessionRecord]
    var stamps: [String: Int]
}

enum BackupParseResult {
    case success(RestoreBundle)
    case malformed
    case unsupportedVersion(Int)
}

enum RestoreOutcome {
    case success
    case malformed
    case unsupportedVersion(Int)
    case tooLarge
    case readError
    case applyError
    case timerRunning
}
