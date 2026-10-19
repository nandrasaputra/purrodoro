package com.endiar.purrodoro.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.billing.PremiumPlan
import com.endiar.purrodoro.billing.SubscriptionOffers
import com.endiar.purrodoro.model.PremiumGate
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.designsystem.LocalKuroSheetDismiss
import com.endiar.purrodoro.ui.theme.kuroTokens

@Composable
fun PremiumUpsellSheet(
    trigger: PremiumUpsellTrigger,
    onDismiss: () -> Unit,
    onUpgrade: (PremiumPlan) -> Unit,
    subscriptionOffers: SubscriptionOffers,
    selectedPlan: PremiumPlan,
    onPlanSelected: (PremiumPlan) -> Unit,
    purchaseInFlight: Boolean,
    onRefreshCatalog: () -> Unit,
    onRetryCatalog: () -> Unit,
    onExplorePremium: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    LaunchedEffect(Unit) {
        onRefreshCatalog()
    }
    val contentKey = when (val reason = trigger.reason) {
        is PremiumUpsellReason.LockedSound -> "sound:${reason.featureName}"
        is PremiumUpsellReason.LockedScene -> "scene:${reason.sceneName}"
        PremiumUpsellReason.ActiveTaskCap -> "cap:tasks"
        PremiumUpsellReason.HabitCap -> "cap:habits"
        PremiumUpsellReason.Generic -> "generic"
    }

    KuroAdaptiveBottomSheet(
        onDismiss = onDismiss,
        horizontalPadding = 0.dp,
        containerColor = tokens.cardFill,
        showCloseButton = true,
        contentKey = contentKey,
        footer = { scrollOverlay ->
            PremiumSheetStickyFooter(
                subscriptionOffers = subscriptionOffers,
                selectedPlan = selectedPlan,
                purchaseInFlight = purchaseInFlight,
                onUpgrade = onUpgrade,
                onRetryCatalog = onRetryCatalog,
                onDismiss = LocalKuroSheetDismiss.current,
                scrollOverlay = scrollOverlay,
            )
        },
    ) {
        PremiumMemberUpsellContent(
            trigger = trigger,
            subscriptionOffers = subscriptionOffers,
            selectedPlan = selectedPlan,
            onPlanSelected = onPlanSelected,
            onExplorePremium = onExplorePremium,
        )
    }
}

@Composable
private fun PremiumMemberUpsellContent(
    trigger: PremiumUpsellTrigger,
    subscriptionOffers: SubscriptionOffers,
    selectedPlan: PremiumPlan,
    onPlanSelected: (PremiumPlan) -> Unit,
    onExplorePremium: (() -> Unit)?,
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
                .padding(top = KuroSpacing.md),
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
            if (onExplorePremium != null && shouldShowCompareLink(trigger.reason)) {
                PremiumSheetCompareLink(onClick = onExplorePremium)
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
            }
            PremiumPlanPicker(
                selectedPlan = selectedPlan,
                onPlanSelected = onPlanSelected,
                subscriptionOffers = subscriptionOffers,
            )
        }
    }
}

@Composable
private fun PremiumSheetCompareLink(onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Text(
        text = stringResource(R.string.premium_sheet_compare_link),
        style = KuroType.bodySecondary.copy(textDecoration = TextDecoration.Underline),
        color = tokens.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = KuroSpacing.xs),
    )
}

private fun shouldShowCompareLink(reason: PremiumUpsellReason): Boolean = when (reason) {
    PremiumUpsellReason.ActiveTaskCap,
    PremiumUpsellReason.HabitCap,
    -> false
    else -> true
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
