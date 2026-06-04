//
//  FocusActivityAttributes.swift
//  Purrodoro (shared between the app and the PurrodoroWidgets extension)
//
//  Describes the data backing the running-timer Live Activity. This type must
//  be compiled into *both* the app target (which starts/updates/ends the
//  activity) and the widget extension (which renders it), so it lives in a
//  shared group that is a member of both targets.
//

#if canImport(ActivityKit)
import ActivityKit
import Foundation

struct FocusActivityAttributes: ActivityAttributes {

    /// The live, frequently-changing portion of the activity.
    public struct ContentState: Codable, Hashable {
        /// `TimerMode` raw value (`focus` / `shortBreak` / `longBreak`) so the
        /// widget can theme itself without importing the app's enum.
        var modeRaw: String
        /// Anchor for the running countdown range (start of the current segment).
        var segmentStart: Date
        /// When the current segment is scheduled to end.
        var endsAt: Date
        /// `true` while counting down, `false` when paused.
        var isRunning: Bool
        /// Seconds left when paused (rendered as a static value).
        var pausedRemaining: Int
        /// Focus segments completed so far this cycle.
        var completedFocus: Int
        /// Total focus segments in this cycle.
        var totalFocus: Int
        /// Name of the attached task/habit, if any.
        var intentName: String?

        var modeLabel: String {
            switch modeRaw {
            case "shortBreak": return "Short Break"
            case "longBreak": return "Long Break"
            default: return "Focus"
            }
        }

        var isFocus: Bool { modeRaw == "focus" }
    }

    /// When the cycle the activity represents was started. Static for the life
    /// of the activity.
    var cycleStart: Date
}
#endif
