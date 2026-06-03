package com.endiar.purrodoro.ui.designsystem

import androidx.compose.ui.graphics.Color
import com.endiar.purrodoro.model.Category

/**
 * Color palette for category dots / icons on task rows.
 *
 * Categories now carry a user-pickable [Category.colorArgb]. When that field is
 * set, callers should resolve via [categoryColorFor] (the [Category] overload),
 * which honors the picked color. For legacy categories created before the picker
 * — and as the source list for the picker UI — we fall back to this deterministic
 * palette indexed by category id.
 *
 * Saturated (not pastel) so the colors stay legible both as a small dot and as an
 * icon tint on the light card surface.
 *
 * Tasks without a category fall back to a neutral muted color supplied by the
 * caller (typically `tokens.textMuted`).
 */
val categoryColorChoices: List<Long> = listOf(
    0xFF5B8DB6L, // blue
    0xFFE89A6FL, // peach
    0xFF9B7CC1L, // lilac
    0xFF7FBFA3L, // mint
    0xFFD09467L, // tan
    0xFFC26861L, // brick
    0xFF6FAF98L, // sage
    0xFFB07AA1L, // mauve
)

private val palette: List<Color> = categoryColorChoices.map { Color(it.toInt()) }

/**
 * Deterministic palette color for a category id. Used as the fallback for
 * categories that predate the color picker (no stored [Category.colorArgb]).
 */
fun categoryColorFor(categoryId: Int?, fallback: Color): Color {
    if (categoryId == null) return fallback
    // Use absolute value so synthetic / legacy negative ids still index safely.
    val idx = (kotlin.math.abs(categoryId) - 1).coerceAtLeast(0) % palette.size
    return palette[idx]
}

/**
 * Resolves the display color for a [category]: the user-picked color when set,
 * otherwise the deterministic palette fallback. Returns [fallback] when there is
 * no category at all.
 */
fun categoryColorFor(category: Category?, fallback: Color): Color {
    if (category == null) return fallback
    category.colorArgb?.let { return Color(it.toInt()) }
    return categoryColorFor(category.id, fallback)
}
