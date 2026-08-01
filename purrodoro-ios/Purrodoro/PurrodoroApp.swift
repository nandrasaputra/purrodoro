//
//  PurrodoroApp.swift
//  Purrodoro
//
//  Created by Nandra Saputra on 03/06/26.
//

import SwiftUI

private var isRunningUnitTests: Bool {
    NSClassFromString("XCTestCase") != nil
}

@main
struct PurrodoroApp: App {
    @StateObject private var syncRepo: SyncRepository
    @StateObject private var auth: AuthStore
    @StateObject private var ownerStore: AccountOwnerStore
    @StateObject private var engine: TimerEngine
    @StateObject private var tasks: TaskStore
    @StateObject private var habits: HabitStore
    @StateObject private var prefs: PreferencesStore
    @StateObject private var history: SessionHistoryStore
    @StateObject private var sounds: SoundsStore
    @StateObject private var storeKit: StoreKitManager
    @StateObject private var syncCoordinator: SyncCoordinator
    @StateObject private var focusMode: FocusModeController
    @StateObject private var tutorial: TutorialController
    @StateObject private var deepLinks: DeepLinkRouter
    @StateObject private var loc: Localization

    @Environment(\.scenePhase) private var scenePhase

    init() {
        let sync = SyncRepository()
        let store = sync.store
        let auth = AuthStore()
        let ownerStore = AccountOwnerStore(store: store)
        let engine = TimerEngine(syncRepo: sync, store: store)
        let tasks = TaskStore(syncRepo: sync, store: store)
        let habits = HabitStore(syncRepo: sync, store: store)
        let prefs = PreferencesStore(syncRepo: sync, store: store)
        let history = SessionHistoryStore(syncRepo: sync)
        let storeKit = StoreKitManager(prefs: prefs)
        let loc = Localization(store: store)
        let sounds = SoundsStore(
            store: store,
            client: auth.client,
            prefs: prefs,
            localization: loc
        )

        tasks.focusMinutesProvider = { engine.settings.focusMinutes }
        prefs.timerSettingsProvider = { engine.settings }
        history.timerSettingsProvider = { engine.settings }

        let coordinator = SyncCoordinator(
            auth: auth,
            ownerStore: ownerStore,
            syncRepo: sync,
            prefs: prefs,
            timerEngine: engine
        )
        coordinator.onStoresShouldReload = {
            tasks.reloadFromSync()
            habits.reloadFromSync()
            history.reloadFromSync()
            prefs.reloadFromSync()
            engine.reloadFromSync()
        }

        _syncRepo = StateObject(wrappedValue: sync)
        _auth = StateObject(wrappedValue: auth)
        _ownerStore = StateObject(wrappedValue: ownerStore)
        _engine = StateObject(wrappedValue: engine)
        _tasks = StateObject(wrappedValue: tasks)
        _habits = StateObject(wrappedValue: habits)
        _prefs = StateObject(wrappedValue: prefs)
        _history = StateObject(wrappedValue: history)
        _sounds = StateObject(wrappedValue: sounds)
        _storeKit = StateObject(wrappedValue: storeKit)
        _syncCoordinator = StateObject(wrappedValue: coordinator)
        _focusMode = StateObject(wrappedValue: FocusModeController())
        _tutorial = StateObject(wrappedValue: TutorialController())
        _deepLinks = StateObject(wrappedValue: DeepLinkRouter())
        _loc = StateObject(wrappedValue: loc)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .environmentObject(ownerStore)
                .environmentObject(syncRepo)
                .environmentObject(syncCoordinator)
                .environmentObject(engine)
                .environmentObject(tasks)
                .environmentObject(habits)
                .environmentObject(prefs)
                .environmentObject(history)
                .environmentObject(sounds)
                .environmentObject(storeKit)
                .environmentObject(focusMode)
                .environmentObject(tutorial)
                .environmentObject(deepLinks)
                .environmentObject(loc)
                .preferredColorScheme(prefs.appearanceMode.preferredColorScheme)
                .onOpenURL { url in
                    if deepLinks.handle(url) { return }
                    _ = auth.handleOpenURL(url)
                }
                .onAppear {
                    if !isRunningUnitTests {
                        syncCoordinator.start()
                        storeKit.start()
                        Task {
                            await storeKit.refreshCatalog()
                            await storeKit.refreshEntitlements()
                            await sounds.refreshCatalog()
                        }
                    }
                    engine.onFocusSessionCompleted = { [weak tasks, weak habits, weak history, weak prefs] minutes in
                        let intentName = tasks?.activeTask?.title
                            ?? habits?.activeHabit?.name
                            ?? prefs?.activeSimpleNote
                        tasks?.creditActiveFocusSession()
                        habits?.creditActiveFocusSession()
                        history?.record(minutes: minutes, intentName: intentName)
                    }
                    engine.activeIntentNameProvider = { [weak tasks, weak habits, weak prefs] in
                        tasks?.activeTask?.title
                            ?? habits?.activeHabit?.name
                            ?? prefs?.activeSimpleNote
                    }
                }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active, !isRunningUnitTests {
                        syncCoordinator.requestImmediateSync()
                        Task {
                            await storeKit.refreshEntitlements()
                            await sounds.refreshCatalog()
                        }
                    }
                }
                .onChange(of: auth.state) { _, state in
                    if case .signedIn = state, !isRunningUnitTests {
                        syncCoordinator.requestImmediateSync()
                        Task {
                            await storeKit.refreshCatalog()
                            await storeKit.refreshEntitlements()
                        }
                    }
                }
        }
    }
}
