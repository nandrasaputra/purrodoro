//
//  CelebrationView.swift
//  Purrodoro
//
//  A warm full-screen celebration shown when a whole Pomodoro cycle finishes —
//  richer than a system alert. Mascot, a gentle confetti shower, and one of a
//  few rotating congratulatory messages (ported from the Android
//  `CycleCompletionCopy` variants).
//

import SwiftUI

struct CelebrationOverlay: View {
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme

    let onDismiss: () -> Void

    @State private var variant = 0
    @State private var appeared = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private static let variantCount = 5

    var body: some View {
        ZStack {
            colors.page.opacity(0.96).ignoresSafeArea()

            ConfettiShower()
                .allowsHitTesting(false)
                .ignoresSafeArea()

            VStack(spacing: 22) {
                Image("CatCelebrate")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 200, height: 200)
                    .scaleEffect(appeared ? 1 : 0.6)
                    .rotationEffect(.degrees(appeared ? 0 : -8))

                VStack(spacing: 10) {
                    Text(loc.t("cycle.v\(variant + 1).title"))
                        .font(.system(.largeTitle, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                        .multilineTextAlignment(.center)
                    Text(loc.t("cycle.v\(variant + 1).body"))
                        .font(.system(.body, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
                .opacity(appeared ? 1 : 0)
                .offset(y: appeared ? 0 : 12)

                PrimaryButton(title: loc.t("cycle.nice"), systemImage: "heart.fill", tint: Brand.coral) {
                    onDismiss()
                }
                .padding(.horizontal, 48)
                .padding(.top, 8)
                .opacity(appeared ? 1 : 0)
            }
            .padding(.horizontal, 24)
        }
        .onAppear {
            variant = Int.random(in: 0..<Self.variantCount)
            withAnimation(.spring(response: 0.5, dampingFraction: 0.62)) {
                appeared = true
            }
        }
        .transition(.opacity)
    }
}

// MARK: - Confetti

private struct ConfettiShower: View {
    private let pieces: [ConfettiPiece] = (0..<70).map { _ in ConfettiPiece() }

    var body: some View {
        GeometryReader { geo in
            TimelineView(.animation) { timeline in
                Canvas { context, size in
                    let t = timeline.date.timeIntervalSinceReferenceDate
                    for piece in pieces {
                        let progress = ((t * piece.speed) + piece.phase).truncatingRemainder(dividingBy: 1)
                        let x = piece.startX * size.width + sin((progress + piece.phase) * .pi * 2 * piece.sway) * 24
                        let y = progress * (size.height + 40) - 20
                        let rect = CGRect(x: x, y: y, width: piece.size, height: piece.size * 1.6)
                        var transform = context
                        transform.translateBy(x: rect.midX, y: rect.midY)
                        transform.rotate(by: .radians(progress * .pi * 2 * piece.spin))
                        transform.fill(
                            Path(roundedRect: CGRect(x: -piece.size / 2, y: -piece.size * 0.8,
                                                     width: piece.size, height: piece.size * 1.6),
                                 cornerSize: CGSize(width: 1.5, height: 1.5)),
                            with: .color(piece.color.opacity(0.9))
                        )
                    }
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
    }
}

private struct ConfettiPiece: Identifiable {
    let id = UUID()
    let startX = CGFloat.random(in: 0...1)
    let size = CGFloat.random(in: 6...11)
    let speed = Double.random(in: 0.05...0.13)
    let phase = Double.random(in: 0...1)
    let sway = Double.random(in: 0.6...1.6)
    let spin = Double.random(in: 0.5...1.8)
    let color: Color = [
        Brand.coral, Brand.peach, Brand.lavender, Brand.mint, Brand.sky, Brand.blush,
    ].randomElement()!
}
