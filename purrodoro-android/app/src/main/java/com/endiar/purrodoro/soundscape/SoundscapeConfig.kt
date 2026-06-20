package com.endiar.purrodoro.soundscape

/**
 * Cloud soundscape audio is served from a public R2 bucket. Catalog metadata
 * still lives in Supabase Postgres (`soundscape_tracks`); only the MP3 bytes
 * are on R2.
 */
object SoundscapeConfig {
    const val R2_PUBLIC_URL = "https://pub-a2af288fd41b4193b1cd198233eb484d.r2.dev"
}
