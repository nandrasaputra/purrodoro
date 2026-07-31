//
//  PremiumEntitlement.swift
//  Purrodoro
//
//  Pure helpers for deciding whether StoreKit entitlements grant Premium.
//

import Foundation

enum PremiumEntitlement {
    static func grantsPremium(
        entitledProductIds: some Collection<String>,
        knownProductIds: [String] = StoreKitConfig.allProductIds
    ) -> Bool {
        entitledProductIds.contains { knownProductIds.contains($0) }
    }
}
