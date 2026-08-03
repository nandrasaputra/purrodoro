//
//  HabitDisplayModels.swift
//  Purrodoro
//
//  Pure display helpers for Habit detail — week strip + monthly progress dots
//  (Android `habitWeekDots` / `monthlyHabitProgressDots`) and best-streak math
//  (`Habit.bestStreak`).
//

import Foundation

struct HabitWeekDot: Equatable {
    let date: Date
    let label: String
    let isScheduled: Bool
    let isCompleted: Bool
    let isToday: Bool
}

struct HabitMonthProgressDot: Equatable {
    let date: Date
    let isCompleted: Bool
}

enum HabitDisplay {
    /// Last 7 calendar days ending today (oldest → newest), matching Android
    /// `habitWeekDots`.
    static func weekDots(
        habit: HabitItem,
        today: Date = Date(),
        calendar: Calendar = Journey.calendar
    ) -> [HabitWeekDot] {
        let start = calendar.startOfDay(for: today)
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = .current
        formatter.dateFormat = "EEEEE"
        return (0..<7).reversed().compactMap { offset -> HabitWeekDot? in
            guard let date = calendar.date(byAdding: .day, value: -offset, to: start) else { return nil }
            return HabitWeekDot(
                date: date,
                label: formatter.string(from: date),
                isScheduled: habit.isScheduled(on: date),
                isCompleted: habit.meetsTarget(on: date),
                isToday: calendar.isDate(date, inSameDayAs: start)
            )
        }
    }

    /// One binary completion dot per day in the month containing `monthDate`
    /// (Android `monthlyHabitProgressDots` for a single habit).
    static func monthlyProgressDots(
        habit: HabitItem,
        monthDate: Date = Date(),
        calendar: Calendar = Journey.calendar
    ) -> [HabitMonthProgressDot] {
        let comps = calendar.dateComponents([.year, .month], from: monthDate)
        guard let monthStart = calendar.date(from: comps),
              let dayRange = calendar.range(of: .day, in: .month, for: monthStart) else {
            return []
        }
        return dayRange.compactMap { day -> HabitMonthProgressDot? in
            guard let date = calendar.date(byAdding: .day, value: day - 1, to: monthStart) else {
                return nil
            }
            return HabitMonthProgressDot(
                date: calendar.startOfDay(for: date),
                isCompleted: habit.meetsTarget(on: date)
            )
        }
    }

    /// Leading blank cells before day 1 so the month grid starts on Monday
    /// (Android habit month uses Mon–Sun; `DayOfWeek.value` is Mon=1 … Sun=7).
    static func mondayLeadingBlanks(
        forFirstDay firstDay: Date,
        calendar: Calendar = Journey.calendar
    ) -> Int {
        // Convert Apple weekday (Sun=1…Sat=7) → Mon=0…Sun=6.
        let weekday = calendar.component(.weekday, from: firstDay)
        return (weekday + 5) % 7
    }

    /// Mon–Sun narrow weekday labels for the habit monthly card.
    static func mondayWeekdaySymbols(locale: Locale = .current) -> [String] {
        var cal = Calendar(identifier: .gregorian)
        cal.locale = locale
        cal.firstWeekday = 2
        let symbols = cal.veryShortStandaloneWeekdaySymbols
        let first = cal.firstWeekday - 1
        return Array(symbols[first...] + symbols[..<first])
    }
}

extension HabitItem {
    /// Longest run of completed scheduled days across the habit's lifetime.
    /// Unscheduled and rest days are transparent (Android `Habit.bestStreak`).
    func bestStreak(asOf today: Date = Date(), calendar: Calendar = Journey.calendar) -> Int {
        guard !log.isEmpty else { return 0 }
        let todayStart = calendar.startOfDay(for: today)
        let logDates = log.keys.compactMap(Journey.date(fromKey:)).map { calendar.startOfDay(for: $0) }
        guard let earliest = logDates.min() else { return 0 }

        var cursor = earliest
        var run = 0
        var best = 0
        var guardCount = 0
        while cursor <= todayStart && guardCount < 4000 {
            guardCount += 1
            if isScheduled(on: cursor) {
                if meetsTarget(on: cursor) {
                    run += 1
                    best = max(best, run)
                } else {
                    run = 0
                }
            }
            guard let next = calendar.date(byAdding: .day, value: 1, to: cursor) else { break }
            cursor = next
        }
        return best
    }
}
