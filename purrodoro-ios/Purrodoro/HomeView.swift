//
//  HomeView.swift
//  Purrodoro
//
//  The focus home screen: greeting, home-scene hero, countdown dial with
//  cycle progress dots, and the start / pause / skip / stop controls.
//  Focus Mode chip opens the immersive zen overlay (RootView).
//

import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @EnvironmentObject private var habits: HabitStore
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var sounds: SoundsStore
    @EnvironmentObject private var focusMode: FocusModeController
    @EnvironmentObject private var auth: AuthStore
    @EnvironmentObject private var storeKit: StoreKitManager
    @EnvironmentObject private var deepLinks: DeepLinkRouter
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var showProfile = false
    @State private var showStartFocus = false
    @State private var showScenePicker = false
    @State private var startPrefill: StartFocusPrefill?
    @State private var premiumUpsellTrigger: PremiumUpsellTrigger?
    @State private var pendingUpsellAfterLogin: PremiumUpsellTrigger?
    @State private var showLoginSheet = false
    @State private var showPremium = false
    @State private var showStopConfirm = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var isDark: Bool { scheme == .dark }

    private var isGuest: Bool {
        if case .signedIn = auth.state { return false }
        return true
    }

    private var heroImageName: String {
        resolveHomeHeroImageName(scene: prefs.displayHomeScene, isDarkTheme: isDark)
    }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 18) {
                    header

                    heroSection

                    if engine.isStarted {
                        activeSessionChips
                    }

                    controls
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 140)
            }
            .readableContentWidth()

            if engine.pendingCycleComplete {
                CelebrationOverlay { engine.acknowledgeCycleComplete() }
                    .zIndex(2)
            }
        }
        .environment(\.pColors, colors)
        .sheet(isPresented: $showProfile) {
            ProfileView()
        }
        .sheet(isPresented: $showStartFocus) {
            StartFocusSheet(prefill: startPrefill)
        }
        .sheet(isPresented: $showScenePicker) {
            HomeScenePickerSheet(
                selected: prefs.homeScene,
                isPremium: prefs.isPremium,
                onSelect: { prefs.homeScene = $0 },
                onPremiumSceneAttempt: { name in
                    premiumUpsellTrigger = .lockedScene(name)
                }
            )
            .environmentObject(loc)
        }
        .sheet(item: $premiumUpsellTrigger) { trigger in
            PremiumUpsellSheet(
                trigger: trigger,
                isGuest: isGuest,
                onDismiss: { premiumUpsellTrigger = nil },
                onSignIn: {
                    pendingUpsellAfterLogin = trigger
                    premiumUpsellTrigger = nil
                    showLoginSheet = true
                },
                onExplorePremium: {
                    premiumUpsellTrigger = nil
                    showPremium = true
                }
            )
            .environmentObject(storeKit)
            .environmentObject(prefs)
            .environmentObject(loc)
        }
        .sheet(isPresented: $showLoginSheet) {
            LoginSheet(
                onSignedIn: { _ in
                    showLoginSheet = false
                    if let pending = pendingUpsellAfterLogin {
                        pendingUpsellAfterLogin = nil
                        premiumUpsellTrigger = pending
                    }
                },
                onContinueAsGuest: { showLoginSheet = false }
            )
        }
        .sheet(isPresented: $showPremium) {
            PremiumView()
                .environmentObject(prefs)
                .environmentObject(storeKit)
                .environmentObject(loc)
        }
        .alert(loc.t("home.stop.title"), isPresented: $showStopConfirm) {
            Button(loc.t("home.stop.confirm"), role: .destructive) { engine.stop() }
            Button(loc.t("home.stop.dismiss"), role: .cancel) {}
        } message: {
            Text(loc.t("home.stop.body"))
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { engine.reconcile() }
        }
        .onAppear { consumeDeepLinkStartFocusIfNeeded() }
        .onChange(of: deepLinks.pendingOpenStartFocus) { _, pending in
            if pending { consumeDeepLinkStartFocusIfNeeded() }
        }
        .animation(.easeInOut(duration: 0.25), value: engine.pendingCycleComplete)
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

    private func consumeDeepLinkStartFocusIfNeeded() {
        guard deepLinks.pendingOpenStartFocus else { return }
        deepLinks.consumeStartFocus()
        openStartFocus()
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

    // MARK: Hero

    private var heroSection: some View {
        ZStack(alignment: .bottom) {
            HeroSceneImage(
                imageName: heroImageName,
                onChangeScene: { showScenePicker = true },
                colors: colors
            )

            if !engine.isStarted {
                PrimaryButton(title: loc.t("home.startFocus"), systemImage: "play.fill", tint: Brand.coral) {
                    openStartFocus()
                }
                .padding(.horizontal, 28)
                .offset(y: 22)
                .tutorialAnchor(.startFocus)
            } else {
                activeTimerCard
                    .padding(.horizontal, 12)
                    .offset(y: 18)
            }
        }
        .padding(.bottom, engine.isStarted ? 10 : 22)
    }

    private var activeTimerCard: some View {
        VStack(spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(formatSeconds(engine.displayRemaining))
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(colors.textPrimary)
                Spacer()
                ModeChip(mode: engine.currentMode, state: engine.session.timerState)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(timerAccessibilityLabel)
            .accessibilityValue(timerAccessibilityValue)

            CycleDotsRow(dots: engine.dots, colors: colors)

            HStack(spacing: 12) {
                Button {
                    if engine.isRunning {
                        engine.pause()
                    } else {
                        engine.resume()
                    }
                } label: {
                    Image(systemName: engine.isRunning ? "pause.fill" : "play.fill")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                        .background(Brand.coral, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(loc.t(engine.isRunning ? "home.pause" : "home.resume"))

                Spacer()

                Text(loc.t(engine.currentMode.labelKey))
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textSecondary)
            }
        }
        .padding(16)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(colors.border, lineWidth: 1)
        )
        .shadow(color: .black.opacity(scheme == .dark ? 0.35 : 0.08), radius: 12, y: 4)
    }

    private var activeSessionChips: some View {
        HStack(spacing: 8) {
            sessionChip(
                systemImage: "leaf.fill",
                title: loc.t("home.focusMode.chip"),
                tint: Brand.coral
            ) {
                focusMode.enter()
            }
            .accessibilityLabel(loc.t("home.focusMode.open"))

            if engine.currentMode != .focus {
                sessionChip(
                    systemImage: "forward.fill",
                    title: loc.t("home.skip"),
                    tint: colors.textSecondary
                ) {
                    engine.skip()
                }
            }

            sessionChip(
                systemImage: "stop.circle",
                title: loc.t("home.stop"),
                tint: colors.textSecondary
            ) {
                if engine.currentMode == .focus {
                    showStopConfirm = true
                } else {
                    engine.stop()
                }
            }
        }
    }

    private func sessionChip(
        systemImage: String,
        title: String,
        tint: Color,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(tint)
                Text(title)
                    .font(.system(.caption, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(colors.card, in: Capsule())
            .overlay(Capsule().stroke(colors.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
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
            } else if let note = prefs.activeSimpleNote {
                activeIntentCard(
                    symbol: "pencil.line",
                    title: note,
                    subtitle: loc.t("start.note"),
                    onDetach: { prefs.clearSimpleNote() }
                )
            }

            if engine.session.timerState == .idle, let interrupted = engine.interruptedFocus {
                resumeCard(interrupted)
            }

            todayStat
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

    private var timerAccessibilityLabel: String {
        loc.t("a11y.timer", loc.t(engine.currentMode.labelKey))
    }

    private var timerAccessibilityValue: String {
        let remaining = loc.t("a11y.remaining", formatSeconds(engine.displayRemaining))
        if engine.session.timerState == .paused {
            return "\(remaining), \(loc.t("a11y.paused"))"
        }
        return remaining
    }
}

// MARK: - Hero scene image

private struct HeroSceneImage: View {
    let imageName: String
    let onChangeScene: () -> Void
    let colors: PurrodoroColors
    @EnvironmentObject private var loc: Localization

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Image(imageName)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .aspectRatio(1.48, contentMode: .fit)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

            Button(action: onChangeScene) {
                Image(systemName: "square.3.layers.3d")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .frame(width: 36, height: 36)
                    .background(colors.card.opacity(0.92), in: Circle())
                    .overlay(Circle().stroke(colors.border, lineWidth: 1))
            }
            .buttonStyle(.plain)
            .padding(10)
            .accessibilityLabel(loc.t("home.scene.change"))
        }
    }
}

// MARK: - Mode chip

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
        .environmentObject(FocusModeController())
        .environmentObject(TutorialController())
        .environmentObject(DeepLinkRouter())
        .environmentObject(AuthStore())
        .environmentObject(StoreKitManager(prefs: PreferencesStore()))
        .environmentObject(Localization())
}
