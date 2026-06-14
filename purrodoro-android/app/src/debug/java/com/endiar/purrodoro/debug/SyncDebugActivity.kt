package com.endiar.purrodoro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.sync.SyncModule
import com.endiar.purrodoro.sync.canSync
import kotlinx.coroutines.launch

/** Debug-only screen opened from the sync debug notification. */
class SyncDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SyncDebugScreen(
                    authRepository = AuthRepository.get(this),
                    appRepository = AppRepository.get(this),
                    onClose = { finish() },
                    onRunSync = {
                        SyncModule.get(this).syncIfEligible()
                    },
                )
            }
        }
    }
}

@Composable
private fun SyncDebugScreen(
    authRepository: AuthRepository,
    appRepository: AppRepository,
    onClose: () -> Unit,
    onRunSync: suspend () -> Unit,
) {
    val authState by authRepository.authState.collectAsStateWithLifecycle(initialValue = AuthState.Guest)
    val ownerId by appRepository.ownerIdFlow.collectAsStateWithLifecycle(initialValue = null)
    val events by SyncDebugController.events.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }

    val userId = (authState as? AuthState.SignedIn)?.user?.userId
    val eligible = canSync(userId, ownerId)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sync Debug", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }

            Text(
                text = buildString {
                    append("Signed in: ")
                    append(if (userId != null) "yes" else "no")
                    append("\nOwner id: ")
                    append(ownerId ?: "(none)")
                    append("\nEligible: ")
                    append(if (eligible) "yes" else "no")
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = {
                    scope.launch {
                        running = true
                        onRunSync()
                        running = false
                    }
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (running) "Syncing…" else "Run sync now")
            }

            HorizontalDivider()

            Text("Recent passes", style = MaterialTheme.typography.titleMedium)
            if (events.isEmpty()) {
                Text(
                    "No sync events yet. Toggle the notification in Dev Tools, then edit data or tap Run sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(events, key = { it.timestampMillis }) { event ->
                        Text(
                            text = SyncDebugController.formatEvent(event),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
