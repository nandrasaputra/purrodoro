package com.endiar.purrodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.purrodoro.R
import com.endiar.purrodoro.audio.SoundscapeLoopPlayer
import com.endiar.purrodoro.auth.AccountManager
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.ui.auth.rememberAccountSwitchGate
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.AppState
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.model.resolvedDisplayName
import com.endiar.purrodoro.model.AppClock
import com.endiar.purrodoro.model.AppTab
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.GlassTreatment
import com.endiar.purrodoro.model.PremiumOverride
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption
import com.endiar.purrodoro.model.TimerMode
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.glassTreatmentFor
import com.endiar.purrodoro.sync.LogoutDecision
import com.endiar.purrodoro.sync.SyncModule
import com.endiar.purrodoro.timer.TimerConnection
import com.endiar.purrodoro.timer.TimerNotification
import com.endiar.purrodoro.ui.CalendarScreen
import com.endiar.purrodoro.ui.CycleCompletionCopy
import com.endiar.purrodoro.ui.HabitsScreen
import com.endiar.purrodoro.ui.HabitDraft
import com.endiar.purrodoro.ui.HistoryScreen
import com.endiar.purrodoro.ui.HomeTimerScreen
import com.endiar.purrodoro.ui.ProfileScreen
import com.endiar.purrodoro.ui.SoundsScreen
import com.endiar.purrodoro.ui.SoundscapeSheetStickyFooter
import com.endiar.purrodoro.ui.soundscapeSavedMixLabel
import com.endiar.purrodoro.ui.StartFocusPrefill
import com.endiar.purrodoro.ui.PremiumUpsellSheet
import com.endiar.purrodoro.ui.PremiumUpsellTrigger
import com.endiar.purrodoro.ui.StartFocusSheet
import com.endiar.purrodoro.ui.TodoScreen
import com.endiar.purrodoro.ui.HomeSuggestion
import com.endiar.purrodoro.ui.homeSuggestion
import com.endiar.purrodoro.ui.interruptedRemainingMinutes
import com.endiar.purrodoro.ui.remainingFocusSessionsAfterCurrent
import com.endiar.purrodoro.ui.auth.LoginSheet
import com.endiar.purrodoro.ui.onboarding.OnboardingScreen
import com.endiar.purrodoro.ui.todayFocusMetrics
import com.endiar.purrodoro.model.InterruptedFocus
import com.endiar.purrodoro.model.effectiveTarget
import com.endiar.purrodoro.model.progressOn
import com.endiar.purrodoro.ui.components.CycleCompleteDialog
import com.endiar.purrodoro.ui.components.KuroFlatSurface
import com.endiar.purrodoro.ui.components.DashboardDock
import com.endiar.purrodoro.ui.components.SoundscapeDockMode
import com.endiar.purrodoro.ui.components.FocusModeMotion
import com.endiar.purrodoro.ui.components.FocusModeOverlay
import com.endiar.purrodoro.ui.components.SpotlightTutorialOverlay
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.components.glassTokens
import com.endiar.purrodoro.ui.theme.kuroTokens
import com.endiar.purrodoro.ui.tutorial.LocalTutorialController
import com.endiar.purrodoro.ui.tutorial.rememberTutorialController
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PurrodoroApp(
    repo: AppRepository,
    authRepository: AuthRepository,
    timerConnection: TimerConnection,
    requestNotificationPermission: () -> Unit,
    onEnableNotifications: () -> Unit = {},
    autoOpenStartFocus: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,
    openTab: kotlinx.coroutines.flow.SharedFlow<AppTab>? = null,
) {
    val authState by authRepository.authState.collectAsStateWithLifecycle(initialValue = AuthState.Guest)

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

    // Default to `true` so returning users never get a flash of onboarding while
    // DataStore loads. Brand-new users may briefly see Home before the real
    // `false` value arrives — acceptable for a one-time flow.
    val hasSeenOnboarding by repo.hasSeenOnboardingFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val hasSeenTutorial by repo.hasSeenTutorialFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val cycleCompletionVariant = remember(pendingCycleAck) {
        if (pendingCycleAck) CycleCompletionCopy.variants.random() else null
    }

    // Snapshot the runtime data once when the dialog first appears, so a
    // late `_state` clear from TimerService doesn't blank out the stats /
    // intent row while the dialog is on screen.
    var cycleCompleteSnapshot by remember { mutableStateOf<CycleCompleteSnapshot?>(null) }
    LaunchedEffect(pendingCycleAck) {
        if (!pendingCycleAck) cycleCompleteSnapshot = null
    }

    var settings by remember(appState.settings) { mutableStateOf(appState.settings) }
    var completedSessions by remember(appState.completedSessions) { mutableIntStateOf(appState.completedSessions) }
    var stamps by remember(appState.stamps) { mutableStateOf(appState.stamps) }
    var selectedSoundId by remember(appState.selectedSoundId) { mutableIntStateOf(appState.selectedSoundId) }
    var glassReadabilityMode by remember(appState.glassReadabilityMode) { mutableStateOf(appState.glassReadabilityMode) }
    var appearanceMode by remember(appState.appearanceMode) { mutableStateOf(appState.appearanceMode) }
    var language by remember(appState.language) { mutableStateOf(appState.language) }
    var userName by remember(appState.userName) { mutableStateOf(appState.userName) }
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
    var playingAmbienceId by rememberSaveable { mutableStateOf<Int?>(null) }
    var playingMusicId by rememberSaveable { mutableStateOf<Int?>(null) }
    var soundscapeMixAmbienceId by remember(appState.soundscapeMixAmbienceId) {
        mutableStateOf(appState.soundscapeMixAmbienceId)
    }
    var soundscapeMixMusicId by remember(appState.soundscapeMixMusicId) {
        mutableStateOf(appState.soundscapeMixMusicId)
    }
    var ambienceVolume by rememberSaveable { mutableStateOf(0.7f) }
    var musicVolume by rememberSaveable { mutableStateOf(0.45f) }
    var showPremiumSheet by rememberSaveable { mutableStateOf(false) }
    var premiumUpsellTrigger by remember { mutableStateOf(PremiumUpsellTrigger()) }
    var showSoundsSheet by rememberSaveable { mutableStateOf(false) }
    var hasEverUsedSoundscape by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(
        playingAmbienceId,
        playingMusicId,
        soundscapeMixAmbienceId,
        soundscapeMixMusicId,
    ) {
        if (playingAmbienceId != null || playingMusicId != null ||
            soundscapeMixAmbienceId != null || soundscapeMixMusicId != null
        ) {
            hasEverUsedSoundscape = true
        }
    }
    var showStartFocusSheet by rememberSaveable { mutableStateOf(false) }
    var startFocusPrefill by remember { mutableStateOf<StartFocusPrefill?>(null) }
    var activeTask by rememberSaveable(stateSaver = FocusTaskSaver) {
        mutableStateOf<FocusTask?>(
            null
        )
    }
    var isFocusMode by rememberSaveable { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var showLogoutWarning by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val tutorialController = rememberTutorialController()
    val accountManager = remember(repo) { AccountManager(repo, SyncModule.get(context).syncHooks) }
    val accountSwitchGate = rememberAccountSwitchGate()

    // Bridge from the Quick Start widget: when the host activity receives a
    // launch intent with EXTRA_AUTO_OPEN_START_FOCUS we surface the existing
    // Start Focus sheet automatically, but only when no cycle is in flight —
    // otherwise the widget's "Open Timer" semantic is preserved.
    if (autoOpenStartFocus != null) {
        LaunchedEffect(autoOpenStartFocus) {
            autoOpenStartFocus.collect {
                if (runtimeState == null) {
                    startFocusPrefill = null
                    showStartFocusSheet = true
                    selectedTab = AppTab.Timer
                }
            }
        }
    }

    // Bridge from the reactive 4x4 widgets: a habit/task nudge asks the app to
    // jump straight to the relevant tab. We close any transient profile overlay
    // so the requested tab is actually visible.
    if (openTab != null) {
        LaunchedEffect(openTab) {
            openTab.collect { tab ->
                showProfile = false
                selectedTab = tab
            }
        }
    }

    // First-run gate: replace the entire app surface with the onboarding flow
    // until the user finishes (or skips) it. Both completion paths just mark
    // the flag and drop the user on Home — they choose when to start their
    // first focus from the home screen's prominent CTA. Tapping "Turn on
    // notifications" additionally fires the Android system permission ask.
    if (!hasSeenOnboarding) {
        OnboardingScreen(
            authRepository = authRepository,
            accountManager = accountManager,
            confirmAccountSwitch = { accountSwitchGate.confirmDrop() },
            language = language,
            onUpdateLanguage = { v ->
                language = v
                scope.launch { repo.setLanguage(v) }
            },
            onSkip = { name ->
                scope.launch {
                    if (name.isNotBlank()) repo.setUserName(name)
                    repo.setHasSeenOnboarding(true)
                }
            },
            onAllowNotifications = { name ->
                scope.launch {
                    if (name.isNotBlank()) repo.setUserName(name)
                    repo.setHasSeenOnboarding(true)
                }
                requestNotificationPermission()
            },
            onPersistUserName = { name ->
                val trimmed = name.trim()
                userName = trimmed
                scope.launch { repo.setUserName(trimmed) }
            },
        )
        return
    }

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

    fun persistSoundscapeMix() {
        scope.launch {
            repo.setSoundscapeMix(soundscapeMixAmbienceId, soundscapeMixMusicId)
        }
    }

    fun persistGlassReadabilityMode(v: GlassReadabilityMode) {
        glassReadabilityMode = v; scope.launch { repo.setGlassReadabilityMode(v) }
    }

    fun persistAppearanceMode(v: AppearanceMode) {
        appearanceMode = v; scope.launch { repo.setAppearanceMode(v) }
    }

    fun persistLanguage(v: com.endiar.purrodoro.model.AppLanguage) {
        language = v; scope.launch { repo.setLanguage(v) }
    }

    fun persistUserName(v: String) {
        val trimmed = v.trim()
        userName = trimmed; scope.launch { repo.setUserName(trimmed) }
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

    fun createTask(
        title: String,
        categoryId: Int?,
        priority: com.endiar.purrodoro.model.TaskPriority,
        dueDate: LocalDate?,
    ): Int {
        val newId = nextTaskId
        val newTask = FocusTask(
            id = newId,
            title = title,
            minutes = settings.focusMinutes,
            categoryId = categoryId,
            priority = priority,
            dueDate = dueDate,
        )
        persistTasks(tasks + newTask)
        persistNextTaskId(nextTaskId + 1)
        return newId
    }
    val addTask: (String, Int?, com.endiar.purrodoro.model.TaskPriority, LocalDate?) -> Unit =
        { title, categoryId, priority, dueDate ->
            createTask(title, categoryId, priority, dueDate)
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
    val ambienceOptions = listOf(
        SoundOption(
            id = 1,
            name = stringResource(R.string.sound_rain_name),
            description = stringResource(R.string.sound_rain_desc),
            locked = false,
            symbol = "Rain",
            audioResId = R.raw.ambience_light_rain,
        ),
        SoundOption(
            id = 2,
            name = stringResource(R.string.sound_cafe_name),
            description = stringResource(R.string.sound_cafe_desc),
            locked = false,
            symbol = "Cafe",
            audioResId = R.raw.ambience_restaurant,
        ),
    )
    val musicOptions = listOf(
        MusicOption(
            id = 1,
            name = stringResource(R.string.music_bloom),
            artist = stringResource(R.string.music_bloom_artist),
            audioResId = R.raw.lofi_bloom_yasumu,
        ),
        MusicOption(
            id = 2,
            name = stringResource(R.string.music_chill_fm),
            artist = stringResource(R.string.music_chill_fm_artist),
            audioResId = R.raw.lofi_chill_fm_dope_cat,
        ),
        MusicOption(
            id = 3,
            name = stringResource(R.string.music_midnight_whisper),
            artist = stringResource(R.string.music_midnight_whisper_artist),
            audioResId = R.raw.lofi_midnight_whisper_matrika,
            locked = true,
        ),
    )

    fun isAmbienceInMix(id: Int): Boolean =
        ambienceOptions.any { it.id == id && (!it.locked || isPremium) }

    fun isMusicInMix(id: Int): Boolean =
        musicOptions.any { it.id == id && (!it.locked || isPremium) }

    val playingAmbienceName = playingAmbienceId?.let { id ->
        ambienceOptions.firstOrNull { it.id == id }?.name
    }
    val playingMusicName = playingMusicId?.let { id ->
        musicOptions.firstOrNull { it.id == id }?.name
    }
    val savedAmbienceName = soundscapeMixAmbienceId?.let { id ->
        ambienceOptions.firstOrNull { it.id == id }?.name
    }
    val savedMusicName = soundscapeMixMusicId?.let { id ->
        musicOptions.firstOrNull { it.id == id }?.name
    }
    val isSoundscapePlaying = playingAmbienceId != null || playingMusicId != null
    val soundscapeTransportLabel = when {
        !hasEverUsedSoundscape -> stringResource(R.string.soundscape_home_hint)
        isSoundscapePlaying -> soundscapeSavedMixLabel(playingAmbienceName, playingMusicName)
        else -> soundscapeSavedMixLabel(savedAmbienceName, savedMusicName)
            .ifBlank { stringResource(R.string.soundscape_home_hint) }
    }

    fun markSoundscapeUsed() {
        hasEverUsedSoundscape = true
    }

    fun resumeSoundscape() {
        markSoundscapeUsed()
        playingAmbienceId = soundscapeMixAmbienceId?.takeIf(::isAmbienceInMix)
        playingMusicId = soundscapeMixMusicId?.takeIf(::isMusicInMix)
    }

    fun pauseSoundscape() {
        playingAmbienceId = null
        playingMusicId = null
    }

    fun addAmbienceToMix(sound: SoundOption) {
        persistSelectedSoundId(sound.id)
        soundscapeMixAmbienceId = sound.id
        persistSoundscapeMix()
        playingAmbienceId = sound.id
        markSoundscapeUsed()
    }

    fun removeAmbienceFromMix() {
        soundscapeMixAmbienceId = null
        persistSoundscapeMix()
        playingAmbienceId = null
    }

    fun addMusicToMix(track: MusicOption) {
        soundscapeMixMusicId = track.id
        persistSoundscapeMix()
        playingMusicId = track.id
        markSoundscapeUsed()
    }

    fun removeMusicFromMix() {
        soundscapeMixMusicId = null
        persistSoundscapeMix()
        playingMusicId = null
    }

    val ambiencePlayer = remember { SoundscapeLoopPlayer(context) }
    val musicPlayer = remember { SoundscapeLoopPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            ambiencePlayer.release()
            musicPlayer.release()
        }
    }
    LaunchedEffect(playingAmbienceId, ambienceVolume, isPremium) {
        val soundId = playingAmbienceId
        if (soundId == null) {
            ambiencePlayer.pause()
            return@LaunchedEffect
        }
        val sound = ambienceOptions.firstOrNull { it.id == soundId } ?: run {
            ambiencePlayer.pause()
            return@LaunchedEffect
        }
        if (sound.locked && !isPremium) {
            ambiencePlayer.pause()
            return@LaunchedEffect
        }
        ambiencePlayer.play(sound.audioResId, ambienceVolume)
    }
    LaunchedEffect(playingMusicId, musicVolume, isPremium) {
        val trackId = playingMusicId
        if (trackId == null) {
            musicPlayer.pause()
            return@LaunchedEffect
        }
        val track = musicOptions.firstOrNull { it.id == trackId } ?: run {
            musicPlayer.pause()
            return@LaunchedEffect
        }
        if (track.locked && !isPremium) {
            musicPlayer.pause()
            return@LaunchedEffect
        }
        musicPlayer.play(track.audioResId, musicVolume)
    }

    // --- Derive timer state from the service ---
    val isRunning = runtimeState?.session?.timerState == TimerState.Playing
    val isStarted = runtimeState != null && runtimeState?.session?.timerState != TimerState.Idle

    // Focus mode is sticky for the duration of a session — running OR paused —
    // so the user can "continue from focus screen" when auto-start-break is off
    // and the timer pauses between segments. We only drop focus mode when the
    // session truly tears down (Idle / null runtime), or when the user
    // explicitly exits (tap-to-exit, Stop, switch tabs from a sheet, etc.).
    // Also covers the brief async window before the runtime arrives so a cold
    // restart into focus mode snaps cleanly to the final state.
    val focusActive = isFocusMode && (isStarted || runtimeState == null)
    val focusTransition = updateTransition(targetState = focusActive, label = "focusMode")

    val soundscapeDockMode = when {
        selectedTab != AppTab.Timer || focusActive -> SoundscapeDockMode.Hidden
        else -> SoundscapeDockMode.Transport
    }

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
    val homeGreeting = com.endiar.purrodoro.ui.rememberHomeGreeting(backdropTime)
    val homeDisplayName = resolvedDisplayName(authState, userName)
    val homeMetrics = todayFocusMetrics(
        sessionRecords = sessionRecords,
        today = LocalDate.now()
    )
    // What the Home card should offer when no cycle is running: resume an
    // interrupted task/habit, else suggest one thing to do (stable per day so
    // the pick doesn't flicker across recompositions).
    val today = LocalDate.now()
    val homeSuggestion = homeSuggestion(
        interrupted = appState.interruptedFocus,
        tasks = tasks,
        habits = appState.habits,
        today = today,
        seed = today.toEpochDay(),
    )
    val glassTreatment = glassTreatmentFor(glassReadabilityMode, backdropTime)

    val overBackdrop = false

    // Hoisted so it survives surface recomposition on tab switches. Otherwise
    // it resets to 0 on every return to the Timer tab, which makes
    // BoxWithConstraints.maxHeight grow then shrink, causing the
    // animateDp-driven topSpacer to slide the cluster upward on re-entry.
    var dockHeightPx by remember { mutableIntStateOf(0) }

    val surfaceContent: @Composable BoxScope.() -> Unit = {
        val density = LocalDensity.current
        val dockHeightDp = with(density) { dockHeightPx.toDp() }
        val screenContentPadding = PaddingValues(bottom = dockHeightDp)

        Box(modifier = Modifier.fillMaxSize()) {
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
                // Crossfade tab content so switching tabs dissolves rather than
                // hard-cutting — a calm, Premium shared-surface feel that matches
                // the rest of the app's motion language.
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 280),
                    label = "tabContentSwap",
                ) { tab ->
                when (tab) {
                    AppTab.Timer -> HomeTimerScreen(
                        timerMode = timerMode,
                        remainingSeconds = remainingSeconds,
                        isRunning = isRunning,
                        isStarted = isStarted,
                        // Prefer the service-restored task so the active hero
                        // (title + "Purrodoro ticks it off" tip) keeps showing the
                        // real focus after a cold restart, when the local
                        // rememberSaveable activeTask has been lost. Mirrors the
                        // FocusModeOverlay below.
                        activeTask = runtimeState?.activeTask ?: activeTask,
                        suggestion = homeSuggestion,
                        suggestionFocusMinutes = settings.focusMinutes,
                        greeting = homeGreeting,
                        userName = homeDisplayName,
                        todayMetrics = homeMetrics,
                        cycleItems = cycleItems,
                        cycleIndex = cycleIndex,
                        currentTimerState = currentTimerState,
                        timerSettings = settings,
                        backdropTime = backdropTime,
                        onSkip = { timerConnection.controller.skip() },
                        onEnterFocusMode = { isFocusMode = true },
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
                            startFocusPrefill = when (val s = homeSuggestion) {
                                is HomeSuggestion.Continue -> {
                                    val mins = if (s.remainingSeconds > 0) {
                                        interruptedRemainingMinutes(s.remainingSeconds)
                                    } else {
                                        s.focusMinutes ?: settings.focusMinutes
                                    }
                                    val sessions = s.remainingFocusSessions
                                        .takeIf { s.remainingSeconds <= 0 && it > 0 }
                                    if (s.focusType == FocusType.Habit) {
                                        s.habitId?.let {
                                            StartFocusPrefill.Habit(
                                                habitId = it,
                                                focusMinutesOverride = mins,
                                                focusSessionsOverride = sessions,
                                            )
                                        }
                                    } else {
                                        s.taskId?.let {
                                            StartFocusPrefill.Task(
                                                taskId = it,
                                                focusMinutesOverride = mins,
                                                focusSessionsOverride = sessions,
                                            )
                                        }
                                    }
                                }
                                is HomeSuggestion.Suggest -> {
                                    if (s.focusType == FocusType.Habit) {
                                        s.habitId?.let { StartFocusPrefill.Habit(it) }
                                    } else {
                                        s.taskId?.let { StartFocusPrefill.Task(it) }
                                    }
                                }
                                HomeSuggestion.Empty -> null
                            }
                            showStartFocusSheet = true
                        },
                        onStop = {
                            // Remember unfinished task/habit focus so Home can
                            // offer to continue it. Mid-focus stops preserve the
                            // leftover minutes; break stops preserve the number
                            // of future focus sessions in the block.
                            val rt = runtimeState
                            val futureFocusSessions = rt?.session?.let {
                                remainingFocusSessionsAfterCurrent(it.items, it.currentIndex)
                            } ?: 0
                            val activeFocusMinutes = rt?.session?.items
                                ?.mapNotNull { (it.type as? SessionItemType.Focus)?.seconds }
                                ?.firstOrNull()
                                ?.let { (it + 59) / 60 }
                                ?: settings.focusMinutes
                            val interrupted: InterruptedFocus? = when {
                                rt == null -> null
                                rt.focusType == FocusType.Task && (rt.activeTask?.id ?: 0) > 0 ->
                                    when {
                                        currentItemType is SessionItemType.Focus && remainingSeconds > 0 ->
                                            InterruptedFocus(
                                                focusType = FocusType.Task,
                                                taskId = rt.activeTask?.id,
                                                habitId = null,
                                                title = rt.activeTask?.title ?: "",
                                                remainingSeconds = remainingSeconds,
                                            )
                                        currentItemType !is SessionItemType.Focus && futureFocusSessions > 0 ->
                                            InterruptedFocus(
                                                focusType = FocusType.Task,
                                                taskId = rt.activeTask?.id,
                                                habitId = null,
                                                title = rt.activeTask?.title ?: "",
                                                remainingSeconds = 0,
                                                remainingFocusSessions = futureFocusSessions,
                                                focusMinutes = activeFocusMinutes,
                                            )
                                        else -> null
                                    }
                                rt.focusType == FocusType.Habit && rt.activeHabitId != null ->
                                    when {
                                        currentItemType is SessionItemType.Focus && remainingSeconds > 0 ->
                                            InterruptedFocus(
                                                focusType = FocusType.Habit,
                                                taskId = null,
                                                habitId = rt.activeHabitId,
                                                title = rt.activeTask?.title ?: "",
                                                remainingSeconds = remainingSeconds,
                                            )
                                        currentItemType !is SessionItemType.Focus && futureFocusSessions > 0 ->
                                            InterruptedFocus(
                                                focusType = FocusType.Habit,
                                                taskId = null,
                                                habitId = rt.activeHabitId,
                                                title = rt.activeTask?.title ?: "",
                                                remainingSeconds = 0,
                                                remainingFocusSessions = futureFocusSessions,
                                                focusMinutes = activeFocusMinutes,
                                            )
                                        else -> null
                                    }
                                else -> null
                            }
                            scope.launch { repo.setInterruptedFocus(interrupted) }
                            timerConnection.controller.stop()
                            isFocusMode = false
                            activeTask = null
                        },
                        onOpenProfile = { showProfile = true },
                        onOpenSoundscape = { showSoundsSheet = true },
                        onMuteSoundscape = { pauseSoundscape() },
                        playingAmbienceName = playingAmbienceName,
                        playingMusicName = playingMusicName,
                        ambienceVolume = ambienceVolume,
                        musicVolume = musicVolume,
                        contentPadding = screenContentPadding,
                        activeFocusType = runtimeState?.focusType ?: FocusType.Empty,
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
                        onAddCategory = { name, color ->
                            scope.launch { repo.addCategory(name, color) }
                        },
                        onRenameCategory = { id, name, color ->
                            scope.launch { repo.updateCategory(id, name, color) }
                        },
                        onDeleteCategory = { id -> scope.launch { repo.deleteCategory(id) } },
                        onFocusTask = { request ->
                            activeTask = request.task
                            // Land on the immersive focus screen (not the Home
                            // dashboard) so task and habit starts behave the same:
                            // "you pressed start, now focus."
                            isFocusMode = true
                            selectedTab = AppTab.Timer
                            requestNotificationPermission()
                            scope.launch { repo.setInterruptedFocus(null) }
                            timerConnection.controller.start(request.settings, request.task, request.habitId, request.focusType)
                        },
                        habits = appState.habits,
                        settings = settings,
                        glassTreatment = glassTreatment,
                        contentPadding = screenContentPadding,
                        overBackdrop = overBackdrop,
                        // Quiet "this task is currently focused" indicator on the
                        // Tasks list. Synthetic habit tasks have negative ids so
                        // they intentionally never match a real FocusTask row here.
                        liveTaskId = runtimeState?.activeTask?.id?.takeIf { it > 0 },
                        // Hero's "Open" action when a cycle is live jumps to the
                        // Home tab so the user can see / control the running
                        // timer instead of re-opening the configure sheet.
                        onSwitchToHome = { selectedTab = AppTab.Timer },
                        // Blocks "Focus with Purrodoro" / swipe-to-focus from a
                        // second task while any Pomodoro (task or habit) is
                        // already running or paused.
                        isFocusActive = isStarted,
                    )

                    AppTab.Calendar -> CalendarScreen(
                        sessionRecords = sessionRecords,
                        onOpenHistory = { showHistory = true },
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
                            // Mirror the Tasks flow: open the Start Focus
                            // confirmation sheet prefilled with this habit
                            // instead of starting the timer immediately. The
                            // sheet shows in place (over the Habits tab) and its
                            // confirm path owns both the start AND the navigation
                            // to Home, so cancelling leaves the user where they were.
                            startFocusPrefill = StartFocusPrefill.Habit(habit.id)
                            showStartFocusSheet = true
                        },
                        contentPadding = screenContentPadding,
                        // Same focus gating as Tasks: block starting a focus on a
                        // habit while another Pomodoro is running, and let the
                        // live habit jump back to Home instead.
                        isFocusActive = isStarted,
                        liveHabitId = runtimeState?.activeHabitId
                            ?.takeIf { runtimeState?.focusType == FocusType.Habit },
                        onSwitchToHome = { selectedTab = AppTab.Timer },
                    )
                }
                }
            }

            // Full-screen Minimal Zen focus surface sits above the dashboard
            // but below the dock / dialogs / sheets. Only the Timer tab can
            // legitimately host focus mode; gating here prevents the cream
            // page from leaking onto other tabs in any edge case where focus
            // state lingers across a tab switch.
            //
            // Pause/skip wired here directly (not through the dashboard's
            // onStartPause/onStop) so they don't clear `isFocusMode` —
            // staying inside the focus surface is exactly the point of
            // "continue from focus screen" while a break is paused.
            if (selectedTab == AppTab.Timer) {
                FocusModeOverlay(
                    focusActive = focusActive,
                    focusTransition = focusTransition,
                    timerMode = timerMode,
                    remainingSeconds = remainingSeconds,
                    activeFocusType = runtimeState?.focusType ?: FocusType.Empty,
                    activeTask = runtimeState?.activeTask ?: activeTask,
                    cycleItems = cycleItems,
                    cycleIndex = cycleIndex,
                    currentTimerState = currentTimerState,
                    isRunning = isRunning,
                    onExit = { isFocusMode = false },
                    onStartPause = {
                        if (isRunning) timerConnection.controller.pause()
                        else timerConnection.controller.resume()
                    },
                    onSkip = { timerConnection.controller.skip() },
                    isSoundscapePlaying = isSoundscapePlaying,
                    soundscapeDisplayName = soundscapeTransportLabel,
                    showSoundscapeFirstRunHint = !hasEverUsedSoundscape,
                    onOpenSoundscape = { showSoundsSheet = true },
                    onSoundscapePlayPause = {
                        if (isSoundscapePlaying) pauseSoundscape() else resumeSoundscape()
                    },
                    contentPadding = PaddingValues(),
                )
            }

            if (showStartFocusSheet) {
                key(startFocusPrefill) {
                    StartFocusSheet(
                        settings = settings,
                        tasks = tasks,
                        habits = appState.habits,
                        today = LocalDate.now(),
                        prefill = startFocusPrefill,
                        categories = categories,
                        defaultCategoryId = selectedCategoryId,
                        onAddTask = { title, categoryId, priority, dueDate ->
                            createTask(title, categoryId, priority, dueDate)
                        },
                        onAddHabit = { draft ->
                            repo.addHabit(
                                name = draft.name,
                                emoji = draft.emoji,
                                colorArgb = draft.colorArgb,
                                occurrence = draft.occurrence,
                                timeBlock = draft.timeBlock,
                            )
                        },
                        onDismiss = {
                            showStartFocusSheet = false
                            startFocusPrefill = null
                        },
                        onStartFocus = { request ->
                            showStartFocusSheet = false
                            startFocusPrefill = null
                            requestNotificationPermission()
                            // Always land on Home's immersive focus screen on
                            // confirm. Home-originated callers are already on the
                            // Timer tab; the Habits-originated caller navigates here.
                            selectedTab = AppTab.Timer
                            isFocusMode = true
                            activeTask = request.task
                            scope.launch { repo.setInterruptedFocus(null) }
                            timerConnection.controller.start(request.settings, request.task, request.habitId, request.focusType)
                        },
                    )
                }
            }

            if (showHistory) {
                HistoryScreen(
                    sessionRecords = sessionRecords,
                    onBack = { showHistory = false },
                    onDeleteRecord = { id -> scope.launch { repo.deleteSessionRecord(id) } },
                )
            }

            if (showProfile) {
                ProfileScreen(
                    settings = settings,
                    onBack = { showProfile = false },
                    onUpdateSettings = { persistSettings(it) },
                    notificationSoundEnabled = notificationSoundEnabled,
                    onUpdateNotificationSound = { persistNotificationSoundEnabled(it) },
                    appearanceMode = appearanceMode,
                    onUpdateAppearanceMode = { persistAppearanceMode(it) },
                    language = language,
                    onUpdateLanguage = { persistLanguage(it) },
                    userName = userName,
                    onUpdateUserName = { persistUserName(it) },
                    onRequestEnableNotifications = onEnableNotifications,
                    authState = authState,
                    onRequestLogin = { showLoginSheet = true },
                    onSignOut = {
                        scope.launch {
                            // Flush local changes first; only warn when the
                            // flush failed AND unsynced rows remain.
                            when (SyncModule.get(context).flushBeforeLogout()) {
                                LogoutDecision.SignOut -> authRepository.signOut()
                                LogoutDecision.WarnUnsynced -> showLogoutWarning = true
                            }
                        }
                    },
                )
            }

            if (showLogoutWarning) {
                AlertDialog(
                    onDismissRequest = { showLogoutWarning = false },
                    title = { Text(stringResource(R.string.logout_unsynced_title)) },
                    text = { Text(stringResource(R.string.logout_unsynced_body)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showLogoutWarning = false
                            scope.launch { authRepository.signOut() }
                        }) {
                            Text(stringResource(R.string.logout_unsynced_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutWarning = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    },
                )
            }

            if (showLoginSheet) {
                LoginSheet(
                    authRepository = authRepository,
                    onDismiss = { showLoginSheet = false },
                    onSignedIn = { user ->
                        scope.launch {
                            val result = accountManager.onSignedIn(
                                incomingUserId = user.userId,
                                confirmDrop = { accountSwitchGate.confirmDrop() },
                                onBeforeClear = { timerConnection.controller.stop() },
                            )
                            when (result) {
                                AccountManager.Result.DropCancelled ->
                                    // User refused to clear the other account's data;
                                    // undo the sign-in so a session never outlives a
                                    // different account's on-device data.
                                    authRepository.signOut()
                                else ->
                                    user.displayName?.trim()?.takeIf { it.isNotBlank() }
                                        ?.let { persistUserName(it) }
                            }
                            showLoginSheet = false
                        }
                    },
                    onContinueAsGuest = { showLoginSheet = false },
                    fallbackDisplayName = userName,
                )
            }

            // Child of focusTransition so it shares the single clock with scrim + timer
            // (frame-perfect parallel). Dock visibility is the inverse of focus mode:
            // appearing = exiting focus (Exit, 400ms, snappier); disappearing = entering focus (Enter, 700ms, deliberate).
            focusTransition.AnimatedVisibility(
                visible = { active -> !active && !showProfile && !showHistory },
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
                    soundscapeMode = soundscapeDockMode,
                    isSoundscapePlaying = isSoundscapePlaying,
                    soundscapeDisplayName = soundscapeTransportLabel,
                    showSoundscapeFirstRunHint = !hasEverUsedSoundscape,
                    onOpenSoundscape = { showSoundsSheet = true },
                    onSoundscapePlayPause = {
                        if (isSoundscapePlaying) pauseSoundscape() else resumeSoundscape()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .onSizeChanged { dockHeightPx = it.height }
                )
            }

            val tutorialVisible = !hasSeenTutorial &&
                !isStarted &&
                selectedTab == AppTab.Timer &&
                !showStartFocusSheet &&
                !showSoundsSheet &&
                !showProfile &&
                !pendingCycleAck
            LaunchedEffect(tutorialVisible) {
                if (tutorialVisible && !tutorialController.visible) {
                    tutorialController.start()
                }
            }
            if (tutorialVisible) {
                val currentStep = tutorialController.currentStep
                val targetBounds = currentStep?.let { tutorialController.boundsFor(it.target) }

                if (currentStep != null && targetBounds != null && tutorialController.visible) {
                    SpotlightTutorialOverlay(
                        targetBounds = targetBounds,
                        title = stringResource(currentStep.titleRes),
                        body = stringResource(currentStep.bodyRes),
                        stepIndex = tutorialController.stepIndex,
                        totalSteps = tutorialController.steps.size,
                        skipLabel = stringResource(R.string.tutorial_skip),
                        previousLabel = if (tutorialController.canGoBack) {
                            stringResource(R.string.tutorial_previous)
                        } else {
                            null
                        },
                        nextLabel = stringResource(
                            if (tutorialController.isLastStep) R.string.tutorial_done else R.string.tutorial_next
                        ),
                        placement = currentStep.placement,
                        spotlightPadding = currentStep.spotlightPadding,
                        spotlightCornerRadius = currentStep.spotlightCornerRadius,
                        onSkip = {
                            tutorialController.skip {
                                scope.launch { repo.setHasSeenTutorial(true) }
                            }
                        },
                        onPrevious = { tutorialController.previous() },
                        onNext = {
                            tutorialController.next {
                                scope.launch { repo.setHasSeenTutorial(true) }
                            }
                        },
                    )
                }
            }

            if (pendingCycleAck && cycleCompletionVariant != null) {
                val snapshot = cycleCompleteSnapshot ?: deriveCycleCompleteSnapshot(
                    runtimeState = runtimeState,
                    settings = settings,
                ).also { cycleCompleteSnapshot = it }

                CycleCompleteDialog(
                    title = androidx.compose.ui.res.stringResource(cycleCompletionVariant.titleRes),
                    body = androidx.compose.ui.res.stringResource(cycleCompletionVariant.bodyRes),
                    focusMinutesTotal = snapshot.focusMinutesTotal,
                    focusSessionsCompleted = snapshot.focusSessionsCompleted,
                    focusType = snapshot.focusType,
                    intentEmoji = snapshot.intentEmoji,
                    intentLabel = snapshot.intentLabel,
                    confirmLabel = androidx.compose.ui.res.stringResource(CycleCompletionCopy.buttonLabelRes),
                    secondaryLabel = androidx.compose.ui.res.stringResource(R.string.cycle_view_journey),
                    onConfirm = {
                        scope.launch { repo.setPendingCycleCompletionAck(false) }
                    },
                    onSecondary = {
                        scope.launch { repo.setPendingCycleCompletionAck(false) }
                        selectedTab = AppTab.Calendar
                    },
                )
            }

            if (showPremiumSheet && !isPremium) {
                PremiumUpsellSheet(
                    trigger = premiumUpsellTrigger,
                    onDismiss = { showPremiumSheet = false },
                    onUpgrade = {
                        scope.launch {
                            repo.setIsPremium(true)
                            showPremiumSheet = false
                        }
                    },
                )
            }

            if (showSoundsSheet) {
                KuroAdaptiveBottomSheet(
                    onDismiss = {
                        showSoundsSheet = false
                    },
                    horizontalPadding = 0.dp,
                    showCloseButton = true,
                    footer = { scrollOverlay ->
                        SoundscapeSheetStickyFooter(
                            ambienceVolume = ambienceVolume,
                            musicVolume = musicVolume,
                            onAmbienceVolumeChange = { ambienceVolume = it },
                            onMusicVolumeChange = { musicVolume = it },
                            ambienceVolumeEnabled = soundscapeMixAmbienceId != null,
                            musicVolumeEnabled = soundscapeMixMusicId != null,
                            isPlaying = isSoundscapePlaying,
                            onPlayPause = {
                                if (isSoundscapePlaying) pauseSoundscape() else resumeSoundscape()
                            },
                            scrollOverlay = scrollOverlay,
                        )
                    },
                ) {
                    // Lo-fi + ambience play from bundled res/raw (MP3 loops).
                    SoundsScreen(
                        ambienceOptions = ambienceOptions,
                        musicOptions = musicOptions,
                        selectedAmbienceId = soundscapeMixAmbienceId ?: selectedSoundId,
                        playingAmbienceId = playingAmbienceId,
                        selectedMusicId = soundscapeMixMusicId,
                        playingMusicId = playingMusicId,
                        onAmbienceSelected = { sound ->
                            if (sound.locked && !isPremium) {
                                premiumUpsellTrigger = PremiumUpsellTrigger(lockedFeatureName = sound.name)
                                showPremiumSheet = true
                            } else {
                                addAmbienceToMix(sound)
                            }
                        },
                        onAmbienceToggle = {
                            removeAmbienceFromMix()
                        },
                        onMusicSelected = { track ->
                            if (track.locked && !isPremium) {
                                premiumUpsellTrigger = PremiumUpsellTrigger(lockedFeatureName = track.name)
                                showPremiumSheet = true
                            } else {
                                addMusicToMix(track)
                            }
                        },
                        onMusicToggle = {
                            removeMusicFromMix()
                        },
                        glassTreatment = GlassTreatment.Clear,
                        overBackdrop = false,
                    )
                }
            }

        }
    }

    CompositionLocalProvider(LocalTutorialController provides tutorialController) {
        KuroFlatSurface(content = surfaceContent)
    }
}

private val AppTabSaver = Saver<AppTab, String>(
    save = { it.name },
    restore = { name -> AppTab.entries.firstOrNull { it.name == name } ?: AppTab.Timer }
)

private data class CycleCompleteSnapshot(
    val focusMinutesTotal: Int,
    val focusSessionsCompleted: Int,
    val focusType: FocusType,
    val intentEmoji: String?,
    val intentLabel: String?,
)

/**
 * Build a snapshot of the just-finished cycle for the completion dialog.
 *
 * Prefers the (still-preserved) runtime cycleItems for stats since they
 * reflect what was actually run; falls back to current [TimerSettings]
 * when the runtime has already been cleared (e.g. after a process kill).
 *
 * Intent label is only populated for non-Empty FocusType + non-null task.
 * For Habit, the leading emoji is split out of the title so the receipt
 * can show it as an icon instead of repeating it inline.
 */
private fun deriveCycleCompleteSnapshot(
    runtimeState: com.endiar.purrodoro.model.TimerRuntimeState?,
    settings: TimerSettings,
): CycleCompleteSnapshot {
    val focusType = runtimeState?.focusType ?: FocusType.Empty
    val task = runtimeState?.activeTask
    val items = runtimeState?.session?.items.orEmpty()

    val runtimeFocusCount = items.count { it.type is SessionItemType.Focus }
    val runtimeFocusSeconds = items
        .mapNotNull { (it.type as? SessionItemType.Focus)?.seconds }
        .sum()

    val focusSessionsCompleted = if (runtimeFocusCount > 0) {
        runtimeFocusCount
    } else {
        settings.focusSessionsPerCycle.coerceAtLeast(1)
    }
    val focusMinutesTotal = if (runtimeFocusSeconds > 0) {
        runtimeFocusSeconds / 60
    } else {
        settings.focusMinutes * focusSessionsCompleted
    }

    val (intentEmoji, intentLabel) = when {
        focusType == FocusType.Empty || task == null -> null to null
        focusType == FocusType.Habit -> {
            val parts = task.title.split(" ", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null to task.title
        }
        else -> null to task.title
    }

    return CycleCompleteSnapshot(
        focusMinutesTotal = focusMinutesTotal,
        focusSessionsCompleted = focusSessionsCompleted,
        focusType = focusType,
        intentEmoji = intentEmoji,
        intentLabel = intentLabel,
    )
}

private val FocusTaskSaver = Saver<FocusTask?, List<Any?>>(
    save = { task ->
        if (task == null) listOf(null)
        else listOf(
            task.id,
            task.title,
            task.minutes,
            task.completed,
            task.plannedSessions,
            task.completedSessions,
            task.priority.name,
            task.dueDate?.toString()
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
            completedSessions = (list[5] as Number).toInt(),
            priority = (list.getOrNull(6) as? String)
                ?.let { runCatching { com.endiar.purrodoro.model.TaskPriority.valueOf(it) }.getOrNull() }
                ?: com.endiar.purrodoro.model.TaskPriority.Normal,
            dueDate = (list.getOrNull(7) as? String)
                ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
        )
    }
)

