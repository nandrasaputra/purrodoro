//
//  SyncJSON.swift
//  Purrodoro
//
//  Wire-row helpers for sync. Mirrors Android JsonElement utilities in
//  `sync/TableSync.kt` and `SyncApi.kt`.
//

import Foundation
import Helpers

typealias SyncRow = [String: AnyJSON]

enum SyncJSON {
    /// Generous upper bound for server transaction start-to-commit lag.
    static let pullOverlapSeconds: TimeInterval = 10

    static func rowId(_ row: SyncRow) -> String {
        guard case .string(let id) = row["id"] else {
            preconditionFailure("sync row missing string id")
        }
        return id
    }

    static func rowDeleted(_ row: SyncRow) -> Bool {
        guard case .bool(let deleted) = row["deleted"] else { return false }
        return deleted
    }

    static func stringField(_ row: SyncRow, _ key: String) -> String? {
        guard case .string(let value) = row[key] else { return nil }
        return value
    }

    static func boolField(_ row: SyncRow, _ key: String) -> Bool? {
        guard case .bool(let value) = row[key] else { return nil }
        return value
    }

    static func pushPayload(from row: SyncRow) -> SyncRow {
        row.filter { $0.key != "updated_at" }
    }

    static func maxUpdatedAt(of rows: [SyncRow]) -> String? {
        var best: (date: Date, iso: String)?
        for row in rows {
            guard let iso = stringField(row, "updated_at") else { continue }
            guard let date = ISO8601DateFormatter.sync.date(from: iso)
                ?? ISO8601DateFormatter.syncNoFraction.date(from: iso) else { continue }
            if best == nil || date > best!.date {
                best = (date, iso)
            }
        }
        return best?.iso
    }

    static func withOverlap(iso: String) -> String {
        let formatter = ISO8601DateFormatter.sync
        let date = formatter.date(from: iso) ?? ISO8601DateFormatter.syncNoFraction.date(from: iso)
        guard let date else { return iso }
        return formatter.string(from: date.addingTimeInterval(-pullOverlapSeconds))
    }
}

private extension ISO8601DateFormatter {
    static let sync: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    static let syncNoFraction: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()
}
