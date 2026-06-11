package com.endiar.purrodoro.ui.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.CompletableDeferred

/**
 * Bridges the [com.endiar.purrodoro.auth.AccountManager]'s suspend `confirmDrop`
 * gate to a Compose confirmation dialog.
 *
 * Signing in with a *different* account over existing on-device data is the one
 * destructive pre-sync action (it clears the previous account's local data). We
 * never run it without an explicit confirm. [confirmDrop] suspends until the
 * user answers the dialog this gate renders.
 */
class AccountSwitchGate(val confirmDrop: suspend () -> Boolean)

@Composable
fun rememberAccountSwitchGate(): AccountSwitchGate {
    var pending by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

    pending?.let { deferred ->
        val tokens = MaterialTheme.colorScheme.kuroTokens
        AlertDialog(
            onDismissRequest = {
                deferred.complete(false)
                pending = null
            },
            title = { Text(stringResource(R.string.account_switch_title)) },
            text = { Text(stringResource(R.string.account_switch_message)) },
            confirmButton = {
                TextButton(onClick = {
                    deferred.complete(true)
                    pending = null
                }) {
                    Text(
                        text = stringResource(R.string.account_switch_confirm),
                        color = tokens.destructive,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deferred.complete(false)
                    pending = null
                }) {
                    Text(stringResource(R.string.account_switch_cancel))
                }
            },
        )
    }

    return AccountSwitchGate(
        confirmDrop = {
            val deferred = CompletableDeferred<Boolean>()
            pending = deferred
            deferred.await()
        },
    )
}
