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
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var showSettings = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()

            VStack(spacing: 24) {
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
            .padding(.bottom, 96)
        }
        .environment(\.pColors, colors)
        .sheet(isPresented: $showSettings) {
            SettingsSheet()
                .environmentObject(engine)
                .environmentObject(prefs)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { engine.reconcile() }
        }
        .alert("Cycle complete", isPresented: cycleCompleteBinding) {
            Button("Nice") { engine.acknowledgeCycleComplete() }
        } message: {
            Text("Purrodoro is proud of you. Take a moment, then start fresh whenever you're ready.")
        }
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

    private var cycleCompleteBinding: Binding<Bool> {
        Binding(
            get: { engine.pendingCycleComplete },
            set: { if !$0 { engine.acknowledgeCycleComplete() } }
        )
    }

    // MARK: Header

    private var header: some View {
        let part = DayPart.current()
        let salutation = prefs.trimmedName.map { "\(part.salutation), \($0)" } ?? part.salutation
        return HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(salutation)
                    .font(.system(.title2, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(part.encouragement)
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }
            Spacer()
            Button {
                showSettings = true
            } label: {
                Image(systemName: "slider.horizontal.3")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .frame(width: 44, height: 44)
                    .background(colors.card, in: Circle())
                    .overlay(Circle().stroke(colors.border, lineWidth: 1))
            }
            .accessibilityLabel("Timer settings")
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
                    subtitle: "Focusing · \(task.completedSessions)/\(task.plannedSessions) sessions",
                    onDetach: { tasks.clearActive() }
                )
            } else if let habit = habits.activeHabit {
                activeIntentCard(
                    emoji: habit.emoji,
                    title: habit.name,
                    subtitle: "Habit · \(habit.todayCount)/\(habit.targetPerDay) today",
                    onDetach: { habits.clearActive() }
                )
            }

            todayStat

            switch engine.session.timerState {
            case .idle:
                PrimaryButton(title: "Start Focus", systemImage: "play.fill", tint: Brand.coral) {
                    engine.start()
                }
            case .playing:
                HStack(spacing: 14) {
                    SecondaryButton(systemImage: "stop.fill", label: "Stop", colors: colors) { engine.stop() }
                    PrimaryButton(title: "Pause", systemImage: "pause.fill", tint: Brand.coral) { engine.pause() }
                    SecondaryButton(systemImage: "forward.fill", label: "Skip", colors: colors) { engine.skip() }
                }
            case .paused:
                HStack(spacing: 14) {
                    SecondaryButton(systemImage: "stop.fill", label: "Stop", colors: colors) { engine.stop() }
                    PrimaryButton(title: "Resume", systemImage: "play.fill", tint: Brand.coral) { engine.resume() }
                    SecondaryButton(systemImage: "forward.fill", label: "Skip", colors: colors) { engine.skip() }
                }
            }
        }
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
            .accessibilityLabel("Detach")
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Brand.coral.opacity(0.5), lineWidth: 1))
    }

    private var todayStat: some View {
        HStack(spacing: 18) {
            stat(value: "\(engine.todayFocusCount)", label: "focus today")
            Rectangle().fill(colors.border).frame(width: 1, height: 28)
            stat(value: "\(engine.todayFocusMinutes)m", label: "minutes")
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
    let remaining: Int
    let total: Int
    let mode: TimerMode
    let state: TimerState
    let colors: PurrodoroColors

    private var progress: Double {
        guard total > 0 else { return 0 }
        return min(max(Double(total - remaining) / Double(total), 0), 1)
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
    }
}

private struct ModeChip: View {
    let mode: TimerMode
    let state: TimerState

    var body: some View {
        let text = state == .idle ? "Ready" : mode.label
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
}
