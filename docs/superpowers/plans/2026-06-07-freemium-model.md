# Freemium Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the freemium gating foundation (a centralized `PremiumFeature` registry + `PremiumGate` helper), a reusable paywall sheet, and wire the first two concrete gates — premium sounds and the soft caps on categories (3) and habits (5).

**Architecture:** A pure Kotlin `PremiumGate` object in `model/` is the single source of truth for "what is locked" and the free-tier caps. It consumes the existing `isPremium` boolean (`PremiumOverride.overrideValue() ?: appState.isPremium`) — it never reads DataStore. UI gates call `PremiumGate` and present locks using existing design-system tokens (`lockedFill`/`lockedText`, `Locked` pill) plus one reusable paywall sheet generalized from the current `PremiumMessage`. Validated entirely through the existing debug `PremiumOverride` toggle; real billing is a separate later spec.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4 (`org.junit`), Gradle unit tests (`./gradlew testDebugUnitTest`), Android string resources (EN + `values-in` ID).

**Scope note:** This plan delivers the registry + paywall + sounds gate + category/habit soft caps as a complete, shippable increment. The spec also names Themes, Widgets, and Advanced Stats as premium. Their enum entries are created here, but their UI gating is deferred to follow-up plans: Themes has no extra theme content to gate yet, and Widgets/Advanced-Stats gating are larger UI efforts. See "Follow-ups" at the end.

**Source spec:** `docs/superpowers/specs/2026-06-07-freemium-model-design.md`

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `app/src/main/java/com/endiar/purrodoro/model/PremiumFeature.kt` | The `PremiumFeature` enum + `PremiumCopy` holder | Create |
| `app/src/main/java/com/endiar/purrodoro/model/PremiumGate.kt` | Pure lock logic + cap helpers + copy lookup | Create |
| `app/src/test/java/com/endiar/purrodoro/model/PremiumGateTest.kt` | Unit tests for `PremiumGate` | Create |
| `app/src/main/java/com/endiar/purrodoro/ui/designsystem/PremiumPaywallSheet.kt` | Reusable paywall content composable | Create |
| `app/src/main/res/values/strings.xml` | EN paywall copy per feature + cap nudges | Modify |
| `app/src/main/res/values-in/strings.xml` | ID paywall copy per feature + cap nudges | Modify |
| `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt` | Replace inline sound lock + add cap gating at add-category / add-habit | Modify |

---

## Task 1: `PremiumFeature` enum + copy holder

**Files:**
- Create: `app/src/main/java/com/endiar/purrodoro/model/PremiumFeature.kt`

- [ ] **Step 1: Create the enum and copy holder**

```kotlin
package com.endiar.purrodoro.model

import androidx.annotation.StringRes

/**
 * The complete set of premium-gated surfaces. Single source of truth for
 * "what is gated" — every premium check routes through [PremiumGate] using
 * one of these entries.
 *
 * Copy lives in string resources (EN + values-in ID), referenced via
 * [PremiumCopy] so the paywall stays localized and free of inline strings.
 */
enum class PremiumFeature {
    PremiumSounds,
    Themes,
    Widgets,
    AdvancedStats,
    UnlimitedCategories,
    UnlimitedHabits,
}

/**
 * String-resource ids for a feature's paywall presentation. Resolved to text
 * at the call site with `stringResource(...)`, keeping [PremiumGate] free of
 * Android Context.
 */
data class PremiumCopy(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (the `androidx.annotation.StringRes` import resolves via the existing AndroidX core dependency).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/model/PremiumFeature.kt
git commit -m "feat: add PremiumFeature enum and PremiumCopy holder"
```

---

## Task 2: Paywall copy string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-in/strings.xml`

- [ ] **Step 1: Add EN strings to `values/strings.xml`**

Add these entries inside the `<resources>` element (place near other feature copy):

```xml
<!-- Premium paywall -->
<string name="premium_paywall_dismiss">Maybe later</string>
<string name="premium_paywall_unlock">Unlock Premium</string>

<string name="premium_sounds_title">Premium sounds</string>
<string name="premium_sounds_body">Unlock the full ambient sound library — Forest, Fire, Keys and future packs — with Purrodoro Premium.</string>

<string name="premium_themes_title">Premium themes</string>
<string name="premium_themes_body">Unlock extra color themes and cozy mascot scenes with Purrodoro Premium.</string>

<string name="premium_widgets_title">Premium widgets</string>
<string name="premium_widgets_body">Unlock every home-screen widget, including Cozy Scene, Streak Hero and Stats Grid, with Purrodoro Premium.</string>

<string name="premium_stats_title">Full insights</string>
<string name="premium_stats_body">See your complete history heatmap, long-range trends and per-habit breakdowns with Purrodoro Premium.</string>

<string name="premium_categories_title">Unlimited categories</string>
<string name="premium_categories_body">You\'ve reached the free limit of 3 categories. Add as many as you like with Purrodoro Premium.</string>

<string name="premium_habits_title">Unlimited habits</string>
<string name="premium_habits_body">You\'ve reached the free limit of 5 habits. Track as many as you like with Purrodoro Premium.</string>
```

- [ ] **Step 2: Add matching ID strings to `values-in/strings.xml`**

Add inside the `<resources>` element:

```xml
<!-- Premium paywall -->
<string name="premium_paywall_dismiss">Nanti saja</string>
<string name="premium_paywall_unlock">Buka Premium</string>

<string name="premium_sounds_title">Suara premium</string>
<string name="premium_sounds_body">Buka seluruh pustaka suara ambient — Hutan, Api, Ketikan, dan paket berikutnya — dengan Purrodoro Premium.</string>

<string name="premium_themes_title">Tema premium</string>
<string name="premium_themes_body">Buka tema warna ekstra dan suasana maskot yang hangat dengan Purrodoro Premium.</string>

<string name="premium_widgets_title">Widget premium</string>
<string name="premium_widgets_body">Buka semua widget layar utama, termasuk Cozy Scene, Streak Hero, dan Stats Grid, dengan Purrodoro Premium.</string>

<string name="premium_stats_title">Wawasan lengkap</string>
<string name="premium_stats_body">Lihat seluruh peta riwayat, tren jangka panjang, dan rincian per kebiasaan dengan Purrodoro Premium.</string>

<string name="premium_categories_title">Kategori tanpa batas</string>
<string name="premium_categories_body">Kamu mencapai batas gratis 3 kategori. Tambah sebanyak yang kamu mau dengan Purrodoro Premium.</string>

<string name="premium_habits_title">Kebiasaan tanpa batas</string>
<string name="premium_habits_body">Kamu mencapai batas gratis 5 kebiasaan. Lacak sebanyak yang kamu mau dengan Purrodoro Premium.</string>
```

- [ ] **Step 3: Verify resources compile**

Run: `./gradlew :app:processDebugResources`
Expected: BUILD SUCCESSFUL (no duplicate-resource or missing-translation errors).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml
git commit -m "feat: add premium paywall copy (EN + ID)"
```

---

## Task 3: `PremiumGate` — lock logic + cap helpers (TDD)

**Files:**
- Create: `app/src/test/java/com/endiar/purrodoro/model/PremiumGateTest.kt`
- Create: `app/src/main/java/com/endiar/purrodoro/model/PremiumGate.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/endiar/purrodoro/model/PremiumGateTest.kt`:

```kotlin
package com.endiar.purrodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumGateTest {

    @Test
    fun everyFeatureIsLockedForFreeUsers() {
        for (feature in PremiumFeature.entries) {
            assertTrue(
                "Expected $feature locked when not premium",
                PremiumGate.isLocked(feature, isPremium = false)
            )
        }
    }

    @Test
    fun noFeatureIsLockedForPremiumUsers() {
        for (feature in PremiumFeature.entries) {
            assertFalse(
                "Expected $feature unlocked when premium",
                PremiumGate.isLocked(feature, isPremium = true)
            )
        }
    }

    @Test
    fun categoryLimitIsFreeConstantWhenNotPremium() {
        assertEquals(PremiumGate.FREE_CATEGORY_LIMIT, PremiumGate.categoryLimit(isPremium = false))
        assertEquals(3, PremiumGate.FREE_CATEGORY_LIMIT)
    }

    @Test
    fun categoryLimitIsUnlimitedWhenPremium() {
        assertEquals(Int.MAX_VALUE, PremiumGate.categoryLimit(isPremium = true))
    }

    @Test
    fun habitLimitIsFreeConstantWhenNotPremium() {
        assertEquals(PremiumGate.FREE_HABIT_LIMIT, PremiumGate.habitLimit(isPremium = false))
        assertEquals(5, PremiumGate.FREE_HABIT_LIMIT)
    }

    @Test
    fun habitLimitIsUnlimitedWhenPremium() {
        assertEquals(Int.MAX_VALUE, PremiumGate.habitLimit(isPremium = true))
    }

    @Test
    fun canAddCategoryRespectsFreeCap() {
        // Free user with 2 existing categories can add a 3rd; with 3 cannot add a 4th.
        assertTrue(PremiumGate.canAddCategory(existingCount = 2, isPremium = false))
        assertFalse(PremiumGate.canAddCategory(existingCount = 3, isPremium = false))
    }

    @Test
    fun canAddCategoryAlwaysTrueForPremium() {
        assertTrue(PremiumGate.canAddCategory(existingCount = 99, isPremium = true))
    }

    @Test
    fun canAddHabitRespectsFreeCap() {
        // Free user with 4 existing habits can add a 5th; with 5 cannot add a 6th.
        assertTrue(PremiumGate.canAddHabit(existingCount = 4, isPremium = false))
        assertFalse(PremiumGate.canAddHabit(existingCount = 5, isPremium = false))
    }

    @Test
    fun canAddHabitAlwaysTrueForPremium() {
        assertTrue(PremiumGate.canAddHabit(existingCount = 99, isPremium = true))
    }

    @Test
    fun copyForReturnsDistinctResourcesPerFeature() {
        val sounds = PremiumGate.copyFor(PremiumFeature.PremiumSounds)
        val habits = PremiumGate.copyFor(PremiumFeature.UnlimitedHabits)
        assertEquals(R.string.premium_sounds_title, sounds.titleRes)
        assertEquals(R.string.premium_habits_title, habits.titleRes)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.model.PremiumGateTest"`
Expected: FAIL — compilation error, `PremiumGate` is unresolved.

- [ ] **Step 3: Write the minimal implementation**

Create `app/src/main/java/com/endiar/purrodoro/model/PremiumGate.kt`:

```kotlin
package com.endiar.purrodoro.model

import com.endiar.purrodoro.R

/**
 * Centralized freemium gating. Single source of truth for "what is locked" and
 * the free-tier caps.
 *
 * Pure logic: consumes the already-resolved [isPremium] boolean
 * (`PremiumOverride.overrideValue() ?: appState.isPremium`) and never reads
 * DataStore or Android Context. Copy is returned as string-resource ids via
 * [copyFor] so the paywall stays localized.
 */
object PremiumGate {

    /** Free-tier cap on task categories. Premium is unlimited. */
    const val FREE_CATEGORY_LIMIT: Int = 3

    /** Free-tier cap on habits. Premium is unlimited. */
    const val FREE_HABIT_LIMIT: Int = 5

    /**
     * Whether [feature] is locked for the given entitlement. Today every
     * feature is simply premium-gated; the indirection lets a feature become
     * always-free later without touching call sites.
     */
    fun isLocked(feature: PremiumFeature, isPremium: Boolean): Boolean = !isPremium

    /** Max categories allowed: the free constant, or unlimited when premium. */
    fun categoryLimit(isPremium: Boolean): Int =
        if (isPremium) Int.MAX_VALUE else FREE_CATEGORY_LIMIT

    /** Max habits allowed: the free constant, or unlimited when premium. */
    fun habitLimit(isPremium: Boolean): Int =
        if (isPremium) Int.MAX_VALUE else FREE_HABIT_LIMIT

    /** True if a new category can be added given how many already exist. */
    fun canAddCategory(existingCount: Int, isPremium: Boolean): Boolean =
        existingCount < categoryLimit(isPremium)

    /** True if a new habit can be added given how many already exist. */
    fun canAddHabit(existingCount: Int, isPremium: Boolean): Boolean =
        existingCount < habitLimit(isPremium)

    /** Paywall copy (title + body string-resource ids) for [feature]. */
    fun copyFor(feature: PremiumFeature): PremiumCopy = when (feature) {
        PremiumFeature.PremiumSounds -> PremiumCopy(
            R.string.premium_sounds_title, R.string.premium_sounds_body
        )
        PremiumFeature.Themes -> PremiumCopy(
            R.string.premium_themes_title, R.string.premium_themes_body
        )
        PremiumFeature.Widgets -> PremiumCopy(
            R.string.premium_widgets_title, R.string.premium_widgets_body
        )
        PremiumFeature.AdvancedStats -> PremiumCopy(
            R.string.premium_stats_title, R.string.premium_stats_body
        )
        PremiumFeature.UnlimitedCategories -> PremiumCopy(
            R.string.premium_categories_title, R.string.premium_categories_body
        )
        PremiumFeature.UnlimitedHabits -> PremiumCopy(
            R.string.premium_habits_title, R.string.premium_habits_body
        )
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.model.PremiumGateTest"`
Expected: PASS (all 11 tests green).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/model/PremiumGate.kt app/src/test/java/com/endiar/purrodoro/model/PremiumGateTest.kt
git commit -m "feat: add PremiumGate with lock logic and soft caps (TDD)"
```

---

## Task 4: Reusable paywall sheet content

**Files:**
- Create: `app/src/main/java/com/endiar/purrodoro/ui/designsystem/PremiumPaywallSheet.kt`

This generalizes the current inline `PremiumMessage` (which renders a single string) into a feature-aware paywall body: title + body + dismiss/unlock actions. It is presentation-only; the unlock action is supplied by the caller (stubbed for now — billing is a later spec).

- [ ] **Step 1: Create the composable**

```kotlin
package com.endiar.purrodoro.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.PremiumFeature
import com.endiar.purrodoro.model.PremiumGate

/**
 * Reusable paywall body for a single [feature]. Renders the feature's localized
 * title + body and two actions: dismiss and unlock. The unlock action is
 * supplied by the caller — real purchase wiring is a later billing spec, so for
 * now callers may stub it (e.g. dismiss, or flip the debug premium override).
 *
 * Presentation-only and free of business logic: copy is resolved via
 * [PremiumGate.copyFor].
 */
@Composable
fun PremiumPaywallSheet(
    feature: PremiumFeature,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = PremiumGate.copyFor(feature)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(copy.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = stringResource(copy.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.premium_paywall_dismiss),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(onClick = onUnlock) {
                    Text(
                        text = stringResource(R.string.premium_paywall_unlock),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/ui/designsystem/PremiumPaywallSheet.kt
git commit -m "feat: add reusable PremiumPaywallSheet"
```

---

## Task 5: Re-flag the Keys sound as premium

The spec's 2-free split (Rain + Cafe) requires Keys to be premium. Today only Forest and Fire are flagged.

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt` (the `sounds` list, the `SoundOption(5, ... Keys ...)` entry)

- [ ] **Step 1: Find the sounds list**

Run: `grep -n "sound_keys_name" app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`
Expected: one line constructing `SoundOption(5, stringResource(R.string.sound_keys_name), stringResource(R.string.sound_keys_desc), false, "Keys")`.

- [ ] **Step 2: Change the Keys `locked` flag from `false` to `true`**

Replace:

```kotlin
        SoundOption(5, stringResource(R.string.sound_keys_name), stringResource(R.string.sound_keys_desc), false, "Keys"),
```

with:

```kotlin
        SoundOption(5, stringResource(R.string.sound_keys_name), stringResource(R.string.sound_keys_desc), true, "Keys"),
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: flag Keys ambient sound as premium"
```

---

## Task 6: Route the sounds gate through the paywall sheet

Replace the inline message-string lock in the sounds sheet with the feature-aware paywall. The sounds sheet currently shows a `PremiumMessage(message=...)`; we switch it to track a locked `PremiumFeature` and render `PremiumPaywallSheet`.

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

Context: today there are two `PremiumMessage(...)` call sites driven by a `var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }`, and the sounds `onSoundSelected` sets that string. We replace the string state with a `PremiumFeature?` state and swap both renderers to `PremiumPaywallSheet`.

- [ ] **Step 1: Add the feature-state and import**

Add the import alongside the other `com.endiar.purrodoro.model.*` imports:

```kotlin
import com.endiar.purrodoro.model.PremiumFeature
import com.endiar.purrodoro.ui.designsystem.PremiumPaywallSheet
```

Add this state declaration immediately after the existing `premiumSheetMessage` declaration:

```kotlin
    var paywallFeature by rememberSaveable { mutableStateOf<PremiumFeature?>(null) }
```

(Keep `premiumSheetMessage` for now to avoid breaking other references; it is removed in Step 5 once unused.)

- [ ] **Step 2: Update the sounds `onSoundSelected` handler**

Replace:

```kotlin
                        onSoundSelected = { sound ->
                            if (sound.locked && !isPremium) {
                                premiumSheetMessage =
                                    "${sound.name} is part of the simulated premium sound pack."
                            } else {
                                persistSelectedSoundId(sound.id)
                                playingSoundId = sound.id
                                premiumSheetMessage = null
                            }
                        },
```

with:

```kotlin
                        onSoundSelected = { sound ->
                            if (sound.locked && PremiumGate.isLocked(PremiumFeature.PremiumSounds, isPremium)) {
                                paywallFeature = PremiumFeature.PremiumSounds
                            } else {
                                persistSelectedSoundId(sound.id)
                                playingSoundId = sound.id
                                paywallFeature = null
                            }
                        },
```

Add the import for the gate if not already present:

```kotlin
import com.endiar.purrodoro.model.PremiumGate
```

- [ ] **Step 3: Replace the two `PremiumMessage` renderers**

Replace the standalone (non-sounds-sheet) renderer block:

```kotlin
            if (!showSoundsSheet) {
                premiumSheetMessage?.let { message ->
                    PremiumMessage(
                        message = message,
                        onDismiss = { premiumSheetMessage = null },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 18.dp)
                            .offset(y = -(dockHeightDp + 8.dp))
                    )
                }
            }
```

with:

```kotlin
            if (!showSoundsSheet) {
                paywallFeature?.let { feature ->
                    PremiumPaywallSheet(
                        feature = feature,
                        onDismiss = { paywallFeature = null },
                        onUnlock = { paywallFeature = null }, // TODO(billing): launch purchase flow
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 18.dp)
                            .offset(y = -(dockHeightDp + 8.dp))
                    )
                }
            }
```

Replace the in-sounds-sheet renderer and its `onDismiss`/`contentKey` wiring:

```kotlin
                KuroAdaptiveBottomSheet(
                    onDismiss = {
                        showSoundsSheet = false
                        premiumSheetMessage = null
                    },
                    horizontalPadding = 0.dp,
                    containerColor = sheetTokens.cardFill,
                    contentKey = premiumSheetMessage,
                ) {
                    premiumSheetMessage?.let { message ->
                        PremiumMessage(
                            message = message,
                            onDismiss = { premiumSheetMessage = null },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
```

with:

```kotlin
                KuroAdaptiveBottomSheet(
                    onDismiss = {
                        showSoundsSheet = false
                        paywallFeature = null
                    },
                    horizontalPadding = 0.dp,
                    containerColor = sheetTokens.cardFill,
                    contentKey = paywallFeature,
                ) {
                    paywallFeature?.let { feature ->
                        PremiumPaywallSheet(
                            feature = feature,
                            onDismiss = { paywallFeature = null },
                            onUnlock = { paywallFeature = null }, // TODO(billing): launch purchase flow
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
```

- [ ] **Step 4: Update the sounds-sheet `onDismiss` reset**

The sounds `KuroAdaptiveBottomSheet`'s outer dismiss already reset to `null` in Step 3. Confirm no remaining `premiumSheetMessage = null` lines exist in the sounds block:

Run: `grep -n "premiumSheetMessage" app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`
Expected: only the now-unused `var premiumSheetMessage` declaration remains (removed next step).

- [ ] **Step 5: Remove the now-unused state and old composable**

Delete the line:

```kotlin
    var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }
```

Delete the private `PremiumMessage` composable definition:

```kotlin
@Composable
private fun PremiumMessage(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Dismiss", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
```

- [ ] **Step 6: Verify the project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If the compiler flags unused imports (`RoundedCornerShape`, `Surface`, `Column`, `TextButton`) now that `PremiumMessage` is gone, remove only the ones it reports as unused.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: route premium sounds through PremiumPaywallSheet"
```

---

## Task 7: Soft cap on task categories

Gate the "add category" action when a free user is at the limit. The add-category callback in `PurrodoroApp.kt` is `onAddCategory = { name, color -> scope.launch { repo.addCategory(name, color) } }`. We guard it with `PremiumGate.canAddCategory` and surface the paywall when blocked.

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

- [ ] **Step 1: Guard the `onAddCategory` callback**

Replace:

```kotlin
                        onAddCategory = { name, color ->
                            scope.launch { repo.addCategory(name, color) }
                        },
```

with:

```kotlin
                        onAddCategory = { name, color ->
                            if (PremiumGate.canAddCategory(categories.size, isPremium)) {
                                scope.launch { repo.addCategory(name, color) }
                            } else {
                                paywallFeature = PremiumFeature.UnlimitedCategories
                            }
                        },
```

(`categories` is the existing hoisted state list; `isPremium` and `paywallFeature` are already in scope.)

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual verification note**

With the `PremiumOverride` debug switch OFF, adding a 4th category surfaces the paywall instead of creating it; with the switch ON, categories add freely. (No automated UI test — the cap logic itself is covered by `PremiumGateTest`.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: soft-cap task categories for free tier"
```

---

## Task 8: Soft cap on habits

There are two `onAddHabit` call sites in `PurrodoroApp.kt` (the Habits tab and the Start Focus sheet's habit creation). Guard both against the habit cap.

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

- [ ] **Step 1: Guard the Habits-tab `onAddHabit`**

Locate the Habits-tab callback (the one passed to `HabitsScreen`, around the `habits = appState.habits` block). Replace:

```kotlin
                        onAddHabit = { draft ->
                            scope.launch {
                                repo.addHabit(
                                    name = draft.name,
                                    emoji = draft.emoji,
```

…through the end of that `repo.addHabit(...)` call and its closing `}` — by wrapping the launch in the cap check. The full replacement for that callback block:

```kotlin
                        onAddHabit = { draft ->
                            if (PremiumGate.canAddHabit(appState.habits.size, isPremium)) {
                                scope.launch {
                                    repo.addHabit(
                                        name = draft.name,
                                        emoji = draft.emoji,
                                        colorArgb = draft.colorArgb,
                                        occurrence = draft.occurrence,
                                        timeBlock = draft.timeBlock,
                                    )
                                }
                            } else {
                                paywallFeature = PremiumFeature.UnlimitedHabits
                            }
                        },
```

> Before editing, run `grep -n "onAddHabit = { draft ->" app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt` and read the full existing `repo.addHabit(...)` argument list at each site, then preserve those exact arguments inside the guarded block above. The argument names (`colorArgb`, `occurrence`, `timeBlock`) come from `AppRepository.addHabit` and `HabitDraft`.

- [ ] **Step 2: Handle the Start Focus sheet `onAddHabit`**

The Start Focus sheet's `onAddHabit` is a `suspend (HabitDraft) -> Int` that must return the new habit id, so it cannot simply show a paywall (the sheet needs an id to attach). For this site, keep the existing behavior but short-circuit when capped by returning a sentinel and surfacing the paywall. Replace the Start-Focus `onAddHabit` block:

```kotlin
                        onAddHabit = { draft ->
                            repo.addHabit(
                                name = draft.name,
                                emoji = draft.emoji,
```

(through its argument list and closing `)`), wrapping it as:

```kotlin
                        onAddHabit = { draft ->
                            if (PremiumGate.canAddHabit(appState.habits.size, isPremium)) {
                                repo.addHabit(
                                    name = draft.name,
                                    emoji = draft.emoji,
                                    colorArgb = draft.colorArgb,
                                    occurrence = draft.occurrence,
                                    timeBlock = draft.timeBlock,
                                )
                            } else {
                                paywallFeature = PremiumFeature.UnlimitedHabits
                                -1
                            }
                        },
```

> Preserve the exact `repo.addHabit(...)` argument list found at this site (read it first as in Step 1). The `-1` is the "not created" sentinel; the Start Focus sheet already guards habit selection on a valid id (`selectedHabitId = newId`), but verify: read `StartFocusSheet.kt` around line 478 (`val newId = onAddHabit!!(draft)`) and, if it does not already ignore non-positive ids, add `if (newId > 0) { selectedHabitId = newId; pickerDraftId = newId }` around that assignment.

- [ ] **Step 3: Verify Start Focus sheet handles the sentinel**

Run: `grep -n "onAddHabit!!(draft)" app/src/main/java/com/endiar/purrodoro/ui/StartFocusSheet.kt`
Read the surrounding ~10 lines. If `newId` is used unconditionally, guard it:

```kotlin
                        scope.launch {
                            val newId = onAddHabit!!(draft)
                            if (newId > 0) {
                                selectedHabitId = newId
                                pickerDraftId = newId
                                // ...preserve any remaining lines from the original block here...
                            }
                        }
```

Only wrap the id-dependent lines; do not change unrelated logic.

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt app/src/main/java/com/endiar/purrodoro/ui/StartFocusSheet.kt
git commit -m "feat: soft-cap habits for free tier"
```

---

## Task 9: Full verification

- [ ] **Step 1: Run the full unit-test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (including `PremiumGateTest`).

- [ ] **Step 2: Assemble the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke test (via debug `PremiumOverride`)**

In a debug build, toggle Premium OFF and confirm:
- Selecting Forest / Fire / Keys in the sounds sheet shows the premium-sounds paywall; Rain / Cafe select normally.
- Adding a 4th category shows the unlimited-categories paywall.
- Adding a 6th habit shows the unlimited-habits paywall.

Toggle Premium ON (restart if prompted) and confirm all of the above are unblocked.

- [ ] **Step 4: Final commit (if any cleanup was needed)**

```bash
git add -A
git commit -m "chore: freemium gating verification cleanup"
```

---

## Follow-ups (separate plans / specs)

- **Billing integration** — Play Billing (Android) + StoreKit (iOS); replace the `onUnlock` stubs with the real purchase flow; restore purchases.
- **Widgets gating** — gate 6 of the 8 home-screen widgets (keep 2 free) using `PremiumFeature.Widgets`; needs a decision on which 2 are free and how the widget picker presents the lock.
- **Advanced stats teaser** — soft-locked/blurred preview of the Journey history heatmap + long-range trends using `PremiumFeature.AdvancedStats`.
- **Themes** — author extra color themes / mascot scenes, then gate with `PremiumFeature.Themes` (no extra theme content exists yet).
- **iOS gating** — mirror this registry in the SwiftUI app.
