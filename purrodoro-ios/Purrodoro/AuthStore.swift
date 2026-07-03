//
//  AuthStore.swift
//  Purrodoro
//
//  Supabase-backed auth for iOS. Session persists in the Keychain via the
//  Supabase Swift SDK default storage. Mirrors Android `AuthRepository.kt`.
//

import Foundation
import Combine
import Supabase
import GoogleSignIn
import AuthenticationServices
import UIKit

enum AuthError: LocalizedError {
    case notConfigured
    case missingIDToken
    case missingPresenter
    case signInIncomplete

    var errorDescription: String? {
        switch self {
        case .notConfigured: return "Provider not configured"
        case .missingIDToken: return "Missing identity token"
        case .missingPresenter: return "No view controller to present sign-in"
        case .signInIncomplete: return "Sign-in did not complete"
        }
    }
}

@MainActor
final class AuthStore: ObservableObject {
    @Published private(set) var state: AuthState = .loading

    let client: SupabaseClient
    private let appleSignIn = AppleSignInCoordinator()
    private var listenTask: Task<Void, Never>?

    init(client: SupabaseClient? = nil) {
        self.client = client ?? SupabaseClient(
            supabaseURL: SupabaseConfig.url,
            supabaseKey: SupabaseConfig.publishableKey
        )
        listenTask = Task { [weak self] in
            await self?.bootstrap()
        }
    }

    deinit {
        listenTask?.cancel()
    }

    var isSignedIn: Bool {
        if case .signedIn = state { return true }
        return false
    }

    var currentUser: AuthUser? {
        if case .signedIn(let user) = state { return user }
        return nil
    }

    func bootstrap() async {
        await refreshState()
        for await _ in client.auth.authStateChanges {
            await refreshState()
        }
    }

    func refreshState() async {
        if let user = client.auth.currentUser {
            state = .signedIn(mapUser(user))
        } else {
            state = .guest
        }
    }

    func signInWithGoogle(presenting viewController: UIViewController) async throws -> AuthUser {
        guard SupabaseConfig.isGoogleConfigured else { throw AuthError.notConfigured }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: SupabaseConfig.googleIOSClientID,
            serverClientID: SupabaseConfig.googleWebClientID
        )

        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: viewController)
        guard let idToken = result.user.idToken?.tokenString else {
            throw AuthError.missingIDToken
        }
        let accessToken = result.user.accessToken.tokenString

        try await client.auth.signInWithIdToken(
            credentials: OpenIDConnectCredentials(
                provider: .google,
                idToken: idToken,
                accessToken: accessToken
            )
        )

        return try await awaitSignedInUser()
    }

    func signInWithApple() async throws -> AuthUser {
        let credential = try await appleSignIn.signIn()
        guard let idTokenData = credential.identityToken,
              let idToken = String(data: idTokenData, encoding: .utf8) else {
            throw AuthError.missingIDToken
        }

        try await client.auth.signInWithIdToken(
            credentials: OpenIDConnectCredentials(
                provider: .apple,
                idToken: idToken
            )
        )

        if let fullName = credential.fullName {
            let parts = [fullName.givenName, fullName.middleName, fullName.familyName]
                .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
            if !parts.isEmpty {
                let joined = parts.joined(separator: " ")
                _ = try? await client.auth.update(
                    user: UserAttributes(data: ["full_name": .string(joined)])
                )
            }
        }

        return try await awaitSignedInUser()
    }

    @discardableResult
    func signOut() async throws -> Bool {
        try await client.auth.signOut(scope: .local)
        await refreshState()
        return !isSignedIn
    }

    func handleOpenURL(_ url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    // MARK: - Private

    private func awaitSignedInUser() async throws -> AuthUser {
        for _ in 0..<20 {
            if let user = client.auth.currentUser {
                let mapped = mapUser(user)
                state = .signedIn(mapped)
                return mapped
            }
            try await Task.sleep(nanoseconds: 100_000_000)
        }
        if let user = client.auth.currentUser {
            let mapped = mapUser(user)
            state = .signedIn(mapped)
            return mapped
        }
        throw AuthError.signInIncomplete
    }
}

// MARK: - User mapping

func mapUser(_ user: User) -> AuthUser {
    func metaString(_ key: String) -> String? {
        guard let value = user.userMetadata[key] else { return nil }
        if case .string(let s) = value {
            let trimmed = s.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? nil : trimmed
        }
        return nil
    }

    let displayName = metaString("full_name") ?? metaString("name")
    let avatarURL = resolveAvatarURL(from: user.userMetadata)
    let providerTag: String? = {
        guard let value = user.appMetadata["provider"] else { return nil }
        if case .string(let s) = value { return s }
        return nil
    }()

    return AuthUser(
        userId: user.id.uuidString,
        email: user.email?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
        displayName: displayName,
        provider: AuthProvider.fromStorageTag(providerTag),
        avatarURL: avatarURL
    )
}

func resolveAvatarURL(from metadata: [String: AnyJSON]) -> URL? {
    for key in ["avatar_url", "picture", "avatar"] {
        if let value = metadata[key], case .string(let raw) = value {
            let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty, let url = URL(string: trimmed) { return url }
        }
    }
    return nil
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}

// MARK: - Apple Sign-In coordinator

@MainActor
private final class AppleSignInCoordinator: NSObject, ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding
{
    private var continuation: CheckedContinuation<ASAuthorizationAppleIDCredential, Error>?

    func signIn() async throws -> ASAuthorizationAppleIDCredential {
        try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            let provider = ASAuthorizationAppleIDProvider()
            let request = provider.createRequest()
            request.requestedScopes = [.fullName, .email]
            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        }
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            continuation?.resume(throwing: AuthError.signInIncomplete)
            continuation = nil
            return
        }
        continuation?.resume(returning: credential)
        continuation = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        continuation?.resume(throwing: error)
        continuation = nil
    }
}

// MARK: - UIViewController helper

enum RootViewController {
    @MainActor
    static func current() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController?
            .topMostPresented()
    }
}

private extension UIViewController {
    func topMostPresented() -> UIViewController {
        presentedViewController?.topMostPresented() ?? self
    }
}
