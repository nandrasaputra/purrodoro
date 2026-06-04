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
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        sectionHeader(loc.t("settings.profile"))
                        nameCard

                        sectionHeader(loc.t("settings.timer"))
                        durationCard(
                            title: loc.t("settings.focus"),
                            mode: .focus,
                            minutes: engine.settings.focusMinutes,
                            step: 5
                        )
                        durationCard(
                            title: loc.t("settings.shortBreak"),
                            mode: .shortBreak,
                            minutes: engine.settings.shortBreakMinutes,
                            step: 1
                        )
                        durationCard(
                            title: loc.t("settings.longBreak"),
                            mode: .longBreak,
                            minutes: engine.settings.longBreakMinutes,
                            step: 5
                        )

                        cycleCard
                        autoStartCard

                        sectionHeader(loc.t("settings.appearance"))
                        appearanceCard

                        sectionHeader(loc.t("settings.language"))
                        languageCard
                    }
                    .padding(20)
                }
            }
            .navigationTitle(loc.t("settings.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc.t("settings.done")) { dismiss() }
                        .font(.system(.body, design: .rounded).weight(.semibold))
                }
            }
        }
    }

    private var languageCard: some View {
        card {
            VStack(spacing: 8) {
                ForEach(AppLanguage.allCases) { lang in
                    let selected = loc.language == lang
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { loc.language = lang }
                    } label: {
                        HStack(spacing: 12) {
                            Text(lang.flag).font(.system(size: 20))
                            Text(lang.nativeLabel)
                                .font(.system(.body, design: .rounded).weight(.medium))
                                .foregroundStyle(colors.textPrimary)
                            Spacer()
                            if selected {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(Brand.coral)
                            }
                        }
                        .padding(.vertical, 6)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    if lang != AppLanguage.allCases.last {
                        Rectangle().fill(colors.border).frame(height: 1)
                    }
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
                    Text(loc.t("settings.yourName"))
                        .font(.system(.caption, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                    TextField(
                        loc.t("settings.addName"),
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
                Text(loc.t("settings.theme"))
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
                Text(loc.t("appearance.\(mode.rawValue)"))
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
                    Text(loc.t("unit.min", minutes))
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textSecondary)
                        .monospacedDigit()
                }
                .labelsHidden()
                Text(loc.t("unit.min", minutes))
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
                    Text(loc.t("settings.focusSessions"))
                        .font(.system(.headline, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                    Text(loc.t("settings.beforeLongBreak"))
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
                    Text(loc.t("settings.autoStart"))
                        .font(.system(.headline, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                    Text(loc.t("settings.autoStartDesc"))
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
        .environmentObject(Localization())
}
