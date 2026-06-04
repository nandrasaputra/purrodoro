//
//  SoundsStore.swift
//  Purrodoro
//
//  Selection + playback state for the ambient focus sounds, backed by the
//  procedural `AmbientAudioPlayer`. Mirrors the Android sounds feature (a list
//  of options with a selected/playing state) but with real audio.
//

import Foundation
import SwiftUI
import Combine

struct SoundOption: Identifiable, Equatable {
    let kind: AmbientKind
    let nameKey: String
    let descKey: String
    let symbol: String
    let color: Color
    var locked: Bool = false

    var id: String { kind.rawValue }
}

@MainActor
final class SoundsStore: ObservableObject {

    static let options: [SoundOption] = [
        SoundOption(kind: .rain, nameKey: "sound.rain.name", descKey: "sound.rain.desc", symbol: "cloud.rain.fill", color: Brand.sky),
        SoundOption(kind: .cafe, nameKey: "sound.cafe.name", descKey: "sound.cafe.desc", symbol: "cup.and.saucer.fill", color: Brand.peach),
        SoundOption(kind: .forest, nameKey: "sound.forest.name", descKey: "sound.forest.desc", symbol: "tree.fill", color: Brand.mint),
        SoundOption(kind: .fire, nameKey: "sound.fire.name", descKey: "sound.fire.desc", symbol: "flame.fill", color: Brand.coral),
        SoundOption(kind: .waves, nameKey: "sound.waves.name", descKey: "sound.waves.desc", symbol: "water.waves", color: Brand.lavender),
    ]

    /// The currently sounding option, or nil when silent.
    @Published private(set) var playingKind: AmbientKind?

    private let player = AmbientAudioPlayer()
    private let store: UserDefaults
    private let selectedKey = "purrodoro.selectedSound"

    init(store: UserDefaults = .standard) {
        self.store = store
    }

    var isPlaying: Bool { playingKind != nil }

    /// Tap behaviour: tapping the playing sound stops it; tapping another
    /// switches to it.
    func toggle(_ kind: AmbientKind) {
        if playingKind == kind {
            stop()
        } else {
            playingKind = kind
            store.set(kind.rawValue, forKey: selectedKey)
            player.play(kind)
        }
    }

    func stop() {
        playingKind = nil
        player.stop()
    }
}
