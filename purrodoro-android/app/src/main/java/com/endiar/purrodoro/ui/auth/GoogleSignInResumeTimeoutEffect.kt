package com.endiar.purrodoro.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.endiar.purrodoro.auth.AuthDiagnostics
import com.endiar.purrodoro.auth.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cancels a stale resume-timeout when [signInAttempt] bumps (flow start / finish).
 *
 * Google account picker + token exchange can take many seconds. The old 1.8s
 * watchdog fired before [NativeSignInResult.Success] on release builds and
 * showed a false timeout while sign-in was still completing.
 */
@Composable
internal fun GoogleSignInResumeTimeoutEffect(
    authRepository: AuthRepository,
    isSigningIn: Boolean,
    signInAttempt: Int,
    onTimedOut: () -> Unit,
) {
    if (!isSigningIn) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner, authRepository, isSigningIn, signInAttempt) {
        var watchJob: Job? = null
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            watchJob?.cancel()
            val attemptAtResume = signInAttempt
            watchJob = scope.launch {
                delay(500)
                if (attemptAtResume != signInAttempt) return@launch
                if (authRepository.currentAuthUser() != null) return@launch
                repeat(60) {
                    delay(1_000)
                    if (attemptAtResume != signInAttempt) return@launch
                    if (authRepository.currentAuthUser() != null) return@launch
                }
                if (attemptAtResume == signInAttempt &&
                    authRepository.currentAuthUser() == null
                ) {
                    AuthDiagnostics.log("google_sign_in_resume_timeout")
                    onTimedOut()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            watchJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
