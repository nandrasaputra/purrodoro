package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.auth.SupabaseConfig
import com.endiar.purrodoro.model.AuthUser
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared Google sign-in button + loading/error state for bottom sheets. */
@Composable
fun GoogleSignInBlock(
    authRepository: AuthRepository,
    onSignedIn: (AuthUser) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSigningInChanged: (Boolean) -> Unit = {},
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.login_error_generic)
    val notConfiguredError = stringResource(R.string.login_error_not_configured)
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isSigningIn) {
        onSigningInChanged(isSigningIn)
    }

    DisposableEffect(lifecycleOwner, authRepository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isSigningIn) {
                scope.launch {
                    delay(300)
                    if (!isSigningIn) return@launch
                    if (authRepository.currentAuthUser() != null) return@launch
                    delay(1500)
                    if (isSigningIn && authRepository.currentAuthUser() == null) {
                        isSigningIn = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val googleSignIn = authRepository.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> authRepository.resolveSignedInUser { user ->
                    isSigningIn = false
                    if (user != null) {
                        onSignedIn(user)
                    } else {
                        errorMessage = genericError
                    }
                }
                is NativeSignInResult.ClosedByUser -> isSigningIn = false
                is NativeSignInResult.Error -> {
                    isSigningIn = false
                    errorMessage = result.message ?: genericError
                }
                is NativeSignInResult.NetworkError -> {
                    isSigningIn = false
                    errorMessage = result.message ?: genericError
                }
            }
        },
    )

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        GoogleSignInButton(
            enabled = enabled && !isSigningIn,
            onClick = {
                errorMessage = null
                if (!SupabaseConfig.isGoogleConfigured) {
                    errorMessage = notConfiguredError
                    return@GoogleSignInButton
                }
                isSigningIn = true
                googleSignIn.startFlow()
            },
        )
        if (isSigningIn) {
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = tokens.accent,
                strokeWidth = 2.dp,
            )
        }
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            Text(
                text = message,
                style = KuroType.labelMeta,
                color = tokens.destructive,
                textAlign = TextAlign.Center,
            )
        }
    }
}
