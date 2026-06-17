package com.endiar.purrodoro.soundscape

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire-format DTO for public.soundscape_bundled_catalog. Field names match
 * supabase/migrations/0005_soundscape_bundled_catalog.sql exactly. RLS allows
 * public read of all rows (active and inactive) so the app can tell a hidden
 * track apart from one not yet seeded.
 */
@Serializable
data class BundledCatalogDto(
    @SerialName("kind") val kind: String,
    @SerialName("local_id") val localId: Int,
    @SerialName("name") val name: String,
    @SerialName("artist") val artist: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("raw_resource") val rawResource: String,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("is_active") val isActive: Boolean,
)

/** Seam for tests; production impl is [SupabaseBundledCatalogApi]. */
interface BundledCatalogApi {
    suspend fun fetchActive(): List<BundledCatalogDto>
}

class SupabaseBundledCatalogApi(
    private val client: SupabaseClient,
) : BundledCatalogApi {
    override suspend fun fetchActive(): List<BundledCatalogDto> =
        client.from("soundscape_bundled_catalog").select().decodeList()
}
