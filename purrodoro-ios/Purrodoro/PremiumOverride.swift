//
//  PremiumOverride.swift
//  Purrodoro
//

#if DEBUG
enum PremiumOverride {
    static var forcedValue: Bool?

    static func overrideValue() -> Bool? {
        forcedValue
    }
}
#else
enum PremiumOverride {
    static func overrideValue() -> Bool? {
        nil
    }
}
#endif
