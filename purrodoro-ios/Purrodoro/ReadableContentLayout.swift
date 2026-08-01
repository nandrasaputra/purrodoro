//
//  ReadableContentLayout.swift
//  Purrodoro
//
//  iPad / regular-width layout helpers. On compact (phone) size class the
//  modifiers are no-ops so existing phone layouts stay unchanged. On regular
//  (iPad) they cap content to a readable column and center it.
//

import SwiftUI

enum ReadableContent {
    /// Comfortable form/list width on iPad landscape / portrait.
    static let columnMaxWidth: CGFloat = 720
    /// Slightly narrower floating dock so it doesn't span the full iPad width.
    static let dockMaxWidth: CGFloat = 580
}

struct ReadableContentWidthModifier: ViewModifier {
    @Environment(\.horizontalSizeClass) private var sizeClass
    var maxWidth: CGFloat = ReadableContent.columnMaxWidth

    func body(content: Content) -> some View {
        content
            .frame(maxWidth: sizeClass == .regular ? maxWidth : .infinity)
            .frame(maxWidth: .infinity)
    }
}

extension View {
    /// Caps width and centers on regular size class (iPad).
    func readableContentWidth(_ maxWidth: CGFloat = ReadableContent.columnMaxWidth) -> some View {
        modifier(ReadableContentWidthModifier(maxWidth: maxWidth))
    }

    /// Caps and centers the bottom dock on regular size class.
    func readableDockWidth(_ maxWidth: CGFloat = ReadableContent.dockMaxWidth) -> some View {
        modifier(ReadableContentWidthModifier(maxWidth: maxWidth))
    }
}
