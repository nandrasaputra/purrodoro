package com.endiar.purrodoro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// DAOs for the synced tables. Reads exclude tombstones (deleted = 1); the
// sync engine reads dirty rows (including tombstones) via getDirty().
// softDelete marks a row as a dirty tombstone so the deletion can be pushed;
// clearAll() is the hard wipe used on account switch.

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deleted = 0")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted = 0")
    suspend fun getAll(): List<TaskEntity>

    /** Write-path helper: includes tombstones so unique legacyIds can be resurrected. */
    @Query("SELECT * FROM tasks")
    suspend fun getAllIncludingDeleted(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dirty = 1")
    suspend fun getDirty(): List<TaskEntity>

    /** Sync helper: atomically clears dirty iff the row is unchanged since the push snapshot. */
    @Query("UPDATE tasks SET dirty = 0 WHERE id = :id AND locallyModifiedAt = :snapshotModifiedAt AND dirty = 1")
    suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE legacyId = :legacyId AND deleted = 0")
    suspend fun getByLegacyId(legacyId: Int): TaskEntity?

    @Upsert
    suspend fun upsert(entities: List<TaskEntity>)

    @Query("UPDATE tasks SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deleted = 0")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deleted = 0")
    suspend fun getAll(): List<CategoryEntity>

    /** Write-path helper: includes tombstones so unique legacyIds can be resurrected. */
    @Query("SELECT * FROM categories")
    suspend fun getAllIncludingDeleted(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE dirty = 1")
    suspend fun getDirty(): List<CategoryEntity>

    /** Sync helper: atomically clears dirty iff the row is unchanged since the push snapshot. */
    @Query("UPDATE categories SET dirty = 0 WHERE id = :id AND locallyModifiedAt = :snapshotModifiedAt AND dirty = 1")
    suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE legacyId = :legacyId AND deleted = 0")
    suspend fun getByLegacyId(legacyId: Int): CategoryEntity?

    @Upsert
    suspend fun upsert(entities: List<CategoryEntity>)

    @Query("UPDATE categories SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deleted = 0")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE deleted = 0")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE dirty = 1")
    suspend fun getDirty(): List<HabitEntity>

    /** Sync helper: atomically clears dirty iff the row is unchanged since the push snapshot. */
    @Query("UPDATE habits SET dirty = 0 WHERE id = :id AND locallyModifiedAt = :snapshotModifiedAt AND dirty = 1")
    suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE legacyId = :legacyId AND deleted = 0")
    suspend fun getByLegacyId(legacyId: Int): HabitEntity?

    @Upsert
    suspend fun upsert(entities: List<HabitEntity>)

    @Query("UPDATE habits SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM habits")
    suspend fun clearAll()
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE deleted = 0")
    fun observeAll(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE deleted = 0")
    suspend fun getAll(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE dirty = 1")
    suspend fun getDirty(): List<HabitLogEntity>

    /** Sync helper: atomically clears dirty iff the row is unchanged since the push snapshot. */
    @Query("UPDATE habit_logs SET dirty = 0 WHERE id = :id AND locallyModifiedAt = :snapshotModifiedAt AND dirty = 1")
    suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long)

    @Query("SELECT * FROM habit_logs WHERE id = :id")
    suspend fun getById(id: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId IN (:habitIds) AND deleted = 0")
    suspend fun forHabits(habitIds: List<String>): List<HabitLogEntity>

    /** Write-path helper: includes tombstones so the unique [habitId, logDateIso] row can be resurrected. */
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND logDateIso = :logDateIso")
    suspend fun getByHabitAndDate(habitId: String, logDateIso: String): HabitLogEntity?

    @Query("UPDATE habit_logs SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE habitId = :habitId AND deleted = 0")
    suspend fun softDeleteForHabit(habitId: String, now: Long)

    @Upsert
    suspend fun upsert(entities: List<HabitLogEntity>)

    @Query("UPDATE habit_logs SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    /** Sync helper: hard-removes one row when a pull re-keys it onto the server id. */
    @Query("DELETE FROM habit_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAll()
}

@Dao
interface SessionRecordDao {
    @Query("SELECT * FROM session_records WHERE deleted = 0")
    fun observeAll(): Flow<List<SessionRecordEntity>>

    @Query("SELECT * FROM session_records WHERE deleted = 0")
    suspend fun getAll(): List<SessionRecordEntity>

    @Query("SELECT * FROM session_records WHERE dirty = 1")
    suspend fun getDirty(): List<SessionRecordEntity>

    /** Sync helper: atomically clears dirty iff the row is unchanged since the push snapshot. */
    @Query("UPDATE session_records SET dirty = 0 WHERE id = :id AND locallyModifiedAt = :snapshotModifiedAt AND dirty = 1")
    suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long)

    @Query("SELECT * FROM session_records WHERE id = :id")
    suspend fun getById(id: String): SessionRecordEntity?

    @Query("SELECT * FROM session_records WHERE legacyId = :legacyId AND deleted = 0")
    suspend fun getByLegacyId(legacyId: Long): SessionRecordEntity?

    /** Write-path helper: includes tombstones so the unique legacyId can be resurrected. */
    @Query("SELECT * FROM session_records WHERE legacyId = :legacyId")
    suspend fun getByLegacyIdIncludingDeleted(legacyId: Long): SessionRecordEntity?

    @Upsert
    suspend fun upsert(entities: List<SessionRecordEntity>)

    @Query("UPDATE session_records SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE session_records SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE deleted = 0")
    suspend fun softDeleteAll(now: Long)

    @Query("DELETE FROM session_records")
    suspend fun clearAll()
}

@Dao
interface DailyStampDao {
    @Query("SELECT * FROM daily_stamps WHERE deleted = 0")
    fun observeAll(): Flow<List<DailyStampEntity>>

    @Query("SELECT * FROM daily_stamps WHERE deleted = 0")
    suspend fun getAll(): List<DailyStampEntity>

    @Query("SELECT * FROM daily_stamps WHERE dirty = 1")
    suspend fun getDirty(): List<DailyStampEntity>

    /** Sync helper: atomically clears dirty iff the row is unchanged since the push snapshot. */
    @Query("UPDATE daily_stamps SET dirty = 0 WHERE id = :id AND locallyModifiedAt = :snapshotModifiedAt AND dirty = 1")
    suspend fun clearDirtyIfUnchanged(id: String, snapshotModifiedAt: Long)

    @Query("SELECT * FROM daily_stamps WHERE id = :id")
    suspend fun getById(id: String): DailyStampEntity?

    @Query("SELECT * FROM daily_stamps WHERE stampDateIso = :stampDateIso AND deleted = 0")
    suspend fun getByDate(stampDateIso: String): DailyStampEntity?

    /** Write-path helper: includes tombstones so the unique stampDateIso row can be resurrected. */
    @Query("SELECT * FROM daily_stamps WHERE stampDateIso = :stampDateIso")
    suspend fun getByDateIncludingDeleted(stampDateIso: String): DailyStampEntity?

    /** Write-path helper: includes tombstones so unique stampDateIso rows can be resurrected. */
    @Query("SELECT * FROM daily_stamps")
    suspend fun getAllIncludingDeleted(): List<DailyStampEntity>

    @Upsert
    suspend fun upsert(entities: List<DailyStampEntity>)

    @Query("UPDATE daily_stamps SET deleted = 1, dirty = 1, locallyModifiedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    /** Sync helper: hard-removes one row when a pull re-keys it onto the server id. */
    @Query("DELETE FROM daily_stamps WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM daily_stamps")
    suspend fun clearAll()
}
