package com.endiar.purrodoro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
import com.endiar.purrodoro.data.local.ProfileEntity
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
import com.endiar.purrodoro.model.HabitOccurrence
import com.endiar.purrodoro.model.InterruptedFocus
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimeBlock
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_RUNTIME_SNAPSHOT = stringPreferencesKey("timer_runtime_snapshot")
        private val KEY_PENDING_CYCLE_ACK = booleanPreferencesKey("pending_cycle_completion_ack")
        private val KEY_NEXT_CATEGORY_ID = intPreferencesKey("next_category_id")
        private val KEY_SELECTED_CATEGORY_ID = stringPreferencesKey("selected_category_id")
        private val KEY_APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound_enabled")
        private val KEY_NEXT_HABIT_ID = intPreferencesKey("next_habit_id")
        private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        private val KEY_HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
        private val KEY_INTERRUPTED_FOCUS = stringPreferencesKey("interrupted_focus")

        // Marks which account the on-device data belongs to. Null = unclaimed
        // (guest data). Survives sign-out; drives the Claim/Keep/Drop rule on the
        // next sign-in. See the auth-and-account-identity spec.
        private val KEY_OWNER_ID = stringPreferencesKey("data_owner_id")

        // Per-table delta-sync checkpoints (max server updated_at applied so
        // far), keyed by "sync_checkpoint_<table>". Cleared together with the
        // account-owned data on an ownership switch.
        private const val SYNC_CHECKPOINT_PREFIX = "sync_checkpoint_"

        // Disjoint-range contract for pull-minted legacy ids: the UI keeps its
        // own optimistic copies of the KEY_NEXT_* counters and writes them
        // back as absolute values (PurrodoroApp.persistNextTaskId(nextTaskId + 1)),
        // so the sync pull path may NOT allocate from those counters — a pull
        // racing a UI create would hand the same id to both rows. Pull-minted
        // ids therefore come from these separate counters starting at
        // 1_000_000, a range the per-create UI counters can never plausibly
        // reach. They must stay positive: the UI treats id > 0 as "real row"
        // (e.g. the liveTaskId filter), and negative ids are reserved for
        // synthetic habit tasks.
        private const val SYNC_LEGACY_ID_FLOOR = 1_000_000
        private val KEY_SYNC_LEGACY_TASK_ID = intPreferencesKey("sync_legacy_task_id")
        private val KEY_SYNC_LEGACY_CATEGORY_ID = intPreferencesKey("sync_legacy_category_id")
        private val KEY_SYNC_LEGACY_HABIT_ID = intPreferencesKey("sync_legacy_habit_id")

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

    /**
     * Fires after every domain-data mutation (tasks, categories, habits +
     * logs, session records, stamps) — but not for prefs-only setters. The
     * sync layer debounces this into push passes.
     */
    private val _localWrites = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val localWrites: SharedFlow<Unit> = _localWrites.asSharedFlow()

    private fun notifyLocalWrite() {
        _localWrites.tryEmit(Unit)
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

    // ── Sync checkpoints ─────────────────────────────────────────────────────

    /** The last server `updated_at` applied for [table], or null before the first pull. */
    suspend fun syncCheckpoint(table: String): String? =
        dataStore.data.first()[stringPreferencesKey(SYNC_CHECKPOINT_PREFIX + table)]
            ?.takeIf { it.isNotBlank() }

    suspend fun setSyncCheckpoint(table: String, iso: String) = dataStore.edit { prefs ->
        prefs[stringPreferencesKey(SYNC_CHECKPOINT_PREFIX + table)] = iso
    }

    /** Drops every per-table checkpoint so the next sync re-pulls from scratch. */
    suspend fun resetSyncCheckpoints() = dataStore.edit { prefs ->
        removeSyncCheckpoints(prefs)
    }

    // ── Legacy id allocation (sync pull path) ────────────────────────────────
    // Atomically claims the next legacy id for a row first seen via a sync
    // pull. Allocates from the dedicated KEY_SYNC_LEGACY_* counters, NOT the
    // UI-facing KEY_NEXT_* ones — see the disjoint-range contract on
    // SYNC_LEGACY_ID_FLOOR.

    suspend fun allocateTaskLegacyId(): Int = allocateLegacyId(KEY_SYNC_LEGACY_TASK_ID)

    suspend fun allocateCategoryLegacyId(): Int = allocateLegacyId(KEY_SYNC_LEGACY_CATEGORY_ID)

    suspend fun allocateHabitLegacyId(): Int = allocateLegacyId(KEY_SYNC_LEGACY_HABIT_ID)

    private suspend fun allocateLegacyId(key: Preferences.Key<Int>): Int {
        var allocated = 0
        dataStore.edit { prefs ->
            val next = prefs[key] ?: SYNC_LEGACY_ID_FLOOR
            allocated = next
            prefs[key] = next + 1
        }
        return allocated
    }

    private fun removeSyncCheckpoints(prefs: MutablePreferences) {
        prefs.asMap().keys
            .filter { it.name.startsWith(SYNC_CHECKPOINT_PREFIX) }
            .forEach { prefs.remove(it) }
    }

    // ── Profile row (display name + timer settings sync) ────────────────────
    // The user-visible values live in DataStore as before; the singleton
    // ProfileEntity row only carries the sync bookkeeping (dirty/modifiedAt)
    // mirroring them, so the engine can push/reconcile the server `profiles`
    // row keyed by user_id.

    private fun timerSettingsFrom(prefs: Preferences) = TimerSettings(
        focusMinutes = prefs[KEY_FOCUS_MIN] ?: 25,
        shortBreakMinutes = prefs[KEY_SHORT_MIN] ?: 5,
        longBreakMinutes = prefs[KEY_LONG_MIN] ?: 15,
        autoStartNextSession = prefs[KEY_AUTO_START_NEXT_SESSION] ?: false,
        focusSessionsPerCycle = (prefs[KEY_CYCLE_SIZE] ?: 4)
            .coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE),
    )

    /** Current profile content derived from prefs, for when the row is missing. */
    private suspend fun profileFromPrefs(): ProfileEntity {
        val prefs = dataStore.data.first()
        return ProfileEntity(
            displayName = (prefs[KEY_USER_NAME] ?: "").trim(),
            timerSettingsJson = encodeTimerSettings(timerSettingsFrom(prefs)),
            dirty = true,
            locallyModifiedAt = 0L,
        )
    }

    private suspend fun upsertProfileDirty(transform: (ProfileEntity) -> ProfileEntity) {
        // Prefs fallback read first (DataStore reads don't belong inside a
        // Room transaction); the read-modify-write itself is atomic so a
        // concurrent sync apply can't interleave between get and upsert.
        val fallback = profileFromPrefs()
        roomDb.withTransaction {
            val current = roomDb.profileDao().get() ?: fallback
            roomDb.profileDao().upsert(
                transform(current).copy(dirty = true, locallyModifiedAt = System.currentTimeMillis())
            )
        }
    }

    /**
     * Creates the singleton profile row from current prefs if missing, and
     * marks it dirty so the next push uploads it. The prefs fallback is read
     * before the transaction (DataStore reads don't belong inside a Room
     * transaction); the read-modify-write itself is atomic.
     */
    internal suspend fun ensureProfileRowDirty(now: Long) {
        val fallback = profileFromPrefs()
        roomDb.withTransaction {
            val current = roomDb.profileDao().get() ?: fallback
            roomDb.profileDao().upsert(current.copy(dirty = true, locallyModifiedAt = now))
        }
    }

    /**
     * Account-claim re-upload: marks every live row in all synced tables —
     * including the singleton profile row, created from current prefs if
     * missing — dirty in ONE transaction, so a crash mid-way can never leave
     * a partially-marked upload set.
     */
    internal suspend fun markAllAccountDataDirty(now: Long) {
        val profileFallback = profileFromPrefs()
        val db = roomDb
        db.withTransaction {
            db.taskDao().markAllDirty(now)
            db.categoryDao().markAllDirty(now)
            db.habitDao().markAllDirty(now)
            db.habitLogDao().markAllDirty(now)
            db.sessionRecordDao().markAllDirty(now)
            db.dailyStampDao().markAllDirty(now)
            val current = db.profileDao().get() ?: profileFallback
            db.profileDao().upsert(current.copy(dirty = true, locallyModifiedAt = now))
        }
    }

    /**
     * Sync-pull apply path: writes the pulled name/settings into the
     * DataStore keys the UI reads — prefs ONLY. The Room profile row is
     * written by [com.endiar.purrodoro.sync.ProfileTableSync] inside its
     * transaction (where the dirty re-check lives); this runs only after that
     * transaction committed with the pulled row accepted, so it can never
     * clobber a concurrent user edit's prefs with stale cloud state while the
     * edit's row survives as dirty.
     */
    internal suspend fun applySyncedProfilePrefs(displayName: String, timerSettingsJson: String) {
        val settings = decodeTimerSettings(timerSettingsJson)
        dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = displayName.trim()
            prefs[KEY_FOCUS_MIN] = settings.focusMinutes
            prefs[KEY_SHORT_MIN] = settings.shortBreakMinutes
            prefs[KEY_LONG_MIN] = settings.longBreakMinutes
            prefs[KEY_AUTO_START_NEXT_SESSION] = settings.autoStartNextSession
            prefs[KEY_CYCLE_SIZE] = settings.focusSessionsPerCycle
        }
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
        // Hard-delete (not tombstone) the synced tables FIRST: an ownership
        // switch must not leave rows the next account's sync could push as
        // deletions. Clearing Room before resetting the DataStore counters
        // means a crash in between leaves counters intact alongside an empty
        // database — never restarted counters over surviving rows, which
        // would collide with the legacyId unique indexes and leak the old
        // account's data. (Same clear-before-commit principle as the owner
        // marker handling in AccountManager.)
        val db = roomDb
        db.withTransaction {
            db.taskDao().clearAll()
            db.categoryDao().clearAll()
            db.habitDao().clearAll()
            db.habitLogDao().clearAll()
            db.sessionRecordDao().clearAll()
            db.dailyStampDao().clearAll()
            db.profileDao().clearAll()
        }
        dataStore.edit { prefs ->
            listOf(
                // task + category counters
                KEY_NEXT_TASK_ID,
                KEY_NEXT_CATEGORY_ID, KEY_SELECTED_CATEGORY_ID,
                // habit counter
                KEY_NEXT_HABIT_ID,
                // pull-minted legacy id counters (Room is wiped, so the next
                // account restarts cleanly from the floor)
                KEY_SYNC_LEGACY_TASK_ID, KEY_SYNC_LEGACY_CATEGORY_ID, KEY_SYNC_LEGACY_HABIT_ID,
                // progress counter
                KEY_COMPLETED,
                // in-flight focus + cycle
                KEY_INTERRUPTED_FOCUS, KEY_RUNTIME_SNAPSHOT, KEY_PENDING_CYCLE_ACK,
                // account-tied greeting name
                KEY_USER_NAME,
                // timer settings revert to defaults for the new account
                KEY_FOCUS_MIN, KEY_SHORT_MIN, KEY_LONG_MIN,
                KEY_AUTO_START_NEXT_SESSION, KEY_CYCLE_SIZE,
            ).forEach { prefs.remove(it) }
            // Checkpoints belong to the old account's server state; the next
            // account must pull from scratch.
            removeSyncCheckpoints(prefs)
        }
    }

    /**
     * True once the user has finished or skipped onboarding. Absent key
     * (brand-new install) resolves to false so the onboarding flow plays.
     */
    val hasSeenOnboardingFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_ONBOARDING] ?: false
    }

    /**
     * True once the user has finished or dismissed the spotlight feature tour.
     * The tour is a lighter, second-stage primer that plays on the Home tab
     * after onboarding.
     */
    val hasSeenTutorialFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_TUTORIAL] ?: false
    }

    /**
     * Single source of truth for the UI. Settings and device-local preferences
     * come from DataStore; account-owned domain data (tasks, categories,
     * habits + logs, session records, daily stamps) is served from Room.
     */
    val appState: Flow<AppState> = flow {
        val db = roomDb
        emitAll(
            combine(
                listOf(
                    dataStore.data,
                    db.taskDao().observeAll(),
                    db.categoryDao().observeAll(),
                    db.habitDao().observeAll(),
                    db.habitLogDao().observeAll(),
                    db.sessionRecordDao().observeAll(),
                    db.dailyStampDao().observeAll(),
                ),
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                buildAppState(
                    prefs = values[0] as Preferences,
                    taskEntities = values[1] as List<TaskEntity>,
                    categoryEntities = values[2] as List<CategoryEntity>,
                    habitEntities = values[3] as List<HabitEntity>,
                    habitLogEntities = values[4] as List<HabitLogEntity>,
                    recordEntities = values[5] as List<SessionRecordEntity>,
                    stampEntities = values[6] as List<DailyStampEntity>,
                )
            }
        )
    }

    private fun buildAppState(
        prefs: Preferences,
        taskEntities: List<TaskEntity>,
        categoryEntities: List<CategoryEntity>,
        habitEntities: List<HabitEntity>,
        habitLogEntities: List<HabitLogEntity>,
        recordEntities: List<SessionRecordEntity>,
        stampEntities: List<DailyStampEntity>,
    ): AppState {
        val sortedCategories = categoryEntities.sortedBy { it.legacyId }
        val legacyCategoryIdByUuid = sortedCategories.associate { it.id to it.legacyId }
        val logsByHabitUuid = habitLogEntities.groupBy { it.habitId }
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
            settings = timerSettingsFrom(prefs),
            nextTaskId = prefs[KEY_NEXT_TASK_ID] ?: 1,
            stamps = stampEntities.mapNotNull { entity ->
                val date = runCatching { LocalDate.parse(entity.stampDateIso) }.getOrNull()
                    ?: return@mapNotNull null
                date to entity.count
            }.toMap(),
            isPremium = prefs[KEY_IS_PREMIUM] ?: false,
            sessionRecords = recordEntities.sortedBy { it.legacyId }.map { it.toDomain() },
            tasks = taskEntities.sortedBy { it.legacyId }.map { entity ->
                entity.toDomain(entity.categoryId?.let { legacyCategoryIdByUuid[it] })
            },
            categories = sortedCategories.map { it.toDomain() },
            nextCategoryId = prefs[KEY_NEXT_CATEGORY_ID] ?: 1,
            selectedCategoryId = prefs[KEY_SELECTED_CATEGORY_ID]
                ?.takeIf { it.isNotBlank() }
                ?.toIntOrNull(),
            notificationSoundEnabled = prefs[KEY_NOTIFICATION_SOUND] ?: true,
            habits = habitEntities.sortedBy { it.legacyId }.map { entity ->
                entity.toDomain(logsByHabitUuid[entity.id].orEmpty())
            },
            nextHabitId = prefs[KEY_NEXT_HABIT_ID] ?: 1,
            interruptedFocus = decodeInterruptedFocus(prefs[KEY_INTERRUPTED_FOCUS] ?: ""),
        )
    }

    suspend fun setGlassReadabilityMode(value: GlassReadabilityMode) = dataStore.edit { it[KEY_GLASS_MODE] = value.name }
    suspend fun setAppearanceMode(value: AppearanceMode) = dataStore.edit { it[KEY_APPEARANCE_MODE] = value.name }
    suspend fun setLanguage(value: AppLanguage) = dataStore.edit { it[KEY_APP_LANGUAGE] = value.name }
    suspend fun setUserName(value: String) {
        val trimmed = value.trim()
        dataStore.edit { it[KEY_USER_NAME] = trimmed }
        upsertProfileDirty { it.copy(displayName = trimmed) }
        notifyLocalWrite()
    }
    suspend fun setSelectedSoundId(value: Int) = dataStore.edit { it[KEY_SOUND_ID] = value }

    suspend fun setSoundscapeMix(ambienceId: Int?, musicId: Int?) = dataStore.edit { prefs ->
        writeOptionalInt(prefs, KEY_SOUNDSCAPE_MIX_AMBIENCE, ambienceId)
        writeOptionalInt(prefs, KEY_SOUNDSCAPE_MIX_MUSIC, musicId)
    }
    suspend fun setCompletedSessions(value: Int) = dataStore.edit { it[KEY_COMPLETED] = value }
    suspend fun setSettings(value: TimerSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_FOCUS_MIN] = value.focusMinutes
            prefs[KEY_SHORT_MIN] = value.shortBreakMinutes
            prefs[KEY_LONG_MIN] = value.longBreakMinutes
            prefs[KEY_AUTO_START_NEXT_SESSION] = value.autoStartNextSession
            prefs[KEY_CYCLE_SIZE] = value.focusSessionsPerCycle
        }
        upsertProfileDirty { it.copy(timerSettingsJson = encodeTimerSettings(value)) }
        notifyLocalWrite()
    }
    suspend fun setNextTaskId(value: Int) = dataStore.edit { it[KEY_NEXT_TASK_ID] = value }
    suspend fun setStamps(value: Map<LocalDate, Int>) {
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = db.dailyStampDao().getAllIncludingDeleted()
            val existingByDate = existing.associateBy { it.stampDateIso }
            val upserts = mutableListOf<DailyStampEntity>()
            for ((date, count) in value) {
                val iso = date.toString()
                val current = existingByDate[iso]
                val entity = DailyStampEntity(
                    id = current?.id ?: newUuid(),
                    stampDateIso = iso,
                    count = count,
                    dirty = true,
                    locallyModifiedAt = now,
                    deleted = false,
                )
                val unchanged = current != null && !current.deleted &&
                    entity == current.copy(dirty = true, locallyModifiedAt = now)
                if (!unchanged) upserts += entity
            }
            if (upserts.isNotEmpty()) db.dailyStampDao().upsert(upserts)
            val keptDates = value.keys.map { it.toString() }.toSet()
            existing
                .filter { !it.deleted && it.stampDateIso !in keptDates }
                .forEach { db.dailyStampDao().softDelete(it.id, now) }
        }
        notifyLocalWrite()
    }
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

    suspend fun appendSessionRecord(record: SessionRecord) =
        appendSessionRecords(listOf(record))

    /** Appends several records in a single transaction (used by debug data seeding). */
    suspend fun appendSessionRecords(records: List<SessionRecord>) {
        if (records.isEmpty()) return
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val entities = records.map { record ->
                val existing = db.sessionRecordDao().getByLegacyIdIncludingDeleted(record.id)
                record.toEntity(
                    uuid = existing?.id ?: newUuid(),
                    dirty = true,
                    modifiedAt = now,
                )
            }
            db.sessionRecordDao().upsert(entities)
        }
        notifyLocalWrite()
    }

    /** Soft-deletes all stored session records (used by debug reset). */
    suspend fun clearSessionRecords() {
        roomDb.sessionRecordDao().softDeleteAll(System.currentTimeMillis())
        notifyLocalWrite()
    }

    suspend fun setRuntimeSnapshot(snapshot: TimerRuntimeState?) = dataStore.edit { prefs ->
        if (snapshot == null) {
            prefs.remove(KEY_RUNTIME_SNAPSHOT)
        } else {
            prefs[KEY_RUNTIME_SNAPSHOT] = encodeRuntimeSnapshot(snapshot)
        }
    }

    suspend fun incrementStampFor(date: LocalDate) {
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val iso = date.toString()
            val existing = db.dailyStampDao().getByDateIncludingDeleted(iso)
            val newCount = if (existing != null && !existing.deleted) existing.count + 1 else 1
            db.dailyStampDao().upsert(
                listOf(
                    DailyStampEntity(
                        id = existing?.id ?: newUuid(),
                        stampDateIso = iso,
                        count = newCount,
                        dirty = true,
                        locallyModifiedAt = now,
                        deleted = false,
                    )
                )
            )
        }
        notifyLocalWrite()
    }

    suspend fun setTasks(value: List<FocusTask>) {
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
        notifyLocalWrite()
    }

    suspend fun setCategories(value: List<Category>) {
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
        notifyLocalWrite()
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
        notifyLocalWrite()
    }

    suspend fun updateCategory(id: Int, name: String, colorArgb: Long?) {
        val db = roomDb
        db.withTransaction {
            val current = db.categoryDao().getByLegacyId(id) ?: return@withTransaction
            db.categoryDao().upsert(
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
        notifyLocalWrite()
    }

    suspend fun deleteCategory(id: Int) {
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
        notifyLocalWrite()
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
        val db = roomDb
        db.withTransaction {
            val current = db.taskDao().getByLegacyId(taskId) ?: return@withTransaction
            if (current.completed == completed) return@withTransaction
            db.taskDao().upsert(
                listOf(
                    current.copy(
                        completed = completed,
                        dirty = true,
                        locallyModifiedAt = System.currentTimeMillis(),
                    )
                )
            )
        }
        notifyLocalWrite()
    }

    suspend fun deleteSessionRecord(id: Long) {
        val db = roomDb
        db.withTransaction {
            val existing = db.sessionRecordDao().getByLegacyId(id) ?: return@withTransaction
            db.sessionRecordDao().softDelete(existing.id, System.currentTimeMillis())
        }
        notifyLocalWrite()
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
            prefs[KEY_NEXT_HABIT_ID] = nextId + 1
        }
        val now = System.currentTimeMillis()
        val habit = Habit(
            id = newId, name = name, emoji = emoji, colorArgb = colorArgb,
            createdAt = now,
            occurrence = occurrence, timeBlock = timeBlock,
        )
        roomDb.habitDao().upsert(listOf(habit.toEntity(uuid = newUuid(), dirty = true, modifiedAt = now)))
        notifyLocalWrite()
        return newId
    }

    suspend fun deleteHabit(id: Int) {
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val habit = db.habitDao().getByLegacyId(id) ?: return@withTransaction
            db.habitDao().softDelete(habit.id, now)
            db.habitLogDao().softDeleteForHabit(habit.id, now)
        }
        notifyLocalWrite()
    }

    suspend fun updateHabitMeta(
        id: Int,
        name: String,
        emoji: String,
        colorArgb: Long,
        occurrence: HabitOccurrence,
        timeBlock: TimeBlock,
    ) {
        val db = roomDb
        db.withTransaction {
            val current = db.habitDao().getByLegacyId(id) ?: return@withTransaction
            val updated = Habit(
                id = id, name = name, emoji = emoji, colorArgb = colorArgb,
                createdAt = current.createdAtMillis,
                occurrence = occurrence, timeBlock = timeBlock,
            ).toEntity(uuid = current.id, dirty = true, modifiedAt = System.currentTimeMillis())
            db.habitDao().upsert(listOf(updated))
        }
        notifyLocalWrite()
    }

    suspend fun toggleHabitBinaryCompletion(id: Int, date: LocalDate) {
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val habit = db.habitDao().getByLegacyId(id) ?: return@withTransaction
            val existing = db.habitLogDao().getByHabitAndDate(habit.id, date.toString())
            val isCompleted = existing != null && !existing.deleted && existing.stateTag == "C"
            if (isCompleted) {
                db.habitLogDao().softDelete(existing!!.id, now)
            } else {
                db.habitLogDao().upsert(listOf(logEntity(existing, habit.id, date, "C", now)))
            }
        }
        notifyLocalWrite()
    }

    suspend fun incrementHabitProgress(id: Int, date: LocalDate, delta: Int = 1) =
        setHabitLogState(id, date, if (delta > 0) "C" else null)

    suspend fun setHabitSkipped(id: Int, date: LocalDate, skipped: Boolean) =
        setHabitLogState(id, date, if (skipped) "S" else null)

    suspend fun clearHabitLog(id: Int, date: LocalDate) =
        setHabitLogState(id, date, null)

    /**
     * Writes (or clears, when [stateTag] is null) the single log row keyed by
     * habit + date. A cleared log becomes a dirty tombstone; re-setting the
     * same day resurrects the existing row so the unique [habitId, logDateIso]
     * index never conflicts.
     */
    private suspend fun setHabitLogState(legacyHabitId: Int, date: LocalDate, stateTag: String?) {
        val db = roomDb
        val now = System.currentTimeMillis()
        db.withTransaction {
            val habit = db.habitDao().getByLegacyId(legacyHabitId) ?: return@withTransaction
            val existing = db.habitLogDao().getByHabitAndDate(habit.id, date.toString())
            if (stateTag == null) {
                if (existing != null && !existing.deleted) {
                    db.habitLogDao().softDelete(existing.id, now)
                }
            } else {
                db.habitLogDao().upsert(listOf(logEntity(existing, habit.id, date, stateTag, now)))
            }
        }
        notifyLocalWrite()
    }

    private fun logEntity(
        existing: HabitLogEntity?,
        habitUuid: String,
        date: LocalDate,
        stateTag: String,
        now: Long,
    ): HabitLogEntity = HabitLogEntity(
        id = existing?.id ?: newUuid(),
        habitId = habitUuid,
        logDateIso = date.toString(),
        stateTag = stateTag,
        progress = 0,
        dirty = true,
        locallyModifiedAt = now,
        deleted = false,
    )
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

private fun decodeOptionalInt(raw: String?): Int? =
    raw?.takeIf { it.isNotBlank() }?.toIntOrNull()

private fun writeOptionalInt(
    prefs: MutablePreferences,
    key: Preferences.Key<String>,
    value: Int?,
) {
    if (value == null) prefs.remove(key)
    else prefs[key] = value.toString()
}
