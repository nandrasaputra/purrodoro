package com.endiar.purrodoro.soundscape

import android.content.Context
import com.endiar.purrodoro.audio.TrackDownloadManager
import com.endiar.purrodoro.soundscape.SoundscapeConfig
import com.endiar.purrodoro.auth.SupabaseModule
import com.endiar.purrodoro.data.local.PurrodoroDatabase

/**
 * Process-wide singletons for the cloud soundscape catalog: the Postgrest-
 * backed catalog repository and the file download manager. Mirrors the
 * [SupabaseModule]/SyncModule pattern.
 */
object SoundscapeModule {

    @Volatile
    private var instance: Holder? = null

    class Holder(
        val catalogRepository: SoundscapeCatalogRepository,
        val bundledCatalogRepository: BundledCatalogRepository,
        val downloadManager: TrackDownloadManager,
        val thumbnailLoader: SoundscapeThumbnailLoader,
    )

    fun get(context: Context): Holder =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(appContext: Context): Holder {
        val db = PurrodoroDatabase.get(appContext)
        val httpClient = SoundscapeHttp.createKtorClient()
        return Holder(
            catalogRepository = SoundscapeCatalogRepository(
                api = SupabaseSoundscapeCatalogApi(SupabaseModule.get(appContext)),
                dao = db.soundscapeTrackDao(),
            ),
            bundledCatalogRepository = BundledCatalogRepository(
                api = SupabaseBundledCatalogApi(SupabaseModule.get(appContext)),
                dao = db.soundscapeBundledDao(),
            ),
            downloadManager = TrackDownloadManager(
                context = appContext,
                httpClient = httpClient,
                audioPublicBaseUrl = SoundscapeConfig.R2_PUBLIC_URL,
            ),
            thumbnailLoader = SoundscapeThumbnailLoader(
                context = appContext,
                httpClient = httpClient,
                publicBaseUrl = SoundscapeConfig.R2_PUBLIC_URL,
            ),
        )
    }
}
