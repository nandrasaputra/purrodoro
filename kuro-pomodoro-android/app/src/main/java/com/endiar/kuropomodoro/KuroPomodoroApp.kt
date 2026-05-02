package com.endiar.kuropomodoro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.model.addFocusStamp
import com.endiar.kuropomodoro.ui.TimerScreen
import com.endiar.kuropomodoro.ui.components.BottomTabBar
import com.endiar.kuropomodoro.ui.components.DecorativeBackground
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun KuroPomodoroApp() {
    var selectedTab by rememberSaveable(stateSaver = AppTabSaver) { mutableStateOf(AppTab.Timer) }
    var timerMode by rememberSaveable(stateSaver = TimerModeSaver) { mutableStateOf(TimerMode.Focus) }
    var settings by rememberSaveable(stateSaver = TimerSettingsSaver) { mutableStateOf(TimerSettings()) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(settings.secondsFor(timerMode)) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var completedSessions by rememberSaveable { mutableIntStateOf(0) }
    var stamps by rememberSaveable(stateSaver = StampsSaver) { mutableStateOf<Map<LocalDate, Int>>(emptyMap()) }
    var selectedSoundId by rememberSaveable { mutableIntStateOf(1) }
    var isSoundPlaying by rememberSaveable { mutableStateOf(false) }
    var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var darkModePreview by rememberSaveable { mutableStateOf(false) }
    var profileExpanded by rememberSaveable { mutableStateOf(false) }
    var settingsExpanded by rememberSaveable { mutableStateOf(false) }
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

    DecorativeBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (selectedTab) {
                    AppTab.Timer -> TimerScreen(
                        mode = timerMode,
                        remainingSeconds = remainingSeconds,
                        totalSeconds = settings.secondsFor(timerMode),
                        isRunning = isRunning,
                        completedSessionsToday = stamps[LocalDate.now()] ?: 0,
                        currentTask = todoLists.firstOrNull { it.id == selectedTodoListId }?.tasks?.firstOrNull { !it.completed },
                        onModeSelected = { selectedMode ->
                            timerMode = selectedMode
                            isRunning = false
                            remainingSeconds = settings.secondsFor(selectedMode)
                        },
                        onStartPause = { isRunning = !isRunning },
                        onReset = {
                            isRunning = false
                            remainingSeconds = settings.secondsFor(timerMode)
                        }
                    )
                    else -> Text(
                        text = "${selectedTab.label} screen coming next",
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
            BottomTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.navigationBarsPadding()
            )
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
