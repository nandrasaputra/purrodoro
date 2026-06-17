package com.endiar.purrodoro.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Local snapshot of the `soundscape_bundled_catalog` table: visibility /
 * premium / order / display overrides for APK-bundled tracks. Audio bytes are
 * NOT here (they ship in res/raw); the row only overrides metadata. Replaced
 * wholesale on every refresh — see BundledCatalogRepository.
 */
@Entity(
    tableName = "soundscape_bundled",
    primaryKeys = ["kind", "localId"],
)
data class SoundscapeBundledEntity(
    val kind: String,
    val localId: Int,
    val name: String,
    val artist: String,
    val symbol: String,
    val rawResource: String,
    val isPremium: Boolean,
    val sortOrder: Int,
    val isActive: Boolean,
)

@Dao
interface SoundscapeBundledDao {

    @Query("SELECT * FROM soundscape_bundled ORDER BY sortOrder, localId")
    fun observeAll(): Flow<List<SoundscapeBundledEntity>>

    @Query("SELECT * FROM soundscape_bundled ORDER BY sortOrder, localId")
    suspend fun getAll(): List<SoundscapeBundledEntity>

    @Transaction
    suspend fun replaceAll(rows: List<SoundscapeBundledEntity>) {
        clear()
        insertAll(rows)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<SoundscapeBundledEntity>)

    @Query("DELETE FROM soundscape_bundled")
    suspend fun clear()
}
