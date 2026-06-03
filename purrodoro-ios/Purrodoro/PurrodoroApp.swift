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

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(engine)
                .environmentObject(tasks)
                .onAppear {
                    engine.onFocusSessionCompleted = { [weak tasks] in
                        tasks?.creditActiveFocusSession()
                    }
                }
        }
    }
}
