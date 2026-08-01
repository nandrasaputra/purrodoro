//
//  WidgetSnapshot.swift
//  PurrodoroShared
//
//  App Group snapshot for Home Screen widgets beyond Today — mirrors Android
//  `KuroWidgetData` / `WidgetNudge` so the extension can render without
//  importing app stores.
//

import Foundation

/// Where a widget tap should take the user (Android `NudgeTarget`).
enum WidgetNudgeTarget: String, Codable, Equatable {
    case startFocus
    case openTimer
    case openHabits
    case openTasks
    case openApp

    var url: URL {
        switch self {
        case .startFocus:
            return URL(string: "purrodoro://start-focus")!
        case .openTimer, .openApp:
            return URL(string: "purrodoro://tab/focus")!
        case .openHabits:
            return URL(string: "purrodoro://tab/habits")!
        case .openTasks:
            return URL(string: "purrodoro://tab/tasks")!
        }
    }
}

enum WidgetNudgeKind: String, Codable, Equatable {
    case focusing
    case paused
    case celebrating
    case goalReached
    case streakAtRisk
    case habitDue
    case taskDue
    case freshFocus
}

struct WidgetChipSnapshot: Codable, Equatable, Identifiable {
    var id: String { "\(target.rawValue)-\(emoji)-\(label)" }
    var emoji: String
    var label: String
    var target: WidgetNudgeTarget
}

struct WidgetNudgeSnapshot: Codable, Equatable {
    var kind: WidgetNudgeKind
    var title: String
    var line: String
    var ctaLabel: String
    var target: WidgetNudgeTarget
}

/// Full dashboard facts for the reactive widgets (Android `KuroWidgetData`).
struct WidgetDashboardSnapshot: Codable, Equatable {
    var dayKey: String
    var focusToday: Int
    var focusTarget: Int
    var minutesToday: Int
    var streak: Int
    var streakAtRisk: Bool
    var goalReached: Bool
    var habitsScheduledToday: Int
    var habitsDoneToday: Int
    var pendingHabits: [WidgetChipSnapshot]
    var pendingTasksCount: Int
    var pendingTasks: [WidgetChipSnapshot]
    var timerPlaying: Bool
    var timerPaused: Bool
    var pendingCycleAck: Bool
    var nudge: WidgetNudgeSnapshot

    /// Up-to-`max` mixed agenda (habits first, then tasks).
    func planChips(max: Int = 3) -> [WidgetChipSnapshot] {
        Array((pendingHabits + pendingTasks).prefix(max))
    }

    static var empty: WidgetDashboardSnapshot {
        WidgetDashboardSnapshot(
            dayKey: SharedStore.dayKey(),
            focusToday: 0,
            focusTarget: 4,
            minutesToday: 0,
            streak: 0,
            streakAtRisk: false,
            goalReached: false,
            habitsScheduledToday: 0,
            habitsDoneToday: 0,
            pendingHabits: [],
            pendingTasksCount: 0,
            pendingTasks: [],
            timerPlaying: false,
            timerPaused: false,
            pendingCycleAck: false,
            nudge: WidgetNudgeSnapshot(
                kind: .freshFocus,
                title: "Purrodoro is ready",
                line: "Purrodoro's ready when you are",
                ctaLabel: "Start Focus",
                target: .startFocus
            )
        )
    }
}
