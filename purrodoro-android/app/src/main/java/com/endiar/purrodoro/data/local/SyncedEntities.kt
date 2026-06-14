package com.endiar.purrodoro.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Room entities for account-owned domain data. Every row carries sync
// bookkeeping: [dirty] marks local changes not yet pushed, [locallyModifiedAt]
// is the wall-clock instant of the last local write, and [deleted] is a
// tombstone so deletions can be propagated to the server before the row is
// physically removed.
//
// `legacyId` preserves the old DataStore integer/long ids so the public
// AppRepository API (which still speaks legacy ids) can address rows; the
// UUID `id` is the stable sync identity.

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["legacyId"], unique = true)],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val legacyId: Int,
    val title: String,
    val completed: Boolean,
    val minutes: Int,
    val plannedSessions: Int,
    val completedSessions: Int,
    /** UUID of the owning category row, or null when uncategorized. */
    val categoryId: String?,
    val priorityTag: String,
    val dueDateIso: String?,
    val dirty: Boolean,
    val locallyModifiedAt: Long,
    val deleted: Boolean,
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["legacyId"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val legacyId: Int,
    val name: String,
    val colorArgb: Long?,
    val dirty: Boolean,
    val locallyModifiedAt: Long,
    val deleted: Boolean,
)

@Entity(
    tableName = "habits",
    indices = [Index(value = ["legacyId"], unique = true)],
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val legacyId: Int,
    val name: String,
    val emoji: String,
    val colorArgb: Long,
    val createdAtMillis: Long,
    /** "D" daily | "W" weekdays — matches the DataStore habit encoder tags. */
    val occurrenceTag: String,
    /** Comma-separated [java.time.DayOfWeek] names when [occurrenceTag] is "W"; empty otherwise. */
    val occurrenceDays: String,
    /** "M" | "A" | "E" | "X" — matches the DataStore time-block tags. */
    val timeBlockTag: String,
    val dirty: Boolean,
    val locallyModifiedAt: Long,
    val deleted: Boolean,
)

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "logDateIso"], unique = true)],
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    /** UUID of the owning habit row. */
    val habitId: String,
    val logDateIso: String,
    /** "C" Completed | "S" Skipped | "P" Partial — matches the DataStore log tags. */
    val stateTag: String,
    val progress: Int,
    val dirty: Boolean,
    val locallyModifiedAt: Long,
    val deleted: Boolean,
)

@Entity(
    tableName = "session_records",
    indices = [Index(value = ["legacyId"], unique = true)],
)
data class SessionRecordEntity(
    @PrimaryKey val id: String,
    val legacyId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val completedFocusCount: Int,
    val totalFocusCount: Int,
    val completedShortBreakCount: Int,
    val completedLongBreakCount: Int,
    val statusName: String,
    val taskName: String?,
    val focusTypeName: String?,
    val dirty: Boolean,
    val locallyModifiedAt: Long,
    val deleted: Boolean,
)

@Entity(
    tableName = "daily_stamps",
    indices = [Index(value = ["stampDateIso"], unique = true)],
)
data class DailyStampEntity(
    @PrimaryKey val id: String,
    val stampDateIso: String,
    val count: Int,
    val dirty: Boolean,
    val locallyModifiedAt: Long,
    val deleted: Boolean,
)

/**
 * Singleton sync-bookkeeping row mirroring the server `profiles` table
 * (display name + timer settings). The user-visible values stay in DataStore
 * — this row only exists so the sync engine has dirty/modifiedAt state to
 * push and reconcile. No `deleted` column: profiles are never tombstoned
 * (the server row lives and dies with the auth user).
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val rowId: Int = 0,
    val displayName: String,
    /** [com.endiar.purrodoro.data.TimerSettingsJson]-encoded timer settings. */
    val timerSettingsJson: String,
    val dirty: Boolean,
    val locallyModifiedAt: Long,
)
