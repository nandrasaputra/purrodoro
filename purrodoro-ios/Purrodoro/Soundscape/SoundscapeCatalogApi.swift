//
//  SoundscapeCatalogApi.swift
//  Purrodoro
//
//  Fetches `soundscape_tracks` + `soundscape_bundled_catalog` via Supabase.
//  Codable DTOs match Android field names / migrations.
//

import Foundation
import Supabase

// MARK: - DTOs

struct SoundscapeTrackDto: Codable, Sendable {
    let id: Int
    let kind: String
    let name: String
    let artist: String
    let symbol: String
    let filePath: String
    let thumbnailPath: String
    let fileSizeBytes: Int64
    let isPremium: Bool
    let sortOrder: Int
    let updatedAt: String

    enum CodingKeys: String, CodingKey {
        case id, kind, name, artist, symbol
        case filePath = "file_path"
        case thumbnailPath = "thumbnail_path"
        case fileSizeBytes = "file_size_bytes"
        case isPremium = "is_premium"
        case sortOrder = "sort_order"
        case updatedAt = "updated_at"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        kind = try c.decode(String.self, forKey: .kind)
        name = try c.decode(String.self, forKey: .name)
        artist = try c.decode(String.self, forKey: .artist)
        symbol = try c.decode(String.self, forKey: .symbol)
        filePath = try c.decode(String.self, forKey: .filePath)
        thumbnailPath = try c.decodeIfPresent(String.self, forKey: .thumbnailPath) ?? ""
        fileSizeBytes = try c.decode(Int64.self, forKey: .fileSizeBytes)
        isPremium = try c.decode(Bool.self, forKey: .isPremium)
        sortOrder = try c.decode(Int.self, forKey: .sortOrder)
        updatedAt = try c.decode(String.self, forKey: .updatedAt)
    }

    func toRow() -> RemoteCatalogRow? {
        guard id >= SoundscapeConfig.remoteTrackIdFloor else { return nil }
        guard let millis = SoundscapeDate.parseMillis(updatedAt) else { return nil }
        return RemoteCatalogRow(
            id: id,
            kind: kind,
            name: name,
            artist: artist,
            symbol: symbol,
            filePath: filePath,
            thumbnailPath: thumbnailPath,
            fileSizeBytes: fileSizeBytes,
            isPremium: isPremium,
            sortOrder: sortOrder,
            updatedAtMillis: millis
        )
    }
}

struct BundledCatalogDto: Codable, Sendable {
    let kind: String
    let localId: Int
    let name: String
    let artist: String
    let symbol: String
    let rawResource: String
    let isPremium: Bool
    let sortOrder: Int
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case kind, name, artist, symbol
        case localId = "local_id"
        case rawResource = "raw_resource"
        case isPremium = "is_premium"
        case sortOrder = "sort_order"
        case isActive = "is_active"
    }

    func toRow() -> BundledCatalogRow {
        BundledCatalogRow(
            kind: kind,
            localId: localId,
            name: name,
            artist: artist,
            symbol: symbol,
            rawResource: rawResource,
            isPremium: isPremium,
            sortOrder: sortOrder,
            isActive: isActive
        )
    }
}

// MARK: - API

protocol SoundscapeCatalogApi: Sendable {
    func fetchActiveTracks() async throws -> [SoundscapeTrackDto]
    func fetchBundledCatalog() async throws -> [BundledCatalogDto]
}

struct SupabaseSoundscapeCatalogApi: SoundscapeCatalogApi {
    let client: SupabaseClient

    func fetchActiveTracks() async throws -> [SoundscapeTrackDto] {
        try await client
            .from("soundscape_tracks")
            .select()
            .execute()
            .value
    }

    func fetchBundledCatalog() async throws -> [BundledCatalogDto] {
        try await client
            .from("soundscape_bundled_catalog")
            .select()
            .execute()
            .value
    }
}

enum SoundscapeDate {
    static func parseMillis(_ iso: String) -> Int64? {
        let date = ISO8601DateFormatter.soundscape.date(from: iso)
            ?? ISO8601DateFormatter.soundscapeNoFraction.date(from: iso)
        guard let date else { return nil }
        return Int64(date.timeIntervalSince1970 * 1000)
    }
}

private extension ISO8601DateFormatter {
    static let soundscape: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    static let soundscapeNoFraction: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()
}
