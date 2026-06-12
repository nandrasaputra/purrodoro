package com.endiar.purrodoro.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Per-table delta-pull checkpoint persistence (max applied server `updated_at`).
 *
 * Checkpoints are a low-water mark, not an exact cursor: `moddatetime` stamps
 * rows with their transaction-start time, so a slow server transaction can
 * commit a row whose `updated_at` is older than an already-advanced
 * checkpoint. [SupabaseSyncApi.pullSince] therefore queries with `gte` minus
 * a small overlap window instead of strictly-greater; re-pulling an
 * already-applied row is harmless because the apply path is idempotent
 * ([resolvePull] + upsert by id).
 */
interface CheckpointStore {
    suspend fun get(table: String): String?
    suspend fun set(table: String, iso: String)
}

/**
 * Delta sync engine. One [syncOnce] pass walks the [tables] in the given
 * order (callers must pass them FK-first: categories before tasks, habits
 * before habit_logs) and, per table, ALWAYS pulls before pushing:
 *
 *  1. pulls rows changed after the table's checkpoint,
 *  2. applies them under the [resolvePull] LWW rule (dirty local rows
 *     survive; natural-key collisions adopt the server identity),
 *  3. advances the checkpoint to the max pulled `updated_at`,
 *  4. pushes dirty local rows (upsert on the table's conflict key), and
 *  5. clears their dirty flags ([TableSync.markPushed], concurrency-safe).
 *
 * Pull-before-push is load-bearing, not an optimization: when two sides
 * independently mint "the same" row (a claim merge where both counted
 * session_records' local_id from 1, or two of one account's devices logging
 * the same habit+day), a push-first pass violates the server's unique
 * indexes — a 409 on EVERY retry, wedging the table permanently. Pulling
 * first lets adoption re-key the local row onto the server identity, so the
 * push that follows is an UPDATE. Because every pass orders this way, any
 * later pass (worker retry, periodic, foreground) heals a previously
 * interrupted merge too.
 *
 * The checkpoint advances before the push runs; if the push then fails, the
 * rows simply stay dirty and re-push on the next pass — nothing is lost to
 * the already-advanced checkpoint.
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

    /**
     * Outcome of the most recently finished pass, shared with callers that
     * collapsed onto it: success carries the [SyncResult], failure carries the
     * fatal throwable so waiters rethrow it instead of reading a stale
     * success. (A cancelled pass records nothing — its waiters see the
     * previous outcome, which is harmless since they trigger another pass.)
     */
    @Volatile
    private var lastOutcome: Result<SyncResult> = Result.success(SyncResult.Success)

    suspend fun syncOnce(): SyncResult {
        if (!mutex.tryLock()) {
            // A pass is already in flight; wait for it and share its outcome.
            // Safe because every pass is identical (same tables, same
            // pull-then-push order) — there is no special mode whose caller
            // could be handed the wrong kind of result.
            mutex.withLock { }
            return lastOutcome.getOrThrow()
        }
        try {
            val failed = mutableListOf<String>()
            for (tableSync in tables) {
                try {
                    syncTable(tableSync)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    if (isFatal(e)) {
                        lastOutcome = Result.failure(e)
                        throw e
                    }
                    failed += tableSync.table
                }
            }
            val result = if (failed.isEmpty()) SyncResult.Success else SyncResult.PartialFailure(failed)
            lastOutcome = Result.success(result)
            return result
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun syncTable(tableSync: TableSync) {
        pull(tableSync)
        push(tableSync)
    }

    private suspend fun push(tableSync: TableSync) {
        val dirty = tableSync.dirtyRows()
        if (dirty.isNotEmpty()) {
            api.upsert(tableSync.table, dirty, tableSync.conflictKey)
            tableSync.markPushed(dirty)
        }
    }

    private suspend fun pull(tableSync: TableSync) {
        val remote = api.pullSince(tableSync.table, checkpoints.get(tableSync.table))
        if (remote.isNotEmpty()) {
            tableSync.applyRemote(remote)
            tableSync.maxRemoteUpdatedAt(remote)?.let { checkpoints.set(tableSync.table, it) }
        }
    }
}
