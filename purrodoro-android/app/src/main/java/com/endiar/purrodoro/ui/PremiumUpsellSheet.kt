package com.endiar.purrodoro.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.model.AuthUser
import com.endiar.purrodoro.model.PremiumGate
import com.endiar.purrodoro.ui.components.KuroPrimaryButton
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens

@Composable
fun PremiumUpsellSheet(
    trigger: PremiumUpsellTrigger,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    isGuest: Boolean = false,
    authRepository: AuthRepository? = null,
    onGuestSignedIn: ((AuthUser) -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var isSigningIn by remember { mutableStateOf(false) }
    val contentKey = when (val reason = trigger.reason) {
        is PremiumUpsellReason.LockedSound -> "sound:${reason.featureName}"
        is PremiumUpsellReason.LockedScene -> "scene:${reason.sceneName}"
        PremiumUpsellReason.ActiveTaskCap -> "cap:tasks"
        PremiumUpsellReason.HabitCap -> "cap:habits"
        PremiumUpsellReason.Generic -> "generic"
    } + if (isGuest) ":guest" else ":signed-in"

    KuroAdaptiveBottomSheet(
        onDismiss = {
            if (!isSigningIn) onDismiss()
        },
        horizontalPadding = 0.dp,
        containerColor = tokens.cardFill,
        showCloseButton = true,
        contentKey = contentKey,
    ) {
        if (isGuest && authRepository != null && onGuestSignedIn != null) {
            PremiumSignInGateContent(
                title = guestUnlockTitle(trigger.reason),
                body = guestUnlockBody(trigger.reason),
                isSigningIn = isSigningIn,
                authRepository = authRepository,
                onSignedIn = onGuestSignedIn,
                onDismiss = onDismiss,
                onSigningInChanged = { isSigningIn = it },
            )
        } else {
            PremiumMemberUpsellContent(
                trigger = trigger,
                onDismiss = onDismiss,
                onUpgrade = onUpgrade,
            )
        }
    }
}

@Composable
private fun PremiumMemberUpsellContent(
    trigger: PremiumUpsellTrigger,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val title = memberPaywallTitle(trigger.reason)
    val useMarketingHeadline = trigger.reason is PremiumUpsellReason.Generic

    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumSheetHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.xl)
                .padding(top = KuroSpacing.md, bottom = KuroSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (useMarketingHeadline) {
                PremiumSheetHeadline(text = title)
            } else {
                PremiumMemberTitle(text = title)
            }
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            Text(
                text = memberPaywallBody(trigger.reason),
                style = KuroType.bodyPrimary,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.lg))
            memberPaywallPerks(trigger.reason).forEachIndexed { index, perk ->
                if (index > 0) Spacer(modifier = Modifier.height(KuroSpacing.sm))
                PremiumPerkRow(
                    icon = perk.icon,
                    label = stringResource(perk.labelRes),
                )
            }
            Spacer(modifier = Modifier.height(KuroSpacing.lg))
            Text(
                text = stringResource(R.string.premium_sheet_price_hint),
                style = KuroType.labelMeta,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KuroSpacing.md))
            KuroPrimaryButton(
                text = stringResource(R.string.premium_sheet_cta),
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.premium_sheet_dismiss),
                    style = KuroType.bodySecondary,
                    color = tokens.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun guestUnlockTitle(reason: PremiumUpsellReason): String = when (reason) {
    is PremiumUpsellReason.LockedSound ->
        stringResource(R.string.premium_guest_title_locked, reason.featureName)
    is PremiumUpsellReason.LockedScene ->
        stringResource(R.string.premium_guest_title_locked, reason.sceneName)
    PremiumUpsellReason.ActiveTaskCap ->
        stringResource(R.string.premium_cap_active_tasks_title)
    PremiumUpsellReason.HabitCap ->
        stringResource(R.string.premium_cap_habits_title)
    PremiumUpsellReason.Generic ->
        stringResource(R.string.premium_guest_title_feature)
}

@Composable
private fun guestUnlockBody(reason: PremiumUpsellReason): String = when (reason) {
    is PremiumUpsellReason.LockedSound, is PremiumUpsellReason.LockedScene ->
        stringResource(R.string.premium_guest_body_locked)
    else ->
        stringResource(R.string.premium_guest_body)
}

@Composable
private fun memberPaywallTitle(reason: PremiumUpsellReason): String = when (reason) {
    is PremiumUpsellReason.LockedSound ->
        stringResource(R.string.premium_sheet_title_locked, reason.featureName)
    is PremiumUpsellReason.LockedScene ->
        stringResource(R.string.premium_sheet_title_locked, reason.sceneName)
    PremiumUpsellReason.ActiveTaskCap ->
        stringResource(R.string.premium_cap_active_tasks_title)
    PremiumUpsellReason.HabitCap ->
        stringResource(R.string.premium_cap_habits_title)
    PremiumUpsellReason.Generic ->
        stringResource(R.string.premium_sheet_title)
}

@Composable
private fun memberPaywallBody(reason: PremiumUpsellReason): String = when (reason) {
    is PremiumUpsellReason.LockedSound, is PremiumUpsellReason.LockedScene ->
        stringResource(R.string.premium_sheet_subtitle_locked)
    PremiumUpsellReason.ActiveTaskCap ->
        stringResource(R.string.premium_cap_active_tasks_body, PremiumGate.FREE_ACTIVE_TASK_LIMIT)
    PremiumUpsellReason.HabitCap ->
        stringResource(R.string.premium_cap_habits_body, PremiumGate.FREE_HABIT_LIMIT)
    else ->
        stringResource(R.string.premium_sheet_subtitle)
}

private data class PaywallPerk(
    val labelRes: Int,
    val icon: @Composable () -> Unit,
)

@Composable
private fun memberPaywallPerks(reason: PremiumUpsellReason): List<PaywallPerk> {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    return when (reason) {
        PremiumUpsellReason.ActiveTaskCap -> listOf(
            PaywallPerk(R.string.premium_cap_perk_unlimited_tasks) {
                Icon(Lucide.ListTodo, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
            PaywallPerk(R.string.premium_perk_sounds) {
                Icon(Lucide.AudioLines, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
        )
        PremiumUpsellReason.HabitCap -> listOf(
            PaywallPerk(R.string.premium_cap_perk_unlimited_habits) {
                Icon(Lucide.Leaf, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
            PaywallPerk(R.string.premium_perk_sounds) {
                Icon(Lucide.AudioLines, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
        )
        is PremiumUpsellReason.LockedScene -> listOf(
            PaywallPerk(R.string.premium_perk_scenes) {
                Icon(Lucide.Layers, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
        )
        is PremiumUpsellReason.LockedSound -> listOf(
            PaywallPerk(R.string.premium_perk_sounds) {
                Icon(Lucide.AudioLines, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
        )
        else -> listOf(
            PaywallPerk(R.string.premium_perk_sounds) {
                Icon(Lucide.AudioLines, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
            PaywallPerk(R.string.premium_perk_scenes) {
                Icon(Lucide.Layers, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
            },
        )
    }
}
