package com.endiar.purrodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.billing.BillingCatalogState
import com.endiar.purrodoro.billing.PremiumCatalog
import com.endiar.purrodoro.model.PremiumGate
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroBackScreenTopBar
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.JourneyGreen
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottom
import com.endiar.purrodoro.ui.theme.kuroTokens
import java.util.Locale

/** Aspect ratio of [R.drawable.premium_explore_header] (3:2). */
private const val PremiumExploreHeaderAspectRatio = 3f / 2f

@Composable
fun PremiumScreen(
    isPremium: Boolean,
    premiumCatalog: PremiumCatalog,
    purchaseInFlight: Boolean,
    onRefreshCatalog: () -> Unit,
    onRetryCatalog: () -> Unit,
    onBack: () -> Unit,
    onPurchase: () -> Unit,
    onRestorePurchases: () -> Unit,
    discountEndsAtMillis: Long? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    LaunchedEffect(Unit) {
        onRefreshCatalog()
    }

    Scaffold(
        topBar = {
            KuroBackScreenTopBar(
                title = stringResource(R.string.premium_screen_nav_title),
                onBack = onBack,
            )
        },
        containerColor = tokens.pageBackground,
        bottomBar = {
            PremiumStickyBottomBar(
                isPremium = isPremium,
                premiumCatalog = premiumCatalog,
                purchaseInFlight = purchaseInFlight,
                onPurchase = onPurchase,
                onRestorePurchases = onRestorePurchases,
                onRetryCatalog = onRetryCatalog,
                discountEndsAtMillis = discountEndsAtMillis,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            PremiumExploreHeader(isPremium = isPremium)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KuroSpacing.lg)
                    .padding(top = KuroSpacing.lg, bottom = KuroSpacing.xxl),
            ) {
                PremiumHeadlineBlock(isPremium = isPremium)

                if (!isPremium) {
                    Spacer(modifier = Modifier.height(KuroSpacing.lg))
                    PremiumSectionLabel(text = stringResource(R.string.premium_screen_what_you_get))
                    Spacer(modifier = Modifier.height(KuroSpacing.sm))
                    PremiumCompareCard(isPremium = false)
                    Spacer(modifier = Modifier.height(KuroSpacing.sm))
                    Text(
                        text = stringResource(R.string.premium_compare_summary),
                        style = KuroType.bodySecondary,
                        color = tokens.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )

                } else {
                    Spacer(modifier = Modifier.height(KuroSpacing.sm))
                    Text(
                        text = stringResource(R.string.premium_screen_body_member),
                        style = KuroType.bodySecondary,
                        color = tokens.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(KuroSpacing.lg))
                    PremiumSectionLabel(text = stringResource(R.string.premium_screen_what_you_get))
                    Spacer(modifier = Modifier.height(KuroSpacing.sm))
                    PremiumCompareCard(isPremium = true)
                }
            }
        }
    }
}

@Composable
private fun PremiumExploreHeader(isPremium: Boolean) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val headerRes = if (isPremium) {
        R.drawable.premium_sheet_header
    } else {
        R.drawable.premium_explore_header
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PremiumExploreHeaderAspectRatio),
    ) {
        Image(
            painter = painterResource(id = headerRes),
            contentDescription = stringResource(R.string.premium_explore_header_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(KuroSpacing.xxxl)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            tokens.pageBackground,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun PremiumStickyBottomBar(
    isPremium: Boolean,
    premiumCatalog: PremiumCatalog,
    purchaseInFlight: Boolean,
    onPurchase: () -> Unit,
    onRestorePurchases: () -> Unit,
    onRetryCatalog: () -> Unit,
    discountEndsAtMillis: Long? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val discountRemaining = rememberDiscountRemaining(discountEndsAtMillis ?: 0L)
    val barShape = RoundedCornerShape(
        topStart = KuroSpacing.radiusLg,
        topEnd = KuroSpacing.radiusLg,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = barShape,
                clip = false,
                ambientColor = if (isDark) {
                    Color.Black.copy(alpha = 0.42f)
                } else {
                    Color(0xFFBFA494).copy(alpha = 0.28f)
                },
                spotColor = if (isDark) {
                    Color.Black.copy(alpha = 0.58f)
                } else {
                    Color(0xFF8A6E62).copy(alpha = 0.22f)
                },
            )
            .clip(barShape)
            .background(tokens.cardFill)
            .border(
                width = 1.dp,
                color = tokens.divider.copy(alpha = if (isDark) 0.55f else 0.85f),
                shape = barShape,
            )
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md)
            .navigationBarsPadding(),
    ) {
        if (isPremium) {
            PremiumActiveFooter()
        } else {
            when (premiumCatalog.catalogState) {
                BillingCatalogState.Idle,
                BillingCatalogState.Loading,
                -> {
                    PremiumGradientCtaButton(
                        text = stringResource(R.string.premium_catalog_loading),
                        onClick = {},
                        enabled = false,
                    )
                }
                BillingCatalogState.Unavailable -> {
                    PremiumGradientCtaButton(
                        text = stringResource(R.string.premium_catalog_retry),
                        onClick = onRetryCatalog,
                        enabled = !purchaseInFlight,
                    )
                    TextButton(
                        onClick = onRestorePurchases,
                        enabled = !purchaseInFlight,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = stringResource(R.string.premium_restore_purchases),
                            style = KuroType.bodySecondary,
                            color = tokens.textSecondary,
                        )
                    }
                }
                BillingCatalogState.Ready -> {
                    val price = premiumCatalog.formattedPrice
                    if (premiumCatalog.discountActive && discountEndsAtMillis != null) {
                        DiscountRemainingPill(
                            remaining = discountRemaining,
                            modifier = Modifier.padding(bottom = KuroSpacing.sm),
                        )
                    }
                    if (price != null) {
                        DiscountOfferPriceLine(
                            price = price,
                            compareAtPrice = premiumCatalog.compareAtPrice,
                            modifier = Modifier.padding(bottom = KuroSpacing.sm),
                        )
                    }
                    PremiumGradientCtaButton(
                        text = if (premiumCatalog.discountActive && price != null) {
                            stringResource(R.string.premium_discount_unlock, price)
                        } else {
                            stringResource(premiumCatalog.ctaResId(forSheet = false))
                        },
                        onClick = onPurchase,
                        enabled = !purchaseInFlight,
                    )
                    Text(
                        text = stringResource(R.string.premium_one_time_hint),
                        style = KuroType.bodySecondary,
                        color = tokens.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KuroSpacing.sm),
                    )
                    TextButton(
                        onClick = onRestorePurchases,
                        enabled = !purchaseInFlight,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = stringResource(R.string.premium_restore_purchases),
                            style = KuroType.bodySecondary,
                            color = tokens.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSectionLabel(text: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = KuroType.labelCaption,
        color = tokens.textMuted,
    )
}

@Composable
private fun PremiumHeadlineBlock(
    isPremium: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isPremium) {
                stringResource(R.string.premium_screen_headline_member)
            } else {
                stringResource(R.string.premium_screen_headline_discover)
            },
            style = KuroType.headingDisplay.copy(
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = tokens.textPrimary,
        )
        if (!isPremium) {
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = stringResource(R.string.premium_screen_body_discover),
                style = KuroType.bodySecondary,
                color = tokens.textSecondary,
            )
        }
    }
}

@Composable
private fun PremiumCompareCard(isPremium: Boolean) {
    KuroCard(modifier = Modifier.fillMaxWidth()) {
        PremiumCompareHeaderRow()
        PremiumCompareDivider()
        PremiumCompareRow(
            feature = stringResource(R.string.premium_compare_sounds),
            freeValue = stringResource(R.string.premium_compare_sounds_free),
            premiumValue = stringResource(R.string.premium_compare_sounds_premium),
            isPremium = isPremium,
        )
        PremiumCompareDivider()
        PremiumCompareRow(
            feature = stringResource(R.string.premium_compare_scenes),
            freeValue = stringResource(R.string.premium_compare_scenes_free),
            premiumValue = stringResource(R.string.premium_compare_scenes_premium),
            isPremium = isPremium,
        )
        PremiumCompareDivider()
        PremiumCompareRow(
            feature = stringResource(R.string.premium_compare_tasks),
            freeValue = stringResource(
                R.string.premium_compare_tasks_free,
                PremiumGate.FREE_ACTIVE_TASK_LIMIT,
            ),
            premiumValue = stringResource(R.string.premium_compare_unlimited),
            isPremium = isPremium,
        )
        PremiumCompareDivider()
        PremiumCompareRow(
            feature = stringResource(R.string.premium_compare_habits),
            freeValue = stringResource(
                R.string.premium_compare_habits_free,
                PremiumGate.FREE_HABIT_LIMIT,
            ),
            premiumValue = stringResource(R.string.premium_compare_unlimited),
            isPremium = isPremium,
        )
        PremiumCompareDivider()
        PremiumCompareRow(
            feature = stringResource(R.string.premium_compare_journey),
            freeValue = stringResource(R.string.premium_compare_included),
            premiumValue = stringResource(R.string.premium_compare_included),
            isPremium = isPremium,
        )
    }
}

@Composable
private fun PremiumCompareHeaderRow() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1.1f))
        Text(
            text = stringResource(R.string.premium_compare_col_free),
            style = KuroType.labelCaption,
            color = tokens.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            text = stringResource(R.string.premium_compare_col_premium),
            style = KuroType.labelCaption,
            color = PremiumBadgeGradientBottom,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.9f),
        )
    }
}

@Composable
private fun PremiumCompareRow(
    feature: String,
    freeValue: String,
    premiumValue: String,
    isPremium: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm + KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = feature,
            style = KuroType.bodyPrimary,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1.1f),
        )
        Text(
            text = freeValue,
            style = KuroType.bodySecondary,
            color = tokens.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            text = premiumValue,
            style = KuroType.bodySecondary.copy(fontWeight = FontWeight.SemiBold),
            color = if (isPremium) JourneyGreen else PremiumBadgeGradientBottom,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.9f),
        )
    }
}

@Composable
private fun PremiumCompareDivider() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(tokens.divider),
    )
}

@Composable
private fun PremiumActiveFooter() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                .background(tokens.lockedFill)
                .padding(vertical = KuroSpacing.md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = JourneyGreen,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(KuroSpacing.sm))
            Text(
                text = stringResource(R.string.premium_screen_active_label),
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.lockedText,
            )
        }
    }
}
