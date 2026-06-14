package com.endiar.purrodoro.timer

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
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.TimerSkipOverride
import com.endiar.purrodoro.model.buildCycle
import com.endiar.purrodoro.model.seconds
import com.endiar.purrodoro.model.shouldStampStopAsFocus
import com.endiar.purrodoro.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

class TimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob)

    // Serializes every state transition (start/pause/resume/stop/skip) and the
    // tick loop's segment-completion. The service scope runs on the multi-threaded
    // Dispatchers.Default, so without this a user action (e.g. Skip near a segment
    // boundary) and a concurrent natural completion could interleave and stomp on
    // each other's writes — advancing the cycle past where the user intended.
    private val transitionMutex = Mutex()

    private val _state = MutableStateFlow<TimerRuntimeState?>(null)
    private var tickJob: Job? = null

    private lateinit var repo: AppRepository
    private lateinit var alarmManager: AlarmManager

    private val controllerImpl: TimerController = object : TimerController {
        override fun start(settings: TimerSettings, task: FocusTask?, activeHabitId: Int?, focusType: FocusType) {
            scope.launch { startCycle(settings, task, activeHabitId, focusType) }
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
        scope.launch {
            val appState = repo.appState.first()
            TimerNotification.setLanguage(appState.language.tag)
            TimerNotification.setNotificationSoundEnabled(applicationContext, appState.notificationSoundEnabled)
        }
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
                ACTION_DISMISS_COMPLETION -> doDismissCompletion()
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

    private suspend fun startCycle(settings: TimerSettings, task: FocusTask?, activeHabitId: Int?, focusType: FocusType = FocusType.Empty) = transitionMutex.withLock {
        val items = buildCycle(settings)
        val firstSeconds = TimerSkipOverride.effectiveSeconds(items.first().type.seconds())
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
            taskName = task?.title,
            focusType = focusType,
        )
        val state = TimerRuntimeState(
            session = TimerSession(items, 0, firstSeconds, TimerState.Playing),
            endsAtMillis = now + firstSeconds * 1000L,
            activeTask = task,
            openRecord = record,
            activeHabitId = activeHabitId,
            focusType = focusType,
        )
        updateState(state)
        scheduleAlarm(state)
        startTicking()
    }

    private suspend fun doPause() = transitionMutex.withLock {
        val current = _state.value ?: repo.runtimeSnapshot.first() ?: return@withLock
        if (current.session.timerState != TimerState.Playing) return@withLock
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

    private suspend fun doResume() = transitionMutex.withLock {
        val current = _state.value ?: repo.runtimeSnapshot.first() ?: return@withLock
        if (current.session.timerState != TimerState.Paused) return@withLock
        val now = System.currentTimeMillis()
        val resumed = current.copy(
            session = current.session.copy(timerState = TimerState.Playing),
            endsAtMillis = now + current.session.remainingSeconds * 1000L
        )
        updateState(resumed)
        scheduleAlarm(resumed)
        startTicking()
    }

    private suspend fun doSkip() = transitionMutex.withLock {
        val current = _state.value ?: return@withLock
        val currentType = current.session.items.getOrNull(current.session.currentIndex)?.type
        if (currentType !is SessionItemType.ShortBreak && currentType !is SessionItemType.LongBreak) {
            return@withLock
        }
        cancelAlarm()
        stopTicking()
        val now = System.currentTimeMillis()
        val nextIndex = current.session.currentIndex + 1
        if (nextIndex >= current.session.items.size) {
            // User skipped the final break — semantically the cycle is complete.
            // Mirror the natural end-of-cycle path (see startTicking) so behavior
            // is identical regardless of how the cycle ended, including the
            // auto-complete of an attached real task. Habits keep their own
            // progress model and are not auto-completed here.
            closeOpenRecord(SessionStatus.Completed, now)
            current.activeTask?.takeIf { it.id > 0 && !it.completed }?.let { task ->
                repo.setTaskCompleted(task.id, true)
            }
            repo.setRuntimeSnapshot(null)
            finishCycleAndStop(buildResetState(current))
            return@withLock
        }
        // Skip is an explicit user action — advance the cycle but leave the next
        // segment paused, regardless of autoStartNextSession.
        val nextSeconds = TimerSkipOverride.effectiveSeconds(
            current.session.items[nextIndex].type.seconds()
        )
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

    private suspend fun doStop() = transitionMutex.withLock {
        val current = _state.value
        if (current != null) {
            val now = System.currentTimeMillis()
            // Acknowledge a partial focus in the daily stamp counter so stopping
            // mid-Focus is not invisible in records / streaks. See
            // shouldStampStopAsFocus for the exact rule.
            if (shouldStampStopAsFocus(current.session)) {
                val today = Instant.ofEpochMilli(now)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                repo.incrementStampFor(today)
            }
            current.openRecord?.let { rec ->
                repo.appendSessionRecord(rec.copy(endedAt = now, status = SessionStatus.Partial))
            }
        }
        cancelAlarm()
        stopTicking()
        repo.setRuntimeSnapshot(null)
        _state.value = null
        WidgetUpdater.refreshAll(applicationContext)
        NotificationManagerCompat.from(applicationContext).notify(
            TimerNotification.NOTIF_ID_COMPLETION + (System.currentTimeMillis() % 1000).toInt(),
            TimerNotification.buildCompletionNotification(applicationContext, TimerNotification.CompletionKind.Stopped)
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Collapse the post-cycle "warm reset" Idle state (preserved items + currentIndex=0)
     * to a fully cold state (null), so subsequent settings changes are reflected
     * immediately in the UI before the user taps Start again. Guarded to no-op if a
     * session is in flight, so an out-of-order intent can't tear down a running cycle.
     */
    private suspend fun doDismissCompletion() = transitionMutex.withLock {
        val current = _state.value ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return@withLock
        }
        if (current.session.timerState != TimerState.Idle) return@withLock
        _state.value = null
        repo.setRuntimeSnapshot(null)
        WidgetUpdater.refreshAll(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Called when the alarm receiver detected a missed segment completion while the service was
     * killed and restarted this service. The receiver already applied [TimerStateMachine] and
     * updated the snapshot; we just load it and resume ticking.
     */
    private suspend fun resumeFromAlarm() = transitionMutex.withLock {
        val snapshot = repo.runtimeSnapshot.first() ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return@withLock
        }
        if (snapshot.session.timerState != TimerState.Playing) {
            _state.value = snapshot
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return@withLock
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
                    // Hand off to a serialized completion: a user action (skip/pause)
                    // may have changed the segment while this tick was waiting, so the
                    // completion re-reads the live state under the lock before acting.
                    if (!handleSegmentCompletion()) return@launch
                } else {
                    refreshOngoingNotification(current, now)
                }
                delay(1000L)
            }
        }
    }

    /**
     * Apply the completion of the segment that just elapsed. Runs under
     * [transitionMutex] and re-reads the live [_state] so it can never undo or leap
     * past a concurrent user action (e.g. Skip pressed in the final second of a break,
     * which previously let a stale tick re-advance the cycle and run it to the end).
     *
     * @return true to keep the tick loop running, false to stop it.
     */
    private suspend fun handleSegmentCompletion(): Boolean = transitionMutex.withLock {
        // Re-validate against the live state captured under the lock.
        val current = _state.value ?: return@withLock false
        if (current.session.timerState != TimerState.Playing) return@withLock false
        val endsAt = current.endsAtMillis ?: return@withLock false
        val now = System.currentTimeMillis()
        // The segment was advanced/extended (e.g. a skip moved us to a new segment)
        // while we waited for the lock — this completion is stale, so drop it. The
        // newly active segment, if any, drives its own tick loop.
        if (now < endsAt) return@withLock false

        val settings = repo.appState.first().settings
        val next = TimerStateMachine.applyTimerComplete(current, settings, now, repo)

        if (next.session.timerState == TimerState.Idle) {
            // End of cycle — applyTimerComplete already closed the record
            // and cleared the snapshot. Mirror skip-to-end-of-cycle path.
            finishCycleAndStop(next)
            return@withLock false
        }

        val completedType = current.session.items
            .getOrNull(current.session.currentIndex)?.type
        val kind = if (completedType is SessionItemType.Focus) {
            TimerNotification.CompletionKind.Focus
        } else {
            TimerNotification.CompletionKind.Break
        }
        NotificationManagerCompat.from(applicationContext).notify(
            TimerNotification.NOTIF_ID_COMPLETION + (System.currentTimeMillis() % 1000).toInt(),
            TimerNotification.buildCompletionNotification(applicationContext, kind)
        )

        _state.value = next
        cancelAlarm()

        if (next.session.timerState == TimerState.Playing) {
            scheduleAlarm(next)
            // Refresh the ongoing notification immediately so it reflects the
            // new segment instead of lagging by one tick (~1s).
            refreshOngoingNotification(next, System.currentTimeMillis())
            true
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            false
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
        WidgetUpdater.refreshAll(applicationContext)
    }

    private suspend fun closeOpenRecord(status: SessionStatus, endedAt: Long) {
        _state.value?.openRecord?.let { rec ->
            repo.appendSessionRecord(rec.copy(endedAt = endedAt, status = status))
        }
    }

    /**
     * Build the post-cycle Idle state: same items, currentIndex reset to 0, openRecord cleared.
     * Mirrors TimerStateMachine.applyTimerComplete's resetState so the UI sees an
     * identical post-cycle layout regardless of whether the cycle ended naturally
     * or via a user skip on the final break.
     */
    private fun buildResetState(current: TimerRuntimeState): TimerRuntimeState {
        val items = current.session.items
        return current.copy(
            session = current.session.copy(
                currentIndex = 0,
                remainingSeconds = items.firstOrNull()?.type?.seconds() ?: 0,
                timerState = TimerState.Idle
            ),
            endsAtMillis = null,
            openRecord = null
        )
    }

    /**
     * Shared end-of-cycle completion: fire the Cycle notification, publish the
     * reset state to UI subscribers, and tear down the foreground service.
     * Caller is responsible for closing any open SessionRecord and clearing the
     * persisted runtime snapshot before invoking this.
     */
    private suspend fun finishCycleAndStop(resetState: TimerRuntimeState) {
        NotificationManagerCompat.from(applicationContext).notify(
            TimerNotification.NOTIF_ID_COMPLETION + (System.currentTimeMillis() % 1000).toInt(),
            TimerNotification.buildCompletionNotification(applicationContext, TimerNotification.CompletionKind.Cycle)
        )
        repo.setPendingCycleCompletionAck(true)
        _state.value = resetState
        cancelAlarm()
        WidgetUpdater.refreshAll(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "com.endiar.purrodoro.timer.START"
        const val ACTION_PAUSE = "com.endiar.purrodoro.timer.PAUSE"
        const val ACTION_RESUME = "com.endiar.purrodoro.timer.RESUME"
        const val ACTION_STOP = "com.endiar.purrodoro.timer.STOP"
        const val ACTION_SKIP = "com.endiar.purrodoro.timer.SKIP"
        const val ACTION_DISMISS_COMPLETION = "com.endiar.purrodoro.timer.DISMISS_COMPLETION"
        const val ACTION_ALARM_FIRED = "com.endiar.purrodoro.timer.ALARM_FIRED"

        private const val ALARM_REQUEST_CODE = 9001

        @Volatile var isRunning: Boolean = false
            internal set
    }
}
