//
//  PremiumLoginRequiredSheet.swift
//  Purrodoro
//
//  Guest gate for Premium discovery — mirrors Android PremiumLoginRequiredSheet.
//

import SwiftUI

struct PremiumLoginRequiredSheet: View {
    var onSignIn: () -> Void
    var onDismiss: () -> Void

    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "sparkles")
                    .font(.system(size: 36, weight: .semibold))
                    .foregroundStyle(Brand.premiumPurpleBottom)
                    .padding(.top, 12)

                Text(loc.t("premium.login.title"))
                    .font(.system(.title3, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .multilineTextAlignment(.center)

                Text(loc.t("premium.login.body"))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .multilineTextAlignment(.center)

                Button(action: onSignIn) {
                    Text(loc.t("premium.login.signIn"))
                        .font(.system(.headline, design: .rounded).weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .foregroundStyle(.white)
                        .background(
                            LinearGradient(
                                colors: [Brand.premiumPurpleTop, Brand.premiumPurpleBottom],
                                startPoint: .top,
                                endPoint: .bottom
                            ),
                            in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                        )
                }
                .buttonStyle(.plain)

                Button(loc.t("premium.cta.maybeLater"), action: onDismiss)
                    .font(.system(.subheadline, design: .rounded).weight(.medium))
                    .foregroundStyle(colors.textSecondary)

                Spacer(minLength: 0)
            }
            .padding(24)
            .background(colors.page.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("premium.cta.maybeLater"), action: onDismiss)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
