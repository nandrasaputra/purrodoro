package com.endiar.purrodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.endiar.purrodoro.R
import com.endiar.purrodoro.billing.PremiumPlan
import com.endiar.purrodoro.billing.SubscriptionOffers
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens

@Composable
fun PremiumPlanPicker(
    selectedPlan: PremiumPlan,
    onPlanSelected: (PremiumPlan) -> Unit,
    subscriptionOffers: SubscriptionOffers,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        PremiumPlan.entries.forEach { plan ->
            val selected = plan == selectedPlan
            val label = when (plan) {
                PremiumPlan.Monthly -> stringResource(R.string.premium_screen_plan_monthly)
                PremiumPlan.Yearly -> stringResource(R.string.premium_screen_plan_yearly)
            }
            val price = subscriptionOffers.priceFor(plan)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                    .background(if (selected) tokens.lockedFill else tokens.cardFill)
                    .clickable { onPlanSelected(plan) }
                    .padding(vertical = KuroSpacing.md, horizontal = KuroSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = KuroType.bodyPrimary.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (selected) tokens.lockedText else tokens.textPrimary,
                )
                if (price != null) {
                    Spacer(modifier = Modifier.height(KuroSpacing.xs))
                    Text(
                        text = price,
                        style = KuroType.labelMeta,
                        color = tokens.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
