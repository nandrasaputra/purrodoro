//
//  SoundsStore.swift
//  Purrodoro
//
//  Dual-deck soundscape state: ambience + lo-fi catalog lists, selection,
//  volumes, downloads, and playback. Mirrors Android SoundsScreen wiring.
//

import Foundation
import Combine
import Supabase
import SwiftUI

@MainActor
final class SoundsStore: ObservableObject {

    // MARK: - Published catalog / playback

    @Published private(set) var ambienceTracks: [SoundscapeTrack] = []
    @Published private(set) var musicTracks: [SoundscapeTrack] = []

    @Published private(set) var selectedAmbienceId: Int?
    @Published private(set) var selectedMusicId: Int?
    @Published private(set) var playingAmbienceId: Int?
    @Published private(set) var playingMusicId: Int?

    @Published var ambienceVolume: Float = 0.10 {
        didSet {
            Self.writeFloat(ambienceVolume, store: store, key: ambienceVolumeKey)
            player.setAmbienceVolume(ambienceVolume)
        }
    }

    @Published var musicVolume: Float = 0.50 {
        didSet {
            Self.writeFloat(musicVolume, store: store, key: musicVolumeKey)
            player.setMusicVolume(musicVolume)
        }
    }

    @Published private(set) var isRefreshing = false

    /// Upsell trigger set when a locked track is tapped on the free tier.
    @Published var pendingUpsell: PremiumUpsellTrigger?

    private let store: KeyValueStore
    private let prefs: PreferencesStore?
    private let localization: Localization?
    private let catalogApi: SoundscapeCatalogApi?
    let downloadManager: TrackDownloadManager
    private let player = SoundscapePlayer()

    private var bundledRows: [BundledCatalogRow] = []
    private var remoteRows: [RemoteCatalogRow] = []
    private var downloadCancellable: AnyCancellable?
    private var prefsCancellable: AnyCancellable?
    private var locCancellable: AnyCancellable?

    private let ambienceIdKey = "purrodoro.soundscapeMixAmbienceId"
    private let musicIdKey = "purrodoro.soundscapeMixMusicId"
    private let legacySelectedKey = "purrodoro.selectedSound"
    private let ambienceVolumeKey = "purrodoro.ambienceVolume"
    private let musicVolumeKey = "purrodoro.musicVolume"

    private var isPremium: Bool { prefs?.isPremium ?? false }

    var isPlaying: Bool {
        playingAmbienceId != nil || playingMusicId != nil
    }

    init(
        store: KeyValueStore = UserDefaults.standard,
        client: SupabaseClient? = nil,
        prefs: PreferencesStore? = nil,
        localization: Localization? = nil,
        catalogApi: SoundscapeCatalogApi? = nil,
        downloadManager: TrackDownloadManager? = nil
    ) {
        self.store = store
        self.prefs = prefs
        self.localization = localization
        if let catalogApi {
            self.catalogApi = catalogApi
        } else if let client {
            self.catalogApi = SupabaseSoundscapeCatalogApi(client: client)
        } else {
            self.catalogApi = nil
        }
        self.downloadManager = downloadManager ?? TrackDownloadManager()

        selectedAmbienceId = Self.readOptionalInt(store: store, key: ambienceIdKey)
            ?? Self.migrateLegacySelectedSound(store: store)
        selectedMusicId = Self.readOptionalInt(store: store, key: musicIdKey)

        if let v = Self.readOptionalFloat(store: store, key: ambienceVolumeKey) {
            ambienceVolume = v
        }
        if let v = Self.readOptionalFloat(store: store, key: musicVolumeKey) {
            musicVolume = v
        }

        rebuildTracks()

        downloadCancellable = self.downloadManager.$states
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                self?.rebuildTracks()
                self?.syncPlaybackFromSelection()
            }

        prefsCancellable = prefs?.objectWillChange
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                self?.syncPlaybackFromSelection()
            }

        locCancellable = localization?.$language
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                self?.rebuildTracks()
            }
    }

    // MARK: - Catalog refresh

    func refreshCatalog() async {
        guard let catalogApi else {
            rebuildTracks()
            return
        }
        isRefreshing = true
        defer { isRefreshing = false }

        do {
            let dtos = try await catalogApi.fetchActiveTracks()
            remoteRows = dtos.compactMap { $0.toRow() }
        } catch {
            // Keep last cache on failure (offline-friendly).
        }
        do {
            let dtos = try await catalogApi.fetchBundledCatalog()
            bundledRows = dtos.map { $0.toRow() }
        } catch {
            // Keep last cache on failure.
        }

        downloadManager.prune(tracks: remoteRows)
        rebuildTracks()
        syncPlaybackFromSelection()
    }

    // MARK: - Toggle / play

    /// Tile tap for either deck. Returns an upsell trigger when locked.
    @discardableResult
    func toggle(_ track: SoundscapeTrack) -> PremiumUpsellTrigger? {
        if track.locked && !isPremium {
            let trigger = PremiumUpsellTrigger.lockedSound(track.name)
            pendingUpsell = trigger
            return trigger
        }

        if track.remote?.downloadState == .notDownloaded {
            startDownload(trackId: track.trackId)
            return nil
        }
        if case .downloading = track.remote?.downloadState {
            return nil
        }
        guard track.audio != nil else { return nil }

        switch track.kind {
        case .ambience:
            if playingAmbienceId == track.trackId {
                stopAmbience()
            } else {
                playAmbience(track)
            }
        case .music:
            if playingMusicId == track.trackId {
                stopMusic()
            } else {
                playMusic(track)
            }
        }
        return nil
    }

    func stop() {
        stopAmbience()
        stopMusic()
    }

    func stopAmbience() {
        selectedAmbienceId = nil
        persistMix()
        playingAmbienceId = nil
        player.pauseAmbience()
        objectWillChange.send()
    }

    func stopMusic() {
        selectedMusicId = nil
        persistMix()
        playingMusicId = nil
        player.pauseMusic()
        objectWillChange.send()
    }

    func pauseAll() {
        playingAmbienceId = nil
        playingMusicId = nil
        player.pauseAll()
        objectWillChange.send()
    }

    // MARK: - Backup

    /// 1-based ambience local id for backup interchange (bundled local ids 1…n).
    var exportSoundId: Int {
        selectedAmbienceId ?? 1
    }

    func applyRestoredSoundId(_ id: Int) {
        let clamped = max(id, 1)
        selectedAmbienceId = clamped
        persistMix()
    }

    // MARK: - Private playback

    private func playAmbience(_ track: SoundscapeTrack) {
        guard let source = track.audio else { return }
        selectedAmbienceId = track.trackId
        persistMix()
        // Also mirror into legacy key for older readers.
        Self.writeOptionalInt(track.trackId, store: store, key: legacySelectedKey)
        playingAmbienceId = track.trackId
        player.playAmbience(source, volume: ambienceVolume)
        objectWillChange.send()
    }

    private func playMusic(_ track: SoundscapeTrack) {
        guard let source = track.audio else { return }
        selectedMusicId = track.trackId
        persistMix()
        playingMusicId = track.trackId
        player.playMusic(source, volume: musicVolume)
        objectWillChange.send()
    }

    private func startDownload(trackId: Int) {
        guard let row = remoteRows.first(where: { $0.id == trackId }) else { return }
        Task {
            let ok = await downloadManager.download(row)
            if ok {
                rebuildTracks()
                if let track = (ambienceTracks + musicTracks).first(where: { $0.trackId == trackId }) {
                    _ = toggle(track)
                }
            }
        }
    }

    private func syncPlaybackFromSelection() {
        if let id = playingAmbienceId {
            guard let track = ambienceTracks.first(where: { $0.trackId == id }),
                  !(track.locked && !isPremium),
                  let source = track.audio
            else {
                playingAmbienceId = nil
                player.pauseAmbience()
                return
            }
            player.playAmbience(source, volume: ambienceVolume)
        }
        if let id = playingMusicId {
            guard let track = musicTracks.first(where: { $0.trackId == id }),
                  !(track.locked && !isPremium),
                  let source = track.audio
            else {
                playingMusicId = nil
                player.pauseMusic()
                return
            }
            player.playMusic(source, volume: musicVolume)
        }
    }

    private func rebuildTracks() {
        let states = downloadManager.states
        var paths: [Int: String] = [:]
        for row in remoteRows {
            if let path = downloadManager.localPath(for: row) {
                paths[row.id] = path
            }
        }

        let loc = localization
        ambienceTracks = SoundscapeCatalogBuilder.buildAmbienceTracks(
            bundledRows: bundledRows,
            remoteRows: remoteRows,
            downloadStates: states,
            localPaths: paths,
            nameFor: { _, def in loc?.t(def.nameKey) ?? def.nameKey },
            subtitleFor: { _, def in loc?.t(def.subtitleKey) ?? "" }
        )
        musicTracks = SoundscapeCatalogBuilder.buildMusicTracks(
            bundledRows: bundledRows,
            remoteRows: remoteRows,
            downloadStates: states,
            localPaths: paths,
            nameFor: { _, def in loc?.t(def.nameKey) ?? def.nameKey },
            subtitleFor: { _, def in loc?.t(def.subtitleKey) ?? "" }
        )
    }

    private func persistMix() {
        Self.writeOptionalInt(selectedAmbienceId, store: store, key: ambienceIdKey)
        Self.writeOptionalInt(selectedMusicId, store: store, key: musicIdKey)
    }

    // MARK: - Persistence helpers

    private static func writeOptionalInt(_ value: Int?, store: KeyValueStore, key: String) {
        if let value, let data = try? JSONEncoder().encode(value) {
            store.set(data, forKey: key)
        } else {
            store.set(nil, forKey: key)
        }
    }

    private static func readOptionalInt(store: KeyValueStore, key: String) -> Int? {
        if let data = store.data(forKey: key),
           let value = try? JSONDecoder().decode(Int.self, from: data) {
            return value
        }
        if let raw = store.string(forKey: key), let value = Int(raw) {
            return value
        }
        return nil
    }

    private static func writeFloat(_ value: Float, store: KeyValueStore, key: String) {
        if let data = try? JSONEncoder().encode(Double(value)) {
            store.set(data, forKey: key)
        }
    }

    private static func readOptionalFloat(store: KeyValueStore, key: String) -> Float? {
        if let data = store.data(forKey: key),
           let value = try? JSONDecoder().decode(Double.self, from: data) {
            return Float(value)
        }
        return nil
    }

    /// Map pre-Soundscape AmbientKind persistence into ambience local ids.
    private static func migrateLegacySelectedSound(store: KeyValueStore) -> Int? {
        if let id = readOptionalInt(store: store, key: "purrodoro.selectedSound") {
            return id
        }
        guard let raw = store.string(forKey: "purrodoro.selectedSound") else { return nil }
        switch raw {
        case "rain": return 1
        case "cafe": return 2
        case "fire": return 3
        case "forest": return 4
        case "waves": return 5
        default: return Int(raw)
        }
    }
}

// MARK: - SF Symbol helpers for sheet rows

extension SoundscapeTrack {
    var systemImage: String {
        switch kind {
        case .music:
            return "headphones"
        case .ambience:
            switch symbol.lowercased() {
            case "rain": return "cloud.rain.fill"
            case "cafe": return "cup.and.saucer.fill"
            case "fire": return "flame.fill"
            case "forest": return "tree.fill"
            case "wave": return "water.waves"
            case "wind": return "wind"
            default: return "speaker.wave.2.fill"
            }
        }
    }

    var accentColor: Color {
        switch kind {
        case .music:
            return Brand.lavender
        case .ambience:
            switch symbol.lowercased() {
            case "rain": return Brand.sky
            case "cafe": return Brand.peach
            case "fire": return Brand.coral
            case "forest": return Brand.mint
            case "wave": return Brand.lavender
            case "wind": return Brand.sky
            default: return Brand.peach
            }
        }
    }
}
