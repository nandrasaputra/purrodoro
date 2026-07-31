//
//  PremiumResolver.swift
//  Purrodoro
//
//  Combines Supabase admin override with store billing — mirrors Android.
//

import Foundation

enum PremiumResolver {
    static func resolve(
        overrideEnabled: Bool,
        overrideActive: Bool,
        storePremium: Bool
    ) -> Bool {
        overrideEnabled ? overrideActive : storePremium
    }
}
