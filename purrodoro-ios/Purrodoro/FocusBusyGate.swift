//
//  FocusBusyGate.swift
//  Purrodoro
//
//  Pure gate for "start focus while another Pomodoro is already running"
//  (Android `FocusBusyDialog` / `attemptStartFocus`). Keeps every Tasks /
//  Habits entry point consistent.
//

import Foundation

enum FocusBusyOutcome: Equatable {
    /// No timer running — attach the intent and continue.
    case proceed
    /// Timer is already on this same intent — just show the Focus tab.
    case openFocus
    /// Timer is busy with a different (or empty) intent — show the dialog.
    case busy
}

enum FocusBusyGate {
    /// - Parameters:
    ///   - isStarted: `TimerEngine.isStarted` (playing or paused).
    ///   - liveMatchesSelection: true when the running cycle is already
    ///     attached to the task/habit the user just tapped.
    static func evaluate(isStarted: Bool, liveMatchesSelection: Bool) -> FocusBusyOutcome {
        if !isStarted { return .proceed }
        if liveMatchesSelection { return .openFocus }
        return .busy
    }
}
