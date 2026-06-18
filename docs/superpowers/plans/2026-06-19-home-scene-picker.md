# Home Scene Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users pick a Home hero backdrop (environment theme). Free users get **Room** only; Premium users choose among Room + premium scenes. Day/night art auto-switches from local time + app theme. Entry point: **layers icon** on the hero image.

**Architecture:** `HomeScene` enum owns drawable + string metadata per theme. `resolveHomeHeroDrawable()` picks day/night. Selection persists in DataStore (`home_scene`). `HeroSceneImage` renders hero + change button; `HomeScenePickerSheet` lists options. `effectiveHomeScene()` gates rendering for free users while preserving their choice for upgrade.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, existing Kuro design system (`KuroAdaptiveBottomSheet`, `KuroPremiumBadge`).

---

## Context (read first)

### Product decisions (approved)

| Decision | Detail |
|---|---|
| **Free scene** | Room only (`cat_home_header_day/night`) |
| **Premium scenes** | Café, Library, Park (+ future themes) |
| **Widgets** | Not gated — all widgets free for everyone |
| **Day/night rule** | One fixed environment per theme; only lighting/time changes between day/night pairs — **not** different compositions |
| **Entry UX** | Circular **layers** button, top-right of home hero (idle + active timer) |
| **Picker UX** | Bottom sheet with thumbnail + title + subtitle rows; Premium badge on locked rows |
| **Paywall** | Free user tapping locked scene → existing `PremiumUpsellSheet` |
| **Persistence** | Device-local DataStore key `home_scene`; survives app restart; not synced |

### UX mockups

Reference: `assets/ux-mockups/`

- `ux-scene-picker-01-home-entry.png` — layers button on hero
- `ux-scene-picker-02-sheet-free-user.png` — picker with Room selected, Café locked
- `ux-scene-picker-03-user-flow.png` — end-to-end flow

### Standalone art assets (source of truth)

All hero banners: **1536×1024**, landscape, aspect **~1.48** in UI.

| Theme | Free/Premium | Day PNG | Night PNG | Shipped webp (`res/drawable/`) |
|---|---|---|---|---|
| **Room** | Free | (in drawable) | (in drawable) | `cat_home_header_day.webp`, `cat_home_header_night.webp` |
| **Café** | Premium | `assets/home-scenes/cafe/cat_home_premium_cafe_day.png` | (paired in drawable) | `cat_home_premium_cafe_day.webp`, `cat_home_premium_cafe_night.webp` |
| **Library** | Premium | `assets/home-scenes/library/cat_home_scene_library_day.png` | `assets/home-scenes/library/cat_home_scene_library_night.png` | `cat_home_scene_library_day.webp`, `cat_home_scene_library_night.webp` |
| **Park** | Premium | `assets/home-scenes/park/cat_home_scene_park_day.png` | `assets/home-scenes/park/cat_home_scene_park_night.png` | `cat_home_scene_park_day.webp`, `cat_home_scene_park_night.webp` |

Proposal iterations (historical): `assets/scene-proposals/`

### Already implemented (this branch)

| File | Status |
|---|---|
| `model/HomeScene.kt` | Room + Café only — **Library/Park not wired** |
| `model/HomeSceneTest.kt` | Basic resolver tests |
| `ui/HomeScenePickerSheet.kt` | Picker sheet |
| `ui/HomeTimerScreen.kt` | `HeroSceneImage` + layers button |
| `data/AppRepository.kt` | `homeScene` in `AppState`, `setHomeScene()` |
| `PurrodoroApp.kt` | Sheet + premium gating |
| `values/strings.xml` | Room + Café strings |

### Not in scope (later)

- Play Billing hookup (still local `isPremium` flag)
- First-run coach mark on layers button (UX mockup suggests it — optional follow-up)
- Scene sync across devices
- Renaming café drawables to `cat_home_scene_cafe_*` (cosmetic; optional)

---

## File map (remaining work)

| File | Action |
|---|---|
| `model/HomeScene.kt` | Add `Library`, `Park` enum entries + drawables/strings |
| `model/HomeSceneTest.kt` | Tests for new scenes |
| `res/values/strings.xml` | Library + Park title/subtitle |
| `res/values-in/strings.xml` | Indonesian strings |
| `ui/PremiumUpsellSheet.kt` | Replace stale widget perk with "Premium home scenes" |
| `docs/PRODUCT.md` | Monetization table: home scenes |

---

## Task 1: Wire Library and Park into HomeScene

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/HomeScene.kt`
- Test: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/model/HomeSceneTest.kt`

- [ ] **Step 1: Extend failing tests**

```kotlin
@Test
fun resolveHomeHeroDrawable_libraryDay() {
    assertEquals(
        R.drawable.cat_home_scene_library_day,
        resolveHomeHeroDrawable(HomeScene.Library, isDarkTheme = false, time = LocalTime.of(10, 0)),
    )
}

@Test
fun resolveHomeHeroDrawable_parkNight() {
    assertEquals(
        R.drawable.cat_home_scene_park_night,
        resolveHomeHeroDrawable(HomeScene.Park, isDarkTheme = true, time = LocalTime.of(10, 0)),
    )
}

@Test
fun premiumScenesArePremiumOnly() {
    assertTrue(HomeScene.Cafe.isPremiumOnly)
    assertTrue(HomeScene.Library.isPremiumOnly)
    assertTrue(HomeScene.Park.isPremiumOnly)
    assertFalse(HomeScene.Room.isPremiumOnly)
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.model.HomeSceneTest" -q`
Expected: FAIL — unresolved `Library` / `Park` or wrong drawable ids

- [ ] **Step 3: Implement enum entries**

```kotlin
enum class HomeScene(
    val storageName: String,
    val isPremiumOnly: Boolean,
) {
    Room("room", isPremiumOnly = false),
    Cafe("cafe", isPremiumOnly = true),
    Library("library", isPremiumOnly = true),
    Park("park", isPremiumOnly = true),
    ;

    @DrawableRes
    fun dayDrawable(): Int = when (this) {
        Room -> R.drawable.cat_home_header_day
        Cafe -> R.drawable.cat_home_premium_cafe_day
        Library -> R.drawable.cat_home_scene_library_day
        Park -> R.drawable.cat_home_scene_park_day
    }

    @DrawableRes
    fun nightDrawable(): Int = when (this) {
        Room -> R.drawable.cat_home_header_night
        Cafe -> R.drawable.cat_home_premium_cafe_night
        Library -> R.drawable.cat_home_scene_library_night
        Park -> R.drawable.cat_home_scene_park_night
    }

    @StringRes
    fun titleRes(): Int = when (this) {
        Room -> R.string.home_scene_room_title
        Cafe -> R.string.home_scene_cafe_title
        Library -> R.string.home_scene_library_title
        Park -> R.string.home_scene_park_title
    }

    @StringRes
    fun subtitleRes(): Int = when (this) {
        Room -> R.string.home_scene_room_subtitle
        Cafe -> R.string.home_scene_cafe_subtitle
        Library -> R.string.home_scene_library_subtitle
        Park -> R.string.home_scene_park_subtitle
    }
    // companion unchanged
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: same command as Step 2
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/HomeScene.kt \
        purrodoro-android/app/src/test/java/com/endiar/purrodoro/model/HomeSceneTest.kt \
        purrodoro-android/app/src/main/res/drawable/cat_home_scene_*.webp
git commit -m "feat(android): add Library and Park home scene assets and enum"
```

---

## Task 2: Strings (EN + ID)

**Files:**
- Modify: `purrodoro-android/app/src/main/res/values/strings.xml`
- Modify: `purrodoro-android/app/src/main/res/values-in/strings.xml`

- [ ] **Step 1: Add English strings** (after existing café strings)

```xml
<string name="home_scene_library_title">Library</string>
<string name="home_scene_library_subtitle">Quiet desk among the stacks</string>
<string name="home_scene_park_title">Park</string>
<string name="home_scene_park_subtitle">Bench under the open sky</string>
```

- [ ] **Step 2: Add Indonesian strings**

```xml
<string name="home_scene_library_title">Perpustakaan</string>
<string name="home_scene_library_subtitle">Meja tenang di antara rak buku</string>
<string name="home_scene_park_title">Taman</string>
<string name="home_scene_park_subtitle">Bangku di bawah langit terbuka</string>
```

- [ ] **Step 3: Build to verify resources**

Run: `cd purrodoro-android && ./gradlew :app:compileDebugKotlin -q`
Expected: SUCCESS

- [ ] **Step 4: Commit**

```bash
git add purrodoro-android/app/src/main/res/values/strings.xml \
        purrodoro-android/app/src/main/res/values-in/strings.xml
git commit -m "feat(android): add Library and Park home scene copy"
```

---

## Task 3: Premium paywall perk copy

**Files:**
- Modify: `purrodoro-android/app/src/main/res/values/strings.xml`
- Modify: `purrodoro-android/app/src/main/res/values-in/strings.xml`
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/PremiumUpsellSheet.kt`

- [ ] **Step 1: Replace widget perk string**

`premium_perk_widgets` → `premium_perk_scenes`

EN: `Premium home scenes`
ID: `Latar beranda premium`

- [ ] **Step 2: Update `paywallPerks()` generic branch**

Replace `R.string.premium_perk_widgets` with `R.string.premium_perk_scenes`. Keep `Lucide.LayoutGrid` or switch to `Lucide.Layers`.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin -q`

- [ ] **Step 4: Commit**

```bash
git commit -m "copy(android): premium paywall lists home scenes perk"
```

---

## Task 4: Product docs

**Files:**
- Modify: `docs/PRODUCT.md`

- [ ] **Step 1: Add row to Free vs premium table**

| Home hero scenes | Room only | Room + Café, Library, Park | Shipped (picker + assets) |

- [ ] **Step 2: Note day/night auto-switch and layers entry point**

- [ ] **Step 3: Commit**

```bash
git add docs/PRODUCT.md
git commit -m "docs: document home scene picker premium model"
```

---

## Task 5: Manual QA checklist

- [ ] Free user: hero shows Room; layers opens sheet; Café/Library/Park show Premium badge; tap locked → paywall; hero stays Room
- [ ] Premium user (Dev Tools flag): select each scene; hero updates immediately; kill app → selection restored
- [ ] Night mode + daytime hours: verify night drawable; after 6pm or dark theme: night art
- [ ] Active timer state: layers button still visible and sheet works
- [ ] Indonesian locale: picker strings render

---

## Adding a future scene (playbook)

1. Generate **day** PNG 1536×1024 → `assets/home-scenes/<name>/`
2. Generate **night** from day PNG as reference (same layout)
3. `cwebp -q 85` → `res/drawable/cat_home_scene_<name>_day.webp` + `_night.webp`
4. Add enum entry in `HomeScene.kt` with `isPremiumOnly = true`
5. Add strings EN + ID
6. No picker UI changes needed — sheet iterates `HomeScene.entries`

---

## Self-review (spec coverage)

| Requirement | Task |
|---|---|
| Free = Room only | Already in `effectiveHomeScene()` |
| Premium multi-scene | Task 1 |
| Consistent day/night pairs | Asset pipeline documented |
| Layers button entry | Already shipped |
| Bottom sheet picker | Already shipped |
| Premium badge on locked | Already shipped |
| Persist selection | Already shipped |
| Library + Park art | Assets committed; Task 1 wires code |
| Paywall copy update | Task 3 |
| PRODUCT.md | Task 4 |

No placeholders remain in task steps.
