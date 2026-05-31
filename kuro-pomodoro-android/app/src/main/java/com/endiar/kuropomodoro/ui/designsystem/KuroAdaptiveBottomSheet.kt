package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MaxSheetHeightFraction = 0.92f
private val SheetCornerRadius = 28.dp
private val CloseButtonSize = 44.dp

private val IntSizeVectorConverter: TwoWayConverter<IntSize, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.width.toFloat(), it.height.toFloat()) },
        convertFromVector = { IntSize(it.v1.toInt(), it.v2.toInt()) },
    )

/**
 * Like [androidx.compose.animation.animateContentSize] but anchors content to the BOTTOM of the
 * animated bounds. When the sheet shrinks (tall → short), the content stays fixed at the bottom
 * and the top edge of the sheet descends. Empty space appears at the top, not the bottom.
 */
private fun Modifier.animateContentSizeBottomAnchored(
    animationSpec: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    ),
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val sizeAnim = remember { Animatable(IntSize.Zero, IntSizeVectorConverter) }

    clipToBounds().then(
        object : androidx.compose.ui.layout.LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                val naturalSize = IntSize(placeable.width, placeable.height)

                val animValue = sizeAnim.value
                val isFirstRender = animValue == IntSize.Zero

                if (isFirstRender) {
                    scope.launch { sizeAnim.snapTo(naturalSize) }
                } else if (sizeAnim.targetValue != naturalSize) {
                    scope.launch { sizeAnim.animateTo(naturalSize, animationSpec) }
                }

                val reportedSize = if (isFirstRender) naturalSize else animValue

                return layout(
                    width = reportedSize.width,
                    height = reportedSize.height.coerceAtLeast(0),
                ) {
                    placeable.placeRelative(
                        x = 0,
                        y = (reportedSize.height - naturalSize.height).coerceAtLeast(0),
                    )
                }
            }
        },
    )
}

@Composable
fun KuroBottomSheetDragHandle(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .padding(top = KuroSpacing.sm, bottom = KuroSpacing.xs)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.cardBorder),
    )
}

/**
 * Bottom sheet that wraps content when it fits on screen, otherwise caps height and
 * scrolls the body with an optional sticky footer. Dismiss via scrim tap or back press.
 */
@Composable
fun KuroAdaptiveBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentKey: Any? = null,
    horizontalPadding: Dp = KuroSpacing.xl,
    containerColor: Color? = null,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    showCloseButton: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val sheetColor = containerColor ?: tokens.cardFill
    val scrollState = rememberScrollState()
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * MaxSheetHeightFraction

    var visible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }

    fun dismissAnimated() {
        if (dismissing) return
        dismissing = true
        visible = false
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(dismissing) {
        if (dismissing) {
            delay(KuroSheetMotion.ExitDurationMs.toLong())
            onDismiss()
        }
    }

    LaunchedEffect(contentKey) {
        scrollState.scrollTo(0)
    }

    Dialog(
        onDismissRequest = ::dismissAnimated,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(
                    tween(
                        durationMillis = KuroSheetMotion.EnterDurationMs,
                        easing = KuroSheetMotion.EnterEasing,
                    ),
                ),
                exit = fadeOut(
                    tween(
                        durationMillis = KuroSheetMotion.ExitDurationMs,
                        easing = KuroSheetMotion.ExitEasing,
                    ),
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimColor)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = ::dismissAnimated,
                        ),
                )
            }

            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = KuroSheetMotion.EnterDurationMs,
                        easing = KuroSheetMotion.EnterEasing,
                    ),
                    initialOffsetY = { fullHeight -> fullHeight },
                ) + fadeIn(
                    tween(
                        durationMillis = KuroSheetMotion.EnterDurationMs,
                        easing = KuroSheetMotion.EnterEasing,
                    ),
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = KuroSheetMotion.ExitDurationMs,
                        easing = KuroSheetMotion.ExitEasing,
                    ),
                    targetOffsetY = { fullHeight -> fullHeight },
                ) + fadeOut(
                    tween(
                        durationMillis = KuroSheetMotion.ExitDurationMs,
                        easing = KuroSheetMotion.ExitEasing,
                    ),
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Close affordance floats *above* the card (on the scrim) so the
                    // card itself starts at its content — no empty header band. It
                    // slides in/out with the sheet and dismisses like a scrim tap.
                    if (showCloseButton) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = KuroSpacing.sm, top = KuroSpacing.sm, bottom = KuroSpacing.sm),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(CloseButtonSize)
                                    .clip(CircleShape)
                                    .background(sheetColor)
                                    .clickable(onClick = ::dismissAnimated)
                                    .semantics { role = Role.Button; contentDescription = "Close" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = null,
                                    tint = tokens.textSecondary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxSheetHeight)
                        .animateContentSizeBottomAnchored()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                        .background(sheetColor)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        )
                        .navigationBarsPadding(),
                ) {
                    KuroBottomSheetDragHandle(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding),
                    ) {
                        // The scrollable body uses weight(fill = false) so the sheet
                        // wraps its content when short, but caps at the parent's
                        // bounded max height (from heightIn) and scrolls when the
                        // content overflows — all while the footer stays pinned.
                        AnimatedContent(
                            targetState = contentKey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            transitionSpec = {
                                fadeIn(
                                    tween(
                                        durationMillis = KuroSheetMotion.ContentSwapDurationMs,
                                        easing = KuroSheetMotion.EnterEasing,
                                    ),
                                ).togetherWith(
                                    fadeOut(
                                        tween(
                                            durationMillis = KuroSheetMotion.ContentSwapDurationMs,
                                            easing = KuroSheetMotion.ExitEasing,
                                        ),
                                    ),
                                )
                            },
                            label = "KuroAdaptiveBottomSheetContent",
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                content = content,
                            )
                        }

                        if (footer != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = KuroSpacing.lg, bottom = KuroSpacing.xxl),
                            ) {
                                footer()
                            }
                        } else {
                            Spacer(Modifier.height(KuroSpacing.xxl))
                        }
                    }
                }
                }
            }
        }
    }
}
