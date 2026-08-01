//
//  SharedStore.swift
//  Purrodoro (shared between the app and the PurrodoroWidgets extension)
//
//  App Group-backed snapshots so Home Screen widgets can render focus stats
//  and the richer dashboard (habits/tasks/nudge) without importing app stores.
//  The app writes after focus credits and local data changes; widgets read on
//  each timeline refresh.
//

import Foundation

enum SharedStore {
    static let appGroup = "group.com.endiar.Purrodoro"

    private static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroup) ?? .standard
    }

    private enum Key {
        static let count = "today.focusCount"
        static let minutes = "today.focusMinutes"
        static let dayKey = "today.dayKey"
        static let streak = "today.streak"
        static let updatedAt = "today.updatedAt"
        static let dashboard = "widget.dashboard.v1"
    }

    static func dayKey(_ date: Date = Date()) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: date)
    }

    /// Persist today's focus snapshot for the widget.
    static func writeToday(count: Int, minutes: Int, streak: Int, date: Date = Date()) {
        let d = defaults
        d.set(count, forKey: Key.count)
        d.set(minutes, forKey: Key.minutes)
        d.set(streak, forKey: Key.streak)
        d.set(dayKey(date), forKey: Key.dayKey)
        d.set(date, forKey: Key.updatedAt)
    }

    /// Read the snapshot. Stale entries (a previous day) read back as zeros so a
    /// widget shown the next morning starts fresh.
    static func readToday(now: Date = Date()) -> TodaySnapshot {
        let d = defaults
        let storedDay = d.string(forKey: Key.dayKey)
        guard storedDay == dayKey(now) else {
            return TodaySnapshot(count: 0, minutes: 0, streak: d.integer(forKey: Key.streak))
        }
        return TodaySnapshot(
            count: d.integer(forKey: Key.count),
            minutes: d.integer(forKey: Key.minutes),
            streak: d.integer(forKey: Key.streak)
        )
    }

    /// Persist the full dashboard snapshot used by Stats / Plan / Streak / etc.
    static func writeDashboard(_ snapshot: WidgetDashboardSnapshot) {
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        defaults.set(data, forKey: Key.dashboard)
        writeToday(
            count: snapshot.focusToday,
            minutes: snapshot.minutesToday,
            streak: snapshot.streak,
            date: Date()
        )
    }

    /// Read the dashboard snapshot. Falls back to Today fields when missing or
    /// stale so widgets still render after an app update.
    static func readDashboard(now: Date = Date()) -> WidgetDashboardSnapshot {
        let d = defaults
        if let data = d.data(forKey: Key.dashboard),
           let snap = try? JSONDecoder().decode(WidgetDashboardSnapshot.self, from: data),
           snap.dayKey == dayKey(now) {
            return snap
        }
        let today = readToday(now: now)
        var empty = WidgetDashboardSnapshot.empty
        empty.dayKey = dayKey(now)
        empty.focusToday = today.count
        empty.minutesToday = today.minutes
        empty.streak = today.streak
        empty.streakAtRisk = today.streak > 0 && today.count == 0
        return empty
    }
}

struct TodaySnapshot: Equatable {
    var count: Int
    var minutes: Int
    var streak: Int
}
