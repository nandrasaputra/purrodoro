package com.endiar.purrodoro.debug

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.endiar.purrodoro.model.AppClock

class KuroDebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val debugRepo = DebugRepository.init(this)
        AppClock.source = debugRepo
        debugRepo.applySyncNotificationPreference()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On API 33+ posting notifications needs runtime permission. We
            // intentionally do NOT auto-prompt on activity create — that used
            // to interrupt the very first frame of onboarding with a system
            // dialog. Instead, post the dev notification opportunistically
            // each time an activity resumes IF permission is already granted
            // (either pre-granted, or just granted by the user via Purrodoro's
            // onboarding flow). If the user never grants it, the dev
            // notification simply never appears in debug builds — accept that
            // tradeoff in favour of a clean first-run experience.
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    val permission = android.Manifest.permission.POST_NOTIFICATIONS
                    if (ContextCompat.checkSelfPermission(activity, permission)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        postDevNotification(this@KuroDebugApplication)
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
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
