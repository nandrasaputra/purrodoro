package com.endiar.kuropomodoro.debug

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.endiar.kuropomodoro.model.AppClock

class KuroDebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val debugRepo = DebugRepository.init()
        AppClock.source = debugRepo

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On API 33+ we need runtime permission before posting notifications.
            // Request it from the first activity that starts, then (re-)post once resumed.
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                private var permissionRequested = false

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (!permissionRequested) {
                        permissionRequested = true
                        val permission = android.Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(activity, permission)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(activity, arrayOf(permission), 0)
                        }
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    // Re-post every time an activity resumes — if permission was just granted
                    // this will make the notification appear; subsequent calls are no-ops
                    // (NotificationManager.notify with same ID just updates the existing one).
                    val permission = android.Manifest.permission.POST_NOTIFICATIONS
                    if (ContextCompat.checkSelfPermission(activity, permission)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        postDevNotification(this@KuroDebugApplication)
                    }
                }

                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
        } else {
            // API < 33: no runtime permission needed
            postDevNotification(this)
        }
    }
}
