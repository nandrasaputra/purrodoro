package com.endiar.kuropomodoro.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.buildCycle
import com.endiar.kuropomodoro.model.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob)

    private val _state = MutableStateFlow<TimerRuntimeState?>(null)
    private var tickJob: Job? = null

    private lateinit var repo: AppRepository
    private lateinit var alarmManager: AlarmManager

    private val controllerImpl: TimerController = object : TimerController {
        override fun start(settings: TimerSettings, task: FocusTask?) {
            scope.launch { startCycle(settings, task) }
        }
        override fun pause()  { scope.launch { doPause() } }
        override fun resume() { scope.launch { doResume() } }
        override fun stop()   { scope.launch { doStop() } }
        override fun skip()   { scope.launch { doSkip() } }
    }

    inner class LocalBinder : Binder() {
        val state: StateFlow<TimerRuntimeState?> = _state.asStateFlow()
        val controller: TimerController = controllerImpl
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        repo = AppRepository.get(applicationContext)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        TimerNotification.ensureChannels(applicationContext)
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        // Promote to foreground immediately.
        val placeholder = _state.value ?: TimerRuntimeState(
            session = TimerSession(emptyList(), 0, 0, TimerState.Idle),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null
        )
        ServiceCompat.startForeground(
            this,
            TimerNotification.NOTIF_ID_RUNNING,
            TimerNotification.buildRunningNotification(applicationContext, placeholder, System.currentTimeMillis()),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        scope.launch {
            when (action) {
                ACTION_PAUSE        -> doPause()
                ACTION_RESUME       -> doResume()
                ACTION_STOP         -> doStop()
                ACTION_SKIP         -> doSkip()
                ACTION_ALARM_FIRED  -> resumeFromAlarm()
                // ACTION_START is handled via the controller's start()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        isRunning = false
        tickJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    // --- Transitions ---

    private suspend fun startCycle(settings: TimerSettings, task: FocusTask?) {
        val items = buildCycle(settings)
        val firstSeconds = items.first().type.seconds()
        val now = System.currentTimeMillis()
        val record = SessionRecord(
            id = now,
            startedAt = now,
            endedAt = 0L,
            focusMinutes = settings.focusMinutes,
            shortBreakMinutes = settings.shortBreakMinutes,
            longBreakMinutes = settings.longBreakMinutes,
            completedFocusCount = 0,
            totalFocusCount = items.count { it.type is SessionItemType.Focus },
            status = SessionStatus.Partial,
            taskName = task?.title
        )
        val state = TimerRuntimeState(
            session = TimerSession(items, 0, firstSeconds, TimerState.Playing),
            endsAtMillis = now + firstSeconds * 1000L,
            activeTask = task,
            openRecord = record
        )
        updateState(state)
        scheduleAlarm(state)
        startTicking()
    }

    private suspend fun doPause() {
        val current = _state.value ?: repo.runtimeSnapshot.first() ?: return
        if (current.session.timerState != TimerState.Playing) return
        val now = System.currentTimeMillis()
        val remaining = current.endsAtMillis
            ?.let { ((it - now) / 1000L).toInt().coerceAtLeast(0) }
            ?: current.session.remainingSeconds
        val paused = current.copy(
            session = current.session.copy(
                remainingSeconds = remaining,
                timerState = TimerState.Paused
            ),
            endsAtMillis = null
        )
        cancelAlarm()
        stopTicking()
        updateState(paused)
    }

    private suspend fun doResume() {
        val current = _state.value ?: repo.runtimeSnapshot.first() ?: return
        if (current.session.timerState != TimerState.Paused) return
        val now = System.currentTimeMillis()
        val resumed = current.copy(
            session = current.session.copy(timerState = TimerState.Playing),
            endsAtMillis = now + current.session.remainingSeconds * 1000L
        )
        updateState(resumed)
        scheduleAlarm(resumed)
        startTicking()
    }

    private suspend fun doSkip() {
        val current = _state.value ?: return
        val currentType = current.session.items.getOrNull(current.session.currentIndex)?.type
        if (currentType !is SessionItemType.ShortBreak && currentType !is SessionItemType.LongBreak) {
            return
        }
        cancelAlarm()
        stopTicking()
        val now = System.currentTimeMillis()
        val nextIndex = current.session.currentIndex + 1
        if (nextIndex >= current.session.items.size) {
            closeOpenRecord(SessionStatus.Completed, now)
            updateState(idleState(current.session.items))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        // Skip is an explicit user action — advance the cycle but leave the next
        // segment paused, regardless of autoStartNextSession. Matches both the
        // legacy in-Compose behavior and TimerSessionReducer's Skip handler.
        val nextSeconds = current.session.items[nextIndex].type.seconds()
        val updated = current.copy(
            session = current.session.copy(
                currentIndex = nextIndex,
                remainingSeconds = nextSeconds,
                timerState = TimerState.Paused
            ),
            endsAtMillis = null
        )
        updateState(updated)
    }

    private suspend fun doStop() {
        val current = _state.value
        if (current != null) {
            val now = System.currentTimeMillis()
            current.openRecord?.let { rec ->
                repo.appendSessionRecord(rec.copy(endedAt = now, status = SessionStatus.Partial))
            }
        }
        cancelAlarm()
        stopTicking()
        repo.setRuntimeSnapshot(null)
        _state.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Called when the alarm receiver detected a missed segment completion while the service was
     * killed and restarted this service. The receiver already applied [TimerStateMachine] and
     * updated the snapshot; we just load it and resume ticking.
     */
    private suspend fun resumeFromAlarm() {
        val snapshot = repo.runtimeSnapshot.first() ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (snapshot.session.timerState != TimerState.Playing) {
            _state.value = snapshot
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        _state.value = snapshot
        scheduleAlarm(snapshot)
        startTicking()
    }

    // --- Tick loop ---

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val current = _state.value ?: break
                if (current.session.timerState != TimerState.Playing) break
                val now = System.currentTimeMillis()
                val endsAt = current.endsAtMillis ?: break

                if (now >= endsAt) {
                    val settings = repo.appState.first().settings
                    val next = TimerStateMachine.applyTimerComplete(current, settings, now, repo)

                    val completedType = current.session.items
                        .getOrNull(current.session.currentIndex)?.type
                    val kind = when {
                        next.session.timerState == TimerState.Idle -> TimerNotification.CompletionKind.Cycle
                        completedType is SessionItemType.Focus -> TimerNotification.CompletionKind.Focus
                        else -> TimerNotification.CompletionKind.Break
                    }
                    NotificationManagerCompat.from(applicationContext).notify(
                        TimerNotification.NOTIF_ID_COMPLETION + (System.currentTimeMillis() % 1000).toInt(),
                        TimerNotification.buildCompletionNotification(applicationContext, kind)
                    )

                    _state.value = next
                    cancelAlarm()

                    if (next.session.timerState == TimerState.Playing) {
                        scheduleAlarm(next)
                    } else {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }
                } else {
                    refreshOngoingNotification(current, now)
                }
                delay(1000L)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun refreshOngoingNotification(state: TimerRuntimeState, now: Long) {
        NotificationManagerCompat.from(applicationContext).notify(
            TimerNotification.NOTIF_ID_RUNNING,
            TimerNotification.buildRunningNotification(applicationContext, state, now)
        )
    }

    // --- Alarm scheduling ---

    private fun scheduleAlarm(state: TimerRuntimeState) {
        val endsAt = state.endsAtMillis ?: return
        val intent = Intent(applicationContext, TimerAlarmReceiver::class.java).apply {
            putExtra(TimerAlarmReceiver.EXTRA_EXPECTED_ENDS_AT, endsAt)
        }
        val pi = PendingIntent.getBroadcast(
            applicationContext, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pi)
    }

    private fun cancelAlarm() {
        val intent = Intent(applicationContext, TimerAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            applicationContext, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    // --- Helpers ---

    private suspend fun updateState(state: TimerRuntimeState) {
        _state.value = state
        repo.setRuntimeSnapshot(state)
        refreshOngoingNotification(state, System.currentTimeMillis())
    }

    private suspend fun closeOpenRecord(status: SessionStatus, endedAt: Long) {
        _state.value?.openRecord?.let { rec ->
            repo.appendSessionRecord(rec.copy(endedAt = endedAt, status = status))
        }
    }

    private fun idleState(items: List<TimerSessionItem>): TimerRuntimeState {
        val firstSeconds = items.firstOrNull()?.type?.seconds() ?: 0
        return TimerRuntimeState(
            session = TimerSession(items, 0, firstSeconds, TimerState.Idle),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null
        )
    }

    companion object {
        const val ACTION_START = "com.endiar.kuropomodoro.timer.START"
        const val ACTION_PAUSE = "com.endiar.kuropomodoro.timer.PAUSE"
        const val ACTION_RESUME = "com.endiar.kuropomodoro.timer.RESUME"
        const val ACTION_STOP = "com.endiar.kuropomodoro.timer.STOP"
        const val ACTION_SKIP = "com.endiar.kuropomodoro.timer.SKIP"
        const val ACTION_ALARM_FIRED = "com.endiar.kuropomodoro.timer.ALARM_FIRED"

        private const val ALARM_REQUEST_CODE = 9001

        @Volatile var isRunning: Boolean = false
            internal set
    }
}
