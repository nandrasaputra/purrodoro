package com.endiar.kuropomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.data.AppState
import com.endiar.kuropomodoro.model.AppClock
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.AppearanceMode
import com.endiar.kuropomodoro.model.Category
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassReadabilityMode
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.PremiumOverride
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.cycleDotsState
import com.endiar.kuropomodoro.model.formatSeconds
import com.endiar.kuropomodoro.model.glassTreatmentFor
import com.endiar.kuropomodoro.timer.TimerConnection
import com.endiar.kuropomodoro.ui.CalendarScreen
import com.endiar.kuropomodoro.ui.CycleCompletionCopy
import com.endiar.kuropomodoro.ui.ProfileScreen
import com.endiar.kuropomodoro.ui.SoundsScreen
import com.endiar.kuropomodoro.ui.TodoScreen
import com.endiar.kuropomodoro.ui.components.CatBackdrop
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.CycleDotsRow
import com.endiar.kuropomodoro.ui.components.DashboardDock
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KuroPomodoroApp(
    repo: AppRepository,
    timerConnection: TimerConnection,
    requestNotificationPermission: () -> Unit
) {
    val appState by repo.appState.collectAsStateWithLifecycle(
        initialValue = AppState(
            glassReadabilityMode = GlassReadabilityMode.Adaptive,
            selectedSoundId = 1,
            completedSessions = 0,
            settings = TimerSettings(),
            nextTaskId = 1,
            stamps = emptyMap(),
            tasks = listOf(),
            categories = listOf(),
            nextCategoryId = 1,
            selectedCategoryId = null
        )
    )

    val runtimeState by timerConnection.state.collectAsStateWithLifecycle(initialValue = null)

    val pendingCycleAck by repo.pendingCycleCompletionAckFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val cycleCompletionVariant = remember(pendingCycleAck) {
        if (pendingCycleAck) CycleCompletionCopy.variants.random() else null
    }

    var settings by remember(appState.settings) { mutableStateOf(appState.settings) }
    var completedSessions by remember(appState.completedSessions) { mutableIntStateOf(appState.completedSessions) }
    var stamps by remember(appState.stamps) { mutableStateOf(appState.stamps) }
    var selectedSoundId by remember(appState.selectedSoundId) { mutableIntStateOf(appState.selectedSoundId) }
    var glassReadabilityMode by remember(appState.glassReadabilityMode) { mutableStateOf(appState.glassReadabilityMode) }
    var appearanceMode by remember(appState.appearanceMode) { mutableStateOf(appState.appearanceMode) }
    var nextTaskId by remember(appState.nextTaskId) { mutableIntStateOf(appState.nextTaskId) }
    var sessionRecords by remember(appState.sessionRecords) { mutableStateOf(appState.sessionRecords) }
    var tasks by remember(appState.tasks) { mutableStateOf(appState.tasks) }
    var categories by remember(appState.categories) { mutableStateOf(appState.categories) }
    var nextCategoryId by remember(appState.nextCategoryId) { mutableIntStateOf(appState.nextCategoryId) }
    var selectedCategoryId by remember(appState.selectedCategoryId) { mutableStateOf(appState.selectedCategoryId) }

    val isPremium = PremiumOverride.overrideValue() ?: appState.isPremium

    var selectedTab by rememberSaveable(stateSaver = AppTabSaver) { mutableStateOf(AppTab.Timer) }
    var playingSoundId by rememberSaveable { mutableStateOf<Int?>(null) }
    val isSoundPlaying = playingSoundId != null
    var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var activeTask by rememberSaveable(stateSaver = FocusTaskSaver) {
        mutableStateOf<FocusTask?>(
            null
        )
    }
    var isFocusMode by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun persistSettings(v: TimerSettings) {
        settings = v; scope.launch { repo.setSettings(v) }
    }

    fun persistCompletedSessions(v: Int) {
        completedSessions = v; scope.launch { repo.setCompletedSessions(v) }
    }

    fun persistStamps(v: Map<LocalDate, Int>) {
        stamps = v; scope.launch { repo.setStamps(v) }
    }

    fun persistSelectedSoundId(v: Int) {
        selectedSoundId = v; scope.launch { repo.setSelectedSoundId(v) }
    }

    fun persistGlassReadabilityMode(v: GlassReadabilityMode) {
        glassReadabilityMode = v; scope.launch { repo.setGlassReadabilityMode(v) }
    }

    fun persistAppearanceMode(v: AppearanceMode) {
        appearanceMode = v; scope.launch { repo.setAppearanceMode(v) }
    }

    fun persistNextTaskId(v: Int) {
        nextTaskId = v; scope.launch { repo.setNextTaskId(v) }
    }

    fun persistTasks(v: List<FocusTask>) {
        tasks = v; scope.launch { repo.setTasks(v) }
    }

    fun persistSelectedCategoryId(v: Int?) {
        selectedCategoryId = v; scope.launch { repo.setSelectedCategoryId(v) }
    }

    val addTask: (String, Int?) -> Unit = { title, categoryId ->
        val newTask = FocusTask(nextTaskId, title, settings.focusMinutes, categoryId = categoryId)
        persistTasks(tasks + newTask)
        persistNextTaskId(nextTaskId + 1)
    }
    val toggleTask: (Int) -> Unit = { taskId ->
        persistTasks(tasks.map { if (it.id == taskId) it.toggleCompleted() else it })
    }
    val deleteTask: (Int) -> Unit = { taskId ->
        persistTasks(tasks.filterNot { it.id == taskId })
    }
    val updateTask: (FocusTask) -> Unit = { updated ->
        persistTasks(tasks.map { if (it.id == updated.id) updated else it })
    }
    val deleteAllCompleted: (Int?) -> Unit = { filterCategoryId ->
        val toDelete: (FocusTask) -> Boolean = { task ->
            task.completed && (filterCategoryId == null || task.categoryId == filterCategoryId)
        }
        persistTasks(tasks.filterNot(toDelete))
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

    // --- Derive timer state from the service ---
    val isRunning = runtimeState?.session?.timerState == TimerState.Playing
    val isStarted = runtimeState != null && runtimeState?.session?.timerState != TimerState.Idle

    // Drive recomposition every 250ms while playing so the displayed countdown ticks.
    var tickNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            tickNow = System.currentTimeMillis()
            delay(250L)
        }
    }
    val now = tickNow

    val remainingSeconds = runtimeState?.let { rt ->
        if (rt.session.timerState == TimerState.Playing && rt.endsAtMillis != null) {
            ((rt.endsAtMillis - now) / 1000L).toInt().coerceAtLeast(0)
        } else {
            rt.session.remainingSeconds
        }
    } ?: settings.secondsFor(TimerMode.Focus)

    val currentItemType = runtimeState?.session?.items
        ?.getOrNull(runtimeState?.session?.currentIndex ?: 0)?.type
    val timerMode = when (currentItemType) {
        is SessionItemType.Focus -> TimerMode.Focus
        is SessionItemType.ShortBreak -> TimerMode.ShortBreak
        is SessionItemType.LongBreak -> TimerMode.LongBreak
        null -> TimerMode.Focus
    }
    val cycleItems: List<TimerSessionItem> = runtimeState?.session?.items ?: emptyList()
    val cycleIndex = runtimeState?.session?.currentIndex ?: 0
    val currentTimerState = runtimeState?.session?.timerState ?: TimerState.Idle

    val backdropTime = AppClock.now()
    val glassTreatment = glassTreatmentFor(glassReadabilityMode, backdropTime)

    val useCatBackdrop = selectedTab == AppTab.Timer
    val overBackdrop = selectedTab == AppTab.Timer

    val surfaceContent: @Composable BoxScope.() -> Unit = {
        var dockHeightPx by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val dockHeightDp = with(density) { dockHeightPx.toDp() }
        val screenContentPadding = PaddingValues(bottom = dockHeightDp)

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                when (selectedTab) {
                    AppTab.Timer -> FocusDashboard(
                        timerMode = timerMode,
                        remainingSeconds = remainingSeconds,
                        isRunning = isRunning,
                        isStarted = isStarted,
                        isFocusMode = isFocusMode,
                        activeTask = activeTask,
                        cycleItems = cycleItems,
                        cycleIndex = cycleIndex,
                        currentTimerState = currentTimerState,
                        timerSettings = settings,
                        onSkip = { timerConnection.controller.skip() },
                        onStartPause = {
                            when {
                                !isStarted -> {
                                    requestNotificationPermission()
                                    isFocusMode = true
                                    timerConnection.controller.start(settings, activeTask)
                                }

                                isRunning -> {
                                    isFocusMode = false
                                    timerConnection.controller.pause()
                                }

                                else -> {
                                    timerConnection.controller.resume()
                                }
                            }
                        },
                        onStop = {
                            timerConnection.controller.stop()
                            isFocusMode = false
                            activeTask = null
                        },
                        onExitFocusMode = { isFocusMode = false },
                        onEnterFocusMode = { isFocusMode = true }
                    )

                    AppTab.Todo -> TodoScreen(
                        tasks = tasks,
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { persistSelectedCategoryId(it) },
                        onAddTask = addTask,
                        onToggleTask = toggleTask,
                        onDeleteTask = deleteTask,
                        onUpdateTask = updateTask,
                        onDeleteAllCompleted = deleteAllCompleted,
                        onAddCategory = { name -> scope.launch { repo.addCategory(name) } },
                        onRenameCategory = { id, name ->
                            scope.launch {
                                repo.renameCategory(
                                    id,
                                    name
                                )
                            }
                        },
                        onDeleteCategory = { id -> scope.launch { repo.deleteCategory(id) } },
                        onFocusTask = { task ->
                            activeTask = task
                            isFocusMode = true
                            selectedTab = AppTab.Timer
                            requestNotificationPermission()
                            timerConnection.controller.start(settings, task)
                        },
                        settings = settings,
                        glassTreatment = glassTreatment,
                        contentPadding = screenContentPadding,
                        overBackdrop = overBackdrop,
                    )

                    AppTab.Calendar -> CalendarScreen(
                        stamps = stamps,
                        completedSessions = completedSessions,
                        sessionRecords = sessionRecords,
                        onDeleteRecord = { id -> scope.launch { repo.deleteSessionRecord(id) } },
                        glassTreatment = glassTreatment,
                        contentPadding = screenContentPadding,
                        overBackdrop = overBackdrop,
                    )

                    AppTab.Sounds -> SoundsScreen(
                        sounds = sounds,
                        selectedSoundId = selectedSoundId,
                        playingSoundId = playingSoundId,
                        onSoundSelected = { sound ->
                            if (sound.locked && !isPremium) {
                                premiumSheetMessage =
                                    "${sound.name} is part of the simulated premium sound pack."
                            } else {
                                persistSelectedSoundId(sound.id)
                                playingSoundId = sound.id
                                premiumSheetMessage = null
                            }
                        },
                        onToggleSound = { sound ->
                            playingSoundId = if (playingSoundId == sound.id) null else sound.id
                        },
                        glassTreatment = glassTreatment,
                        contentPadding = screenContentPadding,
                        overBackdrop = overBackdrop,
                    )

                    AppTab.Profile -> ProfileScreen(
                        settings = settings,
                        glassReadabilityMode = glassReadabilityMode,
                        glassTreatment = glassTreatment,
                        appearanceMode = appearanceMode,
                        onAppearanceModeChanged = ::persistAppearanceMode,
                        onGlassReadabilityModeChanged = { persistGlassReadabilityMode(it) },
                        onDurationChanged = { mode, minutes ->
                            val updatedSettings = settings.update(mode, minutes)
                            persistSettings(updatedSettings)
                        },
                        onCycleSizeChanged = { n ->
                            val updatedSettings = settings.updateCycleSize(n)
                            persistSettings(updatedSettings)
                        },
                        onResetSettings = {
                            scope.launch { repo.resetSettingsToDefault() }
                        },
                        onMockFeature = { premiumSheetMessage = it },
                        overBackdrop = overBackdrop,
                        contentPadding = screenContentPadding,
                    )
                }
            }

            premiumSheetMessage?.let { message ->
                PremiumMessage(
                    message = message,
                    onDismiss = { premiumSheetMessage = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp)
                        .offset(y = -(dockHeightDp + 8.dp))
                )
            }

            if (!(isFocusMode && isRunning)) {
                DashboardDock(
                    isRunning = isRunning,
                    isSoundPlaying = isSoundPlaying,
                    onNavigate = { selectedTab = it },
                    selectedTab = selectedTab,
                    glassTreatment = glassTreatment,
                    overBackdrop = overBackdrop,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .onSizeChanged { dockHeightPx = it.height }
                )
            }

            if (pendingCycleAck && cycleCompletionVariant != null) {
                AlertDialog(
                    onDismissRequest = { /* non-cancelable */ },
                    title = {
                        Text(
                            text = cycleCompletionVariant.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Text(
                            text = cycleCompletionVariant.body,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            timerConnection.controller.dismissCompletion()
                            scope.launch { repo.setPendingCycleCompletionAck(false) }
                        }) {
                            Text(
                                text = CycleCompletionCopy.buttonLabel,
                                color = Color(0xFFFF8B6F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    ),
                    containerColor = Color(0xFF2A2035),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }

    if (useCatBackdrop) {
        CatBackdrop(glassTreatment = glassTreatment, time = backdropTime, content = surfaceContent)
    } else {
        KuroFlatSurface(content = surfaceContent)
    }
}

@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    isFocusMode: Boolean,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
) {
    if (isFocusMode && isRunning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onExitFocusMode),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "FOCUS",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = formatSeconds(remainingSeconds),
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.W200,
                    letterSpacing = (-2).sp
                )
                if (activeTask != null) {
                    Text(
                        text = activeTask.title,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${activeTask.completedSessions + 1} / ${activeTask.plannedSessions}",
                        color = Color(0xFFFF8B6F).copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "tap anywhere to show controls",
                    color = Color.White.copy(alpha = 0.22f),
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    val dots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, timerSettings)
    val totalFocusSessions = dots.totalFocusSessions
    val completedFocusSessions = dots.completedFocusSessions
    val isCurrentlyFocus = dots.isCurrentlyFocus

    val buttonBg = if (isRunning) Color.White.copy(alpha = 0.92f) else Color(0xFFFF8B6F)
    val buttonContentColor = if (isRunning) Color(0xFF241E2B) else Color.White
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        if (isRunning && activeTask != null) {
            Text(
                text = activeTask.title,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${activeTask.completedSessions + 1} / ${activeTask.plannedSessions}",
                color = Color(0xFFFF8B6F).copy(alpha = 0.80f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        CycleDotsRow(
            totalFocusSessions = totalFocusSessions,
            completedFocusSessions = completedFocusSessions,
            isCurrentlyFocus = isCurrentlyFocus,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = formatSeconds(remainingSeconds),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 80.sp,
            fontWeight = FontWeight.W400,
            letterSpacing = (-1).sp
        )
        if (isStarted && !isRunning) {
            Spacer(Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = "${timerMode.label} · paused",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning) {
                val currentItemType = cycleItems.getOrNull(cycleIndex)?.type
                val isOnBreak = currentItemType is SessionItemType.ShortBreak ||
                        currentItemType is SessionItemType.LongBreak
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(buttonBg)
                        .clickable(onClick = onStartPause),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u23f8",
                        color = buttonContentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isOnBreak) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .border(
                                1.5.dp,
                                Color.White.copy(alpha = 0.20f),
                                RoundedCornerShape(28.dp)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onSkip)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (isStarted) {
                val currentItemType = cycleItems.getOrNull(cycleIndex)?.type
                val isOnBreak = currentItemType is SessionItemType.ShortBreak ||
                        currentItemType is SessionItemType.LongBreak
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(buttonBg)
                        .clickable(onClick = onStartPause),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u25b6",
                        color = buttonContentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            val currentItemType2 = cycleItems.getOrNull(cycleIndex)?.type
                            if (currentItemType2 is SessionItemType.Focus) {
                                showStopConfirmDialog = true
                            } else {
                                onStop()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u25a0",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 18.sp
                    )
                }
                if (isOnBreak) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .border(
                                1.5.dp,
                                Color.White.copy(alpha = 0.20f),
                                RoundedCornerShape(28.dp)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onSkip)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFFF8B6F))
                        .clickable(onClick = onStartPause),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Focus Now",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (showStopConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showStopConfirmDialog = false },
                title = {
                    Text(
                        text = "Taking a break?",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "Your focus time so far will still be counted.",
                        color = Color.White.copy(alpha = 0.75f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showStopConfirmDialog = false
                        onStop()
                    }) {
                        Text(
                            text = "Stop for now",
                            color = Color(0xFFFF8B6F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopConfirmDialog = false }) {
                        Text(
                            text = "Stay focused",
                            color = Color.White.copy(alpha = 0.70f)
                        )
                    }
                },
                containerColor = Color(0xFF2A2035),
                shape = RoundedCornerShape(20.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        if (isRunning) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFF8B6F).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFFFF8B6F).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onEnterFocusMode)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "↗  enter focus mode",
                    color = Color(0xFFFF8B6F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(24.dp))
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

private val FocusTaskSaver = Saver<FocusTask?, List<Any?>>(
    save = { task ->
        if (task == null) listOf(null)
        else listOf(
            task.id,
            task.title,
            task.minutes,
            task.completed,
            task.plannedSessions,
            task.completedSessions
        )
    },
    restore = { list ->
        if (list[0] == null) null
        else FocusTask(
            id = (list[0] as Number).toInt(),
            title = list[1] as String,
            minutes = (list[2] as Number).toInt(),
            completed = list[3] as Boolean,
            plannedSessions = (list[4] as Number).toInt(),
            completedSessions = (list[5] as Number).toInt()
        )
    }
)
