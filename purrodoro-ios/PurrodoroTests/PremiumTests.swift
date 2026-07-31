//
//  PremiumTests.swift
//  PurrodoroTests
//

import Testing
import Foundation
@testable import Purrodoro

struct PremiumGateTests {

    @Test func freeUserBlockedAtTenActiveTasks() {
        #expect(PremiumGate.canAddActiveTask(activeCount: 9, isPremium: false))
        #expect(!PremiumGate.canAddActiveTask(activeCount: 10, isPremium: false))
    }

    @Test func premiumBypassesTaskCap() {
        #expect(PremiumGate.canAddActiveTask(activeCount: 50, isPremium: true))
    }

    @Test func freeUserBlockedAtTenHabits() {
        #expect(PremiumGate.canAddHabit(habitCount: 9, isPremium: false))
        #expect(!PremiumGate.canAddHabit(habitCount: 10, isPremium: false))
    }

    @Test func premiumBypassesHabitCap() {
        #expect(PremiumGate.canAddHabit(habitCount: 50, isPremium: true))
    }

    @Test func activeTaskCountIgnoresCompleted() {
        let tasks = [
            TaskItem(title: "A", completed: false),
            TaskItem(title: "B", completed: true),
            TaskItem(title: "C", completed: false),
        ]
        #expect(PremiumGate.activeTaskCount(tasks) == 2)
    }

    @Test func limitsMatchAndroid() {
        #expect(PremiumGate.freeActiveTaskLimit == 10)
        #expect(PremiumGate.freeHabitLimit == 10)
    }
}

struct PremiumResolverTests {

    @Test func overrideEnabledUsesOverrideActive() {
        #expect(PremiumResolver.resolve(overrideEnabled: true, overrideActive: true, storePremium: false))
        #expect(!PremiumResolver.resolve(overrideEnabled: true, overrideActive: false, storePremium: true))
    }

    @Test func overrideDisabledUsesStorePremium() {
        #expect(PremiumResolver.resolve(overrideEnabled: false, overrideActive: false, storePremium: true))
        #expect(!PremiumResolver.resolve(overrideEnabled: false, overrideActive: true, storePremium: false))
    }
}

struct PremiumEntitlementTests {

    @Test func grantsWhenMonthlyMatches() {
        #expect(PremiumEntitlement.grantsPremium(
            entitledProductIds: [StoreKitConfig.monthlyProductId]
        ))
    }

    @Test func grantsWhenYearlyMatches() {
        #expect(PremiumEntitlement.grantsPremium(
            entitledProductIds: [StoreKitConfig.yearlyProductId]
        ))
    }

    @Test func rejectsUnknownProduct() {
        #expect(!PremiumEntitlement.grantsPremium(
            entitledProductIds: ["other.sku"]
        ))
    }
}

struct PremiumUpsellModelsTests {

    @Test func factoryTriggers() {
        #expect(PremiumUpsellTrigger.activeTaskCap.reason == .activeTaskCap)
        #expect(PremiumUpsellTrigger.habitCap.reason == .habitCap)
        #expect(PremiumUpsellTrigger.lockedSound("Rain").reason == .lockedSound("Rain"))
        #expect(PremiumUpsellTrigger.lockedScene("Café").reason == .lockedScene("Café"))
    }
}
