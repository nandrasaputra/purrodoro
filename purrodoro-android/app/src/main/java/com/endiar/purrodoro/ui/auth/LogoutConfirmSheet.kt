package com.endiar.purrodoro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.sync.LogoutDecision
import com.endiar.purrodoro.ui.components.KuroPrimaryButton
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.launch

private enum class LogoutSheetStep {
    Confirm,
    Unsynced,
    SigningOut,
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
    onSignOut: suspend () -> Unit,
) {
    if (!visible) return

    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(LogoutSheetStep.Confirm) }

    LaunchedEffect(visible) {
        if (visible) step = LogoutSheetStep.Confirm
    }

    suspend fun finishSignOut() {
        step = LogoutSheetStep.SigningOut
        onSignOut()
        step = LogoutSheetStep.Confirm
        onDismiss()
    }

    KuroAdaptiveBottomSheet(
        onDismiss = {
            if (step != LogoutSheetStep.SigningOut) {
                step = LogoutSheetStep.Confirm
                onDismiss()
            }
        },
        horizontalPadding = KuroSpacing.xl,
        containerColor = tokens.cardFill,
        showCloseButton = step != LogoutSheetStep.SigningOut,
    ) {
        when (step) {
            LogoutSheetStep.Confirm -> ConfirmStep(
                onCancel = onDismiss,
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
                onKeepSyncing = onDismiss,
                onSignOutAnyway = {
                    scope.launch { finishSignOut() }
                },
            )

            LogoutSheetStep.SigningOut -> SigningOutStep()
        }
    }
}

@Composable
private fun ConfirmStep(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.sm, bottom = KuroSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.logout_confirm_title),
            style = KuroType.headingScreen.copy(
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(KuroSpacing.sm))
        Text(
            text = stringResource(R.string.logout_confirm_body),
            style = KuroType.bodyPrimary,
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.xs))
        Text(
            text = stringResource(R.string.logout_confirm_footnote),
            style = KuroType.labelMeta,
            color = tokens.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.xxl))
        Image(
            painter = painterResource(id = R.drawable.cat_logout_rest),
            contentDescription = stringResource(R.string.logout_sheet_cat_cd),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(92.dp),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.lg))
        KuroPrimaryButton(
            text = stringResource(R.string.logout_confirm_action),
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.sm))
        Text(
            text = stringResource(R.string.common_cancel),
            style = KuroType.labelStrong,
            color = tokens.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onCancel,
                )
                .padding(vertical = KuroSpacing.xs),
        )
    }
}

@Composable
private fun UnsyncedStep(
    onKeepSyncing: () -> Unit,
    onSignOutAnyway: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.sm, bottom = KuroSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.logout_unsynced_title),
            style = KuroType.headingScreen.copy(
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(KuroSpacing.sm))
        Text(
            text = stringResource(R.string.logout_unsynced_body),
            style = KuroType.bodyPrimary,
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.xxl))
        Image(
            painter = painterResource(id = R.drawable.cat_logout_rest),
            contentDescription = stringResource(R.string.logout_sheet_cat_cd),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(92.dp),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.lg))
        KuroPrimaryButton(
            text = stringResource(R.string.logout_unsynced_dismiss),
            onClick = onKeepSyncing,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.sm))
        Text(
            text = stringResource(R.string.logout_unsynced_confirm),
            style = KuroType.labelStrong,
            color = tokens.destructive,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onSignOutAnyway,
                )
                .padding(vertical = KuroSpacing.xs),
        )
    }
}

@Composable
private fun SigningOutStep() {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.lg, bottom = KuroSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = tokens.accent,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.height(KuroSpacing.md))
        Text(
            text = stringResource(R.string.logout_signing_out),
            style = KuroType.bodyPrimary,
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
