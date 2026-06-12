package com.endiar.purrodoro.data.local

import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.HabitLog
import com.endiar.purrodoro.model.HabitLogState
import com.endiar.purrodoro.model.HabitOccurrence
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimeBlock
import java.time.DayOfWeek
import java.time.LocalDate

// Pure mapping functions between domain models and Room entities. The tag
// encodings (priority, occurrence, time block, log state) intentionally match
// the legacy DataStore encoders in AppRepository.kt so migrated rows and
// freshly written rows are indistinguishable.

// ── Task ─────────────────────────────────────────────────────────────────────

fun FocusTask.toEntity(
    uuid: String,
    categoryUuid: String?,
    dirty: Boolean,
    modifiedAt: Long,
): TaskEntity = TaskEntity(
    id = uuid,
    legacyId = id,
    title = title,
    completed = completed,
    minutes = minutes,
    plannedSessions = plannedSessions,
    completedSessions = completedSessions,
    categoryId = categoryUuid,
    priorityTag = priority.storageTag,
    dueDateIso = dueDate?.toString(),
    dirty = dirty,
    locallyModifiedAt = modifiedAt,
    deleted = false,
)

fun TaskEntity.toDomain(legacyCategoryId: Int?): FocusTask = FocusTask(
    id = legacyId,
    title = title,
    minutes = minutes,
    completed = completed,
    plannedSessions = plannedSessions,
    completedSessions = completedSessions,
    categoryId = legacyCategoryId,
    priority = TaskPriority.fromStorageTag(priorityTag),
    dueDate = dueDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
)

// ── Category ─────────────────────────────────────────────────────────────────

fun Category.toEntity(
    uuid: String,
    dirty: Boolean,
    modifiedAt: Long,
): CategoryEntity = CategoryEntity(
    id = uuid,
    legacyId = id,
    name = name,
    colorArgb = colorArgb,
    dirty = dirty,
    locallyModifiedAt = modifiedAt,
    deleted = false,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = legacyId,
    name = name,
    colorArgb = colorArgb,
)

// ── Habit + logs ─────────────────────────────────────────────────────────────

fun Habit.toEntity(
    uuid: String,
    dirty: Boolean,
    modifiedAt: Long,
): HabitEntity {
    val (occurrenceTag, occurrenceDays) = when (val o = occurrence) {
        HabitOccurrence.Daily -> "D" to ""
        is HabitOccurrence.Weekdays ->
            "W" to o.days.sortedBy { it.value }.joinToString(",") { it.name }
    }
    val timeBlockTag = when (timeBlock) {
        TimeBlock.Morning -> "M"
        TimeBlock.Afternoon -> "A"
        TimeBlock.Evening -> "E"
        TimeBlock.Anytime -> "X"
    }
    return HabitEntity(
        id = uuid,
        legacyId = id,
        name = name,
        emoji = emoji,
        colorArgb = colorArgb,
        createdAtMillis = createdAt,
        occurrenceTag = occurrenceTag,
        occurrenceDays = occurrenceDays,
        timeBlockTag = timeBlockTag,
        dirty = dirty,
        locallyModifiedAt = modifiedAt,
        deleted = false,
    )
}

fun Habit.toLogEntities(
    habitUuid: String,
    idFor: (LocalDate) -> String,
    dirty: Boolean,
    modifiedAt: Long,
): List<HabitLogEntity> = logs.map { (date, log) ->
    val (stateTag, progress) = when (val s = log.state) {
        HabitLogState.Completed -> "C" to 0
        HabitLogState.Skipped -> "S" to 0
        is HabitLogState.Partial -> "P" to s.progress
    }
    HabitLogEntity(
        id = idFor(date),
        habitId = habitUuid,
        logDateIso = date.toString(),
        stateTag = stateTag,
        progress = progress,
        dirty = dirty,
        locallyModifiedAt = modifiedAt,
        deleted = false,
    )
}

fun HabitEntity.toDomain(logEntities: List<HabitLogEntity>): Habit {
    val occurrence: HabitOccurrence = when (occurrenceTag) {
        "W" -> {
            val days = occurrenceDays.split(",").filter { it.isNotBlank() }
                .mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
            if (days.isEmpty()) HabitOccurrence.Daily else HabitOccurrence.Weekdays(days)
        }
        else -> HabitOccurrence.Daily
    }
    val timeBlock = when (timeBlockTag) {
        "M" -> TimeBlock.Morning
        "A" -> TimeBlock.Afternoon
        "E" -> TimeBlock.Evening
        else -> TimeBlock.Anytime
    }
    val logs: Map<LocalDate, HabitLog> = logEntities
        .asSequence()
        .filterNot { it.deleted }
        .mapNotNull { entity ->
            val date = runCatching { LocalDate.parse(entity.logDateIso) }.getOrNull()
                ?: return@mapNotNull null
            val state: HabitLogState = when (entity.stateTag) {
                "C" -> HabitLogState.Completed
                "S" -> HabitLogState.Skipped
                "P" -> HabitLogState.Partial(entity.progress.coerceAtLeast(0))
                else -> return@mapNotNull null
            }
            date to HabitLog(state)
        }
        .toMap()
    return Habit(
        id = legacyId,
        name = name,
        emoji = emoji,
        colorArgb = colorArgb,
        createdAt = createdAtMillis,
        occurrence = occurrence,
        timeBlock = timeBlock,
        logs = logs,
    )
}

// ── Session record ───────────────────────────────────────────────────────────

fun SessionRecord.toEntity(
    uuid: String,
    dirty: Boolean,
    modifiedAt: Long,
): SessionRecordEntity = SessionRecordEntity(
    id = uuid,
    legacyId = id,
    startedAt = startedAt,
    endedAt = endedAt,
    focusMinutes = focusMinutes,
    shortBreakMinutes = shortBreakMinutes,
    longBreakMinutes = longBreakMinutes,
    completedFocusCount = completedFocusCount,
    totalFocusCount = totalFocusCount,
    completedShortBreakCount = completedShortBreakCount,
    completedLongBreakCount = completedLongBreakCount,
    statusName = status.name,
    taskName = taskName,
    dirty = dirty,
    locallyModifiedAt = modifiedAt,
    deleted = false,
)

fun SessionRecordEntity.toDomain(): SessionRecord = SessionRecord(
    id = legacyId,
    startedAt = startedAt,
    endedAt = endedAt,
    focusMinutes = focusMinutes,
    shortBreakMinutes = shortBreakMinutes,
    longBreakMinutes = longBreakMinutes,
    completedFocusCount = completedFocusCount,
    totalFocusCount = totalFocusCount,
    status = runCatching { SessionStatus.valueOf(statusName) }.getOrDefault(SessionStatus.Partial),
    taskName = taskName,
    completedShortBreakCount = completedShortBreakCount,
    completedLongBreakCount = completedLongBreakCount,
)
