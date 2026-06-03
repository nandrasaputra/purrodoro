# DataStore Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate all user preferences and app data from `rememberSaveable` to `DataStore<Preferences>` so they survive process death, force-stop, and reboots.

**Architecture:** A single `AppRepository` object owns the `DataStore` instance and exposes a `Flow<AppState>` plus individual `suspend` write functions. `PurrodoroApp` collects the flow with `collectAsStateWithLifecycle` and writes back on every state change via `LaunchedEffect`. No ViewModel, no DI.

**Tech Stack:** `androidx.datastore:datastore-preferences:1.1.1`, `kotlinx-coroutines-android:1.7.3`, `androidx.lifecycle:lifecycle-runtime-compose` (for `collectAsStateWithLifecycle`)

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `gradle/libs.versions.toml` | Modify | Add `datastore`, `coroutines`, `lifecycleRuntimeCompose` version entries + library aliases |
| `app/build.gradle.kts` | Modify | Add three new `implementation` dependencies |
| `app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt` | Create | DataStore instance, `AppState` data class, `Flow<AppState>`, all `suspend` write functions |
| `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt` | Modify | Replace `rememberSaveable` for 10 persisted fields with DataStore-backed reads/writes; keep 8 ephemeral fields as `rememberSaveable` |
| `app/src/test/java/com/endiar/purrodoro/data/AppRepositoryTest.kt` | Create | Unit tests for `AppRepository` serialization round-trips using `DataStoreFactory` in-memory |

---

## Task 1: Add dependencies

**Files:**
- Modify: `purrodoro-android/gradle/libs.versions.toml`
- Modify: `purrodoro-android/app/build.gradle.kts`

- [ ] **Step 1: Add version entries to libs.versions.toml**

Add under `[versions]`:
```toml
datastore = "1.1.1"
coroutines = "1.7.3"
lifecycleRuntimeCompose = "2.10.0"
```

Add under `[libraries]`:
```toml
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeCompose" }
```

- [ ] **Step 2: Add implementation dependencies to app/build.gradle.kts**

Inside the `dependencies { }` block, add after existing `implementation` lines:
```kotlin
implementation(libs.androidx.datastore.preferences)
implementation(libs.kotlinx.coroutines.android)
implementation(libs.androidx.lifecycle.runtime.compose)
```

- [ ] **Step 3: Sync and verify build compiles**

```bash
cd purrodoro-android && ./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add datastore, coroutines, lifecycle-runtime-compose deps"
```

---

## Task 2: Create AppRepository

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt`
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/data/AppRepositoryTest.kt`

### Keys and AppState shape

`AppRepository` stores the following DataStore keys:

| Key name (string) | Kotlin type | Default |
|---|---|---|
| `dark_mode_preview` | `Boolean` | `false` |
| `glass_readability_mode` | `String` | `"Adaptive"` |
| `selected_sound_id` | `Int` | `1` |
| `completed_sessions` | `Int` | `0` |
| `focus_minutes` | `Int` | `25` |
| `short_break_minutes` | `Int` | `5` |
| `long_break_minutes` | `Int` | `15` |
| `next_task_id` | `Int` | `7` |
| `selected_todo_list_id` | `Int` | `1` |
| `stamps_json` | `String` | `""` (empty = empty map) |
| `todo_lists_json` | `String` | `""` (empty = default seed lists) |

`stamps_json` encoding: comma-separated pairs `"date=count"`, e.g. `"2026-05-01=3,2026-05-02=1"`. Empty string = empty map.

`todo_lists_json` encoding: pipe-separated lists, each list is `id|name|tasks`, tasks are semicolon-separated records `taskId:title:minutes:completed`. Example:
```
1|Study|1:Review Compose layout:25:false;2:Read chapter notes:20:false
2|Home|3:Clean desk:10:false;4:Water plants:5:false
```
Empty string = use seed data (three default lists with 6 tasks).

- [ ] **Step 1: Write the failing tests**

Create `purrodoro-android/app/src/test/java/com/endiar/purrodoro/data/AppRepositoryTest.kt`:

```kotlin
package com.endiar.purrodoro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TodoListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("test_prefs.preferences_pb")
        }
        return AppRepository(ds)
    }

    @Test
    fun `default AppState has expected values`() = runTest {
        val repo = makeRepo()
        val state = repo.appState.first()
        assertEquals(false, state.darkModePreview)
        assertEquals(GlassReadabilityMode.Adaptive, state.glassReadabilityMode)
        assertEquals(1, state.selectedSoundId)
        assertEquals(0, state.completedSessions)
        assertEquals(TimerSettings(), state.settings)
        assertEquals(7, state.nextTaskId)
        assertEquals(1, state.selectedTodoListId)
        assertEquals(emptyMap<LocalDate, Int>(), state.stamps)
        assertEquals(3, state.todoLists.size) // seed data
    }

    @Test
    fun `setDarkModePreview persists true`() = runTest {
        val repo = makeRepo()
        repo.setDarkModePreview(true)
        assertEquals(true, repo.appState.first().darkModePreview)
    }

    @Test
    fun `setGlassReadabilityMode persists Readable`() = runTest {
        val repo = makeRepo()
        repo.setGlassReadabilityMode(GlassReadabilityMode.Readable)
        assertEquals(GlassReadabilityMode.Readable, repo.appState.first().glassReadabilityMode)
    }

    @Test
    fun `setSelectedSoundId persists value`() = runTest {
        val repo = makeRepo()
        repo.setSelectedSoundId(3)
        assertEquals(3, repo.appState.first().selectedSoundId)
    }

    @Test
    fun `setCompletedSessions persists value`() = runTest {
        val repo = makeRepo()
        repo.setCompletedSessions(5)
        assertEquals(5, repo.appState.first().completedSessions)
    }

    @Test
    fun `setSettings persists custom durations`() = runTest {
        val repo = makeRepo()
        repo.setSettings(TimerSettings(focusMinutes = 50, shortBreakMinutes = 10, longBreakMinutes = 20))
        val s = repo.appState.first().settings
        assertEquals(50, s.focusMinutes)
        assertEquals(10, s.shortBreakMinutes)
        assertEquals(20, s.longBreakMinutes)
    }

    @Test
    fun `setNextTaskId persists value`() = runTest {
        val repo = makeRepo()
        repo.setNextTaskId(42)
        assertEquals(42, repo.appState.first().nextTaskId)
    }

    @Test
    fun `setSelectedTodoListId persists value`() = runTest {
        val repo = makeRepo()
        repo.setSelectedTodoListId(2)
        assertEquals(2, repo.appState.first().selectedTodoListId)
    }

    @Test
    fun `setStamps round-trips multiple dates`() = runTest {
        val repo = makeRepo()
        val stamps = mapOf(
            LocalDate.of(2026, 5, 1) to 3,
            LocalDate.of(2026, 5, 2) to 1
        )
        repo.setStamps(stamps)
        assertEquals(stamps, repo.appState.first().stamps)
    }

    @Test
    fun `setTodoLists round-trips lists with tasks`() = runTest {
        val repo = makeRepo()
        val lists = listOf(
            TodoListState(1, "Work", listOf(FocusTask(1, "Task A", 25, false))),
            TodoListState(2, "Home", emptyList())
        )
        repo.setTodoLists(lists)
        val result = repo.appState.first().todoLists
        assertEquals(2, result.size)
        assertEquals("Work", result[0].name)
        assertEquals(1, result[0].tasks.size)
        assertEquals("Task A", result[0].tasks[0].title)
        assertEquals("Home", result[1].name)
        assertEquals(0, result[1].tasks.size)
    }

    @Test
    fun `setTodoLists round-trips completed flag`() = runTest {
        val repo = makeRepo()
        val lists = listOf(
            TodoListState(1, "Study", listOf(FocusTask(1, "Done task", 10, true)))
        )
        repo.setTodoLists(lists)
        val task = repo.appState.first().todoLists[0].tasks[0]
        assertEquals(true, task.completed)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd purrodoro-android && ./gradlew testDebugUnitTest --tests "com.endiar.purrodoro.data.AppRepositoryTest"
```
Expected: FAILED — `AppRepository` does not exist yet.

- [ ] **Step 3: Create AppRepository.kt**

Create `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt`:

```kotlin
package com.endiar.purrodoro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TodoListState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class AppState(
    val darkModePreview: Boolean,
    val glassReadabilityMode: GlassReadabilityMode,
    val selectedSoundId: Int,
    val completedSessions: Int,
    val settings: TimerSettings,
    val nextTaskId: Int,
    val selectedTodoListId: Int,
    val stamps: Map<LocalDate, Int>,
    val todoLists: List<TodoListState>
)

private val SEED_TODO_LISTS = listOf(
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

class AppRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_preview")
        private val KEY_GLASS_MODE = stringPreferencesKey("glass_readability_mode")
        private val KEY_SOUND_ID = intPreferencesKey("selected_sound_id")
        private val KEY_COMPLETED = intPreferencesKey("completed_sessions")
        private val KEY_FOCUS_MIN = intPreferencesKey("focus_minutes")
        private val KEY_SHORT_MIN = intPreferencesKey("short_break_minutes")
        private val KEY_LONG_MIN = intPreferencesKey("long_break_minutes")
        private val KEY_NEXT_TASK_ID = intPreferencesKey("next_task_id")
        private val KEY_SELECTED_LIST = intPreferencesKey("selected_todo_list_id")
        private val KEY_STAMPS = stringPreferencesKey("stamps_json")
        private val KEY_TODOS = stringPreferencesKey("todo_lists_json")

        // Singleton instance for production use
        private val Context.dataStore by preferencesDataStore(name = "kuro_app_prefs")

        @Volatile private var INSTANCE: AppRepository? = null

        fun get(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext.dataStore).also { INSTANCE = it }
            }
    }

    val appState: Flow<AppState> = dataStore.data.map { prefs ->
        AppState(
            darkModePreview = prefs[KEY_DARK_MODE] ?: false,
            glassReadabilityMode = prefs[KEY_GLASS_MODE]
                ?.let { runCatching { GlassReadabilityMode.valueOf(it) }.getOrNull() }
                ?: GlassReadabilityMode.Adaptive,
            selectedSoundId = prefs[KEY_SOUND_ID] ?: 1,
            completedSessions = prefs[KEY_COMPLETED] ?: 0,
            settings = TimerSettings(
                focusMinutes = prefs[KEY_FOCUS_MIN] ?: 25,
                shortBreakMinutes = prefs[KEY_SHORT_MIN] ?: 5,
                longBreakMinutes = prefs[KEY_LONG_MIN] ?: 15
            ),
            nextTaskId = prefs[KEY_NEXT_TASK_ID] ?: 7,
            selectedTodoListId = prefs[KEY_SELECTED_LIST] ?: 1,
            stamps = decodeStamps(prefs[KEY_STAMPS] ?: ""),
            todoLists = decodeTodoLists(prefs[KEY_TODOS] ?: "")
        )
    }

    suspend fun setDarkModePreview(value: Boolean) = dataStore.edit { it[KEY_DARK_MODE] = value }
    suspend fun setGlassReadabilityMode(value: GlassReadabilityMode) = dataStore.edit { it[KEY_GLASS_MODE] = value.name }
    suspend fun setSelectedSoundId(value: Int) = dataStore.edit { it[KEY_SOUND_ID] = value }
    suspend fun setCompletedSessions(value: Int) = dataStore.edit { it[KEY_COMPLETED] = value }
    suspend fun setSettings(value: TimerSettings) = dataStore.edit { prefs ->
        prefs[KEY_FOCUS_MIN] = value.focusMinutes
        prefs[KEY_SHORT_MIN] = value.shortBreakMinutes
        prefs[KEY_LONG_MIN] = value.longBreakMinutes
    }
    suspend fun setNextTaskId(value: Int) = dataStore.edit { it[KEY_NEXT_TASK_ID] = value }
    suspend fun setSelectedTodoListId(value: Int) = dataStore.edit { it[KEY_SELECTED_LIST] = value }
    suspend fun setStamps(value: Map<LocalDate, Int>) = dataStore.edit { it[KEY_STAMPS] = encodeStamps(value) }
    suspend fun setTodoLists(value: List<TodoListState>) = dataStore.edit { it[KEY_TODOS] = encodeTodoLists(value) }
}

// --- Encoding helpers ---

internal fun encodeStamps(stamps: Map<LocalDate, Int>): String =
    stamps.entries.joinToString(",") { "${it.key}=${it.value}" }

internal fun decodeStamps(raw: String): Map<LocalDate, Int> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(",").associate { pair ->
        val (date, count) = pair.split("=")
        LocalDate.parse(date) to count.toInt()
    }
}

internal fun encodeTodoLists(lists: List<TodoListState>): String =
    lists.joinToString("\n") { list ->
        val tasksStr = list.tasks.joinToString(";") { t ->
            "${t.id}:${t.title}:${t.minutes}:${t.completed}"
        }
        "${list.id}|${list.name}|$tasksStr"
    }

internal fun decodeTodoLists(raw: String): List<TodoListState> {
    if (raw.isBlank()) return SEED_TODO_LISTS
    return raw.split("\n").map { line ->
        val parts = line.split("|", limit = 3)
        val listId = parts[0].toInt()
        val name = parts[1]
        val tasksRaw = parts.getOrElse(2) { "" }
        val tasks = if (tasksRaw.isBlank()) emptyList()
        else tasksRaw.split(";").map { t ->
            val tp = t.split(":", limit = 4)
            FocusTask(
                id = tp[0].toInt(),
                title = tp[1],
                minutes = tp[2].toInt(),
                completed = tp[3].toBoolean()
            )
        }
        TodoListState(listId, name, tasks)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd purrodoro-android && ./gradlew testDebugUnitTest --tests "com.endiar.purrodoro.data.AppRepositoryTest"
```
Expected: All 11 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/data/AppRepository.kt \
        app/src/test/java/com/endiar/purrodoro/data/AppRepositoryTest.kt
git commit -m "feat: add AppRepository with DataStore persistence"
```

---

## Task 3: Wire AppRepository into PurrodoroApp

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

The strategy:
1. Accept a `Context` parameter in `PurrodoroApp` (or pass `AppRepository` directly — use the latter for testability).
2. Collect `repo.appState` as Compose state using `collectAsStateWithLifecycle`.
3. On first composition, block on the first emission to initialise the 8 ephemeral `rememberSaveable` fields that depend on `settings` (e.g. `remainingSeconds`).
4. Replace each of the 10 persisted `rememberSaveable` vars with derived state from the collected `AppState`.
5. Add `LaunchedEffect` blocks that write back to the repo whenever each persisted value changes.

**Concrete changes to `PurrodoroApp.kt`:**

- [ ] **Step 1: Add the `repo` parameter and the `collectAsStateWithLifecycle` import**

Change the composable signature and add imports. The `MainActivity` (or wherever `PurrodoroApp()` is called) will need to pass `AppRepository.get(context)`.

Replace the function signature:
```kotlin
// Before
@Composable
fun PurrodoroApp() {

// After
@Composable
fun PurrodoroApp(repo: com.endiar.purrodoro.data.AppRepository) {
```

Add these imports:
```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.AppState
import kotlinx.coroutines.launch
```

- [ ] **Step 2: Collect AppState and derive the 10 persisted state vars**

Remove these 10 `rememberSaveable` declarations (lines 71, 75, 76–80, 81, 84, 85–87, 88, 89, 90–116):
```kotlin
var settings by rememberSaveable(stateSaver = TimerSettingsSaver) { mutableStateOf(TimerSettings()) }
var completedSessions by rememberSaveable { mutableIntStateOf(0) }
var stamps by rememberSaveable(stateSaver = StampsSaver) { mutableStateOf<Map<LocalDate, Int>>(emptyMap()) }
var selectedSoundId by rememberSaveable { mutableIntStateOf(1) }
var darkModePreview by rememberSaveable { mutableStateOf(false) }
var glassReadabilityMode by rememberSaveable(stateSaver = GlassReadabilityModeSaver) { mutableStateOf(GlassReadabilityMode.Adaptive) }
var nextTaskId by rememberSaveable { mutableIntStateOf(7) }
var selectedTodoListId by rememberSaveable { mutableIntStateOf(1) }
var todoLists by rememberSaveable(stateSaver = TodoListsSaver) { mutableStateOf(...) }
```

Add at the top of the composable body (before the ephemeral vars):
```kotlin
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
        todoLists = emptyList()
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
```

Note: `remember(key)` re-initialises the mutable state when the DataStore emits a new value (e.g. after a process restart cold-read). This means if the user changes a value in-session and the DataStore hasn't caught up yet, the in-memory value is authoritative until the DataStore emits again. This is acceptable: writes happen in `LaunchedEffect` immediately after mutation, so the DataStore and in-memory state stay in sync within one frame.

- [ ] **Step 3: Add write-back LaunchedEffects**

After the existing `LaunchedEffect(isRunning, timerMode, settings)` block, add a coroutineScope and individual write-back effects. Add these inside the composable body:

```kotlin
val scope = rememberCoroutineScope()
```

Then in each setter lambda that mutates a persisted value, add the write-back. The cleanest approach is to wrap each setter in a lambda that both mutates local state and dispatches to the repo. For example, replace direct `settings = ...` assignments with:

```kotlin
fun updateSettings(newSettings: TimerSettings) {
    settings = newSettings
    scope.launch { repo.setSettings(newSettings) }
}
```

Do this for all 9 write functions. The full list of local helper functions to add inside the composable (before the `addTask` lambda):

```kotlin
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
```

Then replace every mutation of the 9 persisted vars in the composable body with its `persist*` equivalent. Key sites to update:

- `settings = ...` → `persistSettings(...)`
- `completedSessions++` → `persistCompletedSessions(completedSessions + 1)` (inside the timer LaunchedEffect)
- `stamps = addFocusStamp(stamps, ...)` → `persistStamps(addFocusStamp(stamps, ...))`
- `selectedSoundId = ...` → `persistSelectedSoundId(...)`
- `darkModePreview = ...` → `persistDarkModePreview(...)`
- `glassReadabilityMode = ...` → `persistGlassReadabilityMode(...)`
- `nextTaskId += 1` inside `addTask` → `persistNextTaskId(nextTaskId + 1)`
- `selectedTodoListId = ...` → `persistSelectedTodoListId(...)`
- `todoLists = ...` inside `addTask`, `toggleTask`, `deleteTask` → `persistTodoLists(...)`

- [ ] **Step 4: Remove now-unused Savers**

Delete these private val declarations at the bottom of the file (they are no longer referenced):
- `TimerSettingsSaver`
- `StampsSaver`
- `TodoListsSaver`
- `GlassReadabilityModeSaver`

Keep `AppTabSaver` and `TimerModeSaver` (still used for the ephemeral `selectedTab` and `timerMode`).

- [ ] **Step 5: Update MainActivity to pass the repo**

Find the file that calls `PurrodoroApp()` and update the call site. Locate it:
```bash
grep -r "PurrodoroApp()" purrodoro-android/app/src/main/java --include="*.kt" -l
```

In `MainActivity.kt` (or equivalent), the call site will look like:
```kotlin
PurrodoroApp()
```

Change it to:
```kotlin
PurrodoroApp(repo = AppRepository.get(this))
```

Add the import:
```kotlin
import com.endiar.purrodoro.data.AppRepository
```

- [ ] **Step 6: Build and verify**

```bash
cd purrodoro-android && ./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Run all unit tests**

```bash
cd purrodoro-android && ./gradlew testDebugUnitTest
```
Expected: All tests PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt \
        app/src/main/java/com/endiar/purrodoro/MainActivity.kt
git commit -m "feat: wire DataStore persistence into PurrodoroApp"
```

---

## Task 4: Manual smoke test

- [ ] **Step 1: Install and verify cold persistence**

```bash
cd purrodoro-android && ./gradlew installDebug
```

1. Open the app.
2. Change a timer setting (e.g. Focus to 50 min).
3. Add a todo item.
4. Force-stop the app: `adb shell am force-stop com.endiar.purrodoro`
5. Reopen the app.
6. Verify the 50-min setting is still showing.
7. Verify the added todo item is still present.

- [ ] **Step 2: Commit (no code change needed if smoke test passes)**

```bash
git tag smoke-test-datastore-ok
```

---

## Notes for the implementer

- `remember(key) { mutableStateOf(key) }` will reset local state on every DataStore emission. This is intentional on cold start. During a live session the DataStore emits once (initial read) and then again only after a `edit {}` call — which is triggered by the same event that mutated local state, so there is no loop.
- Do not add a `LaunchedEffect` that reads from `appState` and writes back to local state — the `remember(key)` handles that. Adding both would cause a write loop.
- The `preferencesDataStore` delegate in `AppRepository.Companion` uses `Context.applicationContext` so it is safe to call from any Activity or Composable context.
- Task titles in `todo_lists_json` may not contain `:` or `;` or `|` or `\n`. If the app later allows arbitrary titles, the encoding must switch to JSON. For now, the default seed data and typical user input will not contain these characters.
