//
//  PurrodoroTheme.swift
//  Purrodoro
//
//  Brand palette ported from the Android `Color.kt` (warm coral / cream /
//  cocoa). Exposed as a small theme namespace plus a per-mode accent so the
//  UI reads cohesively with the Android app.
//

import SwiftUI

extension Color {
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        self.init(.sRGB, red: r, green: g, blue: b, opacity: 1)
    }
}

enum Brand {
    // Light surfaces
    static let cream = Color(hex: 0xFFF8ED)
    static let warmCream = Color(hex: 0xFFEFD8)
    static let pageWhite = Color(hex: 0xFAF5F2)
    static let cardWhite = Color(hex: 0xFFFCF7)
    static let creamBorder = Color(hex: 0xEBDFCC)
    static let creamDivider = Color(hex: 0xEFE3D0)

    // Accents
    static let peach = Color(hex: 0xFFB59E)
    static let coral = Color(hex: 0xFF7F73)
    static let dockCoral = Color(hex: 0xFF8B6F)
    static let deepCoral = Color(hex: 0xC64432)
    static let blush = Color(hex: 0xFFD8DF)
    static let lavender = Color(hex: 0xD8C8FF)
    static let mint = Color(hex: 0xCDEEDB)
    static let sky = Color(hex: 0xCFEAF8)

    // Text
    static let cocoa = Color(hex: 0x5F4638)
    static let cocoaSoft = Color(hex: 0x8B6B5A)
    static let cocoaFaint = Color(hex: 0xA89486)

    // Dark scheme
    static let nightDeep = Color(hex: 0x0A0816)
    static let nightCard = Color(hex: 0x1A1428)
    static let nightBorder = Color(hex: 0x2B2440)
    static let nightMuted = Color(hex: 0xB8B0C8)
    static let nightFaint = Color(hex: 0x7D768B)
}

extension AppearanceMode {
    /// SwiftUI scheme to force, or `nil` to follow the system (Auto).
    var preferredColorScheme: ColorScheme? {
        switch self {
        case .auto: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }

    var symbol: String {
        switch self {
        case .auto: return "circle.lefthalf.filled"
        case .light: return "sun.max.fill"
        case .dark: return "moon.fill"
        }
    }
}

extension TimerMode {
    /// Soft fill used behind the timer ring / mode chip.
    var accent: Color {
        switch self {
        case .focus: return Brand.peach
        case .shortBreak: return Brand.mint
        case .longBreak: return Brand.lavender
        }
    }

    /// Stronger tone for progress strokes and emphasis.
    var accentStrong: Color {
        switch self {
        case .focus: return Brand.coral
        case .shortBreak: return Color(hex: 0x6FBF96)
        case .longBreak: return Color(hex: 0x9B7FCC)
        }
    }

    var mascotImageName: String {
        switch self {
        case .focus: return "CatFocusing"
        case .shortBreak: return "CatShortBreak"
        case .longBreak: return "CatLongBreak"
        }
    }
}

/// Scheme-aware colors resolved from the environment.
struct PurrodoroColors {
    let scheme: ColorScheme

    var page: Color { scheme == .dark ? Brand.nightDeep : Brand.pageWhite }
    var card: Color { scheme == .dark ? Brand.nightCard : Brand.cardWhite }
    var border: Color { scheme == .dark ? Brand.nightBorder : Brand.creamBorder }
    var textPrimary: Color { scheme == .dark ? .white : Brand.cocoa }
    var textSecondary: Color { scheme == .dark ? Brand.nightMuted : Brand.cocoaSoft }
    var textFaint: Color { scheme == .dark ? Brand.nightFaint : Brand.cocoaFaint }
}

private struct PurrodoroColorsKey: EnvironmentKey {
    static let defaultValue = PurrodoroColors(scheme: .light)
}

extension EnvironmentValues {
    var pColors: PurrodoroColors {
        get { self[PurrodoroColorsKey.self] }
        set { self[PurrodoroColorsKey.self] = newValue }
    }
}
