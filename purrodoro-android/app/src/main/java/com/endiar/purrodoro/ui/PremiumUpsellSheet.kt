package com.endiar.purrodoro.ui

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
import com.composables.icons.lucide.Crown
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.components.KuroPrimaryButton
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.Coral
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens

/** Aspect ratio of [R.drawable.premium_sheet_header] (1536×1024). */
private const val PremiumSheetHeaderAspectRatio = 1536f / 1024f

/**
 * What the user tapped before the premium gate opened. Drives headline copy only —
 * the hero header is a baked illustration; body copy stays localizable below.
 */
data class PremiumUpsellTrigger(val lockedFeatureName: String? = null)

@Composable
fun PremiumUpsellSheet(
    trigger: PremiumUpsellTrigger,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    KuroAdaptiveBottomSheet(
        onDismiss = onDismiss,
        horizontalPadding = 0.dp,
        containerColor = tokens.cardFill,
        showCloseButton = true,
        contentKey = trigger.lockedFeatureName,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.premium_sheet_header),
                contentDescription = stringResource(R.string.premium_sheet_header_cd),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PremiumSheetHeaderAspectRatio),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KuroSpacing.xl)
                    .padding(top = KuroSpacing.md, bottom = KuroSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PremiumSheetHeadline(
                    text = trigger.lockedFeatureName?.let {
                        stringResource(R.string.premium_sheet_title_locked, it)
                    } ?: stringResource(R.string.premium_sheet_title),
                )
                Spacer(modifier = Modifier.height(KuroSpacing.xs))
                Text(
                    text = stringResource(R.string.premium_sheet_subtitle),
                    style = KuroType.bodyPrimary,
                    color = tokens.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp),
                )
                Spacer(modifier = Modifier.height(KuroSpacing.lg))
                PremiumPerkRow(
                    icon = {
                        Icon(
                            Lucide.AudioLines,
                            contentDescription = null,
                            tint = tokens.lockedText,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = stringResource(R.string.premium_perk_sounds),
                )
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
                PremiumPerkRow(
                    icon = {
                        Icon(
                            Lucide.LayoutGrid,
                            contentDescription = null,
                            tint = tokens.lockedText,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = stringResource(R.string.premium_perk_widgets),
                )
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
                PremiumPerkRow(
                    icon = {
                        Icon(
                            Lucide.Sparkles,
                            contentDescription = null,
                            tint = tokens.lockedText,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = stringResource(R.string.premium_perk_mascot),
                )
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
}

@Composable
private fun PremiumSheetHeadline(text: String, modifier: Modifier = Modifier) {
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
private fun PremiumPerkRow(
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
