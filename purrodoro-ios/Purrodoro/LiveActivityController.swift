//
//  LiveActivityController.swift
//  Purrodoro
//
//  Thin wrapper around ActivityKit that mirrors the running timer onto the
//  Lock Screen and Dynamic Island. The engine calls `sync(...)` on every state
//  transition with a flat snapshot, keeping ActivityKit types out of the
//  engine itself. No-ops on platforms without ActivityKit (macOS) and when the
//  user has Live Activities disabled.
//

import Foundation
#if canImport(ActivityKit)
import ActivityKit
#endif

@MainActor
final class LiveActivityController {
    static let shared = LiveActivityController()

    private init() {}

#if canImport(ActivityKit)
    private var activity: Activity<FocusActivityAttributes>?

    var isEnabled: Bool { ActivityAuthorizationInfo().areActivitiesEnabled }
#endif

    /// Reflect the current timer snapshot. Starts the activity on first active
    /// snapshot, updates it while active, and ends it when the timer goes idle.
    func sync(
        isActive: Bool,
        modeRaw: String,
        segmentStart: Date,
        endsAt: Date,
        isRunning: Bool,
        pausedRemaining: Int,
        completedFocus: Int,
        totalFocus: Int,
        intentName: String?
    ) {
#if canImport(ActivityKit)
        guard isActive else {
            end()
            return
        }
        guard isEnabled else { return }

        let state = FocusActivityAttributes.ContentState(
            modeRaw: modeRaw,
            segmentStart: segmentStart,
            endsAt: endsAt,
            isRunning: isRunning,
            pausedRemaining: pausedRemaining,
            completedFocus: completedFocus,
            totalFocus: totalFocus,
            intentName: intentName
        )
        // Let the system mark the activity stale shortly after the segment ends
        // so it doesn't linger with a frozen 00:00.
        let staleDate = isRunning ? endsAt.addingTimeInterval(5) : nil
        let content = ActivityContent(state: state, staleDate: staleDate)

        if let activity {
            Task { await activity.update(content) }
        } else {
            let attributes = FocusActivityAttributes(cycleStart: Date())
            do {
                activity = try Activity.request(
                    attributes: attributes,
                    content: content,
                    pushType: nil
                )
            } catch {
                activity = nil
            }
        }
#endif
    }

    /// Tear down the activity (timer stopped or cycle completed).
    func end() {
#if canImport(ActivityKit)
        guard let activity else { return }
        self.activity = nil
        Task { await activity.end(nil, dismissalPolicy: .immediate) }
#endif
    }
}
