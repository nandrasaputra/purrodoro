//
//  NudgeDashboardWidget.swift
//  PurrodoroWidgets
//
//  Greeting + focus/streak + nudge CTA (Android `NudgeDashboardWidget`).
//

import SwiftUI
import WidgetKit

struct NudgeDashboardWidgetView: View {
    let entry: WidgetDashboardEntry
    private var snap: WidgetDashboardSnapshot { entry.snapshot }

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: entry.date)
        switch hour {
        case ..<5: return "Still up?"
        case ..<12: return "Good morning"
        case ..<17: return "Good afternoon"
        case ..<21: return "Good evening"
        default: return "Winding down?"
        }
    }

    var body: some View {
        ZStack {
            Image("DashboardBg")
                .resizable()
                .scaledToFill()
            VStack(alignment: .leading, spacing: 6) {
                Text(greeting)
                    .font(.system(.headline, design: .rounded).weight(.bold))
                    .foregroundStyle(.white)
                Text(snap.nudge.line)
                    .font(.system(.caption, design: .rounded).weight(.medium))
                    .foregroundStyle(.white.opacity(0.9))
                    .lineLimit(1)
                HStack(spacing: 12) {
                    Text("🎯 \(snap.focusToday) focus")
                    Text("🔥 \(snap.streak) day streak")
                }
                .font(.system(.caption2, design: .rounded).weight(.semibold))
                .foregroundStyle(.white)
                Spacer(minLength: 0)
                Text(snap.nudge.ctaLabel)
                    .font(.system(.caption, design: .rounded).weight(.bold))
                    .foregroundStyle(WidgetBrand.cocoa)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(.white.opacity(0.92), in: Capsule())
            }
            .padding(14)
        }
        .containerBackground(for: .widget) {
            Image("DashboardBg")
                .resizable()
                .scaledToFill()
        }
        .widgetURL(snap.nudge.target.url)
    }
}

struct NudgeDashboardWidget: Widget {
    let kind = "PurrodoroNudgeDashboardWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDashboardProvider()) { entry in
            NudgeDashboardWidgetView(entry: entry)
        }
        .configurationDisplayName("Dashboard")
        .description("A warm greeting and the best next action.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}
