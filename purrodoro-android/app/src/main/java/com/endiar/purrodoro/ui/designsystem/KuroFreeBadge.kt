package com.endiar.purrodoro.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.theme.FreeBadgeBorderDark
import com.endiar.purrodoro.ui.theme.FreeBadgeBorderLight
import com.endiar.purrodoro.ui.theme.FreeBadgeFillDark
import com.endiar.purrodoro.ui.theme.FreeBadgeFillLight
import com.endiar.purrodoro.ui.theme.FreeBadgeTextDark
import com.endiar.purrodoro.ui.theme.FreeBadgeTextLight
import com.endiar.purrodoro.ui.theme.isDarkColorScheme

private val FreeBadgeShape = RoundedCornerShape(KuroSpacing.radiusSm)

/**
 * Muted **Free** tier pill — counterpart to [KuroPremiumBadge] on Profile.
 * Soft fill + outline; no sparkle star.
 */
@Composable
fun KuroFreeBadge(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.profile_free_badge),
) {
    val dark = isDarkColorScheme()
    val fill = if (dark) FreeBadgeFillDark else FreeBadgeFillLight
    val border = if (dark) FreeBadgeBorderDark else FreeBadgeBorderLight
    val textColor = if (dark) FreeBadgeTextDark else FreeBadgeTextLight
    val badgeCd = stringResource(R.string.profile_free_badge_cd, label)

    Box(
        modifier = modifier
            .semantics { contentDescription = badgeCd }
            .border(1.dp, border, FreeBadgeShape)
            .background(fill, FreeBadgeShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = KuroType.labelMeta.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            maxLines = 1,
        )
    }
}
