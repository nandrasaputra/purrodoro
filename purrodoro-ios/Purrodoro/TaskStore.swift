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

    var progress: Double {
        guard plannedSessions > 0 else { return completed ? 1 : 0 }
        return min(Double(completedSessions) / Double(plannedSessions), 1)
    }
}

@MainActor
final class TaskStore: ObservableObject {
    @Published private(set) var tasks: [TaskItem] = []
    @Published var activeTaskID: UUID?

    private let store = UserDefaults.standard
    private let key = "purrodoro.tasks"
    private let activeKey = "purrodoro.activeTask"

    init() { load() }

    // MARK: Derived

    var activeTask: TaskItem? {
        guard let id = activeTaskID else { return nil }
        return tasks.first { $0.id == id }
    }

    var activeTasks: [TaskItem] {
        tasks.filter { !$0.completed }
            .sorted { lhs, rhs in
                if lhs.priority.weight != rhs.priority.weight {
                    return lhs.priority.weight > rhs.priority.weight
                }
                return lhs.createdAt < rhs.createdAt
            }
    }

    var completedTasks: [TaskItem] {
        tasks.filter { $0.completed }.sorted { $0.createdAt > $1.createdAt }
    }

    // MARK: Mutations

    func add(title: String, priority: TaskPriority, plannedSessions: Int) {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        tasks.append(
            TaskItem(
                title: trimmed,
                priority: priority,
                plannedSessions: max(plannedSessions, 1)
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
