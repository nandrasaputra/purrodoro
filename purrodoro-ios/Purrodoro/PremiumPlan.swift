//
//  PremiumPlan.swift
//  Purrodoro
//

import Foundation

enum PremiumPlan: String, CaseIterable, Identifiable, Equatable {
    case monthly
    case yearly

    var id: String { rawValue }

    var productId: String {
        switch self {
        case .monthly: StoreKitConfig.monthlyProductId
        case .yearly: StoreKitConfig.yearlyProductId
        }
    }
}
