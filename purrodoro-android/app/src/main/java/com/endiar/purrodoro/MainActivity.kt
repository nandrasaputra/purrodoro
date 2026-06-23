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
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.billing.BillingModule
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.AppState
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.AppTab
import com.endiar.purrodoro.sync.SyncModule
import com.endiar.purrodoro.timer.TimerConnection
import com.endiar.purrodoro.timer.TimerNotification
import com.endiar.purrodoro.ui.KuroLocalizationProvider
import com.endiar.purrodoro.ui.theme.PurrodoroTheme
import com.endiar.purrodoro.widget.WidgetActions
import com.endiar.purrodoro.widget.WidgetRefreshWorker
import com.endiar.purrodoro.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private lateinit var timerConnection: TimerConnection

    /**
     * One-shot signal pushed when a widget (or deep link) asks the app to
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

        val repo = AppRepository.get(this)
        val authRepository = AuthRepository.get(this)
        SyncModule.get(this) // start the offline-first sync triggers
        BillingModule.get(this) // Play Billing: query subs + sync premium entitlement
        // Block until the first app-state snapshot so theme, locale, and the home
        // hero scene match persisted values on frame 1 (no Room→saved-scene flash).
        val initialAppState = runCatching {
            runBlocking { repo.appState.first() }
        }.getOrNull()
        val systemInDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val initialIsDark = initialAppState?.appearanceMode?.isDark(systemInDark) ?: systemInDark
        val (initialStatusBarStyle, initialNavigationBarStyle) = systemBarStyles(initialIsDark)
        enableEdgeToEdge(
            statusBarStyle = initialStatusBarStyle,
            navigationBarStyle = initialNavigationBarStyle,
        )
        runCatching {
            runBlocking { authRepository.awaitInitialization() }
        }
        val initialAuthState = authRepository.snapshotAuthState()
        val initialOwnerId = runCatching {
            runBlocking { repo.currentOwnerId() }
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
            val authRepository = AuthRepository.get(this)
            val appState by repo.appState.collectAsStateWithLifecycle(
                initialValue = initialAppState ?: AppState.loadingPlaceholder(),
            )
            val appearanceMode = appState.appearanceMode
            val language = appState.language

            val systemInDark = isSystemInDarkTheme()
            val isDark = appearanceMode.isDark(systemInDark)

            // Reactively update system bar icon style so icons are visible in both light and dark.
            SideEffect {
                val (statusBarStyle, navigationBarStyle) = systemBarStyles(isDark)
                enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle,
                )
            }

            KuroLocalizationProvider(language = language) {
                PurrodoroTheme(appearanceMode = appearanceMode) {
                    PurrodoroApp(
                        repo = repo,
                        authRepository = authRepository,
                        timerConnection = timerConnection,
                        requestNotificationPermission = ::requestNotificationPermissionIfNeeded,
                        onEnableNotifications = ::promptEnableNotifications,
                        autoOpenStartFocus = autoOpenStartFocus,
                        openTab = openTab,
                        initialAuthState = initialAuthState,
                        initialOwnerId = initialOwnerId,
                        initialAppState = initialAppState,
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
        if (intent.getBooleanExtra(WidgetActions.EXTRA_AUTO_OPEN_START_FOCUS, false)) {
            _autoOpenStartFocus.tryEmit(Unit)
            // Consume the flag so a config change replay doesn't re-trigger.
            intent.removeExtra(WidgetActions.EXTRA_AUTO_OPEN_START_FOCUS)
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

    private fun systemBarStyles(isDark: Boolean): Pair<SystemBarStyle, SystemBarStyle> {
        val transparent = android.graphics.Color.TRANSPARENT
        return if (isDark) {
            SystemBarStyle.dark(transparent) to SystemBarStyle.dark(transparent)
        } else {
            SystemBarStyle.light(transparent, transparent) to SystemBarStyle.light(transparent, transparent)
        }
    }
}
