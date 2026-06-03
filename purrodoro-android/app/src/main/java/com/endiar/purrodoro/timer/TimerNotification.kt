package com.endiar.purrodoro.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.endiar.purrodoro.MainActivity
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.formatSeconds
import java.util.Locale

object TimerNotification {

    // BCP-47 tag for the user-selected in-app language (null = follow device).
    // Notifications run outside Compose, so we resolve strings against a context
    // configured with this locale instead of the device default.
    @Volatile
    var localeTag: String? = null
        private set

    fun setLanguage(tag: String?) {
        localeTag = tag
    }

    /** A [Context] whose resources resolve against the selected in-app language. */
    private fun localized(context: Context): Context {
        val tag = localeTag ?: return context
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
        }
        return context.createConfigurationContext(config)
    }

    const val CHANNEL_RUNNING = "timer_running"
    // Two fixed channels: one with sound, one silent. Android does not allow updating
    // a channel's sound after it has been created, so we route notifications to the
    // appropriate channel at post-time instead of mutating an existing channel.
    const val CHANNEL_COMPLETION_SOUND = "timer_completion_sound"
    const val CHANNEL_COMPLETION_SILENT = "timer_completion_silent"

    const val NOTIF_ID_RUNNING = 1001
    const val NOTIF_ID_COMPLETION = 1002

    var notificationSoundEnabled: Boolean = true
        private set

    fun setNotificationSoundEnabled(context: Context, enabled: Boolean) {
        notificationSoundEnabled = enabled
        ensureChannels(context)
    }

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val res = localized(context)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNNING,
                res.getString(R.string.notif_channel_running_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = res.getString(R.string.notif_channel_running_desc)
                setShowBadge(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COMPLETION_SOUND,
                res.getString(R.string.notif_channel_complete_sound_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = res.getString(R.string.notif_channel_complete_desc)
                setSound("android.resource://${context.packageName}/raw/kuro_sound".toUri(), null)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COMPLETION_SILENT,
                res.getString(R.string.notif_channel_complete_silent_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = res.getString(R.string.notif_channel_complete_desc)
                setSound(null, null)
            }
        )
    }

    fun buildRunningNotification(
        context: Context,
        state: TimerRuntimeState,
        now: Long
    ): Notification {
        val res = localized(context)
        val (title, subtitle) = describe(res, state, now)
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
                builder.addAction(action(context, res.getString(R.string.notif_action_pause), TimerService.ACTION_PAUSE))
                builder.addAction(action(context, res.getString(R.string.notif_action_stop), TimerService.ACTION_STOP))
                if (isOnBreak(state)) {
                    builder.addAction(action(context, res.getString(R.string.notif_action_skip), TimerService.ACTION_SKIP))
                }
            }

            TimerState.Paused -> {
                builder.addAction(action(context, res.getString(R.string.notif_action_resume), TimerService.ACTION_RESUME))
                builder.addAction(action(context, res.getString(R.string.notif_action_stop), TimerService.ACTION_STOP))
            }

            TimerState.Idle -> Unit
        }
        return builder.build()
    }

    enum class CompletionKind { Focus, Break, Cycle, Stopped }

    fun buildCompletionNotification(context: Context, kind: CompletionKind): Notification {
        val res = localized(context)
        val (title, body) = when (kind) {
            CompletionKind.Focus -> res.getString(R.string.notif_focus_complete_title) to res.getString(R.string.notif_focus_complete_body)
            CompletionKind.Break -> res.getString(R.string.notif_break_complete_title) to res.getString(R.string.notif_break_complete_body)
            CompletionKind.Cycle -> res.getString(R.string.notif_cycle_complete_title) to res.getString(R.string.notif_cycle_complete_body)
            CompletionKind.Stopped -> res.getString(R.string.notif_timer_stopped_title) to res.getString(R.string.notif_timer_stopped_body)
        }
        val channel = if (notificationSoundEnabled) CHANNEL_COMPLETION_SOUND else CHANNEL_COMPLETION_SILENT
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun describe(res: Context, state: TimerRuntimeState, now: Long): Pair<String, String?> {
        val remaining = currentRemainingSeconds(state, now)
        val timeStr = formatSeconds(remaining)
        val currentItem = state.session.items.getOrNull(state.session.currentIndex)
        val segmentLabel = when (currentItem?.type) {
            is SessionItemType.Focus -> res.getString(R.string.notif_segment_focus)
            is SessionItemType.ShortBreak -> res.getString(R.string.notif_segment_short_break)
            is SessionItemType.LongBreak -> res.getString(R.string.notif_segment_long_break)
            null -> res.getString(R.string.notif_segment_timer)
        }
        val title = when (state.session.timerState) {
            TimerState.Playing -> res.getString(R.string.notif_title_playing, segmentLabel, timeStr)
            TimerState.Paused -> res.getString(R.string.notif_title_paused, segmentLabel)
            TimerState.Idle -> segmentLabel
        }
        val subtitle = when {
            state.session.timerState == TimerState.Paused -> res.getString(R.string.notif_remaining, timeStr)
            state.activeTask != null && currentItem?.type is SessionItemType.Focus ->
                res.getString(
                    R.string.notif_task_progress,
                    state.activeTask.title,
                    state.activeTask.completedSessions + 1,
                    state.activeTask.plannedSessions,
                )

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
