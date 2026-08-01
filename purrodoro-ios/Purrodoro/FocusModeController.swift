//
//  FocusModeController.swift
//  Purrodoro
//
//  App-level Focus Mode flag shared by HomeView (entry) and RootView (overlay +
//  dock visibility). Auto-clears when the timer returns to idle.
//

import Foundation
import Combine

@MainActor
final class FocusModeController: ObservableObject {
    @Published var isFocusMode = false

    func enter() {
        isFocusMode = true
    }

    func exit() {
        isFocusMode = false
    }

    /// Drop Focus Mode when the session ends (stop / cycle complete).
    func syncWithTimerState(_ state: TimerState) {
        if isFocusMode && !focusSessionInFlight(timerState: state) {
            isFocusMode = false
        }
    }
}
