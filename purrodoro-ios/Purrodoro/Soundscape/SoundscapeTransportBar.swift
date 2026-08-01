//
//  SoundscapeTransportBar.swift
//  Purrodoro
//
//  Compact transport row fused into the Focus-tab bottom dock.
//  Mirrors Android `SoundscapeTransportBar` in SoundsScreen.kt.
//

import SwiftUI

struct SoundscapeTransportBar: View {
    let isPlaying: Bool
    let displayName: String
    let showFirstRunHint: Bool
    let onPlayPause: () -> Void
    let onExpand: () -> Void

    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    private var rowFill: Color {
        isPlaying ? Brand.peach.opacity(0.32) : colors.card.opacity(0.45)
    }

    private var nameColor: Color {
        if isPlaying {
            return scheme == .dark ? colors.textPrimary : Brand.cocoa
        }
        return colors.textSecondary
    }

    var body: some View {
        HStack(spacing: 10) {
            Button(action: onExpand) {
                HStack(spacing: 10) {
                    SoundscapeEqualizerCapsule(isPlaying: isPlaying)
                        .frame(width: 36, height: 36)

                    Text(displayName)
                        .font(.system(.subheadline, design: .rounded).weight(isPlaying ? .bold : .semibold))
                        .foregroundStyle(nameColor)
                        .lineLimit(1)
                        .opacity(showFirstRunHint && !isPlaying ? 0.9 : 1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(displayName)
            .accessibilityHint(loc.t("soundscape.mini_open"))

            Button(action: onPlayPause) {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(Brand.coral, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(
                loc.t(isPlaying ? "soundscape.transport.pause" : "soundscape.transport.play")
            )
            .tutorialAnchor(.soundscapePlay)

            Button(action: onExpand) {
                Image(systemName: "chevron.up")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(isPlaying ? Brand.coral.opacity(0.85) : colors.textFaint)
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(loc.t("soundscape.mini_open"))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity)
        .frame(height: 56)
        .background(rowFill)
    }
}

// MARK: - EQ capsule

private struct SoundscapeEqualizerCapsule: View {
    let isPlaying: Bool

    private static let idle: [CGFloat] = [0.42, 0.62, 0.38, 0.52]
    private static let floor: [CGFloat] = [0.18, 0.22, 0.14, 0.20]
    private static let peak: [CGFloat] = [0.58, 1.0, 0.48, 0.76]
    private static let phaseMs: [Double] = [0, 90, 180, 130]
    private static let cycleMs: Double = 520

    var body: some View {
        TimelineView(.animation(minimumInterval: isPlaying ? 1.0 / 30.0 : 1.0, paused: !isPlaying)) { timeline in
            let fractions: [CGFloat] = isPlaying
                ? Self.animatedFractions(at: timeline.date)
                : Self.idle
            HStack(alignment: .bottom, spacing: 3) {
                ForEach(0..<4, id: \.self) { i in
                    RoundedRectangle(cornerRadius: 1.5, style: .continuous)
                        .fill(Brand.coral.opacity(isPlaying ? 0.82 : 0.5))
                        .frame(width: 3, height: 14 * fractions[i])
                }
            }
            .frame(height: 18)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Brand.peach.opacity(isPlaying ? 0.52 : 0.4))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(Brand.coral.opacity(isPlaying ? 0.22 : 0.16), lineWidth: 1)
            )
        }
    }

    private static func animatedFractions(at date: Date) -> [CGFloat] {
        let t = date.timeIntervalSinceReferenceDate * 1000
        return (0..<4).map { i in
            let phase = phaseMs[i]
            let cycle = cycleMs
            let local = (t + phase).truncatingRemainder(dividingBy: cycle * 2)
            let tri: Double = local < cycle ? (local / cycle) : (2 - local / cycle)
            return floor[i] + (peak[i] - floor[i]) * CGFloat(tri)
        }
    }
}
