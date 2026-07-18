//
//  MainActorGate.swift
//  Purrodoro
//
//  Safe MainActor hop from sync table adapters.
//
//  `await` of a `@MainActor` closure deadlocks when the caller is already on the
//  main thread (schedule+wait for self). Inline via `assumeIsolated` in that case.
//

import Foundation

enum MainActorGate {
    static func perform<T: Sendable>(_ body: @MainActor @Sendable () throws -> T) async rethrows -> T {
        if Thread.isMainThread {
            return try MainActor.assumeIsolated { try body() }
        }
        return try await body()
    }
}
