//
//  TaskStore.swift
//  Purrodoro
//
//  Tasks model + store. Mirrors the Android `FocusTask` concept (title,
//  priority, planned/completed focus sessions, completion) with a SwiftUI
//  `ObservableObject` backed by UserDefaults. A task can be made "active" so a
//  focus session counts toward its planned sessions.
//

import Foundation
import SwiftUI
import Combine

enum TaskPriority: String, CaseIterable, Codable, Identifiable {
    case low, normal, high

    var id: String { rawValue }

    var label: String {
        switch self {
        case .low: return "Low"
        case .normal: return "Normal"
        case .high: return "High"
        }
    }

    /// Higher sorts first in the active list.
    var weight: Int {
        switch self {
        case .high: return 2
        case .normal: return 1
        case .low: return 0
        }
    }
}

struct TaskItem: Identifiable, Equatable, Codable {
    var id: UUID = UUID()
    var title: String
    var priority: TaskPriority = .normal
    var plannedSessions: Int = 1
    var completedSessions: Int = 0
    var completed: Bool = false
    var createdAt: Date = Date()
    /// Optional free-text grouping label (e.g. "Work", "Study"). `nil`/empty =
    /// uncategorised. Added after launch, so it's Optional for backward-compatible
    /// decoding of older persisted tasks.
    var category: String? = nil
    /// Optional soft deadline. Drives the overdue / due-today badges.
    var dueDate: Date? = nil

    var progress: Double {
        guard plannedSessions > 0 else { return completed ? 1 : 0 }
        return min(Double(completedSessions) / Double(plannedSessions), 1)
    }

    var trimmedCategory: String? {
        guard let c = category?.trimmingCharacters(in: .whitespacesAndNewlines), !c.isEmpty else { return nil }
        return c
    }

    var isOverdue: Bool {
        guard !completed, let due = dueDate else { return false }
        return Calendar.current.startOfDay(for: due) < Calendar.current.startOfDay(for: Date())
    }

    var isDueToday: Bool {
        guard !completed, let due = dueDate else { return false }
        return Calendar.current.isDateInToday(due)
    }
}

@MainActor
final class TaskStore: ObservableObject {
    @Published private(set) var tasks: [TaskItem] = []
    @Published var activeTaskID: UUID?

    private let store: KeyValueStore
    private let key = "purrodoro.tasks"
    private let activeKey = "purrodoro.activeTask"

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        load()
    }

    // MARK: Derived

    var activeTask: TaskItem? {
        guard let id = activeTaskID else { return nil }
        return tasks.first { $0.id == id }
    }

    var activeTasks: [TaskItem] {
        tasks.filter { !$0.completed }
            .sorted { lhs, rhs in
                // Overdue tasks float to the top, then due-soon, then priority.
                if lhs.isOverdue != rhs.isOverdue { return lhs.isOverdue }
                switch (lhs.dueDate, rhs.dueDate) {
                case let (l?, r?) where l != r: return l < r
                case (.some, .none): return true
                case (.none, .some): return false
                default: break
                }
                if lhs.priority.weight != rhs.priority.weight {
                    return lhs.priority.weight > rhs.priority.weight
                }
                return lhs.createdAt < rhs.createdAt
            }
    }

    var completedTasks: [TaskItem] {
        tasks.filter { $0.completed }.sorted { $0.createdAt > $1.createdAt }
    }

    /// Distinct, sorted category labels currently in use (for suggestions).
    var categories: [String] {
        let names = tasks.compactMap { $0.trimmedCategory }
        return Array(Set(names)).sorted()
    }

    // MARK: Mutations

    func add(title: String, priority: TaskPriority, plannedSessions: Int, category: String? = nil, dueDate: Date? = nil) {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let cat = category?.trimmingCharacters(in: .whitespacesAndNewlines)
        tasks.append(
            TaskItem(
                title: trimmed,
                priority: priority,
                plannedSessions: max(plannedSessions, 1),
                category: (cat?.isEmpty ?? true) ? nil : cat,
                dueDate: dueDate
            )
        )
        persist()
    }

    func update(_ task: TaskItem) {
        guard let idx = tasks.firstIndex(where: { $0.id == task.id }) else { return }
        tasks[idx] = task
        persist()
    }

    func toggleCompleted(_ task: TaskItem) {
        guard let idx = tasks.firstIndex(where: { $0.id == task.id }) else { return }
        tasks[idx].completed.toggle()
        if tasks[idx].completed, activeTaskID == task.id { activeTaskID = nil; persistActive() }
        persist()
    }

    func delete(_ task: TaskItem) {
        tasks.removeAll { $0.id == task.id }
        if activeTaskID == task.id { activeTaskID = nil; persistActive() }
        persist()
    }

    func setActive(_ task: TaskItem) {
        activeTaskID = task.id
        persistActive()
    }

    func clearActive() {
        activeTaskID = nil
        persistActive()
    }

    /// Replace all tasks (backup restore — clears active attachment).
    func replaceAll(_ items: [TaskItem]) {
        tasks = items
        activeTaskID = nil
        persist()
        persistActive()
    }

    /// Credit the active task with one completed focus session. Marks it done
    /// once it reaches its planned sessions.
    func creditActiveFocusSession() {
        guard let id = activeTaskID,
              let idx = tasks.firstIndex(where: { $0.id == id }) else { return }
        tasks[idx].completedSessions += 1
        if tasks[idx].completedSessions >= tasks[idx].plannedSessions {
            tasks[idx].completed = true
            activeTaskID = nil
            persistActive()
        }
        persist()
    }

    // MARK: Persistence

    private func persist() {
        if let data = try? JSONEncoder().encode(tasks) {
            store.set(data, forKey: key)
        }
    }

    private func persistActive() {
        store.set(activeTaskID?.uuidString, forKey: activeKey)
    }

    private func load() {
        if let data = store.data(forKey: key),
           let decoded = try? JSONDecoder().decode([TaskItem].self, from: data) {
            tasks = decoded
        }
        if let raw = store.string(forKey: activeKey),
           let id = UUID(uuidString: raw),
           tasks.contains(where: { $0.id == id && !$0.completed }) {
            activeTaskID = id
        }
    }
}

extension TaskPriority {
    /// Accent color for chips / dots.
    var color: Color {
        switch self {
        case .high: return Brand.coral
        case .normal: return Brand.peach
        case .low: return Brand.sky
        }
    }
}

enum CategoryPalette {
    private static let colors: [Color] = [
        Brand.lavender, Brand.mint, Brand.sky, Brand.peach, Brand.blush, Brand.coral,
    ]

    /// Deterministic color for a category name so the same label always reads
    /// the same hue across rows.
    static func color(for name: String) -> Color {
        let hash = name.unicodeScalars.reduce(0) { ($0 &* 31) &+ Int($1.value) }
        return colors[abs(hash) % colors.count]
    }
}
