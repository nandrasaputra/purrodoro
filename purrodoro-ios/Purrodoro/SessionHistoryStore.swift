//
//  SessionHistoryStore.swift
//  Purrodoro
//
//  Persists a rolling log of completed focus sessions (newest first) so the
//  Journey tab can show per-session detail beyond the daily stamp/minute
//  counts. Backed by `UserDefaults` JSON, capped so it never grows unbounded.
//

import Foundation
import Combine

@MainActor
final class SessionHistoryStore: ObservableObject {

    /// Completed focus sessions, newest first.
    @Published private(set) var records: [FocusSessionRecord] = []

    /// Keep history bounded; older entries are dropped past this count.
    static let maxRecords = 500

    private let syncRepo: SyncRepository
    var timerSettingsProvider: () -> TimerSettings = { TimerSettings() }

    init(syncRepo: SyncRepository) {
        self.syncRepo = syncRepo
        load()
    }

    convenience init() {
        self.init(syncRepo: SyncRepository(store: InMemoryKeyValueStore()))
    }

    func record(minutes: Int, intentName: String?, endedAt: Date = Date()) {
        let entry = FocusSessionRecord(endedAt: endedAt, minutes: minutes, intentName: intentName)
        syncRepo.appendFocusSession(
            entry,
            timerSettings: timerSettingsProvider(),
            maxCount: Self.maxRecords
        )
        load()
    }

    func clear() {
        syncRepo.setFocusSessionRecords([], timerSettings: timerSettingsProvider())
        load()
    }

    /// Union imported session rows (backup wins on near-duplicate keys).
    func unionRecords(_ imported: [FocusSessionRecord]) {
        syncRepo.unionFocusSessionRecords(
            imported,
            timerSettings: timerSettingsProvider(),
            maxCount: Self.maxRecords
        )
        load()
    }

    // MARK: Persistence

    private func load() {
        records = syncRepo.focusSessionRecords()
    }

    func reloadFromSync() {
        load()
    }

    private func persist() {
        syncRepo.setFocusSessionRecords(records, timerSettings: timerSettingsProvider())
    }
}
