//
//  AmbientAudio.swift
//  Purrodoro
//
//  Procedural ambient sound generation. Rather than ship large looping audio
//  files, each "sound" is synthesised in real time from coloured noise shaped
//  by a one-pole filter (plus a crackle layer for the fireplace). This keeps the
//  app asset-free while still giving real, gapless, background-capable audio.
//
//  The DSP state lives in `NoiseRenderer`, which the audio render thread owns;
//  it's marked `@unchecked Sendable` because parameter writes from the main
//  thread are plain scalar stores (a momentary tear only nudges a filter cutoff,
//  which is inaudible).
//

import Foundation
import AVFoundation

enum AmbientKind: String, CaseIterable, Codable {
    case rain, cafe, forest, fire, waves
}

private enum NoiseColor {
    case white, pink, brown
}

private final class NoiseRenderer: @unchecked Sendable {
    // Target tone, updated from the main thread when the sound changes.
    var lowpassCutoff: Float = 0.2     // 0..1, fraction of a one-pole coefficient
    var amplitude: Float = 0.25
    var color: NoiseColor = .white
    var crackle: Float = 0             // 0 = off, >0 enables fireplace pops
    var swell: Float = 0               // 0 = off, >0 enables slow wave swell

    // Filter / generator state (audio thread only).
    private var lpL: Float = 0
    private var lpR: Float = 0
    private var brownL: Float = 0
    private var brownR: Float = 0
    private var pink = [Float](repeating: 0, count: 7)
    private var crackleEnv: Float = 0
    private var phase: Float = 0
    private var seed: UInt32 = 0x9E3779B9

    private func nextWhite() -> Float {
        // xorshift32 → [-1, 1)
        seed ^= seed << 13
        seed ^= seed >> 17
        seed ^= seed << 5
        return (Float(seed) / Float(UInt32.max)) * 2 - 1
    }

    private func nextColored() -> Float {
        let w = nextWhite()
        switch color {
        case .white:
            return w
        case .brown:
            brownL = (brownL + 0.02 * w) / 1.02
            return brownL * 3.5
        case .pink:
            pink[0] = 0.99886 * pink[0] + w * 0.0555179
            pink[1] = 0.99332 * pink[1] + w * 0.0750759
            pink[2] = 0.96900 * pink[2] + w * 0.1538520
            pink[3] = 0.86650 * pink[3] + w * 0.3104856
            pink[4] = 0.55000 * pink[4] + w * 0.5329522
            pink[5] = -0.7616 * pink[5] - w * 0.0168980
            let out = pink.reduce(0, +) + w * 0.5362
            pink[6] = w * 0.115926
            return out * 0.11
        }
    }

    func render(into buffers: UnsafeMutableAudioBufferListPointer, frames: Int, sampleRate: Float) {
        let cutoff = max(0.002, min(lowpassCutoff, 1))
        var amp = amplitude
        for frame in 0..<frames {
            if swell > 0 {
                phase += (2 * .pi * 0.05) / sampleRate   // ~0.05 Hz swell
                if phase > 2 * .pi { phase -= 2 * .pi }
                amp = amplitude * (0.6 + 0.4 * (0.5 + 0.5 * sin(phase)))
            }

            var sampleL = nextColored()
            lpL += cutoff * (sampleL - lpL)
            sampleL = lpL

            var sampleR = nextColored()
            lpR += cutoff * (sampleR - lpR)
            sampleR = lpR

            if crackle > 0 {
                if nextWhite() > (0.999 - crackle * 0.01) {
                    crackleEnv = 0.9
                }
                crackleEnv *= 0.6
                let pop = crackleEnv * nextWhite()
                sampleL += pop
                sampleR += pop * 0.8
            }

            let outL = max(-1, min(1, sampleL * amp))
            let outR = max(-1, min(1, sampleR * amp))
            for buffer in buffers {
                let data = buffer.mData!.assumingMemoryBound(to: Float.self)
                if buffer.mNumberChannels == 2 {
                    data[frame * 2] = outL
                    data[frame * 2 + 1] = outR
                } else {
                    data[frame] = (outL + outR) * 0.5
                }
            }
        }
    }
}

@MainActor
final class AmbientAudioPlayer {

    private let avEngine = AVAudioEngine()
    private let renderer = NoiseRenderer()
    private var sourceNode: AVAudioSourceNode?
    private var configured = false
    private(set) var isPlaying = false

    private func configureIfNeeded() {
        guard !configured else { return }
        configured = true

        let format = AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 2)!
        let renderer = self.renderer
        let node = AVAudioSourceNode(format: format) { _, _, frameCount, audioBufferList -> OSStatus in
            let buffers = UnsafeMutableAudioBufferListPointer(audioBufferList)
            renderer.render(into: buffers, frames: Int(frameCount), sampleRate: 44_100)
            return noErr
        }
        sourceNode = node
        avEngine.attach(node)
        avEngine.connect(node, to: avEngine.mainMixerNode, format: format)
        avEngine.mainMixerNode.outputVolume = 0.7
    }

    private func activateSession() {
        #if os(iOS)
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? session.setActive(true)
        #endif
    }

    func play(_ kind: AmbientKind) {
        configureIfNeeded()
        applyTone(for: kind)
        activateSession()
        if !avEngine.isRunning {
            do { try avEngine.start() } catch { return }
        }
        isPlaying = true
    }

    func stop() {
        guard isPlaying else { return }
        avEngine.pause()
        isPlaying = false
        #if os(iOS)
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
        #endif
    }

    private func applyTone(for kind: AmbientKind) {
        switch kind {
        case .rain:
            renderer.color = .white; renderer.lowpassCutoff = 0.45; renderer.amplitude = 0.22
            renderer.crackle = 0; renderer.swell = 0
        case .cafe:
            renderer.color = .brown; renderer.lowpassCutoff = 0.08; renderer.amplitude = 0.5
            renderer.crackle = 0; renderer.swell = 0
        case .forest:
            renderer.color = .pink; renderer.lowpassCutoff = 0.12; renderer.amplitude = 0.3
            renderer.crackle = 0; renderer.swell = 0.6
        case .fire:
            renderer.color = .brown; renderer.lowpassCutoff = 0.1; renderer.amplitude = 0.42
            renderer.crackle = 0.7; renderer.swell = 0
        case .waves:
            renderer.color = .pink; renderer.lowpassCutoff = 0.05; renderer.amplitude = 0.45
            renderer.crackle = 0; renderer.swell = 1
        }
    }
}
