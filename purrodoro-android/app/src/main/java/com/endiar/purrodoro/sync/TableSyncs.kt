package com.endiar.purrodoro.sync

import com.endiar.purrodoro.data.local.CategoryDao
import com.endiar.purrodoro.data.local.CategoryEntity
import com.endiar.purrodoro.data.local.DailyStampDao
import com.endiar.purrodoro.data.local.DailyStampEntity
import com.endiar.purrodoro.data.local.HabitDao
import com.endiar.purrodoro.data.local.HabitEntity
import com.endiar.purrodoro.data.local.HabitLogDao
import com.endiar.purrodoro.data.local.HabitLogEntity
import com.endiar.purrodoro.data.local.SessionRecordDao
import com.endiar.purrodoro.data.local.SessionRecordEntity
import com.endiar.purrodoro.data.local.TaskDao
import com.endiar.purrodoro.data.local.TaskEntity
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// The six TableSync implementations the SyncEngine iterates. Each one is a
// thin binding of BaseTableSync onto a DAO plus the SyncDtos mappers; the
// userId provider stamps pushes with the signed-in user (RLS requires it) and
// the legacy-id allocators mint local ids for rows first seen via a pull.

internal class CategoryTableSync(
    private val dao: CategoryDao,
    private val userId: suspend () -> String,
    private val allocateLegacyId: suspend () -> Int,
) : BaseTableSync<CategoryEntity>("categories") {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<CategoryEntity>) = dao.upsert(entities)
    override fun idOf(entity: CategoryEntity) = entity.id
    override fun modifiedAtOf(entity: CategoryEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: CategoryEntity) = entity.dirty
    override fun clearDirty(entity: CategoryEntity) = entity.copy(dirty = false)
    override suspend fun encodeForPush(entity: CategoryEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: CategoryEntity?) =
        syncJson.decodeFromJsonElement(CategoryDto.serializer(), row).toEntity(existing, allocateLegacyId)
}

internal class TaskTableSync(
    private val dao: TaskDao,
    private val userId: suspend () -> String,
    private val allocateLegacyId: suspend () -> Int,
) : BaseTableSync<TaskEntity>("tasks") {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<TaskEntity>) = dao.upsert(entities)
    override fun idOf(entity: TaskEntity) = entity.id
    override fun modifiedAtOf(entity: TaskEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: TaskEntity) = entity.dirty
    override fun clearDirty(entity: TaskEntity) = entity.copy(dirty = false)
    override suspend fun encodeForPush(entity: TaskEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: TaskEntity?) =
        syncJson.decodeFromJsonElement(TaskDto.serializer(), row).toEntity(existing, allocateLegacyId)
}

internal class HabitTableSync(
    private val dao: HabitDao,
    private val userId: suspend () -> String,
    private val allocateLegacyId: suspend () -> Int,
) : BaseTableSync<HabitEntity>("habits") {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<HabitEntity>) = dao.upsert(entities)
    override fun idOf(entity: HabitEntity) = entity.id
    override fun modifiedAtOf(entity: HabitEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: HabitEntity) = entity.dirty
    override fun clearDirty(entity: HabitEntity) = entity.copy(dirty = false)
    override suspend fun encodeForPush(entity: HabitEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: HabitEntity?) =
        syncJson.decodeFromJsonElement(HabitDto.serializer(), row).toEntity(existing, allocateLegacyId)
}

internal class HabitLogTableSync(
    private val dao: HabitLogDao,
    private val userId: suspend () -> String,
) : BaseTableSync<HabitLogEntity>("habit_logs") {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<HabitLogEntity>) = dao.upsert(entities)
    override fun idOf(entity: HabitLogEntity) = entity.id
    override fun modifiedAtOf(entity: HabitLogEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: HabitLogEntity) = entity.dirty
    override fun clearDirty(entity: HabitLogEntity) = entity.copy(dirty = false)
    override suspend fun encodeForPush(entity: HabitLogEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: HabitLogEntity?) =
        syncJson.decodeFromJsonElement(HabitLogDto.serializer(), row).toEntity(existing)

    override suspend fun entityByNaturalKey(row: JsonElement): HabitLogEntity? {
        val obj = row.jsonObject
        val habitId = obj["habit_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val logDate = obj["log_date"]?.jsonPrimitive?.contentOrNull ?: return null
        return dao.getByHabitAndDate(habitId, logDate)
    }

    override suspend fun deleteEntity(entity: HabitLogEntity) = dao.deleteById(entity.id)
    override fun withId(entity: HabitLogEntity, newId: String) = entity.copy(id = newId)
}

internal class SessionRecordTableSync(
    private val dao: SessionRecordDao,
    private val userId: suspend () -> String,
) : BaseTableSync<SessionRecordEntity>("session_records") {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<SessionRecordEntity>) = dao.upsert(entities)
    override fun idOf(entity: SessionRecordEntity) = entity.id
    override fun modifiedAtOf(entity: SessionRecordEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: SessionRecordEntity) = entity.dirty
    override fun clearDirty(entity: SessionRecordEntity) = entity.copy(dirty = false)
    override suspend fun encodeForPush(entity: SessionRecordEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: SessionRecordEntity?) =
        syncJson.decodeFromJsonElement(SessionRecordDto.serializer(), row).toEntity(existing)
}

internal class DailyStampTableSync(
    private val dao: DailyStampDao,
    private val userId: suspend () -> String,
) : BaseTableSync<DailyStampEntity>("daily_stamps") {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<DailyStampEntity>) = dao.upsert(entities)
    override fun idOf(entity: DailyStampEntity) = entity.id
    override fun modifiedAtOf(entity: DailyStampEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: DailyStampEntity) = entity.dirty
    override fun clearDirty(entity: DailyStampEntity) = entity.copy(dirty = false)
    override suspend fun encodeForPush(entity: DailyStampEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: DailyStampEntity?) =
        syncJson.decodeFromJsonElement(DailyStampDto.serializer(), row).toEntity(existing)

    override suspend fun entityByNaturalKey(row: JsonElement): DailyStampEntity? {
        val stampDate = row.jsonObject["stamp_date"]?.jsonPrimitive?.contentOrNull ?: return null
        return dao.getByDateIncludingDeleted(stampDate)
    }

    override suspend fun deleteEntity(entity: DailyStampEntity) = dao.deleteById(entity.id)
    override fun withId(entity: DailyStampEntity, newId: String) = entity.copy(id = newId)
}
