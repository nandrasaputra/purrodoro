package com.endiar.purrodoro.auth

import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Release-safe breadcrumbs for Google / Supabase sign-in failures. */
internal object AuthDiagnostics {
    fun log(event: String, detail: String? = null) {
        runCatching {
            val message = if (detail.isNullOrBlank()) event else "$event: $detail"
            FirebaseCrashlytics.getInstance().log(message.take(200))
        }
    }
}
