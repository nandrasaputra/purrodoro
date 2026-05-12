package com.endiar.kuropomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.data.AppState
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassReadabilityMode
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.model.addFocusStamp
import com.endiar.kuropomodoro.model.formatSeconds
import com.endiar.kuropomodoro.model.glassTreatmentFor
import com.endiar.kuropomodoro.ui.CalendarScreen
import com.endiar.kuropomodoro.ui.ProfileScreen
import com.endiar.kuropomodoro.ui.SoundsScreen
import com.endiar.kuropomodoro.ui.TodoScreen
import com.endiar.kuropomodoro.ui.components.CatBackdrop
import com.endiar.kuropomodoro.ui.components.DashboardDock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KuroPomodoroApp(repo: AppRepository) {
    val appState by repo.appState.collectAsStateWithLifecycle(
        initialValue = AppState(
            darkModePreview = false,
            glassReadabilityMode = GlassReadabilityMode.Adaptive,
            selectedSoundId = 1,
            completedSessions = 0,
            settings = TimerSettings(),
            nextTaskId = 7,
            selectedTodoListId = 1,
            stamps = emptyMap(),
            todoLists = listOf(
            TodoListState(
                1, "Study", listOf(
                    FocusTask(1, "Review Compose layout", 25),
                    FocusTask(2, "Read chapter notes", 20)
                )
            ),
            TodoListState(
                2, "Home", listOf(
                    FocusTask(3, "Clean desk", 10),
                    FocusTask(4, "Water plants", 5)
                )
            ),
            TodoListState(
                3, "Ideas", listOf(
                    FocusTask(5, "Sketch cat animation", 15),
                    FocusTask(6, "Plan next sprint", 25)
                )
            )
        )
        )
    )

    var settings by remember(appState.settings) { mutableStateOf(appState.settings) }
    var completedSessions by remember(appState.completedSessions) { mutableIntStateOf(appState.completedSessions) }
    var stamps by remember(appState.stamps) { mutableStateOf(appState.stamps) }
    var selectedSoundId by remember(appState.selectedSoundId) { mutableIntStateOf(appState.selectedSoundId) }
    var darkModePreview by remember(appState.darkModePreview) { mutableStateOf(appState.darkModePreview) }
    var glassReadabilityMode by remember(appState.glassReadabilityMode) { mutableStateOf(appState.glassReadabilityMode) }
    var nextTaskId by remember(appState.nextTaskId) { mutableIntStateOf(appState.nextTaskId) }
    var selectedTodoListId by remember(appState.selectedTodoListId) { mutableIntStateOf(appState.selectedTodoListId) }
    var todoLists by remember(appState.todoLists) { mutableStateOf(appState.todoLists) }

    var selectedTab by rememberSaveable(stateSaver = AppTabSaver) { mutableStateOf(AppTab.Timer) }
    var timerMode by rememberSaveable(stateSaver = TimerModeSaver) { mutableStateOf(TimerMode.Focus) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(settings.secondsFor(timerMode)) }
    var activeTotalSeconds by rememberSaveable { mutableIntStateOf(settings.secondsFor(timerMode)) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var playingSoundId by rememberSaveable { mutableStateOf<Int?>(null) }
    val isSoundPlaying = playingSoundId != null
    var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var activeTask by rememberSaveable(stateSaver = FocusTaskSaver) { mutableStateOf<FocusTask?>(null) }
    var isFocusMode by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun persistSettings(v: TimerSettings) { settings = v; scope.launch { repo.setSettings(v) } }
    fun persistCompletedSessions(v: Int) { completedSessions = v; scope.launch { repo.setCompletedSessions(v) } }
    fun persistStamps(v: Map<LocalDate, Int>) { stamps = v; scope.launch { repo.setStamps(v) } }
    fun persistSelectedSoundId(v: Int) { selectedSoundId = v; scope.launch { repo.setSelectedSoundId(v) } }
    fun persistDarkModePreview(v: Boolean) { darkModePreview = v; scope.launch { repo.setDarkModePreview(v) } }
    fun persistGlassReadabilityMode(v: GlassReadabilityMode) { glassReadabilityMode = v; scope.launch { repo.setGlassReadabilityMode(v) } }
    fun persistNextTaskId(v: Int) { nextTaskId = v; scope.launch { repo.setNextTaskId(v) } }
    fun persistSelectedTodoListId(v: Int) { selectedTodoListId = v; scope.launch { repo.setSelectedTodoListId(v) } }
    fun persistTodoLists(v: List<TodoListState>) { todoLists = v; scope.launch { repo.setTodoLists(v) } }

    val addTask: (String) -> Unit = { title ->
        persistTodoLists(todoLists.map { list ->
            if (list.id == selectedTodoListId) {
                list.copy(tasks = list.tasks + FocusTask(nextTaskId, title, 25))
            } else {
                list
            }
        })
        persistNextTaskId(nextTaskId + 1)
    }
    val toggleTask: (Int) -> Unit = { taskId ->
        persistTodoLists(todoLists.map { list ->
            list.copy(tasks = list.tasks.map { task -> if (task.id == taskId) task.toggleCompleted() else task })
        })
    }
    val deleteTask: (Int) -> Unit = { taskId ->
        persistTodoLists(
            todoLists.map { list -> list.copy(tasks = list.tasks.filterNot { it.id == taskId }) }
        )
    }
    var timerStartedAtMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var timerStartedRemainingSeconds by rememberSaveable {
        mutableIntStateOf(
            settings.secondsFor(
                timerMode
            )
        )
    }
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
            val updatedRemainingSeconds =
                (timerStartedRemainingSeconds - elapsedSeconds).coerceAtLeast(0)
            remainingSeconds = updatedRemainingSeconds

            if (updatedRemainingSeconds == 0) {
                isRunning = false
                val nextMode = if (timerMode == TimerMode.Focus) {
                    persistCompletedSessions(completedSessions + 1)
                    persistStamps(addFocusStamp(stamps, LocalDate.now()))
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

    val backdropTime = LocalTime.now()
    val glassTreatment = glassTreatmentFor(glassReadabilityMode, backdropTime)

    CatBackdrop(glassTreatment = glassTreatment, time = backdropTime) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                when (selectedTab) {
                    AppTab.Timer -> FocusDashboard(
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
                        }
                    )
                    AppTab.Todo -> TodoScreen(
                        todoLists = todoLists,
                        selectedListId = selectedTodoListId,
                        onListSelected = { persistSelectedTodoListId(it) },
                        onAddTask = addTask,
                        onToggleTask = toggleTask,
                        onDeleteTask = deleteTask,
                        glassTreatment = glassTreatment
                    )
                    AppTab.Calendar -> CalendarScreen(
                        stamps = stamps,
                        completedSessions = completedSessions,
                        glassTreatment = glassTreatment
                    )
                    AppTab.Sounds -> SoundsScreen(
                        sounds = sounds,
                        selectedSoundId = selectedSoundId,
                        playingSoundId = playingSoundId,
                        onSoundSelected = { sound ->
                            if (sound.locked) {
                                premiumSheetMessage = "${sound.name} is part of the simulated premium sound pack."
                            } else {
                                persistSelectedSoundId(sound.id)
                                playingSoundId = sound.id
                                premiumSheetMessage = null
                            }
                        },
                        onToggleSound = { sound ->
                            playingSoundId = if (playingSoundId == sound.id) null else sound.id
                        },
                        glassTreatment = glassTreatment
                    )
                    AppTab.Profile -> ProfileScreen(
                        settings = settings,
                        darkModePreview = darkModePreview,
                        glassReadabilityMode = glassReadabilityMode,
                        glassTreatment = glassTreatment,
                        onDarkModeChanged = { persistDarkModePreview(it) },
                        onGlassReadabilityModeChanged = { persistGlassReadabilityMode(it) },
                        onDurationChanged = { mode, minutes ->
                            val updatedSettings = settings.update(mode, minutes)
                            persistSettings(updatedSettings)
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
            DashboardDock(
                isRunning = isRunning,
                isSoundPlaying = isSoundPlaying,
                onNavigate = { selectedTab = it },
                selectedTab = selectedTab,
                glassTreatment = glassTreatment,
                modifier = Modifier.navigationBarsPadding()
            )
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
) {
    val buttonBg = if (isRunning) Color.White.copy(alpha = 0.92f) else Color(0xFFFF8B6F)
    val buttonContentColor = if (isRunning) Color(0xFF241E2B) else Color.White
    var showModeMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = formatSeconds(remainingSeconds),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 80.sp,
            fontWeight = FontWeight.W400,
            letterSpacing = (-1).sp
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(
                        1.5.dp,
                        Color.White.copy(alpha = 0.35f),
                        RoundedCornerShape(28.dp)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(buttonBg)
                    .clickable(onClick = onStartPause),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "\u23f8" else "\u25b6",
                    color = buttonContentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.wrapContentSize(Alignment.TopCenter)) {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .border(
                            1.5.dp,
                            Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(28.dp)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { showModeMenu = !showModeMenu }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timerMode.label,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "\u25be",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    }
                }
                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    containerColor = Color(0xFF1E1628)
                ) {
                    TimerMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    color = if (mode == timerMode) Color(0xFFFF8B6F) else Color.White.copy(
                                        alpha = 0.85f
                                    ),
                                    fontWeight = if (mode == timerMode) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onModeSelected(mode)
                                showModeMenu = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
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

private val FocusTaskSaver = Saver<FocusTask?, List<Any?>>(
    save = { task ->
        if (task == null) listOf(null)
        else listOf(task.id, task.title, task.minutes, task.completed, task.plannedSessions, task.completedSessions)
    },
    restore = { list ->
        if (list[0] == null) null
        else FocusTask(
            id = list[0] as Int,
            title = list[1] as String,
            minutes = list[2] as Int,
            completed = list[3] as Boolean,
            plannedSessions = list[4] as Int,
            completedSessions = list[5] as Int
        )
    }
)
