//
//  AccountManager.swift
//  Purrodoro
//
//  Wires the pure `resolveLoginAction` rule to Claim / Keep / Drop side-effects.
//  Mirrors Android `auth/AccountManager.kt`.
//

import Foundation

@MainActor
final class AccountManager {
    enum Result: Equatable {
        case claimed
        case kept
        case dropped
        case dropCancelled
    }

    private let ownerStore: AccountOwnerStore
    private let clearer: AccountDataClearer

    init(ownerStore: AccountOwnerStore, clearer: AccountDataClearer) {
        self.ownerStore = ownerStore
        self.clearer = clearer
    }

    /// Resolve what to do with on-device data for [incomingUserId] that just signed in.
    /// On Drop, [onBeforeClear] runs before wiping (stop timer first).
    func onSignedIn(
        incomingUserId: String,
        confirmDrop: () async -> Bool,
        onBeforeClear: () async -> Void = {}
    ) async -> Result {
        switch resolveLoginAction(ownerId: ownerStore.currentOwnerId(), incomingUserId: incomingUserId) {
        case .claim:
            ownerStore.setOwnerId(incomingUserId)
            return .claimed
        case .keep:
            return .kept
        case .drop:
            guard await confirmDrop() else { return .dropCancelled }
            await onBeforeClear()
            clearer.clearAccountOwnedData()
            ownerStore.setOwnerId(incomingUserId)
            return .dropped
        }
    }
}
