package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.auth.SupabaseConfig
import com.endiar.purrodoro.model.AuthUser
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginSheet(
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
    onSignedIn: (AuthUser) -> Unit,
    onContinueAsGuest: () -> Unit,
    fallbackDisplayName: String = "",
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val genericError = stringResource(R.string.login_error_generic)
    val notConfiguredError = stringResource(R.string.login_error_not_configured)
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Returning from the Google account picker / browser may not deliver a ComposeAuth
    // callback before the user presses back. Clear the signing-in gate on resume so
    // the sheet can always dismiss and tear down its Dialog scrim.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isSigningIn) {
                scope.launch {
                    delay(500)
                    isSigningIn = false
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
                    // Resolution runs on AuthRepository's application scope, so this
                    // fires even if the sheet was already dismissed — sign-in
                    // succeeded server-side and the AccountManager handoff must run.
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

    KuroAdaptiveBottomSheet(
        onDismiss = {
            isSigningIn = false
            onDismiss()
        },
        horizontalPadding = KuroSpacing.xl,
        containerColor = tokens.cardFill,
        showCloseButton = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KuroSpacing.sm, bottom = KuroSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.login_sheet_title),
                style = KuroType.headingScreen.copy(
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tokens.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = stringResource(R.string.login_sheet_subtitle),
                style = KuroType.bodyPrimary,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xxl))
            Image(
                painter = painterResource(id = R.drawable.cat_login_wave),
                contentDescription = stringResource(R.string.login_sheet_cat_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(92.dp),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.md))
            GoogleSignInButton(
                enabled = !isSigningIn,
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
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            Text(
                text = stringResource(R.string.login_continue_as_guest),
                style = KuroType.labelStrong.copy(textDecoration = TextDecoration.Underline),
                color = tokens.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(
                        enabled = !isSigningIn,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { if (!isSigningIn) onContinueAsGuest() },
                    )
                    .padding(vertical = KuroSpacing.xs),
            )
            Text(
                text = stringResource(R.string.login_guest_not_backed_up),
                style = KuroType.labelMeta,
                color = tokens.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(KuroSpacing.radiusPill), clip = false)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusPill))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        GoogleBrandIcon(modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(KuroSpacing.sm))
        Text(
            text = stringResource(R.string.login_continue_with_google),
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.textPrimary,
        )
    }
}
