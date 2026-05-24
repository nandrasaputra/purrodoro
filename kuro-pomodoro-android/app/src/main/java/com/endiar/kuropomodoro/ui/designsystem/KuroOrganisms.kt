package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.theme.kuroTokens

/**
 * Root scaffold for all flat-surface tab screens (Todo, Habits, Journey, etc.).
 */
@Composable
fun KuroScreenScaffold(
    stickyTitle: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.(scrollState: androidx.compose.foundation.ScrollState, headerMeasureModifier: Modifier) -> Unit,
) {
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topPaddingPx = with(density) { KuroSpacing.sm.roundToPx() }
    val showStickyBar by remember {
        derivedStateOf { scrollState.value > headerHeightPx + topPaddingPx }
    }
    val headerMeasureModifier = Modifier.onSizeChanged { headerHeightPx = it.height }

    KuroFlatSurface(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it },
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            ScreenStickyBar(title = stickyTitle, modifier = Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content(scrollState, headerMeasureModifier)
        }
    }
}

/** Shadowed surface card for the Journey tab (metrics, charts, calendar). */
@Composable
fun KuroElevatedCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(KuroSpacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(KuroSpacing.elevationSoft, RoundedCornerShape(KuroSpacing.radiusLg)),
        shape = RoundedCornerShape(KuroSpacing.radiusLg),
        color = tokens.cardFill,
        border = BorderStroke(1.dp, tokens.cardBorder),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Journey 2×2 stat card — icon badge, label, serif value, footnote. */
@Composable
fun KuroJourneyMetricCard(
    title: String,
    value: String,
    note: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    noteColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroElevatedCard(
        modifier = modifier.height(KuroSpacing.journeyMetricCardHeight),
        contentPadding = PaddingValues(KuroSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = iconBackground,
                modifier = Modifier.size(KuroSpacing.journeyMetricIconCircle),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(KuroSpacing.journeyMetricIconGlyph),
                    )
                }
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(KuroSpacing.sm))
        Text(title, color = tokens.textSecondary, style = KuroType.labelMetric)
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            value,
            color = tokens.textPrimary,
            fontFamily = FontFamily.Serif,
            style = KuroType.metricValueSerif,
        )
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            note,
            color = noteColor ?: tokens.textSecondary,
            style = if (noteColor != null) KuroType.metricDeltaPositive else KuroType.labelMeta,
        )
    }
}
