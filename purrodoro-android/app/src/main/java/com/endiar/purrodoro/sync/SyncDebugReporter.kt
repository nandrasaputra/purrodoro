package com.endiar.purrodoro.sync

/**
 * Optional debug-only sink for sync pass outcomes. The release build leaves
 * [instance] null; debug builds register a reporter that drives the sync debug
 * notification / activity.
 */
fun interface SyncDebugReporter {
    fun onSyncFinished(result: SyncEngine.SyncResult?, eligible: Boolean)

    companion object {
        @Volatile
        var instance: SyncDebugReporter? = null
    }
}
