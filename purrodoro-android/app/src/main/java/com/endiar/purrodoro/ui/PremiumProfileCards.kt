package com.endiar.purrodoro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.designsystem.PremiumBadgeStar
import com.endiar.purrodoro.ui.designsystem.PremiumShimmerSurface
import com.endiar.purrodoro.ui.designsystem.rememberPremiumBadgeMotion
import com.endiar.purrodoro.ui.theme.PremiumBadgeText

/** Purple-gradient CTA with badge shimmer — used on Premium discovery and profile upsell. */
@Composable
fun PremiumGradientCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    fillMaxWidth: Boolean = true,
) {
    val motion = rememberPremiumBadgeMotion()
    val verticalPadding = if (compact) KuroSpacing.sm + 2.dp else KuroSpacing.md
    val horizontalPadding = if (compact) KuroSpacing.md else KuroSpacing.lg
    val starSize = if (compact) 14.dp else 16.dp

    PremiumShimmerSurface(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier),
        shape = RoundedCornerShape(KuroSpacing.radiusPill),
        motion = motion,
        onClick = if (enabled) onClick else null,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            PremiumBadgeStar(size = starSize, motion = motion)
            Text(
                text = text,
                style = if (compact) {
                    KuroType.labelStrong.copy(fontWeight = FontWeight.Bold)
                } else {
                    KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold)
                },
                color = PremiumBadgeText,
                maxLines = 1,
            )
        }
    }
}
