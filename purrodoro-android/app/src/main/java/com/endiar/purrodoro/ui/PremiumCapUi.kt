package com.endiar.purrodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.PremiumGate
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens

/** Why the premium paywall opened — drives title, body, and perk rows. */
sealed interface PremiumUpsellReason {
    data class LockedSound(val featureName: String) : PremiumUpsellReason
    data class LockedScene(val sceneName: String) : PremiumUpsellReason
    data object ActiveTaskCap : PremiumUpsellReason
    data object HabitCap : PremiumUpsellReason
    data object Generic : PremiumUpsellReason
}

data class PremiumUpsellTrigger(
    val reason: PremiumUpsellReason = PremiumUpsellReason.Generic,
) {
    companion object {
        fun lockedSound(name: String) = PremiumUpsellTrigger(PremiumUpsellReason.LockedSound(name))
        fun lockedScene(name: String) = PremiumUpsellTrigger(PremiumUpsellReason.LockedScene(name))
        val activeTaskCap = PremiumUpsellTrigger(PremiumUpsellReason.ActiveTaskCap)
        val habitCap = PremiumUpsellTrigger(PremiumUpsellReason.HabitCap)
    }
}

/** What to open after a guest signs in from the premium login gate. */
sealed interface PremiumAccessRequest {
    data object Discovery : PremiumAccessRequest
    data class Upsell(val trigger: PremiumUpsellTrigger) : PremiumAccessRequest
}

/**
 * Subtle free-tier usage label under Tasks / Habits titles.
 * Hidden for premium users ([visible] = false).
 */
@Composable
fun FreeTierCapHeaderLabel(
    visible: Boolean,
    current: Int,
    limit: Int,
    labelRes: Int,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val atCap = current >= limit
    val text = stringResource(labelRes, current, limit)
    val cd = stringResource(R.string.free_tier_cap_cd, current, limit)
    Text(
        text = text,
        style = KuroType.labelMeta,
        color = if (atCap) tokens.lockedText else tokens.textSecondary,
        modifier = modifier
            .semantics { contentDescription = cd }
            .then(
                if (atCap) {
                    Modifier
                        .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                        .background(tokens.lockedFill.copy(alpha = 0.55f))
                        .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs)
                } else {
                    Modifier
                },
            ),
    )
}

/** Title column for list screens: heading + optional free-tier counter. */
@Composable
fun ScreenTitleWithFreeTierCap(
    title: String,
    showFreeTierCap: Boolean,
    current: Int,
    limit: Int,
    capLabelRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.kuroTokens.textPrimary,
            style = KuroType.headingDisplay,
        )
        FreeTierCapHeaderLabel(
            visible = showFreeTierCap,
            current = current,
            limit = limit,
            labelRes = capLabelRes,
            modifier = Modifier.padding(top = KuroSpacing.xs),
        )
    }
}

object FreeTierCapLimits {
    val activeTasks = PremiumGate.FREE_ACTIVE_TASK_LIMIT
    val habits = PremiumGate.FREE_HABIT_LIMIT
}
