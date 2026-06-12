package com.endiar.purrodoro.sync

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Per-table adapter between the local Room store and the sync wire format.
 * The [SyncEngine] drives one push/pull pass per table through this interface;
 * implementations own the entity/DTO mapping and the local conflict handling.
 */
interface TableSync {
    val table: String

    /** Local rows awaiting push, encoded as wire rows (tombstones included). */
    suspend fun dirtyRows(): List<JsonElement>

    /**
     * Marks [rows] (the payload returned by the last [dirtyRows] call) as
     * pushed. A row's dirty flag is cleared only if its `locallyModifiedAt`
     * is unchanged since the [dirtyRows] snapshot — a concurrent user edit
     * stays dirty and re-pushes on the next pass.
     */
    suspend fun markPushed(rows: List<JsonElement>)

    /**
     * Applies pulled remote rows, deciding per row via [resolvePull]; a remote
     * `deleted = true` lands locally as a clean soft-delete tombstone.
     */
    suspend fun applyRemote(rows: List<JsonElement>)

    /** Max server `updated_at` among [rows]; the checkpoint advances to it. */
    suspend fun maxRemoteUpdatedAt(rows: List<JsonElement>): String?
}

/**
 * Shared [TableSync] skeleton: push-snapshot bookkeeping, [resolvePull]
 * dispatch, and wire-row field access. Subclasses plug in the DAO calls and
 * DTO mapping for one entity type.
 */
internal abstract class BaseTableSync<E : Any>(override val table: String) : TableSync {

    /** `locallyModifiedAt` per row id, captured by the latest [dirtyRows] call. */
    private var pushSnapshot: Map<String, Long> = emptyMap()

    protected abstract suspend fun dirtyEntities(): List<E>
    protected abstract suspend fun entityById(id: String): E?
    protected abstract suspend fun upsertEntities(entities: List<E>)
    protected abstract fun idOf(entity: E): String
    protected abstract fun modifiedAtOf(entity: E): Long
    protected abstract fun isDirty(entity: E): Boolean
    protected abstract fun clearDirty(entity: E): E
    protected abstract suspend fun encodeForPush(entity: E): JsonElement
    protected abstract suspend fun decodeRemote(row: JsonElement, existing: E?): E

    /**
     * For tables with a natural unique key besides the UUID (habit logs,
     * daily stamps): the local row occupying the incoming row's natural key,
     * so two devices independently creating "the same" row converge on the
     * server identity instead of wedging on the unique index.
     */
    protected open suspend fun entityByNaturalKey(row: JsonElement): E? = null

    /** Hard-deletes one local row; only required when [entityByNaturalKey] is overridden. */
    protected open suspend fun deleteEntity(entity: E) {
        error("$table does not support natural-key adoption")
    }

    /** Re-keys a local row onto a new id; only required when [entityByNaturalKey] is overridden. */
    protected open fun withId(entity: E, newId: String): E {
        error("$table does not support natural-key adoption")
    }

    final override suspend fun dirtyRows(): List<JsonElement> {
        val dirty = dirtyEntities()
        pushSnapshot = dirty.associate { idOf(it) to modifiedAtOf(it) }
        return dirty.map { encodeForPush(it) }
    }

    final override suspend fun markPushed(rows: List<JsonElement>) {
        val cleared = rows.mapNotNull { row ->
            val id = rowId(row)
            val current = entityById(id) ?: return@mapNotNull null
            val unchanged = modifiedAtOf(current) == pushSnapshot[id]
            if (unchanged && isDirty(current)) clearDirty(current) else null
        }
        if (cleared.isNotEmpty()) upsertEntities(cleared)
    }

    final override suspend fun applyRemote(rows: List<JsonElement>) {
        for (row in rows) {
            val id = rowId(row)
            var existing = entityById(id)
            // A local row under a different id but the same natural key is the
            // same logical row created independently elsewhere; adopt the
            // server id so neither side's unique constraints can clash.
            val adopted = if (existing == null) {
                entityByNaturalKey(row)?.takeIf { idOf(it) != id }?.also { existing = it }
            } else null
            when (resolvePull(existing?.let { isDirty(it) }, rowDeleted(row))) {
                PullAction.KeepLocal -> if (adopted != null) {
                    // Keep the dirty local content, but re-key it onto the
                    // server id so its next push updates the server row.
                    deleteEntity(adopted)
                    upsertEntities(listOf(withId(adopted, id)))
                }
                PullAction.Apply -> {
                    if (adopted != null) deleteEntity(adopted)
                    upsertEntities(listOf(decodeRemote(row, existing)))
                }
            }
        }
    }

    final override suspend fun maxRemoteUpdatedAt(rows: List<JsonElement>): String? =
        rows.mapNotNull { (it.jsonObject["updated_at"] as? JsonPrimitive)?.contentOrNull }
            .maxOrNull()

    protected fun rowId(row: JsonElement): String =
        row.jsonObject.getValue("id").jsonPrimitive.content

    private fun rowDeleted(row: JsonElement): Boolean =
        row.jsonObject["deleted"]?.jsonPrimitive?.booleanOrNull ?: false
}
