//
//  PremiumGate.swift
//  Purrodoro
//
//  Free-tier limits — mirrors Android PremiumGate.kt.
//

import Foundation

enum PremiumGate {
    static let freeActiveTaskLimit = 10
    static let freeHabitLimit = 10

    static func activeTaskCount(_ tasks: [TaskItem]) -> Int {
        tasks.filter { !$0.completed }.count
    }

    static func canAddActiveTask(activeCount: Int, isPremium: Bool) -> Bool {
        isPremium || activeCount < freeActiveTaskLimit
    }

    static func canAddHabit(habitCount: Int, isPremium: Bool) -> Bool {
        isPremium || habitCount < freeHabitLimit
    }
}
