package com.endiar.purrodoro.sync

import com.endiar.purrodoro.data.local.CategoryEntity
import com.endiar.purrodoro.data.local.DailyStampEntity
import com.endiar.purrodoro.data.local.HabitEntity
import com.endiar.purrodoro.data.local.HabitLogEntity
import com.endiar.purrodoro.data.local.SessionRecordEntity
import com.endiar.purrodoro.data.local.TaskEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// Wire-format DTOs for the six synced Supabase tables. Field names (via
// @SerialName) match the SQL in supabase/migrations/0001_sync_tables.sql
// exactly. `updated_at` is server-set (default now() + moddatetime trigger),
// so it is null on push and stripped from the payload by [toPushJson]; on pull
// it carries the server timestamp the checkpoints advance to.

/** Shared Json configuration for the sync wire format. */
internal val syncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Encodes a DTO for pushing, dropping the server-owned `updated_at` column so
 * the moddatetime trigger (or the insert default) stamps it server-side.
 */
internal inline fun <reified T> T.toPushJson(): JsonElement {
    val obj = syncJson.encodeToJsonElement(serializer<T>(), this).jsonObject
    return JsonObject(obj.filterKeys { it != "updated_at" })
}

/**
 * Mints legacy (pre-UUID) integer ids for rows first seen via a pull, so the
 * AppRepository API — which still addresses tasks/categories/habits by their
 * legacy id — can reference them. Backed by the existing DataStore counters.
 */
interface LegacyIdAllocator {
    suspend fun nextTaskId(): Int
    suspend fun nextCategoryId(): Int
    suspend fun nextHabitId(): Int
}

// ── Task ─────────────────────────────────────────────────────────────────────

@Serializable
data class TaskDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String,
    @SerialName("completed") val completed: Boolean,
    @SerialName("minutes") val minutes: Int,
    @SerialName("planned_sessions") val plannedSessions: Int,
    @SerialName("completed_sessions") val completedSessions: Int,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("priority") val priority: String,
    @SerialName("due_date") val dueDate: String?,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted") val deleted: Boolean,
)

fun TaskEntity.toDto(userId: String): TaskDto = TaskDto(
    id = id,
    userId = userId,
    title = title,
    completed = completed,
    minutes = minutes,
    plannedSessions = plannedSessions,
    completedSessions = completedSessions,
    categoryId = categoryId,
    priority = priorityTag,
    dueDate = dueDateIso,
    deleted = deleted,
)

suspend fun TaskDto.toEntity(
    existing: TaskEntity?,
    allocateLegacyId: suspend () -> Int,
): TaskEntity = TaskEntity(
    id = id,
    legacyId = existing?.legacyId ?: allocateLegacyId(),
    title = title,
    completed = completed,
    minutes = minutes,
    plannedSessions = plannedSessions,
    completedSessions = completedSessions,
    categoryId = categoryId,
    priorityTag = priority,
    dueDateIso = dueDate,
    dirty = false,
    locallyModifiedAt = existing?.locallyModifiedAt ?: 0L,
    deleted = deleted,
)

// ── Category ─────────────────────────────────────────────────────────────────

@Serializable
data class CategoryDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("color_argb") val colorArgb: Long?,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted") val deleted: Boolean,
)

fun CategoryEntity.toDto(userId: String): CategoryDto = CategoryDto(
    id = id,
    userId = userId,
    name = name,
    colorArgb = colorArgb,
    deleted = deleted,
)

suspend fun CategoryDto.toEntity(
    existing: CategoryEntity?,
    allocateLegacyId: suspend () -> Int,
): CategoryEntity = CategoryEntity(
    id = id,
    legacyId = existing?.legacyId ?: allocateLegacyId(),
    name = name,
    colorArgb = colorArgb,
    dirty = false,
    locallyModifiedAt = existing?.locallyModifiedAt ?: 0L,
    deleted = deleted,
)

// ── Habit ────────────────────────────────────────────────────────────────────

@Serializable
data class HabitDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("emoji") val emoji: String,
    @SerialName("color_argb") val colorArgb: Long,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("occurrence_tag") val occurrenceTag: String,
    @SerialName("occurrence_days") val occurrenceDays: String,
    @SerialName("time_block") val timeBlock: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted") val deleted: Boolean,
)

fun HabitEntity.toDto(userId: String): HabitDto = HabitDto(
    id = id,
    userId = userId,
    name = name,
    emoji = emoji,
    colorArgb = colorArgb,
    createdAtMillis = createdAtMillis,
    occurrenceTag = occurrenceTag,
    occurrenceDays = occurrenceDays,
    timeBlock = timeBlockTag,
    deleted = deleted,
)

suspend fun HabitDto.toEntity(
    existing: HabitEntity?,
    allocateLegacyId: suspend () -> Int,
): HabitEntity = HabitEntity(
    id = id,
    legacyId = existing?.legacyId ?: allocateLegacyId(),
    name = name,
    emoji = emoji,
    colorArgb = colorArgb,
    createdAtMillis = createdAtMillis,
    occurrenceTag = occurrenceTag,
    occurrenceDays = occurrenceDays,
    timeBlockTag = timeBlock,
    dirty = false,
    locallyModifiedAt = existing?.locallyModifiedAt ?: 0L,
    deleted = deleted,
)

// ── Habit log ────────────────────────────────────────────────────────────────

@Serializable
data class HabitLogDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("log_date") val logDate: String,
    @SerialName("state") val state: String,
    @SerialName("progress") val progress: Int,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted") val deleted: Boolean,
)

fun HabitLogEntity.toDto(userId: String): HabitLogDto = HabitLogDto(
    id = id,
    userId = userId,
    habitId = habitId,
    logDate = logDateIso,
    state = stateTag,
    progress = progress,
    deleted = deleted,
)

fun HabitLogDto.toEntity(existing: HabitLogEntity?): HabitLogEntity = HabitLogEntity(
    id = id,
    habitId = habitId,
    logDateIso = logDate,
    stateTag = state,
    progress = progress,
    dirty = false,
    locallyModifiedAt = existing?.locallyModifiedAt ?: 0L,
    deleted = deleted,
)

// ── Session record ───────────────────────────────────────────────────────────

@Serializable
data class SessionRecordDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("local_id") val localId: Long,
    @SerialName("started_at") val startedAt: Long,
    @SerialName("ended_at") val endedAt: Long,
    @SerialName("focus_minutes") val focusMinutes: Int,
    @SerialName("short_break_minutes") val shortBreakMinutes: Int,
    @SerialName("long_break_minutes") val longBreakMinutes: Int,
    @SerialName("completed_focus_count") val completedFocusCount: Int,
    @SerialName("total_focus_count") val totalFocusCount: Int,
    @SerialName("completed_short_break_count") val completedShortBreakCount: Int,
    @SerialName("completed_long_break_count") val completedLongBreakCount: Int,
    @SerialName("status") val status: String,
    @SerialName("task_name") val taskName: String?,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted") val deleted: Boolean,
)

fun SessionRecordEntity.toDto(userId: String): SessionRecordDto = SessionRecordDto(
    id = id,
    userId = userId,
    localId = legacyId,
    startedAt = startedAt,
    endedAt = endedAt,
    focusMinutes = focusMinutes,
    shortBreakMinutes = shortBreakMinutes,
    longBreakMinutes = longBreakMinutes,
    completedFocusCount = completedFocusCount,
    totalFocusCount = totalFocusCount,
    completedShortBreakCount = completedShortBreakCount,
    completedLongBreakCount = completedLongBreakCount,
    status = statusName,
    taskName = taskName,
    deleted = deleted,
)

/** The legacy id of a session record is the server-stored `local_id`, no allocator needed. */
fun SessionRecordDto.toEntity(existing: SessionRecordEntity?): SessionRecordEntity = SessionRecordEntity(
    id = id,
    legacyId = existing?.legacyId ?: localId,
    startedAt = startedAt,
    endedAt = endedAt,
    focusMinutes = focusMinutes,
    shortBreakMinutes = shortBreakMinutes,
    longBreakMinutes = longBreakMinutes,
    completedFocusCount = completedFocusCount,
    totalFocusCount = totalFocusCount,
    completedShortBreakCount = completedShortBreakCount,
    completedLongBreakCount = completedLongBreakCount,
    statusName = status,
    taskName = taskName,
    dirty = false,
    locallyModifiedAt = existing?.locallyModifiedAt ?: 0L,
    deleted = deleted,
)

// ── Profile ──────────────────────────────────────────────────────────────────

/**
 * Wire row for the singleton `profiles` table. Unlike the other tables it is
 * keyed by `user_id` (no separate `id`), and `timer_settings` is a jsonb
 * object carried here as a raw [JsonElement] (locally it is stored as the
 * encoded string, see TimerSettingsJson). The server `deleted` column exists
 * but is never honored: a profile lives and dies with the auth user.
 */
@Serializable
data class ProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("timer_settings") val timerSettings: JsonElement,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted") val deleted: Boolean = false,
)

// ── Daily stamp ──────────────────────────────────────────────────────────────

@Serializable
data class DailyStampDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("stamp_date") val stampDate: String,
    @SerialName("count") val count: Int,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted") val deleted: Boolean,
)

fun DailyStampEntity.toDto(userId: String): DailyStampDto = DailyStampDto(
    id = id,
    userId = userId,
    stampDate = stampDateIso,
    count = count,
    deleted = deleted,
)

fun DailyStampDto.toEntity(existing: DailyStampEntity?): DailyStampEntity = DailyStampEntity(
    id = id,
    stampDateIso = stampDate,
    count = count,
    dirty = false,
    locallyModifiedAt = existing?.locallyModifiedAt ?: 0L,
    deleted = deleted,
)
