//
//  AccountDataClearer.swift
//  Purrodoro
//
//  Clears account-owned domain data on an ownership switch (Drop). Keeps
//  device-local prefs: appearance, language, onboarding, selected sound.
//  Mirrors Android `AppRepository.clearAccountOwnedData`.
//

import Foundation

@MainActor
struct AccountDataClearer {
    let syncRepo: SyncRepository
    let engine: TimerEngine
    let tasks: TaskStore
    let habits: HabitStore
    let history: SessionHistoryStore
    let prefs: PreferencesStore

    func clearAccountOwnedData() {
        syncRepo.clearAccountOwnedData()
        engine.clearAccountOwnedData()
        tasks.replaceAll([])
        habits.replaceAll([])
        history.clear()
        prefs.clearAccountOwnedName()
    }
}
