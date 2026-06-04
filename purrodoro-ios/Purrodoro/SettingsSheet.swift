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
    @EnvironmentObject private var prefs: PreferencesStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        sectionHeader("Profile")
                        nameCard

                        sectionHeader("Timer")
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

                        sectionHeader("Appearance")
                        appearanceCard
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

    private func sectionHeader(_ title: String) -> some View {
        Text(title.uppercased())
            .font(.system(.caption, design: .rounded).weight(.bold))
            .tracking(1.2)
            .foregroundStyle(colors.textFaint)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 4)
    }

    private var nameCard: some View {
        card {
            HStack(spacing: 12) {
                Image(systemName: "person.fill")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Brand.coral)
                    .frame(width: 28, height: 28)
                    .background(Brand.coral.opacity(0.14), in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text("Your name")
                        .font(.system(.caption, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                    TextField(
                        "Add your name",
                        text: Binding(
                            get: { prefs.userName },
                            set: { prefs.userName = $0 }
                        )
                    )
                    .font(.system(.headline, design: .rounded))
                    .foregroundStyle(colors.textPrimary)
                    .textInputAutocapitalization(.words)
                    .submitLabel(.done)
                }
            }
        }
    }

    private var appearanceCard: some View {
        card {
            VStack(alignment: .leading, spacing: 12) {
                Text("Theme")
                    .font(.system(.headline, design: .rounded))
                    .foregroundStyle(colors.textPrimary)
                HStack(spacing: 8) {
                    ForEach(AppearanceMode.allCases) { mode in
                        appearanceOption(mode)
                    }
                }
            }
        }
    }

    private func appearanceOption(_ mode: AppearanceMode) -> some View {
        let isSelected = prefs.appearanceMode == mode
        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                prefs.appearanceMode = mode
            }
        } label: {
            VStack(spacing: 6) {
                Image(systemName: mode.symbol)
                    .font(.system(size: 18, weight: .semibold))
                Text(mode.label)
                    .font(.system(.caption, design: .rounded).weight(.semibold))
            }
            .foregroundStyle(isSelected ? .white : colors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isSelected ? Brand.coral : colors.page)
            }
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(isSelected ? Color.clear : colors.border, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(mode.label)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }

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
    SettingsSheet()
        .environmentObject(TimerEngine())
        .environmentObject(PreferencesStore())
}
