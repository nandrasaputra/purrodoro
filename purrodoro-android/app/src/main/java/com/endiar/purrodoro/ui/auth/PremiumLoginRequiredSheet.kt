package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Smartphone
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

/**
 * Shown when a guest tries to access Premium. Explains cloud sync benefits, then
 * routes to Google sign-in. Distinct from [LoginSheet] (profile / onboarding).
 */
@Composable
fun PremiumLoginRequiredSheet(
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
    onSignedIn: (AuthUser) -> Unit,
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
                text = stringResource(R.string.premium_login_title),
                style = KuroType.headingScreen.copy(
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tokens.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = stringResource(R.string.premium_login_subtitle),
                style = KuroType.bodyPrimary,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.lg))
            Image(
                painter = painterResource(id = R.drawable.cat_login_wave),
                contentDescription = stringResource(R.string.login_sheet_cat_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(88.dp),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.md))
            PremiumLoginSyncPerk(
                icon = { Icon(Lucide.Cloud, null, tint = tokens.accent, modifier = Modifier.size(18.dp)) },
                label = stringResource(R.string.premium_login_perk_sync_data),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            PremiumLoginSyncPerk(
                icon = { Icon(Lucide.RefreshCw, null, tint = tokens.accent, modifier = Modifier.size(18.dp)) },
                label = stringResource(R.string.premium_login_perk_journey),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            PremiumLoginSyncPerk(
                icon = { Icon(Lucide.Smartphone, null, tint = tokens.accent, modifier = Modifier.size(18.dp)) },
                label = stringResource(R.string.premium_login_perk_devices),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.lg))
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
            TextButton(onClick = onDismiss, enabled = !isSigningIn) {
                Text(
                    text = stringResource(R.string.premium_login_dismiss),
                    style = KuroType.bodySecondary,
                    color = tokens.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun PremiumLoginSyncPerk(
    icon: @Composable () -> Unit,
    label: String,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
            .background(tokens.pageBackground.copy(alpha = 0.65f))
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm + KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tokens.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = label,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}
