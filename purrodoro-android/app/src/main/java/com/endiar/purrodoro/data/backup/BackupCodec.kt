package com.endiar.purrodoro.data.backup

import com.endiar.purrodoro.data.AppState
import com.endiar.purrodoro.data.TimerSettingsJson
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
import com.endiar.purrodoro.model.HomeScene
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimeBlock
import com.endiar.purrodoro.model.TimerSettings
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.serialization.json.Json

/** Validated, coerced data ready to hand to the repository restore path. */
data class RestoreBundle(
    val displayName: String,
    val timerSettings: TimerSettings,
    val prefs: RestorePrefs,
    val categories: List<Category>,
    val tasks: List<FocusTask>,
    /** Habits carry their (unioned at write time) logs folded in. */
    val habits: List<Habit>,
    val records: List<SessionRecord>,
    val stamps: Map<LocalDate, Int>,
)

data class RestorePrefs(
    val appearance: AppearanceMode,
    val language: AppLanguage,
    val glass: GlassReadabilityMode,
    val homeScene: HomeScene,
    val selectedSoundId: Int,
    val notificationSound: Boolean,
)

/** Outcome of decoding an untrusted backup file. */
sealed interface BackupParseResult {
    data class Success(val bundle: RestoreBundle) : BackupParseResult

    /** Not JSON, truncated, or otherwise unreadable as a backup. */
    data object Malformed : BackupParseResult

    /** Written by a newer app version than this build understands. */
    data class UnsupportedVersion(val version: Int) : BackupParseResult
}

/**
 * Pure encode/decode for Backup & Restore files. **All security clamping and
 * untrusted-input validation lives here** — the repository trusts a
 * [RestoreBundle].
 */
object BackupCodec {

    /** Bump when the on-disk shape changes incompatibly. */
    const val CURRENT_SCHEMA_VERSION: Int = 1

    // Defensive caps for untrusted text. Generous but bounded to stop a
    // malicious file from injecting absurd strings.
    private const val MAX_NAME_LEN = 100
    private const val MAX_TITLE_LEN = 200
    private const val MAX_EMOJI_LEN = 16
    private const val MAX_MINUTES = 180
    private const val MAX_TASK_MINUTES = 1440

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val parser = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    // ── Encode ───────────────────────────────────────────────────────────────

    fun encode(state: AppState, appVersionName: String, exportedAt: Long): String {
        val envelope = BackupEnvelope(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appVersionName = appVersionName,
            exportedAt = exportedAt,
            data = BackupData(
                profile = BackupProfile(
                    displayName = state.userName,
                    timerSettings = state.settings.toJson(),
                ),
                prefs = BackupPrefs(
                    appearance = state.appearanceMode.name,
                    language = state.language.name,
                    glass = state.glassReadabilityMode.name,
                    homeScene = state.homeScene.storageName,
                    selectedSoundId = state.selectedSoundId,
                    notificationSound = state.notificationSoundEnabled,
                ),
                categories = state.categories.map {
                    BackupCategory(id = it.id, name = it.name, colorArgb = it.colorArgb)
                },
                tasks = state.tasks.map { it.toBackup() },
                habits = state.habits.map { it.toBackup() },
                habitLogs = state.habits.flatMap { habit -> habit.logsToBackup() },
                records = state.sessionRecords.map { it.toBackup() },
                stamps = state.stamps.map { (date, count) ->
                    BackupStamp(date = date.toString(), count = count)
                },
            ),
        )
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    // ── Decode (untrusted) ─────────────────────────────────────────────────────

    fun decode(raw: String): BackupParseResult {
        val envelope = runCatching {
            parser.decodeFromString(BackupEnvelope.serializer(), raw)
        }.getOrNull() ?: return BackupParseResult.Malformed

        // version 0 means the field was absent — treat as malformed (not a real backup).
        if (envelope.schemaVersion <= 0) return BackupParseResult.Malformed
        if (envelope.schemaVersion > CURRENT_SCHEMA_VERSION) {
            return BackupParseResult.UnsupportedVersion(envelope.schemaVersion)
        }
        return BackupParseResult.Success(envelope.data.toBundle())
    }

    // ── Validation / coercion ──────────────────────────────────────────────────

    private fun BackupData.toBundle(): RestoreBundle {
        // Categories: positive ids, dedupe last-wins, bounded names.
        val categories = categories
            .filter { it.id > 0 }
            .associateBy { it.id } // last wins
            .values
            .map { Category(id = it.id, name = it.name.clean(MAX_NAME_LEN), colorArgb = it.colorArgb) }
        val categoryIds = categories.map { it.id }.toSet()

        // Tasks: positive ids, dedupe, dangling category null-ed, ranges coerced.
        val tasks = tasks
            .filter { it.id > 0 }
            .associateBy { it.id }
            .values
            .map { t ->
                FocusTask(
                    id = t.id,
                    title = t.title.clean(MAX_TITLE_LEN),
                    minutes = t.minutes.coerceIn(0, MAX_TASK_MINUTES),
                    completed = t.completed,
                    plannedSessions = t.plannedSessions.coerceIn(0, 99),
                    completedSessions = t.completedSessions.coerceIn(0, 99),
                    categoryId = t.categoryId?.takeIf { it in categoryIds },
                    priority = TaskPriority.fromStorageTag(t.priority),
                    dueDate = t.dueDate?.let { parseDateOrNull(it) },
                )
            }

        // Habits: positive ids, dedupe. Logs grouped by habit id (dangling dropped).
        val habitDtos = habits.filter { it.id > 0 }.associateBy { it.id }.values
        val habitIds = habitDtos.map { it.id }.toSet()
        val logsByHabit = habitLogs
            .filter { it.habitId in habitIds }
            .groupBy { it.habitId }
        val habits = habitDtos.map { h ->
            val logs: Map<LocalDate, HabitLog> = logsByHabit[h.id].orEmpty()
                .mapNotNull { log ->
                    val date = parseDateOrNull(log.date) ?: return@mapNotNull null
                    val state = log.toLogState() ?: return@mapNotNull null
                    date to HabitLog(state)
                }
                .toMap() // last wins on duplicate date
            Habit(
                id = h.id,
                name = h.name.clean(MAX_NAME_LEN),
                emoji = h.emoji.clean(MAX_EMOJI_LEN),
                colorArgb = h.colorArgb,
                createdAt = h.createdAt,
                occurrence = decodeOccurrence(h.occurrence, h.occurrenceDays),
                timeBlock = decodeTimeBlock(h.timeBlock),
                logs = logs,
            )
        }

        // Records: positive ids, dedupe, ranges coerced.
        val records = records
            .filter { it.id > 0 }
            .associateBy { it.id }
            .values
            .map { r ->
                SessionRecord(
                    id = r.id,
                    startedAt = r.startedAt,
                    endedAt = r.endedAt,
                    focusMinutes = r.focusMinutes.coerceIn(1, MAX_MINUTES),
                    shortBreakMinutes = r.shortBreakMinutes.coerceIn(1, MAX_MINUTES),
                    longBreakMinutes = r.longBreakMinutes.coerceIn(1, MAX_MINUTES),
                    completedFocusCount = r.completedFocusCount.coerceAtLeast(0),
                    totalFocusCount = r.totalFocusCount.coerceAtLeast(0),
                    completedShortBreakCount = r.completedShortBreakCount.coerceAtLeast(0),
                    completedLongBreakCount = r.completedLongBreakCount.coerceAtLeast(0),
                    status = runCatching { SessionStatus.valueOf(r.status) }
                        .getOrDefault(SessionStatus.Partial),
                    taskName = r.taskName?.clean(MAX_TITLE_LEN),
                    focusType = r.focusType?.let {
                        runCatching { FocusType.valueOf(it) }.getOrNull()
                    },
                )
            }

        // Stamps: valid dates, positive counts, dedupe taking the max.
        val stamps = stamps
            .mapNotNull { s ->
                val date = parseDateOrNull(s.date) ?: return@mapNotNull null
                val count = s.count
                if (count <= 0) null else date to count
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, counts) -> counts.max() }

        return RestoreBundle(
            displayName = profile.displayName.clean(MAX_NAME_LEN),
            timerSettings = profile.timerSettings.toSettings(),
            prefs = prefs.toRestorePrefs(),
            categories = categories,
            tasks = tasks,
            habits = habits,
            records = records,
            stamps = stamps,
        )
    }

    private fun BackupPrefs.toRestorePrefs(): RestorePrefs = RestorePrefs(
        appearance = runCatching { AppearanceMode.valueOf(appearance) }
            .getOrDefault(AppearanceMode.Light),
        language = AppLanguage.fromName(language),
        glass = runCatching { GlassReadabilityMode.valueOf(glass) }
            .getOrDefault(GlassReadabilityMode.Adaptive),
        homeScene = HomeScene.fromStorageName(homeScene),
        selectedSoundId = selectedSoundId.coerceAtLeast(1),
        notificationSound = notificationSound,
    )

    // ── Small helpers ──────────────────────────────────────────────────────────

    private fun TimerSettingsJson.toSettings(): TimerSettings = TimerSettings(
        focusMinutes = focusMinutes.coerceIn(1, MAX_MINUTES),
        shortBreakMinutes = shortBreakMinutes.coerceIn(1, MAX_MINUTES),
        longBreakMinutes = longBreakMinutes.coerceIn(1, MAX_MINUTES),
        autoStartNextSession = autoStartNextSession,
        focusSessionsPerCycle = focusSessionsPerCycle
            .coerceIn(1, TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE),
    )

    private fun TimerSettings.toJson(): TimerSettingsJson = TimerSettingsJson(
        focusMinutes = focusMinutes,
        shortBreakMinutes = shortBreakMinutes,
        longBreakMinutes = longBreakMinutes,
        autoStartNextSession = autoStartNextSession,
        focusSessionsPerCycle = focusSessionsPerCycle,
    )

    private fun parseDateOrNull(raw: String): LocalDate? =
        runCatching { LocalDate.parse(raw) }.getOrNull()

    /** Resolves a backup log row to a domain state, carrying Partial progress. */
    private fun BackupHabitLog.toLogState(): HabitLogState? = when (state) {
        "C" -> HabitLogState.Completed
        "S" -> HabitLogState.Skipped
        "P" -> HabitLogState.Partial(progress.coerceAtLeast(0))
        else -> null
    }

    private fun decodeOccurrence(tag: String, days: String): HabitOccurrence = when (tag) {
        "W" -> {
            val parsed = days.split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                .toSet()
            if (parsed.isEmpty()) HabitOccurrence.Daily else HabitOccurrence.Weekdays(parsed)
        }
        else -> HabitOccurrence.Daily
    }

    private fun decodeTimeBlock(tag: String): TimeBlock = when (tag) {
        "M" -> TimeBlock.Morning
        "A" -> TimeBlock.Afternoon
        "E" -> TimeBlock.Evening
        else -> TimeBlock.Anytime
    }

    /** Strip ISO control chars (except plain spaces), trim, and length-cap. */
    private fun String.clean(maxLen: Int): String =
        filter { !it.isISOControl() }.trim().take(maxLen)

    // ── Domain → backup DTO ─────────────────────────────────────────────────────

    private fun FocusTask.toBackup() = BackupTask(
        id = id,
        title = title,
        minutes = minutes,
        completed = completed,
        plannedSessions = plannedSessions,
        completedSessions = completedSessions,
        categoryId = categoryId,
        priority = priority.storageTag,
        dueDate = dueDate?.toString(),
    )

    private fun Habit.toBackup() = BackupHabit(
        id = id,
        name = name,
        emoji = emoji,
        colorArgb = colorArgb,
        createdAt = createdAt,
        occurrence = if (occurrence is HabitOccurrence.Weekdays) "W" else "D",
        occurrenceDays = (occurrence as? HabitOccurrence.Weekdays)
            ?.days?.sortedBy { it.value }?.joinToString(",") { it.name } ?: "",
        timeBlock = when (timeBlock) {
            TimeBlock.Morning -> "M"
            TimeBlock.Afternoon -> "A"
            TimeBlock.Evening -> "E"
            TimeBlock.Anytime -> "X"
        },
    )

    private fun Habit.logsToBackup(): List<BackupHabitLog> = logs.map { (date, log) ->
        val (state, progress) = when (val s = log.state) {
            HabitLogState.Completed -> "C" to 0
            HabitLogState.Skipped -> "S" to 0
            is HabitLogState.Partial -> "P" to s.progress
        }
        BackupHabitLog(habitId = id, date = date.toString(), state = state, progress = progress)
    }

    private fun SessionRecord.toBackup() = BackupRecord(
        id = id,
        startedAt = startedAt,
        endedAt = endedAt,
        focusMinutes = focusMinutes,
        shortBreakMinutes = shortBreakMinutes,
        longBreakMinutes = longBreakMinutes,
        completedFocusCount = completedFocusCount,
        totalFocusCount = totalFocusCount,
        completedShortBreakCount = completedShortBreakCount,
        completedLongBreakCount = completedLongBreakCount,
        status = status.name,
        taskName = taskName,
        focusType = focusType?.name,
    )
}
