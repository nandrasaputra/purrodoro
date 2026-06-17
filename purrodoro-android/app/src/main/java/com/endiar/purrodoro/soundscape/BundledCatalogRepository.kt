package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.data.local.SoundscapeBundledDao
import com.endiar.purrodoro.data.local.SoundscapeBundledEntity
import kotlinx.coroutines.flow.Flow

/**
 * Owns the local snapshot of the bundled-catalog overrides. [refresh] replaces
 * the whole Room table with the server's rows (the catalog is tiny, so
 * full-snapshot semantics keep add/remove/replace trivially correct); failures
 * leave the cache untouched so the app keeps working offline.
 */
class BundledCatalogRepository(
    private val api: BundledCatalogApi,
    private val dao: SoundscapeBundledDao,
) {
    fun observeCatalog(): Flow<List<SoundscapeBundledEntity>> = dao.observeAll()

    suspend fun snapshot(): List<SoundscapeBundledEntity> = dao.getAll()

    suspend fun refresh(): Boolean = runCatching {
        val rows = api.fetchActive().map { it.toEntity() }
        dao.replaceAll(rows)
    }.isSuccess
}

private fun BundledCatalogDto.toEntity() = SoundscapeBundledEntity(
    kind = kind,
    localId = localId,
    name = name,
    artist = artist,
    symbol = symbol,
    rawResource = rawResource,
    isPremium = isPremium,
    sortOrder = sortOrder,
    isActive = isActive,
)
