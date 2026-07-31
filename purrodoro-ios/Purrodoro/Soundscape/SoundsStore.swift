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

    /// True once the user has ever played or saved a mix (drives dock first-run hint).
    @Published private(set) var hasEverUsedSoundscape: Bool = false

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
    private let hasUsedKey = "purrodoro.soundscapeHasEverUsed"

    private var isPremium: Bool { prefs?.isPremium ?? false }

    var isPlaying: Bool {
        playingAmbienceId != nil || playingMusicId != nil
    }

    /// Cloud catalog rows + download states for the Storage screen.
    var storageSummary: StorageSummary {
        buildStorageList(
            catalog: remoteRows,
            downloadStates: downloadManager.states,
            sizeFor: { [downloadManager] row in downloadManager.fileSizeBytes(row) }
        )
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

        hasEverUsedSoundscape = store.data(forKey: hasUsedKey) != nil
            || selectedAmbienceId != nil
            || selectedMusicId != nil

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

    /// Dock play/pause: pause clears playing ids (mix prefs kept); play resumes mix.
    func togglePlayPause() {
        if isPlaying {
            pauseAll()
        } else {
            resumeFromMix()
        }
    }

    /// Resume from saved mix, or first unlocked playable track per deck.
    func resumeFromMix() {
        markSoundscapeUsed()

        func isPlayable(_ track: SoundscapeTrack) -> Bool {
            !(track.locked && !isPremium) && track.audio != nil
        }

        var ambienceId = selectedAmbienceId.flatMap { id in
            ambienceTracks.first(where: { $0.trackId == id && isPlayable($0) })?.trackId
        }
        if ambienceId == nil {
            if let first = ambienceTracks.first(where: isPlayable) {
                ambienceId = first.trackId
                if selectedAmbienceId == nil {
                    selectedAmbienceId = first.trackId
                    persistMix()
                }
            }
        }

        var musicId = selectedMusicId.flatMap { id in
            musicTracks.first(where: { $0.trackId == id && isPlayable($0) })?.trackId
        }
        if musicId == nil {
            if let first = musicTracks.first(where: isPlayable) {
                musicId = first.trackId
                if selectedMusicId == nil {
                    selectedMusicId = first.trackId
                    persistMix()
                }
            }
        }

        if let id = ambienceId,
           let track = ambienceTracks.first(where: { $0.trackId == id }),
           let source = track.audio {
            playingAmbienceId = id
            player.playAmbience(source, volume: ambienceVolume)
        } else {
            playingAmbienceId = nil
            player.pauseAmbience()
        }

        if let id = musicId,
           let track = musicTracks.first(where: { $0.trackId == id }),
           let source = track.audio {
            playingMusicId = id
            player.playMusic(source, volume: musicVolume)
        } else {
            playingMusicId = nil
            player.pauseMusic()
        }
        objectWillChange.send()
    }

    // MARK: - Storage (downloads)

    /// Stop playback of `id` if active, then delete the on-disk file. Mix prefs kept.
    func removeDownload(id: Int) {
        guard let row = remoteRows.first(where: { $0.id == id }) else { return }
        if playingAmbienceId == id {
            playingAmbienceId = nil
            player.pauseAmbience()
        }
        if playingMusicId == id {
            playingMusicId = nil
            player.pauseMusic()
        }
        downloadManager.delete(row)
        rebuildTracks()
        objectWillChange.send()
    }

    func removeAllDownloads() {
        let downloaded = remoteRows.filter { downloadManager.states[$0.id] == .downloaded }
        let ids = Set(downloaded.map(\.id))
        if let id = playingAmbienceId, ids.contains(id) {
            playingAmbienceId = nil
            player.pauseAmbience()
        }
        if let id = playingMusicId, ids.contains(id) {
            playingMusicId = nil
            player.pauseMusic()
        }
        for row in downloaded {
            downloadManager.delete(row)
        }
        rebuildTracks()
        objectWillChange.send()
    }

    // MARK: - Transport label

    /// Dock title: first-run hint, playing names, or saved mix names.
    func transportDisplayName(loc: Localization) -> String {
        if !hasEverUsedSoundscape {
            return loc.t("soundscape.home_hint")
        }
        if isPlaying {
            let label = Self.mixLabel(
                ambience: nameForPlaying(kind: .ambience),
                music: nameForPlaying(kind: .music),
                separator: loc.t("soundscape.compact_separator")
            )
            return label.isEmpty ? loc.t("soundscape.home_hint") : label
        }
        let label = Self.mixLabel(
            ambience: nameForSelected(kind: .ambience),
            music: nameForSelected(kind: .music),
            separator: loc.t("soundscape.compact_separator")
        )
        return label.isEmpty ? loc.t("soundscape.home_hint") : label
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
        markSoundscapeUsed()
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
        markSoundscapeUsed()
        selectedMusicId = track.trackId
        persistMix()
        playingMusicId = track.trackId
        player.playMusic(source, volume: musicVolume)
        objectWillChange.send()
    }

    private func markSoundscapeUsed() {
        guard !hasEverUsedSoundscape else { return }
        hasEverUsedSoundscape = true
        if let data = try? JSONEncoder().encode(true) {
            store.set(data, forKey: hasUsedKey)
        }
    }

    private func nameForPlaying(kind: SoundscapeKind) -> String? {
        switch kind {
        case .ambience:
            guard let id = playingAmbienceId else { return nil }
            return ambienceTracks.first(where: { $0.trackId == id })?.name
        case .music:
            guard let id = playingMusicId else { return nil }
            return musicTracks.first(where: { $0.trackId == id })?.name
        }
    }

    private func nameForSelected(kind: SoundscapeKind) -> String? {
        switch kind {
        case .ambience:
            guard let id = selectedAmbienceId else { return nil }
            return ambienceTracks.first(where: { $0.trackId == id })?.name
        case .music:
            guard let id = selectedMusicId else { return nil }
            return musicTracks.first(where: { $0.trackId == id })?.name
        }
    }

    private static func mixLabel(ambience: String?, music: String?, separator: String) -> String {
        switch (ambience, music) {
        case let (a?, m?): return "\(a)\(separator)\(m)"
        case let (a?, nil): return a
        case let (nil, m?): return m
        case (nil, nil): return ""
        }
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
