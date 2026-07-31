//
//  PremiumUpsellModels.swift
//  Purrodoro
//
//  Mirrors Android PremiumUpsellReason / Trigger / PremiumAccessRequest.
//

import Foundation

enum PremiumUpsellReason: Equatable {
    case lockedSound(String)
    case lockedScene(String)
    case activeTaskCap
    case habitCap
    case generic
}

struct PremiumUpsellTrigger: Equatable {
    var reason: PremiumUpsellReason = .generic

    static func lockedSound(_ name: String) -> PremiumUpsellTrigger {
        PremiumUpsellTrigger(reason: .lockedSound(name))
    }

    static func lockedScene(_ name: String) -> PremiumUpsellTrigger {
        PremiumUpsellTrigger(reason: .lockedScene(name))
    }

    static let activeTaskCap = PremiumUpsellTrigger(reason: .activeTaskCap)
    static let habitCap = PremiumUpsellTrigger(reason: .habitCap)
}

enum PremiumAccessRequest: Equatable {
    case discovery
    case upsell(PremiumUpsellTrigger)
}
