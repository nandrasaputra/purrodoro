package com.endiar.kuropomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.AppLanguage
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.AppearanceMode
import com.endiar.kuropomodoro.timer.TimerConnection
import com.endiar.kuropomodoro.timer.TimerNotification
import com.endiar.kuropomodoro.ui.KuroLocalizationProvider
import com.endiar.kuropomodoro.ui.theme.KuroPomodoroTheme
import com.endiar.kuropomodoro.widget.QuickStartWidget
import com.endiar.kuropomodoro.widget.WidgetActions
import com.endiar.kuropomodoro.widget.WidgetRefreshWorker
import com.endiar.kuropomodoro.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private lateinit var timerConnection: TimerConnection

    /**
     * One-shot signal pushed by the Quick Start widget (and any future entry
     * points) asking the app to auto-open the Start Focus sheet on launch.
     * Replay buffer is zero on purpose — only fresh signals fire the sheet.
     */
    private val _autoOpenStartFocus = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val autoOpenStartFocus: SharedFlow<Unit> = _autoOpenStartFocus.asSharedFlow()

    /**
     * One-shot signal pushed by the reactive 4x4 widgets asking the app to
     * switch to a specific tab (e.g. a habit nudge opens the Habits tab).
     */
    private val _openTab = MutableSharedFlow<AppTab>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val openTab: SharedFlow<AppTab> = _openTab.asSharedFlow()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleWidgetIntent(intent)
        // Initial transparent setup; style (light/dark icons) is updated reactively below.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val repo = AppRepository.get(this)
        val initialAppState = runCatching {
            kotlinx.coroutines.runBlocking { repo.appState.first() }
        }.getOrNull()
        TimerNotification.setLanguage(initialAppState?.language?.tag)
        TimerNotification.setNotificationSoundEnabled(
            applicationContext,
            initialAppState?.notificationSoundEnabled ?: true,
        )
        timerConnection = TimerConnection(applicationContext)

        // Keep the home-screen widgets honest: schedule the daily "new day"
        // refresh and push an immediate update so opening the app reflects the
        // latest focus / habit / task state on the home screen.
        WidgetRefreshWorker.schedule(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            WidgetUpdater.refreshAll(applicationContext)
        }

        setContent {
            val repo = AppRepository.get(this)
            val appearanceMode by repo.appState
                .map { it.appearanceMode }
                .collectAsStateWithLifecycle(initialValue = AppearanceMode.Auto)

            val language by repo.appState
                .map { it.language }
                .collectAsStateWithLifecycle(initialValue = AppLanguage.System)

            val systemInDark = isSystemInDarkTheme()
            val isDark = appearanceMode.isDark(systemInDark)

            // Reactively update status bar icon style so icons are visible in both light and dark.
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                )
            }

            KuroLocalizationProvider(language = language) {
                KuroPomodoroTheme(appearanceMode = appearanceMode) {
                    KuroPomodoroApp(
                        repo = repo,
                        timerConnection = timerConnection,
                        requestNotificationPermission = ::requestNotificationPermissionIfNeeded,
                        autoOpenStartFocus = autoOpenStartFocus,
                        openTab = openTab,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(QuickStartWidget.EXTRA_AUTO_OPEN_START_FOCUS, false)) {
            _autoOpenStartFocus.tryEmit(Unit)
            // Consume the flag so a config change replay doesn't re-trigger.
            intent.removeExtra(QuickStartWidget.EXTRA_AUTO_OPEN_START_FOCUS)
        }
        intent.getStringExtra(WidgetActions.EXTRA_OPEN_TAB)?.let { name ->
            AppTab.entries.firstOrNull { it.name == name }?.let { _openTab.tryEmit(it) }
            intent.removeExtra(WidgetActions.EXTRA_OPEN_TAB)
        }
    }

    override fun onStart() {
        super.onStart()
        timerConnection.bind()
    }

    override fun onStop() {
        timerConnection.unbind()
        super.onStop()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
