package com.endiar.kuropomodoro.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder

// Placeholder — fully implemented in Task 9.
class TimerService : Service() {
    companion object {
        const val ACTION_START = "com.endiar.kuropomodoro.timer.START"
        const val ACTION_PAUSE = "com.endiar.kuropomodoro.timer.PAUSE"
        const val ACTION_RESUME = "com.endiar.kuropomodoro.timer.RESUME"
        const val ACTION_STOP = "com.endiar.kuropomodoro.timer.STOP"
        const val ACTION_SKIP = "com.endiar.kuropomodoro.timer.SKIP"
        const val ACTION_ALARM_FIRED = "com.endiar.kuropomodoro.timer.ALARM_FIRED"

        @Volatile var isRunning: Boolean = false
            internal set
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
