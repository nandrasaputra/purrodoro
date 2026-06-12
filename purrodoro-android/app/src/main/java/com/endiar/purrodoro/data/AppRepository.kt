package com.endiar.purrodoro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.endiar.purrodoro.data.local.CategoryEntity
import com.endiar.purrodoro.data.local.DailyStampEntity
import com.endiar.purrodoro.data.local.HabitEntity
import com.endiar.purrodoro.data.local.HabitLogEntity
import com.endiar.purrodoro.data.local.LegacyBlobs
import com.endiar.purrodoro.data.local.LocalStoreMigration
import com.endiar.purrodoro.data.local.MigrationSink
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.data.local.SessionRecordEntity
import com.endiar.purrodoro.data.local.TaskEntity
import com.endiar.purrodoro.data.local.toDomain
import com.endiar.purrodoro.data.local.toEntity
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.HabitLog
import com.endiar.purrodoro.model.HabitLogState
import com.endiar.purrodoro.model.HabitOccurrence
import com.endiar.purrodoro.model.InterruptedFocus
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimeBlock
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.model.incrementProgress
import com.endiar.purrodoro.model.toggleBinaryOn
import com.endiar.purrodoro.model.withLog
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate
import java.util.UUID

data class AppState(
    val glassReadabilityMode: GlassReadabilityMode,
    val appearanceMode: AppearanceMode = AppearanceMode.Auto,
    val language: AppLanguage = AppLanguage.System,
    val userName: String = "",
    val selectedSoundId: Int,
    val soundscapeMixAmbienceId: Int? = null,
    val soundscapeMixMusicId: Int? = null,
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
    val interruptedFocus: InterruptedFocus? = null,
)


class AppRepository(
    private val dataStore: DataStore<Preferences>,
    private val db: PurrodoroDatabase? = null,
) {

    companion object {
        private val KEY_GLASS_MODE = stringPreferencesKey("glass_readability_mode")
        private val KEY_SOUND_ID = intPreferencesKey("selected_sound_id")
        private val KEY_SOUNDSCAPE_MIX_AMBIENCE = stringPreferencesKey("soundscape_mix_ambience_id")
        private val KEY_SOUNDSCAPE_MIX_MUSIC = stringPreferencesKey("soundscape_mix_music_id")
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
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound_enabled")
        private val KEY_HABITS = stringPreferencesKey("habits_json")
        private val KEY_NEXT_HABIT_ID = intPreferencesKey("next_habit_id")
        private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        private val KEY_HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
        private val KEY_INTERRUPTED_FOCUS = stringPreferencesKey("interrupted_focus")
        private val KEY_MIGRATED_TO_ROOM = booleanPreferencesKey("has_migrated_to_room")

        // Marks which account the on-device data belongs to. Null = unclaimed
        // (guest data). Survives sign-out; drives the Claim/Keep/Drop rule on the
        // next sign-in. See the auth-and-account-identity spec.
        private val KEY_OWNER_ID = stringPreferencesKey("data_owner_id")

        // Singleton instance for production use
        private val Context.dataStore by preferencesDataStore(name = "kuro_app_prefs")

        @Volatile private var INSTANCE: AppRepository? = null

        fun get(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(
                    context.applicationContext.dataStore,
                    PurrodoroDatabase.get(context),
                ).also { INSTANCE = it }
            }
    }

    private val roomDb: PurrodoroDatabase
        get() = checkNotNull(db) {
            "This AppRepository was constructed without a Room database; domain data is unavailable."
        }

    private fun newUuid(): String = UUID.randomUUID().toString()

    private val migrationMutex = Mutex()
    @Volatile private var migrationDone = false

    /**
     * One-time DataStore → Room migration gate. Safe to call from any entry
     * point (activity, widget, timer service): the first caller performs the
     * migration, everyone else waits on the mutex and then no-ops. Once the
     * [KEY_MIGRATED_TO_ROOM] flag is set the gate is a cheap volatile read.
     *
     * Crash safety: the Room tables are cleared before writing, and the flag +
     * legacy-key removal commit in a single DataStore edit after the Room
     * transaction, so a crash mid-migration re-runs it from scratch.
     */
    suspend fun ensureMigrated() {
        val db = db ?: return
        if (migrationDone) return
        migrationMutex.withLock {
            if (migrationDone) return
            val prefs = dataStore.data.first()
            if (prefs[KEY_MIGRATED_TO_ROOM] == true) {
                migrationDone = true
                return
            }
            db.withTransaction {
                db.taskDao().clearAll()
                db.categoryDao().clearAll()
                db.habitDao().clearAll()
                db.habitLogDao().clearAll()
                db.sessionRecordDao().clearAll()
                db.dailyStampDao().clearAll()
            }
            val legacy = LegacyBlobs(
                tasksRaw = prefs[KEY_TASKS] ?: "",
                categoriesRaw = prefs[KEY_CATEGORIES] ?: "",
                habitsRaw = prefs[KEY_HABITS] ?: "",
                recordsRaw = prefs[KEY_SESSION_RECORDS] ?: "",
                stampsRaw = prefs[KEY_STAMPS] ?: "",
            )
            LocalStoreMigration().migrate(legacy, RoomMigrationSink(db))
            // The legacy keys hasPriorAppUsage reads are about to be deleted,
            // so stamp its upgrade conclusion into the explicit flags now.
            val hadDomainData = hasPriorAppUsage(prefs)
            dataStore.edit { p ->
                if (hadDomainData) {
                    if (p[KEY_HAS_SEEN_ONBOARDING] == null) p[KEY_HAS_SEEN_ONBOARDING] = true
                    if (p[KEY_HAS_SEEN_TUTORIAL] == null) p[KEY_HAS_SEEN_TUTORIAL] = true
                }
                listOf(KEY_TASKS, KEY_CATEGORIES, KEY_HABITS, KEY_SESSION_RECORDS, KEY_STAMPS)
                    .forEach { p.remove(it) }
                p[KEY_MIGRATED_TO_ROOM] = true
            }
            migrationDone = true
        }
    }

    private class RoomMigrationSink(private val db: PurrodoroDatabase) : MigrationSink {
        override suspend fun write(
            tasks: List<TaskEntity>,
            categories: List<CategoryEntity>,
            habits: List<HabitEntity>,
            habitLogs: List<HabitLogEntity>,
            records: List<SessionRecordEntity>,
            stamps: List<DailyStampEntity>,
        ) {
            db.withTransaction {
                db.categoryDao().upsert(categories)
                db.taskDao().upsert(tasks)
                db.habitDao().upsert(habits)
                db.habitLogDao().upsert(habitLogs)
                db.sessionRecordDao().upsert(records)
                db.dailyStampDao().upsert(stamps)
            }
        }
    }

    val runtimeSnapshot: Flow<TimerRuntimeState?> = dataStore.data.map { prefs ->
        decodeRuntimeSnapshot(prefs[KEY_RUNTIME_SNAPSHOT] ?: "")
    }

    val pendingCycleCompletionAckFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PENDING_CYCLE_ACK] ?: false
    }

    /**
     * Which account the on-device data belongs to, or `null` if unclaimed
     * (guest data that has never been signed in). Independent of the current
     * auth session — it persists across sign-out.
     */
    val ownerIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_OWNER_ID]?.takeIf { it.isNotBlank() }
    }

    /** Reads the current owner marker once (convenience for the sign-in path). */
    suspend fun currentOwnerId(): String? =
        ownerIdFlow.first()

    /** Sets (or clears, when [value] is null) the data owner marker. */
    suspend fun setOwnerId(value: String?) = dataStore.edit { prefs ->
        if (value.isNullOrBlank()) prefs.remove(KEY_OWNER_ID)
        else prefs[KEY_OWNER_ID] = value
    }

    /**
     * Removes all account-owned data so a different account starts clean. Keeps
     * device-local, non-account preferences (appearance, language, onboarding /
     * tutorial flags, selected sound, notification sound, glass mode, premium).
     *
     * Note: the [KEY_OWNER_ID] marker is intentionally NOT touched here — the
     * caller must clear data first, then write the new owner id, so a crash
     * mid-switch re-evaluates to Drop rather than leaking the old data to the
     * new account. See [com.endiar.purrodoro.auth.AccountManager].
     */
    suspend fun clearAccountOwnedData() {
        ensureMigrated()
        dataStore.edit { prefs ->
            listOf(
                // tasks + categories (legacy blobs pre-migration; counters always)
                KEY_TASKS, KEY_NEXT_TASK_ID,
                KEY_CATEGORIES, KEY_NEXT_CATEGORY_ID, KEY_SELECTED_CATEGORY_ID,
                // habits
                KEY_HABITS, KEY_NEXT_HABIT_ID,
                // history / progress
                KEY_SESSION_RECORDS, KEY_STAMPS, KEY_COMPLETED,
                // in-flight focus + cycle
                KEY_INTERRUPTED_FOCUS, KEY_RUNTIME_SNAPSHOT, KEY_PENDING_CYCLE_ACK,
                // account-tied greeting name
                KEY_USER_NAME,
                // timer settings revert to defaults for the new account
                KEY_FOCUS_MIN, KEY_SHORT_MIN, KEY_LONG_MIN,
                KEY_AUTO_START_NEXT_SESSION, KEY_CYCLE_SIZE,
            ).forEach { prefs.remove(it) }
        }
        // Hard-delete (not tombstone) the synced tables: an ownership switch
        // must not leave rows the next account's sync could push as deletions.
        val db = roomDb
        db.withTransaction {
            db.taskDao().clearAll()
            db.categoryDao().clearAll()
            db.habitDao().clearAll()
            db.habitLogDao().clearAll()
            db.sessionRecordDao().clearAll()
            db.dailyStampDao().clearAll()
        }
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

    /**
     * True once the user has finished or dismissed the spotlight feature tour.
     *
     * The tour is a lighter, second-stage primer that plays on the Home tab
     * after onboarding. For users upgrading from a build that predates the
     * tour, [hasPriorAppUsage] proves they already know the app, so they don't
     * get an unexpected coachmark sweep on next launch.
     */
    val hasSeenTutorialFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_TUTORIAL] ?: hasPriorAppUsage(prefs)
    }

    private fun hasPriorAppUsage(prefs: Preferences): Boolean {
        val records = prefs[KEY_SESSION_RECORDS]
        val tasks = prefs[KEY_TASKS]
        val habits = prefs[KEY_HABITS]
        fun nonEmpty(value: String?): Boolean =
            !value.isNullOrBlank() && value != "[]"
        return nonEmpty(records) || nonEmpty(tasks) || nonEmpty(habits)
    }

    /**
     * Single source of truth for the UI. Settings and device-local preferences
     * come from DataStore; account-owned domain data (tasks, categories,
     * habits + logs, session records, daily stamps) is served from Room. The
     * flow gates on [ensureMigrated] so any entry point (activity, widget,
     * timer service) triggers the one-time DataStore → Room migration before
     * the first emission.
     */
    val appState: Flow<AppState> = flow {
        ensureMigrated()
        val db = roomDb
        emitAll(
            combine(
                listOf(
                    dataStore.data,
                    db.taskDao().observeAll(),
                    db.categoryDao().observeAll(),
                ),
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                buildAppState(
                    prefs = values[0] as Preferences,
                    taskEntities = values[1] as List<TaskEntity>,
                    categoryEntities = values[2] as List<CategoryEntity>,
                )
            }
        )
    }

    private fun buildAppState(
        prefs: Preferences,
        taskEntities: List<TaskEntity>,
        categoryEntities: List<CategoryEntity>,
    ): AppState {
        val sortedCategories = categoryEntities.sortedBy { it.legacyId }
        val legacyCategoryIdByUuid = sortedCategories.associate { it.id to it.legacyId }
        return AppState(
            glassReadabilityMode = prefs[KEY_GLASS_MODE]
                ?.let { runCatching { GlassReadabilityMode.valueOf(it) }.getOrNull() }
                ?: GlassReadabilityMode.Adaptive,
            appearanceMode = prefs[KEY_APPEARANCE_MODE]
                ?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
                ?: AppearanceMode.Auto,
            language = AppLanguage.fromName(prefs[KEY_APP_LANGUAGE]),
            userName = (prefs[KEY_USER_NAME] ?: "").trim(),
            selectedSoundId = prefs[KEY_SOUND_ID] ?: 1,
            soundscapeMixAmbienceId = decodeOptionalInt(prefs[KEY_SOUNDSCAPE_MIX_AMBIENCE]),
            soundscapeMixMusicId = decodeOptionalInt(prefs[KEY_SOUNDSCAPE_MIX_MUSIC]),
            completedSessions = prefs[KEY_COMPLETED] ?: 0,
            settings = TimerSettings(
                focusMinutes = prefs[KEY_FOCUS_MIN] ?: 25,
                shortBreakMinutes = prefs[KEY_SHORT_MIN] ?: 5,
                longBreakMinutes = prefs[KEY_LONG_MIN] ?: 15,
                autoStartNextSession = prefs[KEY_AUTO_START_NEXT_SESSION] ?: false,
                focusSessionsPerCycle = (prefs[KEY_CYCLE_SIZE] ?: 4)
                    .coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE)
            ),
            nextTaskId = prefs[KEY_NEXT_TASK_ID] ?: 1,
            stamps = decodeStamps(prefs[KEY_STAMPS] ?: ""),
            isPremium = prefs[KEY_IS_PREMIUM] ?: false,
            sessionRecords = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: ""),
            tasks = taskEntities.sortedBy { it.legacyId }.map { entity ->
                entity.toDomain(entity.categoryId?.let { legacyCategoryIdByUuid[it] })
            },
            categories = sortedCategories.map { it.toDomain() },
            nextCategoryId = prefs[KEY_NEXT_CATEGORY_ID] ?: 1,
            selectedCategoryId = prefs[KEY_SELECTED_CATEGORY_ID]
                ?.takeIf { it.isNotBlank() }
                ?.toIntOrNull(),
            notificationSoundEnabled = prefs[KEY_NOTIFICATION_SOUND] ?: true,
            habits = decodeHabits(prefs[KEY_HABITS] ?: ""),
            nextHabitId = prefs[KEY_NEXT_HABIT_ID] ?: 1,
            interruptedFocus = decodeInterruptedFocus(prefs[KEY_INTERRUPTED_FOCUS] ?: ""),
        )
    }

    suspend fun setGlassReadabilityMode(value: GlassReadabilityMode) = dataStore.edit { it[KEY_GLASS_MODE] = value.name }
    suspend fun setAppearanceMode(value: AppearanceMode) = dataStore.edit { it[KEY_APPEARANCE_MODE] = value.name }
    suspend fun setLanguage(value: AppLanguage) = dataStore.edit { it[KEY_APP_LANGUAGE] = value.name }
    suspend fun setUserName(value: String) = dataStore.edit { it[KEY_USER_NAME] = value.trim() }
    suspend fun setSelectedSoundId(value: Int) = dataStore.edit { it[KEY_SOUND_ID] = value }

    suspend fun setSoundscapeMix(ambienceId: Int?, musicId: Int?) = dataStore.edit { prefs ->
        writeOptionalInt(prefs, KEY_SOUNDSCAPE_MIX_AMBIENCE, ambienceId)
        writeOptionalInt(prefs, KEY_SOUNDSCAPE_MIX_MUSIC, musicId)
    }
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

    suspend fun setHasSeenTutorial(value: Boolean) =
        dataStore.edit { it[KEY_HAS_SEEN_TUTORIAL] = value }

    suspend fun setInterruptedFocus(value: InterruptedFocus?) = dataStore.edit { prefs ->
        if (value == null) prefs.remove(KEY_INTERRUPTED_FOCUS)
        else prefs[KEY_INTERRUPTED_FOCUS] = encodeInterruptedFocus(value)
    }

    suspend fun appendSessionRecord(record: SessionRecord) = dataStore.edit { prefs ->
        val existing = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: "")
        prefs[KEY_SESSION_RECORDS] = encodeSessionRecords(existing + record)
    }

    /** Appends several records in a single edit (used by debug data seeding). */
    suspend fun appendSessionRecords(records: List<SessionRecord>) = dataStore.edit { prefs ->
        if (records.isEmpty()) return@edit
        val existing = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: "")
        prefs[KEY_SESSION_RECORDS] = encodeSessionRecords(existing + records)
    }

    /** Removes all stored session records (used by debug reset). */
    suspend fun clearSessionRecords() = dataStore.edit { it.remove(KEY_SESSION_RECORDS) }

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

    suspend fun setTasks(value: List<FocusTask>) {
        ensureMigrated()
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = db.taskDao().getAllIncludingDeleted()
            val existingByLegacyId = existing.associateBy { it.legacyId }
            val categoryUuidByLegacyId = db.categoryDao().getAll().associate { it.legacyId to it.id }
            val upserts = mutableListOf<TaskEntity>()
            for (task in value) {
                val current = existingByLegacyId[task.id]
                val entity = task.toEntity(
                    uuid = current?.id ?: newUuid(),
                    categoryUuid = task.categoryId?.let { categoryUuidByLegacyId[it] },
                    dirty = true,
                    modifiedAt = now,
                )
                val unchanged = current != null && !current.deleted &&
                    entity == current.copy(dirty = true, locallyModifiedAt = now)
                if (!unchanged) upserts += entity
            }
            if (upserts.isNotEmpty()) db.taskDao().upsert(upserts)
            val keptLegacyIds = value.map { it.id }.toSet()
            existing
                .filter { !it.deleted && it.legacyId !in keptLegacyIds }
                .forEach { db.taskDao().softDelete(it.id, now) }
        }
    }

    suspend fun setCategories(value: List<Category>) {
        ensureMigrated()
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = db.categoryDao().getAllIncludingDeleted()
            val existingByLegacyId = existing.associateBy { it.legacyId }
            val upserts = mutableListOf<CategoryEntity>()
            for (category in value) {
                val current = existingByLegacyId[category.id]
                val entity = category.toEntity(
                    uuid = current?.id ?: newUuid(),
                    dirty = true,
                    modifiedAt = now,
                )
                val unchanged = current != null && !current.deleted &&
                    entity == current.copy(dirty = true, locallyModifiedAt = now)
                if (!unchanged) upserts += entity
            }
            if (upserts.isNotEmpty()) db.categoryDao().upsert(upserts)
            val keptLegacyIds = value.map { it.id }.toSet()
            existing
                .filter { !it.deleted && it.legacyId !in keptLegacyIds }
                .forEach { db.categoryDao().softDelete(it.id, now) }
        }
    }

    suspend fun setNextCategoryId(value: Int) =
        dataStore.edit { it[KEY_NEXT_CATEGORY_ID] = value }

    suspend fun setSelectedCategoryId(value: Int?) =
        dataStore.edit {
            if (value == null) it.remove(KEY_SELECTED_CATEGORY_ID)
            else it[KEY_SELECTED_CATEGORY_ID] = value.toString()
        }

    suspend fun setNotificationSoundEnabled(value: Boolean) =
        dataStore.edit { it[KEY_NOTIFICATION_SOUND] = value }

    suspend fun addCategory(name: String, colorArgb: Long? = null) {
        ensureMigrated()
        var newId = 0
        dataStore.edit { prefs ->
            val nextId = prefs[KEY_NEXT_CATEGORY_ID] ?: 1
            newId = nextId
            prefs[KEY_NEXT_CATEGORY_ID] = nextId + 1
        }
        roomDb.categoryDao().upsert(
            listOf(
                Category(newId, name, colorArgb)
                    .toEntity(uuid = newUuid(), dirty = true, modifiedAt = System.currentTimeMillis())
            )
        )
    }

    suspend fun updateCategory(id: Int, name: String, colorArgb: Long?) {
        ensureMigrated()
        val dao = roomDb.categoryDao()
        val current = dao.getByLegacyId(id) ?: return
        dao.upsert(
            listOf(
                current.copy(
                    name = name,
                    colorArgb = colorArgb,
                    dirty = true,
                    locallyModifiedAt = System.currentTimeMillis(),
                )
            )
        )
    }

    suspend fun deleteCategory(id: Int) {
        ensureMigrated()
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val category = db.categoryDao().getByLegacyId(id) ?: return@withTransaction
            db.categoryDao().softDelete(category.id, now)
            val affectedTasks = db.taskDao().getAll().filter { it.categoryId == category.id }
            if (affectedTasks.isNotEmpty()) {
                db.taskDao().upsert(
                    affectedTasks.map {
                        it.copy(categoryId = null, dirty = true, locallyModifiedAt = now)
                    }
                )
            }
        }
        dataStore.edit { prefs ->
            val selected = prefs[KEY_SELECTED_CATEGORY_ID]?.toIntOrNull()
            if (selected == id) prefs.remove(KEY_SELECTED_CATEGORY_ID)
        }
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
    suspend fun setTaskCompleted(taskId: Int, completed: Boolean) {
        ensureMigrated()
        val dao = roomDb.taskDao()
        val current = dao.getByLegacyId(taskId) ?: return
        if (current.completed == completed) return
        dao.upsert(
            listOf(
                current.copy(
                    completed = completed,
                    dirty = true,
                    locallyModifiedAt = System.currentTimeMillis(),
                )
            )
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
        occurrence: HabitOccurrence = HabitOccurrence.Daily,
        timeBlock: TimeBlock = TimeBlock.Anytime,
    ): Int {
        var newId = 0
        dataStore.edit { prefs ->
            val nextId = prefs[KEY_NEXT_HABIT_ID] ?: 1
            newId = nextId
            val existing = decodeHabits(prefs[KEY_HABITS] ?: "")
            val habit = Habit(
                id = nextId, name = name, emoji = emoji, colorArgb = colorArgb,
                createdAt = System.currentTimeMillis(),
                occurrence = occurrence, timeBlock = timeBlock,
            )
            prefs[KEY_HABITS] = encodeHabits(existing + habit)
            prefs[KEY_NEXT_HABIT_ID] = nextId + 1
        }
        return newId
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
        occurrence: HabitOccurrence,
        timeBlock: TimeBlock,
    ) = mutateHabit(id) {
        it.copy(name = name, emoji = emoji, colorArgb = colorArgb,
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

// --- Interrupted focus encoding ---
// Single line, pipe-separated:
//   <focusType>|<taskIdOrEmpty>|<habitIdOrEmpty>|<urlTitle>|<remainingSeconds>|<remainingFocusSessions>|<focusMinutesOrEmpty>
internal fun encodeInterruptedFocus(value: InterruptedFocus): String = listOf(
    value.focusType.name,
    value.taskId?.toString() ?: "",
    value.habitId?.toString() ?: "",
    URLEncoder.encode(value.title, "UTF-8"),
    value.remainingSeconds,
    value.remainingFocusSessions,
    value.focusMinutes?.toString() ?: "",
).joinToString("|")

internal fun decodeInterruptedFocus(raw: String): InterruptedFocus? {
    if (raw.isBlank()) return null
    return runCatching {
        val p = raw.split("|", limit = 7)
        InterruptedFocus(
            focusType = FocusType.valueOf(p[0]),
            taskId = p[1].takeIf { it.isNotBlank() }?.toInt(),
            habitId = p[2].takeIf { it.isNotBlank() }?.toInt(),
            title = URLDecoder.decode(p[3], "UTF-8"),
            remainingSeconds = p[4].toInt(),
            remainingFocusSessions = p.getOrNull(5)?.takeIf { it.isNotBlank() }?.toInt() ?: 0,
            focusMinutes = p.getOrNull(6)?.takeIf { it.isNotBlank() }?.toInt(),
        )
    }.getOrNull()
}

// --- Task encoding ---
//
// v1 schema (7 fields):
//   "<id>:<urlTitle>:<minutes>:<completed>:<plannedSessions>:<completedSessions>:<categoryIdOrEmpty>"
//
// v2 schema (9 fields, adds priority + dueDate):
//   "<id>:<urlTitle>:<minutes>:<completed>:<plannedSessions>:<completedSessions>:<categoryIdOrEmpty>:<priorityTag>:<dueDateIsoOrEmpty>"
//
// Decoder accepts both — missing v2 fields fall back to Normal priority and no due date,
// so existing installs read cleanly without a migration step.
internal fun encodeTasks(tasks: List<FocusTask>): String =
    if (tasks.isEmpty()) "[]"
    else tasks.joinToString("\n") { t ->
        val encodedTitle = URLEncoder.encode(t.title, "UTF-8")
        val catField = t.categoryId?.toString() ?: ""
        val priorityField = t.priority.storageTag
        val dueField = t.dueDate?.toString() ?: ""
        "${t.id}:$encodedTitle:${t.minutes}:${t.completed}:${t.plannedSessions}:${t.completedSessions}:$catField:$priorityField:$dueField"
    }

internal fun decodeTasks(raw: String): List<FocusTask> {
    if (raw.isBlank()) return emptyList()
    if (raw == "[]") return emptyList()
    return runCatching {
        raw.split("\n").map { line ->
            val p = line.split(":", limit = 9)
            FocusTask(
                id = p[0].toInt(),
                title = URLDecoder.decode(p[1], "UTF-8"),
                minutes = p[2].toInt(),
                completed = p[3].toBoolean(),
                plannedSessions = p[4].toInt(),
                completedSessions = p[5].toInt(),
                categoryId = p.getOrNull(6)?.takeIf { it.isNotBlank() }?.toInt(),
                priority = TaskPriority.fromStorageTag(p.getOrNull(7)),
                dueDate = p.getOrNull(8)?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            )
        }
    }.getOrElse { emptyList() }
}

// --- Category encoding ---
//
// v1 schema (2 fields): "<id>:<urlName>"
// v2 schema (3 fields): "<id>:<urlName>:<colorArgbOrEmpty>"
//
// The color field was added with the category color picker. Older 2-field lines
// decode with a null color, so existing installs read cleanly without migration.
internal fun encodeCategories(categories: List<Category>): String =
    if (categories.isEmpty()) "[]"
    else categories.joinToString("\n") { c ->
        "${c.id}:${URLEncoder.encode(c.name, "UTF-8")}:${c.colorArgb ?: ""}"
    }

internal fun decodeCategories(raw: String): List<Category> {
    if (raw.isBlank()) return emptyList()
    if (raw == "[]") return emptyList()
    return runCatching {
        raw.split("\n").map { line ->
            val p = line.split(":", limit = 3)
            Category(
                id = p[0].toInt(),
                name = URLDecoder.decode(p[1], "UTF-8"),
                colorArgb = p.getOrNull(2)?.takeIf { it.isNotBlank() }?.toLongOrNull(),
            )
        }
    }.getOrElse { emptyList() }
}

// ─── Habit encoding ───────────────────────────────────────────────────────────
//
// v2 schema — one habit per line, 11 pipe-separated fields:
//   0:id  1:urlName  2:urlEmoji  3:colorArgb  4:createdAt
//   5:targetTag  (legacy "B"|"Q"|"P" — habits are check-off only now, always "B")
//   6:targetValue (legacy — always 1 now)
//   7:occurrenceTag  ("D" daily | "W" weekdays)
//   8:occurrenceData (empty for daily; "MON,WED,FRI" for weekdays)
//   9:timeBlockTag   ("M"|"A"|"E"|"X")
//   10:logsCsv       comma-separated "<isoDate>~<tag>[~progress]"
//                    tags: "C" Completed | "S" Skipped | "P" Partial (legacy)
//
// Fields 5/6 are retained for backward compatibility with previously stored
// data but are no longer meaningful: every habit is a simple binary check-off.
//
// v1 legacy (6 fields): id|urlName|urlEmoji|colorArgb|createdAt|completionDatesCsv
// Detected on read and migrated to Daily/Anytime with Completed logs.

internal fun encodeHabits(habits: List<Habit>): String =
    if (habits.isEmpty()) "[]"
    else habits.joinToString("\n") { h ->
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
            "B", 1, oTag, oData, bTag, logsCsv
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
    // Fields 5 (target tag) and 6 (target value) are legacy and ignored —
    // habits are check-off only now.
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

private fun decodeOptionalInt(raw: String?): Int? =
    raw?.takeIf { it.isNotBlank() }?.toIntOrNull()

private fun writeOptionalInt(
    prefs: androidx.datastore.preferences.core.MutablePreferences,
    key: androidx.datastore.preferences.core.Preferences.Key<String>,
    value: Int?,
) {
    if (value == null) prefs.remove(key)
    else prefs[key] = value.toString()
}
