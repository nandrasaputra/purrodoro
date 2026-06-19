package com.endiar.purrodoro.data.backup

import com.endiar.purrodoro.data.TimerSettingsJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * On-disk JSON shape of a Purrodoro **Backup & Restore** file.
 *
 * Design notes (see `docs/superpowers/specs/2026-06-19-data-backup-and-restore-design.md`):
 *  - **Data only.** Never carries `ownerId`, sync UUIDs / dirty / tombstone
 *    bookkeeping, the Supabase session, or `isPremium` — restoring entitlement
 *    or identity from an untrusted file is forbidden.
 *  - Every field is defaulted and unknown keys are ignored, so older/newer
 *    clients decode forgivingly (mirrors [TimerSettingsJson]).
 *  - Uses the **domain legacy-id** vocabulary the public `AppRepository`
 *    setters speak; the sync layer re-mints UUIDs / dirty flags on write.
 *
 * The compact enum tags reuse the existing storage tags so a backup row and a
 * natively-written row are indistinguishable (priority L/N/H, occurrence D/W,
 * time-block M/A/E/X, habit-log state C/S/P).
 */
@Serializable
data class BackupEnvelope(
    @SerialName("schema_version") val schemaVersion: Int = 0,
    @SerialName("app_version_name") val appVersionName: String = "",
    @SerialName("exported_at") val exportedAt: Long = 0L,
    val data: BackupData = BackupData(),
)

@Serializable
data class BackupData(
    val profile: BackupProfile = BackupProfile(),
    val prefs: BackupPrefs = BackupPrefs(),
    val categories: List<BackupCategory> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val habits: List<BackupHabit> = emptyList(),
    @SerialName("habit_logs") val habitLogs: List<BackupHabitLog> = emptyList(),
    val records: List<BackupRecord> = emptyList(),
    val stamps: List<BackupStamp> = emptyList(),
)

@Serializable
data class BackupProfile(
    @SerialName("display_name") val displayName: String = "",
    @SerialName("timer_settings") val timerSettings: TimerSettingsJson = TimerSettingsJson(),
)

@Serializable
data class BackupPrefs(
    val appearance: String = "Light",
    val language: String = "System",
    val glass: String = "Adaptive",
    @SerialName("home_scene") val homeScene: String = "Room",
    @SerialName("selected_sound_id") val selectedSoundId: Int = 1,
    @SerialName("notification_sound") val notificationSound: Boolean = true,
)

@Serializable
data class BackupCategory(
    val id: Int = 0,
    val name: String = "",
    @SerialName("color_argb") val colorArgb: Long? = null,
)

@Serializable
data class BackupTask(
    val id: Int = 0,
    val title: String = "",
    val minutes: Int = 25,
    val completed: Boolean = false,
    @SerialName("planned_sessions") val plannedSessions: Int = 1,
    @SerialName("completed_sessions") val completedSessions: Int = 0,
    @SerialName("category_id") val categoryId: Int? = null,
    val priority: String = "N",
    @SerialName("due_date") val dueDate: String? = null,
)

@Serializable
data class BackupHabit(
    val id: Int = 0,
    val name: String = "",
    val emoji: String = "",
    @SerialName("color_argb") val colorArgb: Long = 0L,
    @SerialName("created_at") val createdAt: Long = 0L,
    /** "D" daily | "W" weekdays. */
    val occurrence: String = "D",
    /** Comma-separated DayOfWeek names when [occurrence] == "W". */
    @SerialName("occurrence_days") val occurrenceDays: String = "",
    /** "M" | "A" | "E" | "X". */
    @SerialName("time_block") val timeBlock: String = "X",
)

@Serializable
data class BackupHabitLog(
    @SerialName("habit_id") val habitId: Int = 0,
    val date: String = "",
    /** "C" completed | "S" skipped | "P" partial. */
    val state: String = "C",
    val progress: Int = 0,
)

@Serializable
data class BackupRecord(
    val id: Long = 0L,
    @SerialName("started_at") val startedAt: Long = 0L,
    @SerialName("ended_at") val endedAt: Long = 0L,
    @SerialName("focus_minutes") val focusMinutes: Int = 25,
    @SerialName("short_break_minutes") val shortBreakMinutes: Int = 5,
    @SerialName("long_break_minutes") val longBreakMinutes: Int = 15,
    @SerialName("completed_focus_count") val completedFocusCount: Int = 0,
    @SerialName("total_focus_count") val totalFocusCount: Int = 0,
    @SerialName("completed_short_break_count") val completedShortBreakCount: Int = 0,
    @SerialName("completed_long_break_count") val completedLongBreakCount: Int = 0,
    val status: String = "Partial",
    @SerialName("task_name") val taskName: String? = null,
    @SerialName("focus_type") val focusType: String? = null,
)

@Serializable
data class BackupStamp(
    val date: String = "",
    val count: Int = 0,
)
