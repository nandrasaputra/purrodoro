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
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.TodoListState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate

data class AppState(
    val glassReadabilityMode: GlassReadabilityMode,
    val selectedSoundId: Int,
    val completedSessions: Int,
    val settings: TimerSettings,
    val nextTaskId: Int,
    val selectedTodoListId: Int,
    val stamps: Map<LocalDate, Int>,
    val todoLists: List<TodoListState>,
    val isPremium: Boolean = false,
    val sessionRecords: List<SessionRecord> = emptyList()
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
        private val KEY_GLASS_MODE = stringPreferencesKey("glass_readability_mode")
        private val KEY_SOUND_ID = intPreferencesKey("selected_sound_id")
        private val KEY_COMPLETED = intPreferencesKey("completed_sessions")
        private val KEY_FOCUS_MIN = intPreferencesKey("focus_minutes")
        private val KEY_SHORT_MIN = intPreferencesKey("short_break_minutes")
        private val KEY_LONG_MIN = intPreferencesKey("long_break_minutes")
        private val KEY_CYCLE_SIZE = intPreferencesKey("focus_sessions_per_cycle")
        private val KEY_NEXT_TASK_ID = intPreferencesKey("next_task_id")
        private val KEY_SELECTED_LIST = intPreferencesKey("selected_todo_list_id")
        private val KEY_STAMPS = stringPreferencesKey("stamps_json")
        private val KEY_TODOS = stringPreferencesKey("todo_lists_json")
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_SESSION_RECORDS = stringPreferencesKey("session_records_json")
        private val KEY_RUNTIME_SNAPSHOT = stringPreferencesKey("timer_runtime_snapshot")
        private val KEY_PENDING_CYCLE_ACK = booleanPreferencesKey("pending_cycle_completion_ack")

        // Singleton instance for production use
        private val Context.dataStore by preferencesDataStore(name = "kuro_app_prefs")

        @Volatile private var INSTANCE: AppRepository? = null

        fun get(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext.dataStore).also { INSTANCE = it }
            }
    }

    val runtimeSnapshot: Flow<TimerRuntimeState?> = dataStore.data.map { prefs ->
        decodeRuntimeSnapshot(prefs[KEY_RUNTIME_SNAPSHOT] ?: "")
    }

    val pendingCycleCompletionAckFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PENDING_CYCLE_ACK] ?: false
    }

    val appState: Flow<AppState> = dataStore.data.map { prefs ->
        AppState(
            glassReadabilityMode = prefs[KEY_GLASS_MODE]
                ?.let { runCatching { GlassReadabilityMode.valueOf(it) }.getOrNull() }
                ?: GlassReadabilityMode.Adaptive,
            selectedSoundId = prefs[KEY_SOUND_ID] ?: 1,
            completedSessions = prefs[KEY_COMPLETED] ?: 0,
            settings = TimerSettings(
                focusMinutes = prefs[KEY_FOCUS_MIN] ?: 25,
                shortBreakMinutes = prefs[KEY_SHORT_MIN] ?: 5,
                longBreakMinutes = prefs[KEY_LONG_MIN] ?: 15,
                focusSessionsPerCycle = prefs[KEY_CYCLE_SIZE] ?: 4
            ),
            nextTaskId = prefs[KEY_NEXT_TASK_ID] ?: 7,
            selectedTodoListId = prefs[KEY_SELECTED_LIST] ?: 1,
            stamps = decodeStamps(prefs[KEY_STAMPS] ?: ""),
            todoLists = decodeTodoLists(prefs[KEY_TODOS] ?: ""),
            isPremium = prefs[KEY_IS_PREMIUM] ?: false,
            sessionRecords = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: ""),
        )
    }

    suspend fun setGlassReadabilityMode(value: GlassReadabilityMode) = dataStore.edit { it[KEY_GLASS_MODE] = value.name }
    suspend fun setSelectedSoundId(value: Int) = dataStore.edit { it[KEY_SOUND_ID] = value }
    suspend fun setCompletedSessions(value: Int) = dataStore.edit { it[KEY_COMPLETED] = value }
    suspend fun setSettings(value: TimerSettings) = dataStore.edit { prefs ->
        prefs[KEY_FOCUS_MIN] = value.focusMinutes
        prefs[KEY_SHORT_MIN] = value.shortBreakMinutes
        prefs[KEY_LONG_MIN] = value.longBreakMinutes
        prefs[KEY_CYCLE_SIZE] = value.focusSessionsPerCycle
    }
    suspend fun setNextTaskId(value: Int) = dataStore.edit { it[KEY_NEXT_TASK_ID] = value }
    suspend fun setSelectedTodoListId(value: Int) = dataStore.edit { it[KEY_SELECTED_LIST] = value }
    suspend fun setStamps(value: Map<LocalDate, Int>) = dataStore.edit { it[KEY_STAMPS] = encodeStamps(value) }
    suspend fun setTodoLists(value: List<TodoListState>) = dataStore.edit { it[KEY_TODOS] = encodeTodoLists(value) }
    suspend fun setIsPremium(value: Boolean) = dataStore.edit { it[KEY_IS_PREMIUM] = value }
    suspend fun setPendingCycleCompletionAck(value: Boolean) =
        dataStore.edit { it[KEY_PENDING_CYCLE_ACK] = value }

    suspend fun appendSessionRecord(record: SessionRecord) = dataStore.edit { prefs ->
        val existing = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: "")
        prefs[KEY_SESSION_RECORDS] = encodeSessionRecords(existing + record)
    }

    suspend fun setRuntimeSnapshot(snapshot: TimerRuntimeState?) = dataStore.edit { prefs ->
        if (snapshot == null) {
            prefs.remove(KEY_RUNTIME_SNAPSHOT)
        } else {
            prefs[KEY_RUNTIME_SNAPSHOT] = encodeRuntimeSnapshot(snapshot)
        }
    }

    suspend fun incrementStampFor(date: LocalDate) = dataStore.edit { prefs ->
        val existing = decodeStamps(prefs[KEY_STAMPS] ?: "")
        val updated = existing + (date to (existing[date] ?: 0) + 1)
        prefs[KEY_STAMPS] = encodeStamps(updated)
    }
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

internal fun encodeSessionRecords(records: List<SessionRecord>): String =
    records.joinToString("\n") { r ->
        val taskEncoded = if (r.taskName != null) URLEncoder.encode(r.taskName, "UTF-8") else ""
        listOf(
            r.id, r.startedAt, r.endedAt,
            r.focusMinutes, r.shortBreakMinutes, r.longBreakMinutes,
            r.completedFocusCount, r.totalFocusCount,
            r.status.name, taskEncoded
        ).joinToString("|")
    }

internal fun decodeSessionRecords(raw: String): List<SessionRecord> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        raw.split("\n").mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 10) return@mapNotNull null
            SessionRecord(
                id = p[0].toLong(),
                startedAt = p[1].toLong(),
                endedAt = p[2].toLong(),
                focusMinutes = p[3].toInt(),
                shortBreakMinutes = p[4].toInt(),
                longBreakMinutes = p[5].toInt(),
                completedFocusCount = p[6].toInt(),
                totalFocusCount = p[7].toInt(),
                status = SessionStatus.valueOf(p[8]),
                taskName = if (p[9].isBlank()) null else URLDecoder.decode(p[9], "UTF-8")
            )
        }
    }.getOrElse { emptyList() }
}

// --- Runtime snapshot encoding ---
// Schema (single line, pipe-separated, 7 fields):
// <timerState>|<currentIndex>|<remainingSeconds>|<endsAtMillisOrEmpty>|<itemsCsv>|<taskOrEmpty>|<openRecordOrEmpty>
//
// itemsCsv: comma-separated, each token is "F:<sec>", "S:<sec>", or "L:<sec>"
// taskOrEmpty: "<id>:<urlTitle>:<minutes>:<completed>:<plannedSessions>:<completedSessions>"
// openRecordOrEmpty: fields joined by ";" to avoid conflict with the outer "|" separator

internal fun encodeRuntimeSnapshot(state: TimerRuntimeState): String {
    val itemsCsv = state.session.items.joinToString(",") { item ->
        when (val t = item.type) {
            is SessionItemType.Focus -> "F:${t.seconds}"
            is SessionItemType.ShortBreak -> "S:${t.seconds}"
            is SessionItemType.LongBreak -> "L:${t.seconds}"
        }
    }
    val taskField = state.activeTask?.let { t ->
        val encodedTitle = URLEncoder.encode(t.title, "UTF-8")
        "${t.id}:$encodedTitle:${t.minutes}:${t.completed}:${t.plannedSessions}:${t.completedSessions}"
    } ?: ""
    val recordField = state.openRecord?.let { r ->
        val encodedTask = if (r.taskName != null) URLEncoder.encode(r.taskName, "UTF-8") else ""
        listOf(
            r.id, r.startedAt, r.endedAt,
            r.focusMinutes, r.shortBreakMinutes, r.longBreakMinutes,
            r.completedFocusCount, r.totalFocusCount,
            r.status.name, encodedTask
        ).joinToString(";")
    } ?: ""
    return listOf(
        state.session.timerState.name,
        state.session.currentIndex,
        state.session.remainingSeconds,
        state.endsAtMillis ?: "",
        itemsCsv,
        taskField,
        recordField
    ).joinToString("|")
}

internal fun decodeRuntimeSnapshot(raw: String): TimerRuntimeState? {
    if (raw.isBlank()) return null
    return runCatching {
        val parts = raw.split("|", limit = 7)
        val timerState = TimerState.valueOf(parts[0])
        val currentIndex = parts[1].toInt()
        val remainingSeconds = parts[2].toInt()
        val endsAtMillis = parts[3].takeIf { it.isNotBlank() }?.toLong()
        val items = parts[4].split(",").map { token ->
            val (tag, sec) = token.split(":")
            val seconds = sec.toInt()
            TimerSessionItem(
                when (tag) {
                    "S" -> SessionItemType.ShortBreak(seconds)
                    "L" -> SessionItemType.LongBreak(seconds)
                    else -> SessionItemType.Focus(seconds)
                }
            )
        }
        val task = parts.getOrNull(5)?.takeIf { it.isNotBlank() }?.let { t ->
            val tp = t.split(":", limit = 6)
            FocusTask(
                id = tp[0].toInt(),
                title = URLDecoder.decode(tp[1], "UTF-8"),
                minutes = tp[2].toInt(),
                completed = tp[3].toBoolean(),
                plannedSessions = tp[4].toInt(),
                completedSessions = tp[5].toInt()
            )
        }
        val record = parts.getOrNull(6)?.takeIf { it.isNotBlank() }?.let { r ->
            val rp = r.split(";")
            if (rp.size < 10) return@let null
            SessionRecord(
                id = rp[0].toLong(),
                startedAt = rp[1].toLong(),
                endedAt = rp[2].toLong(),
                focusMinutes = rp[3].toInt(),
                shortBreakMinutes = rp[4].toInt(),
                longBreakMinutes = rp[5].toInt(),
                completedFocusCount = rp[6].toInt(),
                totalFocusCount = rp[7].toInt(),
                status = SessionStatus.valueOf(rp[8]),
                taskName = if (rp[9].isBlank()) null else URLDecoder.decode(rp[9], "UTF-8")
            )
        }
        TimerRuntimeState(
            session = TimerSession(items, currentIndex, remainingSeconds, timerState),
            endsAtMillis = endsAtMillis,
            activeTask = task,
            openRecord = record
        )
    }.getOrNull()
}
