//
//  PremiumCapHeader.swift
//  Purrodoro
//
//  Free-tier usage label — mirrors Android FreeTierCapHeaderLabel.
//

import SwiftUI

struct PremiumCapHeader: View {
    let visible: Bool
    let current: Int
    let limit: Int
    let labelKey: String

    @EnvironmentObject private var loc: Localization

    var body: some View {
        if visible {
            let atCap = current >= limit
            Text(loc.t(labelKey, current, limit))
                .font(.system(.caption, design: .rounded).weight(.medium))
                .foregroundStyle(atCap ? Brand.lockedText : Brand.cocoaSoft)
                .padding(.horizontal, atCap ? 10 : 0)
                .padding(.vertical, atCap ? 4 : 0)
                .background(
                    atCap
                        ? AnyShapeStyle(Brand.lockedFill)
                        : AnyShapeStyle(Color.clear),
                    in: Capsule()
                )
                .accessibilityLabel(loc.t("free_tier.cap_cd", current, limit))
        }
    }
}
