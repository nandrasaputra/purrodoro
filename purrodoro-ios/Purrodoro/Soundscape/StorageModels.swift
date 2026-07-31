//
//  StorageModels.swift
//  Purrodoro
//
//  Pure Storage screen model + builder. Mirrors Android `StorageModels.kt`.
//

import Foundation

/// One downloadable asset as shown on the Storage screen. Generic on purpose:
/// today every item is a soundscape track.
struct StorageItem: Identifiable, Equatable, Sendable {
    let id: Int
    let name: String
    /// `"music"` | `"ambience"`
    let kind: String
    let sizeBytes: Int64
    /// false while a download is in progress
    let isRemovable: Bool
    /// non-nil (0…1) while downloading, else nil
    let progress: Float?
}

struct StorageSummary: Equatable, Sendable {
    let items: [StorageItem]
    /// Sum of the *downloaded* item sizes (in-progress rows excluded).
    let totalBytes: Int64

    static let empty = StorageSummary(items: [], totalBytes: 0)
}

/// Pure builder for the Storage screen.
///
/// - **Downloaded** tracks become removable rows (real on-disk size via `sizeFor`).
/// - **Downloading** tracks appear as non-removable rows carrying progress.
/// - **NotDownloaded** tracks are omitted.
///
/// Rows sort downloaded-first, then by size desc, then name. The total counts
/// only downloaded items.
func buildStorageList(
    catalog: [RemoteCatalogRow],
    downloadStates: [Int: DownloadState],
    sizeFor: (RemoteCatalogRow) -> Int64
) -> StorageSummary {
    let items: [StorageItem] = catalog.compactMap { track in
        let state = downloadStates[track.id] ?? .notDownloaded
        switch state {
        case .downloaded:
            return StorageItem(
                id: track.id,
                name: track.name,
                kind: track.kind,
                sizeBytes: sizeFor(track),
                isRemovable: true,
                progress: nil
            )
        case .downloading(let progress):
            return StorageItem(
                id: track.id,
                name: track.name,
                kind: track.kind,
                sizeBytes: 0,
                isRemovable: false,
                progress: progress
            )
        case .notDownloaded:
            return nil
        }
    }
    .sorted {
        // Downloaded (progress == nil) first, then size desc, then name.
        let aDownloading = $0.progress != nil
        let bDownloading = $1.progress != nil
        if aDownloading != bDownloading { return !aDownloading && bDownloading }
        if $0.sizeBytes != $1.sizeBytes { return $0.sizeBytes > $1.sizeBytes }
        return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
    }

    let total = items.filter { $0.progress == nil }.reduce(Int64(0)) { $0 + $1.sizeBytes }
    return StorageSummary(items: items, totalBytes: total)
}

/// Human-readable byte size, e.g. "0 MB", "740 KB", "12.4 MB".
func formatBytes(_ bytes: Int64, locale: Locale = .current) -> String {
    let kb = 1024.0
    let mb = kb * 1024
    let formatter = NumberFormatter()
    formatter.locale = locale
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 1

    if bytes <= 0 { return "0 MB" }
    if Double(bytes) < mb {
        formatter.maximumFractionDigits = 0
        let value = formatter.string(from: NSNumber(value: Double(bytes) / kb)) ?? "0"
        return "\(value) KB"
    }
    formatter.minimumFractionDigits = 1
    formatter.maximumFractionDigits = 1
    let value = formatter.string(from: NSNumber(value: Double(bytes) / mb)) ?? "0.0"
    return "\(value) MB"
}
