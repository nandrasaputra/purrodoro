//
//  Persistence.swift
//  Purrodoro
//
//  A minimal key-value persistence surface used by the app's stores. Putting
//  `UserDefaults` behind a protocol keeps it swappable — an in-memory fake for
//  tests today, a future iCloud-backed store tomorrow — without touching the
//  call sites in each store.
//

import Foundation

protocol KeyValueStore: AnyObject {
    func data(forKey defaultName: String) -> Data?
    func string(forKey defaultName: String) -> String?
    func bool(forKey defaultName: String) -> Bool
    func dictionary(forKey defaultName: String) -> [String: Any]?
    func set(_ value: Any?, forKey defaultName: String)
    /// Whether a value has been written for `defaultName` (distinguishes
    /// "missing" from `bool == false`).
    func contains(_ defaultName: String) -> Bool
}

extension UserDefaults: KeyValueStore {
    func contains(_ defaultName: String) -> Bool {
        object(forKey: defaultName) != nil
    }
}

/// A non-persistent `KeyValueStore` for tests and previews.
final class InMemoryKeyValueStore: KeyValueStore {
    private var values: [String: Any] = [:]

    init(_ seed: [String: Any] = [:]) { values = seed }

    func data(forKey defaultName: String) -> Data? { values[defaultName] as? Data }
    func string(forKey defaultName: String) -> String? { values[defaultName] as? String }
    func bool(forKey defaultName: String) -> Bool { values[defaultName] as? Bool ?? false }
    func dictionary(forKey defaultName: String) -> [String: Any]? { values[defaultName] as? [String: Any] }
    func contains(_ defaultName: String) -> Bool { values[defaultName] != nil }

    func set(_ value: Any?, forKey defaultName: String) {
        if let value { values[defaultName] = value } else { values.removeValue(forKey: defaultName) }
    }
}
