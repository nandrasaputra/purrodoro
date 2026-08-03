//
//  JourneyModels.swift
//  Purrodoro
//
//  Pure helpers for the Journey tab: day keys (matching the engine's stamp
//  format), streak calculation (ported from the Android `calculateStreak`),
//  month-grid construction, and stacked period summaries (today / week /
//  month / all-time) ported from Android `JourneyModels.kt`.
//

import Foundation

enum Journey {
    /// Calendar used for all Journey math. Uses the user's locale but a fixed
    /// key format so it lines up with the engine's persisted stamps.
    static var calendar: Calendar { Calendar.current }

    /// Monday-first Gregorian calendar used for week bars (Android Journey
    /// always starts the week on Monday).
    static var mondayCalendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = calendar.timeZone
        cal.locale = calendar.locale
        cal.firstWeekday = 2
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

    // MARK: Period summaries (Android JourneyModels.kt)

    /// Groups focus segments by the calendar day they started on.
    static func recordsByStartedDay(
        _ records: [FocusSessionRecord],
        calendar: Calendar = calendar
    ) -> [Date: [FocusSessionRecord]] {
        Dictionary(grouping: records) { History.startedDay(for: $0, calendar: calendar) }
    }

    static func focusMinutes(in records: [FocusSessionRecord]) -> Int {
        records.reduce(0) { $0 + max($1.minutes, 0) }
    }

    /// Start of the Monday-based week containing `date`.
    static func mondayWeekStart(containing date: Date, calendar: Calendar = mondayCalendar) -> Date {
        let day = calendar.startOfDay(for: date)
        let weekday = calendar.component(.weekday, from: day)
        // Apple: Sun=1 … Sat=7. Monday-first offset: Mon→0 … Sun→6.
        let daysFromMonday = (weekday + 5) % 7
        return calendar.date(byAdding: .day, value: -daysFromMonday, to: day) ?? day
    }

    static func journeySummary(
        records: [FocusSessionRecord],
        today: Date = Date(),
        calendar: Calendar = mondayCalendar,
        weeklyGoalMinutes: Int = 12 * 60
    ) -> JourneyPeriodSummary {
        let todayStart = calendar.startOfDay(for: today)
        let weekStart = mondayWeekStart(containing: todayStart, calendar: calendar)
        let previousWeekStart = calendar.date(byAdding: .day, value: -7, to: weekStart) ?? weekStart
        let byDay = recordsByStartedDay(records, calendar: calendar)

        let weekBars: [JourneyDayBar] = (0..<7).compactMap { offset in
            guard let date = calendar.date(byAdding: .day, value: offset, to: weekStart) else { return nil }
            let dayRecords = byDay[calendar.startOfDay(for: date)] ?? []
            return JourneyDayBar(
                date: calendar.startOfDay(for: date),
                dayLabel: shortWeekdayLabel(date, calendar: calendar),
                focusMinutes: focusMinutes(in: dayRecords)
            )
        }

        let weekFocusMinutes = weekBars.reduce(0) { $0 + $1.focusMinutes }
        let previousWeekFocusMinutes = (0..<7).reduce(0) { acc, offset in
            guard let date = calendar.date(byAdding: .day, value: offset, to: previousWeekStart) else { return acc }
            return acc + focusMinutes(in: byDay[calendar.startOfDay(for: date)] ?? [])
        }
        let focusDays = weekBars.filter { $0.focusMinutes > 0 }.count
        let dailyAverageMinutes = focusDays == 0 ? 0 : weekFocusMinutes / focusDays
        let weeklyGoalPercent: Int = {
            guard weeklyGoalMinutes > 0 else { return 0 }
            return Int((Double(weekFocusMinutes) / Double(weeklyGoalMinutes) * 100).rounded())
        }()
        let weekOverWeekPercent: Int? = {
            guard previousWeekFocusMinutes > 0 else { return nil }
            return Int(
                (Double(weekFocusMinutes - previousWeekFocusMinutes) / Double(previousWeekFocusMinutes) * 100)
                    .rounded()
            )
        }()

        let todayRecords = (byDay[todayStart] ?? []).sorted { $0.endedAt > $1.endedAt }

        return JourneyPeriodSummary(
            weekFocusMinutes: weekFocusMinutes,
            previousWeekFocusMinutes: previousWeekFocusMinutes,
            weekOverWeekPercent: weekOverWeekPercent,
            focusDays: focusDays,
            dailyAverageMinutes: dailyAverageMinutes,
            weeklyGoalMinutes: weeklyGoalMinutes,
            weeklyGoalPercent: min(max(weeklyGoalPercent, 0), 999),
            weekBars: weekBars,
            todayFocusSessions: todayRecords.count,
            todayRows: todayRecords
        )
    }

    static func monthSummary(
        records: [FocusSessionRecord],
        month: Date = Date(),
        calendar: Calendar = calendar
    ) -> JourneyMonthSummary {
        let comps = calendar.dateComponents([.year, .month], from: month)
        guard let monthStart = calendar.date(from: comps),
              let dayRange = calendar.range(of: .day, in: .month, for: monthStart) else {
            return JourneyMonthSummary(focusDays: 0, focusMinutes: 0)
        }
        let byDay = recordsByStartedDay(records, calendar: calendar)
        var focusMinutesTotal = 0
        var focusDays = 0
        for day in dayRange {
            guard let date = calendar.date(byAdding: .day, value: day - 1, to: monthStart) else { continue }
            let minutes = focusMinutes(in: byDay[calendar.startOfDay(for: date)] ?? [])
            if minutes > 0 {
                focusDays += 1
                focusMinutesTotal += minutes
            }
        }
        return JourneyMonthSummary(focusDays: focusDays, focusMinutes: focusMinutesTotal)
    }

    /// Longest consecutive focused days inside `month` from intensity map.
    static func monthBestStreakDays(
        intensities: [Date: Int],
        month: Date,
        calendar: Calendar = calendar
    ) -> Int {
        let comps = calendar.dateComponents([.year, .month], from: month)
        guard let monthStart = calendar.date(from: comps) else { return 0 }
        let days = intensities
            .filter { $0.value > 0 && calendar.isDate($0.key, equalTo: monthStart, toGranularity: .month) }
            .map { calendar.startOfDay(for: $0.key) }
            .sorted()
        return longestConsecutiveRun(days, calendar: calendar)
    }

    static func allTimeSummary(
        records: [FocusSessionRecord],
        calendar: Calendar = calendar
    ) -> JourneyAllTimeSummary {
        let totalFocusMinutes = focusMinutes(in: records)
        let byDay = recordsByStartedDay(records, calendar: calendar)
        let focusDates = byDay
            .filter { focusMinutes(in: $0.value) > 0 }
            .map { calendar.startOfDay(for: $0.key) }
            .sorted()
        return JourneyAllTimeSummary(
            totalFocusMinutes: totalFocusMinutes,
            bestStreakDays: longestConsecutiveRun(focusDates, calendar: calendar),
            totalSessions: records.count
        )
    }

    /// Y-axis scale for the weekly bar chart (Android `journeyChartScale`).
    static func chartScale(maxDayMinutes: Int) -> JourneyChartScale {
        let top = niceAxisTopMinutes(max(maxDayMinutes, 0))
        return JourneyChartScale(topMinutes: top, midMinutes: top / 2)
    }

    static func allTimeFocusLabel(_ minutes: Int) -> String {
        let safe = max(minutes, 0)
        if safe >= 60 { return "\(safe / 60)h" }
        return "\(safe)m"
    }

    private static func longestConsecutiveRun(_ sortedDates: [Date], calendar: Calendar) -> Int {
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
        if let hit = steps.first(where: { $0 >= maxMinutes }) { return hit }
        return ((maxMinutes + 59) / 60) * 60
    }

    private static func shortWeekdayLabel(_ date: Date, calendar: Calendar) -> String {
        let f = DateFormatter()
        f.calendar = calendar
        f.locale = .current
        f.dateFormat = "EEE"
        return f.string(from: date)
    }
}

// MARK: - Period summary types

struct JourneyDayBar: Equatable {
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
    let bestStreakDays: Int
    let totalSessions: Int

    var totalFocusLabel: String { Journey.allTimeFocusLabel(totalFocusMinutes) }
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

    init(month: Date, mondayFirst: Bool = false) {
        var cal = Journey.calendar
        if mondayFirst {
            cal = Journey.mondayCalendar
        }
        let comps = cal.dateComponents([.year, .month], from: month)
        let start = cal.date(from: comps) ?? month
        self.monthStart = start

        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        self.title = f.string(from: start)

        let range = cal.range(of: .day, in: .month, for: start) ?? (1..<29)
        let weekday = cal.component(.weekday, from: start)
        let lead: Int
        if mondayFirst {
            lead = (weekday + 5) % 7
        } else {
            lead = (weekday - cal.firstWeekday + 7) % 7
        }

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

    /// Mon–Sun symbols when the grid is Monday-first.
    var mondayWeekdaySymbols: [String] {
        HabitDisplay.mondayWeekdaySymbols()
    }
}
