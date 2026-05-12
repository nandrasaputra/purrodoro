package com.endiar.kuropomodoro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassReadabilityMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TodoListState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.net.URLEncoder
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
    return runCatching {
        raw.split(",").associate { pair ->
            val (date, count) = pair.split("=")
            LocalDate.parse(date) to count.toInt()
        }
    }.getOrElse { emptyMap() }
}

internal fun encodeTodoLists(lists: List<TodoListState>): String =
    lists.joinToString("\n") { list ->
        val encodedName = URLEncoder.encode(list.name, "UTF-8")
        val tasksStr = list.tasks.joinToString(";") { t ->
            val encodedTitle = URLEncoder.encode(t.title, "UTF-8")
            "${t.id}:$encodedTitle:${t.minutes}:${t.completed}"
        }
        "${list.id}|$encodedName|$tasksStr"
    }

internal fun decodeTodoLists(raw: String): List<TodoListState> {
    if (raw.isBlank()) return SEED_TODO_LISTS
    return runCatching {
        raw.split("\n").map { line ->
            val parts = line.split("|", limit = 3)
            val listId = parts[0].toInt()
            val name = URLDecoder.decode(parts[1], "UTF-8")
            val tasksRaw = parts.getOrElse(2) { "" }
            val tasks = if (tasksRaw.isBlank()) emptyList()
            else tasksRaw.split(";").map { t ->
                val tp = t.split(":", limit = 4)
                FocusTask(
                    id = tp[0].toInt(),
                    title = URLDecoder.decode(tp[1], "UTF-8"),
                    minutes = tp[2].toInt(),
                    completed = tp[3].toBoolean()
                )
            }
            TodoListState(listId, name, tasks)
        }
    }.getOrElse { SEED_TODO_LISTS }
}
