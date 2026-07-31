//
//  PremiumUpsellSheet.swift
//  Purrodoro
//
//  Cap / locked-feature paywall — mirrors Android PremiumUpsellSheet.
//

import SwiftUI

struct PremiumUpsellSheet: View {
    let trigger: PremiumUpsellTrigger
    var isGuest: Bool
    var onDismiss: () -> Void
    var onSignIn: () -> Void
    var onExplorePremium: (() -> Void)? = nil

    @EnvironmentObject private var storeKit: StoreKitManager
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @Environment(\.openURL) private var openURL

    @State private var selectedPlan: PremiumPlan = .yearly

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    header

                    if isGuest {
                        guestGate
                    } else {
                        memberContent
                    }
                }
                .padding(20)
            }
            .background(colors.page.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("premium.cta.maybeLater"), action: onDismiss)
                }
            }
            .safeAreaInset(edge: .bottom) {
                if !isGuest {
                    stickyFooter
                }
            }
        }
        .presentationDetents([.large])
        .task {
            await storeKit.refreshCatalog()
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(.title2, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
            Text(bodyText)
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(colors.textSecondary)
        }
    }

    private var guestGate: some View {
        VStack(spacing: 14) {
            Text(loc.t("premium.guest.unlock.title"))
                .font(.system(.headline, design: .rounded))
                .foregroundStyle(colors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

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
        }
        .padding(.top, 8)
    }

    private var memberContent: some View {
        VStack(alignment: .leading, spacing: 14) {
            PremiumPlanPicker(
                selectedPlan: $selectedPlan,
                offers: storeKit.offers
            )

            if let explore = onExplorePremium {
                Button(loc.t("profile.premium.explore"), action: explore)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(Brand.premiumPurpleBottom)
            }

            feedbackBanner
        }
    }

    private var stickyFooter: some View {
        VStack(spacing: 10) {
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
                .onAppear {
                    if prefs.isPremium { onDismiss() }
                }
        }
    }

    private var title: String {
        switch trigger.reason {
        case .activeTaskCap: loc.t("premium.upsell.taskCap.title")
        case .habitCap: loc.t("premium.upsell.habitCap.title")
        case .generic: loc.t("premium.upsell.generic.title")
        case .lockedSound(let name): loc.t("premium.upsell.sound.title", name)
        case .lockedScene(let name): loc.t("premium.upsell.scene.title", name)
        }
    }

    private var bodyText: String {
        switch trigger.reason {
        case .activeTaskCap: loc.t("premium.upsell.taskCap.body")
        case .habitCap: loc.t("premium.upsell.habitCap.body")
        case .generic: loc.t("premium.upsell.generic.body")
        case .lockedSound, .lockedScene: loc.t("premium.upsell.locked.body")
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

struct PremiumPlanPicker: View {
    @Binding var selectedPlan: PremiumPlan
    let offers: SubscriptionOffers

    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        VStack(spacing: 10) {
            ForEach(PremiumPlan.allCases) { plan in
                planRow(plan)
            }
        }
    }

    private func planRow(_ plan: PremiumPlan) -> some View {
        let selected = selectedPlan == plan
        let details = offers.forPlan(plan)
        return Button {
            selectedPlan = plan
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text(plan == .monthly ? loc.t("premium.plan.monthly") : loc.t("premium.plan.yearly"))
                            .font(.system(.headline, design: .rounded).weight(.semibold))
                        if plan == .yearly, let savings = offers.savingsPercent {
                            Text(loc.t("premium.plan.savings", savings))
                                .font(.system(.caption2, design: .rounded).weight(.bold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Brand.premiumPurpleBottom, in: Capsule())
                        }
                    }
                    Text(details.priceLine ?? "—")
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                }
                Spacer()
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(selected ? Brand.premiumPurpleBottom : colors.textSecondary)
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(selected ? Brand.premiumPurpleBottom : colors.border, lineWidth: selected ? 2 : 1)
                    .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            )
        }
        .buttonStyle(.plain)
    }
}
