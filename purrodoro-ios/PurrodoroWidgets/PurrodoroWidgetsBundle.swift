//
//  PurrodoroWidgetsBundle.swift
//  PurrodoroWidgets
//
//  Entry point for the Purrodoro widget extension: the running-timer Live
//  Activity plus the "Today" Home Screen widget.
//

import SwiftUI
import WidgetKit

@main
struct PurrodoroWidgetsBundle: WidgetBundle {
    var body: some Widget {
        TodayWidget()
#if canImport(ActivityKit)
        FocusLiveActivity()
#endif
    }
}
