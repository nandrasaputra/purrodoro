//
//  CalmMinimalWidget.swift
//  PurrodoroWidgets
//
//  Quiet full-bleed art + nudge CTA (Android `CalmMinimalWidget`).
//

import SwiftUI
import WidgetKit

struct CalmMinimalWidgetView: View {
    let entry: WidgetDashboardEntry

    var body: some View {
        ZStack {
            Image("CalmSceneBg")
                .resizable()
                .scaledToFill()
            VStack {
                Spacer()
                Text(entry.snapshot.nudge.ctaLabel)
                    .font(.system(.caption, design: .rounded).weight(.bold))
                    .foregroundStyle(WidgetBrand.cocoa)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.white.opacity(0.88), in: Capsule())
                    .padding(.bottom, 12)
            }
        }
        .containerBackground(for: .widget) {
            Image("CalmSceneBg")
                .resizable()
                .scaledToFill()
        }
        .widgetURL(entry.snapshot.nudge.target.url)
    }
}

struct CalmMinimalWidget: Widget {
    let kind = "PurrodoroCalmMinimalWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDashboardProvider()) { entry in
            CalmMinimalWidgetView(entry: entry)
        }
        .configurationDisplayName("Calm")
        .description("A quiet Purrodoro scene with one gentle action.")
        .supportedFamilies([.systemSmall])
    }
}
