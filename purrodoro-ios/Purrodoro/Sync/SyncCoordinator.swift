//
//  SyncCoordinator.swift
//  Purrodoro
//
//  Composition root for sync: engine, triggers, hooks, logout flush.
//  Mirrors Android `sync/SyncModule.kt`.
//

import Foundation
import Combine

@MainActor
final class SyncCoordinator: ObservableObject {
    let statusStore = SyncStatusStore()

    private let auth: AuthStore
    private let ownerStore: AccountOwnerStore
    private let syncRepo: SyncRepository
    private let prefs: PreferencesStore
    private let timerEngine: TimerEngine
    private let engine: SyncEngine
    private let syncHooksImpl = SyncHooksImpl()

    private var debounceTask: Task<Void, Never>?
    private var localWritesCancellable: AnyCancellable?

    var onStoresShouldReload: (() -> Void)?

    var syncHooks: AccountManager.SyncHooks { syncHooksImpl }

    private static let writeDebounceNanos: UInt64 = 5_000_000_000

    init(
        auth: AuthStore,
        ownerStore: AccountOwnerStore,
        syncRepo: SyncRepository,
        prefs: PreferencesStore,
        timerEngine: TimerEngine
    ) {
        self.auth = auth
        self.ownerStore = ownerStore
        self.syncRepo = syncRepo
        self.prefs = prefs
        self.timerEngine = timerEngine

        let userId: @Sendable () async -> String = { [auth] in
            await MainActorGate.perform {
                guard let id = auth.currentUser?.userId else {
                    preconditionFailure("Sync ran without a signed-in user")
                }
                return id
            }
        }

        let applySyncedPrefs: @MainActor @Sendable (String, String, Bool, Bool) -> Void = {
            [prefs, timerEngine] displayName, settingsJson, overrideEnabled, overrideActive in
            prefs.applySyncedProfile(displayName: displayName)
            prefs.applyPremiumOverride(enabled: overrideEnabled, active: overrideActive)
            timerEngine.applySyncedSettings(fromJson: settingsJson)
        }

        let tables = TableSyncs.makeAll(
            repo: syncRepo,
            userId: userId,
            applySyncedPrefs: applySyncedPrefs
        )

        self.engine = SyncEngine(
            api: SupabaseSyncApi(client: auth.client),
            tables: tables,
            checkpoints: syncRepo,
            isFatal: SyncAuthError.shouldSignOut
        )

        syncHooksImpl.target = self
    }

    func start() {
        localWritesCancellable = syncRepo.localWrites
            .receive(on: DispatchQueue.main)
            .sink { [weak self] in self?.scheduleDebouncedSync() }

        Task { await reconcileOwnershipOnStartup() }
    }

    func requestImmediateSync() {
        Task { await syncForeground() }
    }

    func syncIfEligible() async -> SyncEngine.SyncResult? {
        let userId = auth.currentUser?.userId
        let ownerId = ownerStore.currentOwnerId()
        guard canSync(userId: userId, ownerId: ownerId) else {
            statusStore.record(eligible: false, result: nil)
            return nil
        }
        do {
            let result = try await engine.syncOnce()
            statusStore.record(eligible: true, result: result)
            onStoresShouldReload?()
            return result
        } catch {
            if error is CancellationError { return nil }
            if SyncAuthError.shouldSignOut(error) {
                try? await auth.signOut()
            }
            statusStore.record(eligible: true, result: nil)
            return nil
        }
    }

    func flushBeforeLogout() async -> LogoutDecision {
        let wasEligible = canSync(
            userId: auth.currentUser?.userId,
            ownerId: ownerStore.currentOwnerId()
        )
        let result = await syncIfEligible()
        let flushSucceeded = !wasEligible || result == .success
        return logoutDecision(flushSucceeded: flushSucceeded, dirtyRemaining: syncRepo.hasAnyDirty())
    }

    func claimMergeSync() async {
        syncRepo.markAllAccountDataDirty()
        syncRepo.resetSyncCheckpoints()
        _ = await syncIfEligible()
    }

    func resetCheckpointsAndPull() async {
        syncRepo.resetSyncCheckpoints()
        _ = await syncIfEligible()
    }

    private func scheduleDebouncedSync() {
        debounceTask?.cancel()
        debounceTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: Self.writeDebounceNanos)
            guard !Task.isCancelled else { return }
            await self?.syncForeground()
        }
    }

    private func syncForeground() async {
        _ = await syncIfEligible()
    }

    private func reconcileOwnershipOnStartup() async {
        await reconcileStartupOwnership(
            userId: auth.currentUser?.userId,
            ownerId: ownerStore.currentOwnerId(),
            hasLocalData: syncRepo.hasAnyAccountOwnedData(),
            setOwnerId: { [ownerStore] userId in ownerStore.setOwnerId(userId) },
            claimMerge: { [weak self] in await self?.claimMergeSync() },
            restoreFromCloud: { [weak self] in await self?.resetCheckpointsAndPull() },
            signOut: { [auth] in try? await auth.signOut() }
        )
    }
}

@MainActor
private final class SyncHooksImpl: AccountManager.SyncHooks {
    weak var target: SyncCoordinator?

    func claimMergeSync() async { await target?.claimMergeSync() }
    func deltaSync() async { _ = await target?.syncIfEligible() }
    func resetCheckpointsAndPull() async { await target?.resetCheckpointsAndPull() }
}
