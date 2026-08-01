//
//  HomeView.swift
//  Purrodoro
//
//  The focus home screen: greeting, mascot, the circular countdown with
//  cycle progress dots, and the start / pause / skip / stop controls.
//

import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @EnvironmentObject private var habits: HabitStore
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var sounds: SoundsStore
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var showProfile = false
    @State private var showStartFocus = false
    @State private var startPrefill: StartFocusPrefill?

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()

            VStack(spacing: 20) {
                header

                Spacer(minLength: 8)

                TimerDial(
                    remaining: engine.displayRemaining,
                    total: engine.session.currentItem?.type.seconds ?? engine.settings.seconds(for: .focus),
                    mode: engine.currentMode,
                    state: engine.session.timerState,
                    colors: colors
                )

                CycleDotsRow(dots: engine.dots, colors: colors)

                Spacer(minLength: 8)

                controls
            }
            .padding(.horizontal, 28)
            .padding(.top, 12)
            .padding(.bottom, 120)

            if engine.pendingCycleComplete {
                CelebrationOverlay { engine.acknowledgeCycleComplete() }
                    .zIndex(2)
            }
        }
        .environment(\.pColors, colors)
        .sheet(isPresented: $showProfile) {
            ProfileView()
                .environmentObject(engine)
                .environmentObject(tasks)
                .environmentObject(habits)
                .environmentObject(prefs)
                .environmentObject(history)
                .environmentObject(sounds)
                .environmentObject(loc)
        }
        .sheet(isPresented: $showStartFocus) {
            StartFocusSheet(prefill: startPrefill)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { engine.reconcile() }
        }
        .animation(.easeInOut(duration: 0.25), value: engine.pendingCycleComplete)
        // Haptics: weight the feedback to the transition — starting focus is the
        // firmest, pausing/resuming softer; segment changes are a light "tick";
        // finishing a whole cycle gets a celebratory success buzz.
        .sensoryFeedback(trigger: engine.session.timerState) { old, new in
            switch new {
            case .playing: return old == .paused ? .impact(weight: .light) : .impact(weight: .medium)
            case .paused: return .impact(weight: .light)
            case .idle: return .impact(flexibility: .soft)
            }
        }
        .sensoryFeedback(.selection, trigger: engine.session.currentIndex)
        .sensoryFeedback(.success, trigger: engine.pendingCycleComplete) { _, isComplete in isComplete }
    }

    private func openStartFocus(prefill: StartFocusPrefill? = nil) {
        startPrefill = prefill
        showStartFocus = true
    }

    // MARK: Header

    private var header: some View {
        let part = DayPart.current()
        let salutation: String = {
            let key = "greeting.\(dayPartKey(part))"
            if let name = prefs.trimmedName {
                return loc.t(key + ".name", name)
            }
            return loc.t(key)
        }()
        return HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(salutation)
                    .font(.system(.title2, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(loc.t("encourage.\(dayPartKey(part))"))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }
            Spacer()
            Button {
                showProfile = true
            } label: {
                Image(systemName: "person.crop.circle")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .frame(width: 44, height: 44)
                    .background(colors.card, in: Circle())
                    .overlay(Circle().stroke(colors.border, lineWidth: 1))
            }
            .accessibilityLabel(loc.t("home.openProfile"))
        }
    }

    private func dayPartKey(_ part: DayPart) -> String {
        switch part {
        case .morning: return "morning"
        case .afternoon: return "afternoon"
        case .evening: return "evening"
        }
    }

    // MARK: Controls

    @ViewBuilder
    private var controls: some View {
        VStack(spacing: 16) {
            if let task = tasks.activeTask {
                activeIntentCard(
                    symbol: "target",
                    title: task.title,
                    subtitle: loc.t("home.focusingSessions", task.completedSessions, task.plannedSessions),
                    onDetach: { tasks.clearActive() }
                )
            } else if let habit = habits.activeHabit {
                activeIntentCard(
                    emoji: habit.emoji,
                    title: habit.name,
                    subtitle: loc.t("home.habitToday", habit.todayCount, habit.targetPerDay),
                    onDetach: { habits.clearActive() }
                )
            }

            if engine.session.timerState == .idle, let interrupted = engine.interruptedFocus {
                resumeCard(interrupted)
            }

            todayStat

            switch engine.session.timerState {
            case .idle:
                PrimaryButton(title: loc.t("home.startFocus"), systemImage: "play.fill", tint: Brand.coral) {
                    openStartFocus()
                }
            case .playing:
                HStack(spacing: 14) {
                    SecondaryButton(systemImage: "stop.fill", label: loc.t("home.stop"), colors: colors) { engine.stop() }
                    PrimaryButton(title: loc.t("home.pause"), systemImage: "pause.fill", tint: Brand.coral) { engine.pause() }
                    SecondaryButton(systemImage: "forward.fill", label: loc.t("home.skip"), colors: colors) { engine.skip() }
                }
            case .paused:
                HStack(spacing: 14) {
                    SecondaryButton(systemImage: "stop.fill", label: loc.t("home.stop"), colors: colors) { engine.stop() }
                    PrimaryButton(title: loc.t("home.resume"), systemImage: "play.fill", tint: Brand.coral) { engine.resume() }
                    SecondaryButton(systemImage: "forward.fill", label: loc.t("home.skip"), colors: colors) { engine.skip() }
                }
            }
        }
    }

    private func resumeCard(_ interrupted: InterruptedFocus) -> some View {
        Button {
            openStartFocus(prefill: StartFocusPrefill(focusMinutesOverride: interrupted.leftoverMinutes))
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "arrow.clockwise.circle.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(Brand.coral)
                VStack(alignment: .leading, spacing: 1) {
                    Text(loc.t("resume.title"))
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)
                    Text(loc.t("resume.subtitle", interrupted.leftoverMinutes))
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
                Spacer()
                Button {
                    engine.clearInterruptedFocus()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 18))
                        .foregroundStyle(colors.textFaint)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(loc.t("home.detach"))
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity)
            .background(Brand.peach.opacity(0.18), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Brand.coral.opacity(0.5), lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func activeIntentCard(
        symbol: String? = nil,
        emoji: String? = nil,
        title: String,
        subtitle: String,
        onDetach: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 10) {
            if let emoji {
                Text(emoji).font(.system(size: 18))
            } else if let symbol {
                Image(systemName: symbol)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Brand.coral)
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                Text(subtitle)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textFaint)
            }
            Spacer()
            Button(action: onDetach) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 18))
                    .foregroundStyle(colors.textFaint)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(loc.t("home.detach"))
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Brand.coral.opacity(0.5), lineWidth: 1))
    }

    private var todayStat: some View {
        HStack(spacing: 18) {
            stat(value: "\(engine.todayFocusCount)", label: loc.t("home.focusToday"))
            Rectangle().fill(colors.border).frame(width: 1, height: 28)
            stat(value: "\(engine.todayFocusMinutes)m", label: loc.t("home.minutes"))
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 22)
        .background(colors.card, in: Capsule())
        .overlay(Capsule().stroke(colors.border, lineWidth: 1))
    }

    private func stat(value: String, label: String) -> some View {
        VStack(spacing: 1) {
            Text(value)
                .font(.system(.headline, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
            Text(label)
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(colors.textFaint)
        }
    }
}

// MARK: - Timer dial

private struct TimerDial: View {
    @EnvironmentObject private var loc: Localization
    let remaining: Int
    let total: Int
    let mode: TimerMode
    let state: TimerState
    let colors: PurrodoroColors

    private var progress: Double {
        guard total > 0 else { return 0 }
        return min(max(Double(total - remaining) / Double(total), 0), 1)
    }

    private static let spokenDuration: DateComponentsFormatter = {
        let f = DateComponentsFormatter()
        f.allowedUnits = [.minute, .second]
        f.unitsStyle = .full
        return f
    }()

    private var accessibilityLabelText: String {
        let modeName = state == .idle ? loc.t("mode.ready") : loc.t(mode.labelKey)
        return loc.t("a11y.timer", modeName)
    }

    private var accessibilityValueText: String {
        if state == .idle { return formatSeconds(remaining) }
        let spoken = Self.spokenDuration.string(from: Double(remaining)) ?? formatSeconds(remaining)
        let base = loc.t("a11y.remaining", spoken)
        return state == .paused ? "\(loc.t("a11y.paused")). \(base)" : base
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(colors.border, lineWidth: 14)

            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    mode.accentStrong,
                    style: StrokeStyle(lineWidth: 14, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.3), value: progress)

            VStack(spacing: 10) {
                Image(mode == .focus ? "CatFocusing" : mode.mascotImageName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 96, height: 96)
                    .opacity(state == .idle ? 0.0 : 1.0)
                    .overlay {
                        if state == .idle {
                            Image("CatReady")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 104, height: 104)
                        }
                    }

                Text(formatSeconds(remaining))
                    .font(.system(size: 56, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(colors.textPrimary)

                ModeChip(mode: mode, state: state)
            }
        }
        .frame(width: 300, height: 300)
        .padding(.vertical, 8)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabelText)
        .accessibilityValue(accessibilityValueText)
    }
}

private struct ModeChip: View {
    @EnvironmentObject private var loc: Localization
    let mode: TimerMode
    let state: TimerState

    var body: some View {
        let text = state == .idle ? loc.t("mode.ready") : loc.t(mode.labelKey)
        Text(text.uppercased())
            .font(.system(.caption, design: .rounded).weight(.bold))
            .tracking(1.2)
            .foregroundStyle(Brand.cocoa)
            .padding(.vertical, 6)
            .padding(.horizontal, 14)
            .background(mode.accent, in: Capsule())
    }
}

// MARK: - Cycle dots

struct CycleDotsRow: View {
    @EnvironmentObject private var loc: Localization
    let dots: CycleDotsState
    let colors: PurrodoroColors

    var body: some View {
        HStack(spacing: 12) {
            ForEach(0..<dots.totalFocusSessions, id: \.self) { i in
                Circle()
                    .fill(fill(for: i))
                    .frame(width: dotSize(for: i), height: dotSize(for: i))
                    .overlay(Circle().stroke(colors.border, lineWidth: isActive(i) ? 0 : 1))
                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: dots)
            }
        }
        .frame(height: 18)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(loc.t("a11y.cycleProgress"))
        .accessibilityValue(loc.t("a11y.sessionsOf", dots.completedFocusSessions, dots.totalFocusSessions))
    }

    private func isActive(_ i: Int) -> Bool {
        dots.isCurrentlyFocus && i == dots.completedFocusSessions
    }

    private func fill(for i: Int) -> Color {
        if i < dots.completedFocusSessions { return Brand.coral }
        if isActive(i) { return Brand.peach }
        return colors.card
    }

    private func dotSize(for i: Int) -> CGFloat {
        isActive(i) ? 16 : 12
    }
}

// MARK: - Buttons

struct PrimaryButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: systemImage)
                Text(title)
            }
            .font(.system(.headline, design: .rounded).weight(.bold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 58)
            .background(tint, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: tint.opacity(0.35), radius: 12, y: 6)
        }
        .buttonStyle(.plain)
    }
}

struct SecondaryButton: View {
    let systemImage: String
    let label: String
    let colors: PurrodoroColors
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: systemImage)
                    .font(.system(size: 18, weight: .semibold))
                Text(label)
                    .font(.system(.caption2, design: .rounded).weight(.semibold))
            }
            .foregroundStyle(colors.textPrimary)
            .frame(width: 64, height: 58)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(colors.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

#Preview {
    HomeView()
        .environmentObject(TimerEngine())
        .environmentObject(TaskStore())
        .environmentObject(HabitStore())
        .environmentObject(PreferencesStore())
        .environmentObject(SoundsStore())
        .environmentObject(Localization())
}
