//
//  SpotlightTutorialOverlay.swift
//  Purrodoro
//
//  Dimmed overlay with a rounded spotlight cutout + coachmark card.
//  Ported from Android `SpotlightTutorialOverlay.kt`.
//

import SwiftUI

struct SpotlightTutorialOverlay: View {
    let targetBounds: CGRect
    let title: String
    let bodyText: String
    let stepIndex: Int
    let totalSteps: Int
    let skipLabel: String
    let previousLabel: String?
    let nextLabel: String
    let placement: TooltipPlacement
    let spotlightPadding: CGFloat
    let spotlightCornerRadius: CGFloat
    let onSkip: () -> Void
    let onPrevious: () -> Void
    let onNext: () -> Void

    @Environment(\.colorScheme) private var scheme
    @State private var breath: CGFloat = 0

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    private var highlight: CGRect {
        targetBounds.insetBy(dx: -spotlightPadding, dy: -spotlightPadding)
    }

    var body: some View {
        GeometryReader { geo in
            let screen = geo.size
            let cardWidth = min(304, screen.width - 40)
            let placeBelow = placement == .below
            let highlightCenterX = highlight.midX
            let x = min(max(20, highlightCenterX - cardWidth / 2), screen.width - cardWidth - 20)
            // Approximate card height for vertical clamping before measure.
            let estimatedCardHeight: CGFloat = 210
            let gap: CGFloat = 18
            let arrowH: CGFloat = 14
            let groupH = estimatedCardHeight + arrowH
            let y: CGFloat = {
                if placeBelow {
                    return min(highlight.maxY + gap, screen.height - groupH - 20)
                }
                return max(20, highlight.minY - groupH - gap)
            }()
            let arrowCenter = min(max(34, highlightCenterX - x), cardWidth - 34)

            ZStack(alignment: .topLeading) {
                Canvas { context, size in
                    // Dim scrim
                    context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black.opacity(0.56)))

                    let corner = spotlightCornerRadius
                    let glowExpansion = 7 * breath
                    let glowAlpha = 0.26 - (0.12 * breath)
                    let glowRect = highlight.insetBy(dx: -glowExpansion, dy: -glowExpansion)

                    // Soft coral glow
                    var glow = Path(roundedRect: glowRect, cornerRadius: corner + glowExpansion)
                    context.stroke(
                        glow,
                        with: .color(Brand.coral.opacity(glowAlpha)),
                        style: StrokeStyle(lineWidth: 18 + 8 * breath, lineCap: .round)
                    )

                    // Clear the spotlight hole
                    var hole = Path(roundedRect: highlight, cornerRadius: corner)
                    context.blendMode = .clear
                    context.fill(hole, with: .color(.white))
                    context.blendMode = .normal

                    // Thin white rim
                    context.stroke(
                        hole,
                        with: .color(.white.opacity(0.26)),
                        lineWidth: 1.2
                    )
                }
                .compositingGroup()
                .allowsHitTesting(true)

                VStack(alignment: .leading, spacing: 0) {
                    if placeBelow {
                        tutorialArrow(pointingUp: true)
                            .offset(x: arrowCenter - 12)
                    }
                    tutorialCard
                    if !placeBelow {
                        tutorialArrow(pointingUp: false)
                            .offset(x: arrowCenter - 12)
                    }
                }
                .frame(width: cardWidth, alignment: .leading)
                .offset(x: x, y: y)
            }
            .frame(width: screen.width, height: screen.height)
        }
        .ignoresSafeArea()
        .onAppear {
            withAnimation(.easeInOut(duration: 1.6).repeatForever(autoreverses: true)) {
                breath = 1
            }
        }
        // Block taps through to the app while the tour is up.
        .contentShape(Rectangle())
        .onTapGesture {}
    }

    private var tutorialCard: some View {
        ZStack(alignment: .topLeading) {
            VStack(spacing: 10) {
                Text(title)
                    .font(.system(.headline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .multilineTextAlignment(.center)

                Text(bodyText)
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .multilineTextAlignment(.center)

                HStack(spacing: 8) {
                    ForEach(0..<totalSteps, id: \.self) { index in
                        Circle()
                            .fill(index == stepIndex ? Brand.coral : colors.textFaint.opacity(0.26))
                            .frame(width: index == stepIndex ? 8 : 7, height: index == stepIndex ? 8 : 7)
                    }
                }
                .padding(.top, 2)

                HStack {
                    Button(skipLabel, action: onSkip)
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)

                    Spacer()

                    if let previousLabel {
                        Button(previousLabel, action: onPrevious)
                            .font(.system(.subheadline, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textPrimary)
                    }

                    Button(action: onNext) {
                        Text(nextLabel)
                            .font(.system(.subheadline, design: .rounded).weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 11)
                            .background(Brand.coral, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.top, 4)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .padding(.top, 10)
            .frame(maxWidth: .infinity)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(colors.border, lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.12), radius: 18, y: 8)
            .padding(.top, 18)

            Image("TutorialKuroPeek")
                .resizable()
                .scaledToFit()
                .frame(width: 76, height: 60)
                .offset(x: -12, y: -18)
                .accessibilityHidden(true)
        }
    }

    private func tutorialArrow(pointingUp: Bool) -> some View {
        TutorialArrowShape(pointingUp: pointingUp)
            .fill(colors.card)
            .frame(width: 24, height: 14)
    }
}

private struct TutorialArrowShape: Shape {
    let pointingUp: Bool

    func path(in rect: CGRect) -> Path {
        var path = Path()
        if pointingUp {
            path.move(to: CGPoint(x: rect.midX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        } else {
            path.move(to: CGPoint(x: rect.minX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
        }
        path.closeSubpath()
        return path
    }
}

// MARK: - Anchor modifier

private struct TutorialAnchorModifier: ViewModifier {
    @EnvironmentObject private var tutorial: TutorialController
    let target: TutorialTarget

    func body(content: Content) -> some View {
        content.background(
            GeometryReader { geo in
                Color.clear
                    .onAppear { report(geo) }
                    .onChange(of: geo.frame(in: .named(TutorialCoordinateSpace.name))) { _, _ in
                        report(geo)
                    }
                    .onChange(of: tutorial.stepIndex) { _, _ in
                        report(geo)
                    }
                    .onChange(of: tutorial.visible) { _, visible in
                        if visible { report(geo) }
                    }
            }
        )
    }

    private func report(_ geo: GeometryProxy) {
        let frame = geo.frame(in: .named(TutorialCoordinateSpace.name))
        tutorial.reportBounds(target, frame)
    }
}

enum TutorialCoordinateSpace {
    static let name = "purrodoro.tutorial"
}

extension View {
    /// Registers this view's bounds with the ambient `TutorialController`.
    func tutorialAnchor(_ target: TutorialTarget) -> some View {
        modifier(TutorialAnchorModifier(target: target))
    }
}
