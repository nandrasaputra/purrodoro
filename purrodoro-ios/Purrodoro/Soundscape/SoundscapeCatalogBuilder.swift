//
//  SoundscapeCatalogBuilder.swift
//  Purrodoro
//
//  Pure merge of the bundled registry + cached overrides + remote cloud
//  catalog into the option lists the Sounds sheet renders.
//  Mirrors Android `SoundscapeCatalogBuilder.kt` (unit-testable).
//

import Foundation

enum SoundscapeCatalogBuilder {

    static func buildAmbienceTracks(
        bundledRows: [BundledCatalogRow],
        remoteRows: [RemoteCatalogRow],
        downloadStates: [Int: DownloadState],
        localPaths: [Int: String],
        nameFor: (BundledCatalogRow?, BundledTrackDefault) -> String,
        subtitleFor: (BundledCatalogRow?, BundledTrackDefault) -> String,
        bundledDefaults: [BundledTrackDefault] = BundledTrackRegistry.defaults
    ) -> [SoundscapeTrack] {
        let bundled = mergeBundled(kind: .ambience, rows: bundledRows, defaults: bundledDefaults)
            .compactMap { def, row -> SoundscapeTrack? in
                guard let raw = BundledTrackRegistry.resolveRawResource(
                    row?.rawResource,
                    fallback: def.rawResource
                ) else { return nil }
                return SoundscapeTrack(
                    listId: SoundscapeTrack.makeListId(kind: .ambience, trackId: def.localId),
                    kind: .ambience,
                    trackId: def.localId,
                    name: nonBlank(row?.name) ?? nameFor(row, def),
                    subtitle: nonBlank(row?.artist) ?? subtitleFor(row, def),
                    locked: row?.isPremium ?? def.defaultPremium,
                    symbol: nonBlank(row?.symbol) ?? def.symbol,
                    audio: .bundled(resourceName: raw),
                    remote: nil
                )
            }

        let remote = remoteRows
            .filter { $0.kind == SoundscapeKind.ambience.rawValue }
            .sorted { lhs, rhs in
                if lhs.sortOrder != rhs.sortOrder { return lhs.sortOrder < rhs.sortOrder }
                return lhs.id < rhs.id
            }
            .map { row in
                row.toTrack(
                    kind: .ambience,
                    downloadState: stateFor(row, downloadStates),
                    localPath: localPathFor(row, downloadStates, localPaths)
                )
            }

        return bundled + remote
    }

    static func buildMusicTracks(
        bundledRows: [BundledCatalogRow],
        remoteRows: [RemoteCatalogRow],
        downloadStates: [Int: DownloadState],
        localPaths: [Int: String],
        nameFor: (BundledCatalogRow?, BundledTrackDefault) -> String,
        subtitleFor: (BundledCatalogRow?, BundledTrackDefault) -> String,
        bundledDefaults: [BundledTrackDefault] = BundledTrackRegistry.defaults
    ) -> [SoundscapeTrack] {
        let bundled = mergeBundled(kind: .music, rows: bundledRows, defaults: bundledDefaults)
            .map { def, row -> SoundscapeTrack in
                let raw = BundledTrackRegistry.resolveRawResource(
                    row?.rawResource,
                    fallback: def.rawResource
                )
                return SoundscapeTrack(
                    listId: SoundscapeTrack.makeListId(kind: .music, trackId: def.localId),
                    kind: .music,
                    trackId: def.localId,
                    name: nonBlank(row?.name) ?? nameFor(row, def),
                    subtitle: nonBlank(row?.artist) ?? subtitleFor(row, def),
                    locked: row?.isPremium ?? def.defaultPremium,
                    symbol: nonBlank(row?.symbol) ?? def.symbol,
                    audio: raw.map { .bundled(resourceName: $0) },
                    remote: nil
                )
            }

        let remote = remoteRows
            .filter { $0.kind == SoundscapeKind.music.rawValue }
            .sorted { lhs, rhs in
                if lhs.sortOrder != rhs.sortOrder { return lhs.sortOrder < rhs.sortOrder }
                return lhs.id < rhs.id
            }
            .map { row in
                row.toTrack(
                    kind: .music,
                    downloadState: stateFor(row, downloadStates),
                    localPath: localPathFor(row, downloadStates, localPaths)
                )
            }

        return bundled + remote
    }

    // MARK: - Merge

    /// Left-join registry defaults (existence) with cached rows (overrides).
    /// - row absent → visible default
    /// - row present and inactive → dropped (hidden)
    /// - row present and active → kept, overrides applied by the caller
    static func mergeBundled(
        kind: SoundscapeKind,
        rows: [BundledCatalogRow],
        defaults: [BundledTrackDefault]
    ) -> [(BundledTrackDefault, BundledCatalogRow?)] {
        let byLocalId = Dictionary(
            uniqueKeysWithValues: rows.filter { $0.kind == kind.rawValue }.map { ($0.localId, $0) }
        )
        return defaults
            .filter { $0.kind == kind }
            .compactMap { def -> (BundledTrackDefault, BundledCatalogRow?)? in
                let row = byLocalId[def.localId]
                if let row, !row.isActive { return nil }
                return (def, row)
            }
            .sorted { lhs, rhs in
                let left = lhs.1?.sortOrder ?? lhs.0.sortOrder
                let right = rhs.1?.sortOrder ?? rhs.0.sortOrder
                return left < right
            }
    }

    // MARK: - Helpers

    private static func stateFor(
        _ row: RemoteCatalogRow,
        _ states: [Int: DownloadState]
    ) -> DownloadState {
        states[row.id] ?? .notDownloaded
    }

    private static func localPathFor(
        _ row: RemoteCatalogRow,
        _ states: [Int: DownloadState],
        _ localPaths: [Int: String]
    ) -> String? {
        guard stateFor(row, states) == .downloaded else { return nil }
        return localPaths[row.id]
    }

    private static func nonBlank(_ value: String?) -> String? {
        guard let value else { return nil }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
