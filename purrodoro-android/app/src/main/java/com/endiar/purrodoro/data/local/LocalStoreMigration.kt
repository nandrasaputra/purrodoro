package com.endiar.purrodoro.data.local

import com.endiar.purrodoro.data.decodeCategories
import com.endiar.purrodoro.data.decodeHabits
import com.endiar.purrodoro.data.decodeSessionRecords
import com.endiar.purrodoro.data.decodeStamps
import com.endiar.purrodoro.data.decodeTasks
import java.util.UUID

/** The raw DataStore strings holding the pre-Room domain data. */
data class LegacyBlobs(
    val tasksRaw: String,
    val categoriesRaw: String,
    val habitsRaw: String,
    val recordsRaw: String,
    val stampsRaw: String,
)

/** Receives the migrated rows; production writes them into Room in one transaction. */
interface MigrationSink {
    suspend fun write(
        tasks: List<TaskEntity>,
        categories: List<CategoryEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        records: List<SessionRecordEntity>,
        stamps: List<DailyStampEntity>,
    )
}

/**
 * One-time DataStore → Room migration. Decodes the legacy blobs with the same
 * decoders the repository has always used, mints a UUID per row, remaps task →
 * category references from legacy ints to UUIDs (dangling references become
 * null), and marks everything dirty so the first sync pushes it all.
 */
class LocalStoreMigration(
    private val uuidFor: (type: String, key: String) -> String = { _, _ ->
        UUID.randomUUID().toString()
    },
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun migrate(legacy: LegacyBlobs, sink: MigrationSink) {
        val modifiedAt = now()

        val categories = decodeCategories(legacy.categoriesRaw)
        val categoryUuids = categories.associate { it.id to uuidFor("category", it.id.toString()) }
        val categoryEntities = categories.map { category ->
            category.toEntity(
                uuid = categoryUuids.getValue(category.id),
                dirty = true,
                modifiedAt = modifiedAt,
            )
        }

        val taskEntities = decodeTasks(legacy.tasksRaw).map { task ->
            task.toEntity(
                uuid = uuidFor("task", task.id.toString()),
                categoryUuid = task.categoryId?.let { categoryUuids[it] },
                dirty = true,
                modifiedAt = modifiedAt,
            )
        }

        val habitEntities = mutableListOf<HabitEntity>()
        val habitLogEntities = mutableListOf<HabitLogEntity>()
        for (habit in decodeHabits(legacy.habitsRaw)) {
            val habitUuid = uuidFor("habit", habit.id.toString())
            habitEntities += habit.toEntity(habitUuid, dirty = true, modifiedAt = modifiedAt)
            habitLogEntities += habit.toLogEntities(
                habitUuid = habitUuid,
                idFor = { date -> uuidFor("habit_log", "${habit.id}:$date") },
                dirty = true,
                modifiedAt = modifiedAt,
            )
        }

        val recordEntities = decodeSessionRecords(legacy.recordsRaw).map { record ->
            record.toEntity(
                uuid = uuidFor("session_record", record.id.toString()),
                dirty = true,
                modifiedAt = modifiedAt,
            )
        }

        val stampEntities = decodeStamps(legacy.stampsRaw).map { (date, count) ->
            DailyStampEntity(
                id = uuidFor("daily_stamp", date.toString()),
                stampDateIso = date.toString(),
                count = count,
                dirty = true,
                locallyModifiedAt = modifiedAt,
                deleted = false,
            )
        }

        sink.write(
            tasks = taskEntities,
            categories = categoryEntities,
            habits = habitEntities,
            habitLogs = habitLogEntities,
            records = recordEntities,
            stamps = stampEntities,
        )
    }
}
