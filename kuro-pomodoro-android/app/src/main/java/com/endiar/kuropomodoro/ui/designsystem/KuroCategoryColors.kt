package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Deterministic color palette for category dots on task rows.
 *
 * The redesign drops first-letter avatars in favor of small color dots tied
 * to a task's category — gives users a real scanning signal ("blue dot = Work")
 * without forcing them to read text on every row.
 *
 * Colors are derived from a fixed palette indexed by category id. This avoids
 * a data-model migration: [com.endiar.kuropomodoro.model.Category] does not
 * yet carry a user-pickable color field. When that field arrives, callers can
 * keep using [categoryColorFor] as a fallback for legacy rows.
 *
 * Tasks without a category fall back to a neutral muted dot supplied by the
 * caller (typically `tokens.textMuted`).
 */
private val palette: List<Color> = listOf(
    Color(0xFF5B8DB6), // blue (Work)
    Color(0xFFE89A6F), // peach (Personal)
    Color(0xFF9B7CC1), // lilac (Study)
    Color(0xFF7FBFA3), // mint
    Color(0xFFD09467), // tan
    Color(0xFFC26861), // brick
    Color(0xFF6FAF98), // sage
    Color(0xFFB07AA1), // mauve
)

fun categoryColorFor(categoryId: Int?, fallback: Color): Color {
    if (categoryId == null) return fallback
    // Use absolute value so synthetic / legacy negative ids still index safely.
    val idx = (kotlin.math.abs(categoryId) - 1).coerceAtLeast(0) % palette.size
    return palette[idx]
}
