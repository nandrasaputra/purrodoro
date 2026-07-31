//
//  ProfileView.swift
//  Purrodoro
//
//  Settings hub ported from Android `ProfileScreen.kt`. Hosts identity,
//  timer prefs, appearance, language, notifications, backup nav, and live auth.
//

import SwiftUI
import UIKit

struct ProfileView: View {
    @EnvironmentObject private var auth: AuthStore
    @EnvironmentObject private var ownerStore: AccountOwnerStore
    @EnvironmentObject private var syncRepo: SyncRepository
    @EnvironmentObject private var syncCoordinator: SyncCoordinator
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
    @State private var showLoginSheet = false
    @State private var showPremiumLoginSheet = false
    @State private var showPremiumView = false
    @State private var premiumUpsellTrigger: PremiumUpsellTrigger?
    @State private var pendingPremiumAccess: PremiumAccessRequest?
    @State private var showAccountSwitchAlert = false
    @State private var dropConfirmContinuation: CheckedContinuation<Bool, Never>?
    @State private var signingOut = false
    @State private var showUnsyncedLogoutAlert = false

    private var accountManager: AccountManager {
        AccountManager(
            ownerStore: ownerStore,
            clearer: AccountDataClearer(
                syncRepo: syncRepo,
                engine: engine,
                tasks: tasks,
                habits: habits,
                history: history,
                prefs: prefs
            ),
            syncHooks: syncCoordinator.syncHooks
        )
    }

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
            .sheet(isPresented: $showLoginSheet) {
                LoginSheet(
                    onSignedIn: { user in
                        showLoginSheet = false
                        Task {
                            await handleSignedIn(user)
                            await presentPendingPremiumAccessIfNeeded()
                        }
                    },
                    onContinueAsGuest: { showLoginSheet = false }
                )
            }
            .sheet(isPresented: $showPremiumLoginSheet) {
                PremiumLoginRequiredSheet(
                    onSignIn: {
                        showPremiumLoginSheet = false
                        pendingPremiumAccess = .discovery
                        showLoginSheet = true
                    },
                    onDismiss: { showPremiumLoginSheet = false }
                )
            }
            .sheet(isPresented: $showPremiumView) {
                PremiumView()
            }
            .sheet(item: $premiumUpsellTrigger) { trigger in
                PremiumUpsellSheet(
                    trigger: trigger,
                    isGuest: {
                        if case .guest = auth.state { return true }
                        return false
                    }(),
                    onDismiss: { premiumUpsellTrigger = nil },
                    onSignIn: {
                        pendingPremiumAccess = .upsell(trigger)
                        premiumUpsellTrigger = nil
                        showLoginSheet = true
                    },
                    onExplorePremium: {
                        premiumUpsellTrigger = nil
                        showPremiumView = true
                    }
                )
            }
            .alert(loc.t("account.switch.title"), isPresented: $showAccountSwitchAlert) {
                Button(loc.t("account.switch.confirm"), role: .destructive) {
                    dropConfirmContinuation?.resume(returning: true)
                    dropConfirmContinuation = nil
                }
                Button(loc.t("account.switch.cancel"), role: .cancel) {
                    dropConfirmContinuation?.resume(returning: false)
                    dropConfirmContinuation = nil
                }
            } message: {
                Text(loc.t("account.switch.message"))
            }
            .alert(loc.t("logout.unsynced.title"), isPresented: $showUnsyncedLogoutAlert) {
                Button(loc.t("logout.unsynced.confirm"), role: .destructive) {
                    Task { await confirmSignOutDespiteUnsynced() }
                }
                Button(loc.t("logout.unsynced.dismiss"), role: .cancel) {}
            } message: {
                Text(loc.t("logout.unsynced.body"))
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
                avatarView
                VStack(alignment: .leading, spacing: 4) {
                    Text(displayName)
                        .font(.system(.title3, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)
                    identityBadge
                }
                Spacer()
            }
        }
    }

    @ViewBuilder
    private var avatarView: some View {
        if case .signedIn(let user) = auth.state, let url = user.avatarURL {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    guestAvatarFallback
                }
            }
            .frame(width: 56, height: 56)
            .clipShape(Circle())
            .overlay(Circle().stroke(colors.border, lineWidth: 1))
        } else {
            guestAvatarFallback
        }
    }

    private var guestAvatarFallback: some View {
        Image("CatMascot")
            .resizable()
            .scaledToFill()
            .frame(width: 56, height: 56)
            .clipShape(Circle())
            .overlay(Circle().stroke(colors.border, lineWidth: 1))
    }

    @ViewBuilder
    private var identityBadge: some View {
        switch auth.state {
        case .signedIn(let user):
            if let email = user.email {
                Text(email)
                    .font(.system(.caption, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .lineLimit(1)
            }
            Text(providerLabel(user.provider))
                .font(.system(.caption, design: .rounded).weight(.semibold))
                .foregroundStyle(Brand.cocoa)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Brand.peach.opacity(0.55), in: Capsule())
        case .guest, .loading:
            Text(loc.t("profile.guest"))
                .font(.system(.caption, design: .rounded).weight(.semibold))
                .foregroundStyle(Brand.cocoa)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Brand.peach.opacity(0.55), in: Capsule())
        }
    }

    private func providerLabel(_ provider: AuthProvider) -> String {
        switch provider {
        case .google: return loc.t("account.signedInGoogle")
        case .apple: return loc.t("account.signedInApple")
        case .unknown: return loc.t("account.signedInGeneric")
        }
    }

    private var displayName: String {
        let local = prefs.userName
        let resolved = resolvedDisplayName(authState: auth.state, localName: local)
        if !resolved.isEmpty { return resolved }
        return loc.t("profile.friend")
    }

    // MARK: - Premium

    private var isGuest: Bool {
        if case .guest = auth.state { return true }
        return false
    }

    private var premiumBanner: some View {
        Button {
            openPremiumDiscovery()
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
                    Text(
                        prefs.isPremium
                            ? loc.t("profile.premium.activeSubtitle")
                            : loc.t("profile.premium.subtitle")
                    )
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
                    colors: prefs.isPremium
                        ? [Brand.premiumPurpleTop, Brand.premiumPurpleBottom]
                        : [Brand.coral, Brand.coral.opacity(0.82)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: RoundedRectangle(cornerRadius: 18, style: .continuous)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(loc.t("profile.premium.explore"))
    }

    private func openPremiumDiscovery() {
        if isGuest {
            showPremiumLoginSheet = true
        } else {
            showPremiumView = true
        }
    }

    private func presentPendingPremiumAccessIfNeeded() async {
        guard let pending = pendingPremiumAccess else { return }
        pendingPremiumAccess = nil
        // Wait a beat so login sheet dismiss animation can finish.
        try? await Task.sleep(for: .milliseconds(350))
        switch pending {
        case .discovery:
            showPremiumView = true
        case .upsell(let trigger):
            premiumUpsellTrigger = trigger
        }
    }

    // MARK: - Account

    private var accountCard: some View {
        card {
            switch auth.state {
            case .loading:
                HStack {
                    ProgressView()
                    Text(loc.t("account.loading"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                }
            case .guest:
                VStack(alignment: .leading, spacing: 12) {
                    Text(loc.t("profile.guestHint"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Button { showLoginSheet = true } label: {
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
            case .signedIn:
                VStack(alignment: .leading, spacing: 12) {
                    SyncStatusMiniPanel(statusStore: syncCoordinator.statusStore, loc: loc)
                    Text(loc.t("account.signedInHint"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                    Button {
                        Task { await signOut() }
                    } label: {
                        HStack(spacing: 8) {
                            if signingOut { ProgressView().tint(Brand.coral) }
                            Text(loc.t("account.signOut"))
                        }
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(Brand.coral)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Brand.coral.opacity(0.12), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(signingOut)
                }
            }
        }
    }

    @MainActor
    private func handleSignedIn(_ user: AuthUser) async {
        let result = await accountManager.onSignedIn(
            incomingUserId: user.userId,
            confirmDrop: {
                await withCheckedContinuation { continuation in
                    dropConfirmContinuation = continuation
                    showAccountSwitchAlert = true
                }
            },
            onBeforeClear: {
                if engine.isStarted { engine.stop() }
            }
        )
        if result == .dropCancelled {
            try? await auth.signOut()
        }
    }

    @MainActor
    private func signOut() async {
        signingOut = true
        defer { signingOut = false }
        switch await syncCoordinator.flushBeforeLogout() {
        case .signOut:
            try? await auth.signOut()
        case .warnUnsynced:
            showUnsyncedLogoutAlert = true
        }
    }

    @MainActor
    private func confirmSignOutDespiteUnsynced() async {
        signingOut = true
        defer { signingOut = false }
        try? await auth.signOut()
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
                    if isNameFromAccount(authState: auth.state, localName: prefs.userName) {
                        Text(prefs.trimmedName ?? "")
                            .font(.system(.headline, design: .rounded))
                            .foregroundStyle(colors.textPrimary)
                    } else {
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

private struct SyncStatusMiniPanel: View {
    @ObservedObject var statusStore: SyncStatusStore
    let loc: Localization
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let presentation = resolveSyncStatusPresentation(statusStore.snapshot)
        let labels = resolveSyncStatusLabels(
            presentation,
            nowMillis: Int64(Date().timeIntervalSince1970 * 1000)
        )
        let colors = PurrodoroColors(scheme: scheme)
        let accent: Color = {
            if labels.accentIsDestructive { return Brand.coral }
            if labels.accentIsMuted { return colors.textSecondary }
            return Brand.mint
        }()

        HStack(spacing: 12) {
            Image(systemName: "icloud.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(accent)
            VStack(alignment: .leading, spacing: 2) {
                Text(loc.t(labels.titleKey))
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                if let subtitleKey = labels.subtitleKey, let relativeKey = labels.relativeTimeKey {
                    let relative = relativeLabel(relativeKey: relativeKey, value: labels.relativeTimeValue, loc: loc)
                    Text(loc.t(subtitleKey, relative))
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                }
            }
            Spacer()
        }
        .padding(12)
        .background(accent.opacity(0.08), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func relativeLabel(relativeKey: String, value: Int?, loc: Localization) -> String {
        if let value {
            return String(format: loc.t(relativeKey), locale: Locale.current, value)
        }
        return loc.t(relativeKey)
    }
}

#Preview {
    ProfileView()
        .environmentObject(AuthStore())
        .environmentObject(AccountOwnerStore())
        .environmentObject(TimerEngine())
        .environmentObject(PreferencesStore())
        .environmentObject(Localization())
}
