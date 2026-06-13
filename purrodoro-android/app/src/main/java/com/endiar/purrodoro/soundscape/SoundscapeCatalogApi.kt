package com.endiar.purrodoro.soundscape

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire-format DTO for public.soundscape_tracks. Field names match
// supabase/migrations/0004_soundscape_catalog.sql exactly, mirroring the
// pattern in sync/SyncDtos.kt. RLS already restricts reads to active rows.

@Serializable
data class SoundscapeTrackDto(
    @SerialName("id") val id: Int,
    @SerialName("kind") val kind: String,
    @SerialName("name") val name: String,
    @SerialName("artist") val artist: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("file_size_bytes") val fileSizeBytes: Long,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("updated_at") val updatedAt: String,
)

/** Seam for tests; production impl is [SupabaseSoundscapeCatalogApi]. */
interface SoundscapeCatalogApi {
    suspend fun fetchActiveTracks(): List<SoundscapeTrackDto>
}

class SupabaseSoundscapeCatalogApi(
    private val client: SupabaseClient,
) : SoundscapeCatalogApi {
    override suspend fun fetchActiveTracks(): List<SoundscapeTrackDto> =
        client.from("soundscape_tracks").select().decodeList()
}
