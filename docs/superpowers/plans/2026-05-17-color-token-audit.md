# Color Token Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix six WCAG AA contrast failures in light/dark color tokens while preserving the warm coral brand aesthetic for decorative uses.

**Architecture:** Interactive/Display Split — add `interactiveFill`/`interactiveText` token pair to `KuroSurfaceTokens` so buttons and active tab indicators use accessible colors, while decorative elements (timer ring, cycle dots, backdrop tints) keep the existing mid-tone coral. Two palette constants change: `NightFaint` gets a 4-unit brightness nudge and `DeepCoral` is added.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AndroidX DataStore, JUnit 4 local unit tests.

---

## File Map

| File | Change |
|---|---|
| `app/src/test/java/com/endiar/purrodoro/ui/theme/ColorContrastTest.kt` | **Create** — WCAG regression tests |
| `app/src/main/java/com/endiar/purrodoro/ui/theme/Color.kt` | **Modify** — add `DeepCoral`, bump `NightFaint` |
| `app/src/main/java/com/endiar/purrodoro/ui/theme/SurfaceTokens.kt` | **Modify** — add `interactiveFill`/`interactiveText` fields; fix light `textMuted` |
| `app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt` | **Modify** — 6 component updates to use new tokens |

All paths are relative to `purrodoro-android/`.

---

## Task 1 — Write Failing WCAG Contrast Tests

**Files:**
- Create: `app/src/test/java/com/endiar/purrodoro/ui/theme/ColorContrastTest.kt`

These tests import the actual `Color` constants from `Color.kt`. Two of them fail with the current values — `NightFaint` (4.37:1, needs 4.5:1) and `CocoaFaint` (2.75:1, needs 4.5:1). The other two are regressions verifying currently-passing pairs don't regress.

- [ ] **Step 1: Create the test file**

```kotlin
package com.endiar.purrodoro.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {

    // ── WCAG math ──────────────────────────────────────────────────────────────

    private fun linearize(c: Float): Double =
        if (c <= 0.04045f) c / 12.92 else ((c + 0.055f) / 1.055f).toDouble().pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * linearize(color.red) +
        0.7152 * linearize(color.green) +
        0.0722 * linearize(color.blue)

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    // ── Failing tests (RED) — fix these by updating Color.kt ──────────────────

    @Test
    fun `NightFaint on NightDeep meets WCAG AA 4-5 to 1`() {
        // Current NightFaint = #7A7388 → 4.37:1 — FAILS
        // After fix: #7D768B → 4.56:1 — PASSES
        assertTrue(
            "NightFaint on NightDeep: ${contrastRatio(NightFaint, NightDeep)}",
            contrastRatio(NightFaint, NightDeep) >= 4.5
        )
    }

    @Test
    fun `light textMuted CocoaFaint on Cream meets WCAG AA 4-5 to 1`() {
        // Current CocoaFaint = #A89486 on Cream → 2.75:1 — FAILS
        // After fix: textMuted switches to CocoaSoft (#8B6B5A) → 4.57:1 — PASSES
        // This test documents the requirement; update the assertion color to CocoaSoft
        // after Task 3 promotes CocoaSoft to textMuted in LightTokens.
        assertTrue(
            "CocoaFaint on Cream: ${contrastRatio(CocoaFaint, Cream)}",
            contrastRatio(CocoaFaint, Cream) >= 4.5
        )
    }

    // ── Regression tests (stay GREEN) ─────────────────────────────────────────

    @Test
    fun `NightMuted on NightDeep stays above 9 to 1`() {
        assertTrue(contrastRatio(NightMuted, NightDeep) >= 9.0)
    }

    @Test
    fun `Cocoa on Cream stays above 8 to 1`() {
        assertTrue(contrastRatio(Cocoa, Cream) >= 8.0)
    }
}
```

- [ ] **Step 2: Run tests — verify two fail**

```bash
cd purrodoro-android
./gradlew :app:test --tests "com.endiar.purrodoro.ui.theme.ColorContrastTest" --info 2>&1 | grep -E "FAIL|PASS|ERROR|tests were"
```

Expected output contains:
```
NightFaint on NightDeep meets WCAG AA 4-5 to 1 FAILED
light textMuted CocoaFaint on Cream meets WCAG AA 4-5 to 1 FAILED
NightMuted on NightDeep stays above 9 to 1 PASSED
Cocoa on Cream stays above 8 to 1 PASSED
```

---

## Task 2 — Update `Color.kt`

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/ui/theme/Color.kt`

Two changes: bump `NightFaint` from `#7A7388` (4.37:1) to `#7D768B` (4.56:1), and add `DeepCoral` for the new light-mode interactive fill.

- [ ] **Step 1: Apply the changes**

In `Color.kt`, replace the existing `NightFaint` line:
```kotlin
// Before
val NightFaint = Color(0xFF7A7388)       // text-muted

// After
val NightFaint = Color(0xFF7D768B)       // text-muted — #7A7388→#7D768B, contrast 4.37→4.56:1
```

Add `DeepCoral` in the brand palette section, after `DockCoral`:
```kotlin
// After DockCoral = Color(0xFFFF8B6F)
val DeepCoral = Color(0xFFC64432)        // deep terracotta; White on this = 4.91:1 ✓
```

- [ ] **Step 2: Run the NightFaint test — verify it now passes**

```bash
./gradlew :app:test --tests "com.endiar.purrodoro.ui.theme.ColorContrastTest.NightFaint on NightDeep meets WCAG AA 4-5 to 1"
```

Expected:
```
ColorContrastTest > NightFaint on NightDeep meets WCAG AA 4-5 to 1 PASSED
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/ui/theme/Color.kt \
        app/src/test/java/com/endiar/purrodoro/ui/theme/ColorContrastTest.kt
git commit -m "fix(tokens): bump NightFaint to 4.56:1 and add DeepCoral"
```

---

## Task 3 — Update `SurfaceTokens.kt`

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/ui/theme/SurfaceTokens.kt`

Three changes:
1. Add `interactiveFill` and `interactiveText` fields to `KuroSurfaceTokens`.
2. Populate both schemes with accessible token pairs.
3. Fix `LightTokens.textMuted`: promote `CocoaSoft` (4.57:1) from `CocoaFaint` (2.75:1).

- [ ] **Step 1: Add new fields to the data class**

In `SurfaceTokens.kt`, the `KuroSurfaceTokens` data class currently ends at `lockedText`. Add two fields at the end:

```kotlin
data class KuroSurfaceTokens(
    val pageBackground: Color,
    val cardFill: Color,
    val activeCardFill: Color,
    val cardBorder: Color,
    val divider: Color,
    val borderSoft: Color,
    val segmentedTrack: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentOn: Color,
    val lockedFill: Color,
    val lockedBorder: Color,
    val lockedText: Color,
    val interactiveFill: Color,   // button container + active tab/pill background
    val interactiveText: Color,   // text/icon color on interactiveFill
)
```

- [ ] **Step 2: Populate DarkTokens**

Add the two new fields at the end of the `DarkTokens` initializer:
```kotlin
private val DarkTokens = KuroSurfaceTokens(
    // … all existing fields unchanged …
    lockedFill = LockedPurple.copy(alpha = 0.25f),
    lockedBorder = LockedPurple.copy(alpha = 0.45f),
    lockedText = LockedPurpleText,
    interactiveFill = Peach,   // #FFB59E — Cocoa on Peach = 5.10:1 ✓
    interactiveText = Cocoa,
)
```

- [ ] **Step 3: Populate and fix LightTokens**

Two changes to `LightTokens`: add new fields, and change `textMuted` from `CocoaFaint` to `CocoaSoft`, demoting `CocoaFaint` to `textFaint`:

```kotlin
private val LightTokens = KuroSurfaceTokens(
    pageBackground = Cream,
    cardFill = CardWhite,
    activeCardFill = CreamCardActive,
    cardBorder = CreamBorder,
    divider = CreamDivider,
    borderSoft = LightBorderSoft,
    segmentedTrack = LightBorderSoft,
    textPrimary = Cocoa,
    textSecondary = CocoaSoft,
    textMuted = CocoaSoft,        // was CocoaFaint (2.75:1 ✗) → CocoaSoft (4.57:1 ✓)
    textFaint = CocoaFaint,       // demoted from textMuted; decorative/calendar empty cells
    accent = Coral,
    accentSoft = Blush,
    accentOn = Color.White,
    lockedFill = LockedPurpleLight.copy(alpha = 0.15f),
    lockedBorder = LockedPurpleLight,
    lockedText = LockedPurpleTextDark,
    interactiveFill = DeepCoral,  // #C64432 — White on DeepCoral = 4.91:1 ✓
    interactiveText = Color.White,
)
```

- [ ] **Step 4: Update the failing test to use CocoaSoft (now the textMuted token)**

In `ColorContrastTest.kt`, the `light textMuted` test currently checks `CocoaFaint`. Now that `CocoaSoft` is the `textMuted` token, update the test to reflect the actual contract:

```kotlin
@Test
fun `light textMuted CocoaSoft on Cream meets WCAG AA 4-5 to 1`() {
    // LightTokens.textMuted = CocoaSoft (#8B6B5A) — 4.57:1 ✓
    assertTrue(
        "CocoaSoft on Cream: ${contrastRatio(CocoaSoft, Cream)}",
        contrastRatio(CocoaSoft, Cream) >= 4.5
    )
}
```

Also add two new tests for the interactive token pairs:

```kotlin
@Test
fun `dark interactiveFill Peach with Cocoa text meets WCAG AA`() {
    // DarkTokens.interactiveFill = Peach (#FFB59E), interactiveText = Cocoa (#5F4638)
    assertTrue(
        "Cocoa on Peach: ${contrastRatio(Cocoa, Peach)}",
        contrastRatio(Cocoa, Peach) >= 4.5
    )
}

@Test
fun `light interactiveFill DeepCoral with White text meets WCAG AA`() {
    // LightTokens.interactiveFill = DeepCoral (#C64432), interactiveText = White
    assertTrue(
        "White on DeepCoral: ${contrastRatio(Color.White, DeepCoral)}",
        contrastRatio(Color.White, DeepCoral) >= 4.5
    )
}
```

- [ ] **Step 5: Run all contrast tests — verify all pass**

```bash
./gradlew :app:test --tests "com.endiar.purrodoro.ui.theme.ColorContrastTest"
```

Expected:
```
ColorContrastTest > NightFaint on NightDeep meets WCAG AA 4-5 to 1 PASSED
ColorContrastTest > light textMuted CocoaSoft on Cream meets WCAG AA 4-5 to 1 PASSED
ColorContrastTest > dark interactiveFill Peach with Cocoa text meets WCAG AA PASSED
ColorContrastTest > light interactiveFill DeepCoral with White text meets WCAG AA PASSED
ColorContrastTest > NightMuted on NightDeep stays above 9 to 1 PASSED
ColorContrastTest > Cocoa on Cream stays above 8 to 1 PASSED
```

- [ ] **Step 6: Build the app to verify no compile errors from new data class fields**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

> If you see `No value passed for parameter 'interactiveFill'` or similar, it means a call site somewhere constructs `KuroSurfaceTokens(...)` directly. Search the codebase for `KuroSurfaceTokens(` and add the two new fields to that call site.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/ui/theme/SurfaceTokens.kt \
        app/src/test/java/com/endiar/purrodoro/ui/theme/ColorContrastTest.kt
git commit -m "fix(tokens): add interactiveFill/interactiveText; fix light textMuted to CocoaSoft"
```

---

## Task 4 — Update `KuroComponents.kt` — Interactive Token Swaps

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt`

Four components currently use `accent`/`accentOn` (coral) for interactive selected/active states. Replace with `interactiveFill`/`interactiveText`.

- [ ] **Step 1: Update `KuroPrimaryButton`**

Find the existing `KuroPrimaryButton` composable (~line 196). Change the `colors` parameter:

```kotlin
@Composable
fun KuroPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens   // add this line

    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.interactiveFill,    // was colorScheme.primary
            contentColor = tokens.interactiveText       // was colorScheme.onPrimary
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
```

Remove the `val colorScheme = MaterialTheme.colorScheme` line that was in this function if it is now unused.

- [ ] **Step 2: Update `DockTabItem`**

Find the private `DockTabItem` composable (~line 366). Change the active-state color derivation:

```kotlin
@Composable
private fun DockTabItem(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val kuroTokens = MaterialTheme.colorScheme.kuroTokens
    val iconColor = if (active) kuroTokens.interactiveText else kuroTokens.textMuted  // was accentOn / textMuted
    val bgColor   = if (active) kuroTokens.interactiveFill else Color.Transparent     // was accent / Transparent
    // … rest unchanged …
}
```

- [ ] **Step 3: Update `AppearanceModeToggle`**

Find the `AppearanceModeToggle` composable (~line 610). In the inner `Row` for each entry, change the background and text color:

```kotlin
Row(
    modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(11.dp))
        .background(if (selected) tokens.interactiveFill else Color.Transparent)  // was tokens.accent
        .clickable { onModeChanged(entry) }
        .padding(vertical = 8.dp, horizontal = 10.dp),
    // …
) {
    Text(
        text = icon,
        fontSize = 14.sp,
        color = if (selected) tokens.interactiveText else tokens.textSecondary,    // was tokens.accentOn
    )
    Spacer(Modifier.width(6.dp))
    Text(
        text = entry.label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = if (selected) tokens.interactiveText else tokens.textSecondary,    // was tokens.accentOn
    )
}
```

- [ ] **Step 4: Update `GlassPill`**

Find the `GlassPill` composable (~line 507). Change the selected fill and text color:

```kotlin
@Composable
fun GlassPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
    val kuroT = MaterialTheme.colorScheme.kuroTokens
    val fill      = if (selected) kuroT.interactiveFill else tokens.pillFill   // was kuroT.accent
    val textColor = if (selected) kuroT.interactiveText else kuroT.textSecondary // was kuroT.accentOn
    val borderColor = if (selected) Color.Transparent else tokens.pillBorder
    // … rest unchanged …
}
```

- [ ] **Step 5: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt
git commit -m "fix(components): use interactiveFill/interactiveText for buttons, dock, pills, toggle"
```

---

## Task 5 — Update `KuroComponents.kt` — Remove Alpha Text

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt`

Two components use `colorScheme.onSurface.copy(alpha = 0.72f)` for sub-label text. This produces ~2.8:1 contrast in light mode. Replace with `kuroTokens.textSecondary` (4.57:1 light / 9.52:1 dark).

- [ ] **Step 1: Fix `SectionTitle` subtitle**

Find the `SectionTitle` composable (~line 155). Change the subtitle `Text` color:

```kotlin
@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    val colorScheme = MaterialTheme.colorScheme
    val kuroTokens = MaterialTheme.colorScheme.kuroTokens   // add this line

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = colorScheme.onBackground)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = kuroTokens.textSecondary   // was colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}
```

- [ ] **Step 2: Fix `TimerRing` sub-label**

Find the `TimerRing` composable (~line 210). In the `Column` inside, fix the "stay with kuro" text color:

```kotlin
@Composable
fun TimerRing(timeText: String, progress: Float, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val kuroTokens = MaterialTheme.colorScheme.kuroTokens   // add this line

    Box(modifier = modifier.size(230.dp), contentAlignment = Alignment.Center) {
        // … Canvas unchanged …
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, style = MaterialTheme.typography.displaySmall, color = colorScheme.onBackground)
            Text(
                "stay with kuro",
                style = MaterialTheme.typography.bodyMedium,
                color = kuroTokens.textSecondary   // was colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}
```

- [ ] **Step 3: Build to verify**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run full test suite**

```bash
./gradlew :app:test 2>&1 | tail -10
```

Expected: All tests pass, no failures.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt
git commit -m "fix(components): replace onSurface.copy(alpha=0.72) with textSecondary token"
```

---

## Task 6 — Final Build Verification

- [ ] **Step 1: Full clean build**

```bash
./gradlew :app:clean :app:assembleDebug :app:test 2>&1 | tail -15
```

Expected:
```
BUILD SUCCESSFUL
```

All 6 contrast tests green, full test suite passing.

- [ ] **Step 2: Visual spot-check (manual)**

Install the debug APK and verify:
- Primary button (any screen) — deep terracotta fill in light mode, warm peach fill in dark mode
- Active dock tab — terracotta/peach pill replaces coral pill
- Profile screen Appearance toggle — terracotta/peach selected segment
- Timer ring arc and cycle dots — still warm coral (unchanged)
- Focus mode backdrop — unchanged, cat photo with coral glass accents

---

## Contrast Reference (post-implementation)

| Token Pair | Before | After |
|---|---|---|
| Light button text on button bg | 2.42:1 ✗ | **4.91:1 ✓** |
| Dark button text on button bg | 3.79:1 ✗ | **5.10:1 ✓** |
| Light active dock tab (9sp) | 2.46:1 ✗ | **4.91:1 ✓** |
| Dark active dock tab (9sp) | 2.29:1 ✗ | **5.10:1 ✓** |
| Light textMuted on page bg | 2.75:1 ✗ | **4.57:1 ✓** |
| Dark textMuted on page bg | 4.37:1 ✗ | **4.56:1 ✓** |
| Light section subtitle | ~2.8:1 ✗ | **4.57:1 ✓** |
| Dark section subtitle | ~6.4:1 ✓ | 9.52:1 ✓ |
