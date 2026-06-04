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

    static let maxNameLength = 24

    private let store: KeyValueStore
    private let appearanceKey = "purrodoro.appearanceMode"
    private let nameKey = "purrodoro.userName"
    private let onboardingKey = "purrodoro.hasCompletedOnboarding"

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        let raw = store.string(forKey: appearanceKey)
        self.appearanceMode = raw.flatMap(AppearanceMode.init(rawValue:)) ?? .auto
        self.userName = store.string(forKey: nameKey) ?? ""
        self.hasCompletedOnboarding = store.bool(forKey: onboardingKey)
    }
}
