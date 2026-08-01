//
//  WidgetDashboardProvider.swift
//  PurrodoroWidgets
//
//  Shared timeline provider that reads the App Group dashboard snapshot.
//

import SwiftUI
import WidgetKit

struct WidgetDashboardEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetDashboardSnapshot
}

struct WidgetDashboardProvider: TimelineProvider {
    func placeholder(in context: Context) -> WidgetDashboardEntry {
        var snap = WidgetDashboardSnapshot.empty
        snap.focusToday = 2
        snap.streak = 5
        snap.pendingHabits = [
            WidgetChipSnapshot(emoji: "🌱", label: "Water plants", target: .openHabits)
        ]
        snap.pendingTasks = [
            WidgetChipSnapshot(emoji: "📝", label: "Write outline", target: .openTasks)
        ]
        snap.pendingTasksCount = 1
        return WidgetDashboardEntry(date: Date(), snapshot: snap)
    }

    func getSnapshot(in context: Context, completion: @escaping (WidgetDashboardEntry) -> Void) {
        completion(WidgetDashboardEntry(date: Date(), snapshot: SharedStore.readDashboard()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WidgetDashboardEntry>) -> Void) {
        let now = Date()
        let entry = WidgetDashboardEntry(date: now, snapshot: SharedStore.readDashboard(now: now))
        let nextHour = Calendar.current.date(byAdding: .hour, value: 1, to: now) ?? now.addingTimeInterval(3600)
        completion(Timeline(entries: [entry], policy: .after(nextHour)))
    }
}
