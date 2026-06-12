package com.endiar.purrodoro.sync

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.OffsetDateTime

/**
 * Per-table adapter between the local Room store and the sync wire format.
 * The [SyncEngine] drives one push/pull pass per table through this interface;
 * implementations own the entity/DTO mapping and the local conflict handling.
 */
interface TableSync {
    val table: String

    /** Server upsert conflict target; `user_id` for the user-keyed profiles table. */
    val conflictKey: String get() = "id"

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
 * Runs a block inside a single local-database transaction. Injected from
 * [SyncModule] (production: `db.withTransaction`) so the per-row
 * read-decide-write sequences below are atomic against concurrent user
 * mutations, and so tests can supply their in-memory database's runner.
 */
internal typealias TransactionRunner = suspend (suspend () -> Unit) -> Unit

/**
 * Shared [TableSync] skeleton: push-snapshot bookkeeping, [resolvePull]
 * dispatch, and wire-row field access. Subclasses plug in the DAO calls and
 * DTO mapping for one entity type.
 */
internal abstract class BaseTableSync<E : Any>(
    override val table: String,
    private val inTransaction: TransactionRunner,
) : TableSync {

    /** `locallyModifiedAt` per row id, captured by the latest [dirtyRows] call. */
    private var pushSnapshot: Map<String, Long> = emptyMap()

    protected abstract suspend fun dirtyEntities(): List<E>
    protected abstract suspend fun entityById(id: String): E?
    protected abstract suspend fun upsertEntities(entities: List<E>)

    /**
     * Single atomic UPDATE clearing the row's dirty flag iff its
     * `locallyModifiedAt` still equals [snapshotModifiedAt] — the
     * compare-and-clear must happen inside one SQL statement so a user edit
     * committing between the engine's read and write can never be silently
     * marked clean.
     */
    protected abstract suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long)
    protected abstract fun idOf(entity: E): String
    protected abstract fun modifiedAtOf(entity: E): Long
    protected abstract fun isDirty(entity: E): Boolean
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
        for (row in rows) {
            val id = rowId(row)
            val snapshotModifiedAt = pushSnapshot[id] ?: continue
            clearDirtyIfUnchanged(id, snapshotModifiedAt)
        }
    }

    final override suspend fun applyRemote(rows: List<JsonElement>) {
        for (row in rows) {
            // Each row's read-decide-write runs in one local transaction:
            // a user edit can never slip between the dirty check and the
            // write (and be clobbered with dirty = false), and natural-key
            // adoption's delete + re-insert can never be torn by process
            // death.
            inTransaction {
                applyRemoteRow(row)
            }
        }
    }

    private suspend fun applyRemoteRow(row: JsonElement) {
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

    final override suspend fun maxRemoteUpdatedAt(rows: List<JsonElement>): String? =
        maxUpdatedAtOf(rows)

    protected fun rowId(row: JsonElement): String =
        row.jsonObject.getValue("id").jsonPrimitive.content

    private fun rowDeleted(row: JsonElement): Boolean =
        row.jsonObject["deleted"]?.jsonPrimitive?.booleanOrNull ?: false
}

/** Max server `updated_at` among [rows] — what a table's checkpoint advances to. */
internal fun maxUpdatedAtOf(rows: List<JsonElement>): String? =
    rows.mapNotNull { (it.jsonObject["updated_at"] as? JsonPrimitive)?.contentOrNull }
        .maxWithOrNull(compareBy { OffsetDateTime.parse(it) })
