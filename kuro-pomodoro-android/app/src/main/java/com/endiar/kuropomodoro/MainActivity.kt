package com.endiar.kuropomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.timer.TimerConnection
import com.endiar.kuropomodoro.timer.TimerNotification
import com.endiar.kuropomodoro.ui.theme.KuroPomodoroTheme

class MainActivity : ComponentActivity() {

    private lateinit var timerConnection: TimerConnection

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TimerNotification.ensureChannels(applicationContext)
        timerConnection = TimerConnection(applicationContext)

        setContent {
            KuroPomodoroTheme {
                KuroPomodoroApp(
                    repo = AppRepository.get(this),
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
