//
//  HistoryModels.swift
//  Purrodoro
//
//  Pure helpers for the History screen — day drill-down over
//  `FocusSessionRecord`s (Android `historyDayDetail` / `focusIntensitiesByDate`).
//  iOS logs per focus segment rather than per-cycle `SessionRecord`, so rest
//  minutes are always zero and every row is treated as completed.
//

import Foundation

/// Full detail for a single calendar day on the History screen.
struct HistoryDayDetail: Equatable {
    let date: Date
    let focusMinutes: Int
    let focusSessions: Int
    /// Always 0 on iOS — break completions are not logged per segment.
    let restMinutes: Int
    let rows: [FocusSessionRecord]
}

enum History {
    /// Buckets a day's focus minutes into a 0..4 heatmap intensity level
    /// (ported from Android `focusIntensityLevel`).
    static func focusIntensityLevel(minutes: Int) -> Int {
        switch minutes {
        case ...0: return 0
        case ..<60: return 1
        case ..<120: return 2
        case ..<180: return 3
        default: return 4
        }
    }

    /// Calendar day a focus segment started on (endedAt − minutes), matching
    /// Android `SessionRecord.startedDate`.
    static func startedDay(for record: FocusSessionRecord, calendar: Calendar = Journey.calendar) -> Date {
        let started = record.endedAt.addingTimeInterval(-TimeInterval(max(record.minutes, 0)) * 60)
        return calendar.startOfDay(for: started)
    }

    /// Maps each focused calendar day to a 1..4 intensity bucket. Days with no
    /// focus are absent (treated as level 0).
    static func focusIntensitiesByDate(
        records: [FocusSessionRecord],
        calendar: Calendar = Journey.calendar
    ) -> [Date: Int] {
        var minutesByDay: [Date: Int] = [:]
        for record in records {
            let day = startedDay(for: record, calendar: calendar)
            minutesByDay[day, default: 0] += max(record.minutes, 0)
        }
        var result: [Date: Int] = [:]
        for (day, minutes) in minutesByDay where minutes > 0 {
            result[day] = focusIntensityLevel(minutes: minutes)
        }
        return result
    }

    static func dayDetail(
        records: [FocusSessionRecord],
        date: Date,
        calendar: Calendar = Journey.calendar
    ) -> HistoryDayDetail {
        let dayStart = calendar.startOfDay(for: date)
        let dayRecords = records
            .filter { calendar.isDate(startedDay(for: $0, calendar: calendar), inSameDayAs: dayStart) }
            .sorted { $0.endedAt > $1.endedAt }
        let focusMinutes = dayRecords.reduce(0) { $0 + max($1.minutes, 0) }
        return HistoryDayDetail(
            date: dayStart,
            focusMinutes: focusMinutes,
            focusSessions: dayRecords.count,
            restMinutes: 0,
            rows: dayRecords
        )
    }

    /// Formats focus/rest minutes as "45m", "1h", or "1h 15m" (Android
    /// `journeyMinutesLabel`).
    static func minutesLabel(_ minutes: Int) -> String {
        let safe = max(minutes, 0)
        let hours = safe / 60
        let mins = safe % 60
        if hours > 0 && mins > 0 { return "\(hours)h \(mins)m" }
        if hours > 0 { return "\(hours)h" }
        return "\(mins)m"
    }

    /// Estimated start time for a focus segment (endedAt − minutes).
    static func startedAt(for record: FocusSessionRecord) -> Date {
        record.endedAt.addingTimeInterval(-TimeInterval(max(record.minutes, 0)) * 60)
    }
}

/// Six-week month grid that includes adjacent-month days (Android History
/// calendar), so every cell has a real date.
struct HistoryMonthGrid {
    let monthStart: Date
    let days: [Date]
    let title: String

    init(month: Date, calendar: Calendar = Journey.calendar) {
        let comps = calendar.dateComponents([.year, .month], from: month)
        let start = calendar.date(from: comps) ?? month
        self.monthStart = start

        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        self.title = formatter.string(from: start)

        let weekday = calendar.component(.weekday, from: start)
        let lead = (weekday - calendar.firstWeekday + 7) % 7
        let firstVisible = calendar.date(byAdding: .day, value: -lead, to: start) ?? start

        var cells: [Date] = []
        for offset in 0..<42 {
            if let d = calendar.date(byAdding: .day, value: offset, to: firstVisible) {
                cells.append(calendar.startOfDay(for: d))
            }
        }
        self.days = cells
    }

    func isInMonth(_ date: Date, calendar: Calendar = Journey.calendar) -> Bool {
        calendar.isDate(date, equalTo: monthStart, toGranularity: .month)
    }

    var weekdaySymbols: [String] {
        let cal = Journey.calendar
        let symbols = cal.veryShortStandaloneWeekdaySymbols
        let first = cal.firstWeekday - 1
        return Array(symbols[first...] + symbols[..<first])
    }
}
