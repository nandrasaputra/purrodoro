//
//  BackupCodec.swift
//  Purrodoro
//
//  Pure encode/decode for Backup & Restore. Clamping + validation for
//  untrusted imports — mirrors Android `BackupCodec.kt`.
//

import Foundation

private let backupMaxNameLen = 100
private let backupMaxTitleLen = 200
private let backupMaxEmojiLen = 16
private let backupMaxMinutes = 180

enum BackupCodec {
    static let currentSchemaVersion = 1
    static let maxFileBytes = 10 * 1024 * 1024

    private static let encoder: JSONEncoder = {
        let e = JSONEncoder()
        e.outputFormatting = [.prettyPrinted, .sortedKeys]
        return e
    }()

    private static let decoder = JSONDecoder()

    // MARK: - Encode

    static func encode(
        displayName: String,
        timerSettings: TimerSettings,
        appearance: AppearanceMode,
        language: AppLanguage,
        selectedSoundId: Int,
        tasks: [TaskItem],
        habits: [HabitItem],
        sessionRecords: [FocusSessionRecord],
        stamps: [String: Int],
        appVersionName: String,
        exportedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) throws -> String {
        let (categories, categoryIds) = buildCategories(from: tasks)
        let habitPairs = indexedHabits(habits)

        let envelope = BackupEnvelope(
            schemaVersion: currentSchemaVersion,
            appVersionName: appVersionName,
            exportedAt: exportedAt,
            data: BackupData(
                profile: BackupProfile(
                    displayName: displayName.clean(backupMaxNameLen),
                    timerSettings: timerSettings.toBackup()
                ),
                prefs: BackupPrefs(
                    appearance: appearance.toBackupAppearance(),
                    language: language.toBackupLanguage(),
                    glass: "Adaptive",
                    homeScene: "Room",
                    selectedSoundId: max(selectedSoundId, 1),
                    notificationSound: true
                ),
                categories: categories,
                tasks: tasks.enumerated().map { index, task in
                    task.toBackup(
                        id: index + 1,
                        categoryId: task.trimmedCategory.flatMap { categoryIds[$0] },
                        defaultFocusMinutes: timerSettings.focusMinutes
                    )
                },
                habits: habitPairs.map { $0.habit.toBackup(id: $0.id) },
                habitLogs: habitPairs.flatMap { $0.habit.logsToBackup(habitId: $0.id) },
                records: sessionRecords.enumerated().map { index, record in
                    record.toBackup(id: Int64(index + 1))
                },
                stamps: stamps.compactMap { date, count in
                    count > 0 ? BackupStamp(date: date, count: count) : nil
                }.sorted { $0.date < $1.date }
            )
        )
        let data = try encoder.encode(envelope)
        return String(data: data, encoding: .utf8) ?? ""
    }

    // MARK: - Decode

    static func decode(_ raw: String) -> BackupParseResult {
        guard let data = raw.data(using: .utf8),
              let envelope = try? decoder.decode(BackupEnvelope.self, from: data)
        else { return .malformed }

        if envelope.schemaVersion <= 0 { return .malformed }
        if envelope.schemaVersion > currentSchemaVersion {
            return .unsupportedVersion(envelope.schemaVersion)
        }
        return .success(envelope.data.toBundle())
    }

    static func readCapped(_ data: Data, max: Int = maxFileBytes) -> Result<String, ReadCapError> {
        if data.count > max { return .failure(.tooLarge) }
        guard let text = String(data: data, encoding: .utf8) else { return .failure(.invalidData) }
        return .success(text)
    }

    // MARK: - Private mapping

    private static func buildCategories(from tasks: [TaskItem]) -> ([BackupCategory], [String: Int]) {
        var nameToId: [String: Int] = [:]
        var categories: [BackupCategory] = []
        var nextId = 1
        for task in tasks {
            guard let name = task.trimmedCategory, nameToId[name] == nil else { continue }
            nameToId[name] = nextId
            categories.append(BackupCategory(id: nextId, name: name, colorArgb: nil))
            nextId += 1
        }
        return (categories, nameToId)
    }

    private static func indexedHabits(_ habits: [HabitItem]) -> [(id: Int, habit: HabitItem)] {
        habits.sorted { $0.createdAt < $1.createdAt }.enumerated().map { ($0.offset + 1, $0.element) }
    }
}

enum ReadCapError: Error {
    case tooLarge
    case invalidData
}

// MARK: - BackupData → RestoreBundle

private extension BackupData {
    func toBundle() -> RestoreBundle {
        let categories = categories
            .filter { $0.id > 0 }
            .reduce(into: [Int: BackupCategory]()) { $0[$1.id] = $1 }
            .values
            .map { $0 }

        let categoryNames = Dictionary(uniqueKeysWithValues: categories.map { ($0.id, $0.name.clean(backupMaxNameLen)) })

        let tasks = tasks
            .filter { $0.id > 0 }
            .reduce(into: [Int: BackupTask]()) { $0[$1.id] = $1 }
            .values
            .map { $0.toTaskItem(categoryNames: categoryNames) }

        let habitDtos = habits.filter { $0.id > 0 }.reduce(into: [Int: BackupHabit]()) { $0[$1.id] = $1 }
        let habitIds = Set(habitDtos.keys)
        let logsByHabit = habitLogs.filter { habitIds.contains($0.habitId) }.reduce(into: [Int: [BackupHabitLog]]()) {
            $0[$1.habitId, default: []].append($1)
        }

        let habits = habitDtos.values.map { h in
            h.toHabitItem(logs: logsByHabit[h.id] ?? [])
        }

        let records = records
            .filter { $0.id > 0 }
            .reduce(into: [Int64: BackupRecord]()) { $0[$1.id] = $1 }
            .values
            .map { $0.toFocusSessionRecord() }

        let stamps = stamps.compactMap { stamp -> (String, Int)? in
            guard stamp.count > 0, isValidDayKey(stamp.date) else { return nil }
            return (stamp.date, stamp.count)
        }.reduce(into: [String: Int]()) { result, pair in
            result[pair.0] = max(result[pair.0] ?? 0, pair.1)
        }

        return RestoreBundle(
            displayName: profile.displayName.clean(backupMaxNameLen),
            timerSettings: profile.timerSettings.toTimerSettings(),
            prefs: RestorePrefs(
                appearance: AppearanceMode.fromBackup(prefs.appearance),
                language: AppLanguage.fromBackup(prefs.language),
                selectedSoundId: max(prefs.selectedSoundId, 1)
            ),
            tasks: tasks,
            habits: habits,
            sessionRecords: records,
            stamps: stamps
        )
    }
}

// MARK: - Domain conversions

private extension TimerSettings {
    func toBackup() -> BackupTimerSettings {
        BackupTimerSettings(
            focusMinutes: focusMinutes,
            shortBreakMinutes: shortBreakMinutes,
            longBreakMinutes: longBreakMinutes,
            autoStartNextSession: autoStartNextSession,
            focusSessionsPerCycle: focusSessionsPerCycle
        )
    }
}

private extension BackupTimerSettings {
    func toTimerSettings() -> TimerSettings {
        TimerSettings(
            focusMinutes: focusMinutes.clamped(1, backupMaxMinutes),
            shortBreakMinutes: shortBreakMinutes.clamped(1, backupMaxMinutes),
            longBreakMinutes: longBreakMinutes.clamped(1, backupMaxMinutes),
            autoStartNextSession: autoStartNextSession,
            focusSessionsPerCycle: focusSessionsPerCycle.clamped(1, TimerSettings.maxFocusSessionsPerCycle)
        )
    }
}

private extension TaskItem {
    func toBackup(id: Int, categoryId: Int?, defaultFocusMinutes: Int) -> BackupTask {
        BackupTask(
            id: id,
            title: title.clean(backupMaxTitleLen),
            minutes: defaultFocusMinutes.clamped(1, backupMaxMinutes),
            completed: completed,
            plannedSessions: plannedSessions.clamped(0, 99),
            completedSessions: completedSessions.clamped(0, 99),
            categoryId: categoryId,
            priority: priority.toBackupTag(),
            dueDate: dueDate.map { dayFormatter.string(from: $0) }
        )
    }
}

private extension BackupTask {
    func toTaskItem(categoryNames: [Int: String]) -> TaskItem {
        TaskItem(
            title: title.clean(backupMaxTitleLen),
            priority: TaskPriority.fromBackupTag(priority),
            plannedSessions: max(plannedSessions, 1),
            completedSessions: completedSessions.clamped(0, 99),
            completed: completed,
            createdAt: Date(),
            category: categoryId.flatMap { categoryNames[$0] },
            dueDate: dueDate.flatMap { parseDay($0) }
        )
    }
}

private extension HabitItem {
    func toBackup(id: Int) -> BackupHabit {
        let (tag, days) = scheduledWeekdays.toBackupOccurrence()
        return BackupHabit(
            id: id,
            name: name.clean(backupMaxNameLen),
            emoji: emoji.clean(backupMaxEmojiLen),
            colorArgb: Int64(colorHex),
            createdAt: Int64(createdAt.timeIntervalSince1970 * 1000),
            occurrence: tag,
            occurrenceDays: days,
            timeBlock: timeBlock.toBackupTag()
        )
    }

    func logsToBackup(habitId: Int) -> [BackupHabitLog] {
        var rows: [BackupHabitLog] = []
        for day in restDays {
            rows.append(BackupHabitLog(habitId: habitId, date: day, state: "S", progress: 0))
        }
        for (day, count) in log {
            guard !restDays.contains(day) else { continue }
            let target = max(targetPerDay, 1)
            if count >= target {
                rows.append(BackupHabitLog(habitId: habitId, date: day, state: "C", progress: 0))
            } else if count > 0 {
                rows.append(BackupHabitLog(habitId: habitId, date: day, state: "P", progress: count))
            }
        }
        return rows
    }
}

private extension BackupHabit {
    func toHabitItem(logs: [BackupHabitLog]) -> HabitItem {
        var item = HabitItem(
            name: name.clean(backupMaxNameLen),
            emoji: emoji.clean(backupMaxEmojiLen),
            colorHex: UInt32(truncatingIfNeeded: colorArgb),
            targetPerDay: 1,
            createdAt: Date(timeIntervalSince1970: TimeInterval(createdAt) / 1000),
            scheduledWeekdays: decodeWeekdays(occurrence: occurrence, days: occurrenceDays),
            timeBlock: HabitTimeBlock.fromBackupTag(timeBlock)
        )
        var logMap: [String: Int] = [:]
        var rests = Set<String>()
        for entry in logs {
            guard isValidDayKey(entry.date) else { continue }
            switch entry.state {
            case "S":
                rests.insert(entry.date)
            case "C":
                logMap[entry.date] = max(item.targetPerDay, 1)
            case "P":
                logMap[entry.date] = max(entry.progress, 0)
            default:
                break
            }
        }
        item.log = logMap
        item.restDays = rests
        return item
    }
}

private extension FocusSessionRecord {
    func toBackup(id: Int64) -> BackupRecord {
        let endedMs = Int64(endedAt.timeIntervalSince1970 * 1000)
        return BackupRecord(
            id: id,
            startedAt: endedMs - Int64(minutes) * 60_000,
            endedAt: endedMs,
            focusMinutes: minutes.clamped(1, backupMaxMinutes),
            shortBreakMinutes: 5,
            longBreakMinutes: 15,
            completedFocusCount: 1,
            totalFocusCount: 1,
            completedShortBreakCount: 0,
            completedLongBreakCount: 0,
            status: "Completed",
            taskName: intentName?.clean(backupMaxTitleLen),
            focusType: intentName == nil ? "Empty" : "Task"
        )
    }
}

private extension BackupRecord {
    func toFocusSessionRecord() -> FocusSessionRecord {
        FocusSessionRecord(
            endedAt: Date(timeIntervalSince1970: TimeInterval(endedAt) / 1000),
            minutes: focusMinutes.clamped(1, backupMaxMinutes),
            intentName: taskName?.clean(backupMaxTitleLen)
        )
    }
}

// MARK: - Tags

private extension TaskPriority {
    func toBackupTag() -> String {
        switch self {
        case .low: return "L"
        case .normal: return "N"
        case .high: return "H"
        }
    }

    static func fromBackupTag(_ tag: String) -> TaskPriority {
        switch tag {
        case "L": return .low
        case "H": return .high
        default: return .normal
        }
    }
}

private extension AppearanceMode {
    func toBackupAppearance() -> String {
        switch self {
        case .light: return "Light"
        case .dark: return "Dark"
        case .auto: return "Light"
        }
    }

    static func fromBackup(_ profile: String) -> AppearanceMode {
        switch profile {
        case "Dark": return .dark
        case "Light": return .light
        default: return .auto
        }
    }
}

private extension AppLanguage {
    func toBackupLanguage() -> String {
        switch self {
        case .en: return "English"
        case .id: return "Indonesian"
        }
    }

    static func fromBackup(_ raw: String) -> AppLanguage {
        switch raw {
        case "Indonesian", "id": return .id
        case "English", "en": return .en
        default: return .en
        }
    }
}

private extension Set<Int> {
    func toBackupOccurrence() -> (String, String) {
        guard !isEmpty else { return ("D", "") }
        let names = sorted().compactMap { iosWeekdayToJavaName($0) }
        return ("W", names.joined(separator: ","))
    }
}

private extension HabitTimeBlock {
    func toBackupTag() -> String {
        switch self {
        case .morning: return "M"
        case .afternoon: return "A"
        case .evening: return "E"
        case .any: return "X"
        }
    }

    static func fromBackupTag(_ tag: String) -> HabitTimeBlock {
        switch tag {
        case "M": return .morning
        case "A": return .afternoon
        case "E": return .evening
        default: return .any
        }
    }
}

private func decodeWeekdays(occurrence: String, days: String) -> Set<Int> {
    guard occurrence == "W" else { return [] }
    let names = days.split(separator: ",").map(String.init)
    return Set(names.compactMap { javaNameToIOSWeekday($0) })
}

private func iosWeekdayToJavaName(_ ios: Int) -> String? {
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

private func javaNameToIOSWeekday(_ name: String) -> Int? {
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

// MARK: - Helpers

private let dayFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    f.locale = Locale(identifier: "en_US_POSIX")
    return f
}()

private func parseDay(_ raw: String) -> Date? {
    dayFormatter.date(from: raw)
}

private func isValidDayKey(_ key: String) -> Bool {
    parseDay(key) != nil
}

private extension String {
    func clean(_ maxLen: Int) -> String {
        String(trimmingCharacters(in: .whitespacesAndNewlines).prefix(maxLen))
    }
}

private extension Int {
    func clamped(_ lo: Int, _ hi: Int) -> Int { Swift.min(Swift.max(self, lo), hi) }
}
