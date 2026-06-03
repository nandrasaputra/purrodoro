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

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environmentObject(engine)
        }
    }
}
