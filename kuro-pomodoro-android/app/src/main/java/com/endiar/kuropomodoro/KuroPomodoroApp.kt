package com.endiar.kuropomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.endiar.kuropomodoro.model.glassTreatmentFor
import com.endiar.kuropomodoro.timer.TimerConnection
import com.endiar.kuropomodoro.timer.TimerNotification
import com.endiar.kuropomodoro.ui.CalendarScreen
import com.endiar.kuropomodoro.ui.CycleCompletionCopy
import com.endiar.kuropomodoro.ui.HabitsScreen
import com.endiar.kuropomodoro.ui.HabitDraft
import com.endiar.kuropomodoro.ui.HomeTimerScreen
import com.endiar.kuropomodoro.ui.ProfileScreen
import com.endiar.kuropomodoro.ui.SoundsScreen
import com.endiar.kuropomodoro.ui.StartFocusPrefill
import com.endiar.kuropomodoro.ui.StartFocusSheet
import com.endiar.kuropomodoro.ui.TodoScreen
import com.endiar.kuropomodoro.ui.continueTask
import com.endiar.kuropomodoro.ui.homeStartTask
import com.endiar.kuropomodoro.ui.homeGreetingFor
import com.endiar.kuropomodoro.ui.pomodoroProgress
import com.endiar.kuropomodoro.ui.todayFocusMetrics
import com.endiar.kuropomodoro.model.effectiveTarget
import com.endiar.kuropomodoro.model.progressOn
import com.endiar.kuropomodoro.ui.components.CatBackdrop
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.DashboardDock
import com.endiar.kuropomodoro.ui.components.FocusModeMotion
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.endiar.kuropomodoro.ui.components.glassTokens
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    var notificationSoundEnabled by remember(appState.notificationSoundEnabled) { mutableStateOf(appState.notificationSoundEnabled) }

    val context = LocalContext.current

    val isPremium = PremiumOverride.overrideValue() ?: appState.isPremium

    var selectedTab by rememberSaveable(stateSaver = AppTabSaver) { mutableStateOf(AppTab.Timer) }
    var playingSoundId by rememberSaveable { mutableStateOf<Int?>(null) }
    val isSoundPlaying = playingSoundId != null
    var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showSoundsSheet by rememberSaveable { mutableStateOf(false) }
    var showStartFocusSheet by rememberSaveable { mutableStateOf(false) }
    var startFocusPrefill by remember { mutableStateOf<StartFocusPrefill?>(null) }
    var activeTask by rememberSaveable(stateSaver = FocusTaskSaver) {
        mutableStateOf<FocusTask?>(
            null
        )
    }
    var isFocusMode by rememberSaveable { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

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

    fun persistNotificationSoundEnabled(v: Boolean) {
        notificationSoundEnabled = v
        TimerNotification.setNotificationSoundEnabled(context, v)
        scope.launch { repo.setNotificationSoundEnabled(v) }
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

    // Treat focus as active during the brief async window where runtimeState hasn't
    // arrived yet — so cold-restart into focus mode snaps to the final state instead
    // of briefly flashing the dashboard then animating in.
    val focusActive = isFocusMode && (isRunning || runtimeState == null)
    val focusTransition = updateTransition(targetState = focusActive, label = "focusMode")

    val scrimAlpha by focusTransition.animateFloat(
        transitionSpec = FocusModeMotion.spec(),
        label = "scrimAlpha"
    ) { active -> if (active) 0f else FocusModeMotion.ScrimAlphaDashboard }

    // Edge gradient scrims in CatBackdrop fade out alongside the full-screen
    // scrim when entering focus, opening up the cat photo. Stays at 1f on
    // non-Timer tabs because focusActive can only be true on the Timer tab
    // (the dock is hidden in focus mode, so no other tab is reachable).
    val edgeScrimMultiplier by focusTransition.animateFloat(
        transitionSpec = FocusModeMotion.spec(),
        label = "edgeScrimMultiplier"
    ) { active -> if (active) 0f else 1f }

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
    val homeGreeting = homeGreetingFor(backdropTime)
    val homeContinueTask = continueTask(activeTask, tasks)
    val homeMetrics = todayFocusMetrics(
        sessionRecords = sessionRecords,
        today = LocalDate.now()
    )
    val homeDots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, settings)
    val homeProgressTask = runtimeState?.activeTask ?: activeTask ?: homeContinueTask
    val homePomodoroProgress = pomodoroProgress(
        completedFocusSessions = homeDots.completedFocusSessions,
        totalFocusSessions = homeDots.totalFocusSessions,
        isStarted = isStarted,
        task = homeProgressTask,
        settings = settings
    )
    val glassTreatment = glassTreatmentFor(glassReadabilityMode, backdropTime)

    val useCatBackdrop = false
    val overBackdrop = false

    // Hoisted so it survives the CatBackdrop/KuroFlatSurface slot swap on tab
    // switches. Otherwise it resets to 0 on every return to the Timer tab,
    // which makes BoxWithConstraints.maxHeight grow then shrink, causing the
    // animateDp-driven topSpacer to slide the cluster upward on re-entry.
    var dockHeightPx by remember { mutableIntStateOf(0) }

    val surfaceContent: @Composable BoxScope.() -> Unit = {
        val density = LocalDensity.current
        val dockHeightDp = with(density) { dockHeightPx.toDp() }
        val screenContentPadding = PaddingValues(bottom = dockHeightDp)

        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim belongs to the Timer dashboard only — render it nowhere else
            // so tab switches don't fade a black overlay in/out behind content.
            // Only used with CatBackdrop to dim the photo; skip on flat surface.
            if (useCatBackdrop && selectedTab == AppTab.Timer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(scrimAlpha)
                        .background(Color.Black)
                )
            }

            // For the cream-background tabs (Todo, Habits) paint the background
            // edge-to-edge including behind the status bar, before the
            // statusBarsPadding box constrains the content area below.
            // Removed: each screen now uses tokens.pageBackground directly,
            // and KuroFlatSurface fills the full surface including status bar region.

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                when (selectedTab) {
                    AppTab.Timer -> HomeTimerScreen(
                        timerMode = timerMode,
                        remainingSeconds = remainingSeconds,
                        isRunning = isRunning,
                        isStarted = isStarted,
                        activeTask = activeTask,
                        continueTask = homeContinueTask,
                        greeting = homeGreeting,
                        todayMetrics = homeMetrics,
                        cycleItems = cycleItems,
                        cycleIndex = cycleIndex,
                        currentTimerState = currentTimerState,
                        timerSettings = settings,
                        pomodoroProgress = homePomodoroProgress,
                        isSoundPlaying = isSoundPlaying,
                        backdropTime = backdropTime,
                        onSkip = { timerConnection.controller.skip() },
                        onStartPause = {
                            when {
                                isRunning -> {
                                    isFocusMode = false
                                    timerConnection.controller.pause()
                                }

                                else -> {
                                    isFocusMode = false
                                    timerConnection.controller.resume()
                                }
                            }
                        },
                        onRequestStartFocus = {
                            startFocusPrefill = null
                            showStartFocusSheet = true
                        },
                        onContinueFocus = {
                            startFocusPrefill = homeContinueTask?.let { StartFocusPrefill.Task(it.id) }
                            showStartFocusSheet = true
                        },
                        onStop = {
                            timerConnection.controller.stop()
                            isFocusMode = false
                            activeTask = null
                        },
                        onOpenSoundsSheet = { showSoundsSheet = true },
                        onOpenProfile = { showProfile = true },
                        contentPadding = screenContentPadding
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
                        onFocusTask = { request ->
                            activeTask = request.task
                            isFocusMode = false
                            selectedTab = AppTab.Timer
                            requestNotificationPermission()
                            timerConnection.controller.start(request.settings, request.task, request.habitId)
                        },
                        habits = appState.habits,
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

                    AppTab.Habits -> HabitsScreen(
                        habits = appState.habits,
                        today = java.time.LocalDate.now(),
                        onAddHabit = { draft ->
                            scope.launch {
                                repo.addHabit(
                                    name = draft.name,
                                    emoji = draft.emoji,
                                    colorArgb = draft.colorArgb,
                                    targetType = draft.targetType,
                                    targetValue = draft.targetValue,
                                    occurrence = draft.occurrence,
                                    timeBlock = draft.timeBlock,
                                )
                            }
                        },
                        onUpdateHabit = { id, draft ->
                            scope.launch {
                                repo.updateHabitMeta(
                                    id = id,
                                    name = draft.name,
                                    emoji = draft.emoji,
                                    colorArgb = draft.colorArgb,
                                    targetType = draft.targetType,
                                    targetValue = draft.targetValue,
                                    occurrence = draft.occurrence,
                                    timeBlock = draft.timeBlock,
                                )
                            }
                        },
                        onToggleBinary = { id, date ->
                            scope.launch { repo.toggleHabitBinaryCompletion(id, date) }
                        },
                        onIncrementProgress = { id, date, delta ->
                            scope.launch { repo.incrementHabitProgress(id, date, delta) }
                        },
                        onSetSkipped = { id, date, skipped ->
                            scope.launch { repo.setHabitSkipped(id, date, skipped) }
                        },
                        onClearLog = { id, date ->
                            scope.launch { repo.clearHabitLog(id, date) }
                        },
                        onDeleteHabit = { id -> scope.launch { repo.deleteHabit(id) } },
                        glassTreatment = glassTreatment,
                        overBackdrop = overBackdrop,
                        onStartPomodoroForHabit = { habit ->
                            val remaining = habit.effectiveTarget() - habit.progressOn(java.time.LocalDate.now())
                            val displaySessions = remaining.coerceAtLeast(1)
                            val syntheticTask = FocusTask(
                                id = -habit.id,
                                title = "${habit.emoji} ${habit.name}",
                                minutes = 0,
                                completed = false,
                                plannedSessions = displaySessions,
                                completedSessions = habit.progressOn(java.time.LocalDate.now()),
                            )
                            activeTask = syntheticTask
                            isFocusMode = false
                            selectedTab = AppTab.Timer
                            requestNotificationPermission()
                            timerConnection.controller.start(settings, syntheticTask, habit.id)
                        },
                         contentPadding = screenContentPadding,
                     )
                }
            }

            if (showStartFocusSheet) {
                key(startFocusPrefill) {
                    StartFocusSheet(
                        settings = settings,
                        tasks = tasks,
                        habits = appState.habits,
                        today = LocalDate.now(),
                        prefill = startFocusPrefill,
                        onDismiss = {
                            showStartFocusSheet = false
                            startFocusPrefill = null
                        },
                        onStartFocus = { request ->
                            showStartFocusSheet = false
                            startFocusPrefill = null
                            requestNotificationPermission()
                            isFocusMode = false
                            activeTask = request.task
                            timerConnection.controller.start(request.settings, request.task, request.habitId)
                        },
                    )
                }
            }

            if (showProfile) {
                ProfileScreen(
                    settings = settings,
                    onBack = { showProfile = false },
                    onUpdateSettings = { persistSettings(it) },
                    notificationSoundEnabled = notificationSoundEnabled,
                    onUpdateNotificationSound = { persistNotificationSoundEnabled(it) },
                )
            }

            if (!showSoundsSheet) {
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
            }

            // Child of focusTransition so it shares the single clock with scrim + timer
            // (frame-perfect parallel). Dock visibility is the inverse of focus mode:
            // appearing = exiting focus (Exit, 400ms, snappier); disappearing = entering focus (Enter, 700ms, deliberate).
            focusTransition.AnimatedVisibility(
                visible = { active -> !active && !showProfile },
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = FocusModeMotion.ExitDurationMs,
                        easing = FocusModeMotion.ExitEasing
                    )
                ) { fullHeight -> fullHeight } + fadeIn(
                    animationSpec = tween(
                        durationMillis = FocusModeMotion.ExitDurationMs,
                        easing = FocusModeMotion.ExitEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = FocusModeMotion.EnterDurationMs,
                        easing = FocusModeMotion.EnterEasing
                    )
                ) { fullHeight -> fullHeight } + fadeOut(
                    animationSpec = tween(
                        durationMillis = FocusModeMotion.EnterDurationMs,
                        easing = FocusModeMotion.EnterEasing
                    )
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                DashboardDock(
                    isRunning = isRunning,
                    onNavigate = { selectedTab = it },
                    selectedTab = selectedTab,
                    glassTreatment = glassTreatment,
                    overBackdrop = true,
                    modifier = Modifier
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

            if (showSoundsSheet) {
                val sheetTokens = glassTokens(GlassTreatment.Clear, false)
                ModalBottomSheet(
                    onDismissRequest = {
                        showSoundsSheet = false
                        premiumSheetMessage = null
                    },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                    containerColor = sheetTokens.cardFill,
                    contentColor = MaterialTheme.colorScheme.kuroTokens.textPrimary,
                    contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
                ) {
                    premiumSheetMessage?.let { message ->
                        PremiumMessage(
                            message = message,
                            onDismiss = { premiumSheetMessage = null },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    SoundsScreen(
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
                        glassTreatment = GlassTreatment.Clear,
                        overBackdrop = false,
                    )
            }

        }
    }
    }

    if (useCatBackdrop) {
        CatBackdrop(
            glassTreatment = glassTreatment,
            time = backdropTime,
            edgeScrimAlphaMultiplier = edgeScrimMultiplier,
            content = surfaceContent
        )
    } else {
        KuroFlatSurface(content = surfaceContent)
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
    restore = { name -> AppTab.entries.firstOrNull { it.name == name } ?: AppTab.Timer }
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
