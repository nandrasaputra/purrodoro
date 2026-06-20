package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.data.local.SoundscapeTrackDao
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow

/** Cloud tracks must use ids >= this; below is reserved for bundled tracks. */
const val REMOTE_TRACK_ID_FLOOR = 100

/**
 * Owns the local snapshot of the cloud catalog. [refresh] replaces the whole
 * Room table with the server's active rows (the catalog is tiny, so
 * full-snapshot semantics keep add/remove/replace trivially correct);
 * failures leave the cache untouched so the app keeps working offline.
 */
class SoundscapeCatalogRepository(
    private val api: SoundscapeCatalogApi,
    private val dao: SoundscapeTrackDao,
) {

    fun observeCatalog(): Flow<List<SoundscapeTrackEntity>> = dao.observeAll()

    /**
     * Current cache contents. Used by the startup prune pass — pruning must
     * run against the cache, never against a possibly-still-empty UI state
     * (see Task 7), or it would wipe all downloads on launch.
     */
    suspend fun snapshot(): List<SoundscapeTrackEntity> = dao.getAll()

    suspend fun refresh(): Boolean = runCatching {
        val rows = api.fetchActiveTracks()
            .filter { it.id >= REMOTE_TRACK_ID_FLOOR }
            .map { it.toEntity() }
        dao.replaceAll(rows)
    }.isSuccess
}

private fun SoundscapeTrackDto.toEntity() = SoundscapeTrackEntity(
    id = id,
    kind = kind,
    name = name,
    artist = artist,
    symbol = symbol,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    fileSizeBytes = fileSizeBytes,
    isPremium = isPremium,
    sortOrder = sortOrder,
    updatedAtMillis = OffsetDateTime.parse(updatedAt).toInstant().toEpochMilli(),
)
