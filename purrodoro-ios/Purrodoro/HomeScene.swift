//
//  HomeScene.swift
//  Purrodoro
//
//  Home hero backdrop themes — mirrors Android `HomeScene.kt`.
//  Day/night art switches from dark appearance or local time outside 06:00–17:59.
//

import Foundation

enum HomeScene: String, CaseIterable, Identifiable, Codable {
    case room
    case cafe
    case library
    case park

    var id: String { rawValue }
    var storageName: String { rawValue }

    var isPremiumOnly: Bool {
        switch self {
        case .room: return false
        case .cafe, .library, .park: return true
        }
    }

    var titleKey: String { "home.scene.\(rawValue).title" }
    var subtitleKey: String { "home.scene.\(rawValue).subtitle" }

    var dayImageName: String {
        switch self {
        case .room: return "CatHomeHeaderDay"
        case .cafe: return "CatHomeCafeDay"
        case .library: return "CatHomeLibraryDay"
        case .park: return "CatHomeParkDay"
        }
    }

    var nightImageName: String {
        switch self {
        case .room: return "CatHomeHeaderNight"
        case .cafe: return "CatHomeCafeNight"
        case .library: return "CatHomeLibraryNight"
        case .park: return "CatHomeParkNight"
        }
    }

    static func fromStorageName(_ name: String?) -> HomeScene {
        guard let name else { return .room }
        return HomeScene(rawValue: name.lowercased()) ?? .room
    }
}

/// Night art when the app is in dark mode or local time is outside 6:00–17:59.
func isHomeHeroNightTime(isDarkTheme: Bool, date: Date = Date(), calendar: Calendar = .current) -> Bool {
    if isDarkTheme { return true }
    let hour = calendar.component(.hour, from: date)
    return !(6...17).contains(hour)
}

func resolveHomeHeroImageName(
    scene: HomeScene,
    isDarkTheme: Bool,
    date: Date = Date(),
    calendar: Calendar = .current
) -> String {
    isHomeHeroNightTime(isDarkTheme: isDarkTheme, date: date, calendar: calendar)
        ? scene.nightImageName
        : scene.dayImageName
}

/// Free users always render Room; selection is kept for a future upgrade.
func effectiveHomeScene(selected: HomeScene, isPremium: Bool) -> HomeScene {
    if !isPremium && selected.isPremiumOnly { return .room }
    return selected
}

func canSelectHomeScene(_ scene: HomeScene, isPremium: Bool) -> Bool {
    !scene.isPremiumOnly || isPremium
}
