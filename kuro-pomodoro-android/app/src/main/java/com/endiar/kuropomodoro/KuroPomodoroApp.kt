package com.endiar.kuropomodoro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.backgroundImageForTime
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.model.addFocusStamp
import com.endiar.kuropomodoro.model.formatSeconds
import com.endiar.kuropomodoro.ui.CalendarScreen
import com.endiar.kuropomodoro.ui.ProfileScreen
import com.endiar.kuropomodoro.ui.SoundsScreen
import com.endiar.kuropomodoro.ui.TodoScreen
import com.endiar.kuropomodoro.ui.TimerScreen
import com.endiar.kuropomodoro.ui.components.BottomTabBar
import com.endiar.kuropomodoro.ui.components.DashboardDock
import com.endiar.kuropomodoro.ui.components.DashboardModeChip
import com.endiar.kuropomodoro.ui.components.DecorativeBackground
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay

@Composable
fun KuroPomodoroApp() {
    var selectedTab by rememberSaveable(stateSaver = AppTabSaver) { mutableStateOf(AppTab.Timer) }
    var timerMode by rememberSaveable(stateSaver = TimerModeSaver) { mutableStateOf(TimerMode.Focus) }
    var settings by rememberSaveable(stateSaver = TimerSettingsSaver) { mutableStateOf(TimerSettings()) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(settings.secondsFor(timerMode)) }
    var activeTotalSeconds by rememberSaveable { mutableIntStateOf(settings.secondsFor(timerMode)) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var completedSessions by rememberSaveable { mutableIntStateOf(0) }
    var stamps by rememberSaveable(stateSaver = StampsSaver) { mutableStateOf<Map<LocalDate, Int>>(emptyMap()) }
    var selectedSoundId by rememberSaveable { mutableIntStateOf(1) }
    var isSoundPlaying by rememberSaveable { mutableStateOf(false) }
    var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var darkModePreview by rememberSaveable { mutableStateOf(false) }
    var nextTaskId by rememberSaveable { mutableIntStateOf(7) }
    var selectedTodoListId by rememberSaveable { mutableIntStateOf(1) }
    var todoLists by rememberSaveable(stateSaver = TodoListsSaver) {
        mutableStateOf(
            listOf(
                TodoListState(1, "Study", listOf(FocusTask(1, "Review Compose layout", 25), FocusTask(2, "Read chapter notes", 20))),
                TodoListState(2, "Home", listOf(FocusTask(3, "Clean desk", 10), FocusTask(4, "Water plants", 5))),
                TodoListState(3, "Ideas", listOf(FocusTask(5, "Sketch cat animation", 15), FocusTask(6, "Plan next sprint", 25)))
            )
        )
    }

    val addTask: (String) -> Unit = { title ->
        todoLists = todoLists.map { list ->
            if (list.id == selectedTodoListId) {
                list.copy(tasks = list.tasks + FocusTask(nextTaskId, title, 25))
            } else {
                list
            }
        }
        nextTaskId += 1
    }
    val toggleTask: (Int) -> Unit = { taskId ->
        todoLists = todoLists.map { list ->
            list.copy(tasks = list.tasks.map { task -> if (task.id == taskId) task.toggleCompleted() else task })
        }
    }
    val deleteTask: (Int) -> Unit = { taskId ->
        todoLists = todoLists.map { list -> list.copy(tasks = list.tasks.filterNot { it.id == taskId }) }
    }
    var timerStartedAtMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var timerStartedRemainingSeconds by rememberSaveable { mutableIntStateOf(settings.secondsFor(timerMode)) }
    val sounds = remember {
        listOf(
            SoundOption(1, "Rainy Window", "Soft rain for deep reading", false, "Rain"),
            SoundOption(2, "Cafe Purr", "Warm cafe room ambience", false, "Cafe"),
            SoundOption(3, "Forest Nap", "Leaves, birds, and slow wind", true, "Forest"),
            SoundOption(4, "Fireplace", "Crackling fire with sleepy cat", true, "Fire"),
            SoundOption(5, "Night Keyboard", "Gentle keys in a quiet room", false, "Keys")
        )
    }

    LaunchedEffect(isRunning, timerMode, settings) {
        if (!isRunning) {
            timerStartedAtMillis = null
            timerStartedRemainingSeconds = remainingSeconds
            return@LaunchedEffect
        }

        if (timerStartedAtMillis == null) {
            timerStartedAtMillis = System.currentTimeMillis()
            timerStartedRemainingSeconds = remainingSeconds
        }

        while (isRunning) {
            val startedAtMillis = timerStartedAtMillis ?: System.currentTimeMillis().also {
                timerStartedAtMillis = it
            }
            val elapsedSeconds = ((System.currentTimeMillis() - startedAtMillis) / 1000).toInt()
            val updatedRemainingSeconds = (timerStartedRemainingSeconds - elapsedSeconds).coerceAtLeast(0)
            remainingSeconds = updatedRemainingSeconds

            if (updatedRemainingSeconds == 0) {
                isRunning = false
                val nextMode = if (timerMode == TimerMode.Focus) {
                    completedSessions += 1
                    stamps = addFocusStamp(stamps, LocalDate.now())
                    TimerMode.ShortBreak
                } else {
                    timerMode
                }
                timerMode = nextMode
                remainingSeconds = settings.secondsFor(nextMode)
                timerStartedAtMillis = null
                timerStartedRemainingSeconds = remainingSeconds
                break
            }

            delay(250)
        }
    }

    if (selectedTab == AppTab.Timer) {
        FocusDashboard(
            timerMode = timerMode,
            remainingSeconds = remainingSeconds,
            isRunning = isRunning,
            onModeSelected = { mode ->
                timerMode = mode
                isRunning = false
                remainingSeconds = settings.secondsFor(mode)
                activeTotalSeconds = settings.secondsFor(mode)
            },
            onStartPause = {
                if (!isRunning) {
                    activeTotalSeconds = timerStartTotalSeconds(
                        settings = settings,
                        mode = timerMode,
                        remainingSeconds = remainingSeconds,
                        activeTotalSeconds = activeTotalSeconds
                    )
                }
                isRunning = !isRunning
            },
            onNavigate = { selectedTab = it }
        )
    } else {
        DecorativeBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when (selectedTab) {
                        AppTab.Timer -> TimerScreen(
                            mode = timerMode,
                            remainingSeconds = remainingSeconds,
                            totalSeconds = timerDisplayTotalSeconds(
                                settings = settings,
                                mode = timerMode,
                                isRunning = isRunning,
                                activeTotalSeconds = activeTotalSeconds
                            ),
                            isRunning = isRunning,
                            completedSessionsToday = stamps[LocalDate.now()] ?: 0,
                            currentTask = todoLists.firstOrNull { it.id == selectedTodoListId }?.tasks?.firstOrNull { !it.completed },
                            onModeSelected = { selectedMode ->
                                timerMode = selectedMode
                                isRunning = false
                                remainingSeconds = settings.secondsFor(selectedMode)
                                activeTotalSeconds = settings.secondsFor(selectedMode)
                            },
                            onStartPause = {
                                if (!isRunning) {
                                    activeTotalSeconds = timerStartTotalSeconds(
                                        settings = settings,
                                        mode = timerMode,
                                        remainingSeconds = remainingSeconds,
                                        activeTotalSeconds = activeTotalSeconds
                                    )
                                }
                                isRunning = !isRunning
                            },
                            onReset = {
                                isRunning = false
                                remainingSeconds = settings.secondsFor(timerMode)
                                activeTotalSeconds = settings.secondsFor(timerMode)
                            }
                        )
                        AppTab.Todo -> TodoScreen(
                            todoLists = todoLists,
                            selectedListId = selectedTodoListId,
                            onListSelected = { selectedTodoListId = it },
                            onAddTask = addTask,
                            onToggleTask = toggleTask,
                            onDeleteTask = deleteTask
                        )
                        AppTab.Calendar -> CalendarScreen(
                            stamps = stamps,
                            completedSessions = completedSessions
                        )
                        AppTab.Sounds -> SoundsScreen(
                            sounds = sounds,
                            selectedSoundId = selectedSoundId,
                            isPlaying = isSoundPlaying,
                            onSoundSelected = { sound ->
                                if (sound.locked) {
                                    premiumSheetMessage = "${sound.name} is part of the simulated premium sound pack."
                                } else {
                                    selectedSoundId = sound.id
                                    isSoundPlaying = true
                                    premiumSheetMessage = null
                                }
                            }
                        )
                        AppTab.Profile -> ProfileScreen(
                            settings = settings,
                            darkModePreview = darkModePreview,
                            onDarkModeChanged = { darkModePreview = it },
                            onDurationChanged = { mode, minutes ->
                                val updatedSettings = settings.update(mode, minutes)
                                settings = updatedSettings
                                if (mode == timerMode && !isRunning) {
                                    remainingSeconds = updatedSettings.secondsFor(mode)
                                    activeTotalSeconds = updatedSettings.secondsFor(mode)
                                }
                            },
                            onMockFeature = { premiumSheetMessage = it }
                        )
                    }
                }
                premiumSheetMessage?.let { message ->
                    PremiumMessage(
                        message = message,
                        onDismiss = { premiumSheetMessage = null },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
                BottomTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }
}

internal fun timerDisplayTotalSeconds(
    settings: TimerSettings,
    mode: TimerMode,
    isRunning: Boolean,
    activeTotalSeconds: Int
): Int = if (isRunning) activeTotalSeconds else settings.secondsFor(mode)

internal fun timerStartTotalSeconds(
    settings: TimerSettings,
    mode: TimerMode,
    remainingSeconds: Int,
    activeTotalSeconds: Int
): Int {
    val currentModeSeconds = settings.secondsFor(mode)
    return if (remainingSeconds < activeTotalSeconds) activeTotalSeconds else currentModeSeconds
}

@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    onModeSelected: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
    onNavigate: (AppTab) -> Unit,
) {
    val buttonBg = if (isRunning) Color.White.copy(alpha = 0.92f) else Color(0xFFFF8B6F)
    val buttonContentColor = if (isRunning) Color(0xFF241E2B) else Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundImageForTime(LocalTime.now())),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Top scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Brush.verticalGradient(listOf(Color(0xCC0F0A1A), Color.Transparent)))
        )
        // Bottom scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0F0A1A))))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            // Mode chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimerMode.entries.forEach { mode ->
                    DashboardModeChip(
                        mode = mode,
                        selected = mode == timerMode,
                        onClick = { onModeSelected(mode) }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            // Timer
            Text(
                text = formatSeconds(remainingSeconds),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 72.sp,
                fontWeight = FontWeight.W200,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(20.dp))
            // Start / Pause button
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(buttonBg)
                    .clickable(onClick = onStartPause),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRunning) "\u23f8" else "\u25b6",
                        color = buttonContentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRunning) "Pause" else "Start Focus",
                        color = buttonContentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Cat window — flexible space between button and dock
            Spacer(Modifier.weight(1f))
            DashboardDock(
                isRunning = isRunning,
                onNavigate = onNavigate,
                onStartPause = onStartPause,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun PremiumMessage(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Dismiss", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private val AppTabSaver = Saver<AppTab, String>(
    save = { it.name },
    restore = { AppTab.valueOf(it) }
)

private val TimerModeSaver = Saver<TimerMode, String>(
    save = { it.name },
    restore = { TimerMode.valueOf(it) }
)

private val TimerSettingsSaver = listSaver<TimerSettings, Int>(
    save = { listOf(it.focusMinutes, it.shortBreakMinutes, it.longBreakMinutes) },
    restore = { TimerSettings(focusMinutes = it[0], shortBreakMinutes = it[1], longBreakMinutes = it[2]) }
)

private val StampsSaver = listSaver<Map<LocalDate, Int>, Any>(
    save = { stamps -> stamps.entries.flatMap { listOf(it.key.toString(), it.value) } },
    restore = { saved ->
        saved.chunked(2).associate { (date, count) ->
            LocalDate.parse(date as String) to count as Int
        }
    }
)

private val TodoListsSaver = listSaver<List<TodoListState>, Any>(
    save = { lists ->
        lists.flatMap { list ->
            listOf(list.id, list.name, list.tasks.size) + list.tasks.flatMap { task ->
                listOf(task.id, task.title, task.minutes, task.completed)
            }
        }
    },
    restore = { saved ->
        buildList {
            var index = 0
            while (index < saved.size) {
                val listId = saved[index++] as Int
                val name = saved[index++] as String
                val taskCount = saved[index++] as Int
                val tasks = buildList {
                    repeat(taskCount) {
                        add(
                            FocusTask(
                                id = saved[index++] as Int,
                                title = saved[index++] as String,
                                minutes = saved[index++] as Int,
                                completed = saved[index++] as Boolean
                            )
                        )
                    }
                }
                add(TodoListState(listId, name, tasks))
            }
        }
    }
)
