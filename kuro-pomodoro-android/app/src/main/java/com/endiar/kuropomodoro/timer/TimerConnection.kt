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
import com.endiar.kuropomodoro.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI-side helper that talks to [TimerService].
 *
 * Two responsibilities:
 *  - Expose the service's runtime state as a [StateFlow], falling back to the persisted
 *    snapshot when the service is not bound.
 *  - Provide a [controller] that the UI can call. The controller is **never null** and
 *    never silently drops actions:
 *      - Non-start actions (pause/resume/stop/skip) are dispatched via
 *        `startForegroundService` so they work whether the service is bound or not.
 *      - `start()` carries typed parameters that can't fit in an intent without serialization,
 *        so when the service isn't bound yet we buffer the call and flush it in
 *        [ServiceConnection.onServiceConnected].
 *
 * Reconciliation of a stale Playing snapshot (app killed mid-session) happens in [bind].
 */
class TimerConnection(private val context: Context) {

    private val _state = MutableStateFlow<TimerRuntimeState?>(null)
    val state: StateFlow<TimerRuntimeState?> = _state.asStateFlow()

    private var controllerRef: TimerController? = null
    private var pendingStart: Pair<TimerSettings, FocusTask?>? = null

    /**
     * Public controller. Always non-null; routes through the binder when available,
     * falls back to an intent dispatch otherwise.
     */
    val controller: TimerController = object : TimerController {
        override fun start(settings: TimerSettings, task: FocusTask?) {
            val bound = controllerRef
            if (bound != null) {
                bound.start(settings, task)
                return
            }
            pendingStart = settings to task
            val intent = Intent(context, TimerService::class.java)
                .setAction(TimerService.ACTION_START)
            safeStartForegroundService(intent)
            tryBind(intent)
        }
        override fun pause()  = dispatchAction(TimerService.ACTION_PAUSE)  { it.pause() }
        override fun resume() = dispatchAction(TimerService.ACTION_RESUME) { it.resume() }
        override fun stop()   = dispatchAction(TimerService.ACTION_STOP)   { it.stop() }
        override fun skip()   = dispatchAction(TimerService.ACTION_SKIP)   { it.skip() }
    }

    private inline fun dispatchAction(
        intentAction: String,
        crossinline directCall: (TimerController) -> Unit
    ) {
        controllerRef?.let { bound ->
            directCall(bound)
            return
        }
        val intent = Intent(context, TimerService::class.java).setAction(intentAction)
        safeStartForegroundService(intent)
        tryBind(intent)
    }

    private fun safeStartForegroundService(intent: Intent) {
        try {
            context.startForegroundService(intent)
        } catch (_: RuntimeException) {
            // ForegroundServiceStartNotAllowedException (API 31+) or OEM-specific blocks.
            // The user's tap can't promote a background service right now; we'll resync
            // state from the repository once the service is reachable again.
        }
    }

    private fun tryBind(intent: Intent) {
        try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (_: SecurityException) {
            // Defensive: should not happen for our own service.
        }
    }

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
            // Flush a buffered start() if one was queued before the binder arrived.
            pendingStart?.let { (settings, task) ->
                lb.controller.start(settings, task)
                pendingStart = null
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
            val repo = AppRepository.get(context)

            // When the service is not running, check if we have a stale Playing snapshot
            // (e.g. app was killed mid-session). Reconcile against the wall clock.
            //
            // Wrap the read + reconcile + writes in NonCancellable so that a fast
            // onStop() cannot tear us apart between writing stamps and clearing the
            // Playing snapshot, which would cause double-stamping on the next bind().
            if (!TimerService.isRunning) {
                val snapshot = repo.runtimeSnapshot.first()
                if (snapshot != null && snapshot.session.timerState == TimerState.Playing) {
                    val settings = repo.appState.first().settings
                    val now = System.currentTimeMillis()
                    withContext(NonCancellable) {
                        val result = TimerReconciler.reconcile(snapshot, now, settings)

                        // Clear/replace the Playing snapshot FIRST so an interrupted
                        // run cannot replay the same reconciliation.
                        repo.setRuntimeSnapshot(
                            if (result.state.session.timerState == TimerState.Idle) null else result.state
                        )
                        result.stampsToIncrement.forEach { date -> repo.incrementStampFor(date) }
                        result.closedRecords.forEach { repo.appendSessionRecord(it) }

                        if (result.state.session.timerState == TimerState.Playing) {
                            val intent = Intent(context, TimerService::class.java)
                                .setAction(TimerService.ACTION_ALARM_FIRED)
                            safeStartForegroundService(intent)
                        }
                    }
                }
            }

            // Subscribe to persisted snapshot so UI reflects state when service is not bound.
            repo.runtimeSnapshot.collectLatest { snapshot ->
                if (controllerRef == null) _state.value = snapshot
            }
        }
        if (TimerService.isRunning) {
            val intent = Intent(context, TimerService::class.java)
            tryBind(intent)
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
}
