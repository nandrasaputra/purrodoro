package com.endiar.kuropomodoro.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleStop
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Square
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import androidx.compose.material3.MaterialTheme
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.formatSeconds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.designsystem.pressScale
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.LocalTime

@Composable
fun HomeTimerScreen(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    activeTask: FocusTask?,
    continueTask: FocusTask?,
    activeFocusType: FocusType = FocusType.Empty,
    greeting: HomeGreeting,
    todayMetrics: TodayFocusMetrics,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    pomodoroProgress: PomodoroProgress,
    isSoundPlaying: Boolean,
    backdropTime: LocalTime,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
    onRequestStartFocus: () -> Unit,
    onContinueFocus: () -> Unit = {},
    onStop: () -> Unit,
    onOpenSoundsSheet: () -> Unit,
    onOpenProfile: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = KuroSpacing.lg)
                // dockClearance keeps the last content row (e.g. the
                // AutoCompleteTip while a cycle is running) above the
                // floating dock instead of being half-hidden behind it.
                .padding(top = 18.dp, bottom = KuroSpacing.dockClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeHeader(greeting = greeting, onOpenProfile = onOpenProfile)
            if (isStarted) {
                // Auto-complete tip rides along with the ActiveHero (rendered
                // inside its Column at the bottom) so it can't be pushed off
                // the visible area by the floating dock — the hero owns the
                // entire "what's happening" region of the screen.
                val autoCompleteMessage: String? = when {
                    activeFocusType == FocusType.Task &&
                        activeTask != null && activeTask.id > 0 ->
                        "Finish the Pomodoro — Kuro ticks this."

                    activeFocusType == FocusType.Habit ->
                        "Finish the Pomodoro — Kuro counts today."

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
                    onSkip = onSkip,
                    onStartPause = onStartPause,
                    onEnterFocusMode = onEnterFocusMode,
                    onStop = onStop,
                    autoCompleteMessage = autoCompleteMessage,
                )
            } else {
                IdleHero(backdropTime = backdropTime, onRequestStartFocus = onRequestStartFocus)
            }
            ContinueCard(
                task = continueTask,
                progress = pomodoroProgress,
                onClick = onContinueFocus,
            )
            TodayMetricsCard(metrics = todayMetrics)
            QuoteCard()
            SoundChip(isPlaying = isSoundPlaying, onClick = onOpenSoundsSheet)
        }
    }
}

@Composable
private fun HomeHeader(greeting: HomeGreeting, onOpenProfile: () -> Unit = {}) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting.salutation,
                color = tokens.textSecondary,
                style = KuroType.bodySecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nandra",
                    color = tokens.textPrimary,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif
                )
                Canvas(
                    modifier = Modifier
                        .padding(start = KuroSpacing.sm, top = 5.dp)
                        .size(18.dp, 14.dp)
                ) {
                    drawOval(
                        color = Mint,
                        topLeft = Offset(size.width * 0.12f, size.height * 0.05f),
                        size = Size(size.width * 0.72f, size.height * 0.82f)
                    )
                    drawLine(
                        color = tokens.textPrimary.copy(alpha = 0.35f),
                        start = Offset(size.width * 0.28f, size.height * 0.74f),
                        end = Offset(size.width * 0.90f, size.height * 0.08f),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            Text(
                text = greeting.encouragement,
                color = tokens.textSecondary,
                style = KuroType.bodySecondary,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        CatAvatar(onClick = onOpenProfile)
    }
}

@Composable
private fun HeroImage(modifier: Modifier = Modifier, time: LocalTime) {
    val resId = if (time.hour in 6..17) {
        R.drawable.cat_home_header_day
    } else {
        R.drawable.cat_home_header_night
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1.48f)
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
    )
}

@Composable
private fun IdleHero(backdropTime: LocalTime, onRequestStartFocus: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        HeroImage(modifier = Modifier.fillMaxWidth(), time = backdropTime)
        HomePrimaryButton(
            text = "Start Focus",
            onClick = onRequestStartFocus,
            icon = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = KuroSpacing.xl)
                .offset(y = 27.dp)
        )
    }
    Spacer(Modifier.height(27.dp))
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
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onEnterFocusMode: () -> Unit,
    onStop: () -> Unit,
    autoCompleteMessage: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HeroImage(modifier = Modifier.fillMaxWidth(), time = backdropTime)
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
            modifier = Modifier
                .offset(y = (-72).dp)
                .padding(horizontal = 12.dp)
        )
        // The tip rides up by -64.dp so its top edge sits just under the
        // timer card (the card itself is offset by -72.dp). Without this
        // shift the tip lands well below the visual bottom of the card and
        // gets clipped by the floating dock on shorter screens. The message
        // is intentionally short so it fits on one line and the tip stays
        // a compact, non-obtrusive 44.dp pill.
        if (autoCompleteMessage != null) {
            AutoCompleteTip(
                message = autoCompleteMessage,
                modifier = Modifier
                    .offset(y = (-64).dp)
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
    modifier: Modifier = Modifier
) {
    val currentType = cycleItems.getOrNull(cycleIndex)?.type
    val isBreak = currentType is SessionItemType.ShortBreak || currentType is SessionItemType.LongBreak
    val isFocus = currentType is SessionItemType.Focus
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    val tokens = MaterialTheme.colorScheme.kuroTokens

    // Pause pulse animation. Uses a symmetric ease-in-out (sine-like) curve so
    // the breathing has no mechanical "catch" at the turnaround when RepeatMode
    // reverses — ambient loops should feel continuous at both ends.
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

    HomeCardSurface(modifier = modifier, color = tokens.cardFill) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title: keep task/habit name; only show rawTitle for empty/simple-note focus
            val (displayEmoji, displayTitle) = when {
                activeFocusType == FocusType.Habit && activeTask != null -> {
                    val parts = activeTask.title.split(" ", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null to activeTask.title
                }
                activeTask != null -> null to activeTask.title
                isBreak -> null to "Taking a break"
                else -> null to timerMode.label
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusTypeIcon(focusType = activeFocusType, emojiOverride = displayEmoji)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = tokens.textPrimary,
                        style = KuroType.headingCard,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when {
                        isBreak -> timerMode.label
                        activeFocusType == FocusType.Empty -> null
                        else -> focusTypeLabel(activeFocusType)
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = tokens.textSecondary,
                            style = KuroType.labelStrong
                        )
                    }
                }
            }
            // Large centered serif timer
            Text(
                text = formatSeconds(remainingSeconds),
                color = tokens.textPrimary.copy(alpha = timeAlpha),
                fontSize = 72.sp,
                lineHeight = 76.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "remaining",
                color = tokens.textSecondary,
                style = KuroType.labelStrong,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            FullCycleDots(
                cycleItems = cycleItems,
                cycleIndex = cycleIndex,
                currentTimerState = currentTimerState
            )
            OpenFocusModeRow(onClick = onEnterFocusMode)
            ActiveFocusButton(
                isRunning = isRunning,
                onClick = onStartPause,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    if (isFocus) showStopConfirmDialog = true else onStop()
                }) {
                    Icon(
                        imageVector = Lucide.CircleStop,
                        contentDescription = null,
                        tint = tokens.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Stop session", color = tokens.textSecondary, fontWeight = FontWeight.Bold)
                }
                if (isBreak) {
                    TextButton(onClick = onSkip) {
                        Text("Skip break", color = tokens.textSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = {
                Text(
                    text = "Taking a break?",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "Your focus time so far will still be counted.",
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopConfirmDialog = false
                    onStop()
                }) {
                    Text("Stop for now", color = tokens.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmDialog = false }) {
                    Text("Stay focused", color = tokens.textSecondary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.xl)
        )
    }
}

@Composable
private fun ContinueCard(task: FocusTask?, progress: PomodoroProgress, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    HomeCardSurface(modifier = Modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅", fontSize = 14.sp)
                Spacer(Modifier.width(KuroSpacing.sm))
                Text(
                    text = "Continue where you left off",
                    color = tokens.textPrimary,
                    style = KuroType.labelStrong
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CodeTile()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task?.title ?: "Choose a focus task",
                        color = tokens.textPrimary,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (task != null) progress.completedLabel else "A calm place to begin when you are ready.",
                        color = tokens.textSecondary,
                        style = KuroType.bodySecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    text = "Today's Focus",
                    color = tokens.textPrimary,
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "View all",
                    color = tokens.accent,
                    style = KuroType.bodySecondary.copy(fontWeight = FontWeight.Medium)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricColumn(label = "Focus time", value = metrics.focusTimeLabel, modifier = Modifier.weight(1f))
                MetricColumn(label = "Sessions", value = metrics.sessionsLabel, modifier = Modifier.weight(1f))
                MetricColumn(label = "Completed", value = metrics.completedLabel, modifier = Modifier.weight(1f))
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
                text = "Little by little,\na lot becomes a lot. ♡",
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
private fun SoundChip(isPlaying: Boolean, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(KuroSpacing.xl))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.xl))
            .clickable(onClick = onClick)
            .padding(horizontal = KuroSpacing.md, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPlaying) "Sound on  ♫" else "Sound off  ♫",
            color = tokens.textSecondary,
            style = KuroType.labelStrong
        )
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
 * Quiet "Kuro will mark this done" tip that appears under the active hero when
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
            .padding(horizontal = KuroSpacing.md, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
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
private fun OpenFocusModeRow(onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .pressScale(interactionSource, pressedScale = 0.98f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = KuroSpacing.md, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.Leaf,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Open Focus Mode",
            color = tokens.textPrimary,
            style = KuroType.bodyPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = tokens.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ActiveFocusButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    // Punctuate the play↔pause flip with a quick scale-pop on the glyph so the
    // state change registers as a deliberate beat, not a silent icon swap.
    // Skips the first composition so it doesn't fire on initial render.
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
    Row(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.97f)
            .height(58.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.accent)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(start = 8.dp, end = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) Lucide.Pause else Lucide.Play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconPop.value)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = if (isRunning) "Pause Focus" else "Resume Focus",
            color = Color.White,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.ExtraBold)
        )
    }
}

@Composable
private fun FocusTypeIcon(
    focusType: FocusType,
    emojiOverride: String? = null,
    modifier: Modifier = Modifier
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        if (emojiOverride != null) {
            Text(
                text = emojiOverride,
                fontSize = 22.sp,
            )
        } else {
            val icon = when (focusType) {
                FocusType.Task -> Lucide.CircleCheck
                FocusType.Habit -> Lucide.Leaf
                FocusType.SimpleNote -> Lucide.NotebookPen
                FocusType.Empty -> Lucide.Square
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CodeTile(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "</>",
            color = tokens.accent,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.ExtraBold)
        )
    }
}

private fun focusTypeLabel(focusType: FocusType): String = when (focusType) {
    FocusType.Task -> "Task"
    FocusType.Habit -> "Habit"
    FocusType.SimpleNote -> "Simple Note"
    FocusType.Empty -> "Focus"
}


@Composable
private fun FullCycleDots(
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    // Collect original indices of focus-only items
    val focusIndices = cycleItems.mapIndexedNotNull { index, item ->
        if (item.type is SessionItemType.Focus) index else null
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        focusIndices.forEach { originalIndex ->
            val isActive = originalIndex == cycleIndex && currentTimerState != TimerState.Idle
            val isPast = originalIndex < cycleIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(if (isActive) 13.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> tokens.accent
                            isPast -> tokens.accent.copy(alpha = 0.55f)
                            else -> tokens.cardBorder
                        }
                    )
            )
        }
    }
}

@Composable
private fun CatAvatar(onClick: () -> Unit = {}) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(modifier = Modifier.size(64.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(tokens.cardFill)
                .border(1.dp, tokens.cardBorder, CircleShape)
                .padding(6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat_face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .clip(CircleShape)
                .background(tokens.accent)
                .border(2.dp, tokens.pageBackground, CircleShape)
        )
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
