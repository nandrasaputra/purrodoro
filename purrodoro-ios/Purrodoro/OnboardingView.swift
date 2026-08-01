//
//  OnboardingView.swift
//  Purrodoro
//
//  First-run intro shown full-screen over the app. Welcome offers Sign in
//  (LoginSheet) or Continue as guest — matching Android `OnboardingScreen` —
//  then optional name + notification priming. Language can be changed from the
//  top bar. Every step can be skipped; nothing here is required to use the app.
//

import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var loc: Localization
    @EnvironmentObject private var auth: AuthStore
    @EnvironmentObject private var ownerStore: AccountOwnerStore
    @EnvironmentObject private var syncRepo: SyncRepository
    @EnvironmentObject private var syncCoordinator: SyncCoordinator
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @EnvironmentObject private var habits: HabitStore
    @EnvironmentObject private var history: SessionHistoryStore
    @Environment(\.colorScheme) private var scheme

    /// Called when onboarding finishes (or is skipped) so the host can dismiss.
    let onFinish: () -> Void

    @State private var step: Step = .welcome
    @State private var requestingNotifications = false
    @State private var showLoginSheet = false
    @State private var showAccountSwitchAlert = false
    @State private var dropConfirmContinuation: CheckedContinuation<Bool, Never>?
    @State private var authBusy = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    private var isSignedIn: Bool {
        if case .signedIn = auth.state { return true }
        return false
    }

    private var signedInUser: AuthUser? {
        if case .signedIn(let user) = auth.state { return user }
        return nil
    }

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

    enum Step: Int, CaseIterable {
        case welcome, name, notifications
    }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()

            VStack(spacing: 0) {
                topBar

                Spacer(minLength: 0)

                Group {
                    switch step {
                    case .welcome: welcomeStep
                    case .name: nameStep
                    case .notifications: notificationsStep
                    }
                }
                .transition(.asymmetric(
                    insertion: .move(edge: .trailing).combined(with: .opacity),
                    removal: .move(edge: .leading).combined(with: .opacity)
                ))

                Spacer(minLength: 0)

                progressDots
                    .padding(.bottom, 16)

                bottomActions
                    .padding(.horizontal, 28)
                    .padding(.bottom, 12)
            }

            if authBusy {
                Color.black.opacity(0.18).ignoresSafeArea()
                ProgressView()
                    .tint(Brand.coral)
                    .scaleEffect(1.2)
            }
        }
        .environment(\.pColors, colors)
        .sheet(isPresented: $showLoginSheet) {
            LoginSheet(
                onSignedIn: { user in
                    showLoginSheet = false
                    Task { await handleSignedIn(user) }
                },
                onContinueAsGuest: {
                    showLoginSheet = false
                }
            )
            .environmentObject(auth)
            .environmentObject(loc)
        }
        .alert(loc.t("account.switch.title"), isPresented: $showAccountSwitchAlert) {
            Button(loc.t("account.switch.confirm"), role: .destructive) {
                dropConfirmContinuation?.resume(returning: true)
                dropConfirmContinuation = nil
            }
            Button(loc.t("common.cancel"), role: .cancel) {
                dropConfirmContinuation?.resume(returning: false)
                dropConfirmContinuation = nil
            }
        } message: {
            Text(loc.t("account.switch.message"))
        }
    }

    // MARK: Steps

    private var welcomeStep: some View {
        VStack(spacing: 18) {
            Text(loc.t("onboard.welcomeTo"))
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textSecondary)
            Text("Purrodoro")
                .font(.system(size: 40, weight: .bold, design: .rounded))
                .foregroundStyle(colors.textPrimary)

            mascot("CatMascot")

            VStack(spacing: 8) {
                Text(loc.t("onboard.welcome.title"))
                    .font(.system(.title2, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .multilineTextAlignment(.center)
                Text(loc.t(isSignedIn ? "onboard.welcome.signedInBody" : "onboard.welcome.subtitle"))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 8)
            }

            if let user = signedInUser {
                signedInCard(user)
            }
        }
        .padding(.horizontal, 28)
    }

    private var nameStep: some View {
        VStack(spacing: 22) {
            mascot("CatReady")
            VStack(spacing: 8) {
                Text(loc.t("onboard.name.title"))
                    .font(.system(.title, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .multilineTextAlignment(.center)
                Text(loc.t("onboard.name.subtitle"))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .multilineTextAlignment(.center)
            }
            TextField(
                loc.t("settings.yourName"),
                text: Binding(get: { prefs.userName }, set: { prefs.userName = $0 })
            )
            .font(.system(.title3, design: .rounded).weight(.semibold))
            .multilineTextAlignment(.center)
            .foregroundStyle(colors.textPrimary)
            .textInputAutocapitalization(.words)
            .submitLabel(.done)
            .padding(.vertical, 14)
            .padding(.horizontal, 18)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
            .padding(.horizontal, 28)
        }
        .padding(.horizontal, 24)
    }

    private var notificationsStep: some View {
        stepScaffold(
            image: "CatCelebrate",
            title: loc.t("onboard.notif.title"),
            subtitle: loc.t("onboard.notif.subtitle")
        )
    }

    private func stepScaffold(image: String, title: String, subtitle: String) -> some View {
        VStack(spacing: 22) {
            mascot(image)
            VStack(spacing: 10) {
                Text(title)
                    .font(.system(.title, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .multilineTextAlignment(.center)
                Text(subtitle)
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 12)
            }
        }
        .padding(.horizontal, 36)
    }

    private func mascot(_ name: String) -> some View {
        Image(name)
            .resizable()
            .scaledToFit()
            .frame(width: 160, height: 160)
    }

    private func signedInCard(_ user: AuthUser) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(Brand.coral)
            VStack(alignment: .leading, spacing: 2) {
                Text(loc.t("onboard.welcome.signedInHeading"))
                    .font(.system(.subheadline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                Text(user.displayName?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                     ?? user.email
                     ?? loc.t("account.signedInGeneric"))
                    .font(.system(.caption, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    // MARK: Chrome

    private var topBar: some View {
        HStack {
            if step == .notifications || step == .name {
                Button {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        step = step == .notifications ? .name : .welcome
                    }
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(colors.textPrimary)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(loc.t("common.back"))
            }

            Spacer()

            languageMenu

            Button(loc.t("onboard.skip")) { finish() }
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textFaint)
                .disabled(authBusy)
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
    }

    private var languageMenu: some View {
        Menu {
            ForEach(AppLanguage.allCases) { lang in
                Button {
                    loc.language = lang
                } label: {
                    HStack {
                        Text(languageLabel(lang))
                        if loc.language == lang {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: 4) {
                Image(systemName: "globe")
                    .font(.system(size: 14, weight: .semibold))
                Text(languageLabel(loc.language))
                    .font(.system(.caption, design: .rounded).weight(.semibold))
                    .lineLimit(1)
                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .bold))
            }
            .foregroundStyle(colors.textSecondary)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(colors.card, in: Capsule())
            .overlay(Capsule().stroke(colors.border, lineWidth: 1))
        }
        .accessibilityLabel(loc.t("settings.language"))
    }

    private func languageLabel(_ lang: AppLanguage) -> String {
        switch lang {
        case .system:
            return loc.t("settings.languageSystemPicker", AppLanguage.deviceLanguageEndonym)
        case .en: return "English"
        case .id: return "Bahasa Indonesia"
        }
    }

    private var progressDots: some View {
        HStack(spacing: 8) {
            ForEach(Step.allCases, id: \.rawValue) { s in
                Capsule()
                    .fill(s == step ? Brand.coral : colors.border)
                    .frame(width: s == step ? 22 : 8, height: 8)
                    .animation(.spring(response: 0.3, dampingFraction: 0.8), value: step)
            }
        }
    }

    @ViewBuilder
    private var bottomActions: some View {
        switch step {
        case .welcome:
            VStack(spacing: 10) {
                if isSignedIn {
                    PrimaryButton(title: loc.t("onboard.continue"), systemImage: "arrow.right", tint: Brand.coral) {
                        advance(to: .name)
                    }
                    .disabled(authBusy)
                    Button(loc.t("onboard.continueGuestInstead")) {
                        Task { await continueAsGuestInstead() }
                    }
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(Brand.coral)
                    .disabled(authBusy)
                } else {
                    PrimaryButton(title: loc.t("onboard.signIn"), systemImage: "person.crop.circle.badge.checkmark", tint: Brand.coral) {
                        showLoginSheet = true
                    }
                    .disabled(authBusy)
                    Button(loc.t("onboard.continueGuest")) {
                        advance(to: .name)
                    }
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(Brand.coral)
                    .disabled(authBusy)
                }
            }
        case .name:
            PrimaryButton(title: loc.t("onboard.continue"), systemImage: "arrow.right", tint: Brand.coral) {
                advance(to: .notifications)
            }
        case .notifications:
            VStack(spacing: 10) {
                PrimaryButton(
                    title: requestingNotifications ? loc.t("onboard.enabling") : loc.t("onboard.enableNotif"),
                    systemImage: "bell.fill",
                    tint: Brand.coral
                ) {
                    enableNotificationsThenFinish()
                }
                .disabled(requestingNotifications)
                Button(loc.t("onboard.notNow")) { finish() }
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textSecondary)
                    .disabled(requestingNotifications)
            }
        }
    }

    // MARK: Actions

    private func advance(to next: Step) {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
            step = next
        }
    }

    @MainActor
    private func handleSignedIn(_ user: AuthUser) async {
        authBusy = true
        defer { authBusy = false }
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
            return
        }
        if prefs.trimmedName == nil,
           let name = user.displayName?.trimmingCharacters(in: .whitespacesAndNewlines),
           !name.isEmpty {
            prefs.userName = name
        }
        advance(to: .name)
    }

    @MainActor
    private func continueAsGuestInstead() async {
        authBusy = true
        defer { authBusy = false }
        try? await auth.signOut()
        advance(to: .name)
    }

    private func enableNotificationsThenFinish() {
        requestingNotifications = true
        Task {
            await NotificationScheduler.shared.requestAuthorization()
            requestingNotifications = false
            finish()
        }
    }

    private func finish() {
        prefs.hasCompletedOnboarding = true
        onFinish()
    }
}

private extension String {
    var nilIfEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

#Preview {
    OnboardingView(onFinish: {})
        .environmentObject(PreferencesStore())
        .environmentObject(Localization())
        .environmentObject(AuthStore())
        .environmentObject(AccountOwnerStore())
        .environmentObject(SyncRepository())
        .environmentObject(SyncCoordinator(
            auth: AuthStore(),
            ownerStore: AccountOwnerStore(),
            syncRepo: SyncRepository(),
            prefs: PreferencesStore(),
            timerEngine: TimerEngine()
        ))
        .environmentObject(TimerEngine())
        .environmentObject(TaskStore())
        .environmentObject(HabitStore())
        .environmentObject(SessionHistoryStore())
}
