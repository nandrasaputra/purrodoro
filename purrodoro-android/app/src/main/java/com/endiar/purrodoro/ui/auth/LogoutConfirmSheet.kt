package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.endiar.purrodoro.R
import com.endiar.purrodoro.sync.LogoutDecision
import com.endiar.purrodoro.ui.components.KuroPrimaryButton
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.designsystem.LocalKuroSheetDismiss
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.launch

private enum class LogoutSheetStep {
    Confirm,
    Unsynced,
    SigningOut,
    Error,
}

/**
 * Branded sign-out sheet: confirm intent, flush sync, and (only when needed)
 * warn about unsynced rows — all in one surface instead of stacked alert dialogs.
 */
@Composable
fun LogoutConfirmSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    flushBeforeLogout: suspend () -> LogoutDecision,
    onSignOut: suspend () -> Boolean,
) {
    if (!visible) return

    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(LogoutSheetStep.Confirm) }

    LaunchedEffect(visible) {
        if (visible) step = LogoutSheetStep.Confirm
    }

    KuroAdaptiveBottomSheet(
        onDismiss = {
            if (step != LogoutSheetStep.SigningOut) {
                step = LogoutSheetStep.Confirm
                onDismiss()
            }
        },
        dismissBlocked = step == LogoutSheetStep.SigningOut,
        horizontalPadding = KuroSpacing.xl,
        containerColor = tokens.cardFill,
        showCloseButton = step != LogoutSheetStep.SigningOut,
    ) {
        val dismissSheet = LocalKuroSheetDismiss.current

        suspend fun finishSignOut() {
            step = LogoutSheetStep.SigningOut
            val signedOut = onSignOut()
            if (signedOut) {
                step = LogoutSheetStep.Confirm
                dismissSheet()
            } else {
                step = LogoutSheetStep.Error
            }
        }

        when (step) {
            LogoutSheetStep.Confirm -> ConfirmStep(
                onCancel = dismissSheet,
                onConfirm = {
                    scope.launch {
                        step = LogoutSheetStep.SigningOut
                        when (flushBeforeLogout()) {
                            LogoutDecision.SignOut -> finishSignOut()
                            LogoutDecision.WarnUnsynced -> step = LogoutSheetStep.Unsynced
                        }
                    }
                },
            )

            LogoutSheetStep.Unsynced -> UnsyncedStep(
                onKeepSyncing = dismissSheet,
                onSignOutAnyway = {
                    scope.launch { finishSignOut() }
                },
            )

            LogoutSheetStep.SigningOut -> SigningOutStep()

            LogoutSheetStep.Error -> ErrorStep(
                onDismiss = dismissSheet,
                onRetry = {
                    scope.launch { finishSignOut() }
                },
            )
        }
    }
}

@Composable
private fun ConfirmStep(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    LogoutSheetScaffold(
        title = stringResource(R.string.logout_confirm_title),
        body = stringResource(R.string.logout_confirm_body),
        infoRows = {
            LogoutInfoRow(
                icon = { Icon(Lucide.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = stringResource(R.string.logout_confirm_info_local),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            LogoutInfoRow(
                icon = {
                    GoogleBrandIcon(
                        modifier = Modifier.size(18.dp),
                        contentDescription = null,
                    )
                },
                text = stringResource(R.string.logout_confirm_footnote),
            )
        },
        primaryText = stringResource(R.string.logout_confirm_action),
        onPrimary = onConfirm,
        primaryDestructive = true,
        secondaryText = stringResource(R.string.common_cancel),
        onSecondary = onCancel,
        showInfoBox = true,
    )
}

@Composable
private fun UnsyncedStep(
    onKeepSyncing: () -> Unit,
    onSignOutAnyway: () -> Unit,
) {
    LogoutSheetScaffold(
        title = stringResource(R.string.logout_unsynced_title),
        body = stringResource(R.string.logout_unsynced_body),
        infoRows = {
            LogoutInfoRow(
                icon = { Icon(Lucide.Cloud, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = stringResource(R.string.logout_unsynced_info_pending),
            )
        },
        primaryText = stringResource(R.string.logout_unsynced_dismiss),
        onPrimary = onKeepSyncing,
        primaryDestructive = false,
        secondaryText = stringResource(R.string.logout_unsynced_confirm),
        onSecondary = onSignOutAnyway,
        secondaryDestructive = true,
        showInfoBox = true,
    )
}

@Composable
private fun ErrorStep(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    LogoutSheetScaffold(
        title = stringResource(R.string.logout_error_title),
        body = stringResource(R.string.logout_error_body),
        infoRows = {},
        primaryText = stringResource(R.string.logout_error_retry),
        onPrimary = onRetry,
        primaryDestructive = true,
        secondaryText = stringResource(R.string.common_cancel),
        onSecondary = onDismiss,
        showInfoBox = false,
    )
}

@Composable
private fun LogoutSheetScaffold(
    title: String,
    body: String,
    infoRows: @Composable () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    primaryDestructive: Boolean,
    secondaryText: String,
    onSecondary: () -> Unit,
    secondaryDestructive: Boolean = false,
    showInfoBox: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xs, bottom = KuroSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = KuroType.headingScreen.copy(
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.sm))
        Text(
            text = body,
            style = KuroType.bodyPrimary,
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (showInfoBox) {
            Spacer(modifier = Modifier.height(KuroSpacing.md))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                    .background(tokens.pageBackground.copy(alpha = 0.55f))
                    .border(1.dp, tokens.borderSoft, RoundedCornerShape(KuroSpacing.radiusMd))
                    .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
            ) {
                infoRows()
            }
        }
        Spacer(modifier = Modifier.height(KuroSpacing.lg))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuroSpacing.radiusLg))
                .background(tokens.accentSoft.copy(alpha = 0.35f))
                .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat_logout_rest),
                contentDescription = stringResource(R.string.logout_sheet_cat_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(112.dp),
            )
        }
        Spacer(modifier = Modifier.height(KuroSpacing.lg))
        if (primaryDestructive) {
            LogoutDestructiveButton(
                text = primaryText,
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            KuroPrimaryButton(
                text = primaryText,
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(
            onClick = onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KuroSpacing.xs),
        ) {
            Text(
                text = secondaryText,
                style = KuroType.labelStrong,
                color = if (secondaryDestructive) tokens.destructive else tokens.textSecondary,
            )
        }
    }
}

@Composable
private fun LogoutInfoRow(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tokens.accentSoft.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            style = KuroType.bodySecondary.copy(fontWeight = FontWeight.Medium),
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LogoutDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(KuroSpacing.radiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.destructive,
            contentColor = tokens.interactiveText,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SigningOutStep() {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xxl, bottom = KuroSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = tokens.accent,
            strokeWidth = 2.5.dp,
        )
        Spacer(modifier = Modifier.height(KuroSpacing.md))
        Text(
            text = stringResource(R.string.logout_signing_out),
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Medium),
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
