//
//  CrashReporting.swift
//  Purrodoro
//
//  Firebase Crashlytics bootstrap + breadcrumbs. Mirrors Android
//  `PurrodoroApplication` / `AuthDiagnostics`: collection is off in DEBUG,
//  and configure is a no-op until `GoogleService-Info.plist` is present.
//

import Foundation

#if canImport(FirebaseCore)
import FirebaseCore
#endif
#if canImport(FirebaseCrashlytics)
import FirebaseCrashlytics
#endif

enum CrashReporting {
    /// Call once at launch. Safe when the plist is missing (local/CI without
    /// Firebase secrets) — skips configure entirely.
    static func start() {
        #if canImport(FirebaseCore) && canImport(FirebaseCrashlytics)
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            return
        }
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #else
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        #endif
        #endif
    }

    /// Release-safe breadcrumb (Android `AuthDiagnostics.log` / billing logs).
    static func log(_ event: String, detail: String? = nil) {
        #if canImport(FirebaseCrashlytics)
        guard FirebaseApp.app() != nil else { return }
        let message: String
        if let detail, !detail.isEmpty {
            message = "\(event): \(detail)"
        } else {
            message = event
        }
        Crashlytics.crashlytics().log(String(message.prefix(200)))
        #endif
    }

    /// Records a non-fatal error when Crashlytics is active.
    static func record(_ error: Error, userInfo: [String: Any] = [:]) {
        #if canImport(FirebaseCrashlytics)
        guard FirebaseApp.app() != nil else { return }
        let existing = error as NSError
        var info = existing.userInfo
        for (key, value) in userInfo { info[key] = value }
        let nsError = NSError(domain: existing.domain, code: existing.code, userInfo: info)
        Crashlytics.crashlytics().record(error: nsError)
        #endif
    }
}
