package com.endiar.purrodoro.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.model.AuthUser
import com.endiar.purrodoro.ui.PremiumSignInGateContent
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.LocalKuroSheetDismiss
import com.endiar.purrodoro.ui.theme.kuroTokens
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Lightweight sign-in gate when a guest opens Premium discovery from Profile.
 * Feature-level premium attempts use [com.endiar.purrodoro.ui.PremiumUpsellSheet] instead.
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

    KuroAdaptiveBottomSheet(
        onDismiss = onDismiss,
        dismissBlocked = isSigningIn,
        horizontalPadding = 0.dp,
        containerColor = tokens.cardFill,
        showCloseButton = true,
    ) {
        val dismissSheet = LocalKuroSheetDismiss.current
        PremiumSignInGateContent(
            title = stringResource(R.string.premium_login_discover_title),
            body = stringResource(R.string.premium_login_discover_subtitle),
            isSigningIn = isSigningIn,
            authRepository = authRepository,
            onSignedIn = onSignedIn,
            onDismiss = dismissSheet,
            onSigningInChanged = {
                isSigningIn = it
                onSigningInChanged(it)
            },
        )
    }
}
