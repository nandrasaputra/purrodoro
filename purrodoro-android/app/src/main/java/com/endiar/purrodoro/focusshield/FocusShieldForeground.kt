package com.endiar.purrodoro.focusshield

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

object FocusShieldForeground {

    fun currentPackage(context: Context, lookbackMs: Long = 15_000L): String? {
        if (!FocusShieldUsageAccess.isGranted(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val end = System.currentTimeMillis()
        val start = end - lookbackMs
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        var foreground: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                -> {
                    if (event.packageName != context.packageName) {
                        foreground = event.packageName
                    }
                }
            }
        }
        if (foreground != null) return foreground

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, end - 30_000L, end)
            ?: return null
        return stats
            .filter { it.packageName != context.packageName && it.lastTimeUsed > end - 30_000L }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }
}
