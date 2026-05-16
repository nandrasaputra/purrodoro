package com.endiar.kuropomodoro.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        val expectedEndsAtMillis = intent?.getLongExtra(EXTRA_EXPECTED_ENDS_AT, -1L) ?: -1L

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AppRepository.get(appCtx)
                val snapshot = repo.runtimeSnapshot.first() ?: return@launch
                val appState = repo.appState.first()
                val settings = appState.settings

                // If the snapshot has already advanced past the alarm's target time, the service
                // handled it. Avoid double-posting the completion notification.
                if (snapshot.endsAtMillis == null || snapshot.endsAtMillis != expectedEndsAtMillis) {
                    return@launch
                }

                if (snapshot.session.timerState != TimerState.Playing) return@launch

                val completedItemType = snapshot.session.items
                    .getOrNull(snapshot.session.currentIndex)?.type

                val now = System.currentTimeMillis()
                val advanced = TimerStateMachine.applyTimerComplete(
                    snapshot = snapshot,
                    settings = settings,
                    now = now,
                    repo = repo
                )

                val kind = when (completedItemType) {
                    is SessionItemType.Focus ->
                        if (advanced.session.timerState == TimerState.Idle) TimerNotification.CompletionKind.Cycle
                        else TimerNotification.CompletionKind.Focus
                    is SessionItemType.ShortBreak,
                    is SessionItemType.LongBreak ->
                        if (advanced.session.timerState == TimerState.Idle) TimerNotification.CompletionKind.Cycle
                        else TimerNotification.CompletionKind.Break
                    else -> TimerNotification.CompletionKind.Focus
                }

                val soundEnabled = repo.appState.first().notificationSoundEnabled
                TimerNotification.setNotificationSoundEnabled(appCtx, soundEnabled)
                TimerNotification.ensureChannels(appCtx, soundEnabled)
                NotificationManagerCompat.from(appCtx).notify(
                    TimerNotification.NOTIF_ID_COMPLETION + uniqueIdSuffix(),
                    TimerNotification.buildCompletionNotification(appCtx, kind)
                )

                if (advanced.session.timerState == TimerState.Playing && !TimerService.isRunning) {
                    // Auto-advance into next segment but service was killed — restart it.
                    // Some OEMs / strict Doze policies can throw
                    // ForegroundServiceStartNotAllowedException (API 31+) even with an
                    // exact-alarm exemption; if that happens the completion notification
                    // we just posted is the user's cue to reopen the app.
                    try {
                        val resumeIntent = Intent(appCtx, TimerService::class.java)
                            .setAction(TimerService.ACTION_ALARM_FIRED)
                        appCtx.startForegroundService(resumeIntent)
                    } catch (_: RuntimeException) {
                        // Service stays dead; runtimeSnapshot already reflects the new
                        // Playing state, so reconciliation will pick this up on next open.
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun uniqueIdSuffix(): Int =
        (SystemClock.elapsedRealtime() % Int.MAX_VALUE).toInt() and 0x0000_FFFF

    companion object {
        const val EXTRA_EXPECTED_ENDS_AT = "com.endiar.kuropomodoro.timer.EXPECTED_ENDS_AT"
    }
}
