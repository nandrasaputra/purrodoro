//
//  CozySceneWidget.swift
//  PurrodoroWidgets
//
//  Study-nook scene with focus-today badge (Android `CozySceneWidget`).
//

import SwiftUI
import WidgetKit

struct CozySceneWidgetView: View {
    let entry: WidgetDashboardEntry
    private var snap: WidgetDashboardSnapshot { entry.snapshot }

    var body: some View {
        ZStack {
            Image("CozySceneBg")
                .resizable()
                .scaledToFill()
            VStack(alignment: .leading, spacing: 8) {
                Text("A cozy corner for deep work")
                    .font(.system(.subheadline, design: .rounded).weight(.bold))
                    .foregroundStyle(WidgetBrand.cocoa)
                    .lineLimit(2)
                Text("\(snap.focusToday) focus today")
                    .font(.system(.caption2, design: .rounded).weight(.bold))
                    .foregroundStyle(WidgetBrand.coral)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color.white.opacity(0.85), in: Capsule())
                Spacer()
            }
            .padding(14)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .containerBackground(for: .widget) {
            Image("CozySceneBg")
                .resizable()
                .scaledToFill()
        }
        .widgetURL(snap.nudge.target.url)
    }
}

struct CozySceneWidget: Widget {
    let kind = "PurrodoroCozySceneWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDashboardProvider()) { entry in
            CozySceneWidgetView(entry: entry)
        }
        .configurationDisplayName("Cozy scene")
        .description("A study nook with today's focus count.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
