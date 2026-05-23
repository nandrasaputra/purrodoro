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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.backgroundImageForTime
import com.endiar.kuropomodoro.model.cycleDotsState
import com.endiar.kuropomodoro.model.formatSeconds
import java.time.LocalTime

private val HomeCream = Color(0xFFFFF7EC)
private val HomeCard = Color(0xFFFFFCF6)
private val HomeCardWarm = Color(0xFFFFF1E2)
private val HomeBorder = Color(0xFFEFDCC8)
private val HomeText = Color(0xFF2B201C)
private val HomeMuted = Color(0xFF8B7770)
private val HomeCoral = Color(0xFFFF7D62)
private val HomeCoralSoft = Color(0xFFFFE3D9)
private val HomeLeaf = Color(0xFF8EAD7E)

@Composable
fun HomeTimerScreen(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    activeTask: FocusTask?,
    continueTask: FocusTask?,
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
    onStop: () -> Unit,
    onOpenSoundsSheet: () -> Unit,
    onEnterFocusMode: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeHeader(greeting = greeting)
            if (isStarted) {
                ActiveHero(
                    timerMode = timerMode,
                    remainingSeconds = remainingSeconds,
                    isRunning = isRunning,
                    activeTask = activeTask,
                    cycleItems = cycleItems,
                    cycleIndex = cycleIndex,
                    currentTimerState = currentTimerState,
                    timerSettings = timerSettings,
                    pomodoroProgress = pomodoroProgress,
                    backdropTime = backdropTime,
                    onSkip = onSkip,
                    onStartPause = onStartPause,
                    onStop = onStop,
                    onEnterFocusMode = onEnterFocusMode
                )
            } else {
                IdleHero(backdropTime = backdropTime, onStartPause = onStartPause)
            }
            ContinueCard(task = continueTask, progress = pomodoroProgress)
            TodayMetricsCard(metrics = todayMetrics)
            QuoteCard()
            SoundChip(isPlaying = isSoundPlaying, onClick = onOpenSoundsSheet)
        }
    }
}

@Composable
private fun HomeHeader(greeting: HomeGreeting) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting.salutation,
                color = HomeMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nandra",
                    color = HomeText,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif
                )
                Canvas(
                    modifier = Modifier
                        .padding(start = 8.dp, top = 5.dp)
                        .size(18.dp, 14.dp)
                ) {
                    drawOval(
                        color = HomeLeaf,
                        topLeft = Offset(size.width * 0.12f, size.height * 0.05f),
                        size = Size(size.width * 0.72f, size.height * 0.82f)
                    )
                    drawLine(
                        color = HomeText.copy(alpha = 0.35f),
                        start = Offset(size.width * 0.28f, size.height * 0.74f),
                        end = Offset(size.width * 0.90f, size.height * 0.08f),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            Text(
                text = greeting.encouragement,
                color = HomeMuted,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        CatAvatar()
    }
}

@Composable
private fun IdleHero(backdropTime: LocalTime, onStartPause: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.48f)
            .clip(RoundedCornerShape(32.dp))
    ) {
        Image(
            painter = painterResource(id = backgroundImageForTime(backdropTime)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        HomePrimaryButton(
            text = "Start Focus",
            onClick = onStartPause,
            icon = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
}

@Composable
private fun ActiveHero(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    pomodoroProgress: PomodoroProgress,
    backdropTime: LocalTime,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onEnterFocusMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Image(
            painter = painterResource(id = backgroundImageForTime(backdropTime)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.48f)
                .clip(RoundedCornerShape(32.dp))
        )
        ActiveTimerCard(
            timerMode = timerMode,
            remainingSeconds = remainingSeconds,
            isRunning = isRunning,
            activeTask = activeTask,
            cycleItems = cycleItems,
            cycleIndex = cycleIndex,
            currentTimerState = currentTimerState,
            timerSettings = timerSettings,
            pomodoroProgress = pomodoroProgress,
            onSkip = onSkip,
            onStartPause = onStartPause,
            onStop = onStop,
            onEnterFocusMode = onEnterFocusMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 72.dp)
                .padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun ActiveTimerCard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    pomodoroProgress: PomodoroProgress,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onEnterFocusMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, timerSettings)
    val currentType = cycleItems.getOrNull(cycleIndex)?.type
    val isBreak = currentType is SessionItemType.ShortBreak || currentType is SessionItemType.LongBreak
    val isFocus = currentType is SessionItemType.Focus
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    HomeCardSurface(modifier = modifier, color = HomeCard.copy(alpha = 0.94f)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CodeTile()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTask?.title ?: timerMode.label,
                        color = HomeText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Deep Work",
                        color = HomeMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Edit",
                    color = HomeCoral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatSeconds(remainingSeconds),
                        color = HomeText,
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "remaining",
                        color = HomeMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                PomodoroRing(progress = pomodoroProgress)
            }
            CycleDots(dots = dots)
            HomePrimaryButton(
                text = if (isRunning) "Pause Focus" else "Resume Focus",
                onClick = onStartPause,
                icon = !isRunning,
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
                    Text(
                        text = "■",
                        color = HomeMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Stop", color = HomeMuted, fontWeight = FontWeight.Bold)
                }
                if (isBreak) {
                    TextButton(onClick = onSkip) {
                        Text("Skip break", color = HomeMuted, fontWeight = FontWeight.Bold)
                    }
                }
                if (isRunning) {
                    TextButton(onClick = onEnterFocusMode) {
                        Text("Focus mode", color = HomeCoral, fontWeight = FontWeight.Bold)
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
                    color = HomeText,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "Your focus time so far will still be counted.",
                    color = HomeMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopConfirmDialog = false
                    onStop()
                }) {
                    Text("Stop for now", color = HomeCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmDialog = false }) {
                    Text("Stay focused", color = HomeMuted, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = HomeCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun ContinueCard(task: FocusTask?, progress: PomodoroProgress) {
    HomeCardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Continue where you left off",
                color = HomeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CodeTile()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task?.title ?: "Choose a focus task",
                        color = HomeText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (task != null) progress.completedLabel else "A calm place to begin when you are ready.",
                        color = HomeMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HomeMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayMetricsCard(metrics: TodayFocusMetrics) {
    HomeCardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Focus",
                    color = HomeText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "View all",
                    color = HomeCoral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(label = "Focus time", value = metrics.focusTimeLabel)
                MetricColumn(label = "Sessions", value = metrics.sessionsLabel)
                MetricColumn(label = "Completed", value = metrics.completedLabel)
            }
        }
    }
}

@Composable
private fun QuoteCard() {
    HomeCardSurface(color = HomeCardWarm) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Little by little,\na lot becomes a lot. ♡",
                color = HomeText,
                fontSize = 19.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            SleepingCat(modifier = Modifier.size(88.dp))
        }
    }
}

@Composable
private fun SoundChip(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.58f))
            .border(1.dp, HomeBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPlaying) "Sound on  ♫" else "Sound off  ♫",
            color = HomeMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = HomeText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = HomeMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeCardSurface(
    modifier: Modifier = Modifier,
    color: Color = HomeCard,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .border(1.dp, HomeBorder.copy(alpha = 0.74f), RoundedCornerShape(28.dp))
            .padding(18.dp)
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
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(HomeCoral)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun CodeTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HomeCoralSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "</>",
            color = HomeCoral,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PomodoroRing(progress: PomodoroProgress) {
    val total = progress.total.coerceAtLeast(1)
    val fraction = (progress.current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = HomeCoralSoft,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = HomeCoral,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${progress.current.coerceAtLeast(0)}",
                color = HomeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "/ $total",
                color = HomeMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CycleDots(dots: com.endiar.kuropomodoro.model.CycleDotsState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dots.totalFocusSessions.coerceAtLeast(1)) { index ->
            val completed = index < dots.completedFocusSessions
            val active = dots.isCurrentlyFocus && index == dots.completedFocusSessions
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (active) 24.dp else 9.dp, height = 9.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        when {
                            active -> HomeCoral
                            completed -> HomeLeaf
                            else -> HomeBorder
                        }
                    )
            )
        }
    }
}

@Composable
private fun CatAvatar() {
    Box(modifier = Modifier.size(64.dp)) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(HomeCard)
                .border(1.dp, HomeBorder, CircleShape)
        ) {
            val fur = Color(0xFF2D2421)
            val inner = Color(0xFFFFC7B5)
            drawCircle(color = Color(0xFFFFE4D4), radius = size.minDimension * 0.42f, center = center)
            drawCircle(color = fur, radius = size.minDimension * 0.24f, center = Offset(size.width * 0.50f, size.height * 0.52f))
            drawCircle(color = fur, radius = size.minDimension * 0.10f, center = Offset(size.width * 0.34f, size.height * 0.38f))
            drawCircle(color = fur, radius = size.minDimension * 0.10f, center = Offset(size.width * 0.66f, size.height * 0.38f))
            drawCircle(color = inner, radius = size.minDimension * 0.055f, center = Offset(size.width * 0.34f, size.height * 0.38f))
            drawCircle(color = inner, radius = size.minDimension * 0.055f, center = Offset(size.width * 0.66f, size.height * 0.38f))
            drawCircle(color = Color.White, radius = size.minDimension * 0.035f, center = Offset(size.width * 0.42f, size.height * 0.50f))
            drawCircle(color = Color.White, radius = size.minDimension * 0.035f, center = Offset(size.width * 0.58f, size.height * 0.50f))
            drawCircle(color = HomeText, radius = size.minDimension * 0.016f, center = Offset(size.width * 0.42f, size.height * 0.50f))
            drawCircle(color = HomeText, radius = size.minDimension * 0.016f, center = Offset(size.width * 0.58f, size.height * 0.50f))
            drawCircle(color = HomeCoral, radius = size.minDimension * 0.025f, center = Offset(size.width * 0.50f, size.height * 0.57f))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .clip(CircleShape)
                .background(HomeCoral)
                .border(2.dp, HomeCream, CircleShape)
        )
    }
}

@Composable
private fun SleepingCat(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val fur = Color(0xFF2D2421)
        val blush = Color(0xFFFFA891)
        drawOval(
            color = fur,
            topLeft = Offset(size.width * 0.14f, size.height * 0.38f),
            size = Size(size.width * 0.68f, size.height * 0.34f)
        )
        drawCircle(
            color = fur,
            radius = size.minDimension * 0.18f,
            center = Offset(size.width * 0.30f, size.height * 0.40f)
        )
        drawCircle(
            color = fur,
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.18f, size.height * 0.26f)
        )
        drawCircle(
            color = fur,
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.38f, size.height * 0.26f)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.86f),
            startAngle = 10f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(size.width * 0.19f, size.height * 0.37f),
            size = Size(size.width * 0.10f, size.height * 0.08f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.86f),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.34f, size.height * 0.37f),
            size = Size(size.width * 0.10f, size.height * 0.08f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(color = blush, radius = size.minDimension * 0.035f, center = Offset(size.width * 0.26f, size.height * 0.49f))
        drawCircle(color = blush, radius = size.minDimension * 0.035f, center = Offset(size.width * 0.42f, size.height * 0.49f))
        drawArc(
            color = fur,
            startAngle = 165f,
            sweepAngle = 210f,
            useCenter = false,
            topLeft = Offset(size.width * 0.64f, size.height * 0.32f),
            size = Size(size.width * 0.24f, size.height * 0.34f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )
        drawLine(
            color = HomeBorder,
            start = Offset(size.width * 0.05f, size.height * 0.75f),
            end = Offset(size.width * 0.95f, size.height * 0.75f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
