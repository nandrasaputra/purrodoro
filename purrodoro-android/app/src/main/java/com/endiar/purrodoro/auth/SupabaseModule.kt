package com.endiar.purrodoro.auth

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient

/**
 * Process-wide [SupabaseClient] singleton, configured with:
 *  - the Auth module using a Keystore-backed [EncryptedSessionManager], and
 *  - the ComposeAuth module wired for native Google sign-in via Credential
 *    Manager (the server client id is the OAuth Web client id).
 *
 * The Ktor OkHttp engine on the classpath is auto-detected by supabase-kt.
 */
object SupabaseModule {

    @Volatile
    private var instance: SupabaseClient? = null

    fun get(context: Context): SupabaseClient =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(appContext: Context): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                sessionManager = EncryptedSessionManager(appContext)
            }
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
            }
        }
}
