package com.endiar.purrodoro.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One-stop helper for refreshing every Purrodoro widget instance on the home screen.
 *
 * Called from places where state that the widgets surface changes — currently:
 *  - [com.endiar.purrodoro.sync.SyncModule] after task/habit/stamp writes ([scheduleRefresh])
 *    and when the app moves to the background ([refreshAll])
 *  - [com.endiar.purrodoro.MainActivity] on launch (+ [WidgetRefreshWorker] daily)
 *  - [com.endiar.purrodoro.timer.TimerService] every timer state mutation
 *
 * Glance handles the actual `RemoteViews` push; we just trigger `updateAll` for all
 * seven registered widgets. The call is suspending so it must be invoked from a
 * coroutine (or wrapped in `runBlocking` from a non-suspend context).
 */
object WidgetUpdater {

    private const val TAG = "WidgetUpdater"
    private const val COALESCE_MS = 500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingRefresh: Job? = null

    private val widgets: List<Pair<String, GlanceAppWidget>> = listOf(
        "TodayProgressWidget" to TodayProgressWidget(),
        "NudgeDashboardWidget" to NudgeDashboardWidget(),
        "DailyPlanWidget" to DailyPlanWidget(),
        "StreakHeroWidget" to StreakHeroWidget(),
        "CalmMinimalWidget" to CalmMinimalWidget(),
        "StatsGridWidget" to StatsGridWidget(),
        "CozySceneWidget" to CozySceneWidget(),
    )

    /**
     * Coalesces bursty writes (e.g. several task edits in a row) into one refresh.
     * Cancelled when [refreshAll] runs so a background refresh does not double-fire.
     */
    fun scheduleRefresh(context: Context) {
        val app = context.applicationContext
        pendingRefresh?.cancel()
        pendingRefresh = scope.launch {
            delay(COALESCE_MS)
            pendingRefresh = null
            pushUpdates(app)
        }
    }

    suspend fun refreshAll(context: Context) {
        pendingRefresh?.cancel()
        pendingRefresh = null
        pushUpdates(context.applicationContext)
    }

    private suspend fun pushUpdates(app: Context) {
        widgets.forEach { (name, widget) ->
            runCatching { widget.updateAll(app) }
                .onFailure { Log.w(TAG, "$name update failed", it) }
        }
    }
}
