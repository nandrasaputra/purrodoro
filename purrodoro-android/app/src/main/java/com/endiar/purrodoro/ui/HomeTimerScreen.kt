package com.endiar.purrodoro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleStop
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.HomeScene
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerMode
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.resolveHomeHeroDrawable
import androidx.compose.material3.MaterialTheme
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.formatSeconds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import com.endiar.purrodoro.ui.designsystem.KuroAccountAvatarContent
import com.endiar.purrodoro.ui.designsystem.KuroGuestAvatarContent
import com.endiar.purrodoro.ui.designsystem.KuroPremiumAvatarFrame
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.designsystem.pressScale
import com.endiar.purrodoro.ui.theme.Mint
import com.endiar.purrodoro.ui.theme.Mint
import com.endiar.purrodoro.ui.theme.Lavender
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens
import com.endiar.purrodoro.ui.theme.luminance
import com.endiar.purrodoro.ui.tutorial.TutorialTarget
import com.endiar.purrodoro.ui.tutorial.tutorialAnchor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeTimerScreen(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    activeTask: FocusTask?,
    suggestion: HomeSuggestion,
    suggestionFocusMinutes: Int,
    activeFocusType: FocusType = FocusType.Empty,
    greeting: HomeGreeting,
    userName: String = "",
    accountUserId: String? = null,
    todayMetrics: TodayFocusMetrics,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    backdropTime: LocalTime,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
    onRequestStartFocus: () -> Unit,
    onContinueFocus: () -> Unit = {},
    onStop: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenSoundscape: () -> Unit = {},
    onMuteSoundscape: () -> Unit = {},
    playingAmbienceName: String? = null,
    playingMusicName: String? = null,
    ambienceVolume: Float = 0.30f,
    musicVolume: Float = 0.50f,
    homeScene: HomeScene = HomeScene.Room,
    onChangeHomeScene: () -> Unit = {},
    isPremium: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // contentPadding carries the full measured dock height (tabs + transport bar +
    // nav bar inset). This extra value is pure breathing room so the last card
    // never sits flush against the dock edge when scrolled fully down.
    val scrollBottomPadding = KuroSpacing.xl + KuroSpacing.xxxl
    // Raw status-bar inset. The parent applies statusBarsPadding to the whole
    // tab surface, so the sticky bar would otherwise start below the status
    // bar and leave a mismatched strip above it. We let the sticky bar extend
    // up behind the status bar (offset up by this, content padded down by it).
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Measured height of the full (scrolling) header. Once the user has
    // scrolled roughly past it, the compact sticky bar fades in so the name +
    // profile avatar stay reachable without jumping back to the top.
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val showStickyBar by remember {
        derivedStateOf {
            headerHeightPx > 0 && scrollState.value > headerHeightPx * 0.6f
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(horizontal = KuroSpacing.lg)
                // dockClearance keeps the last content row (e.g. the
                // AutoCompleteTip while a cycle is running) above the
                // floating dock instead of being half-hidden behind it.
                .padding(top = 18.dp, bottom = scrollBottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeHeader(
                greeting = greeting,
                userName = userName,
                accountUserId = accountUserId,
                onOpenProfile = onOpenProfile,
                isPremium = isPremium,
                modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
            )
            if (isStarted) {
                // Auto-complete tip rides along with the ActiveHero (rendered
                // inside its Column at the bottom) so it can't be pushed off
                // the visible area by the floating dock — the hero owns the
                // entire "what's happening" region of the screen.
                val autoCompleteMessage: String? = when {
                    activeFocusType == FocusType.Task &&
                        activeTask != null && activeTask.id > 0 ->
                        stringResource(R.string.home_autocomplete_task)

                    activeFocusType == FocusType.Habit ->
                        stringResource(R.string.home_autocomplete_habit)

                    else -> null
                }
                ActiveHero(
                    timerMode = timerMode,
                    remainingSeconds = remainingSeconds,
                    isRunning = isRunning,
                    activeTask = activeTask,
                    activeFocusType = activeFocusType,
                    cycleItems = cycleItems,
                    cycleIndex = cycleIndex,
                    currentTimerState = currentTimerState,
                    timerSettings = timerSettings,
                    backdropTime = backdropTime,
                    homeScene = homeScene,
                    onChangeHomeScene = onChangeHomeScene,
                    onSkip = onSkip,
                    onStartPause = onStartPause,
                    onEnterFocusMode = onEnterFocusMode,
                    onStop = onStop,
                    playingAmbienceName = playingAmbienceName,
                    playingMusicName = playingMusicName,
                    onOpenSoundscape = onOpenSoundscape,
                    onMuteSoundscape = onMuteSoundscape,
                    autoCompleteMessage = autoCompleteMessage,
                )
            } else {
                IdleHero(
                    backdropTime = backdropTime,
                    homeScene = homeScene,
                    onChangeHomeScene = onChangeHomeScene,
                    onRequestStartFocus = onRequestStartFocus,
                )
            }
            // While a cycle is running or paused the ActiveHero already owns the
            // "what's happening" region, so the suggestion card would be noise —
            // only surface it when the timer is idle.
            if (!isStarted) {
                SuggestionCard(
                    suggestion = suggestion,
                    focusMinutes = suggestionFocusMinutes,
                    onClick = onContinueFocus,
                )
                // Deliberate section break between the "act now" group (hero +
                // suggestion) and the "reflect" group (Today's Focus + quote).
                // 8dp on top of the column's 12dp rhythm = a 20dp (lg) step,
                // large enough to read as grouping without becoming a dead zone.
                Spacer(Modifier.height(KuroSpacing.sm))
            }
            TodayMetricsCard(metrics = todayMetrics)
            QuoteCard()
        }

        // Collapsing sticky header: pinned to the top, sliding/fading in once
        // the full header has scrolled away. Sits above the scrolling content
        // and paints the page background so rows pass cleanly beneath it.
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -statusBarTop),
        ) {
            HomeStickyBar(
                userName = userName,
                accountUserId = accountUserId,
                onOpenProfile = onOpenProfile,
                isPremium = isPremium,
                topInset = statusBarTop,
            )
        }

    }
}

@Composable
private fun HomeStickyBar(
    userName: String,
    accountUserId: String?,
    onOpenProfile: () -> Unit,
    isPremium: Boolean = false,
    topInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val displayName = userName.trim().ifBlank { stringResource(R.string.home_default_name) }
    // Uses the dock surface so the top bar + bottom dock read as one "app
    // chrome" plane, clearly distinct from both the page background and the
    // content cards. A soft shadow + hairline give it lift off the content.
    // The fill extends up behind the status bar (topInset), while the title
    // row is padded down by the same amount to clear the system clock/icons.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp)
            .background(tokens.dockFill),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset)
                .padding(horizontal = KuroSpacing.lg)
                .padding(top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayName,
                color = tokens.textPrimary,
                style = KuroType.headingScreen,
                fontFamily = NunitoFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(KuroSpacing.md))
            CatAvatar(
                onClick = onOpenProfile,
                size = 48.dp,
                isPremium = isPremium,
                userId = accountUserId,
                displayName = displayName,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(tokens.dockBorder)
        )
    }
}

@Composable
private fun HomeHeader(
    greeting: HomeGreeting,
    userName: String,
    accountUserId: String? = null,
    onOpenProfile: () -> Unit = {},
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))
    }
    // Fall back to a warm, generic greeting target until the user sets a name
    // in Settings, so the headingDisplay line is never empty.
    val displayName = userName.trim().ifBlank { stringResource(R.string.home_default_name) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = KuroSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
                    Text(
                        text = greeting.salutation,
                        color = tokens.textSecondary,
                        style = KuroType.bodyPrimary,
                        fontFamily = NunitoFamily,
                    )
                    Text(
                        text = displayName,
                        color = tokens.textPrimary,
                        style = KuroType.headingDisplay,
                        fontFamily = NunitoFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LeafSprig(
                    modifier = Modifier
                        .padding(start = KuroSpacing.sm)
                        .padding(top = 14.dp)
                        .size(23.dp, 18.dp),
                    veinColor = tokens.textPrimary.copy(alpha = 0.30f),
                )
            }
            Text(
                text = greeting.encouragement,
                color = tokens.textSecondary,
                style = KuroType.headingCard,
                fontFamily = NunitoFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dateLabel,
                color = tokens.textSecondary,
                style = KuroType.labelMeta,
                fontFamily = NunitoFamily,
            )
        }
        Spacer(Modifier.width(KuroSpacing.md))
        CatAvatar(
            onClick = onOpenProfile,
            isPremium = isPremium,
            userId = accountUserId,
            displayName = displayName,
            modifier = Modifier.align(Alignment.Top),
        )
    }
}

/** Hand-drawn mint leaf sprig used as a quiet flourish beside the name. */
@Composable
private fun LeafSprig(modifier: Modifier = Modifier, veinColor: Color) {
    Canvas(modifier = modifier) {
        // Leaf body — a soft pointed oval angled up to the right.
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.86f)
            cubicTo(
                size.width * 0.05f, size.height * 0.35f,
                size.width * 0.45f, size.height * 0.02f,
                size.width * 0.92f, size.height * 0.10f
            )
            cubicTo(
                size.width * 0.78f, size.height * 0.52f,
                size.width * 0.55f, size.height * 0.82f,
                size.width * 0.12f, size.height * 0.86f
            )
            close()
        }
        drawPath(path = path, color = Mint)
        // Central vein.
        drawLine(
            color = veinColor,
            start = Offset(size.width * 0.18f, size.height * 0.82f),
            end = Offset(size.width * 0.86f, size.height * 0.16f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun HeroSceneImage(
    scene: HomeScene,
    time: LocalTime,
    onChangeScene: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val resId = resolveHomeHeroDrawable(scene, isDark, time)
    val changeSceneLabel = stringResource(R.string.home_change_scene_cd)
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .aspectRatio(1.48f)
            .clip(RoundedCornerShape(KuroSpacing.radiusMd)),
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(KuroSpacing.xs)
                .size(48.dp)
                .clickable(onClick = onChangeScene, onClickLabel = changeSceneLabel)
                .semantics { contentDescription = changeSceneLabel },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tokens.cardFill.copy(alpha = 0.92f))
                    .border(1.dp, tokens.cardBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.Layers,
                    contentDescription = null,
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun IdleHero(
    backdropTime: LocalTime,
    homeScene: HomeScene,
    onChangeHomeScene: () -> Unit,
    onRequestStartFocus: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        HeroSceneImage(
            scene = homeScene,
            time = backdropTime,
            onChangeScene = onChangeHomeScene,
            modifier = Modifier.fillMaxWidth(),
        )
        HomePrimaryButton(
            text = stringResource(R.string.home_start_focus),
            onClick = onRequestStartFocus,
            icon = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = KuroSpacing.xl)
                .offset(y = 27.dp)
                .tutorialAnchor(TutorialTarget.StartFocus)
        )
    }
    Spacer(Modifier.height(27.dp))
}

/**
 * Reduces the laid-out height by [amount] without moving children, reclaiming
 * the dead band left behind when a child is shifted up with a negative
 * `offset` (offset is visual-only and doesn't shrink the reserved space).
 */
private fun Modifier.trimBottom(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val trimmed = (placeable.height - amount.roundToPx()).coerceAtLeast(0)
    layout(placeable.width, trimmed) {
        placeable.place(0, 0)
    }
}

@Composable
private fun ActiveHero(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    activeTask: FocusTask?,
    activeFocusType: FocusType,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    backdropTime: LocalTime,
    homeScene: HomeScene,
    onChangeHomeScene: () -> Unit,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onEnterFocusMode: () -> Unit,
    onStop: () -> Unit,
    playingAmbienceName: String? = null,
    playingMusicName: String? = null,
    onOpenSoundscape: () -> Unit = {},
    onMuteSoundscape: () -> Unit = {},
    autoCompleteMessage: String? = null,
) {
    // The compact timer card (and tip) are pulled upward with a negative offset,
    // which is purely visual — the Column still reserves the un-offset height.
    // Reclaim most of that band but leave ~20dp (lg) before "Today's Focus".
    val deadSpace = if (autoCompleteMessage != null) 8.dp else 12.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .trimBottom(deadSpace)
    ) {
        HeroSceneImage(
            scene = homeScene,
            time = backdropTime,
            onChangeScene = onChangeHomeScene,
            modifier = Modifier.fillMaxWidth(),
        )
        ActiveTimerCard(
            timerMode = timerMode,
            remainingSeconds = remainingSeconds,
            isRunning = isRunning,
            activeTask = activeTask,
            activeFocusType = activeFocusType,
            cycleItems = cycleItems,
            cycleIndex = cycleIndex,
            currentTimerState = currentTimerState,
            timerSettings = timerSettings,
            onSkip = onSkip,
            onStartPause = onStartPause,
            onEnterFocusMode = onEnterFocusMode,
            onStop = onStop,
            playingAmbienceName = playingAmbienceName,
            playingMusicName = playingMusicName,
            onOpenSoundscape = onOpenSoundscape,
            onMuteSoundscape = onMuteSoundscape,
            modifier = Modifier
                .offset(y = (-24).dp)
                .padding(horizontal = 12.dp)
        )
    // The tip rides up so its top edge sits just under the compact timer
    // card (the card itself is offset by -24.dp). Without this shift the
    // tip lands below the visual bottom of the card and gets clipped by
    // the floating dock on shorter screens.
        if (autoCompleteMessage != null) {
            AutoCompleteTip(
                message = autoCompleteMessage,
                modifier = Modifier
                    .offset(y = (-16).dp)
                    .padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun ActiveTimerCard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    activeTask: FocusTask?,
    activeFocusType: FocusType,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onEnterFocusMode: () -> Unit,
    onStop: () -> Unit,
    playingAmbienceName: String? = null,
    playingMusicName: String? = null,
    onOpenSoundscape: () -> Unit = {},
    onMuteSoundscape: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentType = cycleItems.getOrNull(cycleIndex)?.type
    val isBreak = currentType is SessionItemType.ShortBreak || currentType is SessionItemType.LongBreak
    val isFocus = currentType is SessionItemType.Focus
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    val tokens = MaterialTheme.colorScheme.kuroTokens

    val infiniteTransition = rememberInfiniteTransition(label = "pausePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val timeAlpha = if (currentTimerState == TimerState.Paused) pulseAlpha else 1f

    val (displayEmoji, displayTitle) = when {
        activeFocusType == FocusType.Habit && activeTask != null -> {
            val parts = activeTask.title.split(" ", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null to activeTask.title
        }
        activeTask != null -> null to activeTask.title
        // No concrete task: title IS the timer state label (free focus / bare break)
        else -> null to timerModeLabel(timerMode)
    }

    val modeLabel = timerModeLabel(timerMode)
    val intentLabel = if (activeFocusType != FocusType.Empty) focusTypeLabel(activeFocusType) else null

    // Show subtitle only when title is NOT already the timer mode label (i.e. there's
    // a concrete task/habit/note occupying the title line). Combine mode + intent so
    // both are always visible: "Focus · Task", "Short Break · Habit", etc.
    val hasConcreteTitle = activeTask != null
    val subtitle: String? = if (hasConcreteTitle) {
        if (intentLabel != null) "$modeLabel · $intentLabel" else modeLabel
    } else {
        // Title is already the mode label — show intent alone if available, else nothing.
        intentLabel
    }

    val fullTitleForA11y = when {
        activeTask != null -> activeTask.title
        else -> modeLabel
    }
    HomeCardSurface(modifier = modifier, color = tokens.cardFill) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = buildString {
                                append(fullTitleForA11y)
                                subtitle?.let { append(", $it") }
                            }
                        },
                ) {
                    Text(
                        text = displayTitle,
                        color = tokens.textPrimary,
                        style = KuroType.headingCard,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = tokens.textSecondary,
                            style = KuroType.labelStrong,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = formatSeconds(remainingSeconds),
                    color = tokens.textPrimary.copy(alpha = timeAlpha),
                    style = KuroType.displayTimerCompact,
                    fontFamily = NunitoFamily,
                    maxLines = 1,
                )
                CompactPauseButton(
                    isRunning = isRunning,
                    onClick = onStartPause,
                )
            }
            CompactCycleProgressBar(
                cycleItems = cycleItems,
                cycleIndex = cycleIndex,
                currentTimerState = currentTimerState,
                isBreak = isBreak,
                isLongBreak = currentType is SessionItemType.LongBreak,
            )
            ActiveSessionActionChips(
                isBreak = isBreak,
                onEnterFocusMode = onEnterFocusMode,
                onSkip = onSkip,
                onStop = {
                    if (isFocus) showStopConfirmDialog = true else onStop()
                },
            )
        }
    }

    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.home_stop_title),
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.home_stop_body),
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopConfirmDialog = false
                    onStop()
                }) {
                    Text(stringResource(R.string.home_stop_confirm), color = tokens.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmDialog = false }) {
                    Text(stringResource(R.string.home_stop_dismiss), color = tokens.textSecondary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.xl)
        )
    }
}

@Composable
private fun ActiveSessionActionChips(
    isBreak: Boolean,
    onEnterFocusMode: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val focusModeLabel = stringResource(R.string.home_focus_mode_chip)
    val openFocusModeLabel = stringResource(R.string.home_open_focus_mode)
    val skipChipLabel = stringResource(R.string.home_skip_chip)
    val skipBreakLabel = stringResource(R.string.home_skip_break)
    val stopChipLabel = stringResource(R.string.home_stop_chip)
    val stopSessionLabel = stringResource(R.string.home_stop_session)

    // Plain Row — FlowRow has an ABI mismatch with the bundled foundation runtime
    // (same reason TodoScreen avoids it). Short chip labels fit on a single line.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedSessionChip(
            onClick = onEnterFocusMode,
            icon = Lucide.Leaf,
            label = focusModeLabel,
            contentDescription = openFocusModeLabel,
            iconTint = tokens.accent,
            labelColor = tokens.textPrimary,
        )
        if (isBreak) {
            OutlinedSessionChip(
                onClick = onSkip,
                icon = Lucide.SkipForward,
                label = skipChipLabel,
                contentDescription = skipBreakLabel,
                iconTint = tokens.textSecondary,
                labelColor = tokens.textSecondary,
            )
        }
        OutlinedSessionChip(
            onClick = onStop,
            icon = Lucide.CircleStop,
            label = stopChipLabel,
            contentDescription = stopSessionLabel,
            iconTint = tokens.textSecondary,
            labelColor = tokens.textSecondary,
        )
    }
}

@Composable
private fun OutlinedSessionChip(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    contentDescription: String,
    iconTint: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.98f)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusPill))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = labelColor,
            style = KuroType.labelStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactCycleProgressBar(
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    isBreak: Boolean,
    isLongBreak: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val focusIndices = cycleItems.mapIndexedNotNull { index, item ->
        if (item.type is SessionItemType.Focus) index else null
    }
    if (focusIndices.isEmpty()) return

    val breakTint = if (isLongBreak) Lavender else Mint

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        focusIndices.forEach { originalIndex ->
            val isActive = originalIndex == cycleIndex && currentTimerState != TimerState.Idle
            val isPast = originalIndex < cycleIndex
            val segmentHeight by animateDpAsState(
                targetValue = if (isActive) 5.dp else 4.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "cycleSegmentHeight",
            )
            val segmentColor by animateColorAsState(
                targetValue = when {
                    isActive && isBreak -> breakTint
                    isActive -> tokens.accent
                    isPast -> tokens.accent.copy(alpha = 0.55f)
                    else -> tokens.segmentedTrack
                },
                animationSpec = tween(durationMillis = 260, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                label = "cycleSegmentColor",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(segmentHeight)
                    .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                    .background(segmentColor),
            )
        }
    }
}

@Composable
private fun CompactPauseButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    val iconPop = remember { Animatable(1f) }
    var popInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isRunning) {
        if (popInitialized) {
            iconPop.snapTo(0.7f)
            iconPop.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        popInitialized = true
    }
    val pauseLabel = stringResource(
        if (isRunning) R.string.home_pause_focus else R.string.home_resume_focus,
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .pressScale(interactionSource, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(tokens.accent)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .semantics { contentDescription = pauseLabel },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isRunning) Lucide.Pause else Lucide.Play,
            contentDescription = null,
            tint = tokens.accentOn,
            modifier = Modifier
                .size(20.dp)
                .scale(iconPop.value),
        )
    }
}

/**
 * State-driven Home entry-point card. Renders one of three shapes:
 *
 *  - [HomeSuggestion.Continue]: a real task/habit focus was stopped mid-segment
 *    and is still unfinished — offer to pick it back up, surfacing the leftover
 *    time ("adjust the time before you start" reminds the user the sheet lets
 *    them tweak it).
 *  - [HomeSuggestion.Suggest]: nothing was interrupted — gently propose one
 *    task/habit to focus on, with a quiet "rotates gently" footnote.
 *  - [HomeSuggestion.Empty]: nothing to continue or suggest — a calm prompt to
 *    pick a focus.
 *
 * The icon and identity follow the focus type (task vs habit), so the tile is
 * never a misleading fixed glyph.
 */
@Composable
private fun SuggestionCard(
    suggestion: HomeSuggestion,
    focusMinutes: Int,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    val header: String
    val title: String
    val subtitle: String?
    val trailingPill: String?
    val focusType: FocusType
    val emoji: String?

    when (suggestion) {
        is HomeSuggestion.Continue -> {
            header = stringResource(R.string.home_resume_header)
            focusType = suggestion.focusType
            val (e, t) = splitEmojiTitle(suggestion.focusType, suggestion.title)
            emoji = e
            title = t
            if (suggestion.remainingSeconds > 0) {
                val left = interruptedRemainingMinutes(suggestion.remainingSeconds)
                subtitle = null
                trailingPill = stringResource(R.string.home_min_left, left)
            } else {
                val sessions = suggestion.remainingFocusSessions.coerceAtLeast(1)
                val totalMinutes = sessions * (suggestion.focusMinutes ?: focusMinutes).coerceAtLeast(1)
                subtitle = stringResource(R.string.home_resume_left, formatResumeMinutes(totalMinutes))
                trailingPill = pluralStringResource(R.plurals.sessions_count, sessions, sessions)
            }
        }
        is HomeSuggestion.Suggest -> {
            header = stringResource(R.string.home_ready_header)
            focusType = suggestion.focusType
            val (e, t) = splitEmojiTitle(suggestion.focusType, suggestion.title)
            emoji = e
            title = t
            subtitle = listOfNotNull(
                focusTypeLabel(suggestion.focusType),
                stringResource(R.string.home_suggested_for_now),
                stringResource(R.string.home_min_focus, focusMinutes),
            ).joinToString(" · ")
            trailingPill = null
        }
        HomeSuggestion.Empty -> {
            header = stringResource(R.string.home_ready_header)
            focusType = FocusType.Empty
            emoji = null
            title = stringResource(R.string.home_choose_focus_task)
            subtitle = stringResource(R.string.home_empty_subtitle)
            trailingPill = null
        }
    }

    HomeCardSurface(modifier = Modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = header,
                    color = tokens.textSecondary,
                    style = KuroType.labelStrong
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusTypeIcon(focusType = focusType, emojiOverride = emoji)
                Spacer(Modifier.width(KuroSpacing.md))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        color = tokens.textPrimary,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            color = tokens.textSecondary,
                            style = KuroType.bodySecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                trailingPill?.let {
                    Spacer(Modifier.width(KuroSpacing.sm))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                            .background(tokens.activeCardFill)
                            .border(
                                1.dp,
                                tokens.cardBorder.copy(alpha = 0.65f),
                                RoundedCornerShape(KuroSpacing.radiusPill)
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = it,
                            color = tokens.textSecondary,
                            style = KuroType.labelStrong,
                            maxLines = 1,
                        )
                    }
                }
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = tokens.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Splits a leading emoji out of a habit title (e.g. "📚 Read") so it can render
 * inside the icon tile. Tasks/simple notes keep their whole title and use the
 * type's vector icon instead.
 */
private fun splitEmojiTitle(focusType: FocusType, title: String): Pair<String?, String> {
    if (focusType != FocusType.Habit) return null to title
    val parts = title.split(" ", limit = 2)
    return if (parts.size == 2) parts[0] to parts[1] else null to title
}

private fun formatResumeMinutes(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(1)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "$minutes min"
    }
}

@Composable
private fun TodayMetricsCard(metrics: TodayFocusMetrics) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    HomeCardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_todays_focus),
                    color = tokens.textPrimary,
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricColumn(label = stringResource(R.string.home_metric_focus_time), value = metrics.focusTimeLabel, modifier = Modifier.weight(1f))
                MetricColumn(label = stringResource(R.string.home_metric_sessions), value = metrics.sessionsLabel, modifier = Modifier.weight(1f))
                MetricColumn(label = stringResource(R.string.home_metric_completed), value = metrics.completedLabel, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuoteCard() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    HomeCardSurface(color = tokens.activeCardFill) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_quote),
                color = tokens.textPrimary,
                style = KuroType.bodyPrimary,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
            SleepingCat(modifier = Modifier.size(88.dp))
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = tokens.textSecondary,
            style = KuroType.labelMeta,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            text = value,
            color = tokens.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Quiet "Purrodoro will mark this done" tip that appears under the active hero when
 * a real task or habit is attached to the running cycle. Communicates the
 * automatic completion behaviour at exactly the moment it becomes relevant —
 * the user is about to spend time focusing.
 *
 * The icon is a sparkle (`AutoAwesome`) rather than a check mark: the tip
 * describes a *future* automatic action, and a check mark would mis-read
 * as "already done" the moment the cycle starts.
 */
@Composable
private fun AutoCompleteTip(
    message: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.accentSoft.copy(alpha = 0.55f))
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .padding(horizontal = KuroSpacing.md, vertical = 11.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(tokens.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.Sparkles,
                contentDescription = null,
                tint = tokens.accentOn,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = message,
            color = tokens.textPrimary,
            style = KuroType.bodySecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeCardSurface(
    modifier: Modifier = Modifier,
    color: Color? = null,
    content: @Composable () -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val bgColor = color ?: tokens.cardFill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(bgColor)
            .border(1.dp, tokens.cardBorder.copy(alpha = 0.60f), RoundedCornerShape(KuroSpacing.radiusMd))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        content()
    }
}

@Composable
private fun HomePrimaryButton(
    text: String,
    onClick: () -> Unit,
    icon: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = KuroSpacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon) {
            Icon(
                imageVector = Lucide.Play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = Color.White,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.ExtraBold)
        )
    }
}

@Composable
private fun FocusTypeIcon(
    focusType: FocusType,
    emojiOverride: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val cornerRadius = size * (14f / 46f)
    val glyphSize = size * (22f / 46f)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tokens.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        if (emojiOverride != null) {
            Text(
                text = emojiOverride,
                fontSize = glyphSize.value.sp,
            )
        } else {
            val icon = when (focusType) {
                FocusType.Task -> Lucide.CircleCheck
                FocusType.Habit -> Lucide.Leaf
                FocusType.SimpleNote -> Lucide.NotebookPen
                FocusType.Empty -> Lucide.Timer
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}

@Composable
private fun focusTypeLabel(focusType: FocusType): String = when (focusType) {
    FocusType.Task -> stringResource(R.string.focus_type_task)
    FocusType.Habit -> stringResource(R.string.focus_type_habit)
    FocusType.SimpleNote -> stringResource(R.string.focus_type_simple_note)
    FocusType.Empty -> stringResource(R.string.focus_type_free)
}

@Composable
private fun timerModeLabel(mode: TimerMode): String = when (mode) {
    TimerMode.Focus -> stringResource(R.string.timer_mode_focus)
    TimerMode.ShortBreak -> stringResource(R.string.timer_mode_short_break)
    TimerMode.LongBreak -> stringResource(R.string.timer_mode_long_break)
}

/**
 * Time-of-day [HomeGreeting] resolved against the active locale. Mirrors the
 * pure [homeGreetingFor] (kept for tests) but pulls copy from string
 * resources so it follows the in-app language.
 */
@Composable
fun rememberHomeGreeting(time: LocalTime): HomeGreeting {
    val hour = time.hour
    return when (hour) {
        // Pre-dawn hours (00:00–04:59) read as night/evening to a human, not
        // "morning" — so they fall through to the evening copy below.
        in 5..11 -> HomeGreeting(
            salutation = stringResource(R.string.greeting_morning_salutation),
            encouragement = stringResource(R.string.greeting_morning_encouragement),
        )
        in 12..16 -> HomeGreeting(
            salutation = stringResource(R.string.greeting_afternoon_salutation),
            encouragement = stringResource(R.string.greeting_afternoon_encouragement),
        )
        else -> HomeGreeting(
            salutation = stringResource(R.string.greeting_evening_salutation),
            encouragement = stringResource(R.string.greeting_evening_encouragement),
        )
    }
}


@Composable
private fun CatAvatar(
    onClick: () -> Unit = {},
    size: Dp = 52.dp,
    isPremium: Boolean = false,
    userId: String? = null,
    displayName: String = "",
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val openProfileLabel = stringResource(R.string.home_open_profile_cd)

    val avatarContent: @Composable () -> Unit = {
        if (userId == null) {
            KuroGuestAvatarContent(modifier = Modifier.fillMaxSize())
        } else {
            KuroAccountAvatarContent(
                userId = userId,
                displayName = displayName,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (isPremium) {
        KuroPremiumAvatarFrame(
            size = size,
            modifier = modifier.clickable(onClick = onClick, onClickLabel = openProfileLabel),
            content = {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    avatarContent()
                }
            },
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(tokens.cardFill)
                .border(1.dp, tokens.cardBorder, CircleShape)
                .clickable(onClick = onClick, onClickLabel = openProfileLabel),
            contentAlignment = Alignment.Center,
        ) {
            avatarContent()
        }
    }
}

@Composable
private fun SleepingCat(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.cat_home_sleeping),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
