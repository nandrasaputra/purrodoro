package com.endiar.purrodoro.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette (warm coral / cream / cocoa) ─────────────────────────────
val Cream = Color(0xFFFFF8ED)
val WarmCream = Color(0xFFFFEFD8)
val CardWhite = Color(0xFFFFFCF7)
val Peach = Color(0xFFFFB59E)
val Coral = Color(0xFFFF7F73)
val DockCoral = Color(0xFFFF8B6F)
val DeepCoral = Color(0xFFC64432)        // deep terracotta; White on this = 4.91:1 ✓
val Blush = Color(0xFFFFD8DF)
val Lavender = Color(0xFFD8C8FF)
val Mint = Color(0xFFCDEEDB)
val JourneyGreen = Color(0xFF8BA05E)
val Sky = Color(0xFFCFEAF8)
val Cocoa = Color(0xFF5F4638)
val CocoaSoft = Color(0xFF8B6B5A)

// ── Dark scheme tokens ─────────────────────────────────────────────────────
val NightDeep = Color(0xFF0A0816)        // page background
val NightCard = Color(0xFF1A1428)        // flat-surface card
val NightCardActive = Color(0xFF2B1720)  // selected card (e.g. active sound)
val NightBorder = Color(0xFF2B2440)      // card border
val NightDock = Color(0xFF251E3A)        // bottom dock — elevated above cards
val NightDockBorder = Color(0xFF3A3354)  // dock hairline (lighter than card border)
val NightDivider = Color(0xFF231D38)     // hairline between rows
val NightMuted = Color(0xFFB8B0C8)       // text-secondary
val NightFaint = Color(0xFF7D768B)       // text-muted — contrast 4.37→4.56:1
val NightFainter = Color(0xFF4A4458)     // text-faint (calendar empty cells)

// Legacy alias — kept so Theme.kt and KuroComponents.kt still compile during migration
val Night = NightDeep
val NightCardLegacy = Color(0xFF342C3F)  // old value used in existing Theme.kt surface

// ── Light scheme tokens ────────────────────────────────────────────────────
val PageWhite = Color(0xFFFAF5F2)    // flat-screen page background (all tabs)
val CreamBorder = Color(0xFFEBDFCC)
val CreamDock = Color(0xFFEFE0CB)        // bottom dock — warm sand, clearly distinct from white cards + page
val CreamDockBorder = Color(0xFFDFCBAC)  // dock hairline (deeper than card border for definition)
val CreamCardActive = Color(0xFFFFE9DD)
val CreamDivider = Color(0xFFEFE3D0)
val CocoaFaint = Color(0xFFA89486)       // text-muted
val CocoaFainter = Color(0xFFC9B9A8)     // text-faint
val LightBorderSoft = Color(0x1F5F4638) // 12% cocoa — stepper outlines, segmented track

// ── Destructive / error ────────────────────────────────────────────────────
val DestructiveRed = Color(0xFFE53935)   // delete / swipe-to-dismiss / danger actions

// ── Premium / locked accent ────────────────────────────────────────────────
val LockedPurple = Color(0xFF7B4FBF)
val LockedPurpleLight = Color(0xFF9B7FCC)
val LockedPurpleText = Color(0xFFD8C8FF)
val LockedPurpleTextDark = Color(0xFF6B43AA)

// ── Sound icon tile palette (per-sound-id × scheme) ────────────────────────
// Light mode fills (id 1=Rainy Window, 2=Cafe, 3=Forest, 4=Fire, 5=Keys)
val TileRainLight = Blush
val TileCafeLight = Color(0xFFEFE6FF)
val TileForestLight = Color(0xFFDDF1E5)
val TileFireLight = Color(0xFFFFE2D5)
val TileKeysLight = Color(0xFFDDE7F5)

// Dark mode fills
val TileRainDark = DockCoral.copy(alpha = 0.18f)   // note: alpha baked in at definition
val TileCafeDark = Color(0xFF3F2A4A)
val TileForestDark = Color(0xFF1D3326)
val TileFireDark = Color(0xFF4A2218)
val TileKeysDark = Color(0xFF1F2A3F)

// Lo-fi vinyl label fills (light mode; dark uses the same hues at lower alpha)
val LofiStudyLight = Color(0xFFFFC9A8)
val LofiJazzLight = Color(0xFFD4C4F5)
val LofiPianoLight = Color(0xFFB8D9F0)
val LofiChillLight = Color(0xFFFFDDB8)
val LofiStudyDark = Color(0xFF4A2E1F)
val LofiJazzDark = Color(0xFF2E2248)
val LofiPianoDark = Color(0xFF1A3048)
val LofiChillDark = Color(0xFF4A3018)
