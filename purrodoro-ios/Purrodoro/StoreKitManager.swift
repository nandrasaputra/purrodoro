//
//  StoreKitManager.swift
//  Purrodoro
//

import Combine
import Foundation
import StoreKit

@MainActor
final class StoreKitManager: ObservableObject {
    @Published private(set) var offers = SubscriptionOffers()
    @Published private(set) var purchaseInFlight = false
    @Published private(set) var lastError: String?
    @Published private(set) var lastMessage: String?

    private let prefs: PreferencesStore
    private var products: [PremiumPlan: Product] = [:]
    private var transactionUpdatesTask: Task<Void, Never>?
    private var pendingTimeoutTask: Task<Void, Never>?

    init(prefs: PreferencesStore) {
        self.prefs = prefs
    }

    deinit {
        transactionUpdatesTask?.cancel()
        pendingTimeoutTask?.cancel()
    }

    func start() {
        guard transactionUpdatesTask == nil else { return }
        transactionUpdatesTask = Task(priority: .background) { [weak self] in
            for await update in Transaction.updates {
                guard !Task.isCancelled else { return }
                await self?.handleTransactionUpdate(update)
            }
        }
    }

    func refreshCatalog() async {
        offers.catalogState = .loading
        lastError = nil

        do {
            let loadedProducts = try await Product.products(for: StoreKitConfig.allProductIds)
            products = Dictionary(
                uniqueKeysWithValues: PremiumPlan.allCases.compactMap { plan in
                    loadedProducts.first(where: { $0.id == plan.productId }).map { (plan, $0) }
                }
            )

            guard let monthly = products[.monthly],
                  let yearly = products[.yearly] else {
                offers = SubscriptionOffers(catalogState: .failed)
                lastError = "premium.error.catalog_unavailable"
                return
            }

            let yearlyIntro = yearly.subscription?.introductoryOffer
            let yearlyEligible = await yearly.subscription?.isEligibleForIntroOffer ?? false
            let yearlyMonthlyEquivalent = yearly.price / 12
            let savings = savingsPercent(monthlyPrice: monthly.price, yearlyPrice: yearly.price)

            offers = SubscriptionOffers(
                catalogState: .ready,
                monthly: PlanOfferDetails(priceLine: monthly.displayPrice),
                yearly: PlanOfferDetails(
                    priceLine: yearly.displayPrice,
                    secondaryPriceLine: yearly.priceFormatStyle.format(yearlyMonthlyEquivalent),
                    hasTrial: yearlyEligible && yearlyIntro?.paymentMode == .freeTrial,
                    hasOffer: yearlyEligible && yearlyIntro != nil
                ),
                savingsPercent: savings
            )
        } catch {
            products = [:]
            offers = SubscriptionOffers(catalogState: .failed)
            lastError = "premium.error.catalog_failed"
        }
    }

    func refreshEntitlements() async {
        var entitledProductIds: Set<String> = []

        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result,
                  transaction.revocationDate == nil,
                  transaction.expirationDate.map({ $0 > Date() }) ?? true else {
                continue
            }
            entitledProductIds.insert(transaction.productID)
        }

        prefs.setStorePremium(
            PremiumEntitlement.grantsPremium(entitledProductIds: entitledProductIds)
        )
    }

    func purchase(_ plan: PremiumPlan) async {
        clearFeedback()

        if products[plan] == nil {
            await refreshCatalog()
        }
        guard let product = products[plan] else {
            lastError = "premium.error.product_unavailable"
            return
        }

        purchaseInFlight = true
        do {
            switch try await product.purchase() {
            case .success(let result):
                guard case .verified(let transaction) = result else {
                    purchaseInFlight = false
                    lastError = "premium.error.verification_failed"
                    return
                }
                await transaction.finish()
                await refreshEntitlements()
                purchaseInFlight = false
                lastMessage = "premium.purchase.success"
            case .pending:
                schedulePendingTimeout()
            case .userCancelled:
                purchaseInFlight = false
            @unknown default:
                purchaseInFlight = false
            }
        } catch {
            purchaseInFlight = false
            lastError = "premium.error.purchase_failed"
            CrashReporting.log("billing: purchase_failed", detail: String(describing: error))
            CrashReporting.record(error)
        }
    }

    func restore() async {
        clearFeedback()
        purchaseInFlight = true

        do {
            try await AppStore.sync()
            await refreshEntitlements()
            purchaseInFlight = false
            lastMessage = prefs.storePremium
                ? "premium.restore.success"
                : "premium.restore.none"
        } catch {
            purchaseInFlight = false
            lastError = "premium.error.restore_failed"
            CrashReporting.log("billing: restore_failed", detail: String(describing: error))
            CrashReporting.record(error)
        }
    }

    func clearFeedback() {
        lastError = nil
        lastMessage = nil
    }

    private func handleTransactionUpdate(
        _ result: VerificationResult<Transaction>
    ) async {
        guard case .verified(let transaction) = result else {
            purchaseInFlight = false
            lastError = "premium.error.verification_failed"
            return
        }

        await transaction.finish()
        guard StoreKitConfig.allProductIds.contains(transaction.productID) else { return }
        pendingTimeoutTask?.cancel()
        purchaseInFlight = false
        await refreshEntitlements()
    }

    private func schedulePendingTimeout() {
        pendingTimeoutTask?.cancel()
        pendingTimeoutTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(300))
            guard !Task.isCancelled else { return }
            self?.purchaseInFlight = false
        }
    }

    private func savingsPercent(monthlyPrice: Decimal, yearlyPrice: Decimal) -> Int? {
        let annualMonthlyPrice = monthlyPrice * 12
        guard annualMonthlyPrice > 0, yearlyPrice < annualMonthlyPrice else { return nil }
        let savings = (1 - yearlyPrice / annualMonthlyPrice) * 100
        return NSDecimalNumber(decimal: savings).intValue
    }
}
