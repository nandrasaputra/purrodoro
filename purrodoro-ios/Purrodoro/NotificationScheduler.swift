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

@MainActor
final class NotificationScheduler: NSObject {
    static let shared = NotificationScheduler()

    private let center = UNUserNotificationCenter.current()
    private let identifier = "purrodoro.segmentEnd"

    override init() {
        super.init()
        center.delegate = self
    }

    func requestAuthorization() {
        center.requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    /// Async variant used by onboarding so the UI can advance once the user has
    /// responded to the system prompt. Returns whether permission was granted.
    @discardableResult
    func requestAuthorization() async -> Bool {
        (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
    }

    /// Schedule (replacing any pending) a segment-end notification at `date`.
    func schedule(at date: Date, title: String, body: String) {
        let interval = date.timeIntervalSinceNow
        center.removePendingNotificationRequests(withIdentifiers: [identifier])
        guard interval > 0 else { return }

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
        center.add(request)
    }

    func cancel() {
        center.removePendingNotificationRequests(withIdentifiers: [identifier])
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
