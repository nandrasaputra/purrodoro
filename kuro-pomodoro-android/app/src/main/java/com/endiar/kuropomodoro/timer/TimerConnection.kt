package com.endiar.kuropomodoro.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI-side helper that binds to [TimerService] and exposes its state as a [StateFlow].
 *
 * - When the service is running and bound, [state] mirrors the service's internal flow.
 * - When the service is not running, [state] emits the persisted runtime snapshot from
 *   the repository (so the UI can show a paused/reconciled state without the service alive).
 *
 * Reconciliation of a stale Playing snapshot (app killed mid-session) happens in [bind].
 */
class TimerConnection(private val context: Context) {

    private val _state = MutableStateFlow<TimerRuntimeState?>(null)
    val state: StateFlow<TimerRuntimeState?> = _state.asStateFlow()

    private var controllerRef: TimerController? = null
    val controller: TimerController? get() = controllerRef ?: FallbackController

    private var serviceJob: Job? = null
    private var snapshotJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val lb = service as? TimerService.LocalBinder ?: return
            controllerRef = lb.controller
            serviceJob?.cancel()
            serviceJob = scope.launch {
                lb.state.collectLatest { _state.value = it }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            controllerRef = null
            serviceJob?.cancel()
            serviceJob = null
        }
    }

    fun bind() {
        TimerNotification.ensureChannels(context)
        snapshotJob?.cancel()
        snapshotJob = scope.launch {
            AppRepository.get(context).runtimeSnapshot.collectLatest { snapshot ->
                if (controllerRef == null) _state.value = snapshot
            }
        }
        if (TimerService.isRunning) {
            val intent = Intent(context, TimerService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        snapshotJob?.cancel()
        snapshotJob = null
        if (controllerRef != null) {
            try { context.unbindService(serviceConnection) } catch (_: IllegalArgumentException) {}
        }
        controllerRef = null
        serviceJob?.cancel()
        serviceJob = null
    }

    /**
     * Start a new cycle. Bootstraps the service if not running, then binds.
     */
    fun start(settings: TimerSettings, task: FocusTask?) {
        if (!TimerService.isRunning) {
            val intent = Intent(context, TimerService::class.java)
                .setAction(TimerService.ACTION_START)
            context.startForegroundService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            // Defer the start call until the binder arrives
            scope.launch {
                var attempts = 0
                while (controllerRef == null && attempts < 20) {
                    delay(100)
                    attempts++
                }
                controllerRef?.start(settings, task)
            }
        } else {
            controllerRef?.start(settings, task)
        }
    }

    /** No-op fallback used when the service isn't bound yet. */
    private object FallbackController : TimerController {
        override fun start(settings: TimerSettings, task: FocusTask?) {}
        override fun pause()  {}
        override fun resume() {}
        override fun stop()   {}
        override fun skip()   {}
    }
}
