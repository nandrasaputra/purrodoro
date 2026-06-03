package com.endiar.purrodoro.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Square
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.endiar.purrodoro.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.delay
import kotlin.math.hypot

/**
 * The "Cycle complete" celebration dialog.
 *
 * Motion spec (see also docs in the brainstorming session):
 *  - Card: ~240ms spring fade + scale-from-0.96 + translateY 48dp → 0 (60ms delay)
 *  - Done badge: ~320ms spring scale 0.7 → 1 (180ms delay)
 *  - Check stroke: 220ms ease-out path draw (320ms delay)
 *  - Haptic at ~540ms (after check finishes)
 *  - All animations snap when system animation scale = 0 (reduced motion)
 */
@Composable
fun CycleCompleteDialog(
    title: String,
    body: String,
    focusMinutesTotal: Int,
    focusSessionsCompleted: Int,
    focusType: FocusType,
    intentEmoji: String?,
    intentLabel: String?,
    confirmLabel: String = stringResource(R.string.cycle_done),
    secondaryLabel: String? = stringResource(R.string.cycle_view_journey),
    onConfirm: () -> Unit,
    onSecondary: () -> Unit = {},
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val reduceMotion = remember {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
        scale == 0f
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val cardAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 240),
        label = "cycleCompleteCardAlpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.96f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cycleCompleteCardScale"
    )
    val cardTranslateY by animateFloatAsState(
        targetValue = if (entered) 0f else 48f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cycleCompleteCardTY"
    )

    var badgeEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(if (reduceMotion) 0L else 180L)
        badgeEntered = true
    }
    val badgeScale by animateFloatAsState(
        targetValue = if (badgeEntered) 1f else 0.7f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "doneBadgeScale"
    )

    val checkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(if (reduceMotion) 0L else 320L)
        checkProgress.animateTo(
            1f,
            animationSpec = if (reduceMotion) snap() else tween(
                durationMillis = 220,
                easing = LinearOutSlowInEasing
            )
        )
        if (!reduceMotion) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Dialog(
        onDismissRequest = { /* non-cancelable */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f * cardAlpha))
                .padding(horizontal = KuroSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                        translationY = cardTranslateY
                    },
                shape = RoundedCornerShape(KuroSpacing.radiusLg),
                colors = CardDefaults.cardColors(containerColor = tokens.cardFill),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = tokens.cardBorder,
                            shape = RoundedCornerShape(KuroSpacing.radiusLg),
                        )
                        .padding(
                            PaddingValues(
                                start = KuroSpacing.xl,
                                end = KuroSpacing.xl,
                                top = KuroSpacing.xl,
                                bottom = KuroSpacing.lg,
                            )
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DoneBadge(
                        scale = badgeScale,
                        checkProgress = checkProgress.value,
                    )

                    Spacer(Modifier.height(KuroSpacing.md))

                    Text(
                        text = title,
                        color = tokens.textPrimary,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(KuroSpacing.xs))

                    Text(
                        text = body,
                        color = tokens.textSecondary,
                        style = KuroType.bodySecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(KuroSpacing.lg))

                    FocusSummary(
                        focusMinutesTotal = focusMinutesTotal,
                        focusSessionsCompleted = focusSessionsCompleted,
                    )

                    if (intentLabel != null) {
                        Spacer(Modifier.height(KuroSpacing.md))
                        IntentReceiptRow(
                            focusType = focusType,
                            intentEmoji = intentEmoji,
                            intentLabel = intentLabel,
                        )
                    }

                    Spacer(Modifier.height(KuroSpacing.lg))

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(KuroSpacing.radiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.accentOn,
                        ),
                    ) {
                        Text(
                            text = confirmLabel,
                            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }

                    if (secondaryLabel != null) {
                        Spacer(Modifier.height(KuroSpacing.xs))
                        TextButton(onClick = onSecondary) {
                            Text(
                                text = secondaryLabel,
                                color = tokens.accent,
                                style = KuroType.labelStrong,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneBadge(scale: Float, checkProgress: Float) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(tokens.accentSoft),
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(tokens.accent),
        )
        AnimatedCheck(
            progress = checkProgress,
            color = tokens.accentOn,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun AnimatedCheck(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p0 = Offset(0.22f * w, 0.52f * h)
        val p1 = Offset(0.44f * w, 0.72f * h)
        val p2 = Offset(0.78f * w, 0.32f * h)
        val len1 = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
        val len2 = hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat()
        val totalLen = len1 + len2
        val drawn = progress.coerceIn(0f, 1f) * totalLen
        val stroke = 0.14f * minOf(w, h)
        if (drawn <= len1) {
            val t = if (len1 == 0f) 0f else drawn / len1
            val end = Offset(
                x = p0.x + (p1.x - p0.x) * t,
                y = p0.y + (p1.y - p0.y) * t,
            )
            drawLine(color, p0, end, strokeWidth = stroke, cap = StrokeCap.Round)
        } else {
            drawLine(color, p0, p1, strokeWidth = stroke, cap = StrokeCap.Round)
            val t2 = if (len2 == 0f) 0f else (drawn - len1) / len2
            val end2 = Offset(
                x = p1.x + (p2.x - p1.x) * t2,
                y = p1.y + (p2.y - p1.y) * t2,
            )
            drawLine(color, p1, end2, strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun FocusSummary(
    focusMinutesTotal: Int,
    focusSessionsCompleted: Int,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = focusMinutesTotal.toString(),
                color = tokens.textPrimary,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 56.sp,
                lineHeight = 58.sp,
            )
            Text(
                text = stringResource(R.string.cycle_minutes_unit),
                color = tokens.textPrimary,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
            )
        }
        Text(
            text = stringResource(R.string.cycle_focused),
            color = tokens.textSecondary,
            style = KuroType.labelStrong,
        )
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            text = pluralStringResource(
                R.plurals.cycle_focus_sessions_completed,
                focusSessionsCompleted,
                focusSessionsCompleted,
            ),
            color = tokens.textMuted,
            style = KuroType.bodySecondary,
        )
    }
}

@Composable
private fun IntentReceiptRow(
    focusType: FocusType,
    intentEmoji: String?,
    intentLabel: String,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val eyebrow = when (focusType) {
        FocusType.Task -> stringResource(R.string.focus_type_task)
        FocusType.Habit -> stringResource(R.string.focus_type_habit)
        FocusType.SimpleNote -> stringResource(R.string.focus_type_note)
        FocusType.Empty -> stringResource(R.string.focus_type_focus)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.activeCardFill)
            .border(
                width = 1.dp,
                color = tokens.cardBorder,
                shape = RoundedCornerShape(KuroSpacing.radiusMd),
            )
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IntentLeading(focusType = focusType, emojiOverride = intentEmoji)
        Spacer(Modifier.width(KuroSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                color = tokens.textMuted,
                style = KuroType.labelCaption,
            )
            Text(
                text = intentLabel,
                color = tokens.textPrimary,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IntentDoneStamp()
    }
}

@Composable
private fun IntentLeading(focusType: FocusType, emojiOverride: String?) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (emojiOverride != null) {
            Text(text = emojiOverride, fontSize = 18.sp)
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
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun IntentDoneStamp() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(tokens.accent),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedCheck(
            progress = 1f,
            color = tokens.accentOn,
            modifier = Modifier.size(14.dp),
        )
    }
}
