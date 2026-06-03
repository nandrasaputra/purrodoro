//
//  JourneyModels.swift
//  Purrodoro
//
//  Pure helpers for the Journey tab: day keys (matching the engine's stamp
//  format), streak calculation (ported from the Android `calculateStreak`),
//  and month-grid construction for the calendar heatmap.
//

import Foundation

enum Journey {
    /// Calendar used for all Journey math. Uses the user's locale but a fixed
    /// key format so it lines up with the engine's persisted stamps.
    static var calendar: Calendar { Calendar.current }

    static func dayKey(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: date)
    }

    static func count(_ stamps: [String: Int], on date: Date) -> Int {
        stamps[dayKey(date)] ?? 0
    }

    /// Consecutive days (ending today) with at least one focus session.
    /// If today has no stamp yet, we count backwards from yesterday so an
    /// in-progress day doesn't reset a legitimate streak.
    static func currentStreak(_ stamps: [String: Int], today: Date = Date()) -> Int {
        let cal = calendar
        let todayHasStamp = count(stamps, on: today) > 0
        var streak = todayHasStamp ? 1 : 0
        guard var date = cal.date(byAdding: .day, value: -1, to: today) else { return streak }
        while count(stamps, on: date) > 0 {
            streak += 1
            guard let prev = cal.date(byAdding: .day, value: -1, to: date) else { break }
            date = prev
        }
        return streak
    }

    /// The longest run of consecutive stamped days across all recorded history.
    static func bestStreak(_ stamps: [String: Int]) -> Int {
        let cal = calendar
        let days = stamps.filter { $0.value > 0 }.keys.compactMap(date(fromKey:))
            .map { cal.startOfDay(for: $0) }
            .sorted()
        guard !days.isEmpty else { return 0 }
        var best = 1
        var run = 1
        for i in 1..<days.count {
            if let next = cal.date(byAdding: .day, value: 1, to: days[i - 1]),
               cal.isDate(next, inSameDayAs: days[i]) {
                run += 1
            } else if !cal.isDate(days[i - 1], inSameDayAs: days[i]) {
                run = 1
            }
            best = max(best, run)
        }
        return best
    }

    static func date(fromKey key: String) -> Date? {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.date(from: key)
    }

    static func total(_ stamps: [String: Int]) -> Int {
        stamps.values.reduce(0, +)
    }

    /// Sum of values whose day falls within the last `days` days (inclusive of today).
    static func sum(_ map: [String: Int], lastDays days: Int, today: Date = Date()) -> Int {
        let cal = calendar
        guard let start = cal.date(byAdding: .day, value: -(days - 1), to: cal.startOfDay(for: today)) else { return 0 }
        return map.reduce(0) { acc, entry in
            guard let d = date(fromKey: entry.key) else { return acc }
            return d >= start ? acc + entry.value : acc
        }
    }
}

/// A single calendar cell. `date == nil` for leading/trailing padding.
struct MonthDay: Identifiable {
    let id: Int
    let date: Date?
}

struct MonthGrid {
    let monthStart: Date
    let days: [MonthDay]
    let title: String

    init(month: Date) {
        let cal = Journey.calendar
        let comps = cal.dateComponents([.year, .month], from: month)
        let start = cal.date(from: comps) ?? month
        self.monthStart = start

        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        self.title = f.string(from: start)

        let range = cal.range(of: .day, in: .month, for: start) ?? (1..<29)
        let weekday = cal.component(.weekday, from: start)
        // Leading blanks before the 1st (respects locale's first weekday).
        let lead = (weekday - cal.firstWeekday + 7) % 7

        var cells: [MonthDay] = []
        var idx = 0
        for _ in 0..<lead {
            cells.append(MonthDay(id: idx, date: nil)); idx += 1
        }
        for day in range {
            if let d = cal.date(byAdding: .day, value: day - 1, to: start) {
                cells.append(MonthDay(id: idx, date: d)); idx += 1
            }
        }
        // Trailing blanks to complete the final week row.
        while cells.count % 7 != 0 {
            cells.append(MonthDay(id: idx, date: nil)); idx += 1
        }
        self.days = cells
    }

    var weekdaySymbols: [String] {
        let cal = Journey.calendar
        let symbols = cal.veryShortStandaloneWeekdaySymbols
        let first = cal.firstWeekday - 1
        return Array(symbols[first...] + symbols[..<first])
    }
}
