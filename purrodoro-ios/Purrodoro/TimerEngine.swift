//
//  TimerEngine.swift
//  Purrodoro
//
//  Foreground-driven Pomodoro engine. Mirrors the behaviour of the Android
//  `TimerStateMachine` / `TimerService` pair, adapted for a single-process
//  SwiftUI app: while playing, the remaining time is derived from a target
//  `endsAt` date so the countdown stays correct across backgrounding.
//

import Foundation
import Combine

@MainActor
final class TimerEngine: ObservableObject {

    // MARK: Published state

    @Published private(set) var session: TimerSession
    @Published private(set) var displayRemaining: Int
    @Published private(set) var pendingCycleComplete = false
    @Published private(set) var todayFocusCount = 0
    @Published private(set) var todayFocusMinutes = 0

    /// Invoked each time a Focus segment completes. Wired by the app to credit
    /// the active task so a session counts toward its planned sessions.
    var onFocusSessionCompleted: (() -> Void)?

    @Published var settings: TimerSettings {
        didSet {
            persistSettings()
            // When idle, reflect new durations immediately in the preview.
            if session.timerState == .idle {
                session = buildSession(settings)
                displayRemaining = session.remainingSeconds
            }
        }
    }

    // MARK: Private

    private var endsAt: Date?
    private var ticker: AnyCancellable?
    private let store = UserDefaults.standard
    private let settingsKey = "purrodoro.settings"
    private let stampsKey = "purrodoro.focusStamps"
    private let minutesKey = "purrodoro.focusMinutes"

    // MARK: Init

    init() {
        let loaded = Self.loadSettings(store, key: settingsKey)
        self.settings = loaded
        let session = buildSession(loaded)
        self.session = session
        self.displayRemaining = session.remainingSeconds
        refreshTodayMetrics()
    }

    // MARK: Derived

    var currentMode: TimerMode {
        session.currentItem?.type.mode ?? .focus
    }

    var isRunning: Bool { session.timerState == .playing }
    var isStarted: Bool { session.timerState != .idle }

    /// Per-day focus-session counts, keyed "yyyy-MM-dd".
    var focusStampsByDay: [String: Int] {
        store.dictionary(forKey: stampsKey) as? [String: Int] ?? [:]
    }

    /// Per-day focus minutes, keyed "yyyy-MM-dd".
    var focusMinutesByDay: [String: Int] {
        store.dictionary(forKey: minutesKey) as? [String: Int] ?? [:]
    }

    var dots: CycleDotsState {
        cycleDotsState(
            cycleItems: session.items,
            cycleIndex: session.currentIndex,
            timerState: session.timerState,
            settings: settings
        )
    }

    // MARK: Intents

    func start() {
        session = buildSession(settings)
        beginCurrentSegment()
    }

    func pause() {
        guard session.timerState == .playing else { return }
        session.remainingSeconds = remainingFromClock()
        session.timerState = .paused
        endsAt = nil
        displayRemaining = session.remainingSeconds
        stopTicker()
    }

    func resume() {
        guard session.timerState == .paused else { return }
        session.timerState = .playing
        endsAt = Date().addingTimeInterval(TimeInterval(session.remainingSeconds))
        startTicker()
    }

    func toggleStartPause() {
        switch session.timerState {
        case .idle: start()
        case .playing: pause()
        case .paused: resume()
        }
    }

    /// Skip the current segment. Advancing past the final segment ends the cycle.
    func skip() {
        guard isStarted else { return }
        advanceSegment(creditCurrentFocus: false)
    }

    func stop() {
        endsAt = nil
        stopTicker()
        session = buildSession(settings)
        displayRemaining = session.remainingSeconds
    }

    func acknowledgeCycleComplete() {
        pendingCycleComplete = false
    }

    /// Recompute state after returning from background: advance through any
    /// segments whose target time elapsed while we weren't ticking.
    func reconcile() {
        guard session.timerState == .playing else { return }
        while session.timerState == .playing, let target = endsAt, Date() >= target {
            advanceSegment(creditCurrentFocus: true, base: target)
        }
        if session.timerState == .playing {
            displayRemaining = remainingFromClock()
            if ticker == nil { startTicker() }
        }
    }

    // MARK: Segment lifecycle

    private func beginCurrentSegment() {
        let seconds = session.currentItem?.type.seconds ?? settings.seconds(for: .focus)
        session.remainingSeconds = seconds
        if settings.autoStartNextSession || session.currentIndex == 0 {
            session.timerState = .playing
            endsAt = Date().addingTimeInterval(TimeInterval(seconds))
            displayRemaining = seconds
            startTicker()
        } else {
            session.timerState = .paused
            endsAt = nil
            displayRemaining = seconds
            stopTicker()
        }
    }

    /// Advance to the next segment. When [creditCurrentFocus] is true and the
    /// segment we're leaving is a Focus, it counts toward today's stamp.
    /// [base] anchors the next segment's target so reconcile can chain accurately.
    private func advanceSegment(creditCurrentFocus: Bool, base: Date? = nil) {
        if creditCurrentFocus, let item = session.currentItem, item.type.isFocus {
            creditFocus(minutes: item.type.seconds / 60)
        }

        let nextIndex = session.currentIndex + 1
        if nextIndex >= session.items.count {
            finishCycle()
            return
        }

        session.currentIndex = nextIndex
        let nextSeconds = session.items[nextIndex].type.seconds
        session.remainingSeconds = nextSeconds

        if settings.autoStartNextSession {
            session.timerState = .playing
            let anchor = base ?? Date()
            endsAt = anchor.addingTimeInterval(TimeInterval(nextSeconds))
            displayRemaining = remainingFromClock()
            startTicker()
        } else {
            session.timerState = .paused
            endsAt = nil
            displayRemaining = nextSeconds
            stopTicker()
        }
    }

    private func finishCycle() {
        endsAt = nil
        stopTicker()
        pendingCycleComplete = true
        session = buildSession(settings)
        displayRemaining = session.remainingSeconds
    }

    // MARK: Ticking

    private func startTicker() {
        guard ticker == nil else { return }
        ticker = Timer.publish(every: 0.25, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in self?.tick() }
    }

    private func stopTicker() {
        ticker?.cancel()
        ticker = nil
    }

    private func tick() {
        guard session.timerState == .playing else { return }
        let remaining = remainingFromClock()
        if remaining <= 0 {
            advanceSegment(creditCurrentFocus: true, base: endsAt)
        } else {
            displayRemaining = remaining
        }
    }

    private func remainingFromClock() -> Int {
        guard let endsAt else { return session.remainingSeconds }
        return max(0, Int(ceil(endsAt.timeIntervalSinceNow)))
    }

    // MARK: Persistence

    private func creditFocus(minutes: Int) {
        let key = Self.dayKey(Date())
        var stamps = store.dictionary(forKey: stampsKey) as? [String: Int] ?? [:]
        stamps[key, default: 0] += 1
        store.set(stamps, forKey: stampsKey)

        var mins = store.dictionary(forKey: minutesKey) as? [String: Int] ?? [:]
        mins[key, default: 0] += minutes
        store.set(mins, forKey: minutesKey)

        refreshTodayMetrics()
        onFocusSessionCompleted?()
    }

    private func refreshTodayMetrics() {
        let key = Self.dayKey(Date())
        let stamps = store.dictionary(forKey: stampsKey) as? [String: Int] ?? [:]
        let mins = store.dictionary(forKey: minutesKey) as? [String: Int] ?? [:]
        todayFocusCount = stamps[key] ?? 0
        todayFocusMinutes = mins[key] ?? 0
    }

    private func persistSettings() {
        if let data = try? JSONEncoder().encode(settings) {
            store.set(data, forKey: settingsKey)
        }
    }

    private static func loadSettings(_ store: UserDefaults, key: String) -> TimerSettings {
        guard let data = store.data(forKey: key),
              let decoded = try? JSONDecoder().decode(TimerSettings.self, from: data)
        else { return TimerSettings() }
        return decoded
    }

    private static func dayKey(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: date)
    }
}
