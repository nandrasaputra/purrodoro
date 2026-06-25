package com.endiar.purrodoro.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build

/**
 * Schedules segment-end alarms for [TimerService].
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] when [AlarmManager.canScheduleExactAlarms]
 * allows it (requires `SCHEDULE_EXACT_ALARM`, user-granted on API 31+). Otherwise falls
 * back to inexact [AlarmManager.setAndAllowWhileIdle] — the foreground tick loop remains
 * the primary completion path; the alarm is a backstop when the process is killed.
 *
 * Purrodoro must not declare `USE_EXACT_ALARM` (Play policy: calendar / alarm-clock apps only).
 */
internal object ExactAlarmScheduler {

    fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    fun scheduleSegmentEnd(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        operation: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }
}
