//
//  SyncDtoCodec.swift
//  Purrodoro
//
//  Wire-format encode/decode for sync DTOs. Mirrors Android `SyncDtos.kt`.
//

import Foundation
import Helpers

enum SyncDtoCodec {
    private static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        return encoder
    }()

    private static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return decoder
    }()

    static func encodePush<T: Encodable>(_ value: T) throws -> SyncRow {
        let data = try encoder.encode(value)
        guard var object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return [:]
        }
        object.removeValue(forKey: "updated_at")
        return object.mapValues { anyJSON(from: $0) }
    }

    static func decode<T: Decodable>(_ type: T.Type, from row: SyncRow) throws -> T {
        let object = row.mapValues { foundationValue($0) }
        let data = try JSONSerialization.data(withJSONObject: object)
        return try decoder.decode(T.self, from: data)
    }

    static func timerSettingsObject(from json: String) -> AnyJSON {
        guard let data = json.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) else {
            return .object([:])
        }
        return anyJSON(from: object)
    }

    static func timerSettingsJson(from value: AnyJSON) -> String {
        let object = foundationValue(value)
        guard JSONSerialization.isValidJSONObject(object),
              let data = try? JSONSerialization.data(withJSONObject: object),
              let json = String(data: data, encoding: .utf8) else {
            return "{}"
        }
        return json
    }

    private static func foundationValue(_ value: AnyJSON) -> Any {
        switch value {
        case .null:
            return NSNull()
        case .bool(let bool):
            return bool
        case .string(let string):
            return string
        case .integer(let integer):
            return integer
        case .double(let double):
            return double
        case .array(let array):
            return array.map { foundationValue($0) }
        case .object(let object):
            return object.mapValues { foundationValue($0) }
        }
    }

    private static func anyJSON(from value: Any) -> AnyJSON {
        switch value {
        case is NSNull:
            return .null
        case let bool as Bool:
            return .bool(bool)
        case let int as Int:
            return .integer(int)
        case let int64 as Int64:
            return .integer(Int(int64))
        case let double as Double:
            return .double(double)
        case let string as String:
            return .string(string)
        case let array as [Any]:
            return .array(array.map { anyJSON(from: $0) })
        case let dict as [String: Any]:
            return .object(dict.mapValues { anyJSON(from: $0) })
        default:
            return .string(String(describing: value))
        }
    }
}

// MARK: - DTOs

struct CategoryDto: Codable {
    let id: String
    let userId: String
    let name: String
    let colorArgb: Int64?
    let updatedAt: String?
    let deleted: Bool
}

struct TaskDto: Codable {
    let id: String
    let userId: String
    let title: String
    let completed: Bool
    let minutes: Int
    let plannedSessions: Int
    let completedSessions: Int
    let categoryId: String?
    let priority: String
    let dueDate: String?
    let updatedAt: String?
    let deleted: Bool
}

struct HabitDto: Codable {
    let id: String
    let userId: String
    let name: String
    let emoji: String
    let colorArgb: Int64
    let createdAtMillis: Int64
    let occurrenceTag: String
    let occurrenceDays: String
    let timeBlock: String
    let updatedAt: String?
    let deleted: Bool
}

struct HabitLogDto: Codable {
    let id: String
    let userId: String
    let habitId: String
    let logDate: String
    let state: String
    let progress: Int
    let updatedAt: String?
    let deleted: Bool
}

struct SessionRecordDto: Codable {
    let id: String
    let userId: String
    let localId: Int64
    let startedAt: Int64
    let endedAt: Int64
    let focusMinutes: Int
    let shortBreakMinutes: Int
    let longBreakMinutes: Int
    let completedFocusCount: Int
    let totalFocusCount: Int
    let completedShortBreakCount: Int
    let completedLongBreakCount: Int
    let status: String
    let taskName: String?
    let updatedAt: String?
    let deleted: Bool
}

struct DailyStampDto: Codable {
    let id: String
    let userId: String
    let stampDate: String
    let count: Int
    let updatedAt: String?
    let deleted: Bool
}

struct ProfilePushDto: Encodable {
    let userId: String
    let displayName: String?
    let timerSettings: AnyJSON
    let premiumOverrideEnabled: Bool
    let premiumOverrideActive: Bool
}

// MARK: - Entity ↔ DTO

extension CategoryRecord {
    func toDto(userId: String) -> CategoryDto {
        CategoryDto(
            id: id,
            userId: userId,
            name: name,
            colorArgb: colorArgb,
            updatedAt: nil,
            deleted: deleted
        )
    }

    static func fromDto(_ dto: CategoryDto, existing: CategoryRecord?, legacyId: Int) -> CategoryRecord {
        CategoryRecord(
            id: dto.id,
            legacyId: existing?.legacyId ?? legacyId,
            name: dto.name,
            colorArgb: dto.colorArgb,
            dirty: false,
            locallyModifiedAt: existing?.locallyModifiedAt ?? 0,
            deleted: dto.deleted
        )
    }
}

extension TaskRecord {
    func toDto(userId: String) -> TaskDto {
        TaskDto(
            id: id,
            userId: userId,
            title: title,
            completed: completed,
            minutes: minutes,
            plannedSessions: plannedSessions,
            completedSessions: completedSessions,
            categoryId: categoryId,
            priority: priorityTag,
            dueDate: dueDateIso,
            updatedAt: nil,
            deleted: deleted
        )
    }

    static func fromDto(_ dto: TaskDto, existing: TaskRecord?, legacyId: Int) -> TaskRecord {
        TaskRecord(
            id: dto.id,
            legacyId: existing?.legacyId ?? legacyId,
            title: dto.title,
            completed: dto.completed,
            minutes: dto.minutes,
            plannedSessions: dto.plannedSessions,
            completedSessions: dto.completedSessions,
            categoryId: dto.categoryId,
            priorityTag: dto.priority,
            dueDateIso: dto.dueDate,
            dirty: false,
            locallyModifiedAt: existing?.locallyModifiedAt ?? 0,
            deleted: dto.deleted
        )
    }
}

extension HabitRecord {
    func toDto(userId: String) -> HabitDto {
        HabitDto(
            id: id,
            userId: userId,
            name: name,
            emoji: emoji,
            colorArgb: colorArgb,
            createdAtMillis: createdAtMillis,
            occurrenceTag: occurrenceTag,
            occurrenceDays: occurrenceDays,
            timeBlock: timeBlockTag,
            updatedAt: nil,
            deleted: deleted
        )
    }

    static func fromDto(_ dto: HabitDto, existing: HabitRecord?, legacyId: Int) -> HabitRecord {
        HabitRecord(
            id: dto.id,
            legacyId: existing?.legacyId ?? legacyId,
            name: dto.name,
            emoji: dto.emoji,
            colorArgb: dto.colorArgb,
            createdAtMillis: dto.createdAtMillis,
            occurrenceTag: dto.occurrenceTag,
            occurrenceDays: dto.occurrenceDays,
            timeBlockTag: dto.timeBlock,
            dirty: false,
            locallyModifiedAt: existing?.locallyModifiedAt ?? 0,
            deleted: dto.deleted
        )
    }
}

extension HabitLogRecord {
    func toDto(userId: String) -> HabitLogDto {
        HabitLogDto(
            id: id,
            userId: userId,
            habitId: habitId,
            logDate: logDateIso,
            state: stateTag,
            progress: progress,
            updatedAt: nil,
            deleted: deleted
        )
    }

    static func fromDto(_ dto: HabitLogDto, existing: HabitLogRecord?) -> HabitLogRecord {
        HabitLogRecord(
            id: dto.id,
            habitId: dto.habitId,
            logDateIso: dto.logDate,
            stateTag: dto.state,
            progress: dto.progress,
            dirty: false,
            locallyModifiedAt: existing?.locallyModifiedAt ?? 0,
            deleted: dto.deleted
        )
    }
}

extension SessionRecordRow {
    func toDto(userId: String) -> SessionRecordDto {
        SessionRecordDto(
            id: id,
            userId: userId,
            localId: legacyId,
            startedAt: startedAt,
            endedAt: endedAt,
            focusMinutes: focusMinutes,
            shortBreakMinutes: shortBreakMinutes,
            longBreakMinutes: longBreakMinutes,
            completedFocusCount: completedFocusCount,
            totalFocusCount: totalFocusCount,
            completedShortBreakCount: completedShortBreakCount,
            completedLongBreakCount: completedLongBreakCount,
            status: statusName,
            taskName: taskName,
            updatedAt: nil,
            deleted: deleted
        )
    }

    static func fromDto(_ dto: SessionRecordDto, existing: SessionRecordRow?) -> SessionRecordRow {
        SessionRecordRow(
            id: dto.id,
            legacyId: existing?.legacyId ?? dto.localId,
            startedAt: dto.startedAt,
            endedAt: dto.endedAt,
            focusMinutes: dto.focusMinutes,
            shortBreakMinutes: dto.shortBreakMinutes,
            longBreakMinutes: dto.longBreakMinutes,
            completedFocusCount: dto.completedFocusCount,
            totalFocusCount: dto.totalFocusCount,
            completedShortBreakCount: dto.completedShortBreakCount,
            completedLongBreakCount: dto.completedLongBreakCount,
            statusName: dto.status,
            taskName: dto.taskName,
            focusTypeName: existing?.focusTypeName,
            dirty: false,
            locallyModifiedAt: existing?.locallyModifiedAt ?? 0,
            deleted: dto.deleted
        )
    }
}

extension DailyStampRecord {
    func toDto(userId: String) -> DailyStampDto {
        DailyStampDto(
            id: id,
            userId: userId,
            stampDate: stampDateIso,
            count: count,
            updatedAt: nil,
            deleted: deleted
        )
    }

    static func fromDto(_ dto: DailyStampDto, existing: DailyStampRecord?) -> DailyStampRecord {
        DailyStampRecord(
            id: dto.id,
            stampDateIso: dto.stampDate,
            count: dto.count,
            dirty: false,
            locallyModifiedAt: existing?.locallyModifiedAt ?? 0,
            deleted: dto.deleted
        )
    }
}
