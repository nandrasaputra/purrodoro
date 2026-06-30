package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.auth.AuthDiagnostics
import com.endiar.purrodoro.auth.SupabaseConfig
import com.endiar.purrodoro.model.AuthUser
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle

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
    var signInAttempt by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.login_error_generic)
    val timedOutError = stringResource(R.string.login_error_timed_out)
    val notConfiguredError = stringResource(R.string.login_error_not_configured)

    LaunchedEffect(isSigningIn) {
        onSigningInChanged(isSigningIn)
    }

    GoogleSignInResumeTimeoutEffect(
        authRepository = authRepository,
        isSigningIn = isSigningIn,
        signInAttempt = signInAttempt,
        onTimedOut = {
            isSigningIn = false
            errorMessage = timedOutError
        },
    )

    val googleSignIn = authRepository.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            signInAttempt++
            when (result) {
                is NativeSignInResult.Success -> {
                    AuthDiagnostics.log("google_sign_in_success")
                    authRepository.resolveSignedInUser { user ->
                        isSigningIn = false
                        if (user != null) {
                            errorMessage = null
                            AuthDiagnostics.log("google_sign_in_resolved", user.userId)
                            onSignedIn(user)
                        } else {
                            AuthDiagnostics.log("google_sign_in_resolve_null")
                            errorMessage = genericError
                        }
                    }
                }
                is NativeSignInResult.ClosedByUser -> {
                    AuthDiagnostics.log("google_sign_in_closed")
                    isSigningIn = false
                }
                is NativeSignInResult.Error -> {
                    AuthDiagnostics.log("google_sign_in_error", result.message)
                    isSigningIn = false
                    errorMessage = result.message ?: genericError
                }
                is NativeSignInResult.NetworkError -> {
                    AuthDiagnostics.log("google_sign_in_network_error", result.message)
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
                signInAttempt++
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
