//
//  TutorialModels.swift
//  Purrodoro
//
//  Post-launch spotlight tour models + controller (Android `Tutorial.kt`).
//

import Foundation
import CoreGraphics
import Combine

/// Real UI elements the spotlight tour can point at.
enum TutorialTarget: String, CaseIterable, Hashable {
    case startFocus
    case soundscapePlay
    case tasksTab
    case habitsTab
    case journeyTab
}

enum TooltipPlacement: Equatable {
    case below
    case above
}

struct TutorialStep: Equatable, Identifiable {
    var id: TutorialTarget { target }
    let target: TutorialTarget
    let titleKey: String
    let bodyKey: String
    let placement: TooltipPlacement
    let spotlightCornerRadius: CGFloat
    let spotlightPadding: CGFloat
}

let defaultTutorialSteps: [TutorialStep] = [
    TutorialStep(
        target: .startFocus,
        titleKey: "tutorial.startFocus.title",
        bodyKey: "tutorial.startFocus.body",
        placement: .below,
        spotlightCornerRadius: 34,
        spotlightPadding: 6
    ),
    TutorialStep(
        target: .soundscapePlay,
        titleKey: "tutorial.soundscape.title",
        bodyKey: "tutorial.soundscape.body",
        placement: .above,
        spotlightCornerRadius: 22,
        spotlightPadding: 4
    ),
    TutorialStep(
        target: .tasksTab,
        titleKey: "tutorial.tasks.title",
        bodyKey: "tutorial.tasks.body",
        placement: .above,
        spotlightCornerRadius: 26,
        spotlightPadding: 4
    ),
    TutorialStep(
        target: .habitsTab,
        titleKey: "tutorial.habits.title",
        bodyKey: "tutorial.habits.body",
        placement: .above,
        spotlightCornerRadius: 26,
        spotlightPadding: 4
    ),
    TutorialStep(
        target: .journeyTab,
        titleKey: "tutorial.journey.title",
        bodyKey: "tutorial.journey.body",
        placement: .above,
        spotlightCornerRadius: 26,
        spotlightPadding: 4
    ),
]

/// Holds tour state and a live map of measured target bounds in the tutorial
/// root coordinate space.
@MainActor
final class TutorialController: ObservableObject {
    let steps: [TutorialStep]

    @Published private(set) var visible = false
    @Published private(set) var stepIndex = 0
    @Published private(set) var bounds: [TutorialTarget: CGRect] = [:]

    init(steps: [TutorialStep] = defaultTutorialSteps) {
        self.steps = steps
    }

    var currentStep: TutorialStep? {
        steps.indices.contains(stepIndex) ? steps[stepIndex] : nil
    }

    var isLastStep: Bool { stepIndex >= steps.count - 1 }
    var canGoBack: Bool { stepIndex > 0 }

    func boundsFor(_ target: TutorialTarget) -> CGRect? { bounds[target] }

    func reportBounds(_ target: TutorialTarget, _ rect: CGRect) {
        // Ignore zero-size frames (offscreen / not laid out yet).
        guard rect.width > 1, rect.height > 1 else { return }
        if bounds[target] != rect {
            bounds[target] = rect
        }
    }

    func start() {
        guard !steps.isEmpty else { return }
        stepIndex = 0
        visible = true
    }

    func next(onFinish: () -> Void) {
        if stepIndex < steps.count - 1 {
            stepIndex += 1
        } else {
            dismiss(onFinish: onFinish)
        }
    }

    func previous() {
        if stepIndex > 0 { stepIndex -= 1 }
    }

    func skip(onFinish: () -> Void) {
        dismiss(onFinish: onFinish)
    }

    private func dismiss(onFinish: () -> Void) {
        guard visible else { return }
        visible = false
        onFinish()
    }
}
