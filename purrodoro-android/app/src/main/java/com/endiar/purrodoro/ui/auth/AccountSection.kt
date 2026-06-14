package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.AuthProvider
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.sync.SyncStatusPresentation
import com.endiar.purrodoro.ui.SyncStatusMiniPanel
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens

@Composable
fun AccountSection(
    authState: AuthState,
    onRequestLogin: () -> Unit,
    onSignOut: () -> Unit,
    syncPresentation: SyncStatusPresentation? = null,
    modifier: Modifier = Modifier,
) {
    when (authState) {
        AuthState.Guest -> GuestAccountCard(onRequestLogin = onRequestLogin, modifier = modifier)
        is AuthState.SignedIn -> when {
            syncPresentation != null -> SignedInAccountWithSyncCard(
                authState = authState,
                syncPresentation = syncPresentation,
                onSignOut = onSignOut,
                modifier = modifier,
            )
            else -> SignedInAccountCard(
                authState = authState,
                onSignOut = onSignOut,
                modifier = modifier,
            )
        }
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
            Image(
                painter = painterResource(id = R.drawable.cat_profile_guest_icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp),
            )
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

@Composable
private fun SignedInAccountCard(
    authState: AuthState.SignedIn,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KuroCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        ) {
            SignedInAccountIdentityRow(authState = authState)
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            SignedInAccountSignOutRow(onSignOut = onSignOut)
        }
    }
}

@Composable
private fun SignedInAccountWithSyncCard(
    authState: AuthState.SignedIn,
    syncPresentation: SyncStatusPresentation,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KuroSpacing.radiusLg),
        colors = CardDefaults.cardColors(containerColor = tokens.cardFill),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .padding(
                            start = KuroSpacing.lg,
                            end = KuroSpacing.sm,
                            top = KuroSpacing.lg,
                            bottom = KuroSpacing.md,
                        ),
                ) {
                    SignedInAccountIdentityRow(authState = authState)
                }
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .background(
                            color = tokens.accentSoft,
                            shape = RoundedCornerShape(
                                topEnd = KuroSpacing.radiusLg,
                                bottomStart = KuroSpacing.radiusSm,
                            ),
                        )
                        .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    SyncStatusMiniPanel(presentation = syncPresentation)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.divider),
            )
            SignedInAccountSignOutRow(
                onSignOut = onSignOut,
                modifier = Modifier
                    .padding(horizontal = KuroSpacing.lg)
                    .padding(top = KuroSpacing.xs, bottom = KuroSpacing.md),
            )
        }
    }
}

@Composable
private fun SignedInAccountIdentityRow(
    authState: AuthState.SignedIn,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val user = authState.user
    val displayName = user.displayName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.account_default_name)
    val providerLabel = when (user.provider) {
        AuthProvider.Google -> stringResource(R.string.account_signed_in_with_google)
        AuthProvider.Unknown -> stringResource(R.string.account_signed_in_generic)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountInitialAvatar(
            displayName = displayName,
            modifier = Modifier.size(52.dp),
        )
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            user.email?.let { email ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email,
                    style = KuroType.labelMeta,
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                if (user.provider == AuthProvider.Google) {
                    GoogleBrandIcon(modifier = Modifier.size(16.dp))
                }
                Text(
                    text = providerLabel,
                    style = KuroType.labelMeta,
                    color = tokens.textMuted,
                )
            }
        }
    }
}

@Composable
private fun SignedInAccountSignOutRow(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onSignOut) {
            Text(
                text = stringResource(R.string.account_sign_out),
                style = KuroType.labelStrong,
                color = tokens.accent,
            )
        }
    }
}

@Composable
private fun AccountInitialAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(tokens.accent, tokens.interactiveFill),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = KuroType.headingCard.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.interactiveText,
        )
    }
}
