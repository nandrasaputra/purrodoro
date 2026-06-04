//
//  WidgetTheme.swift
//  PurrodoroWidgets
//
//  Minimal brand palette mirrored from the app's `PurrodoroTheme` so the
//  extension renders cohesively without depending on app-target code.
//

import SwiftUI

enum WidgetBrand {
    static func color(_ hex: UInt32) -> Color {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        return Color(.sRGB, red: r, green: g, blue: b, opacity: 1)
    }

    static let peach = color(0xFFB59E)
    static let coral = color(0xFF7F73)
    static let mint = color(0xCDEEDB)
    static let lavender = color(0xD8C8FF)
    static let cocoa = color(0x5F4638)

    /// Mode-keyed strong accent, matching `TimerMode.accentStrong`.
    static func accent(forMode raw: String) -> Color {
        switch raw {
        case "shortBreak": return color(0x6FBF96)
        case "longBreak": return color(0x9B7FCC)
        default: return coral
        }
    }

    static func mascot(forMode raw: String) -> String {
        switch raw {
        case "shortBreak": return "CatShortBreak"
        case "longBreak": return "CatLongBreak"
        default: return "CatFocusing"
        }
    }
}
