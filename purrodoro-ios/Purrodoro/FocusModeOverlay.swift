//
//  FocusModeOverlay.swift
//  Purrodoro
//
//  Full-screen Minimal Zen focus surface — mirrors Android `FocusModeOverlay.kt`.
//

import SwiftUI

struct FocusModeOverlay: View {
    let remainingSeconds: Int
    let mode: TimerMode
    let timerState: TimerState
    let dots: CycleDotsState
    let intentTitle: String?
    let intentIsHabit: Bool
    let isSoundscapePlaying: Bool
    let soundscapeDisplayName: String
    let showSoundscapeFirstRunHint: Bool
    let onExit: () -> Void
    let onStartPause: () -> Void
    let onStop: () -> Void
    let onSkip: () -> Void
    let onOpenSoundscape: () -> Void
    let onSoundscapePlayPause: () -> Void

    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var showStopConfirm = false
    @State private var surfaceVisible = false
    @State private var breathScale: CGFloat = 1
    @State private var hasEntered = false
    @State private var focusingVariant = 1

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    private var isBreak: Bool { mode != .focus }
    private var isRunning: Bool { timerState == .playing }
    private var isPaused: Bool { timerState == .paused }
    private var breakReady: Bool { isBreak && !isRunning }
    private var breakRunning: Bool { isBreak && isRunning }
    private var focusRunning: Bool { !isBreak && isRunning }
    private var focusPaused: Bool { !isBreak && isPaused }

    private var modeLabelKey: String {
        switch mode {
        case .focus: return "focus.mode.label.focus"
        case .shortBreak: return "focus.mode.label.shortBreak"
        case .longBreak: return "focus.mode.label.longBreak"
        }
    }

    private var subtextKey: String? {
        if breakReady { return "focus.subtext.breakReady" }
        if breakRunning { return "focus.subtext.breakRunning" }
        if focusPaused { return "focus.subtext.paused" }
        return nil
    }

    private var showIntent: Bool {
        !isBreak && intentTitle != nil
    }

    private var mascotName: String {
        switch mode {
        case .focus:
            return isRunning ? (focusingVariant == 1 ? "CatFocusing1" : "CatFocusing2") : "CatReady"
        case .shortBreak:
            return isRunning ? "CatShortBreak" : "CatReady"
        case .longBreak:
            return isRunning ? "CatLongBreak" : "CatReady"
        }
    }

    var body: some View {
        ZStack {
            colors.page
                .opacity(surfaceVisible ? 1 : 0)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Top band + timer are tappable to exit (Android tap-anywhere).
                VStack(spacing: 0) {
                    Spacer().frame(height: 24)

                    Text(loc.t(modeLabelKey))
                        .font(.system(size: 11, weight: .semibold, design: .rounded))
                        .tracking(3)
                        .foregroundStyle(colors.textSecondary)

                    Spacer().frame(height: 20)

                    Text(formatSeconds(remainingSeconds))
                        .font(.system(size: 72, weight: .regular, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                        .monospacedDigit()
                        .accessibilityLabel(loc.t("a11y.timer", loc.t(modeLabelKey)))
                        .accessibilityValue(
                            timerState == .paused
                                ? loc.t("a11y.paused") + ", " + loc.t("a11y.remaining", formatSeconds(remainingSeconds))
                                : loc.t("a11y.remaining", formatSeconds(remainingSeconds))
                        )
                        .scaleEffect(breathScale)
                        .frame(maxWidth: .infinity)

                    if let subtextKey {
                        Text(loc.t(subtextKey))
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundStyle(colors.textSecondary)
                            .padding(.top, 4)
                    }

                    if showIntent, let title = intentTitle {
                        FocusIntentPill(
                            isHabit: intentIsHabit,
                            title: title,
                            colors: colors
                        )
                        .padding(.top, 18)
                    }

                    Spacer().frame(height: showIntent ? 18 : 28)

                    FocusOverlayDots(dots: dots, colors: colors)

                    if dots.totalFocusSessions > 0 {
                        Text("\(displayedFocusCount) / \(dots.totalFocusSessions)")
                            .font(.system(.caption, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textSecondary)
                            .padding(.top, 8)
                    }

                    Spacer(minLength: 8)
                }
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
                .onTapGesture { onExit() }

                GeometryReader { geo in
                    let size = focusModeMascotMaxSize(
                        bandWidth: geo.size.width,
                        bandHeight: geo.size.height
                    )
                    Image(mascotName)
                        .resizable()
                        .scaledToFit()
                        .frame(width: size.width, height: size.height)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .accessibilityHidden(true)
                        .contentShape(Rectangle())
                        .onTapGesture { onExit() }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                SoundscapeTransportBar(
                    isPlaying: isSoundscapePlaying,
                    displayName: soundscapeDisplayName,
                    showFirstRunHint: showSoundscapeFirstRunHint,
                    onPlayPause: onSoundscapePlayPause,
                    onExpand: onOpenSoundscape
                )
                .background(colors.card, in: Capsule())
                .overlay(Capsule().stroke(colors.border, lineWidth: 1))
                .shadow(color: colors.textPrimary.opacity(0.08), radius: 8, y: 2)
                .padding(.bottom, 10)

                bottomActions
                    .padding(.bottom, 28)
            }
            .padding(.horizontal, 28)
            .scaleEffect(surfaceVisible ? 1 : 0.985)
            .offset(y: surfaceVisible ? 0 : 20)
            .opacity(surfaceVisible ? 1 : 0)
        }
        .accessibilityAction(named: loc.t("focus.exit.cd")) { onExit() }
        .alert(loc.t("home.stop.title"), isPresented: $showStopConfirm) {
            Button(loc.t("home.stop.confirm"), role: .destructive) { onStop() }
            Button(loc.t("home.stop.dismiss"), role: .cancel) {}
        } message: {
            Text(loc.t("home.stop.body"))
        }
        .onAppear {
            withAnimation(.timingCurve(0.05, 0.7, 0.1, 1.0, duration: FocusModeMotion.enterDuration)) {
                surfaceVisible = true
            }
            rollFocusingVariantIfNeeded()
            if !hasEntered {
                hasEntered = true
                scheduleBreath()
            }
        }
        .onChange(of: mode) { _, _ in rollFocusingVariantIfNeeded() }
        .onChange(of: dots.completedFocusSessions) { _, _ in rollFocusingVariantIfNeeded() }
    }

    private var displayedFocusCount: Int {
        let past = dots.completedFocusSessions
        let onCurrent = dots.isCurrentlyFocus
        let current = onCurrent ? past + 1 : past
        return min(max(current, 1), max(dots.totalFocusSessions, 1))
    }

    @ViewBuilder
    private var bottomActions: some View {
        VStack(spacing: 10) {
            if breakReady {
                FocusCoralPill(title: loc.t("focus.startBreak")) { onStartPause() }
                FocusQuietText(title: loc.t("home.skipBreak")) { onSkip() }
            } else if breakRunning {
                HStack {
                    Spacer().frame(width: 16)
                    FocusQuietText(title: loc.t("home.skipBreak")) { onSkip() }
                    Spacer()
                    FocusCoralPauseCircle { onStartPause() }
                }
            } else if focusPaused {
                FocusCoralPill(title: loc.t("focus.resumeFocus")) { onStartPause() }
                FocusQuietText(title: loc.t("home.stopSession"), systemImage: "stop.circle") {
                    showStopConfirm = true
                }
            } else if focusRunning {
                FocusSubtleSecondary(title: loc.t("focus.pauseFocus"), systemImage: "pause.fill") {
                    onStartPause()
                }
            }

            Text(loc.t("focus.tapToExit"))
                .font(.system(size: 12, weight: .medium, design: .rounded))
                .tracking(0.4)
                .foregroundStyle(colors.textSecondary.opacity(0.5))
                .frame(maxWidth: .infinity)
        }
    }

    private func rollFocusingVariantIfNeeded() {
        guard mode == .focus, isRunning else { return }
        focusingVariant = Int.random(in: 1...2)
    }

    private func scheduleBreath() {
        guard !reduceMotion else { return }
        breathScale = 1
        withAnimation(
            .easeInOut(duration: FocusModeMotion.breathDuration / 2)
            .delay(FocusModeMotion.breathDelay)
        ) {
            breathScale = FocusModeMotion.breathScalePeak
        }
        withAnimation(
            .easeInOut(duration: FocusModeMotion.breathDuration / 2)
            .delay(FocusModeMotion.breathDelay + FocusModeMotion.breathDuration / 2)
        ) {
            breathScale = 1
        }
    }
}

// MARK: - Pieces

private struct FocusIntentPill: View {
    let isHabit: Bool
    let title: String
    let colors: PurrodoroColors
    @EnvironmentObject private var loc: Localization

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: isHabit ? "leaf.fill" : "checkmark.circle.fill")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Brand.coral)
            Text(loc.t(isHabit ? "focus.type.habit" : "focus.type.task"))
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textPrimary)
            Text("·")
                .foregroundStyle(colors.textSecondary)
            Text(title)
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(colors.textPrimary)
                .lineLimit(1)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(colors.card, in: Capsule())
        .overlay(Capsule().stroke(colors.border, lineWidth: 1))
    }
}

private struct FocusOverlayDots: View {
    @EnvironmentObject private var loc: Localization
    let dots: CycleDotsState
    let colors: PurrodoroColors

    var body: some View {
        HStack(spacing: 10) {
            ForEach(0..<dots.totalFocusSessions, id: \.self) { i in
                let isActive = dots.isCurrentlyFocus && i == dots.completedFocusSessions
                let isPast = i < dots.completedFocusSessions
                Circle()
                    .fill(
                        isActive ? Brand.coral
                            : isPast ? Brand.coral.opacity(0.55)
                            : colors.border
                    )
                    .frame(width: isActive ? 11 : 8, height: isActive ? 11 : 8)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(loc.t("a11y.cycleProgress"))
        .accessibilityValue(loc.t("a11y.sessionsOf", dots.completedFocusSessions, dots.totalFocusSessions))
    }
}

private struct FocusCoralPill: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(.body, design: .rounded).weight(.heavy))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(Brand.coral, in: Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct FocusQuietText: View {
    let title: String
    var systemImage: String? = nil
    let action: () -> Void

    @Environment(\.colorScheme) private var scheme
    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.system(size: 14, weight: .semibold))
                }
                Text(title)
                    .font(.system(.body, design: .rounded).weight(.semibold))
            }
            .foregroundStyle(colors.textSecondary)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
    }
}

private struct FocusSubtleSecondary: View {
    let title: String
    let systemImage: String
    let action: () -> Void

    @Environment(\.colorScheme) private var scheme
    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(colors.textSecondary.opacity(0.65))
                Text(title)
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundStyle(colors.textSecondary.opacity(0.75))
            }
            .padding(.horizontal, 20)
            .frame(height: 44)
            .background(colors.card.opacity(0.85), in: Capsule())
            .overlay(Capsule().stroke(colors.border.opacity(0.7), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

private struct FocusCoralPauseCircle: View {
    let action: () -> Void
    @EnvironmentObject private var loc: Localization

    var body: some View {
        Button(action: action) {
            Image(systemName: "pause.fill")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 54, height: 54)
                .background(Brand.coral, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(loc.t("focus.pauseBreak.cd"))
    }
}
