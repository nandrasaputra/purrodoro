//
//  SettingsSheet.swift
//  Purrodoro
//
//  Adjusts the cycle: focus / break durations, focus sessions per cycle, and
//  whether the next segment auto-starts. Edits flow straight into the engine's
//  `TimerSettings`, which persists them and refreshes the idle preview.
//

import SwiftUI

struct SettingsSheet: View {
    @EnvironmentObject private var engine: TimerEngine
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        durationCard(
                            title: "Focus",
                            mode: .focus,
                            minutes: engine.settings.focusMinutes,
                            step: 5
                        )
                        durationCard(
                            title: "Short break",
                            mode: .shortBreak,
                            minutes: engine.settings.shortBreakMinutes,
                            step: 1
                        )
                        durationCard(
                            title: "Long break",
                            mode: .longBreak,
                            minutes: engine.settings.longBreakMinutes,
                            step: 5
                        )

                        cycleCard
                        autoStartCard
                    }
                    .padding(20)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .font(.system(.body, design: .rounded).weight(.semibold))
                }
            }
        }
    }

    // MARK: Cards

    private func durationCard(title: String, mode: TimerMode, minutes: Int, step: Int) -> some View {
        card {
            HStack {
                Circle().fill(mode.accent).frame(width: 12, height: 12)
                Text(title)
                    .font(.system(.headline, design: .rounded))
                    .foregroundStyle(colors.textPrimary)
                Spacer()
                Stepper(
                    value: Binding(
                        get: { minutes },
                        set: { engine.settings = engine.settings.updating(mode, minutes: $0) }
                    ),
                    in: TimerSettings.minMinutes...TimerSettings.maxMinutes,
                    step: step
                ) {
                    Text("\(minutes) min")
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textSecondary)
                        .monospacedDigit()
                }
                .labelsHidden()
                Text("\(minutes) min")
                    .font(.system(.body, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .monospacedDigit()
                    .frame(width: 64, alignment: .trailing)
            }
        }
    }

    private var cycleCard: some View {
        card {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Focus sessions")
                        .font(.system(.headline, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                    Text("Before a long break")
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
                Spacer()
                Stepper(
                    value: Binding(
                        get: { engine.settings.focusSessionsPerCycle },
                        set: { engine.settings = engine.settings.updatingCycleSize($0) }
                    ),
                    in: 1...TimerSettings.maxFocusSessionsPerCycle
                ) { EmptyView() }
                .labelsHidden()
                Text("\(engine.settings.focusSessionsPerCycle)")
                    .font(.system(.body, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .monospacedDigit()
                    .frame(width: 28, alignment: .trailing)
            }
        }
    }

    private var autoStartCard: some View {
        card {
            Toggle(isOn: Binding(
                get: { engine.settings.autoStartNextSession },
                set: { engine.settings.autoStartNextSession = $0 }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Auto-start next")
                        .font(.system(.headline, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                    Text("Flow from focus to break automatically")
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
            }
            .tint(Brand.coral)
        }
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(colors.border, lineWidth: 1))
    }
}

#Preview {
    SettingsSheet().environmentObject(TimerEngine())
}
