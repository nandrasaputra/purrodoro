# Todo Feature Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the Todo screen from a list-of-lists model to a flat task + category model, add category management, a task detail sheet, a completed tasks section, and fix two pre-existing bugs.

**Architecture:** Replace `TodoListState` containers and DataStore keys with a flat `List<FocusTask>` + `List<Category>` stored under new keys `KEY_TASKS`, `KEY_CATEGORIES`, `KEY_SELECTED_CATEGORY_ID`, `KEY_NEXT_CATEGORY_ID`. The `TodoScreen` is rewritten in-place; no new screens or navigation libraries are added.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 `ModalBottomSheet`, DataStore Preferences, existing `GlassCard`/`GlassPill` component library.

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `model/KuroModels.kt` | Modify | Add `Category` data class; add `categoryId: Int?` to `FocusTask`; keep `TodoListState` temporarily (removed in Task 9) |
| `data/AppRepository.kt` | Modify | Add new DataStore keys; add `AppState` fields; add category/task CRUD operations; update encode/decode helpers |
| `timer/TimerStateMachine.kt` | Modify | Bug 1 fix — increment `completedSessions` on focus segment complete |
| `KuroPomodoroApp.kt` | Modify | Bug 2 fix — replace hardcoded `25` with `settings.focusMinutes`; rewire TodoScreen call site |
| `ui/TodoScreen.kt` | Rewrite | New pill row, edit mode, task row with tap-to-detail, `TaskDetailSheet`, completed section |

---

## Task 1: Add `Category` model and `categoryId` to `FocusTask`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt`

- [ ] **Step 1: Add `Category` data class after the `FocusTask` declaration**

In `KuroModels.kt`, after line 53 (`fun toggleCompleted()...`), add:

```kotlin
data class Category(val id: Int, val name: String)
```

- [ ] **Step 2: Add `categoryId` field to `FocusTask`**

Replace the existing `FocusTask` declaration (lines 44–53):

```kotlin
data class FocusTask(
    val id: Int,
    val title: String,
    val minutes: Int,
    val completed: Boolean = false,
    val plannedSessions: Int = 1,
    val completedSessions: Int = 0,
    val categoryId: Int? = null
) {
    fun toggleCompleted(): FocusTask = copy(completed = !completed)
}
```

- [ ] **Step 3: Build to verify no compile errors**

```
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL (warnings OK, no errors)

- [ ] **Step 4: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt
git commit -m "feat(model): add Category type and categoryId to FocusTask"
```

---

## Task 2: Update `AppRepository` — new DataStore keys, new `AppState` fields, encode/decode helpers

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt`

- [ ] **Step 1: Add new imports**

At the top of `AppRepository.kt`, add:

```kotlin
import com.endiar.kuropomodoro.model.Category
```

- [ ] **Step 2: Add new DataStore key constants inside the `companion object`**

After `private val KEY_RUNTIME_SNAPSHOT = ...` add:

```kotlin
private val KEY_TASKS = stringPreferencesKey("tasks_json")
private val KEY_CATEGORIES = stringPreferencesKey("categories_json")
private val KEY_NEXT_CATEGORY_ID = intPreferencesKey("next_category_id")
private val KEY_SELECTED_CATEGORY_ID = stringPreferencesKey("selected_category_id")
```

(`KEY_TODOS` and `KEY_SELECTED_LIST` are kept for now to avoid breaking existing reads; they are removed in Task 9.)

- [ ] **Step 3: Update `AppState` to include new fields**

Replace the `AppState` data class (lines 28–39):

```kotlin
data class AppState(
    val glassReadabilityMode: GlassReadabilityMode,
    val selectedSoundId: Int,
    val completedSessions: Int,
    val settings: TimerSettings,
    val nextTaskId: Int,
    val selectedTodoListId: Int,         // kept for backwards compat, unused after migration
    val stamps: Map<LocalDate, Int>,
    val todoLists: List<TodoListState>,  // kept for backwards compat, unused after migration
    val isPremium: Boolean = false,
    val sessionRecords: List<SessionRecord> = emptyList(),
    val tasks: List<FocusTask> = emptyList(),
    val categories: List<Category> = emptyList(),
    val nextCategoryId: Int = 4,
    val selectedCategoryId: Int? = null
)
```

- [ ] **Step 4: Add seed data constant**

After the `SEED_TODO_LISTS` constant (around line 60), add:

```kotlin
private val SEED_CATEGORIES = listOf(
    Category(1, "Study"),
    Category(2, "Home"),
    Category(3, "Ideas")
)

private val SEED_TASKS = listOf(
    FocusTask(1, "Review Compose layout", 25, categoryId = 1),
    FocusTask(2, "Read chapter notes", 20, categoryId = 1),
    FocusTask(3, "Clean desk", 10, categoryId = 2),
    FocusTask(4, "Water plants", 5, categoryId = 2),
    FocusTask(5, "Sketch cat animation", 15, categoryId = 3),
    FocusTask(6, "Plan next sprint", 25, categoryId = 3)
)
```

- [ ] **Step 5: Update the `appState` Flow to map new keys**

Inside the `appState` flow mapping block, add after the existing `sessionRecords` line:

```kotlin
tasks = decodeTasks(prefs[KEY_TASKS] ?: ""),
categories = decodeCategories(prefs[KEY_CATEGORIES] ?: ""),
nextCategoryId = prefs[KEY_NEXT_CATEGORY_ID] ?: 4,
selectedCategoryId = prefs[KEY_SELECTED_CATEGORY_ID]
    ?.takeIf { it.isNotBlank() }
    ?.toIntOrNull()
```

- [ ] **Step 6: Add new suspend setters below the existing ones (around line 135)**

```kotlin
suspend fun setTasks(value: List<FocusTask>) =
    dataStore.edit { it[KEY_TASKS] = encodeTasks(value) }

suspend fun setCategories(value: List<Category>) =
    dataStore.edit { it[KEY_CATEGORIES] = encodeCategories(value) }

suspend fun setNextCategoryId(value: Int) =
    dataStore.edit { it[KEY_NEXT_CATEGORY_ID] = value }

suspend fun setSelectedCategoryId(value: Int?) =
    dataStore.edit {
        if (value == null) it.remove(KEY_SELECTED_CATEGORY_ID)
        else it[KEY_SELECTED_CATEGORY_ID] = value.toString()
    }

suspend fun addCategory(name: String) = dataStore.edit { prefs ->
    val nextId = prefs[KEY_NEXT_CATEGORY_ID] ?: 4
    val existing = decodeCategories(prefs[KEY_CATEGORIES] ?: "")
    prefs[KEY_CATEGORIES] = encodeCategories(existing + Category(nextId, name))
    prefs[KEY_NEXT_CATEGORY_ID] = nextId + 1
}

suspend fun renameCategory(id: Int, name: String) = dataStore.edit { prefs ->
    val existing = decodeCategories(prefs[KEY_CATEGORIES] ?: "")
    prefs[KEY_CATEGORIES] = encodeCategories(
        existing.map { if (it.id == id) it.copy(name = name) else it }
    )
}

suspend fun deleteCategory(id: Int) = dataStore.edit { prefs ->
    val existingCategories = decodeCategories(prefs[KEY_CATEGORIES] ?: "")
    val existingTasks = decodeTasks(prefs[KEY_TASKS] ?: "")
    prefs[KEY_CATEGORIES] = encodeCategories(existingCategories.filterNot { it.id == id })
    prefs[KEY_TASKS] = encodeTasks(
        existingTasks.map { if (it.categoryId == id) it.copy(categoryId = null) else it }
    )
    // Clear selection if deleted category was selected
    val selected = prefs[KEY_SELECTED_CATEGORY_ID]?.toIntOrNull()
    if (selected == id) prefs.remove(KEY_SELECTED_CATEGORY_ID)
}
```

- [ ] **Step 7: Add `encodeTasks`/`decodeTasks` and `encodeCategories`/`decodeCategories` encoding helpers at the bottom of the file**

```kotlin
// --- Task encoding ---
// Each task: "<id>:<urlTitle>:<minutes>:<completed>:<plannedSessions>:<completedSessions>:<categoryIdOrEmpty>"
internal fun encodeTasks(tasks: List<FocusTask>): String =
    tasks.joinToString("\n") { t ->
        val encodedTitle = URLEncoder.encode(t.title, "UTF-8")
        val catField = t.categoryId?.toString() ?: ""
        "${t.id}:$encodedTitle:${t.minutes}:${t.completed}:${t.plannedSessions}:${t.completedSessions}:$catField"
    }

internal fun decodeTasks(raw: String): List<FocusTask> {
    if (raw.isBlank()) return SEED_TASKS
    return runCatching {
        raw.split("\n").map { line ->
            val p = line.split(":", limit = 7)
            FocusTask(
                id = p[0].toInt(),
                title = URLDecoder.decode(p[1], "UTF-8"),
                minutes = p[2].toInt(),
                completed = p[3].toBoolean(),
                plannedSessions = p[4].toInt(),
                completedSessions = p[5].toInt(),
                categoryId = p.getOrNull(6)?.takeIf { it.isNotBlank() }?.toInt()
            )
        }
    }.getOrElse { SEED_TASKS }
}

// --- Category encoding ---
// Each category: "<id>:<urlName>"
internal fun encodeCategories(categories: List<Category>): String =
    categories.joinToString("\n") { c ->
        "${c.id}:${URLEncoder.encode(c.name, "UTF-8")}"
    }

internal fun decodeCategories(raw: String): List<Category> {
    if (raw.isBlank()) return SEED_CATEGORIES
    return runCatching {
        raw.split("\n").map { line ->
            val p = line.split(":", limit = 2)
            Category(p[0].toInt(), URLDecoder.decode(p[1], "UTF-8"))
        }
    }.getOrElse { SEED_CATEGORIES }
}
```

- [ ] **Step 8: Build to verify no compile errors**

```
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt
git commit -m "feat(repo): add flat tasks/categories DataStore keys and CRUD operations"
```

---

## Task 3: Fix Bug 1 — `completedSessions` never increments

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/timer/TimerStateMachine.kt`

- [ ] **Step 1: Update `applyTimerComplete` to increment `completedSessions` on Focus segment complete**

In `TimerStateMachine.kt`, within the `if (currentItem.type is SessionItemType.Focus)` block (around line 44), add the task increment after `repo.incrementStampFor(date)`:

```kotlin
if (currentItem.type is SessionItemType.Focus) {
    val date = Instant.ofEpochMilli(snapshot.endsAtMillis ?: now)
        .atZone(zone).toLocalDate()
    repo.incrementStampFor(date)
    openRecord = openRecord?.copy(
        completedFocusCount = openRecord.completedFocusCount + 1
    )
    // Bug fix: increment completedSessions on the active task
    val updatedTask = snapshot.activeTask?.copy(
        completedSessions = snapshot.activeTask.completedSessions + 1
    )
    if (updatedTask != null) {
        val currentTasks = repo.appState.value?.tasks
        if (currentTasks != null) {
            repo.setTasks(currentTasks.map {
                if (it.id == updatedTask.id) updatedTask else it
            })
        }
    }
}
```

Wait — `appState` is a `Flow<AppState>`, not a stateful value. We need to read from DataStore directly. The cleanest approach is to add a helper `suspend fun updateTaskCompletedSessions(taskId: Int)` to `AppRepository` and call it here.

- [ ] **Step 2: Add `updateTaskCompletedSessions` to `AppRepository`**

In `AppRepository.kt`, add after the `deleteCategory` method:

```kotlin
suspend fun incrementTaskCompletedSessions(taskId: Int) = dataStore.edit { prefs ->
    val existing = decodeTasks(prefs[KEY_TASKS] ?: "")
    prefs[KEY_TASKS] = encodeTasks(
        existing.map { if (it.id == taskId) it.copy(completedSessions = it.completedSessions + 1) else it }
    )
}
```

- [ ] **Step 3: Use the new helper in `TimerStateMachine`**

Replace the task-increment block from Step 1 with:

```kotlin
if (currentItem.type is SessionItemType.Focus) {
    val date = Instant.ofEpochMilli(snapshot.endsAtMillis ?: now)
        .atZone(zone).toLocalDate()
    repo.incrementStampFor(date)
    openRecord = openRecord?.copy(
        completedFocusCount = openRecord.completedFocusCount + 1
    )
    snapshot.activeTask?.let { repo.incrementTaskCompletedSessions(it.id) }
}
```

- [ ] **Step 4: Build**

```
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/timer/TimerStateMachine.kt \
        kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt
git commit -m "fix(timer): increment completedSessions on active task when focus segment completes"
```

---

## Task 4: Fix Bug 2 — `addTask` hardcodes `minutes = 25`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Update `addTask` lambda to use `settings.focusMinutes`**

In `KuroPomodoroApp.kt`, the `addTask` lambda is around line 158. Change:

```kotlin
list.copy(tasks = list.tasks + FocusTask(nextTaskId, title, 25))
```

to:

```kotlin
list.copy(tasks = list.tasks + FocusTask(nextTaskId, title, settings.focusMinutes))
```

- [ ] **Step 2: Build**

```
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "fix(todo): addTask uses settings.focusMinutes instead of hardcoded 25"
```

---

## Task 5: Rewire `KuroPomodoroApp` to flat task/category model

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add state variables for new fields**

In `KuroPomodoroApp.kt`, after the existing `var todoLists by remember(...)` line (~line 135), add:

```kotlin
var tasks by remember(appState.tasks) { mutableStateOf(appState.tasks) }
var categories by remember(appState.categories) { mutableStateOf(appState.categories) }
var nextCategoryId by remember(appState.nextCategoryId) { mutableIntStateOf(appState.nextCategoryId) }
var selectedCategoryId by remember(appState.selectedCategoryId) { mutableStateOf(appState.selectedCategoryId) }
```

- [ ] **Step 2: Add persist helpers for new fields**

After the existing persist helper functions (~line 156), add:

```kotlin
fun persistTasks(v: List<FocusTask>) { tasks = v; scope.launch { repo.setTasks(v) } }
fun persistSelectedCategoryId(v: Int?) { selectedCategoryId = v; scope.launch { repo.setSelectedCategoryId(v) } }
```

- [ ] **Step 3: Replace the `addTask` lambda with the flat model version**

Replace the existing `addTask` lambda (lines ~158–167):

```kotlin
val addTask: (String, Int?) -> Unit = { title, categoryId ->
    val newTask = FocusTask(nextTaskId, title, settings.focusMinutes, categoryId = categoryId)
    persistTasks(tasks + newTask)
    persistNextTaskId(nextTaskId + 1)
}
```

- [ ] **Step 4: Replace `toggleTask` and `deleteTask` lambdas**

Replace the existing lambdas (lines ~168–177):

```kotlin
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
```

- [ ] **Step 5: Update the `TodoScreen` call site**

Replace the `AppTab.Todo -> TodoScreen(...)` block (lines ~274–291) with:

```kotlin
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
    onRenameCategory = { id, name -> scope.launch { repo.renameCategory(id, name) } },
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
    contentPadding = screenContentPadding
)
```

- [ ] **Step 6: Update the initial `AppState` value in `collectAsStateWithLifecycle` to include the new fields**

In the `initialValue = AppState(...)` block (~line 89), add the new fields at the end:

```kotlin
tasks = listOf(
    FocusTask(1, "Review Compose layout", 25, categoryId = 1),
    FocusTask(2, "Read chapter notes", 20, categoryId = 1),
    FocusTask(3, "Clean desk", 10, categoryId = 2),
    FocusTask(4, "Water plants", 5, categoryId = 2),
    FocusTask(5, "Sketch cat animation", 15, categoryId = 3),
    FocusTask(6, "Plan next sprint", 25, categoryId = 3)
),
categories = listOf(
    com.endiar.kuropomodoro.model.Category(1, "Study"),
    com.endiar.kuropomodoro.model.Category(2, "Home"),
    com.endiar.kuropomodoro.model.Category(3, "Ideas")
),
nextCategoryId = 4,
selectedCategoryId = null
```

- [ ] **Step 7: Build (expect `TodoScreen` compile errors — that is expected until Task 6)**

```
./gradlew :app:compileDebugKotlin 2>&1 | head -40
```

Errors for `TodoScreen` are expected. No other errors should appear.

- [ ] **Step 8: Commit the KuroPomodoroApp changes (even with compile errors on TodoScreen stubs)**

Skip committing until after Task 6 resolves the compile errors.

---

## Task 6: Rewrite `TodoScreen` — filter pill row with Edit mode

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt`

This task rewrites the top portion of `TodoScreen`: the new function signature, header row, and filter pill row with Edit mode. The task rows and sheets are added in Tasks 7 and 8.

- [ ] **Step 1: Replace the `TodoScreen` function signature and header/pill-row section**

Replace the entire file content with:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.Category
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.GlassPill
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    tasks: List<FocusTask>,
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onAddTask: (String, Int?) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onUpdateTask: (FocusTask) -> Unit,
    onDeleteAllCompleted: (Int?) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (Int, String) -> Unit,
    onDeleteCategory: (Int) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    settings: TimerSettings = TimerSettings(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // Derive filtered task list
    val filteredActiveTasks = tasks.filter { task ->
        !task.completed && (selectedCategoryId == null || task.categoryId == selectedCategoryId)
    }
    val filteredCompletedTasks = tasks.filter { task ->
        task.completed && (selectedCategoryId == null || task.categoryId == selectedCategoryId)
    }

    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var newTaskCategoryId by rememberSaveable { mutableStateOf(selectedCategoryId) }
    var focusSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    var detailSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var addCategorySheetVisible by rememberSaveable { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<Category?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val today = LocalDate.now()
    val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

    val submitTask = {
        val title = newTaskTitle.trim()
        if (title.isNotEmpty()) {
            onAddTask(title, newTaskCategoryId)
            newTaskTitle = ""
            addingTask = false
            newTaskCategoryId = selectedCategoryId
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header row ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "To-Do",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateLabel,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isEditMode) "Done" else "Edit",
                        color = Color(0xFFFF8B6F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { isEditMode = !isEditMode }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF8B6F))
                    .clickable { addingTask = !addingTask },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Filter pill row ──────────────────────────────────────────────
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" pill — always first, non-editable
            CategoryPill(
                label = "All",
                selected = selectedCategoryId == null,
                isEditMode = false,
                onClick = { onCategorySelected(null) },
                glassTreatment = glassTreatment
            )
            categories.forEach { category ->
                CategoryPill(
                    label = category.name,
                    selected = category.id == selectedCategoryId,
                    isEditMode = isEditMode,
                    onClick = {
                        if (isEditMode) {
                            renameCategoryTarget = category
                        } else {
                            onCategorySelected(category.id)
                        }
                    },
                    onDelete = { deleteCategoryTarget = category },
                    glassTreatment = glassTreatment
                )
            }
            if (isEditMode) {
                // "+ New" pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFFF8B6F).copy(alpha = 0.50f), RoundedCornerShape(20.dp))
                        .clickable { addCategorySheetVisible = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ New",
                        color = Color(0xFFFF8B6F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Inline add-task input ────────────────────────────────────────
        if (addingTask) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0A1A).copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Add a focus task…",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTask() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFF8B6F),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF8B6F))
                            .clickable { submitTask() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = "Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                // Category selector chips for new task
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // "None" chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (newTaskCategoryId == null) Color(0xFFFF8B6F).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (newTaskCategoryId == null) Color(0xFFFF8B6F).copy(alpha = 0.60f)
                                    else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { newTaskCategoryId = null }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "None",
                                color = if (newTaskCategoryId == null) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        categories.forEach { cat ->
                            val isSelected = newTaskCategoryId == cat.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.60f)
                                        else Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { newTaskCategoryId = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    color = if (isSelected) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Active tasks ────────────────────────────────────────────────
        Text(
            text = "UPCOMING",
            color = Color.White.copy(alpha = 0.30f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        if (filteredActiveTasks.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), glassTreatment = glassTreatment) {
                Text(
                    text = "No tasks yet",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap + to add your first focus task.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        } else {
            val firstActiveTask = filteredActiveTasks.firstOrNull()
            filteredActiveTasks.forEach { task ->
                TaskGlassRow(
                    task = task,
                    active = task == firstActiveTask,
                    showCategoryLabel = selectedCategoryId == null,
                    categoryName = categories.firstOrNull { it.id == task.categoryId }?.name,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onTapRow = { detailSheetTask = it },
                    onFocusTask = { if (!task.completed) focusSheetTask = it },
                    glassTreatment = glassTreatment
                )
            }
        }

        // ── Completed section ───────────────────────────────────────────
        if (filteredCompletedTasks.isNotEmpty()) {
            // Section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { completedExpanded = !completedExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPLETED (${filteredCompletedTasks.size})",
                    color = Color.White.copy(alpha = 0.30f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (completedExpanded) "▲" else "▼",
                    color = Color.White.copy(alpha = 0.30f),
                    fontSize = 10.sp
                )
            }

            if (completedExpanded) {
                filteredCompletedTasks.forEach { task ->
                    TaskGlassRow(
                        task = task,
                        active = false,
                        showCategoryLabel = selectedCategoryId == null,
                        categoryName = categories.firstOrNull { it.id == task.categoryId }?.name,
                        dimmed = true,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onTapRow = { detailSheetTask = it },
                        onFocusTask = {},
                        glassTreatment = glassTreatment
                    )
                }
                // "Delete All Completed" button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE53935).copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                        .clickable { onDeleteAllCompleted(selectedCategoryId) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete All Completed",
                        color = Color(0xFFE53935).copy(alpha = 0.80f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // ── TaskDetailSheet ──────────────────────────────────────────────────
    detailSheetTask?.let { task ->
        TaskDetailSheet(
            task = task,
            categories = categories,
            focusMinutes = settings.focusMinutes,
            onDismiss = { detailSheetTask = null },
            onUpdate = onUpdateTask,
            onFocusTask = { updatedTask ->
                detailSheetTask = null
                focusSheetTask = updatedTask
            }
        )
    }

    // ── PreSessionSheet ──────────────────────────────────────────────────
    focusSheetTask?.let { task ->
        PreSessionSheet(
            task = task,
            focusMinutes = settings.focusMinutes,
            onDismiss = { focusSheetTask = null },
            onStartFocus = { updatedTask ->
                focusSheetTask = null
                onFocusTask(updatedTask)
            }
        )
    }

    // ── Delete Category confirmation dialog ──────────────────────────────
    deleteCategoryTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = {
                Text(
                    text = "Delete ${category.name}?",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Tasks in this category will become uncategorized.",
                    color = Color.White.copy(alpha = 0.75f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(category.id)
                    if (selectedCategoryId == category.id) onCategorySelected(null)
                    deleteCategoryTarget = null
                }) {
                    Text(
                        text = "Delete",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.70f))
                }
            },
            containerColor = Color(0xFF2A2035),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Add Category sheet ───────────────────────────────────────────────
    if (addCategorySheetVisible) {
        CategoryNameSheet(
            title = "New Category",
            initialName = "",
            onDismiss = { addCategorySheetVisible = false },
            onConfirm = { name ->
                onAddCategory(name)
                addCategorySheetVisible = false
            }
        )
    }

    // ── Rename Category sheet ────────────────────────────────────────────
    renameCategoryTarget?.let { category ->
        CategoryNameSheet(
            title = "Rename Category",
            initialName = category.name,
            onDismiss = { renameCategoryTarget = null },
            onConfirm = { name ->
                onRenameCategory(category.id, name)
                renameCategoryTarget = null
            }
        )
    }
}
```

- [ ] **Step 2: Build (will fail on missing composables `CategoryPill`, `TaskGlassRow`, `TaskDetailSheet`, `CategoryNameSheet` — proceed)**

```
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -20
```

Expected: errors only for missing composables we'll add next.

---

## Task 7: Add `CategoryPill` and updated `TaskGlassRow` composables

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt`

Append the following private composables at the end of the file:

- [ ] **Step 1: Add `CategoryPill` composable**

```kotlin
@Composable
private fun CategoryPill(
    label: String,
    selected: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    Box(contentAlignment = Alignment.TopStart) {
        GlassPill(
            text = label,
            selected = selected,
            onClick = onClick,
            glassTreatment = glassTreatment
        )
        if (isEditMode && onDelete != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add the updated `TaskGlassRow` composable (replaces the previous private one)**

Replace the old `TaskGlassRow` composable with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskGlassRow(
    task: FocusTask,
    active: Boolean,
    showCategoryLabel: Boolean,
    categoryName: String?,
    dimmed: Boolean = false,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    val scope = rememberCoroutineScope()
    var resetDismiss: (suspend () -> Unit)? = null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    scope.launch { resetDismiss?.invoke() }
                    onFocusTask(task)
                    false
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
    resetDismiss = { dismissState.reset() }

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
                when {
                    isLeading -> Text(
                        text = "▶  Focus with Kuro",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    isTrailing -> Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        content = {
            val rowAlpha = if (dimmed) 0.50f else 1.0f
            GlassCard(modifier = Modifier.fillMaxWidth(), active = active, glassTreatment = glassTreatment) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTapRow(task) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val checkFill = if (task.completed) Color(0xFFFF8B6F).copy(alpha = rowAlpha) else Color.White.copy(alpha = 0.10f)
                    val checkBorder = if (task.completed) Color.Transparent else Color.White.copy(alpha = 0.25f)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(checkFill)
                            .border(1.dp, checkBorder, CircleShape)
                            .clickable(
                                role = Role.Checkbox,
                                onClickLabel = if (task.completed) "Unmark ${task.title}" else "Complete ${task.title}"
                            ) { onToggleTask(task.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.completed) {
                            Text(text = "✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = task.title,
                            color = if (task.completed) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = rowAlpha),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (showCategoryLabel && categoryName != null) {
                            Text(
                                text = categoryName,
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "${task.minutes} min",
                                color = Color.White.copy(alpha = 0.45f * rowAlpha),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    )
}
```

- [ ] **Step 3: Build**

```
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -20
```

Expected: errors only for `TaskDetailSheet` and `CategoryNameSheet`.

---

## Task 8: Add `TaskDetailSheet` and `CategoryNameSheet` composables

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt`

Append at the end of the file:

- [ ] **Step 1: Add `TaskDetailSheet` composable**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailSheet(
    task: FocusTask,
    categories: List<Category>,
    focusMinutes: Int,
    onDismiss: () -> Unit,
    onUpdate: (FocusTask) -> Unit,
    onFocusTask: (FocusTask) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable { mutableStateOf(task.title) }
    var minutes by rememberSaveable { mutableIntStateOf(task.minutes) }
    var plannedSessions by rememberSaveable { mutableIntStateOf(task.plannedSessions) }
    var categoryId by rememberSaveable { mutableStateOf(task.categoryId) }

    val saveAndDismiss = {
        val trimmedTitle = title.trim().ifEmpty { task.title }
        onUpdate(task.copy(
            title = trimmedTitle,
            minutes = minutes,
            plannedSessions = plannedSessions,
            categoryId = categoryId
        ))
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = saveAndDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1228),
        contentColor = Color.White,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Task Detail",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Title field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TITLE",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF8B6F),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Category selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CATEGORY",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "None" chip
                    val noneSelected = categoryId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (noneSelected) Color(0xFFFF8B6F).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (noneSelected) Color(0xFFFF8B6F).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .clickable { categoryId = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "None",
                            color = if (noneSelected) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    categories.forEach { cat ->
                        val isSelected = categoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                                .border(1.dp, if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable { categoryId = cat.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isSelected) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Minutes stepper
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MINUTES PER SESSION",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    StepperButton(
                        label = "−",
                        enabled = minutes > 1,
                        onClick = { if (minutes > 1) minutes-- }
                    )
                    Spacer(Modifier.width(24.dp))
                    Text(
                        text = minutes.toString(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(24.dp))
                    StepperButton(
                        label = "+",
                        enabled = minutes < 180,
                        onClick = { if (minutes < 180) minutes++ }
                    )
                }
            }

            // Planned sessions stepper
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PLANNED SESSIONS",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    StepperButton(
                        label = "−",
                        enabled = plannedSessions > 1,
                        onClick = { if (plannedSessions > 1) plannedSessions-- }
                    )
                    Spacer(Modifier.width(24.dp))
                    Text(
                        text = plannedSessions.toString(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(24.dp))
                    StepperButton(
                        label = "+",
                        enabled = plannedSessions < 8,
                        onClick = { if (plannedSessions < 8) plannedSessions++ }
                    )
                }
            }

            // Completed sessions (read-only)
            Text(
                text = "${task.completedSessions} of ${task.plannedSessions} sessions done",
                color = Color(0xFFFF8B6F).copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Focus with Kuro button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF8B6F))
                    .clickable {
                        val updated = task.copy(
                            title = title.trim().ifEmpty { task.title },
                            minutes = minutes,
                            plannedSessions = plannedSessions,
                            categoryId = categoryId
                        )
                        onFocusTask(updated)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  Focus with Kuro",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add `StepperButton` helper composable**

```kotlin
@Composable
private fun StepperButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.30f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
```

- [ ] **Step 3: Add `CategoryNameSheet` composable**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryNameSheet(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(initialName) }
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1228),
        contentColor = Color.White,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                placeholder = {
                    Text("Category name", color = Color.White.copy(alpha = 0.35f))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        keyboardController?.hide()
                        onConfirm(trimmed)
                    }
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFF8B6F),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF8B6F))
                    .clickable {
                        val trimmed = name.trim()
                        if (trimmed.isNotEmpty()) {
                            keyboardController?.hide()
                            onConfirm(trimmed)
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Confirm",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
```

- [ ] **Step 4: Keep the existing `PreSessionSheet` composable** (it should still be in the file — do not remove it)

- [ ] **Step 5: Full build**

```
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit Tasks 5–8 together**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt \
        kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat(todo): rewrite TodoScreen with categories, task detail sheet, completed section"
```

---

## Task 9: Clean up legacy `TodoListState` references

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt`
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt`

**Note:** Only remove `TodoListState`, `KEY_TODOS`, `KEY_SELECTED_LIST`, and related fields from `AppState` if there are no remaining callers. Verify before deleting.

- [ ] **Step 1: Search for remaining `TodoListState` references**

```bash
grep -rn "TodoListState\|KEY_TODOS\|KEY_SELECTED_LIST\|todoLists\|selectedTodoListId\|setTodoLists\|persistTodoLists" \
  kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ \
  --include="*.kt"
```

- [ ] **Step 2: Remove or update each reference**

For each file that still references the legacy fields:
- In `KuroModels.kt`: Delete `data class TodoListState(...)` if only `AppRepository` used it.
- In `AppRepository.kt`: Remove `KEY_TODOS`, `KEY_SELECTED_LIST` key constants; remove `todoLists` and `selectedTodoListId` fields from `AppState`; remove `setTodoLists` and `setSelectedTodoListId` methods; remove `encodeTodoLists`/`decodeTodoLists`; remove the `SEED_TODO_LISTS` constant; remove `TodoListState` import.
- In `KuroPomodoroApp.kt`: Remove `todoLists` and `selectedTodoListId` state variables; remove `persistTodoLists` and `persistSelectedTodoListId` helpers; remove legacy initial value fields from `collectAsStateWithLifecycle`; remove `TodoListState` import.

- [ ] **Step 3: Build to confirm no regressions**

```
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run app and smoke test**

Install on emulator/device:
```
./gradlew :app:installDebug
```

Verify:
1. Todo tab loads with "All", "Study", "Home", "Ideas" pills
2. Tapping "Study" shows only Study tasks
3. Edit button toggles edit mode — category pills show × badge and + New pill appears
4. Tapping × on a category shows confirmation sheet
5. Tapping + New creates a new category
6. Adding a task pre-fills category from active filter
7. Tapping a task row opens `TaskDetailSheet`
8. Changes in detail sheet auto-save on dismiss
9. Completing a task moves it to Completed section; expanding shows completed tasks
10. "Delete All Completed" removes completed tasks for current filter
11. Focus tab: `completedSessions` increments after each focus segment

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "chore(todo): remove legacy TodoListState model and related DataStore keys"
```

---

## Self-Review Checklist

- [x] **Section 1 — Data Model:** `Category` type in Task 1; `categoryId` in Task 1; new DataStore keys `KEY_TASKS`, `KEY_CATEGORIES`, `KEY_NEXT_CATEGORY_ID`, `KEY_SELECTED_CATEGORY_ID` in Task 2; seed data in Task 2; all `AppRepository` ops in Task 2.
- [x] **Section 2 — UI:** Filter pill row with "All" in Task 6; Edit mode + × badges + + New in Tasks 6–7; add-task category selector in Task 6; tap-row opens detail in Tasks 6–7; `TaskDetailSheet` with all fields in Task 8; completed section collapsible in Task 6; "Delete All Completed" in Task 6; category labels in "All" view in Task 7.
- [x] **Section 3 — Bug 1:** `completedSessions` increment in Task 3.
- [x] **Section 3 — Bug 2:** Hardcoded `25` fix in Task 4.
- [x] **Out of scope:** No drag-and-drop, due dates, notes, or sync added.
- [x] **Placeholder scan:** No TBDs, no "add validation later", no "similar to Task N" without code.
- [x] **Type consistency:** `onAddTask: (String, Int?) -> Unit` matches Task 5 definition and Task 6 call site; `Category` type used consistently; `encodeTasks`/`decodeTasks` field count (7) matches `FocusTask` field count.
