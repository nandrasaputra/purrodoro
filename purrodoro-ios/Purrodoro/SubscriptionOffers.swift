//
//  SubscriptionOffers.swift
//  Purrodoro
//
//  Catalog + localized price lines — mirrors Android SubscriptionOffers.
//

import Foundation

enum BillingCatalogState: Equatable {
    case idle
    case loading
    case ready
    case failed
}

struct PlanOfferDetails: Equatable {
    var priceLine: String? = nil
    var secondaryPriceLine: String? = nil
    var hasTrial: Bool = false
    var hasOffer: Bool = false
}

struct SubscriptionOffers: Equatable {
    var catalogState: BillingCatalogState = .idle
    var monthly = PlanOfferDetails()
    var yearly = PlanOfferDetails()
    /// Approximate savings vs paying monthly × 12; nil when prices unknown.
    var savingsPercent: Int? = nil

    var isReady: Bool { catalogState == .ready }

    func forPlan(_ plan: PremiumPlan) -> PlanOfferDetails {
        switch plan {
        case .monthly: monthly
        case .yearly: yearly
        }
    }
}
