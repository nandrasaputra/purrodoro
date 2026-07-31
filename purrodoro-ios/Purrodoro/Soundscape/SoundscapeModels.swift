//
//  SoundscapeModels.swift
//  Purrodoro
//
//  Shared soundscape models. Mirrors Android `AudioSource`, `DownloadState`,
//  `RemoteTrackInfo`, and the catalog row shapes used by the builder.
//

import Foundation

enum SoundscapeKind: String, Codable, CaseIterable, Sendable {
    case ambience
    case music
}

/// Where a track's audio bytes come from: shipped in the app bundle or
/// downloaded from the cloud catalog to Application Support.
enum AudioSource: Equatable, Sendable {
    case bundled(resourceName: String)
    case localFile(path: String)
}

enum DownloadState: Equatable, Sendable {
    case notDownloaded
    case downloading(progress: Float)
    case downloaded
}

/// Cloud-catalog metadata; nil on bundled tracks.
struct RemoteTrackInfo: Equatable, Sendable {
    let fileSizeBytes: Int64
    var downloadState: DownloadState
    let thumbnailPath: String
    let filePath: String
    /// Catalog `updated_at` millis — versions on-disk cache entries.
    let cacheVersion: Int64
}

/// One row in the Sounds sheet (ambience or lo-fi). `listId` is unique across
/// both decks so SwiftUI `ForEach` never collides on shared local ids 1…n.
struct SoundscapeTrack: Identifiable, Equatable, Sendable {
    var id: String { listId }
    let listId: String
    let kind: SoundscapeKind
    let trackId: Int
    let name: String
    let subtitle: String
    let locked: Bool
    let symbol: String
    /// nil = cloud track not downloaded yet (not playable), or unresolvable bundled resource.
    let audio: AudioSource?
    let remote: RemoteTrackInfo?

    static func makeListId(kind: SoundscapeKind, trackId: Int) -> String {
        "\(kind.rawValue)-\(trackId)"
    }
}

/// Cached / API row for `soundscape_bundled_catalog` overrides.
struct BundledCatalogRow: Equatable, Sendable {
    let kind: String
    let localId: Int
    let name: String
    let artist: String
    let symbol: String
    let rawResource: String
    let isPremium: Bool
    let sortOrder: Int
    let isActive: Bool
}

/// Cached / API row for `soundscape_tracks` (cloud only).
struct RemoteCatalogRow: Equatable, Sendable {
    let id: Int
    let kind: String
    let name: String
    let artist: String
    let symbol: String
    let filePath: String
    let thumbnailPath: String
    let fileSizeBytes: Int64
    let isPremium: Bool
    let sortOrder: Int
    let updatedAtMillis: Int64
}

extension RemoteCatalogRow {
    func toTrack(
        kind: SoundscapeKind,
        downloadState: DownloadState,
        localPath: String?
    ) -> SoundscapeTrack {
        SoundscapeTrack(
            listId: SoundscapeTrack.makeListId(kind: kind, trackId: id),
            kind: kind,
            trackId: id,
            name: name,
            subtitle: artist,
            locked: isPremium,
            symbol: symbol,
            audio: localPath.map { .localFile(path: $0) },
            remote: RemoteTrackInfo(
                fileSizeBytes: fileSizeBytes,
                downloadState: downloadState,
                thumbnailPath: SoundscapeConfig.thumbnailKey(
                    thumbnailPath: thumbnailPath,
                    filePath: filePath
                ),
                filePath: filePath,
                cacheVersion: updatedAtMillis
            )
        )
    }
}
