//
//  SyncRepository.swift
//  Purrodoro
//
//  Single persistence layer for all Supabase-synced tables. Mirrors the Room +
//  DataStore split in Android `AppRepository.kt` (domain rows + sync metadata).
//

import Foundation
import Combine

@MainActor
final class SyncRepository: ObservableObject {
    let store: KeyValueStore
    let localWrites = PassthroughSubject<Void, Never>()

    private var categories: [CategoryRecord] = []
    private var tasks: [TaskRecord] = []
    private var habits: [HabitRecord] = []
    private var habitLogs: [HabitLogRecord] = []
    private var sessionRecords: [SessionRecordRow] = []
    private var dailyStamps: [DailyStampRecord] = []
    private var profile: ProfileRecord?

    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        loadAll()
        migrateLegacyIfNeeded()
    }

    // MARK: - Domain reads

    func taskItems(focusMinutes: Int) -> [TaskItem] {
        let names = categoryNamesById()
        return tasks.compactMap { record in
            let name = record.categoryId.flatMap { names[$0] }
            return SyncMappers.taskItem(from: record, categoryName: name)
        }
    }

    func habitItems() -> [HabitItem] {
        habits.compactMap { habit in
            let logs = habitLogs.filter { $0.habitId == habit.id }
            return SyncMappers.habitItem(from: habit, logs: logs)
        }
    }

    func focusSessionRecords() -> [FocusSessionRecord] {
        sessionRecords
            .compactMap(SyncMappers.focusRecord)
            .sorted { $0.endedAt > $1.endedAt }
    }

    func stampCounts() -> [String: Int] {
        Dictionary(
            uniqueKeysWithValues: dailyStamps
                .filter { !$0.deleted && $0.count > 0 }
                .map { ($0.stampDateIso, $0.count) }
        )
    }

    func profileDisplayName() -> String {
        profile?.displayName ?? store.string(forKey: "purrodoro.userName") ?? ""
    }

    func profileTimerSettings() -> TimerSettings? {
        guard let json = profile?.timerSettingsJson.data(using: .utf8) else { return nil }
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return try? decoder.decode(TimerSettings.self, from: json)
    }

    // MARK: - Domain writes

    func setTaskItems(_ items: [TaskItem], focusMinutes: Int) {
        let now = nowMillis()
        var nextCategories = categories
        var nextTasks = tasks

        let existingTasks = Dictionary(uniqueKeysWithValues: tasks.map { ($0.id, $0) })
        var categoryIdByName = categoryIdLookup(in: nextCategories)

        var upsertedIds = Set<String>()
        for item in items {
            let syncId = item.id.uuidString.lowercased()
            upsertedIds.insert(syncId)
            let existing = existingTasks[syncId]
            let legacyId = existing?.legacyId ?? allocateNextTaskId()
            let categoryUuid = resolveCategoryId(
                name: item.trimmedCategory,
                categories: &nextCategories,
                categoryIdByName: &categoryIdByName,
                now: now
            )
            let record = SyncMappers.taskRecord(
                from: item,
                syncId: syncId,
                legacyId: legacyId,
                categoryUuid: categoryUuid,
                focusMinutes: focusMinutes,
                dirty: true,
                modifiedAt: now
            )
            if let idx = nextTasks.firstIndex(where: { $0.id == syncId }) {
                nextTasks[idx] = record
            } else {
                nextTasks.append(record)
            }
        }

        for index in nextTasks.indices {
            guard !nextTasks[index].deleted, !upsertedIds.contains(nextTasks[index].id) else { continue }
            nextTasks[index].deleted = true
            nextTasks[index].dirty = true
            nextTasks[index].locallyModifiedAt = now
        }

        categories = nextCategories
        tasks = nextTasks
        saveCategories()
        saveTasks()
        notifyWrite()
    }

    func setHabitItems(_ items: [HabitItem]) {
        let now = nowMillis()
        var nextHabits = habits
        var nextLogs = habitLogs
        let existingHabits = Dictionary(uniqueKeysWithValues: habits.map { ($0.id, $0) })
        var keptHabitIds = Set<String>()

        for item in items {
            let syncId = item.id.uuidString.lowercased()
            keptHabitIds.insert(syncId)
            let legacyId = existingHabits[syncId]?.legacyId ?? allocateNextHabitId()
            let record = SyncMappers.habitRecord(
                from: item,
                syncId: syncId,
                legacyId: legacyId,
                dirty: true,
                modifiedAt: now
            )
            if let idx = nextHabits.firstIndex(where: { $0.id == syncId }) {
                nextHabits[idx] = record
            } else {
                nextHabits.append(record)
            }
            let logRows = SyncMappers.habitLogRecords(
                from: item,
                habitSyncId: syncId,
                existingLogs: nextLogs,
                dirty: true,
                modifiedAt: now
            )
            mergeLogRows(&nextLogs, habitId: syncId, rows: logRows)
        }

        for index in nextHabits.indices where !nextHabits[index].deleted && !keptHabitIds.contains(nextHabits[index].id) {
            nextHabits[index].deleted = true
            nextHabits[index].dirty = true
            nextHabits[index].locallyModifiedAt = now
        }

        habits = nextHabits
        habitLogs = nextLogs
        saveHabits()
        saveHabitLogs()
        notifyWrite()
    }

    func setFocusSessionRecords(_ records: [FocusSessionRecord], timerSettings: TimerSettings) {
        let now = nowMillis()
        var next = sessionRecords
        let existing = Dictionary(uniqueKeysWithValues: sessionRecords.map { ($0.id, $0) })
        var kept = Set<String>()

        for record in records {
            let syncId = record.id.uuidString.lowercased()
            kept.insert(syncId)
            let legacyId = existing[syncId]?.legacyId ?? allocateNextSessionId()
            let row = SyncMappers.sessionRow(
                from: record,
                syncId: syncId,
                legacyId: legacyId,
                timerSettings: timerSettings,
                dirty: true,
                modifiedAt: now
            )
            if let idx = next.firstIndex(where: { $0.id == syncId }) {
                next[idx] = row
            } else {
                next.append(row)
            }
        }

        sessionRecords = next
        saveSessionRecords()
        notifyWrite()
    }

    func appendFocusSession(_ record: FocusSessionRecord, timerSettings: TimerSettings, maxCount: Int) {
        var current = focusSessionRecords()
        current.insert(record, at: 0)
        if current.count > maxCount {
            current = Array(current.prefix(maxCount))
        }
        setFocusSessionRecords(current, timerSettings: timerSettings)
    }

    func unionFocusSessionRecords(_ imported: [FocusSessionRecord], timerSettings: TimerSettings, maxCount: Int) {
        var merged = focusSessionRecords()
        for record in imported {
            let duplicate = merged.contains {
                abs($0.endedAt.timeIntervalSince(record.endedAt)) < 1
                    && $0.minutes == record.minutes
                    && $0.intentName == record.intentName
            }
            if !duplicate { merged.append(record) }
        }
        merged.sort { $0.endedAt > $1.endedAt }
        if merged.count > maxCount {
            merged = Array(merged.prefix(maxCount))
        }
        setFocusSessionRecords(merged, timerSettings: timerSettings)
    }

    /// Soft-deletes a focus session so sync can push the tombstone (Android
    /// `deleteSessionRecord`).
    func deleteFocusSession(id: UUID) {
        let syncId = id.uuidString.lowercased()
        let now = nowMillis()
        guard let index = sessionRecords.firstIndex(where: { $0.id == syncId && !$0.deleted }) else { return }
        sessionRecords[index].deleted = true
        sessionRecords[index].dirty = true
        sessionRecords[index].locallyModifiedAt = now
        saveSessionRecords()
        notifyWrite()
    }

    func incrementStamp(for date: Date) {
        let key = SyncMappers.dayKey(date)
        let now = nowMillis()
        if let idx = dailyStamps.firstIndex(where: { $0.stampDateIso == key && !$0.deleted }) {
            dailyStamps[idx].count += 1
            dailyStamps[idx].dirty = true
            dailyStamps[idx].locallyModifiedAt = now
        } else if let idx = dailyStamps.firstIndex(where: { $0.stampDateIso == key }) {
            dailyStamps[idx].deleted = false
            dailyStamps[idx].count = 1
            dailyStamps[idx].dirty = true
            dailyStamps[idx].locallyModifiedAt = now
        } else {
            dailyStamps.append(
                DailyStampRecord(
                    id: UUID().uuidString.lowercased(),
                    stampDateIso: key,
                    count: 1,
                    dirty: true,
                    locallyModifiedAt: now,
                    deleted: false
                )
            )
        }
        saveDailyStamps()
        notifyWrite()
    }

    func applyRestoredStamps(_ stamps: [String: Int]) {
        let now = nowMillis()
        for (date, count) in stamps where count > 0 {
            if let idx = dailyStamps.firstIndex(where: { $0.stampDateIso == date }) {
                dailyStamps[idx].count = count
                dailyStamps[idx].deleted = false
                dailyStamps[idx].dirty = true
                dailyStamps[idx].locallyModifiedAt = now
            } else {
                dailyStamps.append(
                    DailyStampRecord(
                        id: UUID().uuidString.lowercased(),
                        stampDateIso: date,
                        count: count,
                        dirty: true,
                        locallyModifiedAt: now,
                        deleted: false
                    )
                )
            }
        }
        saveDailyStamps()
        notifyWrite()
    }

    func updateProfile(displayName: String, timerSettings: TimerSettings) {
        let now = nowMillis()
        let json = (try? encoder.encode(timerSettings)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}"
        profile = ProfileRecord(
            displayName: displayName.trimmingCharacters(in: .whitespacesAndNewlines),
            timerSettingsJson: json,
            dirty: true,
            locallyModifiedAt: now
        )
        saveProfile()
        notifyWrite()
    }

    // MARK: - Sync engine hooks

    func markAllAccountDataDirty() {
        let now = nowMillis()
        for index in categories.indices where !categories[index].deleted {
            categories[index].dirty = true
            categories[index].locallyModifiedAt = now
        }
        for index in tasks.indices where !tasks[index].deleted {
            tasks[index].dirty = true
            tasks[index].locallyModifiedAt = now
        }
        for index in habits.indices where !habits[index].deleted {
            habits[index].dirty = true
            habits[index].locallyModifiedAt = now
        }
        for index in habitLogs.indices where !habitLogs[index].deleted {
            habitLogs[index].dirty = true
            habitLogs[index].locallyModifiedAt = now
        }
        for index in sessionRecords.indices where !sessionRecords[index].deleted {
            sessionRecords[index].dirty = true
            sessionRecords[index].locallyModifiedAt = now
        }
        for index in dailyStamps.indices where !dailyStamps[index].deleted {
            dailyStamps[index].dirty = true
            dailyStamps[index].locallyModifiedAt = now
        }
        if profile == nil {
            profile = ProfileRecord(displayName: "", timerSettingsJson: "{}", dirty: true, locallyModifiedAt: now)
        } else {
            profile?.dirty = true
            profile?.locallyModifiedAt = now
        }
        saveAll()
        notifyWrite()
    }

    func hasAnyDirty() -> Bool {
        categories.contains(where: \.dirty)
            || tasks.contains(where: \.dirty)
            || habits.contains(where: \.dirty)
            || habitLogs.contains(where: \.dirty)
            || sessionRecords.contains(where: \.dirty)
            || dailyStamps.contains(where: \.dirty)
            || (profile?.dirty == true)
    }

    func resetSyncCheckpoints() {
        store.set(nil, forKey: SyncEntityKeys.checkpoints)
    }

    func clearAccountOwnedData() {
        categories = []
        tasks = []
        habits = []
        habitLogs = []
        sessionRecords = []
        dailyStamps = []
        profile = nil
        saveAll()
        resetSyncCheckpoints()
        store.set(nil, forKey: SyncEntityKeys.nextTaskId)
        store.set(nil, forKey: SyncEntityKeys.nextCategoryId)
        store.set(nil, forKey: SyncEntityKeys.nextHabitId)
        store.set(nil, forKey: SyncEntityKeys.nextSessionId)
        store.set(nil, forKey: SyncEntityKeys.legacyTaskId)
        store.set(nil, forKey: SyncEntityKeys.legacyCategoryId)
        store.set(nil, forKey: SyncEntityKeys.legacyHabitId)
        notifyWrite()
    }

    func allocateSyncLegacyTaskId() -> Int { allocateLegacyId(key: SyncEntityKeys.legacyTaskId) }
    func allocateSyncLegacyCategoryId() -> Int { allocateLegacyId(key: SyncEntityKeys.legacyCategoryId) }
    func allocateSyncLegacyHabitId() -> Int { allocateLegacyId(key: SyncEntityKeys.legacyHabitId) }

    // MARK: - Table sync access (no localWrites)

    func hasAnyAccountOwnedData() -> Bool {
        !categories.isEmpty
            || !tasks.isEmpty
            || !habits.isEmpty
            || !habitLogs.isEmpty
            || !sessionRecords.isEmpty
            || !dailyStamps.isEmpty
            || profile != nil
    }

    func syncDirtyCategories() -> [CategoryRecord] { categories.filter(\.dirty) }
    func syncCategoryById(_ id: String) -> CategoryRecord? { categories.first { $0.id == id } }
    func syncUpsertCategories(_ items: [CategoryRecord]) {
        for item in items {
            if let index = categories.firstIndex(where: { $0.id == item.id }) {
                categories[index] = item
            } else {
                categories.append(item)
            }
        }
        saveCategories()
    }
    func syncClearCategoryDirtyIfUnchanged(id: String, snapshotModifiedAt: Int64) {
        guard let index = categories.firstIndex(where: { $0.id == id }),
              categories[index].locallyModifiedAt == snapshotModifiedAt else { return }
        categories[index].dirty = false
        saveCategories()
    }
    func syncDeleteCategoryById(_ id: String) {
        categories.removeAll { $0.id == id }
        saveCategories()
    }

    func syncDirtyTasks() -> [TaskRecord] { tasks.filter(\.dirty) }
    func syncTaskById(_ id: String) -> TaskRecord? { tasks.first { $0.id == id } }
    func syncUpsertTasks(_ items: [TaskRecord]) {
        for item in items {
            if let index = tasks.firstIndex(where: { $0.id == item.id }) {
                tasks[index] = item
            } else {
                tasks.append(item)
            }
        }
        saveTasks()
    }
    func syncClearTaskDirtyIfUnchanged(id: String, snapshotModifiedAt: Int64) {
        guard let index = tasks.firstIndex(where: { $0.id == id }),
              tasks[index].locallyModifiedAt == snapshotModifiedAt else { return }
        tasks[index].dirty = false
        saveTasks()
    }

    func syncDirtyHabits() -> [HabitRecord] { habits.filter(\.dirty) }
    func syncHabitById(_ id: String) -> HabitRecord? { habits.first { $0.id == id } }
    func syncUpsertHabits(_ items: [HabitRecord]) {
        for item in items {
            if let index = habits.firstIndex(where: { $0.id == item.id }) {
                habits[index] = item
            } else {
                habits.append(item)
            }
        }
        saveHabits()
    }
    func syncClearHabitDirtyIfUnchanged(id: String, snapshotModifiedAt: Int64) {
        guard let index = habits.firstIndex(where: { $0.id == id }),
              habits[index].locallyModifiedAt == snapshotModifiedAt else { return }
        habits[index].dirty = false
        saveHabits()
    }

    func syncDirtyHabitLogs() -> [HabitLogRecord] { habitLogs.filter(\.dirty) }
    func syncHabitLogById(_ id: String) -> HabitLogRecord? { habitLogs.first { $0.id == id } }
    func syncHabitLogByHabitAndDate(habitId: String, logDate: String) -> HabitLogRecord? {
        habitLogs.first { $0.habitId == habitId && $0.logDateIso == logDate }
    }
    func syncUpsertHabitLogs(_ items: [HabitLogRecord]) {
        for item in items {
            if let index = habitLogs.firstIndex(where: { $0.id == item.id }) {
                habitLogs[index] = item
            } else {
                habitLogs.append(item)
            }
        }
        saveHabitLogs()
    }
    func syncClearHabitLogDirtyIfUnchanged(id: String, snapshotModifiedAt: Int64) {
        guard let index = habitLogs.firstIndex(where: { $0.id == id }),
              habitLogs[index].locallyModifiedAt == snapshotModifiedAt else { return }
        habitLogs[index].dirty = false
        saveHabitLogs()
    }
    func syncDeleteHabitLogById(_ id: String) {
        habitLogs.removeAll { $0.id == id }
        saveHabitLogs()
    }

    func syncDirtySessionRecords() -> [SessionRecordRow] { sessionRecords.filter(\.dirty) }
    func syncSessionById(_ id: String) -> SessionRecordRow? { sessionRecords.first { $0.id == id } }
    func syncSessionByLegacyId(_ legacyId: Int64) -> SessionRecordRow? {
        sessionRecords.first { $0.legacyId == legacyId }
    }
    func syncUpsertSessionRecords(_ items: [SessionRecordRow]) {
        for item in items {
            if let index = sessionRecords.firstIndex(where: { $0.id == item.id }) {
                sessionRecords[index] = item
            } else {
                sessionRecords.append(item)
            }
        }
        saveSessionRecords()
    }
    func syncClearSessionDirtyIfUnchanged(id: String, snapshotModifiedAt: Int64) {
        guard let index = sessionRecords.firstIndex(where: { $0.id == id }),
              sessionRecords[index].locallyModifiedAt == snapshotModifiedAt else { return }
        sessionRecords[index].dirty = false
        saveSessionRecords()
    }
    func syncDeleteSessionById(_ id: String) {
        sessionRecords.removeAll { $0.id == id }
        saveSessionRecords()
    }

    func syncDirtyDailyStamps() -> [DailyStampRecord] { dailyStamps.filter(\.dirty) }
    func syncDailyStampById(_ id: String) -> DailyStampRecord? { dailyStamps.first { $0.id == id } }
    func syncDailyStampByDate(_ stampDate: String) -> DailyStampRecord? {
        dailyStamps.first { $0.stampDateIso == stampDate }
    }
    func syncUpsertDailyStamps(_ items: [DailyStampRecord]) {
        for item in items {
            if let index = dailyStamps.firstIndex(where: { $0.id == item.id }) {
                dailyStamps[index] = item
            } else {
                dailyStamps.append(item)
            }
        }
        saveDailyStamps()
    }
    func syncClearDailyStampDirtyIfUnchanged(id: String, snapshotModifiedAt: Int64) {
        guard let index = dailyStamps.firstIndex(where: { $0.id == id }),
              dailyStamps[index].locallyModifiedAt == snapshotModifiedAt else { return }
        dailyStamps[index].dirty = false
        saveDailyStamps()
    }
    func syncDeleteDailyStampById(_ id: String) {
        dailyStamps.removeAll { $0.id == id }
        saveDailyStamps()
    }

    func syncProfileRecord() -> ProfileRecord? { profile }
    func syncUpsertProfile(_ record: ProfileRecord) {
        profile = record
        saveProfile()
    }
    func syncClearProfileDirtyIfUnchanged(snapshotModifiedAt: Int64) {
        guard profile?.locallyModifiedAt == snapshotModifiedAt else { return }
        profile?.dirty = false
        saveProfile()
    }

    // MARK: - Private

    private func notifyWrite() {
        localWrites.send(())
    }

    private func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }

    private func categoryNamesById() -> [String: String] {
        Dictionary(
            uniqueKeysWithValues: categories.filter { !$0.deleted }.map { ($0.id, $0.name) }
        )
    }

    private func categoryIdLookup(in categories: [CategoryRecord]) -> [String: String] {
        Dictionary(uniqueKeysWithValues: categories.filter { !$0.deleted }.map { ($0.name, $0.id) })
    }

    private func resolveCategoryId(
        name: String?,
        categories: inout [CategoryRecord],
        categoryIdByName: inout [String: String],
        now: Int64
    ) -> String? {
        guard let trimmed = name?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else {
            return nil
        }
        if let existing = categoryIdByName[trimmed] { return existing }
        let syncId = UUID().uuidString.lowercased()
        let legacyId = allocateNextCategoryId()
        categories.append(
            CategoryRecord(
                id: syncId,
                legacyId: legacyId,
                name: trimmed,
                colorArgb: nil,
                dirty: true,
                locallyModifiedAt: now,
                deleted: false
            )
        )
        categoryIdByName[trimmed] = syncId
        return syncId
    }

    private func mergeLogRows(_ logs: inout [HabitLogRecord], habitId: String, rows: [HabitLogRecord]) {
        logs.removeAll { $0.habitId == habitId }
        logs.append(contentsOf: rows)
    }

    private func allocateNextTaskId() -> Int { allocateCounter(key: SyncEntityKeys.nextTaskId, start: 1) }
    private func allocateNextCategoryId() -> Int { allocateCounter(key: SyncEntityKeys.nextCategoryId, start: 1) }
    private func allocateNextHabitId() -> Int { allocateCounter(key: SyncEntityKeys.nextHabitId, start: 1) }
    private func allocateNextSessionId() -> Int64 { Int64(allocateCounter(key: SyncEntityKeys.nextSessionId, start: 1)) }

    private func allocateCounter(key: String, start: Int) -> Int {
        let current = store.data(forKey: key).flatMap { try? decoder.decode(Int.self, from: $0) } ?? (start - 1)
        let next = max(current, start - 1) + 1
        if let data = try? encoder.encode(next) { store.set(data, forKey: key) }
        return next
    }

    private func allocateLegacyId(key: String) -> Int {
        let current = store.data(forKey: key).flatMap { try? decoder.decode(Int.self, from: $0) }
            ?? (SyncEntityKeys.legacyIdFloor - 1)
        let next = max(current, SyncEntityKeys.legacyIdFloor - 1) + 1
        if let data = try? encoder.encode(next) { store.set(data, forKey: key) }
        return next
    }

    // MARK: Persistence

    private func loadAll() {
        categories = loadArray(CategoryRecord.self, key: SyncEntityKeys.categories)
        tasks = loadArray(TaskRecord.self, key: SyncEntityKeys.tasks)
        habits = loadArray(HabitRecord.self, key: SyncEntityKeys.habits)
        habitLogs = loadArray(HabitLogRecord.self, key: SyncEntityKeys.habitLogs)
        sessionRecords = loadArray(SessionRecordRow.self, key: SyncEntityKeys.sessionRecords)
        dailyStamps = loadArray(DailyStampRecord.self, key: SyncEntityKeys.dailyStamps)
        profile = loadOptional(ProfileRecord.self, key: SyncEntityKeys.profile)
    }

    private func saveAll() {
        saveCategories()
        saveTasks()
        saveHabits()
        saveHabitLogs()
        saveSessionRecords()
        saveDailyStamps()
        saveProfile()
    }

    private func loadArray<T: Decodable>(_ type: T.Type, key: String) -> [T] {
        guard let data = store.data(forKey: key),
              let decoded = try? decoder.decode([T].self, from: data) else { return [] }
        return decoded
    }

    private func loadOptional<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = store.data(forKey: key),
              let decoded = try? decoder.decode(T.self, from: data) else { return nil }
        return decoded
    }

    private func save<T: Encodable>(_ value: T, key: String) {
        if let data = try? encoder.encode(value) {
            store.set(data, forKey: key)
        }
    }

    private func saveCategories() { save(categories, key: SyncEntityKeys.categories) }
    private func saveTasks() { save(tasks, key: SyncEntityKeys.tasks) }
    private func saveHabits() { save(habits, key: SyncEntityKeys.habits) }
    private func saveHabitLogs() { save(habitLogs, key: SyncEntityKeys.habitLogs) }
    private func saveSessionRecords() { save(sessionRecords, key: SyncEntityKeys.sessionRecords) }
    private func saveDailyStamps() { save(dailyStamps, key: SyncEntityKeys.dailyStamps) }
    private func saveProfile() { if let profile { save(profile, key: SyncEntityKeys.profile) } }

    private func migrateLegacyIfNeeded() {
        guard !store.bool(forKey: SyncEntityKeys.migrated) else { return }
        let hasSyncData = !tasks.isEmpty || !habits.isEmpty || !sessionRecords.isEmpty || !dailyStamps.isEmpty
        if hasSyncData {
            store.set(true, forKey: SyncEntityKeys.migrated)
            return
        }

        let now = nowMillis()
        if let data = store.data(forKey: "purrodoro.tasks"),
           let legacyTasks = try? decoder.decode([TaskItem].self, from: data) {
            setTaskItems(legacyTasks, focusMinutes: loadLegacyTimerSettings().focusMinutes)
        }
        if let data = store.data(forKey: "purrodoro.habits"),
           let legacyHabits = try? decoder.decode([HabitItem].self, from: data) {
            setHabitItems(legacyHabits)
        }
        if let data = store.data(forKey: "purrodoro.sessionHistory"),
           let legacyRecords = try? decoder.decode([FocusSessionRecord].self, from: data) {
            setFocusSessionRecords(legacyRecords, timerSettings: loadLegacyTimerSettings())
        }
        if let stamps = store.dictionary(forKey: "purrodoro.focusStamps") as? [String: Int] {
            applyRestoredStamps(stamps)
        }
        let name = store.string(forKey: "purrodoro.userName") ?? ""
        updateProfile(displayName: name, timerSettings: loadLegacyTimerSettings())
        store.set(true, forKey: SyncEntityKeys.migrated)
    }

    private func loadLegacyTimerSettings() -> TimerSettings {
        guard let data = store.data(forKey: "purrodoro.settings"),
              let decoded = try? decoder.decode(TimerSettings.self, from: data) else {
            return TimerSettings()
        }
        return decoded
    }
}

// MARK: - CheckpointStore

extension SyncRepository: CheckpointStore {
    func get(table: String) async -> String? {
        guard let data = store.data(forKey: SyncEntityKeys.checkpoints),
              let map = try? decoder.decode([String: String].self, from: data) else { return nil }
        return map[table]
    }

    func set(table: String, iso: String) async {
        var map: [String: String] = [:]
        if let data = store.data(forKey: SyncEntityKeys.checkpoints),
           let existing = try? decoder.decode([String: String].self, from: data) {
            map = existing
        }
        map[table] = iso
        if let data = try? encoder.encode(map) {
            store.set(data, forKey: SyncEntityKeys.checkpoints)
        }
    }
}
