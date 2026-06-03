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

struct HabitItem: Identifiable, Equatable, Codable {
    var id: UUID = UUID()
    var name: String
    var emoji: String = "🌱"
    var colorHex: UInt32 = 0xFFB59E
    var targetPerDay: Int = 1
    var log: [String: Int] = [:]
    var createdAt: Date = Date()

    var color: Color { Color(hex: colorHex) }

    func count(on date: Date) -> Int { log[Journey.dayKey(date)] ?? 0 }
    var todayCount: Int { count(on: Date()) }
    var isDoneToday: Bool { todayCount >= max(targetPerDay, 1) }
    var todayProgress: Double {
        min(Double(todayCount) / Double(max(targetPerDay, 1)), 1)
    }

    func meetsTarget(on date: Date) -> Bool { count(on: date) >= max(targetPerDay, 1) }

    /// Consecutive days (ending today) meeting the daily target. If today's
    /// target isn't met yet, count backwards from yesterday.
    var streak: Int {
        let cal = Journey.calendar
        let today = Date()
        var streak = meetsTarget(on: today) ? 1 : 0
        guard var date = cal.date(byAdding: .day, value: -1, to: today) else { return streak }
        while meetsTarget(on: date) {
            streak += 1
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

    private let store = UserDefaults.standard
    private let key = "purrodoro.habits"
    private let activeKey = "purrodoro.activeHabit"

    init() { load() }

    var activeHabit: HabitItem? {
        guard let id = activeHabitID else { return nil }
        return habits.first { $0.id == id }
    }

    var sortedHabits: [HabitItem] {
        habits.sorted { lhs, rhs in
            if lhs.isDoneToday != rhs.isDoneToday { return !lhs.isDoneToday }
            return lhs.createdAt < rhs.createdAt
        }
    }

    // MARK: Mutations

    func add(name: String, emoji: String, colorHex: UInt32, target: Int) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        habits.append(
            HabitItem(name: trimmed, emoji: emoji, colorHex: colorHex, targetPerDay: max(target, 1))
        )
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
