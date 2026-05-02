package com.endiar.kuropomodoro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.endiar.kuropomodoro.ui.components.BottomTabBar
import com.endiar.kuropomodoro.ui.components.DecorativeBackground
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun KuroPomodoroApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Timer) }
    var timerMode by remember { mutableStateOf(TimerMode.Focus) }
    var settings by remember { mutableStateOf(TimerSettings()) }
    var remainingSeconds by remember { mutableIntStateOf(settings.secondsFor(timerMode)) }
    var isRunning by remember { mutableStateOf(false) }
    var completedSessions by remember { mutableIntStateOf(0) }
    var stamps by remember { mutableStateOf<Map<LocalDate, Int>>(emptyMap()) }
    var selectedSoundId by remember { mutableIntStateOf(1) }
    var isSoundPlaying by remember { mutableStateOf(false) }
    var premiumSheetMessage by remember { mutableStateOf<String?>(null) }
    var darkModePreview by remember { mutableStateOf(false) }
    var nextTaskId by remember { mutableIntStateOf(7) }
    var selectedTodoListId by remember { mutableIntStateOf(1) }
    var todoLists by remember {
        mutableStateOf(
            listOf(
                TodoListState(1, "Study", listOf(FocusTask(1, "Review Compose layout", 25), FocusTask(2, "Read chapter notes", 20))),
                TodoListState(2, "Home", listOf(FocusTask(3, "Clean desk", 10), FocusTask(4, "Water plants", 5))),
                TodoListState(3, "Ideas", listOf(FocusTask(5, "Sketch cat animation", 15), FocusTask(6, "Plan next sprint", 25)))
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

    LaunchedEffect(isRunning, remainingSeconds, timerMode) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (isRunning && remainingSeconds == 0) {
            isRunning = false
            if (timerMode == TimerMode.Focus) {
                completedSessions += 1
                stamps = addFocusStamp(stamps, LocalDate.now())
                timerMode = TimerMode.ShortBreak
            }
            remainingSeconds = settings.secondsFor(timerMode)
        }
    }

    DecorativeBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).padding(18.dp), contentAlignment = Alignment.Center) {
                Text("${selectedTab.label} screen coming next")
            }
            BottomTabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    }
}
