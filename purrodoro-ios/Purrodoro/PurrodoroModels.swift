//
//  PurrodoroModels.swift
//  Purrodoro
//
//  Pure domain models for the Pomodoro cycle, ported from the Android
//  `KuroModels.kt` source of truth. Kept free of UI / framework types so the
//  logic is easy to reason about and unit-test.
//

import Foundation

// MARK: - Timer modes

enum TimerMode: String, CaseIterable, Codable {
    case focus
    case shortBreak
    case longBreak

    var label: String {
        switch self {
        case .focus: return "Focus"
        case .shortBreak: return "Short Break"
        case .longBreak: return "Long Break"
        }
    }
}

// MARK: - Cycle segments

/// A single segment within a cycle. Mirrors the Android `SessionItemType`
/// sealed class; the associated value is the segment duration in seconds.
enum SessionItemType: Equatable, Codable {
    case focus(seconds: Int)
    case shortBreak(seconds: Int)
    case longBreak(seconds: Int)

    var seconds: Int {
        switch self {
        case let .focus(s), let .shortBreak(s), let .longBreak(s): return s
        }
    }

    var mode: TimerMode {
        switch self {
        case .focus: return .focus
        case .shortBreak: return .shortBreak
        case .longBreak: return .longBreak
        }
    }

    var isFocus: Bool {
        if case .focus = self { return true }
        return false
    }
}

struct TimerSessionItem: Equatable, Codable, Identifiable {
    let id: Int
    let type: SessionItemType

    init(id: Int, type: SessionItemType) {
        self.id = id
        self.type = type
    }
}

enum TimerState: String, Codable {
    case idle
    case playing
    case paused
}

struct TimerSession: Equatable, Codable {
    var items: [TimerSessionItem]
    var currentIndex: Int
    var remainingSeconds: Int
    var timerState: TimerState

    var isActive: Bool { timerState != .idle }
    var isComplete: Bool { currentIndex >= items.count }
    var currentItem: TimerSessionItem? {
        items.indices.contains(currentIndex) ? items[currentIndex] : nil
    }
}

// MARK: - Session history

/// A single completed focus session. Lighter than the Android per-cycle
/// `SessionRecord`: we log each finished Focus segment with when it ended, how
/// long it was, and the active intent (task/habit) name, if any.
struct FocusSessionRecord: Identifiable, Codable, Equatable {
    let id: UUID
    let endedAt: Date
    let minutes: Int
    let intentName: String?

    init(id: UUID = UUID(), endedAt: Date = Date(), minutes: Int, intentName: String? = nil) {
        self.id = id
        self.endedAt = endedAt
        self.minutes = minutes
        self.intentName = intentName
    }
}

// MARK: - Appearance

/// User-selectable color scheme preference. Ported from the Android
/// `AppearanceMode`. The SwiftUI `ColorScheme?` mapping lives in
/// `PurrodoroTheme.swift` to keep this file framework-free.
enum AppearanceMode: String, CaseIterable, Codable, Identifiable {
    case auto
    case light
    case dark

    var id: String { rawValue }

    var label: String {
        switch self {
        case .auto: return "Auto"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }
}

// MARK: - Settings

struct TimerSettings: Equatable, Codable {
    var focusMinutes: Int = 25
    var shortBreakMinutes: Int = 5
    var longBreakMinutes: Int = 15
    var autoStartNextSession: Bool = false
    var focusSessionsPerCycle: Int = 4

    static let maxFocusSessionsPerCycle = 4
    static let minMinutes = 1
    static let maxMinutes = 180

    func seconds(for mode: TimerMode) -> Int {
        switch mode {
        case .focus: return focusMinutes * 60
        case .shortBreak: return shortBreakMinutes * 60
        case .longBreak: return longBreakMinutes * 60
        }
    }

    func updating(_ mode: TimerMode, minutes: Int) -> TimerSettings {
        let safe = min(max(minutes, Self.minMinutes), Self.maxMinutes)
        var copy = self
        switch mode {
        case .focus: copy.focusMinutes = safe
        case .shortBreak: copy.shortBreakMinutes = safe
        case .longBreak: copy.longBreakMinutes = safe
        }
        return copy
    }

    func updatingCycleSize(_ n: Int) -> TimerSettings {
        var copy = self
        copy.focusSessionsPerCycle = min(max(n, 1), Self.maxFocusSessionsPerCycle)
        return copy
    }
}

// MARK: - Cycle construction

/// Builds the full Pomodoro cycle item list from settings.
///
/// Pattern for `N = focusSessionsPerCycle`:
///   (Focus + ShortBreak) × (N-1), then Focus + LongBreak
func buildCycle(_ settings: TimerSettings) -> [TimerSessionItem] {
    var items: [TimerSessionItem] = []
    let n = max(settings.focusSessionsPerCycle, 1)
    var nextId = 0
    func append(_ type: SessionItemType) {
        items.append(TimerSessionItem(id: nextId, type: type))
        nextId += 1
    }
    for _ in 0..<(n - 1) {
        append(.focus(seconds: settings.focusMinutes * 60))
        append(.shortBreak(seconds: settings.shortBreakMinutes * 60))
    }
    append(.focus(seconds: settings.focusMinutes * 60))
    append(.longBreak(seconds: settings.longBreakMinutes * 60))
    return items
}

/// Builds a fresh idle `TimerSession` for the start of a new cycle.
func buildSession(_ settings: TimerSettings) -> TimerSession {
    let items = buildCycle(settings)
    return TimerSession(
        items: items,
        currentIndex: 0,
        remainingSeconds: items.first?.type.seconds ?? settings.seconds(for: .focus),
        timerState: .idle
    )
}

// MARK: - Cycle progress dots

struct CycleDotsState: Equatable {
    let totalFocusSessions: Int
    let completedFocusSessions: Int
    let isCurrentlyFocus: Bool
}

func cycleDotsState(
    cycleItems: [TimerSessionItem],
    cycleIndex: Int,
    timerState: TimerState,
    settings: TimerSettings
) -> CycleDotsState {
    let focusIndices = cycleItems.enumerated().compactMap { idx, item in
        item.type.isFocus ? idx : nil
    }
    let total = focusIndices.isEmpty ? settings.focusSessionsPerCycle : focusIndices.count
    let completed = focusIndices.filter { $0 < cycleIndex }.count
    let isFocusNow = timerState != .idle &&
        (cycleItems.indices.contains(cycleIndex) ? cycleItems[cycleIndex].type.isFocus : false)
    return CycleDotsState(
        totalFocusSessions: total,
        completedFocusSessions: completed,
        isCurrentlyFocus: isFocusNow
    )
}

// MARK: - Formatting & greetings

func formatSeconds(_ totalSeconds: Int) -> String {
    let safe = max(totalSeconds, 0)
    return String(format: "%02d:%02d", safe / 60, safe % 60)
}

enum DayPart {
    case morning, afternoon, evening

    static func current(_ date: Date = Date()) -> DayPart {
        let hour = Calendar.current.component(.hour, from: date)
        switch hour {
        case 5..<12: return .morning
        case 12..<17: return .afternoon
        default: return .evening
        }
    }

    var salutation: String {
        switch self {
        case .morning: return "Good morning"
        case .afternoon: return "Good afternoon"
        case .evening: return "Good evening"
        }
    }

    var encouragement: String {
        switch self {
        case .morning: return "Let's make this morning meaningful."
        case .afternoon: return "Keep the afternoon moving gently."
        case .evening: return "Let's make this evening meaningful."
        }
    }
}
