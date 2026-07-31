//
//  StoreKitConfig.swift
//  Purrodoro
//
//  Placeholder product IDs mirroring Android BillingConfig. App Store Connect
//  should use the same monthly/yearly product ids in one subscription group.
//

import Foundation

enum StoreKitConfig {
    /// Play subscription product / App Store group reference — not a StoreKit product id.
    static let subscriptionGroupRef = "purrodoro_premium"
    static let monthlyProductId = "purrodoro-premium-monthly"
    static let yearlyProductId = "purrodoro-premium-yearly"

    static var allProductIds: [String] {
        [monthlyProductId, yearlyProductId]
    }
}
