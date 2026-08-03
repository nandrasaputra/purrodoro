//
//  FocusShield.swift
//  Purrodoro
//
//  Pure arming rules for Focus Shield (Android `FocusShield.kt`). iOS uses
//  Screen Time authorization + a FamilyActivitySelection instead of UsageStats
//  / overlay permissions and package-name sets — product conditions match.
//

import Foundation

enum FocusShield {
    /// Whether distraction shielding should be active right now.
    ///
    /// Requires: preference on, a nonempty selection, Screen Time authorized,
    /// and a non-idle timer whose current segment is Focus (playing or paused).
    static func isArmed(
        preferenceEnabled: Bool,
        hasSelection: Bool,
        authorized: Bool,
        timerState: TimerState,
        currentIsFocus: Bool
    ) -> Bool {
        guard preferenceEnabled, hasSelection, authorized else { return false }
        guard timerState != .idle else { return false }
        return currentIsFocus
    }

    /// Session-scoped bypass window length (Android overlay "Open anyway").
    /// Clamped to [60s, 2h] around the remaining focus seconds.
    static func bypassDuration(remainingSeconds: Int) -> TimeInterval {
        let clamped = min(max(remainingSeconds, 60), 2 * 60 * 60)
        return TimeInterval(clamped)
    }
}
