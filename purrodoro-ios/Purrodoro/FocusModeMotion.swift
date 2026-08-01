//
//  FocusModeMotion.swift
//  Purrodoro
//
//  Shared motion + visibility helpers for Focus Mode — mirrors Android
//  `FocusModeMotion.kt`.
//

import Foundation
import CoreGraphics

enum FocusModeMotion {
    static let enterDuration: TimeInterval = 0.70
    static let exitDuration: TimeInterval = 0.40

    static let breathDelay: TimeInterval = 0.80
    static let breathDuration: TimeInterval = 1.20
    static let breathScalePeak: CGFloat = 1.02

    static let scrimAlphaDashboard: CGFloat = 0.1
    static let scrimAlphaFocus: CGFloat = 0.55
}

/// Whether a Pomodoro cycle is still in flight. Drives focus-mode visibility.
func focusSessionInFlight(timerState: TimerState) -> Bool {
    timerState != .idle
}

func focusModeActive(isFocusMode: Bool, timerState: TimerState) -> Bool {
    isFocusMode && focusSessionInFlight(timerState: timerState)
}

struct FocusModeMascotSize: Equatable {
    var width: CGFloat
    var height: CGFloat
}

private let focusModeMascotAspectRatio: CGFloat = 1.4
private let focusModeMascotMinWidth: CGFloat = 280
private let focusModeMascotMinHeight: CGFloat = 200
private let focusModeMascotMaxWidth: CGFloat = 440
private let focusModeMascotBandWidthFraction: CGFloat = 0.75
private let focusModeMascotBandHeightFraction: CGFloat = 0.9

/// Scales the focus-mode mascot with the flexible center band.
func focusModeMascotMaxSize(bandWidth: CGFloat, bandHeight: CGFloat) -> FocusModeMascotSize {
    guard bandWidth > 0, bandHeight > 0 else {
        return FocusModeMascotSize(width: focusModeMascotMinWidth, height: focusModeMascotMinHeight)
    }
    let scaleBasis = min(bandWidth, bandHeight * focusModeMascotAspectRatio)
    let widthCap = (scaleBasis * focusModeMascotBandWidthFraction)
        .clamped(to: focusModeMascotMinWidth...focusModeMascotMaxWidth)
    let heightCap = max(
        min(widthCap / focusModeMascotAspectRatio, bandHeight * focusModeMascotBandHeightFraction),
        focusModeMascotMinHeight
    )
    let width = max(
        min(heightCap * focusModeMascotAspectRatio, focusModeMascotMaxWidth),
        focusModeMascotMinWidth
    )
    return FocusModeMascotSize(width: width, height: heightCap)
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
