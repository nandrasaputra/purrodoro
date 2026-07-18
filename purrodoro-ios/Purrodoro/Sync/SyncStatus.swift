//
//  SyncStatus.swift
//  Purrodoro
//
//  Latest sync pass outcome for Profile UI. Mirrors Android `sync/SyncStatus.kt`.
//

import Foundation
import Combine

struct SyncSnapshot: Equatable {
    var lastAttemptAtMillis: Int64?
    var eligible: Bool = false
    var result: SyncEngine.SyncResult?
}

enum SyncUiStatus: Equatable {
    case notSyncedYet
    case skipped
    case upToDate
    case incomplete
    case failed
}

struct SyncStatusPresentation: Equatable {
    var status: SyncUiStatus
    var lastAttemptAtMillis: Int64?
}

@MainActor
final class SyncStatusStore: ObservableObject {
    @Published private(set) var snapshot = SyncSnapshot()

    func record(eligible: Bool, result: SyncEngine.SyncResult?) {
        snapshot = SyncSnapshot(
            lastAttemptAtMillis: Int64(Date().timeIntervalSince1970 * 1000),
            eligible: eligible,
            result: result
        )
    }
}

func resolveSyncStatusPresentation(_ snapshot: SyncSnapshot) -> SyncStatusPresentation {
    let status: SyncUiStatus = {
        if snapshot.lastAttemptAtMillis == nil { return .notSyncedYet }
        if !snapshot.eligible { return .skipped }
        switch snapshot.result {
        case .success: return .upToDate
        case .partialFailure: return .incomplete
        case nil: return .failed
        }
    }()
    return SyncStatusPresentation(status: status, lastAttemptAtMillis: snapshot.lastAttemptAtMillis)
}

func resolveSyncPassDetail(eligible: Bool, result: SyncEngine.SyncResult?) -> String {
    if !eligible { return "Skipped — not eligible (guest or owner mismatch)" }
    switch result {
    case .success: return "Success"
    case .partialFailure(let tables): return "Partial failure: \(tables.joined(separator: ", "))"
    case nil: return "Failed or signed out"
    }
}

struct SyncStatusLabels: Equatable {
    var titleKey: String
    var accentIsDestructive: Bool
    var accentIsMuted: Bool
    var relativeTimeKey: String?
    var relativeTimeValue: Int?
    var subtitleKey: String?
}

func resolveSyncStatusLabels(
    _ presentation: SyncStatusPresentation,
    nowMillis: Int64
) -> SyncStatusLabels {
    let titleKey: String = {
        switch presentation.status {
        case .notSyncedYet: return "sync.status.notYet"
        case .skipped: return "sync.status.skipped"
        case .upToDate: return "sync.status.upToDate"
        case .incomplete: return "sync.status.incomplete"
        case .failed: return "sync.status.failed"
        }
    }()

    let accentIsDestructive = presentation.status == .failed
    let accentIsMuted = presentation.status == .notSyncedYet || presentation.status == .skipped

    guard let last = presentation.lastAttemptAtMillis,
          presentation.status != .skipped,
          presentation.status != .notSyncedYet else {
        return SyncStatusLabels(
            titleKey: titleKey,
            accentIsDestructive: accentIsDestructive,
            accentIsMuted: accentIsMuted,
            relativeTimeKey: nil,
            relativeTimeValue: nil,
            subtitleKey: nil
        )
    }

    let elapsedMs = max(0, nowMillis - last)
    let relative: (key: String, value: Int?) = {
        let seconds = elapsedMs / 1000
        if seconds < 45 { return ("sync.time.justNow", nil) }
        let minutes = seconds / 60
        if minutes < 60 { return ("sync.time.minutesAgo", Int(minutes)) }
        let hours = minutes / 60
        if hours < 24 { return ("sync.time.hoursAgo", Int(hours)) }
        return ("sync.time.stale", nil)
    }()

    let subtitleKey: String? = {
        switch presentation.status {
        case .upToDate: return "sync.status.lastSync"
        case .incomplete, .failed: return "sync.status.lastTried"
        default: return nil
        }
    }()

    return SyncStatusLabels(
        titleKey: titleKey,
        accentIsDestructive: accentIsDestructive,
        accentIsMuted: accentIsMuted,
        relativeTimeKey: relative.key,
        relativeTimeValue: relative.value,
        subtitleKey: subtitleKey
    )
}
