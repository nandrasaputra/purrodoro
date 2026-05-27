package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.theme.kuroTokens

// ── KuroListRow ───────────────────────────────────────────────────────────────
// Standard content row: leading slot + title + optional subtitle + trailing slot.

@Composable
fun KuroListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val rowMod = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Row(
        modifier = rowMod
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = KuroType.bodyPrimary, color = tokens.textPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = KuroType.bodySecondary, color = tokens.textSecondary)
            }
        }
        if (trailing != null) trailing()
    }
}

// ── KuroSectionHeader ────────────────────────────────────────────────────────
// Section label with optional trailing action.

@Composable
fun KuroSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = KuroType.labelStrong, color = tokens.textMuted)
        if (action != null) action()
    }
}

// ── KuroFilterBar ────────────────────────────────────────────────────────────
// Horizontal scrolling row of KuroChips.

@Composable
fun KuroFilterBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        options.forEachIndexed { i, label ->
            KuroChip(
                text = label,
                selected = i == selectedIndex,
                onClick = { onSelect(i) },
            )
        }
    }
}

// ── KuroBottomSheet ───────────────────────────────────────────────────────────
// Standard bottom sheet with title and padded content.

@Composable
fun KuroBottomSheet(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentKey: Any? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroAdaptiveBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        contentKey = contentKey,
    ) {
        // The detail sheet redesign inlines its own chrome (close X / TASK / trash)
        // and so opts out of the default headline by passing title = null. Other
        // sheets keep the default centered title for stylistic consistency.
        if (title != null) {
            Text(
                text = title,
                style = KuroType.headingCard,
                color = tokens.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = KuroSpacing.lg),
            )
        }
        content()
    }
}

/** Back + title + trailing text action for sub-pages inside a bottom sheet. */
@Composable
fun KuroBottomSheetSubpageHeader(
    title: String,
    onBack: () -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.sm, bottom = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.ArrowLeft,
            contentDescription = "Back",
            tint = tokens.textPrimary,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(2.dp),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = title,
            style = KuroType.headingCard,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = actionLabel,
            style = KuroType.labelStrong,
            color = if (actionEnabled) tokens.accent else tokens.accent.copy(alpha = 0.45f),
            modifier = Modifier
                .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                .clickable(enabled = actionEnabled, onClick = onAction)
                .padding(horizontal = KuroSpacing.xs, vertical = KuroSpacing.xs),
        )
    }
}

/** Selectable option list for bottom-sheet pickers. */
@Composable
fun KuroSelectionList(
    emptyMessage: String,
    options: List<Pair<Int, String>>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    if (options.isEmpty()) {
        Text(
            text = emptyMessage,
            style = KuroType.bodySecondary,
            color = tokens.textSecondary,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }

    val listModifier = modifier
        .fillMaxWidth()
        .let { base ->
            if (maxHeight != null) {
                base.heightIn(max = maxHeight).verticalScroll(rememberScrollState())
            } else {
                base
            }
        }

    Column(
        modifier = listModifier,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        options.forEach { (id, label) ->
            val selected = id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                    .background(if (selected) tokens.accentSoft else tokens.pageBackground)
                    .border(
                        1.dp,
                        if (selected) tokens.accent.copy(alpha = 0.35f) else tokens.cardBorder,
                        RoundedCornerShape(KuroSpacing.radiusMd),
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = KuroType.bodyPrimary,
                    color = tokens.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ── KuroInputField ────────────────────────────────────────────────────────────
// Styled text field — warm border, no underline, rounded corners.

@Composable
fun KuroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions =
        androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = KuroType.bodyPrimary, color = tokens.textFaint) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(KuroSpacing.radiusMd),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = tokens.cardFill,
            unfocusedContainerColor = tokens.cardFill,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = tokens.textPrimary,
            unfocusedTextColor = tokens.textPrimary,
            cursorColor = tokens.accent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd)),
    )
}

// ── KuroSegmentedControl ─────────────────────────────────────────────────────
// Generalised N-option segmented control. Replaces AppearanceModeToggle.

enum class KuroSegmentedControlStyle {
    /** Filled white pill — Tasks, Habits */
    Filled,
    /** Outlined accent border — Journey period tabs */
    Outlined,
}

@Composable
fun KuroSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: KuroSegmentedControlStyle = KuroSegmentedControlStyle.Filled,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.segmentedTrack)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusPill))
            .padding(KuroSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            val segmentShape = RoundedCornerShape(KuroSpacing.radiusMd)
            val segmentFill = when {
                !selected -> Color.Transparent
                style == KuroSegmentedControlStyle.Filled -> tokens.cardFill
                else -> Color.Transparent
            }
            val segmentBorder = when {
                selected && style == KuroSegmentedControlStyle.Outlined -> tokens.accent
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(segmentShape)
                    .background(segmentFill)
                    .border(1.dp, segmentBorder, segmentShape)
                    .clickable { onSelect(i) }
                    .padding(vertical = KuroSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (selected) KuroType.labelStrong else KuroType.bodySecondary,
                    color = if (selected) tokens.accent else tokens.textSecondary,
                )
            }
        }
    }
}
