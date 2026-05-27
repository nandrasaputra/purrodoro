package com.endiar.kuropomodoro.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll

/**
 * One-stop helper for refreshing every Kuro widget instance on the home screen.
 *
 * Called from places where state that the widgets surface changes — currently:
 *  - [com.endiar.kuropomodoro.timer.TimerService] every state mutation
 *  - the same service when the foreground cycle is torn down
 *  - the daily focus stamp increment path (via the service)
 *
 * Glance handles the actual `RemoteViews` push; we just trigger `updateAll`
 * for both widgets. The call is suspending so it must be invoked from a
 * coroutine (or wrapped in `runBlocking` from a non-suspend context).
 */
object WidgetUpdater {

    private const val TAG = "WidgetUpdater"

    suspend fun refreshAll(context: Context) {
        runCatching {
            QuickStartWidget().updateAll(context.applicationContext)
        }.onFailure { Log.w(TAG, "QuickStartWidget update failed", it) }

        runCatching {
            TodayProgressWidget().updateAll(context.applicationContext)
        }.onFailure { Log.w(TAG, "TodayProgressWidget update failed", it) }
    }
}
