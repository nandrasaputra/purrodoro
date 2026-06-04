//
//  EngineTests.swift
//  PurrodoroTests
//
//  Transition tests for `TimerEngine` (start / pause / resume / skip / stop /
//  cycle completion) driven on the main actor, plus the pure notification-chain
//  builder used for backgrounded auto-start.
//

import Testing
import Foundation
@testable import Purrodoro

@MainActor
struct EngineTransitionTests {

    private func makeEngine(autoStart: Bool = false, n: Int = 4) -> TimerEngine {
        let engine = TimerEngine()
        engine.settings = TimerSettings(
            focusMinutes: 25,
            shortBreakMinutes: 5,
            longBreakMinutes: 15,
            autoStartNextSession: autoStart,
            focusSessionsPerCycle: n
        )
        return engine
    }

    @Test func startsIntoRunningFocus() {
        let e = makeEngine()
        #expect(!e.isStarted)
        e.start()
        #expect(e.isStarted)
        #expect(e.isRunning)
        #expect(e.currentMode == .focus)
        // (Focus + ShortBreak) × 3 + Focus + LongBreak = 8 segments.
        #expect(e.session.items.count == 8)
    }

    @Test func pauseThenResume() {
        let e = makeEngine()
        e.start()
        e.pause()
        #expect(!e.isRunning)
        #expect(e.session.timerState == .paused)
        #expect(e.displayRemaining > 0)
        e.resume()
        #expect(e.isRunning)
        #expect(e.session.timerState == .playing)
    }

    @Test func skipAdvancesToBreakPausedWhenAutoStartOff() {
        let e = makeEngine(autoStart: false)
        e.start()
        e.skip()
        #expect(e.session.currentIndex == 1)
        #expect(e.currentMode == .shortBreak)
        #expect(e.session.timerState == .paused)
    }

    @Test func skipAdvancesAndAutoStartsWhenOn() {
        let e = makeEngine(autoStart: true)
        e.start()
        e.skip()
        #expect(e.session.currentIndex == 1)
        #expect(e.currentMode == .shortBreak)
        #expect(e.session.timerState == .playing)
    }

    @Test func skippingThroughAllSegmentsCompletesCycle() {
        let e = makeEngine(autoStart: true, n: 1) // items: Focus, LongBreak
        e.start()
        #expect(e.session.items.count == 2)
        e.skip()
        #expect(e.currentMode == .longBreak)
        e.skip() // advance past the final segment → finish
        #expect(e.pendingCycleComplete)
        #expect(!e.isStarted)
        #expect(e.session.currentIndex == 0)
        e.acknowledgeCycleComplete()
        #expect(!e.pendingCycleComplete)
    }

    @Test func stopResetsToIdle() {
        let e = makeEngine(autoStart: true)
        e.start()
        e.skip() // off the first focus segment
        e.stop()
        #expect(!e.isStarted)
        #expect(e.session.timerState == .idle)
        #expect(e.session.currentIndex == 0)
    }

    @Test func stopAtStartDoesNotCaptureInterruptedFocus() {
        // Stopping with the full segment still remaining isn't worth resuming.
        let e = makeEngine()
        e.start()
        e.stop()
        #expect(e.interruptedFocus == nil)
    }

    @Test func toggleStartPauseCyclesThroughStates() {
        let e = makeEngine()
        e.toggleStartPause() // idle → playing
        #expect(e.isRunning)
        e.toggleStartPause() // playing → paused
        #expect(!e.isRunning)
        e.toggleStartPause() // paused → playing
        #expect(e.isRunning)
    }

    @Test func settingsPersistThroughInjectedStore() {
        let store = InMemoryKeyValueStore()
        let e1 = TimerEngine(store: store)
        e1.settings = TimerSettings(
            focusMinutes: 42, shortBreakMinutes: 5, longBreakMinutes: 15,
            autoStartNextSession: true, focusSessionsPerCycle: 2
        )
        // A fresh engine over the same store reloads the persisted settings.
        let e2 = TimerEngine(store: store)
        #expect(e2.settings.focusMinutes == 42)
        #expect(e2.settings.focusSessionsPerCycle == 2)
        #expect(e2.settings.autoStartNextSession)
    }

    @Test func dotsReflectProgress() {
        let e = makeEngine(autoStart: true, n: 4)
        e.start()
        #expect(e.dots.totalFocusSessions == 4)
        #expect(e.dots.completedFocusSessions == 0)
        #expect(e.dots.isCurrentlyFocus)
        e.skip() // → short break (index 1)
        #expect(e.dots.completedFocusSessions == 1)
        #expect(!e.dots.isCurrentlyFocus)
    }
}

// MARK: - Notification chain (pure)

struct NotificationChainTests {

    private func cycle(n: Int) -> [TimerSessionItem] {
        buildCycle(TimerSettings(
            focusMinutes: 25, shortBreakMinutes: 5, longBreakMinutes: 15,
            autoStartNextSession: false, focusSessionsPerCycle: n
        ))
    }

    @Test func autoStartOffSchedulesOnlyCurrentBoundary() {
        let items = cycle(n: 4)
        let chain = TimerEngine.notificationChain(
            items: items, currentIndex: 0, firstBoundary: Date(timeIntervalSince1970: 1000), autoStart: false
        )
        #expect(chain.count == 1)
        #expect(chain[0].date == Date(timeIntervalSince1970: 1000))
    }

    @Test func autoStartOnChainsRemainingBoundaries() {
        let items = cycle(n: 4) // 8 segments
        let start = Date(timeIntervalSince1970: 0)
        let chain = TimerEngine.notificationChain(
            items: items, currentIndex: 0, firstBoundary: start, autoStart: true
        )
        // One notification per remaining segment boundary.
        #expect(chain.count == 8)
        // Second boundary = first focus end (already the firstBoundary) + the
        // following short break (5 min).
        #expect(chain[0].date == start)
        #expect(chain[1].date == start.addingTimeInterval(5 * 60))
        // Third = + the next focus (25 min).
        #expect(chain[2].date == start.addingTimeInterval(5 * 60 + 25 * 60))
    }

    @Test func chainFromMidCycleStopsAtEnd() {
        let items = cycle(n: 4) // indices 0..7, last = 7 (long break)
        let chain = TimerEngine.notificationChain(
            items: items, currentIndex: 6, firstBoundary: Date(timeIntervalSince1970: 0), autoStart: true
        )
        // Segments 6 (focus) and 7 (long break) remain.
        #expect(chain.count == 2)
    }

    @Test func outOfRangeIndexYieldsEmpty() {
        let chain = TimerEngine.notificationChain(
            items: cycle(n: 1), currentIndex: 9, firstBoundary: Date(), autoStart: true
        )
        #expect(chain.isEmpty)
    }
}
