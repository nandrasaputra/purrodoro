//
//  FocusLiveActivity.swift
//  PurrodoroWidgets
//
//  Lock Screen + Dynamic Island presentation for the running Purrodoro timer,
//  driven by `FocusActivityAttributes` updates sent from the app.
//

#if canImport(ActivityKit)
import ActivityKit
import SwiftUI
import WidgetKit

// MARK: - Shared bits

private func widgetClock(_ totalSeconds: Int) -> String {
    let s = max(totalSeconds, 0)
    return String(format: "%02d:%02d", s / 60, s % 60)
}

private func modeSymbol(_ raw: String) -> String {
    switch raw {
    case "shortBreak": return "cup.and.saucer.fill"
    case "longBreak": return "moon.zzz.fill"
    default: return "timer"
    }
}

/// Countdown label: live `Text(timerInterval:)` while running, static when paused.
private struct CountdownLabel: View {
    let state: FocusActivityAttributes.ContentState
    var font: Font = .system(.title2, design: .rounded).weight(.bold)

    var body: some View {
        Group {
            if state.isRunning {
                Text(timerInterval: state.segmentStart...state.endsAt, countsDown: true)
                    .monospacedDigit()
            } else {
                Text(widgetClock(state.pausedRemaining))
                    .monospacedDigit()
            }
        }
        .font(font)
    }
}

private struct CycleDots: View {
    let completed: Int
    let total: Int
    let tint: Color

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<max(total, 1), id: \.self) { i in
                Circle()
                    .fill(i < completed ? tint : tint.opacity(0.25))
                    .frame(width: 6, height: 6)
            }
        }
    }
}

// MARK: - Lock Screen / banner

private struct LockScreenLiveActivityView: View {
    let context: ActivityViewContext<FocusActivityAttributes>

    private var state: FocusActivityAttributes.ContentState { context.state }
    private var tint: Color { WidgetBrand.accent(forMode: state.modeRaw) }

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle().fill(tint.opacity(0.18))
                Image(systemName: modeSymbol(state.modeRaw))
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(tint)
            }
            .frame(width: 52, height: 52)

            VStack(alignment: .leading, spacing: 3) {
                Text(state.modeLabel.uppercased())
                    .font(.system(.caption2, design: .rounded).weight(.bold))
                    .tracking(1)
                    .foregroundStyle(tint)
                if let intent = state.intentName, !intent.isEmpty {
                    Text(intent)
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .lineLimit(1)
                } else {
                    Text(state.isRunning ? "In focus" : "Paused")
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(.secondary)
                }
                CycleDots(completed: state.completedFocus, total: state.totalFocus, tint: tint)
                    .padding(.top, 1)
            }

            Spacer(minLength: 8)

            CountdownLabel(state: state, font: .system(.title, design: .rounded).weight(.bold))
                .foregroundStyle(.primary)
                .frame(minWidth: 78, alignment: .trailing)
        }
        .padding(16)
        .activityBackgroundTint(Color.black.opacity(0.04))
        .activitySystemActionForegroundColor(tint)
    }
}

// MARK: - Widget

struct FocusLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: FocusActivityAttributes.self) { context in
            LockScreenLiveActivityView(context: context)
        } dynamicIsland: { context in
            let state = context.state
            let tint = WidgetBrand.accent(forMode: state.modeRaw)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label {
                        Text(state.modeLabel)
                            .font(.system(.caption, design: .rounded).weight(.bold))
                    } icon: {
                        Image(systemName: modeSymbol(state.modeRaw))
                            .foregroundStyle(tint)
                    }
                    .padding(.leading, 4)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    CountdownLabel(state: state)
                        .foregroundStyle(tint)
                        .padding(.trailing, 4)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        if let intent = state.intentName, !intent.isEmpty {
                            Text(intent)
                                .font(.system(.footnote, design: .rounded))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        } else {
                            Text(state.isRunning ? "Stay with it" : "Paused")
                                .font(.system(.footnote, design: .rounded))
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        CycleDots(completed: state.completedFocus, total: state.totalFocus, tint: tint)
                    }
                    .padding(.horizontal, 4)
                }
            } compactLeading: {
                Image(systemName: modeSymbol(state.modeRaw))
                    .foregroundStyle(tint)
            } compactTrailing: {
                CountdownLabel(state: state, font: .system(.body, design: .rounded).weight(.semibold))
                    .foregroundStyle(tint)
                    .frame(maxWidth: 56)
            } minimal: {
                Image(systemName: modeSymbol(state.modeRaw))
                    .foregroundStyle(tint)
            }
            .keylineTint(tint)
        }
    }
}
#endif
