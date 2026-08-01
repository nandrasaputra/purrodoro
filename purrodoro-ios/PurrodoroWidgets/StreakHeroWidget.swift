//
//  StreakHeroWidget.swift
//  PurrodoroWidgets
//
//  Immersive streak tile (Android `StreakHeroWidget`). Tap → Start Focus.
//

import SwiftUI
import WidgetKit

struct StreakHeroWidgetView: View {
    let entry: WidgetDashboardEntry

    var body: some View {
        ZStack {
            Image("StreakSceneBg")
                .resizable()
                .scaledToFill()
            VStack {
                Text("\(entry.snapshot.streak)")
                    .font(.system(size: 44, design: .rounded).weight(.heavy))
                    .foregroundStyle(.white)
                    .shadow(color: .black.opacity(0.25), radius: 4, y: 2)
                    .padding(.top, 18)
                Spacer()
            }
        }
        .containerBackground(for: .widget) {
            Image("StreakSceneBg")
                .resizable()
                .scaledToFill()
        }
        .widgetURL(WidgetNudgeTarget.startFocus.url)
    }
}

struct StreakHeroWidget: Widget {
    let kind = "PurrodoroStreakHeroWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDashboardProvider()) { entry in
            StreakHeroWidgetView(entry: entry)
        }
        .configurationDisplayName("Streak hero")
        .description("Your day streak, front and center.")
        .supportedFamilies([.systemSmall])
    }
}
