package com.endiar.purrodoro.soundscape

import android.content.Context
import com.endiar.purrodoro.audio.TrackDownloadManager
import com.endiar.purrodoro.auth.SupabaseConfig
import com.endiar.purrodoro.auth.SupabaseModule
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

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
    )

    fun get(context: Context): Holder =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(appContext: Context): Holder {
        val db = PurrodoroDatabase.get(appContext)
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
                httpClient = HttpClient(OkHttp),
                baseUrl = SupabaseConfig.URL,
            ),
        )
    }
}
