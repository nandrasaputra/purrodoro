package com.endiar.purrodoro.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.ui.theme.kuroTokens

/**
 * Collapsible "More options" row used by Start Focus create-task / create-habit
 * pages. Collapsed by default so the fast path (name only) stays quick; power
 * users can expand to access the full field set without leaving the sheet.
 */
@Composable
fun KuroMoreOptionsAccordion(
    expanded: Boolean,
    onToggle: () -> Unit,
    previewText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val shape = RoundedCornerShape(KuroSpacing.radiusMd)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(tokens.pageBackground)
                .border(1.dp, tokens.cardBorder, shape)
                .clickable(onClick = onToggle)
                .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            Icon(
                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "More options",
                style = KuroType.labelStrong,
                color = tokens.accent,
                modifier = Modifier.weight(1f),
            )
            if (!expanded) {
                Text(
                    text = previewText,
                    style = KuroType.labelMeta,
                    color = tokens.textMuted,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KuroSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                content()
            }
        }
    }
}
