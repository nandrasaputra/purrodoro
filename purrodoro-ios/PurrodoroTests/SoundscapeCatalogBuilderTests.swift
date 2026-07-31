//
//  SoundscapeCatalogBuilderTests.swift
//  PurrodoroTests
//

import Testing
import Foundation
@testable import Purrodoro

struct SoundscapeCatalogBuilderTests {

    private let rainDefault = BundledTrackDefault(
        kind: .ambience, localId: 1, rawResource: "l_amb_rain",
        nameKey: "sound.rain.name", subtitleKey: "sound.rain.desc",
        symbol: "Rain", defaultPremium: false, sortOrder: 10
    )
    private let cafeDefault = BundledTrackDefault(
        kind: .ambience, localId: 2, rawResource: "l_amb_cafe",
        nameKey: "sound.cafe.name", subtitleKey: "sound.cafe.desc",
        symbol: "Cafe", defaultPremium: false, sortOrder: 20
    )

    private func bundled(
        localId: Int,
        premium: Bool = false,
        active: Bool = true,
        sort: Int = 0,
        rawResource: String = "l_amb_rain",
        name: String = "",
        artist: String = "",
        symbol: String = ""
    ) -> BundledCatalogRow {
        BundledCatalogRow(
            kind: "ambience",
            localId: localId,
            name: name,
            artist: artist,
            symbol: symbol,
            rawResource: rawResource,
            isPremium: premium,
            sortOrder: sort,
            isActive: active
        )
    }

    private func remote(id: Int, kind: String = "music") -> RemoteCatalogRow {
        RemoteCatalogRow(
            id: id,
            kind: kind,
            name: "Cloud Track",
            artist: "Artist",
            symbol: "",
            filePath: "lofi/x.mp3",
            thumbnailPath: "",
            fileSizeBytes: 2_000_000,
            isPremium: false,
            sortOrder: 10,
            updatedAtMillis: 1_000
        )
    }

    @Test func missingRowsFallBackToDefaultsAndStayVisible() {
        let options = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "name" },
            subtitleFor: { _, _ in "sub" },
            bundledDefaults: [rainDefault, cafeDefault]
        )
        #expect(options.map(\.trackId) == [1, 2])
        #expect(options[0].audio == .bundled(resourceName: "l_amb_rain"))
        #expect(options[0].remote == nil)
    }

    @Test func inactiveRowHidesItsDefault() {
        let options = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [bundled(localId: 1, active: false)],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "name" },
            subtitleFor: { _, _ in "sub" },
            bundledDefaults: [rainDefault, cafeDefault]
        )
        #expect(options.map(\.trackId) == [2])
    }

    @Test func rowPremiumOverrideWinsOverDefault() {
        let options = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [bundled(localId: 1, premium: true)],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "name" },
            subtitleFor: { _, _ in "sub" },
            bundledDefaults: [rainDefault]
        )
        #expect(options.single.locked)
    }

    @Test func rowSortOrderOverrideReordersBundledTracks() {
        let options = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [
                bundled(localId: 1, sort: 99),
                bundled(localId: 2, sort: 1),
            ],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "name" },
            subtitleFor: { _, _ in "sub" },
            bundledDefaults: [rainDefault, cafeDefault]
        )
        #expect(options.map(\.trackId) == [2, 1])
    }

    @Test func nonBlankNameAndDescriptionOverridesWin() {
        let row = bundled(
            localId: 1,
            name: "Custom Rain",
            artist: "Soft drizzle",
            symbol: "Storm"
        )
        let options = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [row],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "default name" },
            subtitleFor: { _, _ in "default sub" },
            bundledDefaults: [rainDefault]
        )
        #expect(options.single.name == "Custom Rain")
        #expect(options.single.subtitle == "Soft drizzle")
        #expect(options.single.symbol == "Storm")
    }

    @Test func remoteMusicNotDownloadedHasNullAudioAndNotDownloadedState() {
        let options = SoundscapeCatalogBuilder.buildMusicTracks(
            bundledRows: [],
            remoteRows: [remote(id: 100)],
            downloadStates: [100: .notDownloaded],
            localPaths: [:],
            nameFor: { _, _ in "unused" },
            subtitleFor: { _, _ in "unused" },
            bundledDefaults: []
        )
        #expect(options.single.audio == nil)
        #expect(options.single.remote?.downloadState == .notDownloaded)
    }

    @Test func unknownCatalogRawResourceFallsBackToRegistryDefault() {
        let row = BundledCatalogRow(
            kind: "ambience",
            localId: 1,
            name: "",
            artist: "",
            symbol: "",
            rawResource: "stale_server_name",
            isPremium: false,
            sortOrder: 10,
            isActive: true
        )
        let options = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [row],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "name" },
            subtitleFor: { _, _ in "sub" },
            bundledDefaults: [rainDefault]
        )
        #expect(options.single.audio == .bundled(resourceName: "l_amb_rain"))
    }

    @Test func bundledAmbienceOmittedWhenNoRawResourceResolves() {
        let unmapped = rainDefault // raw still valid — use a fake default instead
        let fake = BundledTrackDefault(
            kind: .ambience, localId: 1, rawResource: "does_not_exist",
            nameKey: "x", subtitleKey: "y", symbol: "Rain",
            defaultPremium: false, sortOrder: 10
        )
        let options = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "name" },
            subtitleFor: { _, _ in "sub" },
            bundledDefaults: [fake, cafeDefault]
        )
        #expect(options.map(\.trackId) == [2])
        _ = unmapped
    }

    @Test func bundledMusicKeepsRowWithNullAudioWhenRawMissing() {
        let unmapped = BundledTrackDefault(
            kind: .music, localId: 1, rawResource: "does_not_exist",
            nameKey: "sound.lofi.desk_lamp_rain.name",
            subtitleKey: "sound.lofi.local_artist",
            symbol: "", defaultPremium: false, sortOrder: 10
        )
        let options = SoundscapeCatalogBuilder.buildMusicTracks(
            bundledRows: [],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "name" },
            subtitleFor: { _, _ in "artist" },
            bundledDefaults: [unmapped]
        )
        #expect(options.count == 1)
        #expect(options.single.trackId == 1)
        #expect(options.single.audio == nil)
    }

    @Test func bundledTracksRenderBeforeRemoteTracks() {
        let options = SoundscapeCatalogBuilder.buildMusicTracks(
            bundledRows: [],
            remoteRows: [remote(id: 100)],
            downloadStates: [100: .notDownloaded],
            localPaths: [:],
            nameFor: { _, _ in "Bloom" },
            subtitleFor: { _, _ in "Yasumu" },
            bundledDefaults: [
                BundledTrackDefault(
                    kind: .music, localId: 1, rawResource: "l_lofi_desk_lamp_rain",
                    nameKey: "sound.lofi.desk_lamp_rain.name",
                    subtitleKey: "sound.lofi.local_artist",
                    symbol: "", defaultPremium: false, sortOrder: 10
                ),
            ]
        )
        #expect(options.map(\.trackId) == [1, 100])
    }

    @Test func listIdsAreUniqueAcrossDecks() {
        let ambience = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: [],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "a" },
            subtitleFor: { _, _ in "s" },
            bundledDefaults: [rainDefault]
        )
        let music = SoundscapeCatalogBuilder.buildMusicTracks(
            bundledRows: [],
            remoteRows: [],
            downloadStates: [:],
            localPaths: [:],
            nameFor: { _, _ in "m" },
            subtitleFor: { _, _ in "s" },
            bundledDefaults: [
                BundledTrackDefault(
                    kind: .music, localId: 1, rawResource: "l_lofi_desk_lamp_rain",
                    nameKey: "x", subtitleKey: "y", symbol: "",
                    defaultPremium: false, sortOrder: 10
                ),
            ]
        )
        #expect(ambience.single.listId == "ambience-1")
        #expect(music.single.listId == "music-1")
        #expect(ambience.single.listId != music.single.listId)
    }
}

private extension Array {
    var single: Element {
        precondition(count == 1)
        return self[0]
    }
}
