//
//  FocusShieldStore.swift
//  Purrodoro
//
//  Device-local Focus Shield preferences + Screen Time authorization +
//  ManagedSettings apply/clear while a Focus segment is armed. See
//  docs/superpowers/specs/2026-08-03-ios-focus-shield-design.md.
//

import Combine
import Foundation
import FamilyControls
import ManagedSettings
import SwiftUI

extension ManagedSettingsStore.Name {
    static let focusShield = ManagedSettingsStore.Name("purrodoro.focusShield")
}

@MainActor
final class FocusShieldStore: ObservableObject {

    @Published var enabled: Bool {
        didSet { store.set(enabled, forKey: enabledKey) }
    }

    /// Apps / categories / domains chosen via `FamilyActivityPicker`.
    @Published var selection: FamilyActivitySelection {
        didSet { persistSelection() }
    }

    @Published private(set) var authorizationStatus: AuthorizationStatus
    @Published private(set) var isApplyingShield = false
    @Published private(set) var lastErrorMessage: String?

    /// When set, ManagedSettings stay cleared until this date (session bypass).
    @Published private(set) var bypassUntil: Date?

    var isAuthorized: Bool {
        authorizationStatus == .approved
    }

    var hasSelection: Bool {
        !selection.applicationTokens.isEmpty
            || !selection.categoryTokens.isEmpty
            || !selection.webDomainTokens.isEmpty
    }

    var selectedAppCount: Int {
        selection.applicationTokens.count
            + selection.categoryTokens.count
            + selection.webDomainTokens.count
    }

    /// Invoked on the main actor when a session bypass window ends so the app
    /// can re-apply shields if still armed.
    var onBypassExpired: (() -> Void)?

    private let store: KeyValueStore
    private let enabledKey = "purrodoro.focusShieldEnabled"
    private let selectionKey = "purrodoro.focusShieldSelection"
    private let managedStore = ManagedSettingsStore(named: .focusShield)
    private var bypassTask: Task<Void, Never>?

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        self.enabled = store.bool(forKey: enabledKey)
        self.selection = Self.loadSelection(from: store, key: selectionKey) ?? FamilyActivitySelection()
        self.authorizationStatus = AuthorizationCenter.shared.authorizationStatus
    }

    func refreshAuthorizationStatus() {
        authorizationStatus = AuthorizationCenter.shared.authorizationStatus
    }

    @discardableResult
    func requestAuthorization() async -> Bool {
        lastErrorMessage = nil
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            refreshAuthorizationStatus()
            return isAuthorized
        } catch {
            refreshAuthorizationStatus()
            lastErrorMessage = error.localizedDescription
            return false
        }
    }

    /// Recompute arming from timer state and apply or clear the system shield.
    func syncWithTimer(timerState: TimerState, currentIsFocus: Bool) {
        refreshAuthorizationStatus()
        if let until = bypassUntil, Date() >= until {
            bypassUntil = nil
        }

        let armed = FocusShield.isArmed(
            preferenceEnabled: enabled,
            hasSelection: hasSelection,
            authorized: isAuthorized,
            timerState: timerState,
            currentIsFocus: currentIsFocus
        )

        if armed, bypassUntil == nil || Date() >= (bypassUntil ?? .distantPast) {
            applyShield()
        } else {
            clearShield()
        }
    }

    /// Clear shields for the remainder of the current focus (Android "Open anyway").
    func bypass(remainingSeconds: Int) {
        bypassTask?.cancel()
        let duration = FocusShield.bypassDuration(remainingSeconds: remainingSeconds)
        bypassUntil = Date().addingTimeInterval(duration)
        clearShield()
        bypassTask = Task { [weak self] in
            let nanos = UInt64(duration * 1_000_000_000)
            try? await Task.sleep(nanoseconds: nanos)
            guard !Task.isCancelled else { return }
            await MainActor.run {
                guard let self else { return }
                self.bypassUntil = nil
                self.onBypassExpired?()
            }
        }
    }

    func clearBypass() {
        bypassTask?.cancel()
        bypassTask = nil
        bypassUntil = nil
    }

    func clearSelection() {
        selection = FamilyActivitySelection()
        clearShield()
    }

    // MARK: - ManagedSettings

    private func applyShield() {
        isApplyingShield = true
        managedStore.shield.applications = selection.applicationTokens.isEmpty
            ? nil
            : selection.applicationTokens
        managedStore.shield.applicationCategories = selection.categoryTokens.isEmpty
            ? nil
            : .specific(selection.categoryTokens)
        managedStore.shield.webDomains = selection.webDomainTokens.isEmpty
            ? nil
            : selection.webDomainTokens
        isApplyingShield = false
    }

    private func clearShield() {
        managedStore.clearAllSettings()
        isApplyingShield = false
    }

    // MARK: - Persistence

    private func persistSelection() {
        do {
            let data = try PropertyListEncoder().encode(selection)
            store.set(data, forKey: selectionKey)
        } catch {
            // Selection stays in memory; next successful encode will persist.
            lastErrorMessage = error.localizedDescription
        }
    }

    private static func loadSelection(from store: KeyValueStore, key: String) -> FamilyActivitySelection? {
        guard let data = store.data(forKey: key) else { return nil }
        return try? PropertyListDecoder().decode(FamilyActivitySelection.self, from: data)
    }
}

// MARK: - Status helpers (UI)

enum FocusShieldReadiness: Equatable {
    case off
    case needAuthorization
    case needApps
    case ready(count: Int)

    var subtitleKey: String {
        switch self {
        case .off: return "focusShield.status.offHint"
        case .needAuthorization: return "focusShield.status.needAuth"
        case .needApps: return "focusShield.status.needApps"
        case .ready: return "focusShield.status.ready"
        }
    }
}

extension FocusShieldStore {
    var readiness: FocusShieldReadiness {
        guard enabled else { return .off }
        guard isAuthorized else { return .needAuthorization }
        guard hasSelection else { return .needApps }
        return .ready(count: selectedAppCount)
    }
}
