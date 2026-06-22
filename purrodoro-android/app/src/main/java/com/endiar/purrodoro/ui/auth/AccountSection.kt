package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.model.AuthUser
import com.endiar.purrodoro.sync.SyncStatusPresentation
import com.endiar.purrodoro.ui.ProfileIdentityCard
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroAccountAvatar
import com.endiar.purrodoro.ui.designsystem.KuroGuestAvatarContent
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens

@Composable
fun AccountSection(
    authState: AuthState,
    onRequestLogin: () -> Unit,
    onSignOut: () -> Unit,
    syncPresentation: SyncStatusPresentation? = null,
    isPremium: Boolean = false,
    onOpenPremium: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (authState) {
        AuthState.Loading -> Unit
        AuthState.Guest -> GuestAccountCard(onRequestLogin = onRequestLogin, modifier = modifier)
        is AuthState.SignedIn -> ProfileIdentityCard(
            authState = authState,
            isPremium = isPremium,
            syncPresentation = syncPresentation,
            onSignOut = onSignOut,
            onOpenPremium = onOpenPremium,
            modifier = modifier,
        )
    }
}

@Composable
private fun GuestAccountCard(
    onRequestLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRequestLogin),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KuroGuestAvatarContent(modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.width(KuroSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.account_sign_in_title),
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.textPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.account_sign_in_subtitle),
                    style = KuroType.labelMeta,
                    color = tokens.textSecondary,
                )
            }
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(KuroSpacing.lg),
            )
        }
    }
}

/** Compact signed-in identity row (avatar, name, email). */
@Composable
internal fun SignedInAccountSummary(
    user: AuthUser,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val displayName = user.displayName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.account_default_name)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KuroAccountAvatar(
            userId = user.userId,
            displayName = displayName,
            isPremium = isPremium,
            size = 52.dp,
        )
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.textPrimary,
            )
            user.email?.let { email ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email,
                    style = KuroType.labelMeta,
                    color = tokens.textSecondary,
                )
            }
        }
    }
}
