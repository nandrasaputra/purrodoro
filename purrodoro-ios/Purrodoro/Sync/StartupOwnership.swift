//
//  StartupOwnership.swift
//  Purrodoro
//
//  Crash-remnant repair for ownership marker windows. Mirrors Android
//  `sync/StartupOwnership.kt`.
//

import Foundation

enum StartupAction: Equatable {
    case none
    case claim
    case completeDrop
    case signOut
}

func resolveStartupOwnership(userId: String?, ownerId: String?, hasLocalData: Bool) -> StartupAction {
    guard let userId else { return .none }
    if userId == ownerId { return .none }
    if ownerId == nil { return .claim }
    if !hasLocalData { return .completeDrop }
    return .signOut
}

func reconcileStartupOwnership(
    userId: String?,
    ownerId: String?,
    hasLocalData: Bool,
    setOwnerId: (String) async -> Void,
    claimMerge: () async -> Void,
    restoreFromCloud: () async -> Void,
    signOut: () async -> Void
) async {
    switch resolveStartupOwnership(userId: userId, ownerId: ownerId, hasLocalData: hasLocalData) {
    case .none:
        break
    case .claim:
        guard let userId else { return }
        await setOwnerId(userId)
        await claimMerge()
    case .completeDrop:
        guard let userId else { return }
        await setOwnerId(userId)
        await restoreFromCloud()
    case .signOut:
        await signOut()
    }
}
