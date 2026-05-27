package com.endiar.kuropomodoro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.endiar.kuropomodoro.model.AppearanceMode
import com.endiar.kuropomodoro.model.Category
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.GlassReadabilityMode
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitLog
import com.endiar.kuropomodoro.model.HabitLogState
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.HabitTargetType
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimeBlock
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.incrementProgress
import com.endiar.kuropomodoro.model.toggleBinaryOn
import com.endiar.kuropomodoro.model.withLog
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate

data class AppState(
    val glassReadabilityMode: GlassReadabilityMode,
    val appearanceMode: AppearanceMode = AppearanceMode.Auto,
    val selectedSoundId: Int,
    val completedSessions: Int,
    val settings: TimerSettings,
    val nextTaskId: Int,
    val stamps: Map<LocalDate, Int>,
    val isPremium: Boolean = false,
    val sessionRecords: List<SessionRecord> = emptyList(),
    val tasks: List<FocusTask> = emptyList(),
    val categories: List<Category> = emptyList(),
    val nextCategoryId: Int = 1,
    val selectedCategoryId: Int? = null,
    val notificationSoundEnabled: Boolean = true,
    val habits: List<Habit> = emptyList(),
    val nextHabitId: Int = 1,
)


class AppRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_GLASS_MODE = stringPreferencesKey("glass_readability_mode")
        private val KEY_SOUND_ID = intPreferencesKey("selected_sound_id")
        private val KEY_COMPLETED = intPreferencesKey("completed_sessions")
        private val KEY_FOCUS_MIN = intPreferencesKey("focus_minutes")
        private val KEY_SHORT_MIN = intPreferencesKey("short_break_minutes")
        private val KEY_LONG_MIN = intPreferencesKey("long_break_minutes")
        private val KEY_AUTO_START_NEXT_SESSION = booleanPreferencesKey("auto_start_next_session")
        private val KEY_CYCLE_SIZE = intPreferencesKey("focus_sessions_per_cycle")
        private val KEY_NEXT_TASK_ID = intPreferencesKey("next_task_id")
        private val KEY_STAMPS = stringPreferencesKey("stamps_json")
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_SESSION_RECORDS = stringPreferencesKey("session_records_json")
        private val KEY_RUNTIME_SNAPSHOT = stringPreferencesKey("timer_runtime_snapshot")
        private val KEY_PENDING_CYCLE_ACK = booleanPreferencesKey("pending_cycle_completion_ack")
        private val KEY_TASKS = stringPreferencesKey("tasks_json")
        private val KEY_CATEGORIES = stringPreferencesKey("categories_json")
        private val KEY_NEXT_CATEGORY_ID = intPreferencesKey("next_category_id")
        private val KEY_SELECTED_CATEGORY_ID = stringPreferencesKey("selected_category_id")
        private val KEY_APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        private val KEY_NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound_enabled")
        private val KEY_HABITS = stringPreferencesKey("habits_json")
        private val KEY_NEXT_HABIT_ID = intPreferencesKey("next_habit_id")
        private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

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

    /**
     * True once the user has finished or skipped onboarding.
     *
     * For a brand-new install the key is absent and we resolve to false so the
     * onboarding flow plays. For users upgrading from a build that predates
     * onboarding, we treat existing tasks / habits / session records as proof
     * they've already learned the app, so they don't get an unexpected first-run
     * flow on next launch.
     */
    val hasSeenOnboardingFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_ONBOARDING] ?: hasPriorAppUsage(prefs)
    }

    private fun hasPriorAppUsage(prefs: Preferences): Boolean {
        val records = prefs[KEY_SESSION_RECORDS]
        val tasks = prefs[KEY_TASKS]
        val habits = prefs[KEY_HABITS]
        fun nonEmpty(value: String?): Boolean =
            !value.isNullOrBlank() && value != "[]"
        return nonEmpty(records) || nonEmpty(tasks) || nonEmpty(habits)
    }

    val appState: Flow<AppState> = dataStore.data.map { prefs ->
        AppState(
            glassReadabilityMode = prefs[KEY_GLASS_MODE]
                ?.let { runCatching { GlassReadabilityMode.valueOf(it) }.getOrNull() }
                ?: GlassReadabilityMode.Adaptive,
            appearanceMode = prefs[KEY_APPEARANCE_MODE]
                ?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
                ?: AppearanceMode.Auto,
            selectedSoundId = prefs[KEY_SOUND_ID] ?: 1,
            completedSessions = prefs[KEY_COMPLETED] ?: 0,
            settings = TimerSettings(
                focusMinutes = prefs[KEY_FOCUS_MIN] ?: 25,
                shortBreakMinutes = prefs[KEY_SHORT_MIN] ?: 5,
                longBreakMinutes = prefs[KEY_LONG_MIN] ?: 15,
                autoStartNextSession = prefs[KEY_AUTO_START_NEXT_SESSION] ?: false,
                focusSessionsPerCycle = prefs[KEY_CYCLE_SIZE] ?: 4
            ),
            nextTaskId = prefs[KEY_NEXT_TASK_ID] ?: 1,
            stamps = decodeStamps(prefs[KEY_STAMPS] ?: ""),
            isPremium = prefs[KEY_IS_PREMIUM] ?: false,
            sessionRecords = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: ""),
            tasks = decodeTasks(prefs[KEY_TASKS] ?: ""),
            categories = decodeCategories(prefs[KEY_CATEGORIES] ?: ""),
            nextCategoryId = prefs[KEY_NEXT_CATEGORY_ID] ?: 1,
            selectedCategoryId = prefs[KEY_SELECTED_CATEGORY_ID]
                ?.takeIf { it.isNotBlank() }
                ?.toIntOrNull(),
            notificationSoundEnabled = prefs[KEY_NOTIFICATION_SOUND] ?: true,
            habits = decodeHabits(prefs[KEY_HABITS] ?: ""),
            nextHabitId = prefs[KEY_NEXT_HABIT_ID] ?: 1,
        )
    }

    suspend fun setGlassReadabilityMode(value: GlassReadabilityMode) = dataStore.edit { it[KEY_GLASS_MODE] = value.name }
    suspend fun setAppearanceMode(value: AppearanceMode) = dataStore.edit { it[KEY_APPEARANCE_MODE] = value.name }
    suspend fun setSelectedSoundId(value: Int) = dataStore.edit { it[KEY_SOUND_ID] = value }
    suspend fun setCompletedSessions(value: Int) = dataStore.edit { it[KEY_COMPLETED] = value }
    suspend fun setSettings(value: TimerSettings) = dataStore.edit { prefs ->
        prefs[KEY_FOCUS_MIN] = value.focusMinutes
        prefs[KEY_SHORT_MIN] = value.shortBreakMinutes
        prefs[KEY_LONG_MIN] = value.longBreakMinutes
        prefs[KEY_AUTO_START_NEXT_SESSION] = value.autoStartNextSession
        prefs[KEY_CYCLE_SIZE] = value.focusSessionsPerCycle
    }
    suspend fun setNextTaskId(value: Int) = dataStore.edit { it[KEY_NEXT_TASK_ID] = value }
    suspend fun setStamps(value: Map<LocalDate, Int>) = dataStore.edit { it[KEY_STAMPS] = encodeStamps(value) }
    suspend fun setIsPremium(value: Boolean) = dataStore.edit { it[KEY_IS_PREMIUM] = value }
    suspend fun setPendingCycleCompletionAck(value: Boolean) =
        dataStore.edit { it[KEY_PENDING_CYCLE_ACK] = value }

    suspend fun setHasSeenOnboarding(value: Boolean) =
        dataStore.edit { it[KEY_HAS_SEEN_ONBOARDING] = value }

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

    suspend fun setNotificationSoundEnabled(value: Boolean) =
        dataStore.edit { it[KEY_NOTIFICATION_SOUND] = value }

    suspend fun addCategory(name: String) = dataStore.edit { prefs ->
        val nextId = prefs[KEY_NEXT_CATEGORY_ID] ?: 1
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
        val selected = prefs[KEY_SELECTED_CATEGORY_ID]?.toIntOrNull()
        if (selected == id) prefs.remove(KEY_SELECTED_CATEGORY_ID)
    }

    /**
     * Sets the completed flag on a single task by id.
     *
     * Used by the cycle-end auto-complete path in [TimerStateMachine] (and the
     * skip-final mirror in TimerService.doSkip) when a real task ([FocusTask.id] > 0)
     * has been attached to a Pomodoro cycle that finished successfully.
     *
     * Replaces the now-removed `incrementTaskCompletedSessions`: tasks no longer
     * carry a per-task pomodoro target, so per-segment session counting against
     * the task is gone — completion is binary and triggered by cycle success.
     */
    suspend fun setTaskCompleted(taskId: Int, completed: Boolean) = dataStore.edit { prefs ->
        val existing = decodeTasks(prefs[KEY_TASKS] ?: "")
        prefs[KEY_TASKS] = encodeTasks(
            existing.map { if (it.id == taskId) it.copy(completed = completed) else it }
        )
    }

    suspend fun deleteSessionRecord(id: Long) = dataStore.edit { prefs ->
        val existing = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: "")
        prefs[KEY_SESSION_RECORDS] = encodeSessionRecords(existing.filterNot { it.id == id })
    }

    suspend fun resetSettingsToDefault() = dataStore.edit { prefs ->
        prefs[KEY_FOCUS_MIN] = 25
        prefs[KEY_SHORT_MIN] = 5
        prefs[KEY_LONG_MIN] = 15
        prefs[KEY_CYCLE_SIZE] = 4
    }

    // ── Habit mutators ────────────────────────────────────────────────────────

    suspend fun addHabit(
        name: String,
        emoji: String,
        colorArgb: Long,
        targetType: HabitTargetType = HabitTargetType.Binary,
        targetValue: Int = 1,
        occurrence: HabitOccurrence = HabitOccurrence.Daily,
        timeBlock: TimeBlock = TimeBlock.Anytime,
    ) = dataStore.edit { prefs ->
        val nextId = prefs[KEY_NEXT_HABIT_ID] ?: 1
        val existing = decodeHabits(prefs[KEY_HABITS] ?: "")
        val habit = Habit(
            id = nextId, name = name, emoji = emoji, colorArgb = colorArgb,
            createdAt = System.currentTimeMillis(),
            targetType = targetType, targetValue = targetValue,
            occurrence = occurrence, timeBlock = timeBlock,
        )
        prefs[KEY_HABITS] = encodeHabits(existing + habit)
        prefs[KEY_NEXT_HABIT_ID] = nextId + 1
    }

    suspend fun deleteHabit(id: Int) = dataStore.edit { prefs ->
        val existing = decodeHabits(prefs[KEY_HABITS] ?: "")
        prefs[KEY_HABITS] = encodeHabits(existing.filterNot { it.id == id })
    }

    suspend fun updateHabitMeta(
        id: Int,
        name: String,
        emoji: String,
        colorArgb: Long,
        targetType: HabitTargetType,
        targetValue: Int,
        occurrence: HabitOccurrence,
        timeBlock: TimeBlock,
    ) = mutateHabit(id) {
        it.copy(name = name, emoji = emoji, colorArgb = colorArgb,
            targetType = targetType, targetValue = targetValue,
            occurrence = occurrence, timeBlock = timeBlock)
    }

    suspend fun toggleHabitBinaryCompletion(id: Int, date: LocalDate) =
        mutateHabit(id) { it.toggleBinaryOn(date) }

    suspend fun incrementHabitProgress(id: Int, date: LocalDate, delta: Int = 1) =
        mutateHabit(id) { it.incrementProgress(date, delta) }

    suspend fun setHabitSkipped(id: Int, date: LocalDate, skipped: Boolean) =
        mutateHabit(id) {
            if (skipped) it.withLog(date, HabitLogState.Skipped) else it.withLog(date, null)
        }

    suspend fun clearHabitLog(id: Int, date: LocalDate) =
        mutateHabit(id) { it.withLog(date, null) }

    private suspend fun mutateHabit(id: Int, transform: (Habit) -> Habit) =
        dataStore.edit { prefs ->
            val existing = decodeHabits(prefs[KEY_HABITS] ?: "")
            prefs[KEY_HABITS] = encodeHabits(
                existing.map { if (it.id == id) transform(it) else it }
            )
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

// Schema, pipe-separated per record:
//   0:id 1:startedAt 2:endedAt 3:focusMinutes 4:shortBreakMinutes 5:longBreakMinutes
//   6:completedFocusCount 7:totalFocusCount 8:status 9:taskEncoded
//   10:completedShortBreakCount (v2+) 11:completedLongBreakCount (v2+)
//
// Fields 10/11 were added in v2. Older records have 10 fields; the decoder defaults
// the new counts to 0 so previously-saved sessions stay readable without migration.
internal fun encodeSessionRecords(records: List<SessionRecord>): String =
    records.joinToString("\n") { r ->
        val taskEncoded = if (r.taskName != null) URLEncoder.encode(r.taskName, "UTF-8") else ""
        listOf(
            r.id, r.startedAt, r.endedAt,
            r.focusMinutes, r.shortBreakMinutes, r.longBreakMinutes,
            r.completedFocusCount, r.totalFocusCount,
            r.status.name, taskEncoded,
            r.completedShortBreakCount, r.completedLongBreakCount
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
                taskName = if (p[9].isBlank()) null else URLDecoder.decode(p[9], "UTF-8"),
                completedShortBreakCount = p.getOrNull(10)?.toIntOrNull() ?: 0,
                completedLongBreakCount = p.getOrNull(11)?.toIntOrNull() ?: 0
            )
        }
    }.getOrElse { emptyList() }
}

// --- Runtime snapshot encoding ---
// Schema (single line, pipe-separated):
//   v4 (9 fields): <timerState>|<currentIndex>|<remainingSeconds>|<endsAtMillisOrEmpty>|<itemsCsv>|<taskOrEmpty>|<openRecordOrEmpty>|<activeHabitIdOrEmpty>|<focusType>
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
            r.status.name, encodedTask,
            r.completedShortBreakCount, r.completedLongBreakCount
        ).joinToString(";")
    } ?: ""
    return listOf(
        state.session.timerState.name,
        state.session.currentIndex,
        state.session.remainingSeconds,
        state.endsAtMillis ?: "",
        itemsCsv,
        taskField,
        recordField,
        state.activeHabitId?.toString() ?: "",
        state.focusType.name,
    ).joinToString("|")
}

internal fun decodeRuntimeSnapshot(raw: String): TimerRuntimeState? {
    if (raw.isBlank()) return null
    return runCatching {
        val parts = raw.split("|", limit = 9)
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
                taskName = if (rp[9].isBlank()) null else URLDecoder.decode(rp[9], "UTF-8"),
                completedShortBreakCount = rp.getOrNull(10)?.toIntOrNull() ?: 0,
                completedLongBreakCount = rp.getOrNull(11)?.toIntOrNull() ?: 0
            )
        }
        val activeHabitId = parts.getOrNull(7)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        val focusType = parts.getOrNull(8)
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { FocusType.valueOf(it) }.getOrNull() }
            ?: FocusType.Empty
        TimerRuntimeState(
            session = TimerSession(items, currentIndex, remainingSeconds, timerState),
            endsAtMillis = endsAtMillis,
            activeTask = task,
            openRecord = record,
            activeHabitId = activeHabitId,
            focusType = focusType
        )
    }.getOrNull()
}

// --- Task encoding ---
// Each task: "<id>:<urlTitle>:<minutes>:<completed>:<plannedSessions>:<completedSessions>:<categoryIdOrEmpty>"
internal fun encodeTasks(tasks: List<FocusTask>): String =
    if (tasks.isEmpty()) "[]"
    else tasks.joinToString("\n") { t ->
        val encodedTitle = URLEncoder.encode(t.title, "UTF-8")
        val catField = t.categoryId?.toString() ?: ""
        "${t.id}:$encodedTitle:${t.minutes}:${t.completed}:${t.plannedSessions}:${t.completedSessions}:$catField"
    }

internal fun decodeTasks(raw: String): List<FocusTask> {
    if (raw.isBlank()) return emptyList()
    if (raw == "[]") return emptyList()
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
    }.getOrElse { emptyList() }
}

// --- Category encoding ---
// Each category: "<id>:<urlName>"
internal fun encodeCategories(categories: List<Category>): String =
    if (categories.isEmpty()) "[]"
    else categories.joinToString("\n") { c ->
        "${c.id}:${URLEncoder.encode(c.name, "UTF-8")}"
    }

internal fun decodeCategories(raw: String): List<Category> {
    if (raw.isBlank()) return emptyList()
    if (raw == "[]") return emptyList()
    return runCatching {
        raw.split("\n").map { line ->
            val p = line.split(":", limit = 2)
            Category(p[0].toInt(), URLDecoder.decode(p[1], "UTF-8"))
        }
    }.getOrElse { emptyList() }
}

// ─── Habit encoding ───────────────────────────────────────────────────────────
//
// v2 schema — one habit per line, 11 pipe-separated fields:
//   0:id  1:urlName  2:urlEmoji  3:colorArgb  4:createdAt
//   5:targetTag  ("B"|"Q"|"P")
//   6:targetValue
//   7:occurrenceTag  ("D" daily | "W" weekdays)
//   8:occurrenceData (empty for daily; "MON,WED,FRI" for weekdays)
//   9:timeBlockTag   ("M"|"A"|"E"|"X")
//   10:logsCsv       comma-separated "<isoDate>~<tag>[~progress]"
//                    tags: "C" Completed | "S" Skipped | "P" Partial
//
// v1 legacy (6 fields): id|urlName|urlEmoji|colorArgb|createdAt|completionDatesCsv
// Detected on read and migrated to Binary/Daily/Anytime with Completed logs.

internal fun encodeHabits(habits: List<Habit>): String =
    if (habits.isEmpty()) "[]"
    else habits.joinToString("\n") { h ->
        val tTag = when (h.targetType) {
            HabitTargetType.Binary -> "B"; HabitTargetType.Quantity -> "Q"
            HabitTargetType.Pomodoro -> "P"
        }
        val (oTag, oData) = when (val o = h.occurrence) {
            HabitOccurrence.Daily -> "D" to ""
            is HabitOccurrence.Weekdays -> "W" to o.days.sortedBy { it.value }.joinToString(",") { it.name }
        }
        val bTag = when (h.timeBlock) {
            TimeBlock.Morning -> "M"; TimeBlock.Afternoon -> "A"
            TimeBlock.Evening -> "E"; TimeBlock.Anytime -> "X"
        }
        val logsCsv = h.logs.entries.sortedBy { it.key }.joinToString(",") { (date, log) ->
            when (val s = log.state) {
                HabitLogState.Completed -> "$date~C"
                HabitLogState.Skipped   -> "$date~S"
                is HabitLogState.Partial -> "$date~P~${s.progress}"
            }
        }
        listOf(
            h.id, URLEncoder.encode(h.name, "UTF-8"), URLEncoder.encode(h.emoji, "UTF-8"),
            h.colorArgb, h.createdAt,
            tTag, h.targetValue, oTag, oData, bTag, logsCsv
        ).joinToString("|")
    }

internal fun decodeHabits(raw: String): List<Habit> {
    if (raw.isBlank() || raw == "[]") return emptyList()
    return runCatching {
        raw.split("\n").mapNotNull { line ->
            val p = line.split("|")
            when {
                p.size >= 11 -> decodeHabitV2(p)
                p.size in 5..6 -> decodeHabitV1(p)
                else -> null
            }
        }
    }.getOrElse { emptyList() }
}

private fun decodeHabitV2(p: List<String>): Habit {
    val targetType = when (p[5]) {
        "Q" -> HabitTargetType.Quantity; "P" -> HabitTargetType.Pomodoro
        else -> HabitTargetType.Binary
    }
    val occurrence: HabitOccurrence = when (p[7]) {
        "W" -> {
            val days = p[8].split(",").filter { it.isNotBlank() }
                .mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
            if (days.isEmpty()) HabitOccurrence.Daily else HabitOccurrence.Weekdays(days)
        }
        else -> HabitOccurrence.Daily
    }
    val timeBlock = when (p[9]) {
        "M" -> TimeBlock.Morning; "A" -> TimeBlock.Afternoon
        "E" -> TimeBlock.Evening; else -> TimeBlock.Anytime
    }
    val logs: Map<LocalDate, HabitLog> = p.getOrNull(10)
        ?.takeIf { it.isNotBlank() }
        ?.split(",")
        ?.mapNotNull { entry ->
            val parts = entry.split("~")
            if (parts.size < 2) return@mapNotNull null
            val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val state: HabitLogState = when (parts[1]) {
                "C" -> HabitLogState.Completed
                "S" -> HabitLogState.Skipped
                "P" -> HabitLogState.Partial(parts.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(0) ?: 0)
                else -> return@mapNotNull null
            }
            date to HabitLog(state)
        }
        ?.toMap() ?: emptyMap()
    return Habit(
        id = p[0].toInt(),
        name = URLDecoder.decode(p[1], "UTF-8"),
        emoji = URLDecoder.decode(p[2], "UTF-8"),
        colorArgb = p[3].toLong(),
        createdAt = p[4].toLong(),
        targetType = targetType,
        targetValue = p[6].toIntOrNull()?.coerceAtLeast(1) ?: 1,
        occurrence = occurrence, timeBlock = timeBlock, logs = logs,
    )
}

private fun decodeHabitV1(p: List<String>): Habit {
    val completions = p.getOrNull(5)
        ?.takeIf { it.isNotBlank() }
        ?.split(",")
        ?.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.associateWith { HabitLog(HabitLogState.Completed) }
        ?: emptyMap()
    return Habit(
        id = p[0].toInt(),
        name = URLDecoder.decode(p[1], "UTF-8"),
        emoji = URLDecoder.decode(p[2], "UTF-8"),
        colorArgb = p[3].toLong(),
        createdAt = p[4].toLong(),
        logs = completions,
    )
}
