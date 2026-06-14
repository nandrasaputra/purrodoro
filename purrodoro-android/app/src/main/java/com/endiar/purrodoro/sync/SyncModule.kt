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
        val ownerId = repo.currentOwnerId()
        if (!canSync(userId, ownerId)) {
            SyncDebugReporter.instance?.onSyncFinished(result = null, eligible = false)
            SyncStatusStore.record(eligible = false, result = null)
            return null
        }
        return try {
            engine.syncOnce().also { result ->
                SyncDebugReporter.instance?.onSyncFinished(result = result, eligible = true)
                SyncStatusStore.record(eligible = true, result = result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (shouldSignOut(t)) authRepository.signOut()
            SyncDebugReporter.instance?.onSyncFinished(result = null, eligible = true)
            SyncStatusStore.record(eligible = true, result = null)
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

    /**
     * Last flush before signing out: one sync pass, then [logoutDecision] over
     * whether it succeeded and whether dirty rows remain. Eligibility is
     * captured BEFORE the pass: a 401 inside the flush signs the user out,
     * and a post-hoc null result would then look like a harmless guest no-op
     * instead of the failed flush it was. A genuinely not-eligible flush
     * (guest / unclaimed) counts as succeeded — there is nothing a sync could
     * have uploaded for this user anyway.
     */
    suspend fun flushBeforeLogout(): LogoutDecision {
        val wasEligible = canSync(authRepository.currentAuthUser()?.userId, repo.currentOwnerId())
        val result = syncIfEligible()
        val flushSucceeded = !wasEligible || result is SyncEngine.SyncResult.Success
        return logoutDecision(flushSucceeded, dirtyRemaining = hasDirtyRows())
    }

    private suspend fun hasDirtyRows(): Boolean =
        db.taskDao().hasDirty() ||
            db.categoryDao().hasDirty() ||
            db.habitDao().hasDirty() ||
            db.habitLogDao().hasDirty() ||
            db.sessionRecordDao().hasDirty() ||
            db.dailyStampDao().hasDirty() ||
            db.profileDao().hasDirty()

    /** Whether any account-ownable row exists at all, tombstones included. */
    private suspend fun hasAnyLocalData(): Boolean =
        db.taskDao().hasAny() ||
            db.categoryDao().hasAny() ||
            db.habitDao().hasAny() ||
            db.habitLogDao().hasAny() ||
            db.sessionRecordDao().hasAny() ||
            db.dailyStampDao().hasAny() ||
            db.profileDao().hasAny()

    /**
     * Startup repair of crash remnants in the sign-in flow's ownership
     * windows (decision: [resolveStartupOwnership]). Most relevant: a crash
     * between a Drop's data clear and its owner re-tag would otherwise leave
     * a signed-in user whose sync is silently ineligible forever.
     */
    internal suspend fun reconcileOwnershipOnStartup() {
        reconcileStartupOwnership(
            userId = authRepository.currentAuthUser()?.userId,
            ownerId = repo.currentOwnerId(),
            hasLocalData = hasAnyLocalData(),
            setOwnerId = { repo.setOwnerId(it) },
            claimMerge = { syncHooks.claimMergeSync() },
            restoreFromCloud = { syncHooks.resetCheckpointsAndPull() },
            signOut = { authRepository.signOut() },
        )
    }

    /**
     * [AccountManager.SyncHooks] implementation: a Claim merges this device's
     * data into the account — everything marked dirty, checkpoints reset, and
     * one pass. Every pass pulls before pushing (see [SyncEngine]), so local
     * rows adopt server identities before the upload, and an interrupted
     * merge heals on whichever pass runs next. Keep is a plain delta pass.
     * A confirmed Drop re-pulls the new account's cloud from a blank slate.
     */
    val syncHooks: AccountManager.SyncHooks = object : AccountManager.SyncHooks {
        override suspend fun claimMergeSync() {
            repo.markAllAccountDataDirty(System.currentTimeMillis())
            // Pull everything the account already has, not just the delta —
            // adoption needs to see the server rows occupying local keys.
            repo.resetSyncCheckpoints()
            // A partial failure (offline sign-in, transient 5xx) must not
            // wait for the 6h periodic worker: hand it to a backed-off retry.
            if (syncIfEligible() is SyncEngine.SyncResult.PartialFailure) {
                SyncWorker.enqueueRetry(appContext)
            }
        }

        override suspend fun deltaSync() {
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
        // Repair any crash remnant of a Claim/Drop before the triggers below
        // start firing passes (they are gated by canSync, so a pass racing
        // ahead of the repair just no-ops).
        scope.launch { reconcileOwnershipOnStartup() }
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
            // habits before habit_logs). The independent profile row goes last.
            val tables = listOf(
                CategoryTableSync(db.categoryDao(), userId, inTransaction, allocator::nextCategoryId),
                TaskTableSync(db.taskDao(), userId, inTransaction, allocator::nextTaskId),
                HabitTableSync(db.habitDao(), userId, inTransaction, allocator::nextHabitId),
                HabitLogTableSync(db.habitLogDao(), userId, inTransaction),
                SessionRecordTableSync(db.sessionRecordDao(), userId, inTransaction),
                DailyStampTableSync(db.dailyStampDao(), userId, inTransaction),
                ProfileTableSync(db.profileDao(), userId, inTransaction, repo::applySyncedProfilePrefs),
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
