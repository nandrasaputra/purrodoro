package com.endiar.purrodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.PremiumGate
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroBackScreenTopBar
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.JourneyGreen
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottom
import com.endiar.purrodoro.ui.theme.kuroTokens

private const val PremiumHeroAspectRatio = 1536f / 1024f

@Composable
fun PremiumScreen(
    isPremium: Boolean,
    onBack: () -> Unit,
    onSubscribe: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Scaffold(
        topBar = {
            KuroBackScreenTopBar(
                title = stringResource(R.string.premium_screen_title),
                onBack = onBack,
            )
        },
        containerColor = tokens.pageBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.cardFill)
                    .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md)
                    .navigationBarsPadding(),
            ) {
                if (isPremium) {
                    PremiumActiveFooter()
                } else {
                    Text(
                        text = stringResource(R.string.premium_screen_price_placeholder),
                        style = KuroType.labelMeta,
                        color = tokens.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = KuroSpacing.sm),
                    )
                    PremiumGradientCtaButton(
                        text = stringResource(R.string.premium_screen_cta_subscribe),
                        onClick = onSubscribe,
                    )
                    Text(
                        text = stringResource(R.string.premium_one_time_hint),
                        style = KuroType.labelMeta,
                        color = tokens.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KuroSpacing.sm),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Image(
                painter = painterResource(id = R.drawable.premium_sheet_header),
                contentDescription = stringResource(R.string.premium_sheet_header_cd),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PremiumHeroAspectRatio),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KuroSpacing.lg)
                    .padding(top = KuroSpacing.md, bottom = KuroSpacing.xxl),
            ) {
                Text(
                    text = if (isPremium) {
                        stringResource(R.string.premium_screen_headline_member)
                    } else {
                        stringResource(R.string.premium_screen_headline_discover)
                    },
                    style = KuroType.headingScreen.copy(
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = tokens.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
                Text(
                    text = if (isPremium) {
                        stringResource(R.string.premium_screen_body_member)
                    } else {
                        stringResource(R.string.premium_screen_body_discover)
                    },
                    style = KuroType.bodyPrimary,
                    color = tokens.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(KuroSpacing.xl))

                PremiumBenefitStoryCard(
                    title = stringResource(R.string.premium_story_sounds_title),
                    body = stringResource(R.string.premium_story_sounds_body),
                    imageRes = R.drawable.cat_premium_starry,
                    icon = {
                        Icon(Lucide.AudioLines, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
                    },
                )
                Spacer(modifier = Modifier.height(KuroSpacing.md))
                PremiumBenefitStoryCard(
                    title = stringResource(R.string.premium_story_scenes_title),
                    body = stringResource(R.string.premium_story_scenes_body),
                    imageRes = R.drawable.cat_home_premium_cafe_day,
                    icon = {
                        Icon(Lucide.Layers, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
                    },
                )
                Spacer(modifier = Modifier.height(KuroSpacing.md))
                PremiumBenefitStoryCard(
                    title = stringResource(R.string.premium_story_headroom_title),
                    body = stringResource(
                        R.string.premium_story_headroom_body,
                        PremiumGate.FREE_ACTIVE_TASK_LIMIT,
                        PremiumGate.FREE_HABIT_LIMIT,
                    ),
                    imageRes = R.drawable.cat_mascot,
                    icon = {
                        Icon(Lucide.ListTodo, null, tint = tokens.lockedText, modifier = Modifier.size(18.dp))
                    },
                )

                Spacer(modifier = Modifier.height(KuroSpacing.xl))
                Text(
                    text = stringResource(R.string.premium_compare_section_title),
                    style = KuroType.headingCard.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.textPrimary,
                )
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
                KuroCard(modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}

@Composable
private fun PremiumBenefitStoryCard(
    title: String,
    body: String,
    imageRes: Int,
    icon: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KuroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(KuroSpacing.radiusSm)),
            )
            Spacer(modifier = Modifier.size(KuroSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                            .background(tokens.lockedFill),
                        contentAlignment = Alignment.Center,
                    ) {
                        icon()
                    }
                    Text(
                        text = title,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.textPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(KuroSpacing.xs))
                Text(
                    text = body,
                    style = KuroType.bodySecondary,
                    color = tokens.textSecondary,
                )
            }
        }
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
            style = KuroType.labelMeta,
            color = tokens.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            text = premiumValue,
            style = KuroType.labelMeta.copy(fontWeight = FontWeight.SemiBold),
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
