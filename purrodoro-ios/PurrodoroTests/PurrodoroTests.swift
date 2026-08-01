//
//  PurrodoroTests.swift
//  PurrodoroTests
//
//  Unit tests for the pure domain logic: cycle construction, settings
//  clamping, cycle-progress dots, time formatting, day-part greetings, and
//  the Journey streak/aggregation helpers ported from the Android app.
//

import Testing
import Foundation
@testable import Purrodoro

// MARK: - Cycle construction

struct CycleBuildingTests {

    @Test func cycleFollowsFocusBreakPattern() {
        let settings = TimerSettings(
            focusMinutes: 25, shortBreakMinutes: 5, longBreakMinutes: 15,
            autoStartNextSession: false, focusSessionsPerCycle: 4
        )
        let items = buildCycle(settings)

        // (Focus + ShortBreak) × (N-1) + Focus + LongBreak  ⇒  2N items.
        #expect(items.count == 8)

        let modes = items.map(\.type.mode)
        #expect(modes == [
            .focus, .shortBreak,
            .focus, .shortBreak,
            .focus, .shortBreak,
            .focus, .longBreak,
        ])

        #expect(items.filter { $0.type.isFocus }.count == 4)
        #expect(items.filter { $0.type.mode == .shortBreak }.count == 3)
        #expect(items.filter { $0.type.mode == .longBreak }.count == 1)
    }

    @Test func cycleWithSingleFocusSessionHasNoShortBreak() {
        let settings = TimerSettings(focusSessionsPerCycle: 1)
        let items = buildCycle(settings)
        #expect(items.map(\.type.mode) == [.focus, .longBreak])
    }

    @Test func cycleAssignsContiguousIds() {
        let items = buildCycle(TimerSettings(focusSessionsPerCycle: 3))
        #expect(items.map(\.id) == Array(0..<items.count))
    }

    @Test func cycleEncodesDurationsInSeconds() {
        let settings = TimerSettings(focusMinutes: 25, shortBreakMinutes: 5, longBreakMinutes: 15)
        let items = buildCycle(settings)
        #expect(items.first?.type.seconds == 25 * 60)
        #expect(items.last?.type.seconds == 15 * 60)
    }

    @Test func buildSessionStartsIdleOnFirstFocus() {
        let settings = TimerSettings(focusMinutes: 30)
        let session = buildSession(settings)
        #expect(session.timerState == .idle)
        #expect(session.currentIndex == 0)
        #expect(session.remainingSeconds == 30 * 60)
        #expect(session.currentItem?.type.isFocus == true)
        #expect(session.isActive == false)
    }
}

// MARK: - Settings clamping

struct TimerSettingsTests {

    @Test func updatingClampsBelowMinimum() {
        let s = TimerSettings().updating(.focus, minutes: -10)
        #expect(s.focusMinutes == TimerSettings.minMinutes)
    }

    @Test func updatingClampsAboveMaximum() {
        let s = TimerSettings().updating(.shortBreak, minutes: 10_000)
        #expect(s.shortBreakMinutes == TimerSettings.maxMinutes)
    }

    @Test func updatingTouchesOnlyTheGivenMode() {
        let base = TimerSettings(focusMinutes: 25, shortBreakMinutes: 5, longBreakMinutes: 15)
        let s = base.updating(.longBreak, minutes: 20)
        #expect(s.longBreakMinutes == 20)
        #expect(s.focusMinutes == 25)
        #expect(s.shortBreakMinutes == 5)
    }

    @Test func cycleSizeClampsToValidRange() {
        #expect(TimerSettings().updatingCycleSize(0).focusSessionsPerCycle == 1)
        #expect(TimerSettings().updatingCycleSize(99).focusSessionsPerCycle
                == TimerSettings.maxFocusSessionsPerCycle)
    }

    @Test func secondsForModeMatchesMinutes() {
        let s = TimerSettings(focusMinutes: 25, shortBreakMinutes: 5, longBreakMinutes: 15)
        #expect(s.seconds(for: .focus) == 1500)
        #expect(s.seconds(for: .shortBreak) == 300)
        #expect(s.seconds(for: .longBreak) == 900)
    }
}

// MARK: - Cycle dots

struct CycleDotsTests {

    private let settings = TimerSettings(focusSessionsPerCycle: 4)

    @Test func idleShowsAllFocusSlotsAndNoneActive() {
        let items = buildCycle(settings)
        let dots = cycleDotsState(cycleItems: items, cycleIndex: 0, timerState: .idle, settings: settings)
        #expect(dots.totalFocusSessions == 4)
        #expect(dots.completedFocusSessions == 0)
        #expect(dots.isCurrentlyFocus == false)
    }

    @Test func midCycleCountsCompletedFocusBlocks() {
        let items = buildCycle(settings)
        // Index 2 is the second focus block (indices 0,2,4,6 are focus).
        let dots = cycleDotsState(cycleItems: items, cycleIndex: 2, timerState: .playing, settings: settings)
        #expect(dots.completedFocusSessions == 1)
        #expect(dots.isCurrentlyFocus == true)
    }

    @Test func duringBreakIsNotCurrentlyFocus() {
        let items = buildCycle(settings)
        // Index 1 is the first short break.
        let dots = cycleDotsState(cycleItems: items, cycleIndex: 1, timerState: .playing, settings: settings)
        #expect(dots.isCurrentlyFocus == false)
        #expect(dots.completedFocusSessions == 1)
    }
}

// MARK: - Formatting & greetings

struct FormattingTests {

    @Test func formatsMinutesAndSeconds() {
        #expect(formatSeconds(0) == "00:00")
        #expect(formatSeconds(65) == "01:05")
        #expect(formatSeconds(600) == "10:00")
        #expect(formatSeconds(3661) == "61:01")
    }

    @Test func negativeSecondsClampToZero() {
        #expect(formatSeconds(-5) == "00:00")
    }

    @Test func dayPartBucketsByHour() {
        func part(at hour: Int) -> DayPart {
            var c = DateComponents()
            c.year = 2026; c.month = 6; c.day = 4; c.hour = hour
            let date = Calendar.current.date(from: c)!
            return DayPart.current(date)
        }
        #expect(part(at: 8) == .morning)
        #expect(part(at: 13) == .afternoon)
        #expect(part(at: 21) == .evening)
        #expect(part(at: 3) == .evening)
    }
}

// MARK: - Journey streaks & aggregation

struct JourneyTests {

    private let cal = Journey.calendar

    private func day(_ offset: Int, from today: Date) -> String {
        Journey.dayKey(cal.date(byAdding: .day, value: offset, to: today)!)
    }

    private var fixedToday: Date {
        var c = DateComponents()
        c.year = 2026; c.month = 6; c.day = 4; c.hour = 10
        return Calendar.current.date(from: c)!
    }

    @Test func dayKeyRoundTrips() {
        let key = Journey.dayKey(fixedToday)
        let parsed = Journey.date(fromKey: key)
        #expect(parsed != nil)
        #expect(Journey.dayKey(parsed!) == key)
    }

    @Test func currentStreakCountsConsecutiveDaysIncludingToday() {
        let today = fixedToday
        let stamps = [day(0, from: today): 2, day(-1, from: today): 1, day(-2, from: today): 3]
        #expect(Journey.currentStreak(stamps, today: today) == 3)
    }

    @Test func currentStreakIgnoresMissingTodayButKeepsRun() {
        let today = fixedToday
        // No stamp today; yesterday and the day before are stamped.
        let stamps = [day(-1, from: today): 1, day(-2, from: today): 1]
        #expect(Journey.currentStreak(stamps, today: today) == 2)
    }

    @Test func currentStreakBreaksOnGap() {
        let today = fixedToday
        let stamps = [day(0, from: today): 1, day(-2, from: today): 1]
        #expect(Journey.currentStreak(stamps, today: today) == 1)
    }

    @Test func bestStreakFindsLongestHistoricalRun() {
        let today = fixedToday
        let stamps = [
            day(-1, from: today): 1, day(-2, from: today): 1, day(-3, from: today): 1, // run of 3
            day(-10, from: today): 1, day(-11, from: today): 1,                          // run of 2
        ]
        #expect(Journey.bestStreak(stamps) == 3)
    }

    @Test func totalSumsAllValues() {
        #expect(Journey.total(["a": 2, "b": 3, "c": 0]) == 5)
    }

    @Test func sumLastDaysRespectsWindow() {
        let today = fixedToday
        let minutes = [
            day(0, from: today): 25,
            day(-3, from: today): 10,
            day(-6, from: today): 5,
            day(-8, from: today): 99, // outside a 7-day window
        ]
        #expect(Journey.sum(minutes, lastDays: 7, today: today) == 40)
    }

    @Test func countReadsDayValue() {
        let today = fixedToday
        let stamps = [Journey.dayKey(today): 4]
        #expect(Journey.count(stamps, on: today) == 4)
    }
}

// MARK: - Session history record

struct FocusSessionRecordTests {

    @Test func defaultsGenerateIdAndTimestamp() {
        let before = Date()
        let record = FocusSessionRecord(minutes: 25, intentName: "Write report")
        #expect(record.minutes == 25)
        #expect(record.intentName == "Write report")
        #expect(record.endedAt >= before)
    }

    @Test func codableRoundTrips() throws {
        let record = FocusSessionRecord(minutes: 30, intentName: nil)
        let data = try JSONEncoder().encode(record)
        let decoded = try JSONDecoder().decode(FocusSessionRecord.self, from: data)
        #expect(decoded == record)
    }
}

// MARK: - History day helpers (Android HistoryScreen parity)

struct HistoryModelTests {

    private var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        cal.firstWeekday = 2 // Monday, matching Android History calendar
        return cal
    }

    private func date(_ year: Int, _ month: Int, _ day: Int, hour: Int = 12, minute: Int = 0) -> Date {
        calendar.date(from: DateComponents(
            year: year, month: month, day: day, hour: hour, minute: minute
        ))!
    }

    @Test func intensityBucketsMatchAndroid() {
        #expect(History.focusIntensityLevel(minutes: 0) == 0)
        #expect(History.focusIntensityLevel(minutes: 30) == 1)
        #expect(History.focusIntensityLevel(minutes: 59) == 1)
        #expect(History.focusIntensityLevel(minutes: 60) == 2)
        #expect(History.focusIntensityLevel(minutes: 119) == 2)
        #expect(History.focusIntensityLevel(minutes: 120) == 3)
        #expect(History.focusIntensityLevel(minutes: 179) == 3)
        #expect(History.focusIntensityLevel(minutes: 180) == 4)
        #expect(History.focusIntensityLevel(minutes: 400) == 4)
    }

    @Test func minutesLabelFormatsHoursAndMinutes() {
        #expect(History.minutesLabel(0) == "0m")
        #expect(History.minutesLabel(45) == "45m")
        #expect(History.minutesLabel(60) == "1h")
        #expect(History.minutesLabel(75) == "1h 15m")
    }

    @Test func dayDetailAggregatesSessionsOnSelectedDay() {
        let day = date(2026, 8, 1)
        let morning = FocusSessionRecord(
            endedAt: date(2026, 8, 1, hour: 10, minute: 0),
            minutes: 25,
            intentName: "Write"
        )
        let afternoon = FocusSessionRecord(
            endedAt: date(2026, 8, 1, hour: 15, minute: 30),
            minutes: 50,
            intentName: nil
        )
        let otherDay = FocusSessionRecord(
            endedAt: date(2026, 7, 31, hour: 18, minute: 0),
            minutes: 25,
            intentName: "Other"
        )

        let detail = History.dayDetail(
            records: [morning, afternoon, otherDay],
            date: day,
            calendar: calendar
        )

        #expect(detail.focusSessions == 2)
        #expect(detail.focusMinutes == 75)
        #expect(detail.restMinutes == 0)
        #expect(detail.rows.map(\.id) == [afternoon.id, morning.id]) // newest first
    }

    @Test func intensitiesGroupByStartedDay() {
        // Session ends just after midnight but started the previous evening.
        let late = FocusSessionRecord(
            endedAt: date(2026, 8, 2, hour: 0, minute: 10),
            minutes: 40,
            intentName: nil
        )
        let intensities = History.focusIntensitiesByDate(records: [late], calendar: calendar)
        let august1 = calendar.startOfDay(for: date(2026, 8, 1))
        let august2 = calendar.startOfDay(for: date(2026, 8, 2))
        #expect(intensities[august1] == 1)
        #expect(intensities[august2] == nil)
    }

    @Test func monthGridProducesFortyTwoCells() {
        let grid = HistoryMonthGrid(month: date(2026, 8, 15), calendar: calendar)
        #expect(grid.days.count == 42)
        #expect(grid.isInMonth(date(2026, 8, 1), calendar: calendar))
        #expect(!grid.isInMonth(date(2026, 7, 31), calendar: calendar))
    }
}

// MARK: - Appearance mode

struct AppearanceModeTests {

    @Test func labelsAreStable() {
        #expect(AppearanceMode.auto.label == "Auto")
        #expect(AppearanceMode.light.label == "Light")
        #expect(AppearanceMode.dark.label == "Dark")
    }

    @Test func rawValuesRoundTrip() {
        for mode in AppearanceMode.allCases {
            #expect(AppearanceMode(rawValue: mode.rawValue) == mode)
        }
    }
}
