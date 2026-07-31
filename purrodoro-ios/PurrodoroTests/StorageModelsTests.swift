//
//  StorageModelsTests.swift
//  PurrodoroTests
//

import Testing
import Foundation
@testable import Purrodoro

struct StorageModelsTests {

    private func track(
        id: Int,
        kind: String = "music",
        name: String = "Track \(id)"
    ) -> RemoteCatalogRow {
        RemoteCatalogRow(
            id: id,
            kind: kind,
            name: name,
            artist: "",
            symbol: "",
            filePath: "x/\(id).mp3",
            thumbnailPath: "",
            fileSizeBytes: 0,
            isPremium: false,
            sortOrder: 0,
            updatedAtMillis: 1
        )
    }

    @Test func onlyDownloadedAndDownloadingAppear() {
        let catalog = [track(id: 100), track(id: 101), track(id: 102)]
        let states: [Int: DownloadState] = [
            100: .downloaded,
            101: .notDownloaded,
            102: .downloading(progress: 0.5),
        ]
        let summary = buildStorageList(catalog: catalog, downloadStates: states, sizeFor: { _ in 1_000 })
        #expect(summary.items.map(\.id) == [100, 102])
    }

    @Test func totalCountsDownloadedSizesOnly() {
        let catalog = [track(id: 100), track(id: 101)]
        let states: [Int: DownloadState] = [
            100: .downloaded,
            101: .downloading(progress: 0.2),
        ]
        let summary = buildStorageList(catalog: catalog, downloadStates: states) { row in
            row.id == 100 ? 2_000 : 9_999
        }
        #expect(summary.totalBytes == 2_000)
    }

    @Test func downloadedRowsSortBeforeDownloadingThenBySizeDesc() {
        let catalog = [track(id: 100), track(id: 101), track(id: 102)]
        let states: [Int: DownloadState] = [
            100: .downloaded,
            101: .downloaded,
            102: .downloading(progress: 0.1),
        ]
        let summary = buildStorageList(catalog: catalog, downloadStates: states) { row in
            row.id == 100 ? 500 : 5_000
        }
        // 101 (5000) before 100 (500), downloading 102 last.
        #expect(summary.items.map(\.id) == [101, 100, 102])
    }

    @Test func downloadingRowIsNotRemovableAndCarriesProgress() {
        let summary = buildStorageList(
            catalog: [track(id: 102)],
            downloadStates: [102: .downloading(progress: 0.42)],
            sizeFor: { _ in 0 }
        )
        let row = summary.items[0]
        #expect(!row.isRemovable)
        #expect(row.progress == 0.42)
    }

    @Test func downloadedRowIsRemovableWithNilProgress() {
        let summary = buildStorageList(
            catalog: [track(id: 100)],
            downloadStates: [100: .downloaded],
            sizeFor: { _ in 1_234 }
        )
        let row = summary.items[0]
        #expect(row.isRemovable)
        #expect(row.progress == nil)
        #expect(row.sizeBytes == 1_234)
    }

    @Test func formatBytesRendersZeroKbAndMb() {
        #expect(formatBytes(0, locale: Locale(identifier: "en_US")) == "0 MB")
        #expect(formatBytes(740 * 1024, locale: Locale(identifier: "en_US")) == "740 KB")
        #expect(formatBytes(2 * 1024 * 1024, locale: Locale(identifier: "en_US")) == "2.0 MB")
    }

    @Test func thumbnailKeyFallsBackToWebpStem() {
        #expect(
            SoundscapeConfig.thumbnailKey(thumbnailPath: "", filePath: "lofi/track.mp3")
            == "lofi/track.webp"
        )
        #expect(
            SoundscapeConfig.thumbnailKey(thumbnailPath: "covers/a.webp", filePath: "x.mp3")
            == "covers/a.webp"
        )
    }
}
