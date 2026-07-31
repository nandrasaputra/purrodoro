//
//  BundledTrackRegistry.swift
//  Purrodoro
//
//  Source of *existence* for bundled tracks (MP3s ship in the app). The
//  Supabase `soundscape_bundled_catalog` table only *overrides* visibility,
//  premium, order, and display — see `SoundscapeCatalogBuilder`.
//
//  Mirrors Android `BundledTrackRegistry.kt`. Bundle URLs resolve from the
//  `Soundscape/` resource subdirectory.
//

import Foundation

struct BundledTrackDefault: Equatable, Sendable {
    let kind: SoundscapeKind
    let localId: Int
    let rawResource: String
    let nameKey: String
    let subtitleKey: String
    let symbol: String
    let defaultPremium: Bool
    let sortOrder: Int
}

enum BundledTrackRegistry {
    static let defaults: [BundledTrackDefault] = [
        BundledTrackDefault(
            kind: .ambience, localId: 1, rawResource: "l_amb_rain",
            nameKey: "sound.rain.name", subtitleKey: "sound.rain.desc",
            symbol: "Rain", defaultPremium: false, sortOrder: 10
        ),
        BundledTrackDefault(
            kind: .ambience, localId: 2, rawResource: "l_amb_cafe",
            nameKey: "sound.cafe.name", subtitleKey: "sound.cafe.desc",
            symbol: "Cafe", defaultPremium: false, sortOrder: 20
        ),
        BundledTrackDefault(
            kind: .ambience, localId: 3, rawResource: "l_amb_fireplace",
            nameKey: "sound.fire.name", subtitleKey: "sound.fire.desc",
            symbol: "Fire", defaultPremium: false, sortOrder: 30
        ),
        BundledTrackDefault(
            kind: .ambience, localId: 4, rawResource: "l_amb_forest",
            nameKey: "sound.forest.name", subtitleKey: "sound.forest.desc",
            symbol: "Forest", defaultPremium: true, sortOrder: 40
        ),
        BundledTrackDefault(
            kind: .ambience, localId: 5, rawResource: "l_amb_sea_wave",
            nameKey: "sound.sea_wave.name", subtitleKey: "sound.sea_wave.desc",
            symbol: "Wave", defaultPremium: false, sortOrder: 50
        ),
        BundledTrackDefault(
            kind: .ambience, localId: 6, rawResource: "l_amb_winter_wind",
            nameKey: "sound.winter_wind.name", subtitleKey: "sound.winter_wind.desc",
            symbol: "Wind", defaultPremium: true, sortOrder: 60
        ),
        BundledTrackDefault(
            kind: .music, localId: 1, rawResource: "l_lofi_desk_lamp_rain",
            nameKey: "sound.lofi.desk_lamp_rain.name", subtitleKey: "sound.lofi.local_artist",
            symbol: "", defaultPremium: false, sortOrder: 10
        ),
        BundledTrackDefault(
            kind: .music, localId: 2, rawResource: "l_lofi_mosswood_morning",
            nameKey: "sound.lofi.mosswood_morning.name", subtitleKey: "sound.lofi.local_artist",
            symbol: "", defaultPremium: false, sortOrder: 20
        ),
        BundledTrackDefault(
            kind: .music, localId: 3, rawResource: "l_lofi_paper_skies",
            nameKey: "sound.lofi.paper_skies.name", subtitleKey: "sound.lofi.local_artist",
            symbol: "", defaultPremium: false, sortOrder: 30
        ),
        BundledTrackDefault(
            kind: .music, localId: 4, rawResource: "l_lofi_quiet_window_code",
            nameKey: "sound.lofi.quiet_window_code.name", subtitleKey: "sound.lofi.local_artist",
            symbol: "", defaultPremium: false, sortOrder: 40
        ),
        BundledTrackDefault(
            kind: .music, localId: 5, rawResource: "l_lofi_sore_di_pendopo",
            nameKey: "sound.lofi.sore_di_pendopo.name", subtitleKey: "sound.lofi.local_artist",
            symbol: "", defaultPremium: true, sortOrder: 50
        ),
        BundledTrackDefault(
            kind: .music, localId: 6, rawResource: "l_lofi_sudirman_rain_loop",
            nameKey: "sound.lofi.sudirman_rain_loop.name", subtitleKey: "sound.lofi.local_artist",
            symbol: "", defaultPremium: true, sortOrder: 60
        ),
    ]

    private static let knownRawNames: Set<String> = [
        "l_amb_rain", "l_amb_cafe", "l_amb_fireplace", "l_amb_forest",
        "l_amb_sea_wave", "l_amb_winter_wind",
        "l_lofi_desk_lamp_rain", "l_lofi_mosswood_morning", "l_lofi_paper_skies",
        "l_lofi_quiet_window_code", "l_lofi_sore_di_pendopo", "l_lofi_sudirman_rain_loop",
    ]

    /// Stale `soundscape_bundled_catalog.raw_resource` values from earlier seeds.
    private static let legacyRawAliases: [String: String] = [
        "ambience_rain": "l_amb_rain",
        "ambience_cafe": "l_amb_cafe",
        "ambience_fireplace": "l_amb_fireplace",
        "ambience_forest": "l_amb_forest",
        "ambience_sea_wave": "l_amb_sea_wave",
        "ambience_winter_wind": "l_amb_winter_wind",
        "l_rain": "l_amb_rain",
        "l_cafe": "l_amb_cafe",
        "l_fireplace": "l_amb_fireplace",
        "l_forest": "l_amb_forest",
        "l_sea_wave": "l_amb_sea_wave",
        "l_winter_wind": "l_amb_winter_wind",
        "lofi_bloom_yasumu": "l_lofi_desk_lamp_rain",
        "lofi_chill_fm_dope_cat": "l_lofi_mosswood_morning",
        "lofi_midnight_whisper_matrika": "l_lofi_paper_skies",
        "l_desk_lamp_rain": "l_lofi_desk_lamp_rain",
        "l_mosswood_morning": "l_lofi_mosswood_morning",
        "l_paper_skies": "l_lofi_paper_skies",
        "l_quiet_window_code": "l_lofi_quiet_window_code",
        "l_sore_di_pendopo": "l_lofi_sore_di_pendopo",
        "l_sudirman_rain_loop": "l_lofi_sudirman_rain_loop",
    ]

    /// Canonical raw resource name, or nil when unknown.
    static func resolveRawResource(_ requested: String?, fallback: String) -> String? {
        if let override = requested?.trimmingCharacters(in: .whitespacesAndNewlines), !override.isEmpty {
            if let resolved = canonicalize(override) { return resolved }
        }
        return canonicalize(fallback)
    }

    static func canonicalize(_ rawResource: String) -> String? {
        let key = rawResource.trimmingCharacters(in: .whitespacesAndNewlines)
        if knownRawNames.contains(key) { return key }
        if let alias = legacyRawAliases[key], knownRawNames.contains(alias) { return alias }
        return nil
    }

    static func bundleURL(forResource rawResource: String) -> URL? {
        guard let name = canonicalize(rawResource) else { return nil }
        return Bundle.main.url(
            forResource: name,
            withExtension: "mp3",
            subdirectory: "Soundscape"
        )
    }

    static func url(requested: String?, fallback: String) -> URL? {
        guard let name = resolveRawResource(requested, fallback: fallback) else { return nil }
        return bundleURL(forResource: name)
    }
}
