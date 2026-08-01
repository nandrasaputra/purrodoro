//
//  PurrodoroWidgetsBundle.swift
//  PurrodoroWidgets
//
//  Entry point for the Purrodoro widget extension: Live Activity plus the
//  Home Screen widgets (Today + Android Glance parity set).
//

import SwiftUI
import WidgetKit

@main
struct PurrodoroWidgetsBundle: WidgetBundle {
    var body: some Widget {
        TodayWidget()
        StatsGridWidget()
        DailyPlanWidget()
        StreakHeroWidget()
        NudgeDashboardWidget()
        CalmMinimalWidget()
        CozySceneWidget()
#if canImport(ActivityKit)
        FocusLiveActivity()
#endif
    }
}
