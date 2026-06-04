//
//  HabitStore.swift
//  Purrodoro
//
//  Habits model + store. Mirrors the Android `Habit` concept (name, emoji,
//  color, a daily target, and a per-day completion log) with a SwiftUI
//  ObservableObject backed by UserDefaults. Like tasks, a habit can be the
//  active focus so completed sessions count toward its daily target.
//

import Foundation
import SwiftUI
import Combine

/// When a day a habit is meant to happen — purely informational/scheduling
/// metadata ported from the Android habit "time block".
enum HabitTimeBlock: String, CaseIterable, Codable, Identifiable {
    case any, morning, afternoon, evening
    var id: String { rawValue }

    var labelKey: String { "timeblock.\(rawValue)" }

    var symbol: String {
        switch self {
        case .any: return "clock"
        case .morning: return "sunrise.fill"
        case .afternoon: return "sun.max.fill"
        case .evening: return "moon.stars.fill"
        }
    }
}

struct HabitItem: Identifiable, Equatable, Codable {
    var id: UUID = UUID()
    var name: String
    var emoji: String = "🌱"
    var colorHex: UInt32 = 0xFFB59E
    var targetPerDay: Int = 1
    var log: [String: Int] = [:]
    var createdAt: Date = Date()
    /// Calendar weekdays (1 = Sunday … 7 = Saturday) the habit recurs on. Empty
    /// means every day. Optional default keeps older persisted habits decoding.
    var scheduledWeekdays: Set<Int> = []
    /// Preferred time of day (display + nudges). Defaults to anytime.
    var timeBlock: HabitTimeBlock = .any
    /// One-off skipped days (day-keys) — explicit rest days that don't break a
    /// streak and aren't expected to be completed.
    var restDays: Set<String> = []

    var color: Color { Color(hex: colorHex) }

    func count(on date: Date) -> Int { log[Journey.dayKey(date)] ?? 0 }
    var todayCount: Int { count(on: Date()) }
    var isDoneToday: Bool { todayCount >= max(targetPerDay, 1) }
    var todayProgress: Double {
        min(Double(todayCount) / Double(max(targetPerDay, 1)), 1)
    }

    func meetsTarget(on date: Date) -> Bool { count(on: date) >= max(targetPerDay, 1) }

    private func weekday(_ date: Date) -> Int {
        Journey.calendar.component(.weekday, from: date)
    }

    /// Whether the habit is meant to be done on `date`: a scheduled weekday and
    /// not an explicit rest day.
    func isScheduled(on date: Date) -> Bool {
        guard !restDays.contains(Journey.dayKey(date)) else { return false }
        return scheduledWeekdays.isEmpty || scheduledWeekdays.contains(weekday(date))
    }

    func isRestDay(_ date: Date) -> Bool { restDays.contains(Journey.dayKey(date)) }

    var isScheduledToday: Bool { isScheduled(on: Date()) }

    /// Consecutive *scheduled* days (ending today) meeting the daily target.
    /// Non-scheduled days and rest days are skipped without breaking the run; an
    /// in-progress today that isn't met yet doesn't reset a legitimate streak.
    var streak: Int {
        let cal = Journey.calendar
        var date = Date()
        var streak = 0
        var first = true
        var guardCount = 0
        while guardCount < 366 {
            guardCount += 1
            if isScheduled(on: date) {
                if meetsTarget(on: date) {
                    streak += 1
                } else if !first {
                    break
                }
            }
            first = false
            guard let prev = cal.date(byAdding: .day, value: -1, to: date) else { break }
            date = prev
        }
        return streak
    }
}

@MainActor
final class HabitStore: ObservableObject {
    @Published private(set) var habits: [HabitItem] = []
    @Published var activeHabitID: UUID?

    static let palette: [UInt32] = [0xFFB59E, 0xCDEEDB, 0xD8C8FF, 0xCFEAF8, 0xFFD8DF, 0xFF7F73]
    static let emojis = ["🌱", "📚", "🏃", "🧘", "💧", "🎨", "🎸", "✍️", "🧹", "☀️", "🌙", "🍎"]

    private let store: KeyValueStore
    private let key = "purrodoro.habits"
    private let activeKey = "purrodoro.activeHabit"

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        load()
    }

    var activeHabit: HabitItem? {
        guard let id = activeHabitID else { return nil }
        return habits.first { $0.id == id }
    }

    var sortedHabits: [HabitItem] {
        habits.sorted { lhs, rhs in
            // Scheduled-and-unfinished first, then scheduled-done, then off-day.
            let lRank = rank(lhs)
            let rRank = rank(rhs)
            if lRank != rRank { return lRank < rRank }
            return lhs.createdAt < rhs.createdAt
        }
    }

    private func rank(_ habit: HabitItem) -> Int {
        if !habit.isScheduledToday { return 2 }
        return habit.isDoneToday ? 1 : 0
    }

    // MARK: Mutations

    func add(
        name: String,
        emoji: String,
        colorHex: UInt32,
        target: Int,
        weekdays: Set<Int> = [],
        timeBlock: HabitTimeBlock = .any
    ) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        habits.append(
            HabitItem(
                name: trimmed,
                emoji: emoji,
                colorHex: colorHex,
                targetPerDay: max(target, 1),
                scheduledWeekdays: weekdays,
                timeBlock: timeBlock
            )
        )
        persist()
    }

    /// Toggle today as an explicit rest day for the habit.
    func toggleRestDay(_ habit: HabitItem, on date: Date = Date()) {
        guard let idx = habits.firstIndex(where: { $0.id == habit.id }) else { return }
        let key = Journey.dayKey(date)
        if habits[idx].restDays.contains(key) {
            habits[idx].restDays.remove(key)
        } else {
            habits[idx].restDays.insert(key)
        }
        persist()
    }

    func update(_ habit: HabitItem) {
        guard let idx = habits.firstIndex(where: { $0.id == habit.id }) else { return }
        habits[idx] = habit
        persist()
    }

    func delete(_ habit: HabitItem) {
        habits.removeAll { $0.id == habit.id }
        if activeHabitID == habit.id { activeHabitID = nil; persistActive() }
        persist()
    }

    /// Tap-to-log for today: bumps the count, wrapping back to 0 once the
    /// target is exceeded so a tap can also "undo" a finished habit.
    func toggleToday(_ habit: HabitItem) {
        guard let idx = habits.firstIndex(where: { $0.id == habit.id }) else { return }
        let key = Journey.dayKey(Date())
        let current = habits[idx].log[key] ?? 0
        let target = max(habits[idx].targetPerDay, 1)
        habits[idx].log[key] = current >= target ? 0 : current + 1
        persist()
    }

    func setActive(_ habit: HabitItem) {
        activeHabitID = habit.id
        persistActive()
    }

    func clearActive() {
        activeHabitID = nil
        persistActive()
    }

    /// Credit the active habit with one completed focus session.
    func creditActiveFocusSession() {
        guard let id = activeHabitID,
              let idx = habits.firstIndex(where: { $0.id == id }) else { return }
        let key = Journey.dayKey(Date())
        habits[idx].log[key, default: 0] += 1
        persist()
    }

    // MARK: Persistence

    private func persist() {
        if let data = try? JSONEncoder().encode(habits) {
            store.set(data, forKey: key)
        }
    }

    private func persistActive() {
        store.set(activeHabitID?.uuidString, forKey: activeKey)
    }

    private func load() {
        if let data = store.data(forKey: key),
           let decoded = try? JSONDecoder().decode([HabitItem].self, from: data) {
            habits = decoded
        }
        if let raw = store.string(forKey: activeKey),
           let id = UUID(uuidString: raw),
           habits.contains(where: { $0.id == id }) {
            activeHabitID = id
        }
    }
}
