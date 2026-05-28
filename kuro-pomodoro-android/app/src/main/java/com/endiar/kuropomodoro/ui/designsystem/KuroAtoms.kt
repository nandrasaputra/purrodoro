package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.theme.JourneyGreen
import com.endiar.kuropomodoro.ui.theme.kuroTokens

// ── KuroChip ─────────────────────────────────────────────────────────────────
// Flat-surface filter chip. Used in Todo and Habits filter bars.

enum class KuroChipVariant {
    /** Coral fill when selected — default filter chips */
    Default,
    /** Dark fill when selected — Tasks category bar */
    Inverted,
    /** Accent outline — "+ New" in category edit mode */
    Accent,
}

@Composable
fun KuroChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KuroChipVariant = KuroChipVariant.Default,
    onDelete: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = when (variant) {
        KuroChipVariant.Default -> if (selected) tokens.interactiveFill else Color.Transparent
        KuroChipVariant.Inverted -> if (selected) tokens.textPrimary else tokens.cardFill
        KuroChipVariant.Accent -> tokens.cardFill
    }
    val textColor = when (variant) {
        KuroChipVariant.Default -> if (selected) tokens.interactiveText else tokens.textSecondary
        KuroChipVariant.Inverted -> if (selected) tokens.accentOn else tokens.textSecondary
        KuroChipVariant.Accent -> tokens.accent
    }
    val borderColor = when (variant) {
        KuroChipVariant.Accent -> tokens.accent.copy(alpha = 0.45f)
        KuroChipVariant.Default -> if (selected) Color.Transparent else tokens.cardBorder
        KuroChipVariant.Inverted -> tokens.cardBorder
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(contentAlignment = Alignment.TopStart) {
        Box(
            modifier = modifier
                .pressScale(interactionSource)
                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                .background(fill)
                .border(1.dp, borderColor, RoundedCornerShape(KuroSpacing.radiusMd))
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = null,
                )
                .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        ) {
            Text(text = text, style = KuroType.labelStrong, color = textColor)
        }
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.taskChipDeleteBadge)
                    .clip(CircleShape)
                    .background(tokens.destructive)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "x", color = tokens.accentOn, style = KuroType.labelMeta)
            }
        }
    }
}

// ── KuroPill ─────────────────────────────────────────────────────────────────
// Semantic status pill. Replaces StatusPill.

enum class KuroPillKind {
    Playing,
    Selected,
    Locked,
    Default,
    /** Journey timeline: completed session */
    JourneyCompleted,
    /** Journey timeline: partial session */
    JourneyPartial,
}

@Composable
fun KuroPill(
    label: String,
    kind: KuroPillKind = KuroPillKind.Default,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = when (kind) {
        KuroPillKind.Playing          -> tokens.accent
        KuroPillKind.Selected         -> tokens.accentSoft
        KuroPillKind.Locked           -> tokens.lockedFill
        KuroPillKind.Default          -> tokens.cardBorder
        KuroPillKind.JourneyCompleted -> tokens.accentSoft
        KuroPillKind.JourneyPartial   -> tokens.activeCardFill
    }
    val textColor = when (kind) {
        KuroPillKind.Playing          -> tokens.accentOn
        KuroPillKind.Selected         -> tokens.accent
        KuroPillKind.Locked           -> tokens.lockedText
        KuroPillKind.Default          -> tokens.textSecondary
        KuroPillKind.JourneyCompleted -> JourneyGreen
        KuroPillKind.JourneyPartial   -> tokens.accent
    }
    val borderColor = when (kind) {
        KuroPillKind.Selected -> tokens.accent.copy(alpha = 0.45f)
        KuroPillKind.Locked   -> tokens.lockedBorder
        else                  -> Color.Transparent
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(KuroSpacing.radiusSm))
            .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
    ) {
        Text(text = label, style = KuroType.labelMeta, color = textColor)
    }
}

// ── KuroStepper ──────────────────────────────────────────────────────────────
// +/- button used in habit quantity steppers.

@Composable
fun KuroStepper(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    large: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val stepperSize = if (large) 44.dp else 32.dp
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.90f)
            .size(stepperSize)
            .clip(CircleShape)
            .background(
                if (enabled) tokens.segmentedTrack
                else tokens.segmentedTrack.copy(alpha = 0.5f)
            )
            .border(1.dp, tokens.borderSoft, CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (large) KuroType.headingCard else KuroType.labelStrong,
            color = if (enabled) tokens.textPrimary else tokens.textMuted,
        )
    }
}

// ── KuroDivider ───────────────────────────────────────────────────────────────
// Hairline divider between list rows.

@Composable
fun KuroDivider(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .padding(horizontal = KuroSpacing.lg)
            .height(1.dp)
            .background(tokens.divider),
    )
}

// ── KuroBadge ────────────────────────────────────────────────────────────────
// Generalized label badge (replaces MockBadge, also usable for "Premium", etc.)

@Composable
fun KuroBadge(
    label: String,
    fill: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.sm))
            .background(fill)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text = label, style = KuroType.labelMeta, color = textColor)
    }
}
