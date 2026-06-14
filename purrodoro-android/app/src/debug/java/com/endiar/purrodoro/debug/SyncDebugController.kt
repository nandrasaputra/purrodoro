package com.endiar.purrodoro.debug

import android.content.Context
import com.endiar.purrodoro.sync.SyncDebugReporter
import com.endiar.purrodoro.sync.SyncEngine
import com.endiar.purrodoro.sync.resolveSyncPassDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SyncDebugEvent(
    val timestampMillis: Long,
    val eligible: Boolean,
    val result: SyncEngine.SyncResult?,
    val detail: String,
)

/**
 * Debug-only sink registered on [SyncDebugReporter] when the drawer toggle is on.
 * Keeps a short in-memory log and refreshes the sync debug notification.
 */
object SyncDebugController : SyncDebugReporter {

    private val _events = MutableStateFlow<List<SyncDebugEvent>>(emptyList())
    val events: StateFlow<List<SyncDebugEvent>> = _events.asStateFlow()

    private var appContext: Context? = null
    private var enabled = false

    fun setEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        this.enabled = enabled
        if (enabled) {
            SyncDebugReporter.instance = this
            postSyncDebugNotification(context, latestSummary())
        } else {
            if (SyncDebugReporter.instance === this) {
                SyncDebugReporter.instance = null
            }
            cancelSyncDebugNotification(context)
        }
    }

    override fun onSyncFinished(result: SyncEngine.SyncResult?, eligible: Boolean) {
        if (!enabled) return
        val detail = resolveSyncPassDetail(eligible, result)
        val event = SyncDebugEvent(
            timestampMillis = System.currentTimeMillis(),
            eligible = eligible,
            result = result,
            detail = detail,
        )
        _events.value = (listOf(event) + _events.value).take(20)
        appContext?.let { postSyncDebugNotification(it, latestSummary()) }
    }

    private fun latestSummary(): String =
        _events.value.firstOrNull()?.let { formatEvent(it) } ?: "Waiting for sync…"

    fun formatEvent(event: SyncDebugEvent): String {
        val time = DateTimeFormatter.ofPattern("HH:mm:ss")
            .format(Instant.ofEpochMilli(event.timestampMillis).atZone(ZoneId.systemDefault()))
        return "$time · ${event.detail}"
    }
}
