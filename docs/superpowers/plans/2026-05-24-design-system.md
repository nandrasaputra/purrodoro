# Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a full design system in `ui/designsystem/` — token scales, component library, screen patterns, and a debug catalog — so every screen shares a single source of truth for colors, spacing, typography, and layout.

**Architecture:** New `ui/designsystem/` package holds spacing/type scales and all new components. Existing `KuroComponents.kt` is refactored to use them. Screen files have their private color palettes deleted and replaced with `KuroTokens.current`. A `ComponentCatalogScreen` in `src/debug/` lets you preview every token and component in isolation.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, existing `KuroSurfaceTokens` / `KuroTokens` infrastructure.

---

## File Map

| File | Status | Responsibility |
|---|---|---|
| `ui/designsystem/KuroSpacing.kt` | Create | Named spacing + corner-radius constants |
| `ui/designsystem/KuroType.kt` | Create | Named typography role constants |
| `ui/designsystem/KuroAtoms.kt` | Create | `KuroChip`, `KuroPill`, `KuroStepper`, `KuroDivider`, `KuroBadge` |
| `ui/designsystem/KuroMolecules.kt` | Create | `KuroListRow`, `KuroSectionHeader`, `KuroFilterBar`, `KuroBottomSheet`, `KuroInputField`, `KuroSegmentedControl` |
| `ui/designsystem/KuroOrganisms.kt` | Create | `KuroScreenScaffold` |
| `ui/components/KuroComponents.kt` | Modify | Import spacing/type tokens; remove hardcoded literals |
| `ui/HomeTimerScreen.kt` | Modify | Delete `Home*` palette; use `KuroTokens.current` + `KuroSpacing` + `KuroType` |
| `ui/TodoScreen.kt` | Modify | Delete `Reference*` palette; use tokens + new molecules |
| `ui/HabitsScreen.kt` | Modify | Delete `Reference*` palette; use tokens + new molecules |
| `src/debug/.../ComponentCatalogScreen.kt` | Create | Full catalog: Tokens, Components, Patterns, Audit sections |
| `src/debug/.../DebugActivity.kt` | Modify | Add "Design System" entry that opens catalog |
| `docs/DESIGN_SYSTEM.md` | Create | Enforcement rules reference doc |

Base package for all `ui/designsystem/` files:
`com.endiar.kuropomodoro.ui.designsystem`

Base package for debug files:
`com.endiar.kuropomodoro.debug`

---

## Task 1: KuroSpacing

**Files:**
- Create: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/designsystem/KuroSpacing.kt`
- Test: `kuro-pomodoro-android/app/src/test/java/com/endiar/kuropomodoro/ui/designsystem/KuroSpacingTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.Assert.assertEquals

class KuroSpacingTest {
    @Test fun spacingValues() {
        assertEquals(4.dp,  KuroSpacing.xs)
        assertEquals(8.dp,  KuroSpacing.sm)
        assertEquals(16.dp, KuroSpacing.md)
        assertEquals(20.dp, KuroSpacing.lg)
        assertEquals(24.dp, KuroSpacing.xl)
        assertEquals(32.dp, KuroSpacing.xxl)
        assertEquals(40.dp, KuroSpacing.xxxl)
    }
    @Test fun radiusValues() {
        assertEquals(12.dp, KuroSpacing.radiusSm)
        assertEquals(20.dp, KuroSpacing.radiusMd)
        assertEquals(28.dp, KuroSpacing.radiusLg)
        assertEquals(34.dp, KuroSpacing.radiusPill)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
./gradlew :app:testDebugUnitTest --tests "*.KuroSpacingTest" 2>&1 | tail -10
```
Expected: compilation error — `KuroSpacing` not found.

- [ ] **Step 3: Create `KuroSpacing.kt`**

```kotlin
package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.unit.dp

object KuroSpacing {
    val xs    = 4.dp
    val sm    = 8.dp
    val md    = 16.dp
    val lg    = 20.dp
    val xl    = 24.dp
    val xxl   = 32.dp
    val xxxl  = 40.dp

    val radiusSm   = 12.dp
    val radiusMd   = 20.dp
    val radiusLg   = 28.dp
    val radiusPill = 34.dp
}
```

- [ ] **Step 4: Run test to confirm it passes**

```
./gradlew :app:testDebugUnitTest --tests "*.KuroSpacingTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

---

## Task 2: KuroType

**Files:**
- Create: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/designsystem/KuroType.kt`
- Test: `kuro-pomodoro-android/app/src/test/java/com/endiar/kuropomodoro/ui/designsystem/KuroTypeTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import org.junit.Test
import org.junit.Assert.assertEquals

class KuroTypeTest {
    @Test fun displayTimerSpec() {
        assertEquals(48.sp, KuroType.displayTimer.fontSize)
        assertEquals(FontWeight.Bold, KuroType.displayTimer.fontWeight)
    }
    @Test fun headingScreenSpec() {
        assertEquals(28.sp, KuroType.headingScreen.fontSize)
        assertEquals(FontWeight.Bold, KuroType.headingScreen.fontWeight)
    }
    @Test fun labelMetaSpec() {
        assertEquals(11.sp, KuroType.labelMeta.fontSize)
        assertEquals(FontWeight.SemiBold, KuroType.labelMeta.fontWeight)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
./gradlew :app:testDebugUnitTest --tests "*.KuroTypeTest" 2>&1 | tail -10
```
Expected: compilation error — `KuroType` not found.

- [ ] **Step 3: Create `KuroType.kt`**

```kotlin
package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KuroType {
    /** Timer countdown digit */
    val displayTimer = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold)

    /** Screen title, e.g. "Tasks" */
    val headingScreen = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)

    /** Card section title */
    val headingCard = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

    /** Main list item text */
    val bodyPrimary = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)

    /** Supporting / subtitle text */
    val bodySecondary = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)

    /** Chip and pill labels */
    val labelStrong = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

    /** Dock tab labels, badges */
    val labelMeta = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}
```

- [ ] **Step 4: Run test to confirm it passes**

```
./gradlew :app:testDebugUnitTest --tests "*.KuroTypeTest" 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

---

## Task 3: KuroAtoms

**Files:**
- Create: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/designsystem/KuroAtoms.kt`

No unit tests for pure Composables (no Espresso in this project). Visual verification via the catalog in Task 7.

- [ ] **Step 1: Create `KuroAtoms.kt`**

```kotlin
package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.theme.kuroTokens

// ── KuroChip ─────────────────────────────────────────────────────────────────
// Flat-surface filter chip. Used in Todo and Habits filter bars.

@Composable
fun KuroChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = if (selected) tokens.interactiveFill else Color.Transparent
    val textColor = if (selected) tokens.interactiveText else tokens.textSecondary
    val borderColor = if (selected) Color.Transparent else tokens.cardBorder
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(KuroSpacing.radiusMd))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
    ) {
        Text(text = text, style = KuroType.labelStrong, color = textColor)
    }
}

// ── KuroPill ─────────────────────────────────────────────────────────────────
// Semantic status pill. Replaces StatusPill.

enum class KuroPillKind { Playing, Locked, Default }

@Composable
fun KuroPill(
    label: String,
    kind: KuroPillKind = KuroPillKind.Default,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = when (kind) {
        KuroPillKind.Playing -> tokens.accent
        KuroPillKind.Locked  -> tokens.lockedFill
        KuroPillKind.Default -> tokens.cardBorder
    }
    val textColor = when (kind) {
        KuroPillKind.Playing -> tokens.accentOn
        KuroPillKind.Locked  -> tokens.lockedText
        KuroPillKind.Default -> tokens.textSecondary
    }
    val borderColor = when (kind) {
        KuroPillKind.Locked -> tokens.lockedBorder
        else                -> Color.Transparent
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(KuroSpacing.radiusSm))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, style = KuroType.labelMeta, color = textColor)
    }
}

// ── KuroStepper ──────────────────────────────────────────────────────────────
// +/- button used in habit quantity steppers.

@Composable
fun KuroStepper(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(tokens.segmentedTrack)
            .border(1.dp, tokens.borderSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = KuroType.headingCard, color = tokens.textPrimary)
    }
}

// ── KuroDivider ───────────────────────────────────────────────────────────────
// Hairline divider between list rows.

@Composable
fun KuroDivider(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .padding(horizontal = KuroSpacing.lg)
            .height(1.dp)
            .background(tokens.divider),
    )
}

// ── KuroBadge ────────────────────────────────────────────────────────────────
// Generalized label badge (replaces MockBadge, also usable for "Premium", etc.)

@Composable
fun KuroBadge(
    label: String,
    fill: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.sm))
            .background(fill)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text = label, style = KuroType.labelMeta, color = textColor)
    }
}
```

- [ ] **Step 2: Verify it compiles**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

---

## Task 4: KuroMolecules

**Files:**
- Create: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/designsystem/KuroMolecules.kt`

- [ ] **Step 1: Create `KuroMolecules.kt`**

```kotlin
package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.theme.kuroTokens

// ── KuroListRow ───────────────────────────────────────────────────────────────
// Standard content row: leading slot + title + optional subtitle + trailing slot.

@Composable
fun KuroListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val rowMod = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Row(
        modifier = rowMod
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = KuroType.bodyPrimary, color = tokens.textPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = KuroType.bodySecondary, color = tokens.textSecondary)
            }
        }
        if (trailing != null) trailing()
    }
}

// ── KuroSectionHeader ────────────────────────────────────────────────────────
// Section label with optional trailing action.

@Composable
fun KuroSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = KuroType.labelStrong, color = tokens.textMuted)
        if (action != null) action()
    }
}

// ── KuroFilterBar ────────────────────────────────────────────────────────────
// Horizontal scrolling row of KuroChips.

@Composable
fun KuroFilterBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        options.forEachIndexed { i, label ->
            KuroChip(
                text = label,
                selected = i == selectedIndex,
                onClick = { onSelect(i) },
            )
        }
    }
}

// ── KuroBottomSheet ───────────────────────────────────────────────────────────
// Standard bottom sheet with drag handle, title, and padded content.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuroBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.pageBackground,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md)) {
            Text(
                text = title,
                style = KuroType.headingCard,
                color = tokens.textPrimary,
                modifier = Modifier.padding(bottom = KuroSpacing.md),
            )
            content()
        }
    }
}

// ── KuroInputField ────────────────────────────────────────────────────────────
// Styled text field — warm border, no underline, rounded corners.

@Composable
fun KuroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = KuroType.bodyPrimary, color = tokens.textFaint) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(KuroSpacing.radiusMd),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = tokens.cardFill,
            unfocusedContainerColor = tokens.cardFill,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = tokens.textPrimary,
            unfocusedTextColor = tokens.textPrimary,
            cursorColor = tokens.accent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd)),
    )
}

// ── KuroSegmentedControl ─────────────────────────────────────────────────────
// Generalised N-option segmented control. Replaces AppearanceModeToggle.

@Composable
fun KuroSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.segmentedTrack)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) tokens.interactiveFill else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = KuroSpacing.sm, horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (selected) KuroType.labelStrong else KuroType.bodySecondary,
                    color = if (selected) tokens.interactiveText else tokens.textSecondary,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

---

## Task 5: KuroOrganisms

**Files:**
- Create: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/designsystem/KuroOrganisms.kt`

- [ ] **Step 1: Create `KuroOrganisms.kt`**

```kotlin
package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.components.KuroFlatSurface
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar

/**
 * Root scaffold for all flat-surface tab screens (Todo, Habits, etc.).
 *
 * Provides:
 * - [KuroFlatSurface] page background
 * - [ScreenStickyBar] that appears once [headerContent] scrolls out of view
 * - Shared [ScrollState] passed to [content] for the screen's scrollable body
 *
 * @param stickyTitle   Title shown in the sticky bar once header scrolls off.
 * @param headerContent Composable for the full visible header (measured to
 *                      determine when to show the sticky bar).
 * @param content       The scrollable body. Receives the shared [ScrollState]
 *                      and a [BoxScope] to anchor overlays (e.g. FAB).
 */
@Composable
fun KuroScreenScaffold(
    stickyTitle: String,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
    content: @Composable BoxScope.(scrollState: ScrollState) -> Unit,
) {
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 8.dp.roundToPx() }
    val showStickyBar by remember {
        derivedStateOf { scrollState.value > headerHeightPx + topPaddingPx }
    }

    KuroFlatSurface(modifier = modifier) {
        // Sticky bar (animated in/out)
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            ScreenStickyBar(title = stickyTitle, modifier = Modifier.fillMaxWidth())
        }

        // Header measurement overlay (invisible, zero-alpha)
        Box(modifier = Modifier.fillMaxWidth().onSizeChanged { headerHeightPx = it.height }) {
            headerContent()
        }

        // Screen body
        content(scrollState)
    }
}
```

- [ ] **Step 2: Verify it compiles**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

---

## Task 6: Refactor KuroComponents to use design system tokens

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`

Replace all hardcoded `dp`, `sp`, `FontWeight`, and `Color` literals with `KuroSpacing`, `KuroType`, and token references. The behavior and visual output must not change.

- [ ] **Step 1: Add imports to `KuroComponents.kt`**

At the top of the import block, add:
```kotlin
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
```

- [ ] **Step 2: Update `KuroCard`**

Replace:
```kotlin
shape = RoundedCornerShape(28.dp),
```
With:
```kotlin
shape = RoundedCornerShape(KuroSpacing.radiusLg),
```

Replace the border+padding line:
```kotlin
.border(1.dp, tokens.cardBorder, RoundedCornerShape(28.dp))
.padding(20.dp),
```
With:
```kotlin
.border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
.padding(KuroSpacing.lg),
```

- [ ] **Step 3: Update `KuroPrimaryButton`**

Replace:
```kotlin
modifier = modifier.height(56.dp),
shape = RoundedCornerShape(20.dp),
```
With:
```kotlin
modifier = modifier.height(56.dp),
shape = RoundedCornerShape(KuroSpacing.radiusMd),
```

- [ ] **Step 4: Update `DockTabItem` — remove hardcoded colors**

Replace:
```kotlin
val iconColor = if (active) Color(0xFFD36B4F) else Color(0xFF717171)
val bgColor = if (active) Color(0xFFFFF3EC) else Color.Transparent
```
With:
```kotlin
val tokens = MaterialTheme.colorScheme.kuroTokens
val iconColor = if (active) tokens.accent else tokens.textMuted
val bgColor = if (active) tokens.accentSoft else Color.Transparent
```

Also add `import androidx.compose.material3.MaterialTheme` if not already present.

- [ ] **Step 5: Update `ScreenStickyBar` — replace raw sp/fontWeight**

Replace:
```kotlin
fontSize = 24.sp,
fontWeight = FontWeight.Bold
```
With:
```kotlin
style = KuroType.headingScreen,
```
(Remove the `fontSize` and `fontWeight` parameters entirely from that `Text` call.)

- [ ] **Step 6: Update `CycleDotsRow` — remove hardcoded colors**

Replace:
```kotlin
val peach = Color(0xFFFF8B6F)
val peachMuted = Color(0xFFFF8B6F).copy(alpha = 0.25f)
```
With:
```kotlin
val tokens = MaterialTheme.colorScheme.kuroTokens
val peach = tokens.accent
val peachMuted = tokens.accent.copy(alpha = 0.25f)
```

- [ ] **Step 7: Verify it compiles and existing tests still pass**

```
./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

---

## Task 7: Delete private palettes from TodoScreen and HabitsScreen

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt`
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/HabitsScreen.kt`

Both files have identical 10-line `private val Reference*` blocks (lines 79–88 in TodoScreen, lines 84–93 in HabitsScreen). Every usage must be replaced with the token equivalent below.

**Token mapping:**

| Private constant | Token replacement |
|---|---|
| `ReferenceCream` | `tokens.pageBackground` |
| `ReferenceCard` | `tokens.cardFill` |
| `ReferenceCardWarm` | `tokens.activeCardFill` |
| `ReferenceBorder` | `tokens.cardBorder` |
| `ReferenceTrack` | `tokens.segmentedTrack` |
| `ReferenceInk` | `tokens.textPrimary` |
| `ReferenceMuted` | `tokens.textSecondary` |
| `ReferenceFaint` | `tokens.textFaint` |
| `ReferenceCoral` | `tokens.accent` |
| `ReferenceGreen` | `Mint` (from `com.endiar.kuropomodoro.ui.theme.Mint`) |
| `ReferenceLeaf` (HabitsScreen) | `Mint` |

- [ ] **Step 1: In `TodoScreen.kt` — delete lines 79–88 (the `private val Reference*` block)**

Delete these 10 lines:
```kotlin
private val ReferenceCream = Color(0xFFF9F5F0)
private val ReferenceCard = Color(0xFFFFFFFF)
private val ReferenceCardWarm = Color(0xFFFFF8F2)
private val ReferenceBorder = Color(0xFFEDE0D4)
private val ReferenceTrack = Color(0xFFF5EEE8)
private val ReferenceInk = Color(0xFF201714)
private val ReferenceMuted = Color(0xFF786B64)
private val ReferenceFaint = Color(0xFFA99B92)
private val ReferenceCoral = Color(0xFFC96F51)
private val ReferenceGreen = Color(0xFF8BA478)
```

- [ ] **Step 2: In `TodoScreen.kt` — replace all usages**

In every composable in `TodoScreen.kt`, add `val tokens = MaterialTheme.colorScheme.kuroTokens` at the top of the composable scope (if not already present). Then do these replacements throughout the file:

- `ReferenceInk` → `tokens.textPrimary`
- `ReferenceCard` → `tokens.cardFill`
- `ReferenceCardWarm` → `tokens.activeCardFill`
- `ReferenceMuted` → `tokens.textSecondary`
- `ReferenceFaint` → `tokens.textFaint`
- `ReferenceCoral` → `tokens.accent`
- `ReferenceBorder` → `tokens.cardBorder`
- `ReferenceTrack` → `tokens.segmentedTrack`
- `ReferenceGreen` → `Mint`

Add to imports if not present:
```kotlin
import androidx.compose.material3.MaterialTheme
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.kuroTokens
```

- [ ] **Step 3: In `HabitsScreen.kt` — delete lines 84–93 (the `private val Reference*` block)**

Delete:
```kotlin
private val ReferenceCream = Color(0xFFF9F5F0)
private val ReferenceCard = Color(0xFFFFFFFF)
private val ReferenceCardWarm = Color(0xFFFFF8F2)
private val ReferenceBorder = Color(0xFFEDE0D4)
private val ReferenceTrack = Color(0xFFF5EEE8)
private val ReferenceInk = Color(0xFF201714)
private val ReferenceMuted = Color(0xFF786B64)
private val ReferenceFaint = Color(0xFFA99B92)
private val ReferenceCoral = Color(0xFFC96F51)
private val ReferenceLeaf = Color(0xFF8BA478)
```

- [ ] **Step 4: In `HabitsScreen.kt` — replace all usages**

Same replacements as Step 2. Use `ReferenceLeaf` → `Mint`.

- [ ] **Step 5: Verify compilation**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Run all unit tests**

```
./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

---

## Task 8: Delete private palette from HomeTimerScreen

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/HomeTimerScreen.kt`

**Token mapping:**

| Private constant | Replacement |
|---|---|
| `HomeCream` | `tokens.pageBackground` |
| `HomeCard` | `tokens.cardFill` |
| `HomeCardWarm` | `tokens.activeCardFill` |
| `HomeBorder` | `tokens.cardBorder` |
| `HomeText` | `tokens.textPrimary` |
| `HomeMuted` | `tokens.textSecondary` |
| `HomeCoral` | `tokens.accent` |
| `HomeCoralSoft` | `tokens.accentSoft` |
| `HomeLeaf` | `Mint` (from `com.endiar.kuropomodoro.ui.theme.Mint`) |

- [ ] **Step 1: Delete lines 67–75 (the `private val Home*` block)**

Delete:
```kotlin
private val HomeCream = Color(0xFFFAF5F2)
private val HomeCard = Color(0xFFFFFFFF)
private val HomeCardWarm = Color(0xFFF9EEE5)
private val HomeBorder = Color(0xFFE9DED8)
private val HomeText = Color(0xFF1E1612)
private val HomeMuted = Color(0xFF9E8A7E)
private val HomeCoral = Color(0xFFBF5B3D)
private val HomeCoralSoft = Color(0xFFF2DDD5)
private val HomeLeaf = Color(0xFF7A9A60)
```

- [ ] **Step 2: Replace all usages throughout `HomeTimerScreen.kt`**

In each composable, obtain tokens at the top: `val tokens = MaterialTheme.colorScheme.kuroTokens`

Apply the token mapping above. Also replace hardcoded `fontSize`/`fontWeight` literals per `KuroType`:
- `fontSize = 44.sp, fontWeight = FontWeight.Light` → `style = KuroType.displayTimer`
- `fontSize = 17.sp, fontWeight = FontWeight.ExtraBold` → `style = KuroType.headingCard`
- `fontSize = 13.sp, fontWeight = FontWeight.Medium` → `style = KuroType.bodySecondary`
- `fontSize = 12.sp, fontWeight = FontWeight.Bold` → `style = KuroType.labelStrong`
- `fontWeight = FontWeight.Bold` on `Text("Stop", ...)` → add `style = KuroType.labelStrong`

Add to imports:
```kotlin
import androidx.compose.material3.MaterialTheme
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.kuroTokens
```

- [ ] **Step 3: Verify compilation and tests**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```
Both expected: `BUILD SUCCESSFUL`

---

## Task 9: Component Catalog (Debug)

**Files:**
- Create: `kuro-pomodoro-android/app/src/debug/java/com/endiar/kuropomodoro/debug/ComponentCatalogScreen.kt`
- Modify: `kuro-pomodoro-android/app/src/debug/java/com/endiar/kuropomodoro/debug/DebugActivity.kt`

- [ ] **Step 1: Create `ComponentCatalogScreen.kt`**

```kotlin
package com.endiar.kuropomodoro.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.designsystem.KuroBadge
import com.endiar.kuropomodoro.ui.designsystem.KuroChip
import com.endiar.kuropomodoro.ui.designsystem.KuroDivider
import com.endiar.kuropomodoro.ui.designsystem.KuroFilterBar
import com.endiar.kuropomodoro.ui.designsystem.KuroInputField
import com.endiar.kuropomodoro.ui.designsystem.KuroListRow
import com.endiar.kuropomodoro.ui.designsystem.KuroPill
import com.endiar.kuropomodoro.ui.designsystem.KuroPillKind
import com.endiar.kuropomodoro.ui.designsystem.KuroSectionHeader
import com.endiar.kuropomodoro.ui.designsystem.KuroSegmentedControl
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroStepper
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.theme.Coral
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.Cream
import com.endiar.kuropomodoro.ui.theme.WarmCream
import com.endiar.kuropomodoro.ui.theme.Peach
import com.endiar.kuropomodoro.ui.theme.Blush
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.NightDeep
import com.endiar.kuropomodoro.ui.theme.NightCard
import com.endiar.kuropomodoro.ui.theme.DockCoral

private val catalogSections = listOf("Tokens", "Components", "Patterns", "Audit")

@Composable
fun ComponentCatalogScreen() {
    var selectedSection by remember { mutableIntStateOf(0) }
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
    ) {
        // Header
        Text(
            text = "Design System",
            style = KuroType.headingScreen,
            color = tokens.textPrimary,
            modifier = Modifier.padding(KuroSpacing.lg),
        )

        // Section filter bar
        KuroFilterBar(
            options = catalogSections,
            selectedIndex = selectedSection,
            onSelect = { selectedSection = it },
        )

        Spacer(Modifier.height(KuroSpacing.sm))

        // Section content
        when (selectedSection) {
            0 -> TokensSection()
            1 -> ComponentsSection()
            2 -> PatternsSection()
            3 -> AuditSection()
        }
    }
}

// ── Tokens ────────────────────────────────────────────────────────────────────

@Composable
private fun TokensSection() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xl),
    ) {
        // Color swatches
        Text("Color Palette", style = KuroType.headingCard, color = tokens.textPrimary)
        val swatches = listOf(
            "Coral" to Coral,
            "Cocoa" to Cocoa,
            "Cream" to Cream,
            "WarmCream" to WarmCream,
            "Peach" to Peach,
            "Blush" to Blush,
            "Mint" to Mint,
            "NightDeep" to NightDeep,
            "NightCard" to NightCard,
            "DockCoral" to DockCoral,
        )
        swatches.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                row.forEach { (name, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                                .background(color)
                                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusSm))
                        )
                        Text(name, style = KuroType.labelMeta, color = tokens.textMuted)
                    }
                }
            }
        }

        // Surface token pairs
        Text("Surface Tokens", style = KuroType.headingCard, color = tokens.textPrimary)
        val surfaceTokens = listOf(
            "pageBackground" to tokens.pageBackground,
            "cardFill" to tokens.cardFill,
            "activeCardFill" to tokens.activeCardFill,
            "cardBorder" to tokens.cardBorder,
            "divider" to tokens.divider,
            "accent" to tokens.accent,
            "accentSoft" to tokens.accentSoft,
            "interactiveFill" to tokens.interactiveFill,
            "textPrimary" to tokens.textPrimary,
            "textSecondary" to tokens.textSecondary,
            "textMuted" to tokens.textMuted,
            "textFaint" to tokens.textFaint,
        )
        surfaceTokens.forEach { (name, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                        .background(color)
                        .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusSm))
                )
                Text(name, style = KuroType.bodySecondary, color = tokens.textPrimary)
            }
        }

        // Spacing scale
        Text("Spacing Scale", style = KuroType.headingCard, color = tokens.textPrimary)
        listOf(
            "xs=4" to KuroSpacing.xs,
            "sm=8" to KuroSpacing.sm,
            "md=16" to KuroSpacing.md,
            "lg=20" to KuroSpacing.lg,
            "xl=24" to KuroSpacing.xl,
            "xxl=32" to KuroSpacing.xxl,
            "xxxl=40" to KuroSpacing.xxxl,
        ).forEach { (label, size) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                Box(modifier = Modifier.width(size).height(16.dp).clip(RoundedCornerShape(4.dp)).background(tokens.accent))
                Text(label, style = KuroType.labelMeta, color = tokens.textMuted)
            }
        }

        // Typography scale
        Text("Typography Scale", style = KuroType.headingCard, color = tokens.textPrimary)
        listOf(
            "displayTimer" to KuroType.displayTimer,
            "headingScreen" to KuroType.headingScreen,
            "headingCard" to KuroType.headingCard,
            "bodyPrimary" to KuroType.bodyPrimary,
            "bodySecondary" to KuroType.bodySecondary,
            "labelStrong" to KuroType.labelStrong,
            "labelMeta" to KuroType.labelMeta,
        ).forEach { (name, style) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            ) {
                Text(text = "Aa", style = style, color = tokens.textPrimary, modifier = Modifier.width(64.dp))
                Text(text = name, style = KuroType.labelMeta, color = tokens.textMuted)
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun ComponentsSection() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var chipSelected by remember { mutableIntStateOf(0) }
    var segSelected by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xl),
    ) {
        // Atoms
        Text("Atoms", style = KuroType.headingCard, color = tokens.textPrimary)

        Text("KuroChip", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            listOf("All", "Active", "Done").forEachIndexed { i, label ->
                KuroChip(text = label, selected = i == chipSelected, onClick = { chipSelected = i })
            }
        }

        Text("KuroPill", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            KuroPill(label = "Playing", kind = KuroPillKind.Playing)
            KuroPill(label = "Locked", kind = KuroPillKind.Locked)
            KuroPill(label = "Default", kind = KuroPillKind.Default)
        }

        Text("KuroStepper", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            KuroStepper(label = "−", onClick = {})
            KuroStepper(label = "+", onClick = {})
        }

        Text("KuroDivider", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroDivider(modifier = Modifier.fillMaxWidth())

        Text("KuroBadge", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            KuroBadge(label = "Mock", fill = tokens.lockedFill, textColor = tokens.lockedText)
            KuroBadge(label = "Premium", fill = tokens.accentSoft, textColor = tokens.accent)
        }

        // Molecules
        Text("Molecules", style = KuroType.headingCard, color = tokens.textPrimary)

        Text("KuroListRow", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroCard {
            KuroListRow(title = "Focus duration", subtitle = "25 minutes", trailing = {
                Text("25 min", style = KuroType.bodySecondary, color = tokens.textMuted)
            })
            KuroDivider(modifier = Modifier.fillMaxWidth())
            KuroListRow(title = "Short break", trailing = {
                Text("5 min", style = KuroType.bodySecondary, color = tokens.textMuted)
            })
        }

        Text("KuroSectionHeader", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroSectionHeader(title = "TODAY", action = {
            Text("Add", style = KuroType.labelStrong, color = tokens.accent)
        })

        Text("KuroFilterBar", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroFilterBar(
            options = listOf("All", "Work", "Personal", "Health"),
            selectedIndex = chipSelected,
            onSelect = { chipSelected = it },
        )

        Text("KuroInputField", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroInputField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = "Type something...",
        )

        Text("KuroSegmentedControl", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroSegmentedControl(
            options = listOf("☀ Light", "☾ Dark", "◐ Auto"),
            selectedIndex = segSelected,
            onSelect = { segSelected = it },
        )

        // Organisms
        Text("Organisms", style = KuroType.headingCard, color = tokens.textPrimary)

        Text("KuroPrimaryButton", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroPrimaryButton(text = "Start Focus", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

// ── Patterns ─────────────────────────────────────────────────────────────────

@Composable
private fun PatternsSection() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var patternIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        KuroSegmentedControl(
            options = listOf("List", "Settings", "Sheet"),
            selectedIndex = patternIndex,
            onSelect = { patternIndex = it },
            modifier = Modifier.padding(KuroSpacing.lg),
        )
        when (patternIndex) {
            0 -> ListPatternPreview()
            1 -> SettingsPatternPreview()
            2 -> SheetPatternPreview()
        }
    }
}

@Composable
private fun ListPatternPreview() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = KuroSpacing.xxxl),
    ) {
        Text(
            text = "Tasks",
            style = KuroType.headingScreen,
            color = tokens.textPrimary,
            modifier = Modifier.padding(KuroSpacing.lg),
        )
        var chipSel by remember { mutableIntStateOf(0) }
        KuroFilterBar(
            options = listOf("All", "Work", "Personal"),
            selectedIndex = chipSel,
            onSelect = { chipSel = it },
        )
        KuroSectionHeader(title = "TODAY")
        KuroCard(modifier = Modifier.padding(horizontal = KuroSpacing.lg)) {
            listOf("Write design spec", "Review PR", "Update tests").forEachIndexed { i, t ->
                if (i > 0) KuroDivider(modifier = Modifier.fillMaxWidth())
                KuroListRow(title = t)
            }
        }
    }
}

@Composable
private fun SettingsPatternPreview() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Text("Settings", style = KuroType.headingScreen, color = tokens.textPrimary)
        KuroCard {
            KuroListRow(title = "Focus duration", subtitle = "25 min", trailing = {
                Text("›", style = KuroType.headingCard, color = tokens.textMuted)
            })
            KuroDivider(modifier = Modifier.fillMaxWidth())
            KuroListRow(title = "Short break", subtitle = "5 min", trailing = {
                Text("›", style = KuroType.headingCard, color = tokens.textMuted)
            })
            KuroDivider(modifier = Modifier.fillMaxWidth())
            KuroListRow(title = "Long break", subtitle = "15 min", trailing = {
                Text("›", style = KuroType.headingCard, color = tokens.textMuted)
            })
        }
        KuroCard {
            KuroListRow(
                title = "Delete all data",
                trailing = { Text("›", style = KuroType.headingCard, color = tokens.accent) },
                onClick = {},
            )
        }
    }
}

@Composable
private fun SheetPatternPreview() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Text("Add Task (sheet preview)", style = KuroType.headingCard, color = tokens.textPrimary)
        KuroInputField(value = text, onValueChange = { text = it }, placeholder = "Task name")
        KuroListRow(title = "Category", subtitle = "Work", trailing = {
            Text("›", style = KuroType.headingCard, color = tokens.textMuted)
        })
        KuroDivider(modifier = Modifier.fillMaxWidth())
        KuroListRow(title = "Pomodoros", subtitle = "4 sessions", trailing = {
            KuroStepper(label = "−", onClick = {})
            Spacer(Modifier.width(KuroSpacing.sm))
            KuroStepper(label = "+", onClick = {})
        })
        Spacer(Modifier.weight(1f))
        KuroPrimaryButton(text = "Add Task", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

// ── Audit ─────────────────────────────────────────────────────────────────────

@Composable
private fun AuditSection() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    // In the future this section can scan for drift programmatically.
    // For now it documents known rules as a static checklist.
    val rules = listOf(
        "No Color(0xFF...) literals in screen files" to true,
        "No fontSize/fontWeight literals in screen files" to true,
        "No raw N.dp spacing in screen-level layout" to true,
        "glassTokens() only used in Dashboard pattern" to true,
        "No private Reference* or Home* color blocks in screens" to true,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Text("Enforcement Rules", style = KuroType.headingCard, color = tokens.textPrimary)
        Spacer(Modifier.height(KuroSpacing.sm))
        rules.forEach { (rule, _) ->
            KuroListRow(
                title = rule,
                leading = { Text("✓", style = KuroType.labelStrong, color = tokens.accent) },
            )
            KuroDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}
```

- [ ] **Step 2: Wire catalog into `DebugActivity.kt`**

In `DebugActivity.kt`, find the `DebugScreen` composable. At the end of its scrollable content, add a new section card with a button to open the catalog. Add a `showCatalog` state at the top of `DebugScreen`:

```kotlin
var showCatalog by remember { mutableStateOf(false) }

if (showCatalog) {
    ComponentCatalogScreen()
    return
}
```

Then add a button at the bottom of the debug screen scroll content:

```kotlin
Spacer(Modifier.height(16.dp))
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Design System", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = { showCatalog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Component Catalog")
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

---

## Task 10: DESIGN_SYSTEM.md enforcement doc

**Files:**
- Create: `docs/DESIGN_SYSTEM.md`

- [ ] **Step 1: Create `docs/DESIGN_SYSTEM.md`**

```markdown
# Kuro Pomodoro Design System

## Rules

These rules apply to all screen files (`ui/**Screen.kt`) and any new UI code.

### Colors
- No `Color(0xFF...)` literals in screen files.
- All colors via `KuroTokens.current`, `MaterialTheme.colorScheme`, or named constants from `ui/theme/Color.kt`.
- No private `val Reference*`, `val Home*`, or similar per-file color palettes.

### Typography
- No `fontSize = N.sp` or `fontWeight = FontWeight.X` literals in screen files.
- Use `KuroType.<role>` (e.g. `KuroType.bodyPrimary`) or `MaterialTheme.typography.<role>`.

### Spacing
- No raw `N.dp` values in screen-level layout. Use `KuroSpacing.<name>`.
- Raw `dp` values are allowed inside component-internal layout (inside `ui/designsystem/`).

### Glass tokens
- `glassTokens()` and `GlassCard` are only used in the **Dashboard pattern** (Home/Timer tab).
- All other screens use flat surface tokens via `KuroTokens.current`.

### New components
- New reusable Composables go in `ui/designsystem/` before being used in screens.
- Atoms → `KuroAtoms.kt`, Molecules → `KuroMolecules.kt`, Organisms → `KuroOrganisms.kt`.

## Package Map

| Package | Contents |
|---|---|
| `ui/designsystem/` | `KuroSpacing`, `KuroType`, `KuroAtoms`, `KuroMolecules`, `KuroOrganisms` |
| `ui/components/` | `KuroComponents.kt` (existing), `FocusModeMotion.kt` |
| `ui/theme/` | `Color.kt`, `SurfaceTokens.kt`, `Theme.kt` — do not add screen-specific values here |
| `src/debug/` | `ComponentCatalogScreen.kt` — debug only, never shipped |

## Screen Patterns

See `docs/superpowers/specs/2026-05-24-design-system-design.md` for the four named patterns:
**List Screen**, **Settings Screen**, **Detail Bottom Sheet**, **Dashboard Screen**.
```

- [ ] **Step 2: Verify the build is still green**

```
./gradlew :app:testDebugUnitTest 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`
