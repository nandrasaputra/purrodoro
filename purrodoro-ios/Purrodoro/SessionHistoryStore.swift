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

    private let store: KeyValueStore
    private let key = "purrodoro.sessionHistory"

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        load()
    }

    /// Append a freshly completed focus session and persist.
    func record(minutes: Int, intentName: String?, endedAt: Date = Date()) {
        let entry = FocusSessionRecord(endedAt: endedAt, minutes: minutes, intentName: intentName)
        records.insert(entry, at: 0)
        if records.count > Self.maxRecords {
            records.removeLast(records.count - Self.maxRecords)
        }
        persist()
    }

    func clear() {
        records.removeAll()
        persist()
    }

    // MARK: Persistence

    private func load() {
        guard let data = store.data(forKey: key),
              let decoded = try? JSONDecoder().decode([FocusSessionRecord].self, from: data)
        else { return }
        records = decoded.sorted { $0.endedAt > $1.endedAt }
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(records) {
            store.set(data, forKey: key)
        }
    }
}
