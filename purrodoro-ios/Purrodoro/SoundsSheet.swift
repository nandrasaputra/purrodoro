//
//  SoundsSheet.swift
//  Purrodoro
//
//  The ambient-sounds picker, surfaced as a bottom sheet from the Focus screen
//  (mirroring the Android "sounds bottom sheet" design). Tapping a row plays it;
//  tapping the playing row stops it. Playback continues after the sheet closes.
//

import SwiftUI

struct SoundsSheet: View {
    @EnvironmentObject private var sounds: SoundsStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        Text(loc.t("sounds.subtitle"))
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundStyle(colors.textSecondary)
                            .padding(.top, 4)

                        ForEach(SoundsStore.options) { option in
                            row(option)
                        }

                        silenceRow
                    }
                    .padding(20)
                }
            }
            .navigationTitle(loc.t("sounds.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc.t("settings.done")) { dismiss() }
                        .font(.system(.body, design: .rounded).weight(.semibold))
                }
            }
        }
        .environment(\.pColors, colors)
        .presentationDetents([.medium, .large])
    }

    private func row(_ option: SoundOption) -> some View {
        let isPlaying = sounds.playingKind == option.kind
        return Button {
            sounds.toggle(option.kind)
        } label: {
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(option.color.opacity(0.35))
                        .frame(width: 48, height: 48)
                    Image(systemName: option.symbol)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(isPlaying ? Brand.deepCoral : colors.textPrimary)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(loc.t(option.nameKey))
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                    Text(loc.t(option.descKey))
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
                Spacer()
                statusPill(isPlaying ? loc.t("sounds.playing") : loc.t("sounds.tapToPlay"), active: isPlaying)
            }
            .padding(14)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(isPlaying ? Brand.coral : colors.border, lineWidth: isPlaying ? 2 : 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var silenceRow: some View {
        Button {
            sounds.stop()
        } label: {
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(colors.border.opacity(0.5)).frame(width: 48, height: 48)
                    Image(systemName: "speaker.slash.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(colors.textSecondary)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(loc.t("sounds.none"))
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                    Text(loc.t("sounds.noneDesc"))
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
                Spacer()
                if !sounds.isPlaying {
                    statusPill(loc.t("sounds.selected"), active: true)
                }
            }
            .padding(14)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(!sounds.isPlaying ? Brand.coral : colors.border, lineWidth: !sounds.isPlaying ? 2 : 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func statusPill(_ text: String, active: Bool) -> some View {
        Text(text)
            .font(.system(.caption2, design: .rounded).weight(.bold))
            .foregroundStyle(active ? Brand.cocoa : colors.textSecondary)
            .padding(.vertical, 5)
            .padding(.horizontal, 10)
            .background(active ? Brand.peach : colors.page, in: Capsule())
            .overlay(Capsule().stroke(active ? Color.clear : colors.border, lineWidth: 1))
    }
}
