package com.endiar.purrodoro.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.billing.BillingCatalogState
import com.endiar.purrodoro.billing.PlanOfferDetails
import com.endiar.purrodoro.billing.PremiumPlan
import com.endiar.purrodoro.billing.SubscriptionOffers
import com.endiar.purrodoro.billing.TrialPeriod
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.Coral
import com.endiar.purrodoro.ui.theme.JourneyGreen
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottom
import com.endiar.purrodoro.ui.theme.kuroTokens

enum class PlanPickerLayout {
    Compact,
    Expanded,
}

@Composable
fun PremiumPlanPicker(
    selectedPlan: PremiumPlan,
    onPlanSelected: (PremiumPlan) -> Unit,
    subscriptionOffers: SubscriptionOffers,
    modifier: Modifier = Modifier,
    layout: PlanPickerLayout = PlanPickerLayout.Compact,
) {
    when (subscriptionOffers.catalogState) {
        BillingCatalogState.Idle,
        BillingCatalogState.Loading,
        -> PlanPickerLoading(layout = layout, modifier = modifier)
        BillingCatalogState.Unavailable -> PlanPickerUnavailable(modifier = modifier)
        BillingCatalogState.Ready -> when (layout) {
            PlanPickerLayout.Compact -> CompactPlanPicker(
                selectedPlan = selectedPlan,
                onPlanSelected = onPlanSelected,
                subscriptionOffers = subscriptionOffers,
                modifier = modifier,
            )
            PlanPickerLayout.Expanded -> ExpandedPlanPicker(
                selectedPlan = selectedPlan,
                onPlanSelected = onPlanSelected,
                subscriptionOffers = subscriptionOffers,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CompactPlanPicker(
    selectedPlan: PremiumPlan,
    onPlanSelected: (PremiumPlan) -> Unit,
    subscriptionOffers: SubscriptionOffers,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        PremiumPlan.entries.forEach { plan ->
            PlanCard(
                plan = plan,
                selected = plan == selectedPlan,
                details = subscriptionOffers.forPlan(plan),
                savingsPercent = if (plan == PremiumPlan.Yearly) subscriptionOffers.savingsPercent else null,
                onClick = { onPlanSelected(plan) },
                equalHeight = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ExpandedPlanPicker(
    selectedPlan: PremiumPlan,
    onPlanSelected: (PremiumPlan) -> Unit,
    subscriptionOffers: SubscriptionOffers,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        PlanCard(
            plan = PremiumPlan.Yearly,
            selected = selectedPlan == PremiumPlan.Yearly,
            details = subscriptionOffers.yearly,
            savingsPercent = subscriptionOffers.savingsPercent,
            onClick = { onPlanSelected(PremiumPlan.Yearly) },
            modifier = Modifier.fillMaxWidth(),
        )
        PlanCard(
            plan = PremiumPlan.Monthly,
            selected = selectedPlan == PremiumPlan.Monthly,
            details = subscriptionOffers.monthly,
            savingsPercent = null,
            onClick = { onPlanSelected(PremiumPlan.Monthly) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    selected: Boolean,
    details: PlanOfferDetails,
    savingsPercent: Int?,
    onClick: () -> Unit,
    equalHeight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val label = when (plan) {
        PremiumPlan.Monthly -> stringResource(R.string.premium_screen_plan_monthly)
        PremiumPlan.Yearly -> stringResource(R.string.premium_screen_plan_yearly)
    }
    val shape = RoundedCornerShape(KuroSpacing.radiusMd)
    Box(
        modifier = modifier
            .heightIn(min = PlanCardMinHeight)
            .clip(shape)
            .background(if (selected) tokens.lockedFill else tokens.cardFill)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, PremiumBadgeGradientBottom.copy(alpha = 0.55f), shape)
                } else {
                    Modifier.border(1.dp, tokens.divider, shape)
                },
            )
            .clickable(onClick = onClick),
    ) {
        PlanSelectionRadio(
            selected = selected,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(KuroSpacing.sm),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (equalHeight) {
                        Modifier.fillMaxHeight()
                    } else {
                        Modifier.align(Alignment.Center)
                    },
                )
                .padding(horizontal = KuroSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PlanCardContent(
                plan = plan,
                label = label,
                details = details,
                savingsPercent = savingsPercent,
                emphasize = selected,
            )
        }
    }
}

@Composable
private fun PlanCardContent(
    plan: PremiumPlan,
    label: String,
    details: PlanOfferDetails,
    savingsPercent: Int?,
    emphasize: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val heroColor = if (emphasize) tokens.lockedText else tokens.textPrimary
    val heroText: String?
    val secondaryText: String?
    val captionText: String?

    when {
        plan == PremiumPlan.Yearly && details.hasTrial -> {
            heroText = trialFreeForText(details.trialPeriod)
            secondaryText = details.secondaryPriceLine?.let { price ->
                stringResource(R.string.premium_price_then_yearly, price)
            }
            captionText = savingsPercent?.let { savingsVsMonthlyCaption(it) }
        }
        plan == PremiumPlan.Yearly && details.priceLine != null -> {
            heroText = stringResource(R.string.premium_price_per_year, details.priceLine)
            secondaryText = null
            captionText = savingsPercent?.let { savingsVsMonthlyCaption(it) }
        }
        plan == PremiumPlan.Monthly && details.priceLine != null -> {
            heroText = stringResource(R.string.premium_price_per_month, details.priceLine)
            secondaryText = null
            captionText = stringResource(R.string.premium_plan_billed_monthly)
        }
        else -> {
            heroText = null
            secondaryText = null
            captionText = null
        }
    }

    PlanCardBadgeSlot(
        plan = plan,
        details = details,
        savingsPercent = savingsPercent,
    )
    Spacer(modifier = Modifier.height(KuroSpacing.xs))
    PlanCardTextSlot(minHeight = PlanTitleSlotHeight) {
        Text(
            text = label,
            style = KuroType.bodyPrimary.copy(
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = heroColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
    PlanCardTextSlot(minHeight = PlanHeroSlotHeight) {
        if (heroText != null) {
            Text(
                text = heroText,
                style = KuroType.labelStrong.copy(fontWeight = FontWeight.SemiBold),
                color = heroColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
    PlanCardTextSlot(minHeight = PlanSecondarySlotHeight) {
        if (secondaryText != null) {
            Text(
                text = secondaryText,
                style = KuroType.bodySecondary,
                color = tokens.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
    PlanCardTextSlot(minHeight = PlanCaptionSlotHeight) {
        if (captionText != null) {
            Text(
                text = captionText,
                style = KuroType.labelMeta.copy(
                    fontWeight = if (plan == PremiumPlan.Yearly && savingsPercent != null) {
                        FontWeight.Medium
                    } else {
                        FontWeight.Normal
                    },
                ),
                color = if (plan == PremiumPlan.Yearly && savingsPercent != null) {
                    JourneyGreen
                } else {
                    tokens.textMuted
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PlanCardTextSlot(
    minHeight: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Min height fits badge slot and fixed text rows. */
private val PlanCardMinHeight = 132.dp

private val PlanBadgeSlotHeight = 26.dp
private val PlanTitleSlotHeight = 22.dp
private val PlanHeroSlotHeight = 22.dp
private val PlanSecondarySlotHeight = 20.dp
private val PlanCaptionSlotHeight = 18.dp

@Composable
private fun PlanSelectionRadio(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val ringColor = if (selected) PremiumBadgeGradientBottom else tokens.divider
    Box(
        modifier = modifier
            .size(18.dp)
            .border(width = 1.5.dp, color = ringColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PremiumBadgeGradientBottom),
            )
        }
    }
}

@Composable
private fun PlanCardBadgeSlot(
    plan: PremiumPlan,
    details: PlanOfferDetails,
    savingsPercent: Int?,
) {
    Box(
        modifier = Modifier
            .heightIn(min = PlanBadgeSlotHeight)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            plan == PremiumPlan.Yearly && details.hasTrial ->
                TrialPill(trialPeriod = details.trialPeriod)
            plan == PremiumPlan.Yearly && !details.hasTrial && savingsPercent == null ->
                BestValueBadge()
            else -> Unit
        }
    }
}

@Composable
private fun BestValueBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(JourneyGreen.copy(alpha = 0.14f))
            .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.premium_plan_best_value),
            style = KuroType.labelMeta.copy(fontWeight = FontWeight.SemiBold),
            color = JourneyGreen,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrialPill(trialPeriod: TrialPeriod?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(Coral.copy(alpha = 0.18f))
            .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
    ) {
        Text(
            text = trialPillText(trialPeriod),
            style = KuroType.labelMeta.copy(fontWeight = FontWeight.SemiBold),
            color = Coral,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlanPickerLoading(
    layout: PlanPickerLayout,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val transition = rememberInfiniteTransition(label = "plan_picker_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )
    val shimmerModifier = Modifier
        .clip(RoundedCornerShape(KuroSpacing.radiusMd))
        .background(tokens.divider.copy(alpha = alpha))
    when (layout) {
        PlanPickerLayout.Compact -> Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = PlanCardMinHeight)
                    .then(shimmerModifier),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = PlanCardMinHeight)
                    .then(shimmerModifier),
            )
        }
        PlanPickerLayout.Expanded -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PlanCardMinHeight)
                    .then(shimmerModifier),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PlanCardMinHeight)
                    .then(shimmerModifier),
            )
        }
    }
}

@Composable
private fun PlanPickerUnavailable(modifier: Modifier = Modifier) {
    PremiumCatalogErrorPanel(modifier = modifier)
}
