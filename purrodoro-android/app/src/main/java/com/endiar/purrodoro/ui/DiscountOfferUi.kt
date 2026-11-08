package com.endiar.purrodoro.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.endiar.purrodoro.R
import com.endiar.purrodoro.billing.DiscountOffer
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.DeepCoral
import com.endiar.purrodoro.ui.theme.Lavender
import com.endiar.purrodoro.ui.theme.LockedPurple
import com.endiar.purrodoro.ui.theme.PremiumAuroraGold
import com.endiar.purrodoro.ui.theme.PremiumBadgeText
import com.endiar.purrodoro.ui.theme.PremiumMemberCardWashDark
import com.endiar.purrodoro.ui.theme.PremiumMemberCardWashLight
import com.endiar.purrodoro.ui.theme.isDarkColorScheme
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.delay

@Composable
fun DiscountOfferDialog(
    price: String?,
    endsAtMillis: Long,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val remaining = rememberDiscountRemaining(endsAtMillis)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                .background(tokens.cardFill),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDarkColorScheme()) PremiumMemberCardWashDark else PremiumMemberCardWashLight,
                    )
                    .padding(top = KuroSpacing.lg, bottom = KuroSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.premium_discount_ribbon),
                    style = KuroType.labelStrong,
                    color = PremiumBadgeText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                        .background(LockedPurple)
                        .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.xs),
                )
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
                Image(
                    painter = painterResource(R.drawable.cat_celebrate_starry),
                    contentDescription = stringResource(R.string.premium_sheet_cat_cd),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(140.dp),
                )
                if (price != null) {
                    Text(
                        text = price,
                        style = KuroType.labelStrong,
                        color = tokens.textPrimary,
                        modifier = Modifier
                            .padding(top = KuroSpacing.xs)
                            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                            .background(tokens.cardFill)
                            .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md),
            ) {
                Text(
                    text = stringResource(R.string.premium_discount_title),
                    style = KuroType.headingCard,
                    color = tokens.textPrimary,
                )
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
                Text(
                    text = stringResource(R.string.premium_discount_body),
                    style = KuroType.bodyPrimary,
                    color = tokens.textSecondary,
                )
                Spacer(modifier = Modifier.height(KuroSpacing.sm))
                DiscountRemainingPill(remaining)
                Spacer(modifier = Modifier.height(KuroSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.premium_sheet_dismiss),
                            style = KuroType.bodySecondary,
                            color = tokens.textSecondary,
                        )
                    }
                    PremiumGradientCtaButton(
                        text = if (price != null) {
                            stringResource(R.string.premium_discount_unlock, price)
                        } else {
                            stringResource(R.string.premium_screen_cta_subscribe)
                        },
                        onClick = onUnlock,
                        modifier = Modifier.padding(start = KuroSpacing.sm),
                        enabled = price != null,
                        compact = true,
                        fillMaxWidth = false,
                    )
                }
            }
        }
    }
}

@Composable
fun DiscountOfferProfileBlock(
    endsAtMillis: Long,
    price: String?,
    compareAtPrice: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val remaining = rememberDiscountRemaining(endsAtMillis)
    val reduceMotion = rememberReduceMotion()
    val pulse = rememberDiscountPulse(reduceMotion)
    val shape = RoundedCornerShape(KuroSpacing.radiusMd)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm)
            .clip(shape)
            .background(if (isDarkColorScheme()) PremiumMemberCardWashDark else PremiumMemberCardWashLight)
            .discountOfferGlow(reduceMotion)
            .clickable(onClick = onClick)
            .padding(KuroSpacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            Icon(
                imageVector = Lucide.Sparkles,
                contentDescription = null,
                tint = LockedPurple,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.premium_screen_title),
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.premium_discount_ribbon),
                style = KuroType.labelStrong,
                color = LockedPurple,
                modifier = Modifier
                    .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                    .background(Lavender.copy(alpha = 0.45f))
                    .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
            )
        }
        Spacer(modifier = Modifier.height(KuroSpacing.sm))
        Text(
            text = remaining,
            style = KuroType.headingScreen.copy(fontWeight = FontWeight.Bold),
            color = tokens.textPrimary,
        )
        if (price != null) {
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            ) {
                if (!compareAtPrice.isNullOrBlank() && compareAtPrice != price) {
                    Text(
                        text = compareAtPrice,
                        style = KuroType.bodyPrimary.copy(textDecoration = TextDecoration.LineThrough),
                        color = tokens.textMuted,
                    )
                }
                Text(
                    text = price,
                    style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold),
                    color = DeepCoral,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(KuroSpacing.xs))
        Text(
            text = stringResource(R.string.premium_one_time_hint),
            style = KuroType.bodySecondary,
            color = tokens.textSecondary,
        )
    }
}

@Composable
fun DiscountOfferPriceLine(
    price: String,
    compareAtPrice: String?,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!compareAtPrice.isNullOrBlank() && compareAtPrice != price) {
            Text(
                text = compareAtPrice,
                style = KuroType.bodySecondary.copy(textDecoration = TextDecoration.LineThrough),
                color = tokens.textMuted,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stringResource(R.string.premium_one_time_price, price),
            style = KuroType.labelStrong,
            color = if (compareAtPrice != null) DeepCoral else tokens.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DiscountRemainingPill(remaining: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.premium_discount_remaining, remaining),
        style = KuroType.labelStrong,
        color = LockedPurple,
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(Lavender.copy(alpha = 0.45f))
            .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
    )
}

@Composable
fun rememberDiscountRemaining(endsAtMillis: Long): String {
    var now by remember(endsAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return DiscountOffer.formatRemaining(endsAtMillis - now)
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

@Composable
private fun rememberDiscountPulse(reduceMotion: Boolean): Float {
    if (reduceMotion) return 1f
    val transition = rememberInfiniteTransition(label = "discountPricePulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "discountPriceScale",
    )
    return pulse
}

@Composable
private fun Modifier.discountOfferGlow(reduceMotion: Boolean): Modifier {
    val transition = rememberInfiniteTransition(label = "discountGlow")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
        ),
        label = "discountGlowSweep",
    )
    val corner = KuroSpacing.radiusMd
    return drawBehind {
        val stroke = 2.dp.toPx()
        val half = stroke / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val brush = Brush.sweepGradient(
            0f to Lavender,
            0.22f to PremiumAuroraGold,
            0.5f to LockedPurple,
            0.78f to PremiumAuroraGold,
            1f to Lavender,
            center = center,
        )
        rotate(degrees = if (reduceMotion) 0f else sweep, pivot = center) {
            drawRoundRect(
                brush = brush,
                topLeft = Offset(half, half),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
                style = Stroke(width = stroke),
            )
        }
    }
}
