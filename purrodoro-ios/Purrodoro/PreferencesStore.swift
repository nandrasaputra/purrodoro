//
//  PreferencesStore.swift
//  Purrodoro
//
//  App-wide user preferences that aren't part of the timer cycle itself.
//  Currently holds the appearance mode (Light / Dark / Auto); kept separate
//  from `TimerEngine` so it can grow (name, language, …) without bloating the
//  engine. Persisted to `UserDefaults`, mirroring the engine's approach.
//

import Foundation
import Combine

@MainActor
final class PreferencesStore: ObservableObject {

    @Published var appearanceMode: AppearanceMode {
        didSet { store.set(appearanceMode.rawValue, forKey: appearanceKey) }
    }

    /// Optional display name used to personalize the greeting. Capped in length
    /// while editing; only trimmed for display via `trimmedName` so spaces can
    /// be typed mid-name. Empty (after trimming) means "no name set".
    @Published var userName: String {
        didSet {
            if userName.count > Self.maxNameLength {
                userName = String(userName.prefix(Self.maxNameLength))
                return // didSet re-fires with the capped value, which persists
            }
            store.set(userName, forKey: nameKey)
            mirrorProfileToSync()
        }
    }
    /// Whitespace-trimmed name suitable for display; `nil` when nothing is set.
    var trimmedName: String? {
        let t = userName.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }

    /// Whether the first-run onboarding has been completed. Drives the
    /// full-screen intro shown over the app on first launch.
    @Published var hasCompletedOnboarding: Bool {
        didSet { store.set(hasCompletedOnboarding, forKey: onboardingKey) }
    }

    /// Whether the post-onboarding spotlight tour has been finished or skipped.
    @Published var hasSeenTutorial: Bool {
        didSet { store.set(hasSeenTutorial, forKey: tutorialKey) }
    }

    /// Ephemeral "simple note" intent for the active focus cycle (Android
    /// `FocusType.SimpleNote`). Cleared when a task/habit is attached or the
    /// user detaches the note.
    @Published var activeSimpleNote: String? {
        didSet {
            if let note = activeSimpleNote, !note.isEmpty {
                store.set(note, forKey: simpleNoteKey)
            } else {
                store.set(nil as String?, forKey: simpleNoteKey)
            }
        }
    }

    /// Selected home hero backdrop. Premium scenes stay stored for free users
    /// but render falls back to Room via `effectiveHomeScene`.
    @Published var homeScene: HomeScene {
        didSet { store.set(homeScene.storageName, forKey: homeSceneKey) }
    }

    @Published private(set) var storePremium: Bool
    @Published private(set) var premiumOverrideEnabled: Bool
    @Published private(set) var premiumOverrideActive: Bool

    var isPremium: Bool {
        PremiumOverride.overrideValue()
            ?? PremiumResolver.resolve(
                overrideEnabled: premiumOverrideEnabled,
                overrideActive: premiumOverrideActive,
                storePremium: storePremium
            )
    }

    /// Scene actually shown on Home (Room for free users with a premium pick).
    var displayHomeScene: HomeScene {
        effectiveHomeScene(selected: homeScene, isPremium: isPremium)
    }

    static let maxNameLength = 24

    private let syncRepo: SyncRepository?
    private let store: KeyValueStore
    private let appearanceKey = "purrodoro.appearanceMode"
    private let nameKey = "purrodoro.userName"
    private let onboardingKey = "purrodoro.hasCompletedOnboarding"
    private let tutorialKey = "purrodoro.hasSeenTutorial"
    private let simpleNoteKey = "purrodoro.activeSimpleNote"
    private let homeSceneKey = "purrodoro.homeScene"
    private let storePremiumKey = "purrodoro.storePremium"
    private let overrideEnabledKey = "purrodoro.premiumOverrideEnabled"
    private let overrideActiveKey = "purrodoro.premiumOverrideActive"
    private var suppressProfileMirror = false

    var timerSettingsProvider: () -> TimerSettings = { TimerSettings() }

    init(syncRepo: SyncRepository? = nil, store: KeyValueStore = UserDefaults.standard) {
        self.syncRepo = syncRepo
        self.store = syncRepo?.store ?? store
        let raw = store.string(forKey: appearanceKey)
        self.appearanceMode = raw.flatMap(AppearanceMode.init(rawValue:)) ?? .auto
        self.userName = store.string(forKey: nameKey) ?? ""
        self.hasCompletedOnboarding = store.bool(forKey: onboardingKey)
        self.hasSeenTutorial = store.bool(forKey: tutorialKey)
        let savedNote = store.string(forKey: simpleNoteKey)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        self.activeSimpleNote = (savedNote?.isEmpty == false) ? savedNote : nil
        self.homeScene = HomeScene.fromStorageName(store.string(forKey: homeSceneKey))
        self.storePremium = store.bool(forKey: storePremiumKey)
        self.premiumOverrideEnabled = store.bool(forKey: overrideEnabledKey)
        self.premiumOverrideActive = store.bool(forKey: overrideActiveKey)
    }

    func setSimpleNote(_ note: String) {
        let trimmed = note.trimmingCharacters(in: .whitespacesAndNewlines)
        activeSimpleNote = trimmed.isEmpty ? nil : trimmed
    }

    func clearSimpleNote() {
        activeSimpleNote = nil
    }

    /// Clears the greeting name on account switch (Drop). Appearance, language,
    /// and onboarding flags are device-local and kept.
    func clearAccountOwnedName() {
        userName = ""
        clearSimpleNote()
    }

    /// Sync-pull apply path: writes prefs only (row written by ProfileTableSync).
    func applySyncedProfile(displayName: String) {
        suppressProfileMirror = true
        defer { suppressProfileMirror = false }
        let trimmed = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        store.set(trimmed, forKey: nameKey)
        userName = trimmed
    }

    func setStorePremium(_ value: Bool) {
        storePremium = value
        store.set(value, forKey: storePremiumKey)
    }

    func applyPremiumOverride(enabled: Bool, active: Bool) {
        premiumOverrideEnabled = enabled
        premiumOverrideActive = active
        store.set(enabled, forKey: overrideEnabledKey)
        store.set(active, forKey: overrideActiveKey)
    }

    func reloadFromSync() {
        let name = syncRepo?.profileDisplayName() ?? store.string(forKey: nameKey) ?? ""
        suppressProfileMirror = true
        userName = name
        suppressProfileMirror = false
    }

    private func mirrorProfileToSync() {
        guard let syncRepo, !suppressProfileMirror else { return }
        syncRepo.updateProfile(displayName: userName, timerSettings: timerSettingsProvider())
    }
}
