//
//  SharedStore.swift
//  Purrodoro (shared between the app and the PurrodoroWidgets extension)
//
//  A tiny App Group-backed snapshot of "today" so the Home Screen widget can
//  render the same focus count / minutes the app shows. The app writes after
//  every focus credit; the widget reads on each timeline refresh.
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
}

struct TodaySnapshot: Equatable {
    var count: Int
    var minutes: Int
    var streak: Int
}
