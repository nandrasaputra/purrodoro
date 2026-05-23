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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.cycleDotsState
import com.endiar.kuropomodoro.model.formatSeconds
import java.time.LocalTime

// Color palette matched to reference image
private val HomeCream = Color(0xFFFAF5F2)      // exact extracted main background
private val HomeCard = Color(0xFFFFFFFF)        // white card lifts off the beige background
private val HomeCardWarm = Color(0xFFF9EEE5)   // exact extracted bottom card color
private val HomeBorder = Color(0xFFE9DED8)     // subtle warm border tuned for FAF5F2
private val HomeText = Color(0xFF1E1612)        // near-black warm brown
private val HomeMuted = Color(0xFF9E8A7E)       // muted warm grey-brown
private val HomeCoral = Color(0xFFBF5B3D)       // reference coral/terracotta (darker, less orange)
private val HomeCoralSoft = Color(0xFFF2DDD5)   // soft coral tint for tiles
private val HomeLeaf = Color(0xFF7A9A60)        // leaf green

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    onStop = onStop
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nandra",
                    color = HomeText,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold,
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
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        CatAvatar()
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
            .clip(RoundedCornerShape(20.dp))
    )
}

@Composable
private fun IdleHero(backdropTime: LocalTime, onStartPause: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        HeroImage(modifier = Modifier.fillMaxWidth(), time = backdropTime)
        HomePrimaryButton(
            text = "Start Focus",
            onClick = onStartPause,
            icon = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
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
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    pomodoroProgress: PomodoroProgress,
    backdropTime: LocalTime,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HeroImage(modifier = Modifier.fillMaxWidth(), time = backdropTime)
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
            modifier = Modifier
                .offset(y = (-72).dp)
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
    modifier: Modifier = Modifier
) {
    val dots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, timerSettings)
    val currentType = cycleItems.getOrNull(cycleIndex)?.type
    val isBreak = currentType is SessionItemType.ShortBreak || currentType is SessionItemType.LongBreak
    val isFocus = currentType is SessionItemType.Focus
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    HomeCardSurface(modifier = modifier, color = HomeCard) {
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
                    text = "✎ Edit",
                    color = HomeCoral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
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
                        fontSize = 44.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.Light
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Continue where you left off",
                    color = HomeText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
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
                        color = HomeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
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
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HomeMuted,
                    modifier = Modifier.size(20.dp)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "View all",
                    color = HomeCoral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
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
    HomeCardSurface(color = HomeCardWarm) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Little by little,\na lot becomes a lot. ♡",
                color = HomeText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
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
            .heightIn(min = 48.dp)
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
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = HomeMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = HomeText,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
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
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .border(1.dp, HomeBorder.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
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
        Canvas(modifier = Modifier.size(64.dp)) {
            val stroke = 7.dp.toPx()
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
                text = "Pomodoro",
                color = HomeMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${progress.current.coerceAtLeast(1)} of $total",
                color = HomeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
                    .padding(horizontal = 5.dp)
                    .size(if (active) 13.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            active -> HomeCoral
                            completed -> HomeCoral.copy(alpha = 0.55f)
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
    Image(
        painter = painterResource(id = R.drawable.cat_home_sleeping),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
