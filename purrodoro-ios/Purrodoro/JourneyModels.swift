//
//  JourneyModels.swift
//  Purrodoro
//
//  Pure helpers for the Journey tab: day keys (matching the engine's stamp
//  format), streak calculation (ported from the Android `calculateStreak`),
//  month-grid construction, and Android CalendarScreen period summaries
//  (`journeySummary` / `monthSummary` / `allTimeSummary` / `journeyChartScale`).
//

import Foundation

enum Journey {
    /// Calendar used for all Journey math. Uses the user's locale but a fixed
    /// key format so it lines up with the engine's persisted stamps.
    static var calendar: Calendar { Calendar.current }

    /// Monday-first gregorian calendar matching Android Journey week/month grids.
    static var mondayCalendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.firstWeekday = 2 // Monday
        cal.timeZone = calendar.timeZone
        return cal
    }

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

    // MARK: - Period summaries (Android CalendarScreen)

    /// Monday of the week containing `date` (Android `today.with(MONDAY)`).
    static func mondayWeekStart(containing date: Date, calendar: Calendar = mondayCalendar) -> Date {
        let start = calendar.startOfDay(for: date)
        let weekday = calendar.component(.weekday, from: start) // 1=Sun … 7=Sat
        let daysFromMonday = (weekday + 5) % 7
        return calendar.date(byAdding: .day, value: -daysFromMonday, to: start) ?? start
    }

    /// Port of Android `journeySummary` over per-segment `FocusSessionRecord`s.
    static func summary(
        records: [FocusSessionRecord],
        today: Date = Date(),
        calendar: Calendar = mondayCalendar,
        weeklyGoalMinutes: Int = 12 * 60
    ) -> JourneyPeriodSummary {
        let todayStart = calendar.startOfDay(for: today)
        let weekStart = mondayWeekStart(containing: todayStart, calendar: calendar)
        let previousWeekStart = calendar.date(byAdding: .day, value: -7, to: weekStart) ?? weekStart
        let byDate = groupMinutesAndRecords(records, calendar: calendar)

        let dayFormatter = DateFormatter()
        dayFormatter.dateFormat = "EEE"
        dayFormatter.calendar = calendar
        dayFormatter.locale = .current

        let weekBars: [JourneyDayBar] = (0..<7).compactMap { offset in
            guard let date = calendar.date(byAdding: .day, value: offset, to: weekStart) else { return nil }
            let day = calendar.startOfDay(for: date)
            return JourneyDayBar(
                date: day,
                dayLabel: dayFormatter.string(from: day),
                focusMinutes: byDate[day]?.minutes ?? 0
            )
        }
        let weekFocusMinutes = weekBars.reduce(0) { $0 + $1.focusMinutes }
        let previousWeekFocusMinutes = (0..<7).reduce(0) { acc, offset in
            guard let date = calendar.date(byAdding: .day, value: offset, to: previousWeekStart) else { return acc }
            return acc + (byDate[calendar.startOfDay(for: date)]?.minutes ?? 0)
        }
        let focusDays = weekBars.filter { $0.focusMinutes > 0 }.count
        let dailyAverageMinutes = focusDays == 0 ? 0 : weekFocusMinutes / focusDays
        let weeklyGoalPercent: Int = {
            guard weeklyGoalMinutes > 0 else { return 0 }
            let raw = Int((Double(weekFocusMinutes) / Double(weeklyGoalMinutes) * 100).rounded())
            return min(max(raw, 0), 999)
        }()
        let weekOverWeekPercent: Int? = {
            guard previousWeekFocusMinutes > 0 else { return nil }
            return Int(
                (Double(weekFocusMinutes - previousWeekFocusMinutes)
                    / Double(previousWeekFocusMinutes) * 100).rounded()
            )
        }()

        let todayRows = (byDate[todayStart]?.records ?? []).sorted { $0.endedAt > $1.endedAt }

        return JourneyPeriodSummary(
            weekFocusMinutes: weekFocusMinutes,
            previousWeekFocusMinutes: previousWeekFocusMinutes,
            weekOverWeekPercent: weekOverWeekPercent,
            focusDays: focusDays,
            dailyAverageMinutes: dailyAverageMinutes,
            weeklyGoalMinutes: weeklyGoalMinutes,
            weeklyGoalPercent: weeklyGoalPercent,
            weekBars: weekBars,
            todayFocusSessions: todayRows.count,
            todayRows: todayRows
        )
    }

    /// Port of Android `monthSummary`.
    static func monthSummary(
        records: [FocusSessionRecord],
        month: Date = Date(),
        calendar: Calendar = mondayCalendar
    ) -> JourneyMonthSummary {
        let comps = calendar.dateComponents([.year, .month], from: month)
        guard let monthStart = calendar.date(from: comps),
              let range = calendar.range(of: .day, in: .month, for: monthStart) else {
            return JourneyMonthSummary(focusDays: 0, focusMinutes: 0)
        }
        let byDate = groupMinutesAndRecords(records, calendar: calendar)
        var focusMinutes = 0
        var focusDays = 0
        for day in range {
            guard let date = calendar.date(byAdding: .day, value: day - 1, to: monthStart) else { continue }
            let mins = byDate[calendar.startOfDay(for: date)]?.minutes ?? 0
            if mins > 0 {
                focusDays += 1
                focusMinutes += mins
            }
        }
        return JourneyMonthSummary(focusDays: focusDays, focusMinutes: focusMinutes)
    }

    /// Port of Android `allTimeSummary`.
    static func allTimeSummary(
        records: [FocusSessionRecord],
        calendar: Calendar = mondayCalendar
    ) -> JourneyAllTimeSummary {
        let totalFocusMinutes = records.reduce(0) { $0 + max($1.minutes, 0) }
        let byDate = groupMinutesAndRecords(records, calendar: calendar)
        let focusDates = byDate
            .filter { $0.value.minutes > 0 }
            .map(\.key)
            .sorted()
        return JourneyAllTimeSummary(
            totalFocusMinutes: totalFocusMinutes,
            totalFocusLabel: compactHoursMinutesLabel(totalFocusMinutes),
            bestStreakDays: longestConsecutiveRun(focusDates, calendar: calendar),
            totalSessions: records.count
        )
    }

    /// Longest consecutive focused days inside `month` (Android `monthBestStreakDays`).
    static func monthBestStreakDays(
        intensities: [Date: Int],
        month: Date,
        calendar: Calendar = mondayCalendar
    ) -> Int {
        let days = intensities
            .filter { $0.value > 0 }
            .keys
            .filter { calendar.isDate($0, equalTo: month, toGranularity: .month) }
            .map { calendar.startOfDay(for: $0) }
            .sorted()
        return longestConsecutiveRun(days, calendar: calendar)
    }

    /// Port of Android `journeyChartScale`.
    static func chartScale(maxDayMinutes: Int) -> JourneyChartScale {
        let top = niceAxisTopMinutes(max(maxDayMinutes, 0))
        return JourneyChartScale(topMinutes: top, midMinutes: top / 2)
    }

    /// Android all-time label: `"59m"` / `"1h"` (no mixed hours+minutes).
    static func compactHoursMinutesLabel(_ minutes: Int) -> String {
        let safe = max(minutes, 0)
        if safe >= 60 { return "\(safe / 60)h" }
        return "\(safe)m"
    }

    static func longestConsecutiveRun(_ sortedDates: [Date], calendar: Calendar = mondayCalendar) -> Int {
        guard !sortedDates.isEmpty else { return 0 }
        var best = 1
        var current = 1
        for i in 1..<sortedDates.count {
            if let next = calendar.date(byAdding: .day, value: 1, to: sortedDates[i - 1]),
               calendar.isDate(next, inSameDayAs: sortedDates[i]) {
                current += 1
            } else if !calendar.isDate(sortedDates[i - 1], inSameDayAs: sortedDates[i]) {
                current = 1
            }
            best = max(best, current)
        }
        return best
    }

    private static func niceAxisTopMinutes(_ maxMinutes: Int) -> Int {
        if maxMinutes <= 0 { return 60 }
        let steps = [15, 30, 45, 60, 90, 120, 180, 240, 300, 360, 420, 480, 540, 600, 660, 720]
        if let step = steps.first(where: { $0 >= maxMinutes }) { return step }
        return ((maxMinutes + 59) / 60) * 60
    }

    private static func groupMinutesAndRecords(
        _ records: [FocusSessionRecord],
        calendar: Calendar
    ) -> [Date: (minutes: Int, records: [FocusSessionRecord])] {
        var result: [Date: (minutes: Int, records: [FocusSessionRecord])] = [:]
        for record in records {
            let day = History.startedDay(for: record, calendar: calendar)
            let existing = result[day] ?? (minutes: 0, records: [])
            result[day] = (
                minutes: existing.minutes + max(record.minutes, 0),
                records: existing.records + [record]
            )
        }
        return result
    }
}

// MARK: - Period summary types

struct JourneyDayBar: Equatable, Identifiable {
    var id: Date { date }
    let date: Date
    let dayLabel: String
    let focusMinutes: Int
}

struct JourneyPeriodSummary: Equatable {
    let weekFocusMinutes: Int
    let previousWeekFocusMinutes: Int
    let weekOverWeekPercent: Int?
    let focusDays: Int
    let dailyAverageMinutes: Int
    let weeklyGoalMinutes: Int
    let weeklyGoalPercent: Int
    let weekBars: [JourneyDayBar]
    let todayFocusSessions: Int
    let todayRows: [FocusSessionRecord]
}

struct JourneyMonthSummary: Equatable {
    let focusDays: Int
    let focusMinutes: Int
    var focusTimeLabel: String { History.minutesLabel(focusMinutes) }
}

struct JourneyAllTimeSummary: Equatable {
    let totalFocusMinutes: Int
    let totalFocusLabel: String
    let bestStreakDays: Int
    let totalSessions: Int
}

struct JourneyChartScale: Equatable {
    let topMinutes: Int
    let midMinutes: Int
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

    init(month: Date, calendar: Calendar = Journey.calendar) {
        let comps = calendar.dateComponents([.year, .month], from: month)
        let start = calendar.date(from: comps) ?? month
        self.monthStart = start

        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        self.title = f.string(from: start)

        let range = calendar.range(of: .day, in: .month, for: start) ?? (1..<29)
        let weekday = calendar.component(.weekday, from: start)
        // Leading blanks before the 1st (respects calendar's first weekday).
        let lead = (weekday - calendar.firstWeekday + 7) % 7

        var cells: [MonthDay] = []
        var idx = 0
        for _ in 0..<lead {
            cells.append(MonthDay(id: idx, date: nil)); idx += 1
        }
        for day in range {
            if let d = calendar.date(byAdding: .day, value: day - 1, to: start) {
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
