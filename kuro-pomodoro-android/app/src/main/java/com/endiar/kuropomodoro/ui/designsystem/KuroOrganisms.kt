package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar

/**
 * Root scaffold for all flat-surface tab screens (Todo, Habits, etc.).
 *
 * Provides:
 * - [KuroFlatSurface] page background
 * - [ScreenStickyBar] that appears once [headerContent] scrolls out of view
 * - Shared [ScrollState] passed to [content] for the screen's scrollable body
 *
 * @param stickyTitle   Title shown in the sticky bar once header scrolls off.
 * @param headerContent Composable for the full visible header (measured to
 *                      determine when to show the sticky bar).
 * @param content       The scrollable body. Receives the shared [ScrollState]
 *                      and a [BoxScope] to anchor overlays (e.g. FAB).
 */
@Composable
fun KuroScreenScaffold(
    stickyTitle: String,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
    content: @Composable BoxScope.(scrollState: ScrollState) -> Unit,
) {
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 8.dp.roundToPx() }
    val showStickyBar by remember {
        derivedStateOf { scrollState.value > headerHeightPx + topPaddingPx }
    }

    KuroFlatSurface(modifier = modifier) {
        // Sticky bar (animated in/out)
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            ScreenStickyBar(title = stickyTitle, modifier = Modifier.fillMaxWidth())
        }

        // Header measurement overlay (invisible, zero-alpha)
        Box(modifier = Modifier.fillMaxWidth().onSizeChanged { headerHeightPx = it.height }) {
            headerContent()
        }

        // Screen body
        content(scrollState)
    }
}
