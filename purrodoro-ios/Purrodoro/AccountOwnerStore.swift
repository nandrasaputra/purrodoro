//
//  AccountOwnerStore.swift
//  Purrodoro
//
//  Persists the local data ownership marker (`ownerId`). Independent of the
//  live auth session — survives sign-out. See auth-and-account-identity spec.
//

import Foundation
import Combine

@MainActor
final class AccountOwnerStore: ObservableObject {
    @Published private(set) var ownerId: String?

    private let store: KeyValueStore
    private let key = "purrodoro.ownerId"

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        ownerId = store.string(forKey: key)
    }

    func currentOwnerId() -> String? { ownerId }

    func setOwnerId(_ value: String?) {
        ownerId = value
        store.set(value, forKey: key)
    }
}
