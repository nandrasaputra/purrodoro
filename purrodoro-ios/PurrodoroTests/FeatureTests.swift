//
//  FeatureTests.swift
//  PurrodoroTests
//
//  Unit tests for the features added in the iOS parity pass: interrupted-focus
//  resume, task categories/due dates, habit scheduling (occurrence + rest
//  days), and the runtime localization table.
//

import Testing
import Foundation
@testable import Purrodoro

// MARK: - Interrupted focus

struct InterruptedFocusTests {

    @Test func leftoverMinutesClampToAtLeastOne() {
        let zero = InterruptedFocus(leftoverMinutes: 0, intentName: nil)
        #expect(zero.leftoverMinutes == 1)
        let negative = InterruptedFocus(leftoverMinutes: -5, intentName: "Write")
        #expect(negative.leftoverMinutes == 1)
        #expect(negative.intentName == "Write")
    }

    @Test func codableRoundTrips() throws {
        let value = InterruptedFocus(leftoverMinutes: 12, intentName: "Read", stoppedAt: Date(timeIntervalSince1970: 100))
        let data = try JSONEncoder().encode(value)
        let decoded = try JSONDecoder().decode(InterruptedFocus.self, from: data)
        #expect(decoded == value)
    }
}

// MARK: - Task categories & due dates

struct TaskItemTests {

    private var cal: Calendar { Calendar.current }

    @Test func trimmedCategoryNormalisesBlankToNil() {
        var task = TaskItem(title: "A")
        task.category = "   "
        #expect(task.trimmedCategory == nil)
        task.category = "  Work "
        #expect(task.trimmedCategory == "Work")
    }

    @Test func overdueOnlyWhenPastAndIncomplete() {
        let yesterday = cal.date(byAdding: .day, value: -1, to: Date())!
        var task = TaskItem(title: "A")
        task.dueDate = yesterday
        #expect(task.isOverdue)
        task.completed = true
        #expect(!task.isOverdue)
    }

    @Test func dueTodayDetected() {
        var task = TaskItem(title: "A")
        task.dueDate = Date()
        #expect(task.isDueToday)
        #expect(!task.isOverdue)
    }

    @Test func noDueDateIsNeverOverdueOrDueToday() {
        let task = TaskItem(title: "A")
        #expect(!task.isOverdue)
        #expect(!task.isDueToday)
    }

    @Test func decodesLegacyTaskWithoutNewFields() throws {
        // Older persisted tasks have no category/dueDate keys.
        let legacy = """
        {"id":"\(UUID().uuidString)","title":"Legacy","priority":"normal","plannedSessions":2,"completedSessions":0,"completed":false,"createdAt":0}
        """.data(using: .utf8)!
        let task = try JSONDecoder().decode(TaskItem.self, from: legacy)
        #expect(task.title == "Legacy")
        #expect(task.category == nil)
        #expect(task.dueDate == nil)
    }
}

// MARK: - Habit scheduling

struct HabitSchedulingTests {

    private var cal: Calendar { Calendar.current }

    private func date(weekday: Int, base: Date = Date()) -> Date {
        var d = cal.startOfDay(for: base)
        for _ in 0..<7 {
            if cal.component(.weekday, from: d) == weekday { return d }
            d = cal.date(byAdding: .day, value: 1, to: d)!
        }
        return d
    }

    @Test func emptyWeekdaysMeansEveryDay() {
        let habit = HabitItem(name: "Read")
        #expect(habit.isScheduled(on: date(weekday: 2)))
        #expect(habit.isScheduled(on: date(weekday: 6)))
    }

    @Test func specificWeekdaysGateScheduling() {
        var habit = HabitItem(name: "Gym")
        habit.scheduledWeekdays = [2, 4] // Mon, Wed (1=Sun)
        #expect(habit.isScheduled(on: date(weekday: 2)))
        #expect(!habit.isScheduled(on: date(weekday: 3)))
        #expect(habit.isScheduled(on: date(weekday: 4)))
    }

    @Test func restDayOverridesSchedule() {
        var habit = HabitItem(name: "Walk")
        let today = Date()
        habit.restDays = [Journey.dayKey(today)]
        #expect(!habit.isScheduled(on: today))
        #expect(habit.isRestDay(today))
    }

    @Test func streakSkipsUnscheduledDaysWithoutBreaking() {
        // Schedule only on the weekday of "today" and the same weekday last week;
        // both met. The 6 unscheduled days in between must not break the streak.
        var habit = HabitItem(name: "Weekly")
        let today = cal.startOfDay(for: Date())
        let weekday = cal.component(.weekday, from: today)
        habit.scheduledWeekdays = [weekday]
        habit.targetPerDay = 1
        let lastWeek = cal.date(byAdding: .day, value: -7, to: today)!
        habit.log[Journey.dayKey(today)] = 1
        habit.log[Journey.dayKey(lastWeek)] = 1
        #expect(habit.streak == 2)
    }
}

// MARK: - Localization

@MainActor
struct LocalizationTests {

    private func manager() -> Localization {
        let suite = UserDefaults(suiteName: "loc.test.\(UUID().uuidString)")!
        return Localization(store: suite)
    }

    @Test func returnsEnglishAndIndonesian() {
        let loc = manager()
        loc.language = .en
        #expect(loc.t("home.startFocus") == "Start Focus")
        loc.language = .id
        #expect(loc.t("home.startFocus") == "Mulai Fokus")
    }

    @Test func unknownKeyFallsBackToKey() {
        let loc = manager()
        #expect(loc.t("nonexistent.key") == "nonexistent.key")
    }

    @Test func formatsIntegerArguments() {
        let loc = manager()
        loc.language = .en
        #expect(loc.t("resume.subtitle", 7) == "7 min of focus left")
    }

    @Test func everyKeyHasBothLanguages() {
        // Guard against a half-translated key slipping in.
        let loc = manager()
        for key in [
            "tab.focus", "home.startFocus", "settings.language", "sounds.title",
            "journey.title", "journey.recentSessions", "journey.currentStreak",
            "onboard.welcome.title", "onboard.enableNotif", "date.today",
        ] {
            loc.language = .en
            #expect(loc.t(key) != key)
            loc.language = .id
            #expect(loc.t(key) != key)
        }
    }
}
