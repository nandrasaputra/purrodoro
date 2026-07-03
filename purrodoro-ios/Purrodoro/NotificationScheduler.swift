//
//  NotificationScheduler.swift
//  Purrodoro
//
//  Schedules a single local notification at the active segment's end so the
//  timer stays useful while the app is backgrounded. The engine reschedules
//  on every state transition (start / resume / advance) and cancels on
//  pause / stop / finish.
//

import Foundation
import UserNotifications

/// One scheduled segment-boundary notification.
struct NotificationEntry: Equatable {
    let date: Date
    let title: String
    let body: String
}

@MainActor
final class NotificationScheduler: NSObject {
    static let shared = NotificationScheduler()

    private let center = UNUserNotificationCenter.current()
    private let identifierPrefix = "purrodoro.segmentEnd"
    private var scheduledIDs: [String] = []

    override init() {
        super.init()
        center.delegate = self
    }

    func requestAuthorization() {
        center.requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    /// Whether the user has granted alert permission (mirrors Android's
    /// `NotificationManagerCompat.areNotificationsEnabled()` check in Profile).
    func notificationsEnabled() async -> Bool {
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        default:
            return false
        }
    }

    /// Async variant used by onboarding so the UI can advance once the user has
    /// responded to the system prompt. Returns whether permission was granted.
    @discardableResult
    func requestAuthorization() async -> Bool {
        (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
    }

    /// Schedule (replacing any pending) a chain of segment-boundary
    /// notifications. With auto-start on, the engine passes every upcoming
    /// boundary so the user is still nudged at each transition while the app is
    /// backgrounded; otherwise it passes just the current boundary.
    func schedule(_ entries: [NotificationEntry]) {
        cancel()
        var ids: [String] = []
        for (index, entry) in entries.enumerated() {
            let interval = entry.date.timeIntervalSinceNow
            guard interval > 0 else { continue }

            let content = UNMutableNotificationContent()
            content.title = entry.title
            content.body = entry.body
            content.sound = .default

            let id = "\(identifierPrefix).\(index)"
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
            center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
            ids.append(id)
        }
        scheduledIDs = ids
    }

    func cancel() {
        guard !scheduledIDs.isEmpty else { return }
        center.removePendingNotificationRequests(withIdentifiers: scheduledIDs)
        scheduledIDs = []
    }
}

extension NotificationScheduler: UNUserNotificationCenterDelegate {
    // Show the chime/banner even when the app is foregrounded.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}
