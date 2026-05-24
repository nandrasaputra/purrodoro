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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
// Standard bottom sheet with drag handle, title, and padded content.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuroBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.pageBackground,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md)) {
            Text(
                text = title,
                style = KuroType.headingCard,
                color = tokens.textPrimary,
                modifier = Modifier.padding(bottom = KuroSpacing.md),
            )
            content()
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
    singleLine: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = KuroType.bodyPrimary, color = tokens.textFaint) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
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

@Composable
fun KuroSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.segmentedTrack)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) tokens.interactiveFill else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = KuroSpacing.sm, horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (selected) KuroType.labelStrong else KuroType.bodySecondary,
                    color = if (selected) tokens.interactiveText else tokens.textSecondary,
                )
            }
        }
    }
}
