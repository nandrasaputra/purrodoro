package com.endiar.purrodoro.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Crown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.endiar.purrodoro.R
import com.endiar.purrodoro.billing.BillingCatalogState
import com.endiar.purrodoro.billing.PremiumPlan
import com.endiar.purrodoro.billing.SubscriptionOffers
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.Cocoa
import com.endiar.purrodoro.ui.theme.Coral
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens

/** Aspect ratio of [R.drawable.premium_sheet_header] (1536×1024). */
const val PremiumSheetHeaderAspectRatio = 1536f / 1024f

/** Aspect ratio of [R.drawable.premium_guest_unlock_header] (1536×1024). */
const val PremiumGuestUnlockHeaderAspectRatio = 1536f / 1024f

@Composable
fun PremiumSheetHeader(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.premium_sheet_header),
        contentDescription = stringResource(R.string.premium_sheet_header_cd),
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PremiumSheetHeaderAspectRatio),
    )
}

@Composable
fun PremiumGuestUnlockHeader(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.premium_guest_unlock_header),
        contentDescription = stringResource(R.string.premium_guest_unlock_header_cd),
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PremiumGuestUnlockHeaderAspectRatio),
    )
}

/**
 * Shared sign-in gate layout for guest premium attempts and Profile → Explore Premium.
 *
 * Hierarchy: illustration → headline → supporting line → primary CTA → secondary dismiss.
 */
/** Sticky paywall footer: gradient CTA + dismiss. Used with [KuroAdaptiveBottomSheet] footer slot. */
@Composable
fun PremiumSheetStickyFooter(
    subscriptionOffers: SubscriptionOffers,
    selectedPlan: PremiumPlan,
    purchaseInFlight: Boolean,
    onUpgrade: (PremiumPlan) -> Unit,
    onRetryCatalog: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrollOverlay: Float = 0f,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val overlay = scrollOverlay.coerceIn(0f, 1f)
    val footerElevation by animateDpAsState(
        targetValue = KuroSpacing.soundscapeStickyFooterElevation * overlay,
        animationSpec = tween(durationMillis = 180),
        label = "premiumSheetFooterElevation",
    )
    val shadowAmbient = if (isDark) {
        Color.Black.copy(alpha = 0.52f * overlay)
    } else {
        Color(0xFF9A7B6A).copy(alpha = 0.38f * overlay)
    }
    val shadowSpot = if (isDark) {
        Color.Black.copy(alpha = 0.62f * overlay)
    } else {
        Color(0xFF7A5C4C).copy(alpha = 0.32f * overlay)
    }
    val scrimColor = if (isDark) Color.Black else Cocoa
    val catalogState = subscriptionOffers.catalogState
    val ctaText = stringResource(
        when (catalogState) {
            BillingCatalogState.Unavailable -> R.string.premium_catalog_retry
            BillingCatalogState.Idle,
            BillingCatalogState.Loading,
            -> R.string.premium_catalog_loading
            BillingCatalogState.Ready ->
                subscriptionOffers.ctaResId(selectedPlan, forSheet = true)
        },
    )
    val ctaEnabled = when (catalogState) {
        BillingCatalogState.Unavailable -> !purchaseInFlight
        BillingCatalogState.Ready -> !purchaseInFlight
        else -> false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = footerElevation,
                shape = RectangleShape,
                clip = false,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot,
            )
            .background(tokens.cardFill),
    ) {
        if (overlay > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KuroSpacing.soundscapeStickyFooterScrim)
                    .align(Alignment.TopCenter)
                    .offset(y = -KuroSpacing.soundscapeStickyFooterScrim)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.55f to scrimColor.copy(alpha = 0.04f * overlay),
                                1f to scrimColor.copy(alpha = 0.14f * overlay),
                            ),
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.cardBorder.copy(alpha = 0.45f + (0.35f * overlay))),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.md))
            PremiumGradientCtaButton(
                text = ctaText,
                onClick = {
                    when (catalogState) {
                        BillingCatalogState.Unavailable -> onRetryCatalog()
                        BillingCatalogState.Ready -> onUpgrade(selectedPlan)
                        else -> Unit
                    }
                },
                enabled = ctaEnabled,
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
fun PremiumMemberTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Text(
        text = text,
        style = KuroType.headingScreen.copy(
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
        ),
        color = tokens.textPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PremiumSheetHeadline(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.Sparkles,
            contentDescription = null,
            tint = Coral.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = KuroType.headingCard.copy(
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = KuroSpacing.sm),
        )
        Icon(
            imageVector = Lucide.Sparkles,
            contentDescription = null,
            tint = Coral.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun PremiumPerkRow(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
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
                .background(tokens.lockedFill),
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
        Icon(
            imageVector = Lucide.Crown,
            contentDescription = null,
            tint = tokens.lockedText,
            modifier = Modifier.size(16.dp),
        )
    }
}
