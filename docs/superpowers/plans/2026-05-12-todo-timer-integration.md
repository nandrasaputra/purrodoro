# Todo ↔ Timer Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the Todo screen to the Focus timer so users can start a Pomodoro session from a task via leading swipe, with auto task completion when planned sessions finish.

**Architecture:** All state lives as `rememberSaveable` in `PurrodoroApp`. `FocusTask` gains two new fields (`plannedSessions`, `pomodorosDone`). `TodoScreen`/`TaskGlassRow` gets a `SwipeToDismiss` leading-swipe + pre-session `ModalBottomSheet`. The Focus Mode fullscreen overlay and Running-state controls are added to `FocusDashboard` inside `PurrodoroApp.kt`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (`SwipeToDismiss`, `ModalBottomSheet`), no persistence changes needed.

---

## File Map

| File | Change |
|---|---|
| `model/KuroModels.kt` | Add `plannedSessions: Int = 1` and `pomodorosDone: Int = 0` to `FocusTask` |
| `PurrodoroApp.kt` | Add `activeTask`, `isFocusMode` state; session-complete logic; `FocusDashboard` running-state redesign; `FocusModeScreen` fullscreen overlay; pass `onFocusTask` callback to `TodoScreen` |
| `ui/TodoScreen.kt` | Add `onFocusTask` parameter to `TodoScreen`; add `SwipeToDismiss` leading/trailing swipe to `TaskGlassRow`; add `PreSessionSheet` `ModalBottomSheet` composable |

---

## Task 1: Extend FocusTask data model

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt:23-30`

- [ ] **Step 1: Add fields to FocusTask**

Replace lines 23-30 in `KuroModels.kt`:

```kotlin
data class FocusTask(
    val id: Int,
    val title: String,
    val minutes: Int,
    val completed: Boolean = false,
    val plannedSessions: Int = 1,
    val pomodorosDone: Int = 0
) {
    fun toggleCompleted(): FocusTask = copy(completed = !completed)
}
```

- [ ] **Step 2: Build and verify no compile errors**

```bash
cd purrodoro-android
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt
git commit -m "feat: add plannedSessions and pomodorosDone to FocusTask"
```

---

## Task 2: Add activeTask and isFocusMode state to PurrodoroApp

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt:116-123`

- [ ] **Step 1: Add state variables after line 123 (after `premiumSheetMessage`)**

In `PurrodoroApp.kt`, add these two lines after `var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }` (line 123):

```kotlin
var activeTask by rememberSaveable(stateSaver = FocusTaskSaver) { mutableStateOf<FocusTask?>(null) }
var isFocusMode by rememberSaveable { mutableStateOf(false) }
```

- [ ] **Step 2: Add FocusTaskSaver at the bottom of the file (after `TimerModeSaver`)**

Add after line 481 (after the `TimerModeSaver` block):

```kotlin
private val FocusTaskSaver = Saver<FocusTask?, List<Any?>>(
    save = { task ->
        if (task == null) listOf(null)
        else listOf(task.id, task.title, task.minutes, task.completed, task.plannedSessions, task.pomodorosDone)
    },
    restore = { list ->
        if (list[0] == null) null
        else FocusTask(
            id = list[0] as Int,
            title = list[1] as String,
            minutes = list[2] as Int,
            completed = list[3] as Boolean,
            plannedSessions = list[4] as Int,
            pomodorosDone = list[5] as Int
        )
    }
)
```

- [ ] **Step 3: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: add activeTask and isFocusMode state to PurrodoroApp"
```

---

## Task 3: Wire onFocusTask callback through TodoScreen

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/TodoScreen.kt:53-63`
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt:250-258`

- [ ] **Step 1: Add onFocusTask parameter to TodoScreen signature**

In `TodoScreen.kt`, replace lines 53-63:

```kotlin
@Composable
fun TodoScreen(
    todoLists: List<TodoListState>,
    selectedListId: Int,
    onListSelected: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
```

- [ ] **Step 2: Pass onFocusTask down to TaskGlassRow call sites**

In `TodoScreen.kt`, replace the `TaskGlassRow` call (lines 211-218):

```kotlin
TaskGlassRow(
    task = task,
    active = task == firstActiveTask,
    onToggleTask = onToggleTask,
    onDeleteTask = onDeleteTask,
    onFocusTask = onFocusTask,
    glassTreatment = glassTreatment
)
```

- [ ] **Step 3: Add onFocusTask parameter to TaskGlassRow**

In `TodoScreen.kt`, replace lines 224-230 (the `TaskGlassRow` function signature):

```kotlin
@Composable
private fun TaskGlassRow(
    task: FocusTask,
    active: Boolean,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
```

- [ ] **Step 4: Pass onFocusTask from PurrodoroApp to TodoScreen**

In `PurrodoroApp.kt`, replace the `AppTab.Todo` branch (lines 250-258):

```kotlin
AppTab.Todo -> TodoScreen(
    todoLists = todoLists,
    selectedListId = selectedTodoListId,
    onListSelected = { persistSelectedTodoListId(it) },
    onAddTask = addTask,
    onToggleTask = toggleTask,
    onDeleteTask = deleteTask,
    onFocusTask = { task ->
        // Pre-session sheet is shown inside TodoScreen; this callback
        // is invoked with the updated task (plannedSessions set) after the
        // user taps "Start Focus" in the sheet.
        activeTask = task
        isFocusMode = true
        selectedTab = AppTab.Timer
        isRunning = true
    },
    glassTreatment = glassTreatment
)
```

- [ ] **Step 5: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/TodoScreen.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: wire onFocusTask callback through TodoScreen to PurrodoroApp"
```

---

## Task 4: Add Pre-Session ModalBottomSheet to TodoScreen

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/TodoScreen.kt`

This task adds a `PreSessionSheet` composable and opens it when `TaskGlassRow` triggers `onFocusTask`.

- [ ] **Step 1: Add required imports to TodoScreen.kt**

Add these imports after the existing import block (after line 51):

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
```

- [ ] **Step 2: Add focusSheetTask state to TodoScreen and open/close logic**

Inside `TodoScreen`, after `var addingTask by rememberSaveable { mutableStateOf(false) }` (line 66), add:

```kotlin
var focusSheetTask by remember { mutableStateOf<FocusTask?>(null) }
```

- [ ] **Step 3: Show PreSessionSheet when focusSheetTask is non-null**

Add this block at the end of the `Column` in `TodoScreen`, just before the closing `}` of the composable (after line 220):

```kotlin
focusSheetTask?.let { task ->
    PreSessionSheet(
        task = task,
        onDismiss = { focusSheetTask = null },
        onStartFocus = { updatedTask ->
            focusSheetTask = null
            onFocusTask(updatedTask)
        }
    )
}
```

- [ ] **Step 4: Add PreSessionSheet composable at bottom of TodoScreen.kt**

Add after the `TaskGlassRow` composable (after line 284):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreSessionSheet(
    task: FocusTask,
    onDismiss: () -> Unit,
    onStartFocus: (FocusTask) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sessionCount by remember { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1228),
        contentColor = Color.White,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Task name subtitle
            Text(
                text = task.title,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            // Heading
            Text(
                text = "How many sessions?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            // Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { if (sessionCount > 1) sessionCount-- },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = sessionCount.toString(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { if (sessionCount < 8) sessionCount++ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Start Focus button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF8B6F))
                    .clickable {
                        onStartFocus(task.copy(plannedSessions = sessionCount, pomodorosDone = 0))
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  Start Focus",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
```

- [ ] **Step 5: Add mutableIntStateOf import to TodoScreen.kt**

Add import:
```kotlin
import androidx.compose.runtime.mutableIntStateOf
```

- [ ] **Step 6: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/TodoScreen.kt
git commit -m "feat: add PreSessionSheet bottom sheet to TodoScreen"
```

---

## Task 5: Add SwipeToDismiss leading swipe to TaskGlassRow

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/TodoScreen.kt`

- [ ] **Step 1: Add SwipeToDismiss imports**

Add imports to `TodoScreen.kt`:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
```

- [ ] **Step 2: Replace TaskGlassRow body with SwipeToDismissBox**

Replace the entire `TaskGlassRow` composable body (everything inside the function braces, starting at line 231):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskGlassRow(
    task: FocusTask,
    active: Boolean,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Leading swipe: open Focus sheet — snap back
                    scope.launch {
                        dismissState.reset()
                    }
                    onFocusTask(task)
                    false // don't dismiss the row
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteTask(task.id)
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.40f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isLeading = direction == SwipeToDismissBoxValue.StartToEnd
            val isTrailing = direction == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when {
                            isLeading -> Color(0xFFFF8B6F)
                            isTrailing -> Color(0xFFE53935)
                            else -> Color.Transparent
                        }
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isLeading) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isLeading) {
                    Text(
                        text = "▶  Focus with Purrodoro",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isTrailing) {
                    Text(text = "✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        content = {
            GlassCard(modifier = Modifier.fillMaxWidth(), active = active, glassTreatment = glassTreatment) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Checkbox,
                            onClickLabel = if (task.completed) "Unmark ${task.title}" else "Complete ${task.title}"
                        ) { onToggleTask(task.id) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val checkFill = if (task.completed) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.10f)
                    val checkBorder = if (task.completed) Color.Transparent else Color.White.copy(alpha = 0.25f)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(checkFill)
                            .border(1.dp, checkBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.completed) {
                            Text(text = "✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = task.title,
                            color = if (task.completed) Color.White.copy(alpha = 0.40f) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = "${task.minutes} min",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "✕",
                        color = Color.White.copy(alpha = 0.30f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Delete ${task.title}"
                            ) { onDeleteTask(task.id) }
                            .semantics { contentDescription = "Delete ${task.title}" }
                    )
                }
            }
        }
    )
}
```

> **Note:** `dismissState` is referenced inside `confirmValueChange` lambda before declaration. Kotlin allows this via `lateinit`-style forward reference in lambdas — but with `rememberSwipeToDismissBoxState` the state object is created before the lambda runs, so `scope.launch { dismissState.reset() }` is safe.

- [ ] **Step 3: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/TodoScreen.kt
git commit -m "feat: add leading swipe Focus with Purrodoro and trailing swipe delete to TaskGlassRow"
```

---

## Task 6: Add session-complete logic to timer LaunchedEffect

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt:196-209`

- [ ] **Step 1: Extend timer-complete block to handle active task**

In `PurrodoroApp.kt`, replace lines 196-209 (the `if (updatedRemainingSeconds == 0)` block):

```kotlin
if (updatedRemainingSeconds == 0) {
    isRunning = false
    if (timerMode == TimerMode.Focus) {
        persistCompletedSessions(completedSessions + 1)
        persistStamps(addFocusStamp(stamps, LocalDate.now()))

        val currentActiveTask = activeTask
        if (currentActiveTask != null) {
            val updatedPomodoros = currentActiveTask.pomodorosDone + 1
            if (updatedPomodoros >= currentActiveTask.plannedSessions) {
                // All sessions done — mark task complete and return to Todo
                persistTodoLists(todoLists.map { list ->
                    list.copy(tasks = list.tasks.map { t ->
                        if (t.id == currentActiveTask.id) t.copy(completed = true) else t
                    })
                })
                activeTask = null
                isFocusMode = false
                selectedTab = AppTab.Todo
                timerMode = TimerMode.Focus
                remainingSeconds = settings.secondsFor(TimerMode.Focus)
                timerStartedAtMillis = null
                timerStartedRemainingSeconds = remainingSeconds
            } else {
                // More sessions remain — update pomodorosDone, start short break
                activeTask = currentActiveTask.copy(pomodorosDone = updatedPomodoros)
                isFocusMode = false
                timerMode = TimerMode.ShortBreak
                remainingSeconds = settings.secondsFor(TimerMode.ShortBreak)
                timerStartedAtMillis = null
                timerStartedRemainingSeconds = remainingSeconds
            }
        } else {
            timerMode = TimerMode.ShortBreak
            remainingSeconds = settings.secondsFor(TimerMode.ShortBreak)
            timerStartedAtMillis = null
            timerStartedRemainingSeconds = remainingSeconds
        }
    } else {
        timerMode = timerMode
        remainingSeconds = settings.secondsFor(timerMode)
        timerStartedAtMillis = null
        timerStartedRemainingSeconds = remainingSeconds
    }
    break
}
```

- [ ] **Step 2: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: session-complete logic — auto-mark task done and return to Todo tab"
```

---

## Task 7: Add Focus Mode fullscreen overlay

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

- [ ] **Step 1: Add FocusModeScreen composable at the bottom of PurrodoroApp.kt (before the Saver objects)**

Add after the `PremiumMessage` composable (after the closing `}` of `PremiumMessage`):

```kotlin
@Composable
private fun FocusModeScreen(
    task: FocusTask?,
    remainingSeconds: Int,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0819))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "FOCUS · DND ON",
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
            if (task != null) {
                Text(
                    text = task.title,
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${task.pomodorosDone + 1} / ${task.plannedSessions}",
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
}
```

- [ ] **Step 2: Show FocusModeScreen when isFocusMode is true**

In `PurrodoroApp.kt`, the root surface (`KuroFlatSurface`) block at line 219 wraps a `Column`. After the `Box` that holds the tab content (the `Box` with `weight(1f)`) and before the `premiumSheetMessage?.let` block, add this overlay. The simplest approach: wrap the existing `Box(weight(1f))` content in a `Box` that can host the overlay:

Replace lines 221-300 (the `Column` contents) — specifically the `Box(weight(1f))` block — so the overlay can float on top. Replace the inner `Box` (lines 221-299) with:

```kotlin
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
            activeTask = activeTask,
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
            onBackToFocusMode = { isFocusMode = true }
        )
        AppTab.Todo -> TodoScreen(
            todoLists = todoLists,
            selectedListId = selectedTodoListId,
            onListSelected = { persistSelectedTodoListId(it) },
            onAddTask = addTask,
            onToggleTask = toggleTask,
            onDeleteTask = deleteTask,
            onFocusTask = { task ->
                activeTask = task
                isFocusMode = true
                selectedTab = AppTab.Timer
                isRunning = true
            },
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
    // Focus Mode fullscreen overlay
    if (isFocusMode && isRunning) {
        FocusModeScreen(
            task = activeTask,
            remainingSeconds = remainingSeconds,
            onTap = { isFocusMode = false }
        )
    }
}
```

- [ ] **Step 3: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: add FocusModeScreen fullscreen overlay when isFocusMode is true"
```

---

## Task 8: Update FocusDashboard Running state — add task context and controls

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt` — `FocusDashboard` composable (lines 337-452)

- [ ] **Step 1: Update FocusDashboard signature to accept activeTask and onBackToFocusMode**

Replace lines 337-344 (the `FocusDashboard` signature):

```kotlin
@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    activeTask: FocusTask?,
    onModeSelected: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
    onBackToFocusMode: () -> Unit,
) {
```

- [ ] **Step 2: Add task header row and "back to focus mode" chip to FocusDashboard body**

Inside `FocusDashboard`, after `Spacer(Modifier.height(40.dp))` (line 353), add a task info header when `isRunning && activeTask != null`:

```kotlin
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
        text = "${activeTask.pomodorosDone + 1} / ${activeTask.plannedSessions}",
        color = Color(0xFFFF8B6F).copy(alpha = 0.80f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
}
```

- [ ] **Step 3: Add "back to focus mode" chip below the timer controls row**

Inside `FocusDashboard`, after the `Row` block that contains the play/mode buttons (after the closing `}` of that `Row`, around line 449), and before `Spacer(Modifier.weight(1f))`, add:

```kotlin
if (isRunning && activeTask != null) {
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFF8B6F).copy(alpha = 0.18f))
            .border(1.dp, Color(0xFFFF8B6F).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable(onClick = onBackToFocusMode)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "↗  back to focus mode",
            color = Color(0xFFFF8B6F),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
```

- [ ] **Step 4: Build and verify**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: show active task info and back-to-focus chip in FocusDashboard running state"
```

---

## Self-Review Checklist

- [x] **Spec §1 (FocusTask fields):** Task 1 adds `plannedSessions` and `pomodorosDone`
- [x] **Spec §2 (State in PurrodoroApp):** Task 2 adds `activeTask` + `isFocusMode` with `rememberSaveable`; `FocusTaskSaver` handles nullable `FocusTask`
- [x] **Spec §3 (Leading swipe):** Task 5 adds `SwipeToDismissBox` with orange leading slab, 40% threshold, snaps back after action
- [x] **Spec §4 (Pre-session sheet):** Task 4 adds `PreSessionSheet` with stepper, default 1, max 8, "Start Focus" button
- [x] **Spec §5 (Focus Mode screen):** Task 7 adds `FocusModeScreen` fullscreen overlay with tap-to-dismiss
- [x] **Spec §6 (Running state redesign):** Task 8 adds task header, sessions pill, "back to focus mode" chip
- [x] **Spec §7 (Session complete):** Task 6 handles both all-sessions-done (mark complete → Todo tab) and mid-session (start break, decrement remaining)
- [x] **`FocusTask.id` is `Int`:** All task ID references use `Int` — confirmed
- [x] **No ViewModel/persistence changes:** All state is `rememberSaveable`; `persistTodoLists` used for task completion
- [x] **`TimerScreen.kt` untouched:** No tasks modify it
- [x] **Type consistency:** `activeTask: FocusTask?`, `onFocusTask: (FocusTask) -> Unit` used consistently across Tasks 2–8
