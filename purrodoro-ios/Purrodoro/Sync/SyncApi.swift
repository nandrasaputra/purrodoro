//
//  SyncApi.swift
//  Purrodoro
//
//  PostgREST transport for sync. Mirrors Android `sync/SyncApi.kt`.
//

import Foundation
import Supabase
import Helpers

protocol SyncApi: Sendable {
    func upsert(table: String, rows: [SyncRow], conflictKey: String) async throws
    func pullSince(table: String, afterIso: String?) async throws -> [SyncRow]
}

struct SupabaseSyncApi: SyncApi {
    let client: SupabaseClient

    func upsert(table: String, rows: [SyncRow], conflictKey: String) async throws {
        guard !rows.isEmpty else { return }
        let payload = rows.map(SyncJSON.pushPayload)
        try await client
            .from(table)
            .upsert(payload, onConflict: conflictKey)
            .execute()
    }

    func pullSince(table: String, afterIso: String?) async throws -> [SyncRow] {
        var query = client.from(table).select()
        if let afterIso {
            query = query.gte("updated_at", value: SyncJSON.withOverlap(iso: afterIso))
        }
        let rows: [SyncRow] = try await query
            .order("updated_at", ascending: true)
            .execute()
            .value
        return rows
    }
}

enum SyncAuthError {
    /// True when the session is dead — same reaction as Android `shouldSignOut`.
    static func shouldSignOut(_ error: Error) -> Bool {
        if let postgrest = error as? PostgrestError, postgrest.code == "401" { return true }
        return false
    }
}

func canSync(userId: String?, ownerId: String?) -> Bool {
    guard let userId, let ownerId else { return false }
    return userId == ownerId
}
