package com.endiar.purrodoro.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray

/**
 * Transport boundary of the sync engine: bulk upsert of pushed rows and a
 * delta pull of rows changed after a checkpoint. Rows travel as raw
 * [JsonElement]s so the engine stays independent of the postgrest client.
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
                filter { gt("updated_at", afterIso) }
            }
            order("updated_at", Order.ASCENDING)
        }
        return Json.parseToJsonElement(result.data).jsonArray.toList()
    }
}
