package com.endiar.purrodoro.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Pause
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerMode
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.formatSeconds
import com.endiar.purrodoro.ui.SoundscapeCompactBar
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.designsystem.pressScale
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens

/**
 * Full-screen "Minimal Zen" focus surface that takes over the home dashboard
 * while [focusActive] is true.
 *
 * Visual language matches `assets/kuro_minimal_zen_focus_states.png`:
 *  - Opaque cream page (not a dark scrim) so the room feels still, not dimmed.
 *  - Top mode label morphs across `FOCUS / SHORT BREAK / LONG BREAK`.
 *  - Centered serif timer in cocoa with a state-aware subtext.
 *  - Intent pill (Task / Habit / Note + title) shown only on focus segments
 *    when a non-Empty intent exists; hidden during breaks.
 *  - Coral progress dots with current emphasized + `n / m` text.
 *  - Mascot anchored in the lower-middle.
 *  - State-aware bottom CTA:
 *      * running focus  → "tap anywhere to exit" hint
 *      * break ready    → coral "Start break" pill + "Skip break" text
 *      * break running  → "Skip break" text + small coral pause circle
 *
 * Tap-anywhere-to-exit applies on all states; explicit controls consume the
 * tap so they don't accidentally collapse the overlay.
 */
@Composable
fun FocusModeOverlay(
    focusActive: Boolean,
    focusTransition: Transition<Boolean>,
    timerMode: TimerMode,
    remainingSeconds: Int,
    activeFocusType: FocusType,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    isRunning: Boolean,
    onExit: () -> Unit,
    onStartPause: () -> Unit,
    onSkip: () -> Unit,
    playingAmbienceName: String? = null,
    playingMusicName: String? = null,
    onOpenSoundscape: () -> Unit = {},
    onMuteSoundscape: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val context = LocalContext.current

    val reduceMotion = remember {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        scale == 0f
    }

    val surfaceAlpha by focusTransition.animateFloat(
        transitionSpec = FocusModeMotion.spec(),
        label = "focusSurfaceAlpha"
    ) { active -> if (active) 1f else 0f }

    if (surfaceAlpha <= 0f && !focusActive) return

    // Spatial arrival: the content cluster doesn't just fade — it drifts up the
    // last few dp and settles from a hair under full size, so entry reads as
    // "rising into place" rather than a flat cross-fade. Driven off the shared
    // transition clock (emphasized-decelerate on enter), so it lands in lockstep
    // with the scrim and dock.
    val contentScale = 0.985f + 0.015f * surfaceAlpha
    val contentTranslateY = (1f - surfaceAlpha) * 20f

    // One-shot "breath" pulse on the timer once focus is fully entered — the
    // ambient layer that signals "you're in". Gated on `hasEntered` so it never
    // fires on process restore (where focusActive is already true at first
    // composition) and respects the system reduced-motion setting.
    val breathScale = remember { Animatable(1f) }
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(focusActive) {
        if (focusActive && hasEntered && !reduceMotion) {
            breathScale.snapTo(1f)
            breathScale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = FocusModeMotion.BreathDurationMs
                    delayMillis = FocusModeMotion.BreathDelayMs.toInt()
                    1f at 0
                    FocusModeMotion.BreathScalePeak at FocusModeMotion.BreathDurationMs / 2
                    1f at FocusModeMotion.BreathDurationMs
                },
            )
        }
        hasEntered = focusActive
    }

    val isBreak = timerMode == TimerMode.ShortBreak || timerMode == TimerMode.LongBreak
    val isPaused = currentTimerState == TimerState.Paused
    val breakReady = isBreak && !isRunning
    val breakRunning = isBreak && isRunning
    val focusRunning = !isBreak && isRunning
    val focusPaused = !isBreak && isPaused

    val modeLabel = when (timerMode) {
        TimerMode.Focus -> stringResource(R.string.focus_mode_label_focus)
        TimerMode.ShortBreak -> stringResource(R.string.focus_mode_label_short_break)
        TimerMode.LongBreak -> stringResource(R.string.focus_mode_label_long_break)
    }

    val subtext: String? = when {
        breakReady -> stringResource(R.string.focus_subtext_break_ready)
        breakRunning -> stringResource(R.string.focus_subtext_break_running)
        focusPaused -> stringResource(R.string.focus_subtext_paused)
        else -> null
    }

    val showIntent = !isBreak &&
        activeFocusType != FocusType.Empty &&
        activeTask != null

    val exitLabel = stringResource(R.string.focus_exit_cd)

    // State-based mascot. Focus-started has 3 variants — re-rolled each time
    // the user enters a new focus segment so they don't see the same pose
    // every Pomodoro, while staying stable within a single segment so it
    // doesn't flicker on recomposition (e.g. every tick).
    val mascotRes = rememberMascotResource(
        timerMode = timerMode,
        isRunning = isRunning,
        cycleIndex = cycleIndex,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.pageBackground.copy(alpha = surfaceAlpha))
            .then(
                if (focusActive) {
                    Modifier
                        .pointerInput(focusActive) {
                            detectTapGestures(onTap = { onExit() })
                        }
                        .semantics(mergeDescendants = false) {
                            onClick(label = exitLabel) {
                                onExit()
                                true
                            }
                        }
                } else Modifier
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = contentTranslateY
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .padding(contentPadding)
                .padding(horizontal = KuroSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))

            Text(
                text = modeLabel,
                color = tokens.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = formatSeconds(remainingSeconds),
                color = tokens.textPrimary,
                fontSize = 84.sp,
                lineHeight = 88.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = NunitoFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = breathScale.value
                        scaleY = breathScale.value
                    },
            )

            if (subtext != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtext,
                    color = tokens.textSecondary,
                    style = KuroType.bodySecondary,
                    textAlign = TextAlign.Center,
                )
            }

            if (showIntent) {
                Spacer(Modifier.height(18.dp))
                IntentPill(focusType = activeFocusType, task = activeTask!!)
            }

            Spacer(Modifier.height(if (showIntent) 18.dp else 28.dp))

            FocusCycleDots(
                cycleItems = cycleItems,
                cycleIndex = cycleIndex,
                currentTimerState = currentTimerState,
            )
            val (completed, total) = focusCounts(cycleItems, cycleIndex, currentTimerState)
            if (total > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$completed / $total",
                    color = tokens.textSecondary,
                    style = KuroType.labelStrong,
                )
            }

            // Keep Purrodoro as the emotional anchor in the middle-lower area.
            // The previous 1f/0.4f split pushed the mascot too close to the
            // bottom, making it feel like footer decoration. This balance puts
            // the mascot around ~60% of the screen while leaving the controls
            // and hint anchored near the navigation bar.
            Spacer(modifier = Modifier.weight(0.55f))

            Image(
                painter = painterResource(id = mascotRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.4f),
            )

            Spacer(modifier = Modifier.weight(0.85f))

            SoundscapeCompactBar(
                playingAmbienceName = playingAmbienceName,
                playingMusicName = playingMusicName,
                onOpenSoundscape = onOpenSoundscape,
                onMuteAll = onMuteSoundscape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = KuroSpacing.sm),
            )

            BottomActions(
                breakReady = breakReady,
                breakRunning = breakRunning,
                focusRunning = focusRunning,
                focusPaused = focusPaused,
                onStartPause = onStartPause,
                onSkip = onSkip,
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun IntentPill(focusType: FocusType, task: FocusTask) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val (icon, label, title) = when (focusType) {
        FocusType.Task -> Triple(Lucide.CircleCheck, stringResource(R.string.focus_type_task), task.title)
        FocusType.Habit -> {
            val parts = task.title.split(" ", limit = 2)
            val name = if (parts.size == 2) parts[1] else task.title
            Triple(Lucide.Leaf, stringResource(R.string.focus_type_habit), name)
        }
        FocusType.SimpleNote -> Triple(Lucide.NotebookPen, stringResource(R.string.focus_type_note), task.title)
        FocusType.Empty -> return
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.cardFill)
            .border(
                1.dp,
                tokens.cardBorder,
                RoundedCornerShape(KuroSpacing.radiusPill),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = tokens.textPrimary,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = "  ·  ",
            color = tokens.textSecondary,
            style = KuroType.bodyPrimary,
        )
        Text(
            text = title,
            color = tokens.textPrimary,
            style = KuroType.bodyPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
    }
}

@Composable
private fun FocusCycleDots(
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val focusIndices = cycleItems.mapIndexedNotNull { index, item ->
        if (item.type is SessionItemType.Focus) index else null
    }
    if (focusIndices.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        focusIndices.forEach { originalIndex ->
            val isActive =
                originalIndex == cycleIndex && currentTimerState != TimerState.Idle
            val isPast = originalIndex < cycleIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(if (isActive) 11.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> tokens.accent
                            isPast -> tokens.accent.copy(alpha = 0.55f)
                            else -> tokens.cardBorder
                        }
                    ),
            )
        }
    }
}

private fun focusCounts(
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
): Pair<Int, Int> {
    val focusIndices = cycleItems.mapIndexedNotNull { index, item ->
        if (item.type is SessionItemType.Focus) index else null
    }
    val total = focusIndices.size
    val pastCount = focusIndices.count { it < cycleIndex }
    val onCurrentFocus = focusIndices.contains(cycleIndex) &&
        currentTimerState != TimerState.Idle
    val current = if (onCurrentFocus) pastCount + 1 else pastCount
    val displayed = current.coerceAtLeast(1).coerceAtMost(total.coerceAtLeast(1))
    return displayed to total
}

@Composable
private fun BottomActions(
    breakReady: Boolean,
    breakRunning: Boolean,
    focusRunning: Boolean,
    focusPaused: Boolean,
    onStartPause: () -> Unit,
    onSkip: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    when {
        breakReady -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CoralPillButton(
                text = stringResource(R.string.focus_start_break),
                onClick = onStartPause,
                modifier = Modifier.fillMaxWidth(),
            )
            QuietTextAction(text = stringResource(R.string.home_skip_break), onClick = onSkip)
        }

        breakRunning -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))
            QuietTextAction(text = stringResource(R.string.home_skip_break), onClick = onSkip)
            CoralPauseCircle(onClick = onStartPause)
        }

        focusPaused -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CoralPillButton(
                text = stringResource(R.string.focus_resume_focus),
                onClick = onStartPause,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.focus_tap_to_exit),
                color = tokens.textSecondary.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
            )
        }

        focusRunning -> Text(
            text = stringResource(R.string.focus_tap_to_exit),
            color = tokens.textSecondary.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
        )

        else -> Text(
            text = stringResource(R.string.focus_tap_to_exit),
            color = tokens.textSecondary.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CoralPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.97f)
            .height(54.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.accent)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = KuroSpacing.xl)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

@Composable
private fun QuietTextAction(text: String, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = text },
    ) {
        Text(
            text = text,
            color = tokens.textSecondary,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun CoralPauseCircle(onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    val pauseBreakLabel = stringResource(R.string.focus_pause_break_cd)
    Box(
        modifier = Modifier
            .pressScale(interactionSource, pressedScale = 0.92f)
            .size(54.dp)
            .clip(CircleShape)
            .background(tokens.accent)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            )
            .semantics { contentDescription = pauseBreakLabel },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.Pause,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Picks the right mascot illustration for the current focus-screen state.
 *
 * The focus-started case has two variants. We pick one randomly but key
 * the `remember` on `(timerMode, isRunning, cycleIndex)` so the same variant
 * is reused across recompositions inside a single focus segment (e.g. every
 * 1-second tick) and only re-rolls when the user advances to a fresh focus
 * segment in the cycle. This gives variety across Pomodoros without making
 * the image flicker mid-session.
 *
 * Assets here are intent-agnostic by design: focused cat applies equally to
 * Task, Habit, Note, and Empty. `cat_ready` covers the waiting state between
 * segments (focus done waiting for break, or break done waiting for focus).
 */
@Composable
private fun rememberMascotResource(
    timerMode: TimerMode,
    isRunning: Boolean,
    cycleIndex: Int,
): Int {
    val focusStartedVariants = remember {
        intArrayOf(
            R.drawable.cat_focusing_1,
            R.drawable.cat_focusing_2,
        )
    }
    return remember(timerMode, isRunning, cycleIndex) {
        when (timerMode) {
            TimerMode.Focus -> {
                if (isRunning) focusStartedVariants.random()
                else R.drawable.cat_ready
            }
            TimerMode.ShortBreak -> {
                if (isRunning) R.drawable.cat_short_break
                else R.drawable.cat_ready
            }
            TimerMode.LongBreak -> {
                if (isRunning) R.drawable.cat_long_break
                else R.drawable.cat_ready
            }
        }
    }
}
