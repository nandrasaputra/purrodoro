//
//  SyncLogout.swift
//  Purrodoro
//
//  Pre-logout flush decision. Mirrors Android `sync/LogoutFlush.kt`.
//

import Foundation

enum LogoutDecision: Equatable {
    case signOut
    case warnUnsynced
}

func logoutDecision(flushSucceeded: Bool, dirtyRemaining: Bool) -> LogoutDecision {
    if !flushSucceeded && dirtyRemaining { return .warnUnsynced }
    return .signOut
}
