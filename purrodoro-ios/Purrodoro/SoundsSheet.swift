//
//  SoundsSheet.swift
//  Purrodoro
//
//  Dual-deck Soundscape picker (Ambience + Lo-fi). Tapping a row plays it;
//  tapping the playing row stops that deck. Locked tracks open the premium
//  upsell. Cloud tracks download on first tap.
//

import SwiftUI

struct SoundsSheet: View {
    @EnvironmentObject private var sounds: SoundsStore
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var auth: AuthStore
    @EnvironmentObject private var storeKit: StoreKitManager
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    @State private var premiumUpsellTrigger: PremiumUpsellTrigger?
    @State private var pendingUpsellAfterLogin: PremiumUpsellTrigger?
    @State private var showLoginSheet = false
    @State private var showPremium = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    private var isGuest: Bool {
        if case .signedIn = auth.state { return false }
        return true
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        Text(loc.t("sounds.subtitle"))
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundStyle(colors.textSecondary)
                            .padding(.top, 4)

                        section(
                            title: loc.t("soundscape.section.ambience"),
                            tracks: sounds.ambienceTracks,
                            playingId: sounds.playingAmbienceId,
                            silenceLabel: loc.t("sounds.none"),
                            silenceDesc: loc.t("sounds.noneDesc"),
                            isSilent: sounds.playingAmbienceId == nil,
                            onSilence: { sounds.stopAmbience() }
                        )

                        section(
                            title: loc.t("soundscape.section.lofi"),
                            tracks: sounds.musicTracks,
                            playingId: sounds.playingMusicId,
                            silenceLabel: loc.t("soundscape.music.none"),
                            silenceDesc: loc.t("soundscape.music.noneDesc"),
                            isSilent: sounds.playingMusicId == nil,
                            onSilence: { sounds.stopMusic() }
                        )

                        globalSilenceRow
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
        .onChange(of: sounds.pendingUpsell) { _, trigger in
            if let trigger {
                premiumUpsellTrigger = trigger
                sounds.pendingUpsell = nil
            }
        }
    }

    // MARK: - Sections

    private func section(
        title: String,
        tracks: [SoundscapeTrack],
        playingId: Int?,
        silenceLabel: String,
        silenceDesc: String,
        isSilent: Bool,
        onSilence: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title.uppercased())
                .font(.system(.caption, design: .rounded).weight(.bold))
                .tracking(1)
                .foregroundStyle(colors.textFaint)

            ForEach(tracks) { track in
                trackRow(track, isPlaying: playingId == track.trackId)
            }

            silenceRow(
                label: silenceLabel,
                desc: silenceDesc,
                isSelected: isSilent,
                action: onSilence
            )
        }
    }

    private func trackRow(_ track: SoundscapeTrack, isPlaying: Bool) -> some View {
        Button {
            if let upsell = sounds.toggle(track) {
                premiumUpsellTrigger = upsell
            }
        } label: {
            HStack(spacing: 14) {
                SoundscapeThumbnailView(track: track, isPlaying: isPlaying, size: 48)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(track.name)
                            .font(.system(.body, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textPrimary)
                            .lineLimit(1)
                        if track.locked && !prefs.isPremium {
                            Image(systemName: "lock.fill")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(Brand.lavender)
                        }
                    }
                    Text(subtitle(for: track))
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                        .lineLimit(1)
                }

                Spacer()

                trailingStatus(for: track, isPlaying: isPlaying)
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

    private func subtitle(for track: SoundscapeTrack) -> String {
        if case .downloading(let progress) = track.remote?.downloadState {
            let pct = Int((progress * 100).rounded())
            return loc.t("soundscape.downloading_pct", pct)
        }
        if track.remote?.downloadState == .notDownloaded {
            let mb = Double(track.remote?.fileSizeBytes ?? 0) / 1_000_000
            return loc.t("soundscape.download_size", String(format: "%.1f", mb))
        }
        if track.subtitle.isEmpty {
            return loc.t("sounds.tapToPlay")
        }
        return track.subtitle
    }

    @ViewBuilder
    private func trailingStatus(for track: SoundscapeTrack, isPlaying: Bool) -> some View {
        if track.locked && !prefs.isPremium {
            statusPill(loc.t("sounds.premium"), active: false)
        } else if case .downloading(let progress) = track.remote?.downloadState {
            ProgressView(value: Double(progress))
                .frame(width: 36)
        } else if track.remote?.downloadState == .notDownloaded {
            Image(systemName: "arrow.down.circle")
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(colors.textSecondary)
        } else if isPlaying {
            statusPill(loc.t("sounds.playing"), active: true)
        } else {
            statusPill(loc.t("sounds.tapToPlay"), active: false)
        }
    }

    private func silenceRow(
        label: String,
        desc: String,
        isSelected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    Circle().fill(colors.border.opacity(0.5)).frame(width: 48, height: 48)
                    Image(systemName: "speaker.slash.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(colors.textSecondary)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                    Text(desc)
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
                Spacer()
                if isSelected {
                    statusPill(loc.t("sounds.selected"), active: true)
                }
            }
            .padding(14)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(isSelected ? Brand.coral : colors.border, lineWidth: isSelected ? 2 : 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var globalSilenceRow: some View {
        Button {
            sounds.stop()
        } label: {
            HStack {
                Image(systemName: "speaker.slash")
                    .font(.system(size: 16, weight: .semibold))
                Text(loc.t("soundscape.mute_all"))
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                Spacer()
            }
            .foregroundStyle(colors.textSecondary)
            .padding(14)
            .background(colors.card.opacity(0.6), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(colors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .padding(.top, 4)
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
