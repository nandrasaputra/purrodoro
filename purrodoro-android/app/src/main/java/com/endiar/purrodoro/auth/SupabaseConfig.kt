package com.endiar.purrodoro.auth

/**
 * Static configuration for the Supabase backend.
 *
 * [URL] and [PUBLISHABLE_KEY] are safe to ship in the client — the publishable
 * key only grants the anon role and all data access is gated by RLS.
 *
 * [GOOGLE_WEB_CLIENT_ID] is the OAuth **Web** client ID (not the Android one)
 * created in the Google Cloud Console; Credential Manager hands Supabase a
 * Google ID token minted for this audience, which Supabase verifies during the
 * `signInWithIdToken` exchange. The separate **Android** client ID (tied to the
 * app's package + signing SHA-1) is what lets Credential Manager surface the
 * account picker on-device — it is configured in Google Cloud, not referenced
 * here.
 *
 * Setup checklist (manual — done once, outside the app):
 *  1. Google Cloud Console → Credentials → create an OAuth **Web** client and an
 *     **Android** client (package `com.endiar.purrodoro` + release/debug SHA-1).
 *  2. Paste the Web client ID below.
 *  3. Supabase dashboard → Authentication → Providers → Google: enable it and
 *     add the Web client ID (and its secret) there too. Keep anonymous sign-in
 *     OFF (guests are local-only by design).
 */
object SupabaseConfig {
    const val URL = "https://birjkyerhokqdzhdfgpt.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_GgfnZ3nRLCm-_HBfJfbWVw_4K2D_KRu"

    // TODO(setup): replace with the real Google OAuth *Web* client ID.
    const val GOOGLE_WEB_CLIENT_ID =
        "REPLACE_WITH_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"

    /** Guard so the UI can surface a clear message instead of a cryptic failure. */
    val isGoogleConfigured: Boolean
        get() = !GOOGLE_WEB_CLIENT_ID.startsWith("REPLACE_WITH_")
}
