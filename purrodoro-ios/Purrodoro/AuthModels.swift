//
//  AuthModels.swift
//  Purrodoro
//
//  Provider-agnostic auth types and the pure login ownership rule. Mirrors
//  Android `model/AuthState.kt` — see auth-and-account-identity spec.
//

import Foundation

enum AuthProvider: String, Equatable {
    case google
    case apple
    case unknown

    static func fromStorageTag(_ tag: String?) -> AuthProvider {
        switch tag {
        case "google": return .google
        case "apple": return .apple
        default: return .unknown
        }
    }

    var storageTag: String { rawValue }
}

enum LoginDataAction: Equatable {
    case claim
    case keep
    case drop
}

/// Pure ownership rule — identical contract on Android and iOS.
func resolveLoginAction(ownerId: String?, incomingUserId: String) -> LoginDataAction {
    if ownerId == nil { return .claim }
    if ownerId == incomingUserId { return .keep }
    return .drop
}

struct AuthUser: Equatable {
    let userId: String
    let email: String?
    let displayName: String?
    let provider: AuthProvider
    let avatarURL: URL?
}

enum AuthState: Equatable {
    case loading
    case guest
    case signedIn(AuthUser)
}

func resolvedDisplayName(authState: AuthState, localName: String) -> String {
    switch authState {
    case .signedIn(let user):
        let account = user.displayName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !account.isEmpty { return account }
        let local = localName.trimmingCharacters(in: .whitespacesAndNewlines)
        return local.isEmpty ? "" : local
    case .guest:
        return localName.trimmingCharacters(in: .whitespacesAndNewlines)
    case .loading:
        return ""
    }
}

func isNameFromAccount(authState: AuthState, localName: String) -> Bool {
    guard case .signedIn(let user) = authState else { return false }
    let account = user.displayName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    guard !account.isEmpty else { return false }
    return account == localName.trimmingCharacters(in: .whitespacesAndNewlines)
}
