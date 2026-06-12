package com.endiar.purrodoro.sync

import android.content.Context
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.auth.SupabaseModule
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import kotlinx.coroutines.CancellationException

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
 */
fun shouldSignOut(t: Throwable): Boolean = t is UnauthorizedRestException

/**
 * Composition root of the sync feature: builds the [SyncEngine] over the
 * shared Supabase client, Room DAOs, and AppRepository-backed checkpoint
 * store, and gates every run behind [canSync].
 */
class SyncModule private constructor(
    private val repo: AppRepository,
    private val authRepository: AuthRepository,
    private val engine: SyncEngine,
) {

    /**
     * Runs one sync pass if the signed-in user owns the local data; otherwise
     * does nothing. All failures are absorbed here: per-table errors are
     * already isolated by the engine, and a dead session signs the user out.
     */
    suspend fun syncIfEligible() {
        val userId = authRepository.currentAuthUser()?.userId
        if (!canSync(userId, repo.currentOwnerId())) return
        try {
            engine.syncOnce()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (shouldSignOut(t)) authRepository.signOut()
        }
    }

    companion object {
        @Volatile private var INSTANCE: SyncModule? = null

        fun get(context: Context): SyncModule =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
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
            // FK order: parents before children (categories before tasks,
            // habits before habit_logs).
            val tables = listOf(
                CategoryTableSync(db.categoryDao(), userId, allocator::nextCategoryId),
                TaskTableSync(db.taskDao(), userId, allocator::nextTaskId),
                HabitTableSync(db.habitDao(), userId, allocator::nextHabitId),
                HabitLogTableSync(db.habitLogDao(), userId),
                SessionRecordTableSync(db.sessionRecordDao(), userId),
                DailyStampTableSync(db.dailyStampDao(), userId),
            )
            val engine = SyncEngine(
                api = api,
                tables = tables,
                checkpoints = RepositoryCheckpointStore(repo),
                isFatal = ::shouldSignOut,
            )
            return SyncModule(repo, authRepository, engine)
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
