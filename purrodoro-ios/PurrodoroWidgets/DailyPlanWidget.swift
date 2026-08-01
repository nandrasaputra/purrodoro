//
//  DailyPlanWidget.swift
//  PurrodoroWidgets
//
//  Pending habit/task chips with a Start Focus CTA (Android `DailyPlanWidget`).
//

import SwiftUI
import WidgetKit

struct DailyPlanWidgetView: View {
    let entry: WidgetDashboardEntry
    private var snap: WidgetDashboardSnapshot { entry.snapshot }
    private var chips: [WidgetChipSnapshot] { snap.planChips(max: 3) }
    private var overflow: Int {
        max((snap.pendingHabits.count + snap.pendingTasks.count) - chips.count, 0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Today's plan")
                    .font(.system(.caption, design: .rounded).weight(.bold))
                    .foregroundStyle(WidgetBrand.cocoa)
                Spacer()
                Image("CatPeek")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 36, height: 36)
            }

            if chips.isEmpty {
                Spacer(minLength: 0)
                Text("Nothing waiting — enjoy the calm.")
                    .font(.system(.caption, design: .rounded))
                    .foregroundStyle(.secondary)
                Spacer(minLength: 0)
            } else {
                ForEach(chips) { chip in
                    HStack(spacing: 8) {
                        Text(chip.emoji)
                        Text(chip.label)
                            .font(.system(.caption, design: .rounded).weight(.semibold))
                            .foregroundStyle(WidgetBrand.cocoa)
                            .lineLimit(1)
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.55), in: Capsule())
                }
                if overflow > 0 {
                    Text("+\(overflow) more")
                        .font(.system(.caption2, design: .rounded).weight(.medium))
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
            }

            Text("Start Focus")
                .font(.system(.caption, design: .rounded).weight(.bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(WidgetBrand.coral, in: Capsule())
        }
        .containerBackground(WidgetBrand.mint.opacity(0.22), for: .widget)
        .widgetURL(WidgetNudgeTarget.startFocus.url)
    }
}

struct DailyPlanWidget: Widget {
    let kind = "PurrodoroDailyPlanWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDashboardProvider()) { entry in
            DailyPlanWidgetView(entry: entry)
        }
        .configurationDisplayName("Today's plan")
        .description("Habits and tasks waiting, with a Start Focus button.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}
