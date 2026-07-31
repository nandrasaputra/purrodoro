//
//  SoundscapeConfig.swift
//  Purrodoro
//
//  Cloud soundscape audio is served from a public R2 bucket. Catalog metadata
//  still lives in Supabase Postgres (`soundscape_tracks`); only the MP3 bytes
//  are on R2. Mirrors Android `SoundscapeConfig.kt`.
//

import Foundation

enum SoundscapeConfig {
    static let r2PublicURL = "https://pub-a2af288fd41b4193b1cd198233eb484d.r2.dev"

    /// Cloud tracks must use ids >= this; below is reserved for bundled tracks.
    static let remoteTrackIdFloor = 100

    /// R2 object key for a track thumbnail, falling back to the audio stem + .webp.
    static func thumbnailKey(thumbnailPath: String, filePath: String = "") -> String {
        let fromCatalog = thumbnailPath.trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if !fromCatalog.isEmpty { return fromCatalog }
        let audio = filePath.trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !audio.isEmpty else { return "" }
        if let range = audio.range(of: #"\.mp3$"#, options: [.regularExpression, .caseInsensitive]) {
            return audio.replacingCharacters(in: range, with: ".webp")
        }
        return audio
    }

    static func thumbnailURL(thumbnailPath: String, filePath: String = "") -> URL? {
        let key = thumbnailKey(thumbnailPath: thumbnailPath, filePath: filePath)
        guard !key.isEmpty else { return nil }
        let base = r2PublicURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return URL(string: "\(base)/\(key)")
    }

    static func audioURL(filePath: String) -> URL? {
        let base = r2PublicURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let path = filePath.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !path.isEmpty else { return nil }
        return URL(string: "\(base)/\(path)")
    }
}
