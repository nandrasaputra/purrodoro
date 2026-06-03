package com.endiar.purrodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.AppTab
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.timer.TimerConnection
import com.endiar.purrodoro.timer.TimerNotification
import com.endiar.purrodoro.ui.KuroLocalizationProvider
import com.endiar.purrodoro.ui.theme.KuroPomodoroTheme
import com.endiar.purrodoro.widget.QuickStartWidget
import com.endiar.purrodoro.widget.WidgetActions
import com.endiar.purrodoro.widget.WidgetRefreshWorker
import com.endiar.purrodoro.widget.WidgetUpdater
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // If the user denied and the OS will no longer surface the in-app
            // prompt (permanently denied / "Don't allow"), guide them to the
            // app's notification settings so the gentle reminder isn't a dead end.
            if (!granted &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                openAppNotificationSettings()
            }
        }

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
                        onEnableNotifications = ::promptEnableNotifications,
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

    /**
     * Entry point for the Profile "Allow" reminder. On Android 13+ with the
     * runtime permission still ungranted we fire the in-app system prompt
     * (the launcher routes a permanent denial to settings). Otherwise — pre-13,
     * or when the permission is granted but notifications are switched off at
     * the app/channel level — we open the OS notification settings directly.
     */
    private fun promptEnableNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        openAppNotificationSettings()
    }

    private fun openAppNotificationSettings() {
        val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        runCatching { startActivity(notificationSettings) }.onFailure {
            // Fall back to the generic app-details page on OEMs that lack the
            // dedicated notification settings action.
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", packageName, null))
            )
        }
    }
}
