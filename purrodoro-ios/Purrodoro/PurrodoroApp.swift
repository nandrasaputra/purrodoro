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

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(engine)
                .environmentObject(tasks)
                .environmentObject(habits)
                .onAppear {
                    engine.onFocusSessionCompleted = { [weak tasks, weak habits] in
                        // Only the active intent (task or habit) holds an id, so
                        // crediting both is safe — the inactive store is a no-op.
                        tasks?.creditActiveFocusSession()
                        habits?.creditActiveFocusSession()
                    }
                }
        }
    }
}
