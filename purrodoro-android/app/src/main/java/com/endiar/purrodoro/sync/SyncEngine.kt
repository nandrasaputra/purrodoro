package com.endiar.purrodoro.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Per-table delta-pull checkpoint persistence (max applied server `updated_at`). */
interface CheckpointStore {
    suspend fun get(table: String): String?
    suspend fun set(table: String, iso: String)
}

/**
 * Delta push/pull sync engine. One [syncOnce] pass walks the [tables] in the
 * given order (callers must pass them FK-first: categories before tasks,
 * habits before habit_logs) and, per table:
 *
 *  1. pushes dirty local rows (upsert on id),
 *  2. clears their dirty flags ([TableSync.markPushed], concurrency-safe),
 *  3. pulls rows changed after the table's checkpoint,
 *  4. applies them under the [resolvePull] LWW rule, and
 *  5. advances the checkpoint to the max pulled `updated_at`.
 *
 * Failures are isolated per table: a failing table is recorded in
 * [SyncResult.PartialFailure] and its checkpoint stays put (so the next pass
 * re-pulls the same delta), while the remaining tables still sync. Throwables
 * matching [isFatal] abort the pass and propagate instead — the caller uses
 * this for auth failures, where every table would fail identically.
 *
 * Concurrent [syncOnce] calls collapse: while a pass is running, a second
 * caller just waits for it and returns its result instead of starting another
 * pass.
 */
class SyncEngine(
    private val api: SyncApi,
    private val tables: List<TableSync>,
    private val checkpoints: CheckpointStore,
    private val isFatal: (Throwable) -> Boolean = { false },
) {

    sealed class SyncResult {
        object Success : SyncResult()
        data class PartialFailure(val failedTables: List<String>) : SyncResult()
    }

    private val mutex = Mutex()

    @Volatile
    private var lastResult: SyncResult = SyncResult.Success

    suspend fun syncOnce(): SyncResult {
        if (!mutex.tryLock()) {
            // A pass is already in flight; wait for it and share its outcome.
            mutex.withLock { }
            return lastResult
        }
        try {
            val failed = mutableListOf<String>()
            for (tableSync in tables) {
                try {
                    syncTable(tableSync)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    if (isFatal(e)) throw e
                    failed += tableSync.table
                }
            }
            return (if (failed.isEmpty()) SyncResult.Success else SyncResult.PartialFailure(failed))
                .also { lastResult = it }
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun syncTable(tableSync: TableSync) {
        val dirty = tableSync.dirtyRows()
        if (dirty.isNotEmpty()) {
            api.upsert(tableSync.table, dirty)
            tableSync.markPushed(dirty)
        }
        val remote = api.pullSince(tableSync.table, checkpoints.get(tableSync.table))
        if (remote.isNotEmpty()) {
            tableSync.applyRemote(remote)
            tableSync.maxRemoteUpdatedAt(remote)?.let { checkpoints.set(tableSync.table, it) }
        }
    }
}
