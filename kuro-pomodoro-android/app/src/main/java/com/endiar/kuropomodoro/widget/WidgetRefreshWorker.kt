package com.endiar.kuropomodoro.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalTime

/**
 * Periodically refreshes the home-screen widgets so a fresh day flips them back
 * into "nudge" mode even when the app hasn't been opened.
 *
 * Glance widgets don't tick on their own at midnight, so this worker is the
 * "new day" trigger the nudge experience relies on. We run roughly daily (with
 * flex) and also refresh on app launch — together that keeps the nudge current
 * without hammering the battery.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        WidgetUpdater.refreshAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "kuro_widget_daily_refresh"

        /**
         * Schedules the once-per-day refresh. Uses KEEP so re-launching the app
         * doesn't reset the existing schedule. The initial delay targets the next
         * early-morning window so the day-flip lands when users first check their
         * phone.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                repeatInterval = Duration.ofHours(24),
                flexTimeInterval = Duration.ofHours(3),
            )
                .setInitialDelay(initialDelayToMorning())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Delay until the next 06:00 local time, so the first run lands in the morning. */
        private fun initialDelayToMorning(): Duration {
            val now = LocalTime.now()
            val target = LocalTime.of(6, 0)
            val secondsUntil = if (now.isBefore(target)) {
                Duration.between(now, target).seconds
            } else {
                Duration.ofDays(1).seconds - Duration.between(target, now).seconds
            }
            return Duration.ofSeconds(secondsUntil)
        }
    }
}
