package com.endiar.kuropomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
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
import com.endiar.kuropomodoro.timer.TimerNotification
import com.endiar.kuropomodoro.ui.CalendarScreen
import com.endiar.kuropomodoro.ui.CycleCompletionCopy
import com.endiar.kuropomodoro.ui.ProfileScreen
import com.endiar.kuropomodoro.ui.SoundsScreen
import com.endiar.kuropomodoro.ui.TodoScreen
import com.endiar.kuropomodoro.ui.components.CatBackdrop
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.CycleDotsRow
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
    val glassTreatment = glassTreatmentFor(glassReadabilityMode, backdropTime)

    val useCatBackdrop = selectedTab == AppTab.Timer
    val overBackdrop = selectedTab == AppTab.Timer

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
            if (selectedTab == AppTab.Timer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(scrimAlpha)
                        .background(Color.Black)
                )
            }

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
                        focusActive = focusActive,
                        focusTransition = focusTransition,
                        activeTask = activeTask,
                        cycleItems = cycleItems,
                        cycleIndex = cycleIndex,
                        currentTimerState = currentTimerState,
                        timerSettings = settings,
                        isSoundPlaying = isSoundPlaying,
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
                        onOpenSoundsSheet = { showSoundsSheet = true },
                        onEnterFocusMode = { isFocusMode = true },
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
                        notificationSoundEnabled = notificationSoundEnabled,
                        onNotificationSoundChanged = ::persistNotificationSoundEnabled,
                    )
                }
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
                visible = { active -> !active },
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
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    focusActive: Boolean,
    focusTransition: Transition<Boolean>,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    isSoundPlaying: Boolean,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onOpenSoundsSheet: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val dots = cycleDotsState(cycleItems, cycleIndex, currentTimerState, timerSettings)
    val totalFocusSessions = dots.totalFocusSessions
    val completedFocusSessions = dots.completedFocusSessions
    val isCurrentlyFocus = dots.isCurrentlyFocus

    val buttonBg = if (isRunning) Color.White.copy(alpha = 0.92f) else Color(0xFFFF8B6F)
    val buttonContentColor = if (isRunning) Color(0xFF241E2B) else Color.White
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    val timerSize by focusTransition.animateFloat(
        transitionSpec = FocusModeMotion.spec(),
        label = "timerSize"
    ) { active -> if (active) 96f else 80f }

    // Snap weight on transition start. Fonts only render discrete weights
    // (100/200/.../900), so interpolating ints creates a visible stair-step
    // mid-morph. Snapping lets the size/letter-spacing morph carry the
    // visual change cleanly.
    val timerWeight = if (focusActive) 200 else 400

    val timerLetterSpacing by focusTransition.animateFloat(
        transitionSpec = FocusModeMotion.spec(),
        label = "timerLetterSpacing"
    ) { active -> if (active) -2f else -1f }

    val timerAlpha by focusTransition.animateFloat(
        transitionSpec = FocusModeMotion.spec(),
        label = "timerAlpha"
    ) { active -> if (active) 0.95f else 0.9f }

    val breathScale = remember { Animatable(1f) }
    var hasComposed by remember { mutableStateOf(false) }
    LaunchedEffect(focusActive) {
        if (focusActive && hasComposed) {
            delay(FocusModeMotion.BreathDelayMs)
            breathScale.animateTo(
                targetValue = FocusModeMotion.BreathScalePeak,
                animationSpec = tween(
                    durationMillis = FocusModeMotion.BreathDurationMs / 2,
                    easing = FocusModeMotion.EnterEasing
                )
            )
            breathScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = FocusModeMotion.BreathDurationMs / 2,
                    easing = FocusModeMotion.ExitEasing
                )
            )
        } else if (!focusActive) {
            breathScale.snapTo(1f)
        }
        hasComposed = true
    }

    // Guards enter animations from replaying when FocusDashboard re-enters the
    // composition after a tab switch. Dashboard elements (CycleDotsRow, buttons)
    // are immediately visible on first frame — no enter animation needed. The
    // flag becomes true after the first frame so focus-exit transitions still
    // animate normally on all subsequent visibility changes.
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { initialized = true }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            // Full-screen tap-to-exit layer for focus mode, gated on the underlying
            // boolean so taps work the instant focus is active (not after animation).
            // The semantics block exposes the same action to TalkBack / assistive tech.
            .then(
                if (focusActive) {
                    Modifier
                        .pointerInput(focusActive) {
                            detectTapGestures(onTap = { onExitFocusMode() })
                        }
                        .semantics(mergeDescendants = true) {
                            onClick(label = "Exit focus mode") {
                                onExitFocusMode()
                                true
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        val centerReserve = ((maxHeight - 240.dp) / 2).coerceAtLeast(40.dp)
        val topSpacerHeight by focusTransition.animateDp(
            transitionSpec = FocusModeMotion.spec(),
            label = "topSpacer"
        ) { centerReserve }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(topSpacerHeight))

        focusTransition.AnimatedVisibility(
            visible = { it },
            // expandVertically grows the LAYOUT space smoothly so the timer below
            // doesn't get pushed by an instant ~30dp jump when FOCUS appears.
            enter = (
                fadeIn(
                    animationSpec = tween(
                        durationMillis = FocusModeMotion.EnterDurationMs - 150,
                        delayMillis = 150,
                        easing = FocusModeMotion.EnterEasing
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = FocusModeMotion.EnterDurationMs - 150,
                        delayMillis = 150,
                        easing = FocusModeMotion.EnterEasing
                    )
                ) { -it / 2 } + expandVertically(
                    animationSpec = tween(
                        durationMillis = FocusModeMotion.EnterDurationMs,
                        easing = FocusModeMotion.EnterEasing
                    )
                )
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FocusModeMotion.ExitEasing)
            ) + shrinkVertically(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.ExitDurationMs,
                    easing = FocusModeMotion.ExitEasing
                )
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FOCUS",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        focusTransition.AnimatedVisibility(
            visible = { active -> !active },
            enter = if (initialized) fadeIn(
                animationSpec = tween(FocusModeMotion.ExitDurationMs, easing = FocusModeMotion.ExitEasing)
            ) + expandVertically(
                animationSpec = tween(FocusModeMotion.ExitDurationMs, easing = FocusModeMotion.ExitEasing)
            ) else EnterTransition.None,
            exit = fadeOut(
                animationSpec = tween(durationMillis = 350, easing = FocusModeMotion.EnterEasing)
            ) + shrinkVertically(
                animationSpec = tween(durationMillis = 350, easing = FocusModeMotion.EnterEasing)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            }
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = breathScale.value
                    scaleY = breathScale.value
                },
            textAlign = TextAlign.Center,
            text = formatSeconds(remainingSeconds),
            color = Color.White.copy(alpha = timerAlpha),
            fontSize = timerSize.sp,
            fontWeight = FontWeight(timerWeight),
            letterSpacing = timerLetterSpacing.sp
        )

        focusTransition.AnimatedVisibility(
            visible = { it && activeTask != null },
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.EnterDurationMs - 150,
                    delayMillis = 150,
                    easing = FocusModeMotion.EnterEasing
                )
            ) + expandVertically(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.EnterDurationMs - 100,
                    delayMillis = 100,
                    easing = FocusModeMotion.EnterEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FocusModeMotion.ExitEasing)
            ) + shrinkVertically(
                animationSpec = tween(durationMillis = 250, easing = FocusModeMotion.ExitEasing)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                activeTask?.let { task ->
                    Text(
                        text = task.title,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${task.completedSessions + 1} / ${task.plannedSessions}",
                        color = Color(0xFFFF8B6F).copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        focusTransition.AnimatedVisibility(
            visible = { active -> !active && isStarted && !isRunning },
            enter = fadeIn(animationSpec = tween(durationMillis = 250, easing = FocusModeMotion.ExitEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = FocusModeMotion.EnterEasing))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        }

        if (!focusActive) {
            Spacer(Modifier.height(24.dp))
        }

        focusTransition.AnimatedVisibility(
            visible = { active -> !active },
            enter = if (initialized) fadeIn(
                animationSpec = tween(
                    durationMillis = FocusModeMotion.ExitDurationMs,
                    easing = FocusModeMotion.ExitEasing
                )
            ) else EnterTransition.None,
            exit = fadeOut(
                animationSpec = tween(durationMillis = 300, easing = FocusModeMotion.EnterEasing)
            )
        ) {
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
                                .border(1.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
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
                                .border(1.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
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
        }

        Spacer(Modifier.height(16.dp))
        MusicButton(
            isPlaying = isSoundPlaying,
            onClick = onOpenSoundsSheet
        )

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
        focusTransition.AnimatedVisibility(
            visible = { active -> !active && isRunning },
            enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 100, easing = FocusModeMotion.ExitEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = FocusModeMotion.EnterEasing))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                Spacer(Modifier.height(40.dp))
            }
        }

        focusTransition.AnimatedVisibility(
            visible = { it },
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 350,
                    delayMillis = 350,
                    easing = FocusModeMotion.EnterEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FocusModeMotion.ExitEasing)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "tap anywhere to show controls",
                    color = Color.White.copy(alpha = 0.22f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(40.dp))
            }
        }
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

@Composable
private fun MusicButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "musicRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = if (isPlaying) 0.50f else 0.20f),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(if (isPlaying) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .graphicsLayer { if (isPlaying) rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u266B",
            color = Color.White.copy(alpha = if (isPlaying) 0.90f else 0.40f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
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
