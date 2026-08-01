//
//  DeepLinkRouter.swift
//  Purrodoro
//
//  Parses `purrodoro://` URLs from widgets / external opens and surfaces
//  pending navigation (tab switch, auto Start Focus). Mirrors Android
//  `WidgetActions` extras handled in `MainActivity`.
//

import Foundation
import Combine

enum DeepLinkAction: Equatable {
    case openApp
    case openTab(AppTab)
    case startFocus
}

enum DeepLink {
    static let scheme = "purrodoro"

    static func url(for action: DeepLinkAction) -> URL {
        switch action {
        case .openApp:
            return URL(string: "\(scheme)://")!
        case .openTab(let tab):
            return URL(string: "\(scheme)://tab/\(tab.rawValue)")!
        case .startFocus:
            return URL(string: "\(scheme)://start-focus")!
        }
    }

    static func parse(_ url: URL) -> DeepLinkAction? {
        guard url.scheme?.lowercased() == scheme else { return nil }
        let host = (url.host ?? "").lowercased()
        let path = url.path.trimmingCharacters(in: CharacterSet(charactersIn: "/")).lowercased()

        if host.isEmpty || host == "open" {
            return .openApp
        }
        if host == "start-focus" || (host == "start" && path == "focus") {
            return .startFocus
        }
        if host == "tab" {
            if let tab = AppTab(rawValue: path) { return .openTab(tab) }
            return .openApp
        }
        // Also accept purrodoro://focus style hosts matching tab names.
        if let tab = AppTab(rawValue: host) {
            return .openTab(tab)
        }
        return .openApp
    }
}

@MainActor
final class DeepLinkRouter: ObservableObject {
    /// Tab to select on the next RootView apply pass.
    @Published var pendingTab: AppTab?
    /// When true, RootView should open the Start Focus sheet on the Focus tab.
    @Published var pendingOpenStartFocus = false

    func handle(_ url: URL) -> Bool {
        guard let action = DeepLink.parse(url) else { return false }
        apply(action)
        return true
    }

    func apply(_ action: DeepLinkAction) {
        switch action {
        case .openApp:
            pendingTab = .focus
        case .openTab(let tab):
            pendingTab = tab
        case .startFocus:
            pendingTab = .focus
            pendingOpenStartFocus = true
        }
    }

    func consumeStartFocus() {
        pendingOpenStartFocus = false
    }

    func consumeTab() -> AppTab? {
        let tab = pendingTab
        pendingTab = nil
        return tab
    }
}
