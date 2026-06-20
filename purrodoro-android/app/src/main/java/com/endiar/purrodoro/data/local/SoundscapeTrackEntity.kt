package com.endiar.purrodoro.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Local snapshot of the `soundscape_tracks` cloud catalog (cloud tracks only;
 * bundled tracks never appear here). Replaced wholesale on every refresh —
 * see SoundscapeCatalogRepository. [updatedAtMillis] versions the audio file:
 * it is baked into the local filename so a server-side replacement triggers a
 * re-download.
 */
@Entity(tableName = "soundscape_track")
data class SoundscapeTrackEntity(
    @PrimaryKey val id: Int,
    val kind: String,          // "music" | "ambience"
    val name: String,
    val artist: String,
    val symbol: String,
    val filePath: String,      // R2 object key, e.g. lofi/lofi_rainy_window.mp3
    val thumbnailPath: String = "", // R2 object key, e.g. lofi/lofi_rainy_window.webp
    val fileSizeBytes: Long,
    val isPremium: Boolean,
    val sortOrder: Int,
    val updatedAtMillis: Long,
)

@Dao
interface SoundscapeTrackDao {

    @Query("SELECT * FROM soundscape_track ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<SoundscapeTrackEntity>>

    @Query("SELECT * FROM soundscape_track ORDER BY sortOrder, id")
    suspend fun getAll(): List<SoundscapeTrackEntity>

    @Transaction
    suspend fun replaceAll(rows: List<SoundscapeTrackEntity>) {
        clear()
        insertAll(rows)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<SoundscapeTrackEntity>)

    @Query("DELETE FROM soundscape_track")
    suspend fun clear()
}
