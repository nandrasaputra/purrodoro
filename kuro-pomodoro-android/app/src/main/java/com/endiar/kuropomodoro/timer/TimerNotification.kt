package com.endiar.kuropomodoro.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.endiar.kuropomodoro.MainActivity
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.formatSeconds

object TimerNotification {

    const val CHANNEL_RUNNING = "timer_running"
    const val CHANNEL_COMPLETION = "timer_completion"

    const val NOTIF_ID_RUNNING = 1001
    const val NOTIF_ID_COMPLETION = 1002

    var notificationSoundEnabled: Boolean = true
        private set

    fun setNotificationSoundEnabled(context: Context, enabled: Boolean) {
        notificationSoundEnabled = enabled
        ensureChannels(context, enabled)
    }

    fun ensureChannels(context: Context, soundEnabled: Boolean = true) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNNING,
                "Timer running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while a Pomodoro is active"
                setShowBadge(false)
            }
        )
        val completionChannel = NotificationChannel(
            CHANNEL_COMPLETION,
            "Segment complete",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notification when a Focus or Break segment ends"
            if (soundEnabled) {
                setSound("android.resource://${context.packageName}/raw/kuro_sound".toUri(), null)
            }
        }
        nm.createNotificationChannel(completionChannel)
    }

    fun buildRunningNotification(
        context: Context,
        state: TimerRuntimeState,
        now: Long
    ): Notification {
        val (title, subtitle) = describe(state, now)
        val remaining = currentRemainingSeconds(state, now)
        val total = state.session.items.getOrNull(state.session.currentIndex)?.type?.seconds() ?: 1

        val builder = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .setProgress(total, total - remaining, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        when (state.session.timerState) {
            TimerState.Playing -> {
                builder.addAction(action(context, "Pause", TimerService.ACTION_PAUSE))
                builder.addAction(action(context, "Stop", TimerService.ACTION_STOP))
                if (isOnBreak(state)) {
                    builder.addAction(action(context, "Skip", TimerService.ACTION_SKIP))
                }
            }

            TimerState.Paused -> {
                builder.addAction(action(context, "Resume", TimerService.ACTION_RESUME))
                builder.addAction(action(context, "Stop", TimerService.ACTION_STOP))
            }

            TimerState.Idle -> Unit
        }
        return builder.build()
    }

    enum class CompletionKind { Focus, Break, Cycle, Stopped }

    fun buildCompletionNotification(context: Context, kind: CompletionKind): Notification {
        val (title, body) = when (kind) {
            CompletionKind.Focus -> "Focus complete" to "Tap to start your break"
            CompletionKind.Break -> "Break complete" to "Ready for another focus session?"
            CompletionKind.Cycle -> "Cycle complete" to "Nice work — your Pomodoro cycle is done"
            CompletionKind.Stopped -> "Timer stopped" to "Session ended early"
        }
        return NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun describe(state: TimerRuntimeState, now: Long): Pair<String, String?> {
        val remaining = currentRemainingSeconds(state, now)
        val timeStr = formatSeconds(remaining)
        val currentItem = state.session.items.getOrNull(state.session.currentIndex)
        val segmentLabel = when (currentItem?.type) {
            is SessionItemType.Focus -> "Focus"
            is SessionItemType.ShortBreak -> "Short Break"
            is SessionItemType.LongBreak -> "Long Break"
            null -> "Timer"
        }
        val title = when (state.session.timerState) {
            TimerState.Playing -> "$segmentLabel · $timeStr"
            TimerState.Paused -> "$segmentLabel · Paused"
            TimerState.Idle -> segmentLabel
        }
        val subtitle = when {
            state.session.timerState == TimerState.Paused -> "$timeStr remaining"
            state.activeTask != null && currentItem?.type is SessionItemType.Focus ->
                "${state.activeTask.title} · ${state.activeTask.completedSessions + 1} / ${state.activeTask.plannedSessions}"

            else -> null
        }
        return title to subtitle
    }

    private fun isOnBreak(state: TimerRuntimeState): Boolean {
        val t = state.session.items.getOrNull(state.session.currentIndex)?.type ?: return false
        return t is SessionItemType.ShortBreak || t is SessionItemType.LongBreak
    }

    private fun currentRemainingSeconds(state: TimerRuntimeState, now: Long): Int {
        return if (state.session.timerState == TimerState.Playing && state.endsAtMillis != null) {
            ((state.endsAtMillis - now) / 1000L).toInt().coerceAtLeast(0)
        } else {
            state.session.remainingSeconds
        }
    }

    private fun SessionItemType.seconds(): Int = when (this) {
        is SessionItemType.Focus -> seconds
        is SessionItemType.ShortBreak -> seconds
        is SessionItemType.LongBreak -> seconds
    }

    private fun action(
        context: Context,
        label: String,
        intentAction: String
    ): NotificationCompat.Action {
        val intent = Intent(context, TimerService::class.java).setAction(intentAction)
        val pi = PendingIntent.getService(
            context,
            intentAction.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pi).build()
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
