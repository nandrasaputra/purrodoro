//
//  HabitDisplayModels.swift
//  Purrodoro
//
//  Pure display helpers for habit week strips and monthly progress grids —
//  port of Android `TaskHabitDisplayModels.kt` (`habitWeekDots`,
//  `monthlyHabitProgressDots`).
//

import Foundation

struct HabitWeekDot: Equatable, Identifiable {
    var id: String { Journey.dayKey(date) }
    let label: String
    let date: Date
    let isScheduled: Bool
    let isCompleted: Bool
    let isToday: Bool
}

struct HabitMonthProgressDot: Equatable, Identifiable {
    var id: String { Journey.dayKey(date) }
    let date: Date
    let isCompleted: Bool
}

enum HabitDisplay {
    /// Last 7 days ending on `today` (Android `habitWeekDots`).
    static func weekDots(for habit: HabitItem, today: Date = Date()) -> [HabitWeekDot] {
        let cal = Journey.calendar
        let start = cal.startOfDay(for: today)
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEEE"
        return (0..<7).reversed().compactMap { offset -> HabitWeekDot? in
            guard let date = cal.date(byAdding: .day, value: -offset, to: start) else { return nil }
            return HabitWeekDot(
                label: formatter.string(from: date),
                date: date,
                isScheduled: habit.isScheduled(on: date),
                isCompleted: habit.meetsTarget(on: date),
                isToday: cal.isDate(date, inSameDayAs: start)
            )
        }
    }

    /// One binary completion slot per day of the month containing `monthDate`
    /// (Android `monthlyHabitProgressDots`). When multiple habits are passed,
    /// a day is complete if any habit met its target that day.
    static func monthlyProgressDots(
        habits: [HabitItem],
        monthDate: Date = Date()
    ) -> [HabitMonthProgressDot] {
        let cal = Journey.calendar
        let comps = cal.dateComponents([.year, .month], from: monthDate)
        guard let monthStart = cal.date(from: comps),
              let range = cal.range(of: .day, in: .month, for: monthStart) else { return [] }
        return range.compactMap { day -> HabitMonthProgressDot? in
            guard let date = cal.date(byAdding: .day, value: day - 1, to: monthStart) else { return nil }
            let done = habits.contains { $0.meetsTarget(on: date) }
            return HabitMonthProgressDot(date: date, isCompleted: done)
        }
    }

    /// Monday-first leading blank count for the habit month grid (Android parity:
    /// `DayOfWeek.value - 1`, where Monday = 1).
    static func mondayFirstLeadingBlanks(for monthDate: Date) -> Int {
        let cal = Calendar(identifier: .gregorian)
        let comps = cal.dateComponents([.year, .month], from: monthDate)
        guard let start = cal.date(from: comps) else { return 0 }
        // Calendar weekday: 1 = Sunday … 7 = Saturday. Convert to Mon=0 … Sun=6.
        let weekday = cal.component(.weekday, from: start)
        return (weekday + 5) % 7
    }

    /// Hardcoded Mon-first headers matching Android `KuroMonthlyHabitProgressCard`.
    static let mondayFirstWeekdayHeaders = ["M", "T", "W", "T", "F", "S", "S"]
}
