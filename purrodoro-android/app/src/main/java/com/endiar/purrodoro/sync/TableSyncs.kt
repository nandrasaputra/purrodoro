package com.endiar.purrodoro.sync

import com.endiar.purrodoro.data.local.CategoryDao
import com.endiar.purrodoro.data.local.CategoryEntity
import com.endiar.purrodoro.data.local.DailyStampDao
import com.endiar.purrodoro.data.local.DailyStampEntity
import com.endiar.purrodoro.data.local.HabitDao
import com.endiar.purrodoro.data.local.HabitEntity
import com.endiar.purrodoro.data.local.HabitLogDao
import com.endiar.purrodoro.data.local.HabitLogEntity
import com.endiar.purrodoro.data.local.ProfileDao
import com.endiar.purrodoro.data.local.ProfileEntity
import com.endiar.purrodoro.data.local.SessionRecordDao
import com.endiar.purrodoro.data.local.SessionRecordEntity
import com.endiar.purrodoro.data.local.TaskDao
import com.endiar.purrodoro.data.local.TaskEntity
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.OffsetDateTime

// The TableSync implementations the SyncEngine iterates. The six domain
// tables are thin bindings of BaseTableSync onto a DAO plus the SyncDtos
// mappers; the userId provider stamps pushes with the signed-in user (RLS
// requires it) and the legacy-id allocators mint local ids for rows first
// seen via a pull. ProfileTableSync (singleton row, user_id-keyed) implements
// TableSync directly.

internal class CategoryTableSync(
    private val dao: CategoryDao,
    private val userId: suspend () -> String,
    inTransaction: TransactionRunner,
    private val allocateLegacyId: suspend () -> Int,
) : BaseTableSync<CategoryEntity>("categories", inTransaction) {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<CategoryEntity>) = dao.upsert(entities)
    override suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long) =
        dao.clearDirtyIfUnchanged(id, snapshotModifiedAt)
    override fun idOf(entity: CategoryEntity) = entity.id
    override fun modifiedAtOf(entity: CategoryEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: CategoryEntity) = entity.dirty
    override suspend fun encodeForPush(entity: CategoryEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: CategoryEntity?) =
        syncJson.decodeFromJsonElement(CategoryDto.serializer(), row).toEntity(existing, allocateLegacyId)
}

internal class TaskTableSync(
    private val dao: TaskDao,
    private val userId: suspend () -> String,
    inTransaction: TransactionRunner,
    private val allocateLegacyId: suspend () -> Int,
) : BaseTableSync<TaskEntity>("tasks", inTransaction) {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<TaskEntity>) = dao.upsert(entities)
    override suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long) =
        dao.clearDirtyIfUnchanged(id, snapshotModifiedAt)
    override fun idOf(entity: TaskEntity) = entity.id
    override fun modifiedAtOf(entity: TaskEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: TaskEntity) = entity.dirty
    override suspend fun encodeForPush(entity: TaskEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: TaskEntity?) =
        syncJson.decodeFromJsonElement(TaskDto.serializer(), row).toEntity(existing, allocateLegacyId)
}

internal class HabitTableSync(
    private val dao: HabitDao,
    private val userId: suspend () -> String,
    inTransaction: TransactionRunner,
    private val allocateLegacyId: suspend () -> Int,
) : BaseTableSync<HabitEntity>("habits", inTransaction) {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<HabitEntity>) = dao.upsert(entities)
    override suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long) =
        dao.clearDirtyIfUnchanged(id, snapshotModifiedAt)
    override fun idOf(entity: HabitEntity) = entity.id
    override fun modifiedAtOf(entity: HabitEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: HabitEntity) = entity.dirty
    override suspend fun encodeForPush(entity: HabitEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: HabitEntity?) =
        syncJson.decodeFromJsonElement(HabitDto.serializer(), row).toEntity(existing, allocateLegacyId)
}

internal class HabitLogTableSync(
    private val dao: HabitLogDao,
    private val userId: suspend () -> String,
    inTransaction: TransactionRunner,
) : BaseTableSync<HabitLogEntity>("habit_logs", inTransaction) {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<HabitLogEntity>) = dao.upsert(entities)
    override suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long) =
        dao.clearDirtyIfUnchanged(id, snapshotModifiedAt)
    override fun idOf(entity: HabitLogEntity) = entity.id
    override fun modifiedAtOf(entity: HabitLogEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: HabitLogEntity) = entity.dirty
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
    inTransaction: TransactionRunner,
) : BaseTableSync<SessionRecordEntity>("session_records", inTransaction) {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<SessionRecordEntity>) = dao.upsert(entities)
    override suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long) =
        dao.clearDirtyIfUnchanged(id, snapshotModifiedAt)
    override fun idOf(entity: SessionRecordEntity) = entity.id
    override fun modifiedAtOf(entity: SessionRecordEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: SessionRecordEntity) = entity.dirty
    override suspend fun encodeForPush(entity: SessionRecordEntity) = entity.toDto(userId()).toPushJson()
    override suspend fun decodeRemote(row: JsonElement, existing: SessionRecordEntity?) =
        syncJson.decodeFromJsonElement(SessionRecordDto.serializer(), row).toEntity(existing)

    // Natural key: the server enforces unique (user_id, local_id), and both a
    // guest device and the account's existing cloud data mint local_ids from
    // 1 — so the first merge after a Claim WILL collide. Adoption re-keys the
    // local row onto the server identity instead of wedging on the index.
    override suspend fun entityByNaturalKey(row: JsonElement): SessionRecordEntity? {
        val localId = row.jsonObject["local_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: return null
        return dao.getByLegacyIdIncludingDeleted(localId)
    }

    override suspend fun deleteEntity(entity: SessionRecordEntity) = dao.deleteById(entity.id)
    override fun withId(entity: SessionRecordEntity, newId: String) = entity.copy(id = newId)
}

internal class DailyStampTableSync(
    private val dao: DailyStampDao,
    private val userId: suspend () -> String,
    inTransaction: TransactionRunner,
) : BaseTableSync<DailyStampEntity>("daily_stamps", inTransaction) {
    override suspend fun dirtyEntities() = dao.getDirty()
    override suspend fun entityById(id: String) = dao.getById(id)
    override suspend fun upsertEntities(entities: List<DailyStampEntity>) = dao.upsert(entities)
    override suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long) =
        dao.clearDirtyIfUnchanged(id, snapshotModifiedAt)
    override fun idOf(entity: DailyStampEntity) = entity.id
    override fun modifiedAtOf(entity: DailyStampEntity) = entity.locallyModifiedAt
    override fun isDirty(entity: DailyStampEntity) = entity.dirty
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

/**
 * Sync for the singleton `profiles` row (display name + timer settings).
 * Implemented directly on [TableSync] rather than [BaseTableSync] because the
 * table is keyed by `user_id` (no `id` column, conflict target included) and
 * because applying a pulled profile also means writing the DataStore prefs
 * the UI reads. The Room row is written inside a transaction (the dirty
 * re-check and the write must be atomic against a concurrent user edit); the
 * prefs write — [applySyncedPrefs], production
 * [com.endiar.purrodoro.data.AppRepository.applySyncedProfilePrefs] — runs
 * only AFTER the transaction committed with the row accepted, and never when
 * a dirty local row won.
 *
 * Tombstones: the server `deleted` column exists on `profiles` but is unused —
 * a profile lives and dies with the auth user, so pulls ignore it.
 */
internal class ProfileTableSync(
    private val dao: ProfileDao,
    private val userId: suspend () -> String,
    private val inTransaction: TransactionRunner,
    private val applySyncedPrefs: suspend (displayName: String, timerSettingsJson: String) -> Unit,
) : TableSync {

    override val table: String = "profiles"
    override val conflictKey: String = "user_id"

    /** `locallyModifiedAt` captured by the latest [dirtyRows] call (same contract as BaseTableSync). */
    private var pushSnapshotModifiedAt: Long? = null

    override suspend fun dirtyRows(): List<JsonElement> {
        val row = dao.get()?.takeIf { it.dirty }
        pushSnapshotModifiedAt = row?.locallyModifiedAt
        row ?: return emptyList()
        val dto = ProfileDto(
            userId = userId(),
            displayName = row.displayName,
            timerSettings = syncJson.parseToJsonElement(row.timerSettingsJson),
        )
        return listOf(dto.toPushJson())
    }

    override suspend fun markPushed(rows: List<JsonElement>) {
        pushSnapshotModifiedAt?.let { dao.clearDirtyIfUnchanged(it) }
    }

    override suspend fun applyRemote(rows: List<JsonElement>) {
        // At most one row exists per user (PK user_id + RLS); take the newest
        // defensively in case an overlap window re-delivers older snapshots.
        // Parsed comparison, not lexicographic — server timestamps may carry
        // differing offsets (same rule as maxUpdatedAtOf).
        val row = rows.maxWithOrNull(
            compareBy {
                it.jsonObject["updated_at"]?.jsonPrimitive?.contentOrNull
                    ?.let(OffsetDateTime::parse) ?: OffsetDateTime.MIN
            }
        ) ?: return
        val dto = syncJson.decodeFromJsonElement(ProfileDto.serializer(), row)
        val displayName = dto.displayName.orEmpty().trim()
        val settingsJson = dto.timerSettings.toString()
        var accepted = false
        inTransaction {
            // resolvePull is evaluated INSIDE the transaction: a user edit
            // committing between an outside check and the write would
            // otherwise be silently overwritten with a clean row.
            val local = dao.get()
            if (resolvePull(local?.dirty, remoteDeleted = false) == PullAction.Apply) {
                dao.upsert(
                    ProfileEntity(
                        displayName = displayName,
                        timerSettingsJson = settingsJson,
                        dirty = false,
                        locallyModifiedAt = local?.locallyModifiedAt ?: 0L,
                    )
                )
                accepted = true
            }
        }
        // Re-check right before the prefs write: a user edit landing in the
        // commit-to-here gap re-dirtied the row, and its setter already wrote
        // the prefs it wants — overwriting them with the pulled values would
        // flash stale cloud state in the UI until the next pass.
        if (accepted && dao.get()?.dirty == false) applySyncedPrefs(displayName, settingsJson)
    }

    override suspend fun maxRemoteUpdatedAt(rows: List<JsonElement>): String? = maxUpdatedAtOf(rows)
}
