//
//  SyncRules.swift
//  Purrodoro
//
//  Row-level LWW rule for sync pulls. Mirrors Android `sync/SyncRules.kt`.
//

import Foundation

enum PullAction: Equatable {
    case apply
    case keepLocal
}

/// Dirty local row survives pull and re-pushes on next pass.
func resolvePull(localDirty: Bool?, remoteDeleted: Bool) -> PullAction {
    if localDirty == true { return .keepLocal }
    return .apply
}
