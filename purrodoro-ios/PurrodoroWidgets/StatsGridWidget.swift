//
//  StatsGridWidget.swift
//  PurrodoroWidgets
//
//  2×2 glance of today's focus, streak, pending habits, and open tasks
//  (Android `StatsGridWidget`).
//

import SwiftUI
import WidgetKit

struct StatsGridWidgetView: View {
    let entry: WidgetDashboardEntry
    private var snap: WidgetDashboardSnapshot { entry.snapshot }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Daily starts")
                .font(.system(.caption, design: .rounded).weight(.bold))
                .foregroundStyle(WidgetBrand.cocoa)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                tile(emoji: "🎯", value: "\(snap.focusToday)", label: "Focus")
                tile(emoji: "🔥", value: "\(snap.streak)", label: "Streak")
                tile(emoji: "🌱", value: "\(snap.pendingHabits.count)", label: "Habits")
                tile(emoji: "✅", value: "\(snap.pendingTasksCount)", label: "Tasks")
            }
        }
        .containerBackground(WidgetBrand.peach.opacity(0.14), for: .widget)
        .widgetURL(snap.nudge.target.url)
    }

    private func tile(emoji: String, value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(emoji)
                .font(.system(size: 12))
            Text(value)
                .font(.system(size: 22, design: .rounded).weight(.heavy))
                .foregroundStyle(WidgetBrand.cocoa)
                .monospacedDigit()
            Text(label)
                .font(.system(.caption2, design: .rounded).weight(.semibold))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(6)
        .background(Color.white.opacity(0.55), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct StatsGridWidget: Widget {
    let kind = "PurrodoroStatsGridWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDashboardProvider()) { entry in
            StatsGridWidgetView(entry: entry)
        }
        .configurationDisplayName("Daily starts")
        .description("Focus, streak, habits, and tasks at a glance.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
