package com.endiar.purrodoro.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration

/**
 * Background sync entry point. Runs the same gated pass as every other
 * trigger ([SyncModule.syncIfEligible]). A pass that left failed tables
 * behind reports [Result.retry] so WorkManager's exponential backoff drives
 * the re-run; an ineligible run or a fully clean pass reports success.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        when (SyncModule.get(applicationContext).syncIfEligible()) {
            is SyncEngine.SyncResult.PartialFailure -> Result.retry()
            else -> Result.success()
        }

    companion object {
        private const val UNIQUE_PERIODIC_WORK = "purrodoro_sync"
        private const val RETRY_WORK = "purrodoro_sync_retry"

        private val connected = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Schedules the 6-hourly safety-net sync. KEEP, so app relaunches
         * don't reset the existing cadence.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(Duration.ofHours(6))
                .setConstraints(connected)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * One-shot retry enqueued by the foreground (non-worker) path when a
         * sync pass left failed tables behind
         * ([SyncEngine.SyncResult.PartialFailure]): expedited when quota
         * allows, and because [doWork] returns [Result.retry] while tables
         * keep failing, the backoff criteria below genuinely re-run it.
         * KEEP, so a retry chain already backing off isn't reset.
         */
        fun enqueueRetry(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(connected)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(1))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                RETRY_WORK,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
