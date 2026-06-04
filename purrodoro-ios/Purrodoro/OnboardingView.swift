//
//  OnboardingView.swift
//  Purrodoro
//
//  First-run intro shown full-screen over the app. Three gentle steps:
//  welcome → name → notification priming. Mirrors the spirit of the Android
//  onboarding (warm mascot, name step, permission ask) without blocking: every
//  step can be skipped and nothing here is required to use the app.
//

import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme

    /// Called when onboarding finishes (or is skipped) so the host can dismiss.
    let onFinish: () -> Void

    @State private var step: Step = .welcome
    @State private var requestingNotifications = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    enum Step: Int, CaseIterable {
        case welcome, name, notifications
    }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()

            VStack(spacing: 0) {
                skipBar

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

                primaryAction
                    .padding(.horizontal, 28)
                    .padding(.bottom, 12)
            }
        }
        .environment(\.pColors, colors)
    }

    // MARK: Steps

    private var welcomeStep: some View {
        stepScaffold(
            image: "CatMascot",
            title: loc.t("onboard.welcome.title"),
            subtitle: loc.t("onboard.welcome.subtitle")
        )
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
            .frame(width: 180, height: 180)
    }

    // MARK: Chrome

    private var skipBar: some View {
        HStack {
            Spacer()
            Button(loc.t("onboard.skip")) { finish() }
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textFaint)
        }
        .padding(.horizontal, 24)
        .padding(.top, 12)
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
    private var primaryAction: some View {
        switch step {
        case .welcome:
            PrimaryButton(title: loc.t("onboard.getStarted"), systemImage: "arrow.right", tint: Brand.coral) {
                advance(to: .name)
            }
        case .name:
            PrimaryButton(title: loc.t("onboard.continue"), systemImage: "arrow.right", tint: Brand.coral) {
                advance(to: .notifications)
            }
        case .notifications:
            PrimaryButton(
                title: requestingNotifications ? loc.t("onboard.enabling") : loc.t("onboard.enableNotif"),
                systemImage: "bell.fill",
                tint: Brand.coral
            ) {
                enableNotificationsThenFinish()
            }
            .disabled(requestingNotifications)
        }
    }

    // MARK: Actions

    private func advance(to next: Step) {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
            step = next
        }
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

#Preview {
    OnboardingView(onFinish: {})
        .environmentObject(PreferencesStore())
        .environmentObject(Localization())
}
