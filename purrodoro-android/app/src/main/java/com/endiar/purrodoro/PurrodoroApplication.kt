package com.endiar.purrodoro

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import kotlin.concurrent.thread

/**
 * Application entry for release builds. Initializes Firebase Crashlytics when
 * [google-services.json] is present (processed by the Google Services Gradle
 * plugin).
 */
open class PurrodoroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initCrashlytics()
        thread(name = "clear-retired-files", start = true) {
            clearRetiredSoundscapeFiles(this)
        }
    }

    private fun clearRetiredSoundscapeFiles(app: Application) {
        listOf("soundscapes", "soundscape-thumbs", "account-avatars").forEach { name ->
            runCatching { File(app.filesDir, name).deleteRecursively() }
        }
    }

    private fun initCrashlytics() {
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) return
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !isDebuggable
        }
    }

    private val isDebuggable: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
