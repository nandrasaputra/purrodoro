//
//  PurrodoroApp.swift
//  Purrodoro
//
//  Created by Nandra Saputra on 03/06/26.
//

import SwiftUI

@main
struct PurrodoroApp: App {
    @StateObject private var engine = TimerEngine()
    @StateObject private var tasks = TaskStore()
    @StateObject private var habits = HabitStore()
    @StateObject private var prefs = PreferencesStore()
    @StateObject private var history = SessionHistoryStore()
    @StateObject private var sounds = SoundsStore()
    @StateObject private var loc = Localization()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(engine)
                .environmentObject(tasks)
                .environmentObject(habits)
                .environmentObject(prefs)
                .environmentObject(history)
                .environmentObject(sounds)
                .environmentObject(loc)
                .preferredColorScheme(prefs.appearanceMode.preferredColorScheme)
                .onAppear {
                    engine.onFocusSessionCompleted = { [weak tasks, weak habits, weak history] minutes in
                        // Capture the active intent name before crediting: a task
                        // that meets its planned sessions auto-completes and clears
                        // itself, so reading the name afterwards could miss it.
                        let intentName = tasks?.activeTask?.title ?? habits?.activeHabit?.name
                        // Only the active intent (task or habit) holds an id, so
                        // crediting both is safe — the inactive store is a no-op.
                        tasks?.creditActiveFocusSession()
                        habits?.creditActiveFocusSession()
                        history?.record(minutes: minutes, intentName: intentName)
                    }
                    engine.activeIntentNameProvider = { [weak tasks, weak habits] in
                        tasks?.activeTask?.title ?? habits?.activeHabit?.name
                    }
                }
        }
    }
}
