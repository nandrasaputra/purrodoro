//
//  ProfileView.swift
//  Purrodoro
//
//  Settings hub ported from Android `ProfileScreen.kt`. Hosts identity,
//  timer prefs, appearance, language, notifications, and stub rows for
//  premium / backup / storage until those slices land.
//

import SwiftUI
import UIKit

struct ProfileView: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @EnvironmentObject private var habits: HabitStore
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var sounds: SoundsStore
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase

    @State private var notificationsEnabled = false
    @State private var showComingSoonAlert = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        identityCard
                        premiumBanner

                        sectionHeader(loc.t("profile.account"))
                        accountCard

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

                        sectionHeader(loc.t("profile.notifications"))
                        notificationsCard

                        sectionHeader(loc.t("profile.data"))
                        dataRows

                        sectionHeader(loc.t("profile.about"))
                        aboutCard
                    }
                    .padding(20)
                }
            }
            .navigationTitle(loc.t("profile.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc.t("settings.done")) { dismiss() }
                        .font(.system(.body, design: .rounded).weight(.semibold))
                }
            }
            .alert(loc.t("profile.comingSoon"), isPresented: $showComingSoonAlert) {
                Button(loc.t("common.ok"), role: .cancel) {}
            }
        }
        .task { await refreshNotificationStatus() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                Task { await refreshNotificationStatus() }
            }
        }
    }

    // MARK: - Identity

    private var identityCard: some View {
        card {
            HStack(spacing: 14) {
                Image("CatMascot")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 56, height: 56)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(colors.border, lineWidth: 1))

                VStack(alignment: .leading, spacing: 4) {
                    Text(displayName)
                        .font(.system(.title3, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)
                    Text(loc.t("profile.guest"))
                        .font(.system(.caption, design: .rounded).weight(.semibold))
                        .foregroundStyle(Brand.cocoa)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Brand.peach.opacity(0.55), in: Capsule())
                }
                Spacer()
            }
        }
    }

    private var displayName: String {
        if let name = prefs.trimmedName { return name }
        return loc.t("profile.friend")
    }

    // MARK: - Premium stub

    private var premiumBanner: some View {
        Button {
            showComingSoonAlert = true
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "sparkles")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(.white.opacity(0.2), in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(loc.t("profile.premium.title"))
                        .font(.system(.headline, design: .rounded).weight(.bold))
                        .foregroundStyle(.white)
                    Text(loc.t("profile.premium.subtitle"))
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(.white.opacity(0.88))
                        .multilineTextAlignment(.leading)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.9))
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                LinearGradient(
                    colors: [Brand.coral, Brand.coral.opacity(0.82)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: RoundedRectangle(cornerRadius: 18, style: .continuous)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(loc.t("profile.premium.explore"))
    }

    // MARK: - Account stub

    private var accountCard: some View {
        card {
            VStack(alignment: .leading, spacing: 12) {
                Text(loc.t("profile.guestHint"))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                Button {
                    showComingSoonAlert = true
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "person.crop.circle.badge.plus")
                        Text(loc.t("profile.signIn"))
                    }
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Brand.coral, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Notifications

    private var notificationsCard: some View {
        card {
            if notificationsEnabled {
                HStack(spacing: 12) {
                    Image(systemName: "bell.badge.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Brand.coral)
                    Text(loc.t("profile.notificationsOn"))
                        .font(.system(.subheadline, design: .rounded).weight(.medium))
                        .foregroundStyle(colors.textPrimary)
                    Spacer()
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Brand.coral)
                }
            } else {
                VStack(alignment: .leading, spacing: 12) {
                    Label(loc.t("profile.notificationsOff"), systemImage: "bell.slash")
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                    Text(loc.t("profile.notificationsOffHint"))
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                    Button {
                        openURL(URL(string: UIApplication.openSettingsURLString)!)
                    } label: {
                        Text(loc.t("profile.openSettings"))
                            .font(.system(.subheadline, design: .rounded).weight(.semibold))
                            .foregroundStyle(Brand.coral)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Brand.coral.opacity(0.12), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Data stubs

    private var dataRows: some View {
        VStack(spacing: 0) {
            NavigationLink {
                BackupRestoreView()
            } label: {
                dataRowLabel(icon: "arrow.triangle.2.circlepath", title: loc.t("profile.backup"), badge: nil)
            }
            .buttonStyle(.plain)
            divider
            Button {
                showComingSoonAlert = true
            } label: {
                dataRowLabel(
                    icon: "internaldrive",
                    title: loc.t("profile.storage"),
                    badge: loc.t("profile.comingSoon")
                )
            }
            .buttonStyle(.plain)
        }
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func dataRowLabel(icon: String, title: String, badge: String?) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(colors.textFaint)
                .frame(width: 28)
            Text(title)
                .font(.system(.body, design: .rounded).weight(.medium))
                .foregroundStyle(colors.textPrimary)
            Spacer()
            if let badge {
                Text(badge)
                    .font(.system(.caption2, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textFaint)
            }
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(colors.textFaint)
        }
        .padding(16)
        .contentShape(Rectangle())
    }

    // MARK: - About

    private var aboutCard: some View {
        card {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "info.circle")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(colors.textFaint)
                        .frame(width: 28)
                    Text(loc.t("profile.version", appVersion))
                        .font(.system(.body, design: .rounded).weight(.medium))
                        .foregroundStyle(colors.textPrimary)
                    Spacer()
                }
                .padding(.vertical, 4)

                divider.padding(.vertical, 8)

                Button {
                    openURL(AppLinks.privacyPolicy)
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "hand.raised")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(colors.textFaint)
                            .frame(width: 28)
                        Text(loc.t("profile.privacy"))
                            .font(.system(.body, design: .rounded).weight(.medium))
                            .foregroundStyle(colors.textPrimary)
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(colors.textFaint)
                    }
                    .padding(.vertical, 4)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Settings cards (from SettingsSheet)

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
                        divider
                    }
                }
            }
        }
    }

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
                ) { EmptyView() }
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

    private var divider: some View {
        Rectangle().fill(colors.border).frame(height: 1)
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func refreshNotificationStatus() async {
        notificationsEnabled = await NotificationScheduler.shared.notificationsEnabled()
    }
}

#Preview {
    ProfileView()
        .environmentObject(TimerEngine())
        .environmentObject(PreferencesStore())
        .environmentObject(Localization())
}
