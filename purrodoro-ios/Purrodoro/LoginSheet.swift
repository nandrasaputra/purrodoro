//
//  LoginSheet.swift
//  Purrodoro
//
//  Sign-in sheet — Google + Apple + guest. Mirrors Android `LoginSheet.kt`.
//

import SwiftUI
import AuthenticationServices
import GoogleSignIn

struct LoginSheet: View {
    @EnvironmentObject private var auth: AuthStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    let onSignedIn: (AuthUser) -> Void
    let onContinueAsGuest: () -> Void

    @State private var isSigningIn = false
    @State private var errorMessage: String?

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 20) {
                        Text(loc.t("login.title"))
                            .font(.system(.title2, design: .rounded).weight(.bold))
                            .foregroundStyle(colors.textPrimary)
                            .multilineTextAlignment(.center)

                        Text(loc.t("login.subtitle"))
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundStyle(colors.textSecondary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: 280)

                        Image("CatMascot")
                            .resizable()
                            .scaledToFit()
                            .frame(height: 92)
                            .accessibilityLabel(loc.t("login.catA11y"))

                        googleButton

                        SignInWithAppleButton(.signIn) { request in
                            request.requestedScopes = [.fullName, .email]
                        } onCompletion: { result in
                            handleAppleResult(result)
                        }
                        .signInWithAppleButtonStyle(scheme == .dark ? .white : .black)
                        .frame(height: 50)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .disabled(isSigningIn)

                        if isSigningIn {
                            ProgressView()
                                .tint(Brand.coral)
                        }

                        if let errorMessage {
                            Text(errorMessage)
                                .font(.system(.caption, design: .rounded))
                                .foregroundStyle(Brand.coral)
                                .multilineTextAlignment(.center)
                        }

                        Button(loc.t("login.continueGuest")) {
                            onContinueAsGuest()
                            dismiss()
                        }
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(Brand.coral)
                        .disabled(isSigningIn)

                        Text(loc.t("login.guestHint"))
                            .font(.system(.caption, design: .rounded))
                            .foregroundStyle(colors.textFaint)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: 280)
                    }
                    .padding(24)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("common.cancel")) {
                        dismiss()
                    }
                    .disabled(isSigningIn)
                }
            }
        }
        .interactiveDismissDisabled(isSigningIn)
    }

    private var googleButton: some View {
        Button {
            Task { await signInWithGoogle() }
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "g.circle.fill")
                    .font(.system(size: 20, weight: .semibold))
                Text(loc.t("login.continueGoogle"))
                    .font(.system(.body, design: .rounded).weight(.semibold))
            }
            .foregroundStyle(colors.textPrimary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(colors.card, in: Capsule())
            .overlay(Capsule().stroke(colors.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .disabled(isSigningIn)
    }

    @MainActor
    private func signInWithGoogle() async {
        errorMessage = nil
        guard SupabaseConfig.isGoogleConfigured else {
            errorMessage = loc.t("login.error.notConfigured")
            return
        }
        guard let presenter = RootViewController.current() else {
            errorMessage = loc.t("login.error.generic")
            return
        }
        isSigningIn = true
        defer { isSigningIn = false }
        do {
            let user = try await auth.signInWithGoogle(presenting: presenter)
            errorMessage = nil
            onSignedIn(user)
        } catch {
            if (error as NSError).code == GIDSignInError.canceled.rawValue { return }
            CrashReporting.log("auth: google_sign_in_failed", detail: String(describing: error))
            CrashReporting.record(error)
            errorMessage = loc.t("login.error.generic")
        }
    }

    private func handleAppleResult(_ result: Result<ASAuthorization, Error>) {
        switch result {
        case .failure(let error):
            if (error as NSError).code == ASAuthorizationError.canceled.rawValue { return }
            errorMessage = loc.t("login.error.generic")
        case .success:
            Task { await signInWithApple() }
        }
    }

    @MainActor
    private func signInWithApple() async {
        errorMessage = nil
        isSigningIn = true
        defer { isSigningIn = false }
        do {
            let user = try await auth.signInWithApple()
            onSignedIn(user)
        } catch {
            if (error as NSError).code == ASAuthorizationError.canceled.rawValue { return }
            CrashReporting.log("auth: apple_sign_in_failed", detail: String(describing: error))
            CrashReporting.record(error)
            errorMessage = loc.t("login.error.generic")
        }
    }
}
