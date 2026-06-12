package com.endiar.purrodoro.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.withTransaction
import com.endiar.purrodoro.auth.AccountManager
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.auth.SupabaseModule
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Sync is eligible only when someone is signed in AND the on-device data is
 * marked as theirs — never for guests, an unclaimed device, or while the local
 * data still belongs to a different account (the Claim/Keep/Drop decision of
 * the sign-in flow must run first).
 */
fun canSync(userId: String?, ownerId: String?): Boolean =
    userId != null && userId == ownerId

/**
 * Whether a sync failure means the Supabase session is dead (the server
 * rejected our token), in which case the only sane reaction is to sign out:
 * local data and the owner marker stay, so a later re-login resolves to Keep
 * and resumes syncing. Transient failures (network, server errors) must NOT
 * sign the user out.
 *
 * Accepted limitation (M3): no explicit token-refresh attempt is made before
 * signing out — the auth client refreshes sessions on its own schedule, so a
 * 401 reaching this layer is treated as a genuinely dead session.
 */
fun shouldSignOut(t: Throwable): Boolean = t is UnauthorizedRestException

/**
 * Composition root of the sync feature: builds the [SyncEngine] over the
 * shared Supabase client, Room DAOs, and AppRepository-backed checkpoint
 * store, gates every run behind [canSync], and owns the automatic triggers —
 * debounced local writes, app foreground, and the periodic [SyncWorker].
 */
class SyncModule private constructor(
    private val appContext: Context,
    private val repo: AppRepository,
    private val authRepository: AuthRepository,
    private val db: PurrodoroDatabase,
    private val engine: SyncEngine,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val scheduler = SyncScheduler(
        scope = scope,
        debounceMs = WRITE_DEBOUNCE_MS,
        sync = { syncForeground() },
    )

    /**
     * Runs one sync pass if the signed-in user owns the local data; otherwise
     * does nothing and returns null. All failures are absorbed here: per-table
     * errors are already isolated by the engine (and surface as
     * [SyncEngine.SyncResult.PartialFailure] for the caller to react to), and
     * a dead session signs the user out.
     */
    suspend fun syncIfEligible(): SyncEngine.SyncResult? {
        val userId = authRepository.currentAuthUser()?.userId
        if (!canSync(userId, repo.currentOwnerId())) return null
        return try {
            engine.syncOnce()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (shouldSignOut(t)) authRepository.signOut()
            null
        }
    }

    /**
     * Foreground-trigger pass (debounced writes, app start, post-sign-in):
     * a partial failure hands the leftovers to a backed-off [SyncWorker]
     * retry. The worker itself does NOT use this path — it reports
     * [SyncEngine.SyncResult.PartialFailure] as `Result.retry()` and lets
     * WorkManager's backoff drive the re-run.
     */
    private suspend fun syncForeground() {
        if (syncIfEligible() is SyncEngine.SyncResult.PartialFailure) {
            SyncWorker.enqueueRetry(appContext)
        }
    }

    /** Post-sign-in sync, called by the account flow once ownership is settled (Part 4). */
    suspend fun onSignedInSync() = syncForeground()

    /**
     * Last flush before signing out: one sync pass, then [logoutDecision] over
     * whether it succeeded and whether dirty rows remain. A `null`
     * (not-eligible) pass counts as succeeded — there is nothing a sync could
     * have uploaded for this user anyway.
     */
    suspend fun flushBeforeLogout(): LogoutDecision {
        val result = syncIfEligible()
        val flushSucceeded = result == null || result is SyncEngine.SyncResult.Success
        return logoutDecision(flushSucceeded, dirtyRemaining = hasDirtyRows())
    }

    private suspend fun hasDirtyRows(): Boolean =
        db.taskDao().hasDirty() ||
            db.categoryDao().hasDirty() ||
            db.habitDao().hasDirty() ||
            db.habitLogDao().hasDirty() ||
            db.sessionRecordDao().hasDirty() ||
            db.dailyStampDao().hasDirty()

    /**
     * [AccountManager.SyncHooks] implementation: Claim/Keep re-uploads
     * everything by marking every live row dirty before a pass; a confirmed
     * Drop re-pulls the new account's cloud from a blank checkpoint slate.
     */
    val syncHooks: AccountManager.SyncHooks = object : AccountManager.SyncHooks {
        override suspend fun markAllDirtyAndSync() {
            val now = System.currentTimeMillis()
            db.withTransaction {
                db.taskDao().markAllDirty(now)
                db.categoryDao().markAllDirty(now)
                db.habitDao().markAllDirty(now)
                db.habitLogDao().markAllDirty(now)
                db.sessionRecordDao().markAllDirty(now)
                db.dailyStampDao().markAllDirty(now)
            }
            // The singleton profile row joins this re-upload once profile
            // sync lands (ensure-exists + dirty), see ProfileTableSync.
            syncIfEligible()
        }

        override suspend fun resetCheckpointsAndPull() {
            // Defensive: clearAccountOwnedData already dropped the checkpoints,
            // but the pull-from-scratch contract is this hook's to guarantee.
            repo.resetSyncCheckpoints()
            syncIfEligible()
        }
    }

    private fun init() {
        // Local writes -> debounced sync.
        scope.launch {
            repo.localWrites.collect { scheduler.onLocalWrite() }
        }
        // App foreground -> immediate sync. Observers must be registered on
        // the main thread, and get() may run on a worker thread (SyncWorker).
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    scheduler.requestImmediate()
                }
            })
        }
        // Safety net while the app is closed.
        SyncWorker.schedule(appContext)
    }

    companion object {
        private const val WRITE_DEBOUNCE_MS = 5_000L
        @Volatile private var INSTANCE: SyncModule? = null

        fun get(context: Context): SyncModule =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext)
                    .also {
                        INSTANCE = it
                        it.init()
                    }
            }

        private fun build(appContext: Context): SyncModule {
            val repo = AppRepository.get(appContext)
            val authRepository = AuthRepository.get(appContext)
            val db = PurrodoroDatabase.get(appContext)
            val api = SupabaseSyncApi(SupabaseModule.get(appContext))
            val userId: suspend () -> String = {
                checkNotNull(authRepository.currentAuthUser()?.userId) {
                    "Sync ran without a signed-in user"
                }
            }
            val allocator = RepositoryLegacyIdAllocator(repo)
            val inTransaction: TransactionRunner = { block -> db.withTransaction { block() } }
            // FK order: parents before children (categories before tasks,
            // habits before habit_logs).
            val tables = listOf(
                CategoryTableSync(db.categoryDao(), userId, inTransaction, allocator::nextCategoryId),
                TaskTableSync(db.taskDao(), userId, inTransaction, allocator::nextTaskId),
                HabitTableSync(db.habitDao(), userId, inTransaction, allocator::nextHabitId),
                HabitLogTableSync(db.habitLogDao(), userId, inTransaction),
                SessionRecordTableSync(db.sessionRecordDao(), userId, inTransaction),
                DailyStampTableSync(db.dailyStampDao(), userId, inTransaction),
            )
            val engine = SyncEngine(
                api = api,
                tables = tables,
                checkpoints = RepositoryCheckpointStore(repo),
                isFatal = ::shouldSignOut,
            )
            return SyncModule(appContext, repo, authRepository, db, engine)
        }
    }
}

/** [LegacyIdAllocator] over the AppRepository DataStore counters. */
internal class RepositoryLegacyIdAllocator(private val repo: AppRepository) : LegacyIdAllocator {
    override suspend fun nextTaskId(): Int = repo.allocateTaskLegacyId()
    override suspend fun nextCategoryId(): Int = repo.allocateCategoryLegacyId()
    override suspend fun nextHabitId(): Int = repo.allocateHabitLegacyId()
}

/** [CheckpointStore] over the AppRepository `sync_checkpoint_*` preferences. */
internal class RepositoryCheckpointStore(private val repo: AppRepository) : CheckpointStore {
    override suspend fun get(table: String): String? = repo.syncCheckpoint(table)
    override suspend fun set(table: String, iso: String) {
        repo.setSyncCheckpoint(table, iso)
    }
}
