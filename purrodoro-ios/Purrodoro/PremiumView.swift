//
//  PremiumView.swift
//  Purrodoro
//
//  Premium discovery — mirrors Android PremiumScreen.
//

import SwiftUI

struct PremiumView: View {
    @EnvironmentObject private var storeKit: StoreKitManager
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @Environment(\.openURL) private var openURL

    @State private var selectedPlan: PremiumPlan = .yearly

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var isPremium: Bool { prefs.isPremium }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    headerBlock

                    Text(loc.t("premium.screen.whatYouGet"))
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textSecondary)

                    compareCard

                    if !isPremium {
                        Text(loc.t("premium.screen.compareSummary"))
                            .font(.system(.footnote, design: .rounded))
                            .foregroundStyle(colors.textSecondary)

                        Text(loc.t("premium.screen.pickPlan"))
                            .font(.system(.subheadline, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textSecondary)
                            .padding(.top, 8)

                        PremiumPlanPicker(selectedPlan: $selectedPlan, offers: storeKit.offers)

                        feedbackBanner
                    } else {
                        Text(loc.t("premium.member.body"))
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundStyle(colors.textSecondary)
                    }
                }
                .padding(20)
            }
            .background(colors.page.ignoresSafeArea())
            .navigationTitle(loc.t("profile.premium.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("premium.cta.maybeLater")) { dismiss() }
                }
            }
            .safeAreaInset(edge: .bottom) {
                bottomActions
            }
        }
        .task {
            await storeKit.refreshCatalog()
            await storeKit.refreshEntitlements()
        }
    }

    private var headerBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: "sparkles")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 56, height: 56)
                .background(
                    LinearGradient(
                        colors: [Brand.premiumPurpleTop, Brand.premiumPurpleBottom],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                )

            Text(isPremium ? loc.t("premium.member.title") : loc.t("premium.screen.headline"))
                .font(.system(.title2, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
        }
    }

    private var compareCard: some View {
        VStack(spacing: 0) {
            compareRow(
                free: loc.t("premium.compare.tasks.free"),
                premium: loc.t("premium.compare.tasks.premium")
            )
            Divider()
            compareRow(
                free: loc.t("premium.compare.habits.free"),
                premium: loc.t("premium.compare.habits.premium")
            )
            Divider()
            compareRow(
                free: loc.t("premium.compare.sounds.free"),
                premium: loc.t("premium.compare.sounds.premium")
            )
            Divider()
            compareRow(
                free: loc.t("premium.compare.scenes.free"),
                premium: loc.t("premium.compare.scenes.premium")
            )
        }
        .padding(.vertical, 4)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(colors.border, lineWidth: 1)
        )
    }

    private func compareRow(free: String, premium: String) -> some View {
        HStack(alignment: .top) {
            Text(free)
                .font(.system(.footnote, design: .rounded))
                .foregroundStyle(colors.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(premium)
                .font(.system(.footnote, design: .rounded).weight(.semibold))
                .foregroundStyle(Brand.premiumPurpleBottom)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }

    private var bottomActions: some View {
        VStack(spacing: 10) {
            if !isPremium {
                Button {
                    Task { await storeKit.purchase(selectedPlan) }
                } label: {
                    Group {
                        if storeKit.purchaseInFlight {
                            ProgressView()
                        } else {
                            Text(ctaLabel)
                        }
                    }
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
                .disabled(storeKit.purchaseInFlight || !storeKit.offers.isReady)

                if storeKit.offers.catalogState == .failed {
                    Button(loc.t("premium.cta.retry")) {
                        Task { await storeKit.refreshCatalog() }
                    }
                    .font(.system(.subheadline, design: .rounded).weight(.medium))
                }
            }

            Button(loc.t("premium.restore")) {
                Task { await storeKit.restore() }
            }
            .font(.system(.subheadline, design: .rounded).weight(.semibold))
            .foregroundStyle(Brand.premiumPurpleBottom)
            .disabled(storeKit.purchaseInFlight)

            if isPremium {
                Button(loc.t("premium.manage")) {
                    if let url = URL(string: "https://apps.apple.com/account/subscriptions") {
                        openURL(url)
                    }
                }
                .font(.system(.subheadline, design: .rounded).weight(.medium))
                .foregroundStyle(colors.textSecondary)
            }
        }
        .padding(16)
        .background(colors.card.opacity(0.96))
    }

    @ViewBuilder
    private var feedbackBanner: some View {
        if let key = storeKit.lastError {
            Text(loc.t(key))
                .font(.system(.caption, design: .rounded))
                .foregroundStyle(Brand.deepCoral)
        } else if let key = storeKit.lastMessage {
            Text(loc.t(key))
                .font(.system(.caption, design: .rounded))
                .foregroundStyle(Brand.cocoaSoft)
        }
    }

    private var ctaLabel: String {
        let offer = storeKit.offers.forPlan(selectedPlan)
        if selectedPlan == .yearly, offer.hasTrial {
            return loc.t("premium.cta.startTrial")
        }
        return loc.t("premium.cta.subscribe")
    }
}
