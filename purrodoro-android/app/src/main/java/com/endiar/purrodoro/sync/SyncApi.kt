package com.endiar.purrodoro.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Transport boundary of the sync engine: bulk upsert of pushed rows and a
 * delta pull of rows changed after a checkpoint. Rows travel as raw
 * [JsonElement]s so the engine stays independent of the postgrest client.
 *
 * Accepted limitation (M4): [pullSince] fetches the whole delta in one
 * response, with no paging — fine for this app's per-user data sizes, where
 * even a from-scratch pull is a few thousand small rows.
 */
interface SyncApi {
    suspend fun upsert(table: String, rows: List<JsonElement>, conflictKey: String = "id")
    suspend fun pullSince(table: String, afterIso: String?): List<JsonElement>
}

/**
 * [SyncApi] over supabase-kt postgrest. Auth is implicit: the Postgrest plugin
 * attaches the current session's access token, and RLS scopes every query to
 * the signed-in user's rows.
 */
class SupabaseSyncApi(private val client: SupabaseClient) : SyncApi {

    override suspend fun upsert(table: String, rows: List<JsonElement>, conflictKey: String) {
        if (rows.isEmpty()) return
        client.postgrest[table].upsert(JsonArray(rows)) {
            onConflict = conflictKey
        }
    }

    override suspend fun pullSince(table: String, afterIso: String?): List<JsonElement> {
        val result = client.postgrest[table].select {
            if (afterIso != null) {
                // gte minus an overlap window, NOT strictly-greater: the
                // server's moddatetime stamps transaction-start time, so a
                // row from an in-flight transaction can commit with an
                // updated_at at or before an already-advanced checkpoint.
                // Re-pulled rows no-op locally (see CheckpointStore docs).
                filter { gte("updated_at", withOverlap(afterIso)) }
            }
            order("updated_at", Order.ASCENDING)
        }
        return Json.parseToJsonElement(result.data).jsonArray.toList()
    }

    companion object {
        /** Generous upper bound for a server transaction's start-to-commit lag. */
        private val PULL_OVERLAP: Duration = Duration.ofSeconds(10)

        internal fun withOverlap(iso: String): String =
            runCatching { OffsetDateTime.parse(iso).minus(PULL_OVERLAP).toString() }
                .getOrDefault(iso)
    }
}
