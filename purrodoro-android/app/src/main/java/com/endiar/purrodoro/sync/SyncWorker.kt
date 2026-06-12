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
 * trigger ([SyncModule.syncIfEligible]) and always reports success: per-table
 * failures are isolated inside the engine, and an ineligible run is a no-op,
 * so there is nothing WorkManager-level retries could fix that the next
 * scheduled pass won't.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        SyncModule.get(applicationContext).syncIfEligible()
        return Result.success()
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
         * One-shot retry used when a foreground sync pass left failed tables
         * behind ([SyncEngine.SyncResult.PartialFailure]): expedited when
         * quota allows, with exponential backoff while the failure persists.
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
