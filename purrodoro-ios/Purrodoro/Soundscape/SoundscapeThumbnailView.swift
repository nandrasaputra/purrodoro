//
//  SoundscapeThumbnailView.swift
//  Purrodoro
//
//  Track art for the Sounds sheet. Remote R2 WebP via SoundscapeThumbnailLoader;
//  bundled / missing art falls back to the SF Symbol circle used in Soundscape #1.
//

import SwiftUI
import UIKit

struct SoundscapeThumbnailView: View {
    let track: SoundscapeTrack
    var isPlaying: Bool = false
    var size: CGFloat = 48

    @State private var remoteImage: UIImage?

    var body: some View {
        Group {
            if track.kind == .music {
                musicCover
            } else {
                ambienceThumb
            }
        }
        .frame(width: size, height: size)
        .task(id: thumbnailIdentity) {
            await loadRemoteIfNeeded()
        }
    }

    private var thumbnailIdentity: String {
        let path = track.remote?.thumbnailPath ?? ""
        let version = track.remote?.cacheVersion ?? 0
        return "\(track.listId)-\(path)-\(version)"
    }

    // MARK: - Ambience (rounded square / circle crop)

    private var ambienceThumb: some View {
        ZStack {
            if let remoteImage {
                Image(uiImage: remoteImage)
                    .resizable()
                    .scaledToFill()
                    .clipShape(RoundedRectangle(cornerRadius: size * 0.28, style: .continuous))
            } else {
                symbolFallback
            }
        }
    }

    // MARK: - Music (vinyl disc with cover label)

    private var musicCover: some View {
        ZStack {
            // Vinyl disc
            Circle()
                .fill(
                    RadialGradient(
                        colors: [
                            Color(hex: 0x2C2C2C),
                            Color(hex: 0x181818),
                            Color(hex: 0x0C0C0C),
                        ],
                        center: .center,
                        startRadius: 2,
                        endRadius: size / 2
                    )
                )
            // Center label (cover art or accent)
            Group {
                if let remoteImage {
                    Image(uiImage: remoteImage)
                        .resizable()
                        .scaledToFill()
                } else {
                    Circle()
                        .fill(track.accentColor.opacity(0.85))
                        .overlay(
                            Image(systemName: track.systemImage)
                                .font(.system(size: size * 0.18, weight: .semibold))
                                .foregroundStyle(isPlaying ? Brand.deepCoral : .white.opacity(0.9))
                        )
                }
            }
            .frame(width: size * 0.52, height: size * 0.52)
            .clipShape(Circle())

            // Spindle
            Circle()
                .fill(Color(hex: 0x241818))
                .frame(width: size * 0.07, height: size * 0.07)
        }
    }

    private var symbolFallback: some View {
        ZStack {
            Circle().fill(track.accentColor.opacity(0.35))
            Image(systemName: track.systemImage)
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundStyle(isPlaying ? Brand.deepCoral : Color.primary.opacity(0.85))
        }
    }

    private func loadRemoteIfNeeded() async {
        remoteImage = nil
        guard let remote = track.remote else { return }
        let key = remote.thumbnailPath
        guard !key.isEmpty else { return }
        remoteImage = await SoundscapeThumbnailLoader.shared.loadImage(
            objectKey: key,
            cacheVersion: remote.cacheVersion
        )
    }
}
