//
//  SupabaseConfig.swift
//  Purrodoro
//
//  Static Supabase + OAuth client configuration. Mirrors Android
//  `auth/SupabaseConfig.kt`. Publishable key is safe in the client (RLS gates data).
//

import Foundation

enum SupabaseConfig {
    static let url = URL(string: "https://birjkyerhokqdzhdfgpt.supabase.co")!
    static let publishableKey = "sb_publishable_GgfnZ3nRLCm-_HBfJfbWVw_4K2D_KRu"

    /// Web client ID — audience for the Google ID token Supabase verifies.
    static let googleWebClientID =
        "779946032719-he3g7a7e4fog471u71ls69cpig8a4mth.apps.googleusercontent.com"

    /// iOS OAuth client ID from Google Cloud (bundle `com.endiar.Purrodoro`).
    /// Create under Credentials → OAuth client → iOS, then paste here.
    static let googleIOSClientID = "REPLACE_WITH_IOS_CLIENT_ID"

    static var isGoogleConfigured: Bool {
        !googleWebClientID.hasPrefix("REPLACE_WITH_")
            && !googleIOSClientID.hasPrefix("REPLACE_WITH_")
    }
}
