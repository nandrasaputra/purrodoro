package com.endiar.purrodoro.soundscape

/**
 * Cloud soundscape audio is served from a public R2 bucket. Catalog metadata
 * still lives in Supabase Postgres (`soundscape_tracks`); only the MP3 bytes
 * are on R2.
 */
object SoundscapeConfig {
    const val R2_PUBLIC_URL = "https://pub-a2af288fd41b4193b1cd198233eb484d.r2.dev"

    /** R2 object key for a track thumbnail, falling back to the audio stem + .webp. */
    fun thumbnailKey(thumbnailPath: String, filePath: String = ""): String {
        val fromCatalog = thumbnailPath.trim().trimStart('/')
        if (fromCatalog.isNotEmpty()) return fromCatalog
        return filePath.trim().trimStart('/')
            .replace(Regex("\\.mp3$", RegexOption.IGNORE_CASE), ".webp")
    }

    /** Public HTTPS URL for a catalog thumbnail object key. */
    fun thumbnailUrl(thumbnailPath: String): String? {
        val key = thumbnailKey(thumbnailPath)
        if (key.isEmpty()) return null
        return "${R2_PUBLIC_URL.trimEnd('/')}/$key"
    }

    /** Resolve thumbnail key from catalog + audio paths, then build the public URL. */
    fun thumbnailUrlForTrack(thumbnailPath: String, filePath: String = ""): String? {
        val key = thumbnailKey(thumbnailPath, filePath)
        if (key.isEmpty()) return null
        return "${R2_PUBLIC_URL.trimEnd('/')}/$key"
    }
}
