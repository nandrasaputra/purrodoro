package com.endiar.purrodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.AuthProvider
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.sync.SyncStatusPresentation
import com.endiar.purrodoro.ui.auth.GoogleBrandIcon
import com.endiar.purrodoro.ui.designsystem.KuroAccountAvatar
import com.endiar.purrodoro.ui.designsystem.KuroFreeBadge
import com.endiar.purrodoro.ui.designsystem.KuroPremiumBadge
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.designsystem.premiumAuroraOutline
import com.endiar.purrodoro.ui.theme.PremiumBadgeGradientBottom
import com.endiar.purrodoro.ui.theme.PremiumMemberCardWashDark
import com.endiar.purrodoro.ui.theme.PremiumMemberCardWashLight
import com.endiar.purrodoro.ui.theme.isDarkColorScheme
import com.endiar.purrodoro.ui.theme.kuroTokens

private const val ProfileBannerAspectRatio = 1536f / 434f
private val ProfileAvatarSize = 52.dp

/**
 * Unified Profile identity card — account + tier badge + sync.
 * Premium: aurora outline, starry banner, Starry Pill, View benefits footer.
 * Free: plain border, lavender banner, Free pill, one-line benefit preview + footer CTA.
 */
@Composable
fun ProfileIdentityCard(
    authState: AuthState.SignedIn,
    isPremium: Boolean,
    onSignOut: () -> Unit,
    onOpenPremium: () -> Unit,
    syncPresentation: SyncStatusPresentation? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val dark = isDarkColorScheme()
    val shape = RoundedCornerShape(KuroSpacing.radiusLg)
    val cardFill = if (isPremium) {
        if (dark) PremiumMemberCardWashDark else PremiumMemberCardWashLight
    } else {
        tokens.cardFill
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = if (isPremium) 4.dp else 0.dp, shape = shape, clip = false)
            .then(
                if (isPremium) Modifier.premiumAuroraOutline(shape = shape)
                else Modifier.border(1.dp, tokens.cardBorder, shape),
            )
            .clip(shape)
            .background(cardFill),
    ) {
        ProfileIdentityBanner(isPremium = isPremium)

        if (syncPresentation != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .padding(
                            start = KuroSpacing.lg,
                            end = KuroSpacing.sm,
                            top = KuroSpacing.lg,
                            bottom = KuroSpacing.md,
                        ),
                ) {
                    ProfileIdentityDetails(
                        authState = authState,
                        isPremium = isPremium,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .background(
                            color = tokens.accentSoft,
                            shape = RoundedCornerShape(
                                bottomStart = KuroSpacing.radiusSm,
                            ),
                        )
                        .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    SyncStatusMiniPanel(presentation = syncPresentation)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KuroSpacing.lg)
                    .padding(top = KuroSpacing.lg, bottom = KuroSpacing.md),
            ) {
                ProfileIdentityDetails(
                    authState = authState,
                    isPremium = isPremium,
                )
            }
        }

        if (!isPremium) {
            ProfileBenefitSection(
                modifier = Modifier.padding(horizontal = KuroSpacing.lg),
            )
        }

        Spacer(modifier = Modifier.height(KuroSpacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.lg)
                .height(1.dp)
                .background(tokens.divider),
        )

        ProfileIdentityFooter(
            isPremium = isPremium,
            onOpenPremium = onOpenPremium,
            onSignOut = onSignOut,
            modifier = Modifier.padding(horizontal = KuroSpacing.sm),
        )
    }
}

@Composable
private fun ProfileIdentityBanner(isPremium: Boolean) {
    val bannerRes = if (isPremium) {
        R.drawable.profile_premium_banner
    } else {
        R.drawable.profile_free_banner
    }
    val bannerCd = if (isPremium) {
        stringResource(R.string.profile_premium_banner_cd)
    } else {
        stringResource(R.string.profile_free_banner_cd)
    }

    Image(
        painter = painterResource(id = bannerRes),
        contentDescription = bannerCd,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ProfileBannerAspectRatio)
            .clip(RoundedCornerShape(topStart = KuroSpacing.radiusLg, topEnd = KuroSpacing.radiusLg)),
    )
}

@Composable
private fun ProfileIdentityDetails(
    authState: AuthState.SignedIn,
    isPremium: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val user = authState.user
    val displayName = user.displayName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.account_default_name)
    val providerLabel = when (user.provider) {
        AuthProvider.Google -> stringResource(R.string.account_signed_in_with_google)
        AuthProvider.Unknown -> stringResource(R.string.account_signed_in_generic)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        ProfileAccountAvatar(
            userId = user.userId,
            displayName = displayName,
            isPremium = isPremium,
        )
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            user.email?.let { email ->
                Spacer(modifier = Modifier.height(KuroSpacing.xs))
                Text(
                    text = email,
                    style = KuroType.labelMeta.copy(fontWeight = FontWeight.Normal),
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                if (user.provider == AuthProvider.Google) {
                    GoogleBrandIcon(modifier = Modifier.size(16.dp))
                }
                Text(
                    text = providerLabel,
                    style = KuroType.labelMeta.copy(fontWeight = FontWeight.Normal),
                    color = tokens.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(KuroSpacing.md))
            if (isPremium) {
                KuroPremiumBadge()
            } else {
                KuroFreeBadge()
            }
        }
    }
}

@Composable
private fun ProfileAccountAvatar(
    userId: String,
    displayName: String,
    isPremium: Boolean,
) {
    KuroAccountAvatar(
        userId = userId,
        displayName = displayName,
        isPremium = isPremium,
        size = ProfileAvatarSize,
    )
}

@Composable
private fun ProfileBenefitSection(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val benefits = listOf(
        stringResource(R.string.profile_benefit_sounds),
        stringResource(R.string.profile_benefit_scenes),
        stringResource(R.string.profile_benefit_tasks),
        stringResource(R.string.profile_benefit_habits),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xs, bottom = KuroSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.profile_upgrade_heading),
            style = KuroType.bodySecondary.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
            ),
            color = tokens.textPrimary,
            modifier = Modifier.padding(bottom = KuroSpacing.md),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            benefits.chunked(2).forEach { rowBenefits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
                ) {
                    rowBenefits.forEach { label ->
                        ProfileBenefitChip(
                            label = label,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBenefitChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
            .background(tokens.cardFill.copy(alpha = if (isDarkColorScheme()) 0.5f else 0.92f))
            .border(1.dp, tokens.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(KuroSpacing.radiusSm))
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(PremiumBadgeGradientBottom),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_profile_benefit_unlock),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp),
            )
        }
        Text(
            text = label,
            style = KuroType.labelStrong.copy(fontWeight = FontWeight.Medium),
            color = tokens.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileIdentityFooter(
    isPremium: Boolean,
    onOpenPremium: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.sm, bottom = KuroSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPremium) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onOpenPremium)
                    .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.premium_member_view_benefits),
                    style = KuroType.labelStrong,
                    color = PremiumBadgeGradientBottom,
                )
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = PremiumBadgeGradientBottom,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            PremiumGradientCtaButton(
                text = stringResource(R.string.profile_explore_premium),
                onClick = onOpenPremium,
                compact = true,
                fillMaxWidth = false,
                modifier = Modifier.padding(start = KuroSpacing.xs),
            )
        }
        TextButton(onClick = onSignOut) {
            Text(
                text = stringResource(R.string.account_sign_out),
                style = KuroType.labelStrong,
                color = tokens.accent,
            )
        }
    }
}
