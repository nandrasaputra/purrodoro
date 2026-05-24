package com.endiar.kuropomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.AppearanceMode
import com.endiar.kuropomodoro.timer.TimerConnection
import com.endiar.kuropomodoro.timer.TimerNotification
import com.endiar.kuropomodoro.ui.theme.KuroPomodoroTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private lateinit var timerConnection: TimerConnection

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val repo = AppRepository.get(this)
        val soundEnabled = runCatching {
            kotlinx.coroutines.runBlocking { repo.appState.first().notificationSoundEnabled }
        }.getOrDefault(true)
        TimerNotification.setNotificationSoundEnabled(applicationContext, soundEnabled)
        timerConnection = TimerConnection(applicationContext)

        setContent {
            val repo = AppRepository.get(this)
            val appearanceMode by repo.appState
                .map { it.appearanceMode }
                .collectAsStateWithLifecycle(initialValue = AppearanceMode.Auto)

            KuroPomodoroTheme(appearanceMode = appearanceMode) {
                KuroPomodoroApp(
                    repo = repo,
                    timerConnection = timerConnection,
                    requestNotificationPermission = ::requestNotificationPermissionIfNeeded
                )
            }
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
