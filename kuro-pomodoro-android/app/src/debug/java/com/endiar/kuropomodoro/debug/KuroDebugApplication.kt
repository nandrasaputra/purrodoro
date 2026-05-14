package com.endiar.kuropomodoro.debug

import android.app.Application
import com.endiar.kuropomodoro.model.AppClock

class KuroDebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val debugRepo = DebugRepository.init()
        AppClock.source = debugRepo
        postDevNotification(this)
    }
}
