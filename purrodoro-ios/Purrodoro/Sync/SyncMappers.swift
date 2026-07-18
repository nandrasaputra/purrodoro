//
//  SyncMappers.swift
//  Purrodoro
//
//  Domain model ↔ sync row mapping. Mirrors Android `EntityMappers.kt`.
//

import Foundation

// MARK: - Priority tags

extension TaskPriority {
    var storageTag: String {
        switch self {
        case .low: return "L"
        case .normal: return "N"
        case .high: return "H"
        }
    }

    static func fromStorageTag(_ tag: String) -> TaskPriority {
        switch tag {
        case "L": return .low
        case "H": return .high
        default: return .normal
        }
    }
}

// MARK: - Categories

enum SyncMappers {
    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    static func dayKey(_ date: Date) -> String {
        dayFormatter.string(from: date)
    }

    static func parseDay(_ iso: String) -> Date? {
        dayFormatter.date(from: iso)
    }

    // MARK: Task

    static func taskItem(
        from record: TaskRecord,
        categoryName: String?
    ) -> TaskItem? {
        guard let uuid = UUID(uuidString: record.id), !record.deleted else { return nil }
        return TaskItem(
            id: uuid,
            title: record.title,
            priority: TaskPriority.fromStorageTag(record.priorityTag),
            plannedSessions: max(record.plannedSessions, 1),
            completedSessions: record.completedSessions,
            completed: record.completed,
            createdAt: Date(),
            category: categoryName,
            dueDate: record.dueDateIso.flatMap(parseDay)
        )
    }

    static func taskRecord(
        from item: TaskItem,
        syncId: String,
        legacyId: Int,
        categoryUuid: String?,
        focusMinutes: Int,
        dirty: Bool,
        modifiedAt: Int64,
        deleted: Bool = false
    ) -> TaskRecord {
        TaskRecord(
            id: syncId,
            legacyId: legacyId,
            title: item.title,
            completed: item.completed,
            minutes: focusMinutes,
            plannedSessions: item.plannedSessions,
            completedSessions: item.completedSessions,
            categoryId: categoryUuid,
            priorityTag: item.priority.storageTag,
            dueDateIso: item.dueDate.map { dayKey($0) },
            dirty: dirty,
            locallyModifiedAt: modifiedAt,
            deleted: deleted
        )
    }

    // MARK: Habit

    static func habitItem(
        from habit: HabitRecord,
        logs: [HabitLogRecord]
    ) -> HabitItem? {
        guard let uuid = UUID(uuidString: habit.id), !habit.deleted else { return nil }
        var item = HabitItem(
            id: uuid,
            name: habit.name,
            emoji: habit.emoji,
            colorHex: UInt32(truncatingIfNeeded: habit.colorArgb),
            targetPerDay: 1,
            createdAt: Date(timeIntervalSince1970: TimeInterval(habit.createdAtMillis) / 1000),
            scheduledWeekdays: decodeWeekdays(tag: habit.occurrenceTag, days: habit.occurrenceDays),
            timeBlock: HabitTimeBlock.fromStorageTag(habit.timeBlockTag)
        )
        var logMap: [String: Int] = [:]
        var rests = Set<String>()
        for entry in logs where !entry.deleted {
            switch entry.stateTag {
            case "S":
                rests.insert(entry.logDateIso)
            case "C":
                logMap[entry.logDateIso] = max(item.targetPerDay, 1)
            case "P":
                logMap[entry.logDateIso] = max(entry.progress, 0)
            default:
                break
            }
        }
        item.log = logMap
        item.restDays = rests
        return item
    }

    static func habitRecord(
        from item: HabitItem,
        syncId: String,
        legacyId: Int,
        dirty: Bool,
        modifiedAt: Int64,
        deleted: Bool = false
    ) -> HabitRecord {
        let (tag, days) = encodeWeekdays(item.scheduledWeekdays)
        return HabitRecord(
            id: syncId,
            legacyId: legacyId,
            name: item.name,
            emoji: item.emoji,
            colorArgb: Int64(item.colorHex),
            createdAtMillis: Int64(item.createdAt.timeIntervalSince1970 * 1000),
            occurrenceTag: tag,
            occurrenceDays: days,
            timeBlockTag: item.timeBlock.storageTag,
            dirty: dirty,
            locallyModifiedAt: modifiedAt,
            deleted: deleted
        )
    }

    static func habitLogRecords(
        from item: HabitItem,
        habitSyncId: String,
        existingLogs: [HabitLogRecord],
        dirty: Bool,
        modifiedAt: Int64
    ) -> [HabitLogRecord] {
        let existingByDate = Dictionary(
            uniqueKeysWithValues: existingLogs.filter { $0.habitId == habitSyncId }.map { ($0.logDateIso, $0) }
        )
        var rows: [HabitLogRecord] = []
        var seenDates = Set<String>()

        for day in item.restDays {
            seenDates.insert(day)
            rows.append(logRow(
                habitId: habitSyncId,
                date: day,
                state: "S",
                progress: 0,
                existing: existingByDate[day],
                dirty: dirty,
                modifiedAt: modifiedAt
            ))
        }
        for (day, count) in item.log {
            guard !item.restDays.contains(day) else { continue }
            seenDates.insert(day)
            let target = max(item.targetPerDay, 1)
            if count >= target {
                rows.append(logRow(
                    habitId: habitSyncId, date: day, state: "C", progress: 0,
                    existing: existingByDate[day], dirty: dirty, modifiedAt: modifiedAt
                ))
            } else if count > 0 {
                rows.append(logRow(
                    habitId: habitSyncId, date: day, state: "P", progress: count,
                    existing: existingByDate[day], dirty: dirty, modifiedAt: modifiedAt
                ))
            }
        }
        for (date, existing) in existingByDate where !seenDates.contains(date) && !existing.deleted {
            var tombstone = existing
            tombstone.deleted = true
            tombstone.dirty = dirty
            tombstone.locallyModifiedAt = modifiedAt
            rows.append(tombstone)
        }
        return rows
    }

    private static func logRow(
        habitId: String,
        date: String,
        state: String,
        progress: Int,
        existing: HabitLogRecord?,
        dirty: Bool,
        modifiedAt: Int64
    ) -> HabitLogRecord {
        HabitLogRecord(
            id: existing?.id ?? UUID().uuidString.lowercased(),
            habitId: habitId,
            logDateIso: date,
            stateTag: state,
            progress: progress,
            dirty: dirty,
            locallyModifiedAt: modifiedAt,
            deleted: false
        )
    }

    // MARK: Session history

    static func focusRecord(from row: SessionRecordRow) -> FocusSessionRecord? {
        guard !row.deleted, let uuid = UUID(uuidString: row.id) else { return nil }
        return FocusSessionRecord(
            id: uuid,
            endedAt: Date(timeIntervalSince1970: TimeInterval(row.endedAt) / 1000),
            minutes: row.focusMinutes,
            intentName: row.taskName
        )
    }

    static func sessionRow(
        from record: FocusSessionRecord,
        syncId: String,
        legacyId: Int64,
        timerSettings: TimerSettings,
        dirty: Bool,
        modifiedAt: Int64,
        deleted: Bool = false
    ) -> SessionRecordRow {
        let endedMs = Int64(record.endedAt.timeIntervalSince1970 * 1000)
        let startedMs = endedMs - Int64(record.minutes) * 60_000
        return SessionRecordRow(
            id: syncId,
            legacyId: legacyId,
            startedAt: startedMs,
            endedAt: endedMs,
            focusMinutes: record.minutes,
            shortBreakMinutes: timerSettings.shortBreakMinutes,
            longBreakMinutes: timerSettings.longBreakMinutes,
            completedFocusCount: 1,
            totalFocusCount: 1,
            completedShortBreakCount: 0,
            completedLongBreakCount: 0,
            statusName: "Completed",
            taskName: record.intentName,
            focusTypeName: record.intentName == nil ? "Empty" : "Task",
            dirty: dirty,
            locallyModifiedAt: modifiedAt,
            deleted: deleted
        )
    }

    // MARK: Weekday encoding

    private static func encodeWeekdays(_ weekdays: Set<Int>) -> (String, String) {
        guard !weekdays.isEmpty else { return ("D", "") }
        let names = weekdays.sorted().compactMap { iosWeekdayToJavaName($0) }
        return ("W", names.joined(separator: ","))
    }

    private static func decodeWeekdays(tag: String, days: String) -> Set<Int> {
        guard tag == "W" else { return [] }
        return Set(days.split(separator: ",").compactMap { javaNameToIOSWeekday(String($0)) })
    }

    private static func iosWeekdayToJavaName(_ ios: Int) -> String? {
        switch ios {
        case 1: return "SUNDAY"
        case 2: return "MONDAY"
        case 3: return "TUESDAY"
        case 4: return "WEDNESDAY"
        case 5: return "THURSDAY"
        case 6: return "FRIDAY"
        case 7: return "SATURDAY"
        default: return nil
        }
    }

    private static func javaNameToIOSWeekday(_ name: String) -> Int? {
        switch name.uppercased() {
        case "SUNDAY": return 1
        case "MONDAY": return 2
        case "TUESDAY": return 3
        case "WEDNESDAY": return 4
        case "THURSDAY": return 5
        case "FRIDAY": return 6
        case "SATURDAY": return 7
        default: return nil
        }
    }
}

extension HabitTimeBlock {
    var storageTag: String {
        switch self {
        case .morning: return "M"
        case .afternoon: return "A"
        case .evening: return "E"
        case .any: return "X"
        }
    }

    static func fromStorageTag(_ tag: String) -> HabitTimeBlock {
        switch tag {
        case "M": return .morning
        case "A": return .afternoon
        case "E": return .evening
        default: return .any
        }
    }
}
