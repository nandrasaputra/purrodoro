//
//  SoundscapePlayer.swift
//  Purrodoro
//
//  Dual looping AVPlayers (ambience + lo-fi). Mirrors Android's two
//  `SoundscapeLoopPlayer` instances. Pause/resume keeps position; switching
//  sources restarts from the beginning.
//

import Foundation
import AVFoundation
import Combine

@MainActor
final class SoundscapePlayer: ObservableObject {
    @Published private(set) var isAmbiencePlaying = false
    @Published private(set) var isMusicPlaying = false

    private var ambienceDeck = LoopDeck()
    private var musicDeck = LoopDeck()
    private var sessionConfigured = false

    var isPlaying: Bool { isAmbiencePlaying || isMusicPlaying }

    func playAmbience(_ source: AudioSource, volume: Float) {
        configureSessionIfNeeded()
        ambienceDeck.play(source: source, volume: volume)
        isAmbiencePlaying = ambienceDeck.isPlaying
    }

    func playMusic(_ source: AudioSource, volume: Float) {
        configureSessionIfNeeded()
        musicDeck.play(source: source, volume: volume)
        isMusicPlaying = musicDeck.isPlaying
    }

    func setAmbienceVolume(_ volume: Float) {
        ambienceDeck.setVolume(volume)
    }

    func setMusicVolume(_ volume: Float) {
        musicDeck.setVolume(volume)
    }

    func pauseAmbience() {
        ambienceDeck.pause()
        isAmbiencePlaying = false
    }

    func pauseMusic() {
        musicDeck.pause()
        isMusicPlaying = false
    }

    func pauseAll() {
        pauseAmbience()
        pauseMusic()
    }

    func release() {
        ambienceDeck.release()
        musicDeck.release()
        isAmbiencePlaying = false
        isMusicPlaying = false
    }

    private func configureSessionIfNeeded() {
        guard !sessionConfigured else { return }
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? session.setActive(true)
        sessionConfigured = true
    }
}

// MARK: - Loop deck

@MainActor
private final class LoopDeck {
    private var queuePlayer: AVQueuePlayer?
    private var looper: AVPlayerLooper?
    private var loadedSource: AudioSource?
    private(set) var isPlaying = false

    func play(source: AudioSource, volume: Float) {
        let clamped = max(0, min(1, volume))
        if loadedSource == source, let player = queuePlayer {
            player.volume = clamped
            if player.rate == 0 { player.play() }
            isPlaying = true
            return
        }
        tearDown()
        guard let url = Self.resolveURL(source) else { return }
        let item = AVPlayerItem(url: url)
        let player = AVQueuePlayer(playerItem: item)
        player.volume = clamped
        looper = AVPlayerLooper(player: player, templateItem: item)
        queuePlayer = player
        loadedSource = source
        player.play()
        isPlaying = true
    }

    func setVolume(_ volume: Float) {
        queuePlayer?.volume = max(0, min(1, volume))
    }

    func pause() {
        queuePlayer?.pause()
        isPlaying = false
    }

    func release() {
        tearDown()
    }

    private func tearDown() {
        queuePlayer?.pause()
        looper?.disableLooping()
        looper = nil
        queuePlayer?.removeAllItems()
        queuePlayer = nil
        loadedSource = nil
        isPlaying = false
    }

    private static func resolveURL(_ source: AudioSource) -> URL? {
        switch source {
        case .bundled(let name):
            return BundledTrackRegistry.bundleURL(forResource: name)
        case .localFile(let path):
            let url = URL(fileURLWithPath: path)
            return FileManager.default.fileExists(atPath: url.path) ? url : nil
        }
    }
}
