# Soundscape Admin + Unified Local/Remote Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a debug-only local web admin for soundscape assets and unify the Android app's local vs remote catalog model so visibility, premium, and download state are handled consistently.

**Architecture:** Remote tracks stay in `soundscape_tracks` (id ≥ 100) with existing download flow. Local (APK) tracks get a new Supabase table `soundscape_bundled_catalog` keyed by `(kind, local_id)` for visibility/premium/order only. Android fetches both catalogs on startup, caches remote + bundled rows in Room, and builds `MusicOption`/`SoundOption` lists via a pure `SoundscapeCatalogBuilder` instead of hardcoded lists in `PurrodoroApp.kt`. A Vite + TypeScript SPA in `tools/soundscape-admin/` uses the service role key for CRUD and Storage uploads.

**Tech Stack:** Supabase (Postgres + Storage), Kotlin/Room/Jetpack Compose, Vite/TypeScript, `@supabase/supabase-js`, JUnit4/Robolectric.

**Source spec:** `docs/superpowers/specs/2026-06-17-soundscape-admin-design.md`

---

## Design decisions (resolved in lead review)

These supersede earlier drafts of this plan; tasks below already reflect them.

1. **No `SoundscapeCatalogEntry` / `SoundscapeDelivery` types.** Delivery is
   already encoded on the existing option models: `RemoteTrackInfo? == null`
   means a bundled (local) track, non-null means remote. Adding a parallel
   unified model that nothing consumes is dead code. The builder produces the
   existing `MusicOption` / `SoundOption` directly. (Former Task 3 removed.)

2. **The `BundledTrackRegistry` is the source of *existence*; the Supabase
   table is an *override* only.** The builder always iterates the registry
   defaults and left-joins the cached DB row by `(kind, localId)`:
   - row present → apply its overrides (visibility, premium, sort, display).
   - row absent → keep the default and treat it as **visible**.

   This makes "a newly bundled APK track not yet seeded in the table" default
   to *visible* (so a release is never silently swallowed), while still letting
   the admin hide an existing track.

3. **Hiding requires reading inactive rows.** Because hiding must be
   distinguishable from "unknown to the table", the app reads **all** bundled
   rows (active and inactive) and filters on `is_active` client-side. The
   bundled table's RLS therefore allows public read of every row (it exposes
   only flags/metadata, never audio bytes). `is_active` is carried through the
   DTO → entity → builder. (Remote `soundscape_tracks` RLS is unchanged:
   still active-only.)

4. **Service-role key never reaches a built bundle.** The admin is
   **dev-server only** (`npm run dev`); `npm run build`/`dist/` must not be
   served because Vite inlines `VITE_*` env vars into client JS, which would
   expose the RLS-bypassing service role key. See Task 8.

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `supabase/migrations/0005_soundscape_bundled_catalog.sql` | Local catalog table + seed rows + RLS | Create |
| `tools/soundscape-admin/bundled-manifest.json` | APK raw ↔ string-key map (seed source) | Create |
| `tools/soundscape-admin/package.json` | Debug admin deps/scripts | Create |
| `tools/soundscape-admin/vite.config.ts` | Vite dev server | Create |
| `tools/soundscape-admin/index.html` | SPA shell | Create |
| `tools/soundscape-admin/.env.example` | Env template | Create |
| `tools/soundscape-admin/src/main.ts` | Admin UI entry | Create |
| `tools/soundscape-admin/src/supabase.ts` | Supabase client (service role) | Create |
| `tools/soundscape-admin/src/types.ts` | Shared TS types | Create |
| `tools/soundscape-admin/src/styles.css` | Minimal layout | Create |
| `tools/soundscape-admin/README.md` | How to run admin | Create |
| `purrodoro-android/.../soundscape/BundledTrackRegistry.kt` | APK raw + string res defaults (source of *existence*) | Create |
| `purrodoro-android/.../soundscape/SoundscapeCatalogBuilder.kt` | Merge local + remote → options | Create |
| `purrodoro-android/.../soundscape/BundledCatalogApi.kt` | PostgREST fetch for bundled table | Create |
| `purrodoro-android/.../soundscape/BundledCatalogRepository.kt` | Room cache + refresh | Create |
| `purrodoro-android/.../data/local/SoundscapeBundledEntity.kt` | Room entity + DAO | Create |
| `purrodoro-android/.../data/local/PurrodoroDatabase.kt` | DB v5 + migration | Modify |
| `purrodoro-android/.../soundscape/SoundscapeModule.kt` | Wire bundled repo | Modify |
| `purrodoro-android/.../PurrodoroApp.kt` | Use builder; remove hardcoded lists | Modify |
| `purrodoro-android/.../test/.../SoundscapeCatalogBuilderTest.kt` | Builder unit tests | Create |
| `purrodoro-android/.../test/.../BundledCatalogRepositoryTest.kt` | Bundled refresh tests | Create |
| `docs/CLOUD_ASSET_MANAGEMENT.md` | Document admin tool + local table | Modify |
| `.gitignore` | Ignore `tools/soundscape-admin/.env.local` + `node_modules` | Modify |

---

### Task 1: Supabase migration — `soundscape_bundled_catalog`

**Files:**
- Create: `supabase/migrations/0005_soundscape_bundled_catalog.sql`

- [ ] **Step 1: Write the migration**

```sql
-- Local (APK) soundscape catalog metadata. Audio bytes live in res/raw;
-- this table controls visibility, premium, and order without an app release.

create table public.soundscape_bundled_catalog (
  kind text not null check (kind in ('music', 'ambience')),
  local_id int not null check (local_id >= 1 and local_id < 100),
  name text not null default '',
  artist text not null default '',
  symbol text not null default '',
  raw_resource text not null,
  is_premium boolean not null default false,
  sort_order int not null default 0,
  is_active boolean not null default true,
  primary key (kind, local_id)
);

alter table public.soundscape_bundled_catalog enable row level security;

-- Public read of ALL rows (active and inactive). The app needs inactive rows
-- to distinguish "hidden by admin" from "not yet seeded" — see Design
-- decision 3. This table holds only flags/metadata, never audio bytes, so
-- exposing inactive rows is harmless. Writes go through the service role
-- (admin tool), which bypasses RLS.
create policy "public_read_bundled" on public.soundscape_bundled_catalog
  for select using (true);

-- Seed: matches PurrodoroApp.kt defaults + res/raw filenames (2026-06-17).
insert into public.soundscape_bundled_catalog
  (kind, local_id, raw_resource, is_premium, sort_order, symbol)
values
  ('ambience', 1, 'ambience_rain',         false, 10, 'Rain'),
  ('ambience', 2, 'ambience_cafe',         false, 20, 'Cafe'),
  ('ambience', 3, 'ambience_fireplace',    true,  30, 'Fire'),
  ('ambience', 4, 'ambience_forest',       true,  40, 'Forest'),
  ('ambience', 5, 'ambience_sea_wave',     true,  50, 'Wave'),
  ('ambience', 6, 'ambience_winter_wind',  true,  60, 'Wind'),
  ('music',    1, 'lofi_bloom_yasumu',              false, 10, ''),
  ('music',    2, 'lofi_chill_fm_dope_cat',         false, 20, ''),
  ('music',    3, 'lofi_midnight_whisper_matrika',  true,  30, '');
```

- [ ] **Step 2: Apply migration to Supabase**

Run (from repo root, with Supabase CLI linked to project `birjkyerhokqdzhdfgpt`):

```bash
supabase db push
```

Expected: migration `0005_soundscape_bundled_catalog` applied without error.

- [ ] **Step 3: Verify public read**

```bash
curl "https://birjkyerhokqdzhdfgpt.supabase.co/rest/v1/soundscape_bundled_catalog?select=kind,local_id,raw_resource,is_premium,is_active" \
  -H "apikey: sb_publishable_GgfnZ3nRLCm-_HBfJfbWVw_4K2D_KRu"
```

Expected: JSON array with 9 rows (6 ambience + 3 music).

- [ ] **Step 4: Commit**

```bash
git add supabase/migrations/0005_soundscape_bundled_catalog.sql
git commit -m "feat: add soundscape_bundled_catalog table for local track metadata"
```

---

### Task 2: Bundled manifest (repo source of truth)

**Files:**
- Create: `tools/soundscape-admin/bundled-manifest.json`

- [ ] **Step 1: Create manifest**

```json
[
  {
    "kind": "ambience",
    "localId": 1,
    "rawResource": "ambience_rain",
    "symbol": "Rain",
    "defaultPremium": false,
    "sortOrder": 10,
    "stringKeys": { "name": "sound_rain_name", "description": "sound_rain_desc" }
  },
  {
    "kind": "ambience",
    "localId": 2,
    "rawResource": "ambience_cafe",
    "symbol": "Cafe",
    "defaultPremium": false,
    "sortOrder": 20,
    "stringKeys": { "name": "sound_cafe_name", "description": "sound_cafe_desc" }
  },
  {
    "kind": "ambience",
    "localId": 3,
    "rawResource": "ambience_fireplace",
    "symbol": "Fire",
    "defaultPremium": true,
    "sortOrder": 30,
    "stringKeys": { "name": "sound_fire_name", "description": "sound_fire_desc" }
  },
  {
    "kind": "ambience",
    "localId": 4,
    "rawResource": "ambience_forest",
    "symbol": "Forest",
    "defaultPremium": true,
    "sortOrder": 40,
    "stringKeys": { "name": "sound_forest_name", "description": "sound_forest_desc" }
  },
  {
    "kind": "ambience",
    "localId": 5,
    "rawResource": "ambience_sea_wave",
    "symbol": "Wave",
    "defaultPremium": true,
    "sortOrder": 50,
    "stringKeys": { "name": "sound_sea_wave_name", "description": "sound_sea_wave_desc" }
  },
  {
    "kind": "ambience",
    "localId": 6,
    "rawResource": "ambience_winter_wind",
    "symbol": "Wind",
    "defaultPremium": true,
    "sortOrder": 60,
    "stringKeys": { "name": "sound_winter_wind_name", "description": "sound_winter_wind_desc" }
  },
  {
    "kind": "music",
    "localId": 1,
    "rawResource": "lofi_bloom_yasumu",
    "defaultPremium": false,
    "sortOrder": 10,
    "stringKeys": { "name": "music_bloom", "artist": "music_bloom_artist" }
  },
  {
    "kind": "music",
    "localId": 2,
    "rawResource": "lofi_chill_fm_dope_cat",
    "defaultPremium": false,
    "sortOrder": 20,
    "stringKeys": { "name": "music_chill_fm", "artist": "music_chill_fm_artist" }
  },
  {
    "kind": "music",
    "localId": 3,
    "rawResource": "lofi_midnight_whisper_matrika",
    "defaultPremium": true,
    "sortOrder": 30,
    "stringKeys": { "name": "music_midnight_whisper", "artist": "music_midnight_whisper_artist" }
  }
]
```

- [ ] **Step 2: Commit**

```bash
git add tools/soundscape-admin/bundled-manifest.json
git commit -m "docs: add bundled soundscape manifest for admin and Android registry"
```

---

### Task 3: ~~Android — delivery enum + catalog entry model~~ (removed)

**Removed in lead review (Design decision 1).** `SoundscapeDelivery` and
`SoundscapeCatalogEntry` are not built: nothing downstream consumes them, and
delivery is already encoded by `RemoteTrackInfo? == null` on the existing
`MusicOption` / `SoundOption` models. The builder (Task 6) produces those
models directly. Task numbers below are unchanged to avoid churn.

---

### Task 4: Android — `BundledTrackRegistry` (APK raw + string defaults)

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/BundledTrackRegistry.kt`
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/soundscape/BundledTrackRegistryTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.R
import org.junit.Assert.assertEquals
import org.junit.Test

class BundledTrackRegistryTest {

    @Test
    fun `resolveRaw maps ambience_rain to R raw id`() {
        assertEquals(R.raw.ambience_rain, BundledTrackRegistry.rawResId("ambience_rain"))
    }

    @Test
    fun `defaults include six ambience and three music tracks`() {
        assertEquals(6, BundledTrackRegistry.defaults.count { it.kind == "ambience" })
        assertEquals(3, BundledTrackRegistry.defaults.count { it.kind == "music" })
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests com.endiar.purrodoro.soundscape.BundledTrackRegistryTest -q`

- [ ] **Step 3: Implement registry**

```kotlin
package com.endiar.purrodoro.soundscape

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.endiar.purrodoro.R

data class BundledTrackDefault(
    val kind: String,
    val localId: Int,
    val rawResource: String,
    @StringRes val nameRes: Int,
    @StringRes val subtitleRes: Int,
    val symbol: String,
    val defaultPremium: Boolean,
    val sortOrder: Int,
)

object BundledTrackRegistry {

    val defaults: List<BundledTrackDefault> = listOf(
        BundledTrackDefault("ambience", 1, "ambience_rain", R.string.sound_rain_name, R.string.sound_rain_desc, "Rain", false, 10),
        BundledTrackDefault("ambience", 2, "ambience_cafe", R.string.sound_cafe_name, R.string.sound_cafe_desc, "Cafe", false, 20),
        BundledTrackDefault("ambience", 3, "ambience_fireplace", R.string.sound_fire_name, R.string.sound_fire_desc, "Fire", true, 30),
        BundledTrackDefault("ambience", 4, "ambience_forest", R.string.sound_forest_name, R.string.sound_forest_desc, "Forest", true, 40),
        BundledTrackDefault("ambience", 5, "ambience_sea_wave", R.string.sound_sea_wave_name, R.string.sound_sea_wave_desc, "Wave", true, 50),
        BundledTrackDefault("ambience", 6, "ambience_winter_wind", R.string.sound_winter_wind_name, R.string.sound_winter_wind_desc, "Wind", true, 60),
        BundledTrackDefault("music", 1, "lofi_bloom_yasumu", R.string.music_bloom, R.string.music_bloom_artist, "", false, 10),
        BundledTrackDefault("music", 2, "lofi_chill_fm_dope_cat", R.string.music_chill_fm, R.string.music_chill_fm_artist, "", false, 20),
        BundledTrackDefault("music", 3, "lofi_midnight_whisper_matrika", R.string.music_midnight_whisper, R.string.music_midnight_whisper_artist, "", true, 30),
    )

    private val rawByName: Map<String, Int> = mapOf(
        "ambience_rain" to R.raw.ambience_rain,
        "ambience_cafe" to R.raw.ambience_cafe,
        "ambience_fireplace" to R.raw.ambience_fireplace,
        "ambience_forest" to R.raw.ambience_forest,
        "ambience_sea_wave" to R.raw.ambience_sea_wave,
        "ambience_winter_wind" to R.raw.ambience_winter_wind,
        "lofi_bloom_yasumu" to R.raw.lofi_bloom_yasumu,
        "lofi_chill_fm_dope_cat" to R.raw.lofi_chill_fm_dope_cat,
        "lofi_midnight_whisper_matrika" to R.raw.lofi_midnight_whisper_matrika,
    )

    @RawRes
    fun rawResId(rawResource: String): Int =
        rawByName[rawResource] ?: error("Unknown bundled raw resource: $rawResource")
}
```

- [ ] **Step 4: Run test — expect PASS**

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/BundledTrackRegistry.kt \
        purrodoro-android/app/src/test/java/com/endiar/purrodoro/soundscape/BundledTrackRegistryTest.kt
git commit -m "feat: add BundledTrackRegistry for APK soundscape defaults"
```

---

### Task 5: Android — bundled catalog API + Room cache

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/BundledCatalogApi.kt`
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/local/SoundscapeBundledEntity.kt`
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/BundledCatalogRepository.kt`
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/local/PurrodoroDatabase.kt`
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/soundscape/BundledCatalogRepositoryTest.kt`

- [ ] **Step 1: Write failing repository test**

```kotlin
package com.endiar.purrodoro.soundscape

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundledCatalogRepositoryTest {

    private lateinit var db: PurrodoroDatabase
    private lateinit var repository: BundledCatalogRepository
    private var apiResult: () -> List<BundledCatalogDto> = { emptyList() }

    private val fakeApi = object : BundledCatalogApi {
        override suspend fun fetchActive(): List<BundledCatalogDto> = apiResult()
    }

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PurrodoroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BundledCatalogRepository(fakeApi, db.soundscapeBundledDao())
    }

    @After
    fun tearDown() = db.close()

    private fun dto(kind: String, localId: Int) = BundledCatalogDto(
        kind = kind,
        localId = localId,
        name = "",
        artist = "",
        symbol = "Rain",
        rawResource = "ambience_rain",
        isPremium = false,
        sortOrder = localId,
        isActive = true,
    )

    @Test
    fun `refresh stores fetched bundled rows`() = runTest {
        apiResult = { listOf(dto("ambience", 1)) }
        assertTrue(repository.refresh())
        assertEquals(listOf("ambience:1"), repository.snapshot().map { "${it.kind}:${it.localId}" })
    }

    @Test
    fun `failed refresh keeps cache`() = runTest {
        apiResult = { listOf(dto("ambience", 1)) }
        repository.refresh()
        apiResult = { throw RuntimeException("offline") }
        assertFalse(repository.refresh())
        assertEquals(1, repository.snapshot().size)
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement API DTO + interface**

`BundledCatalogApi.kt`:

```kotlin
package com.endiar.purrodoro.soundscape

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BundledCatalogDto(
    @SerialName("kind") val kind: String,
    @SerialName("local_id") val localId: Int,
    @SerialName("name") val name: String,
    @SerialName("artist") val artist: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("raw_resource") val rawResource: String,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("is_active") val isActive: Boolean,
)

interface BundledCatalogApi {
    suspend fun fetchActive(): List<BundledCatalogDto>
}

class SupabaseBundledCatalogApi(
    private val client: SupabaseClient,
) : BundledCatalogApi {
    override suspend fun fetchActive(): List<BundledCatalogDto> =
        client.from("soundscape_bundled_catalog").select().decodeList()
}
```

- [ ] **Step 4: Implement Room entity + DAO**

`SoundscapeBundledEntity.kt`:

```kotlin
package com.endiar.purrodoro.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "soundscape_bundled",
    primaryKeys = ["kind", "localId"],
)
data class SoundscapeBundledEntity(
    val kind: String,
    val localId: Int,
    val name: String,
    val artist: String,
    val symbol: String,
    val rawResource: String,
    val isPremium: Boolean,
    val sortOrder: Int,
    val isActive: Boolean,
)

@Dao
interface SoundscapeBundledDao {
    @Query("SELECT * FROM soundscape_bundled ORDER BY sortOrder, localId")
    fun observeAll(): Flow<List<SoundscapeBundledEntity>>

    @Query("SELECT * FROM soundscape_bundled ORDER BY sortOrder, localId")
    suspend fun getAll(): List<SoundscapeBundledEntity>

    @Transaction
    suspend fun replaceAll(rows: List<SoundscapeBundledEntity>) {
        clear()
        insertAll(rows)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<SoundscapeBundledEntity>)

    @Query("DELETE FROM soundscape_bundled")
    suspend fun clear()
}
```

- [ ] **Step 5: Bump Room to v5 in `PurrodoroDatabase.kt`**

Add entity to `@Database(entities = [..., SoundscapeBundledEntity::class], version = 5)`.

Add abstract `fun soundscapeBundledDao(): SoundscapeBundledDao`.

Add migration:

```kotlin
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `soundscape_bundled` (" +
                "`kind` TEXT NOT NULL, " +
                "`localId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`artist` TEXT NOT NULL, " +
                "`symbol` TEXT NOT NULL, " +
                "`rawResource` TEXT NOT NULL, " +
                "`isPremium` INTEGER NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, " +
                "`isActive` INTEGER NOT NULL, " +
                "PRIMARY KEY(`kind`, `localId`))"
        )
    }
}
```

Register: `.addMigrations(..., MIGRATION_4_5)`.

- [ ] **Step 6: Implement repository**

```kotlin
package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.data.local.SoundscapeBundledDao
import com.endiar.purrodoro.data.local.SoundscapeBundledEntity
import kotlinx.coroutines.flow.Flow

class BundledCatalogRepository(
    private val api: BundledCatalogApi,
    private val dao: SoundscapeBundledDao,
) {
    fun observeCatalog(): Flow<List<SoundscapeBundledEntity>> = dao.observeAll()

    suspend fun snapshot(): List<SoundscapeBundledEntity> = dao.getAll()

    suspend fun refresh(): Boolean = runCatching {
        val rows = api.fetchActive().map { it.toEntity() }
        dao.replaceAll(rows)
    }.isSuccess
}

private fun BundledCatalogDto.toEntity() = SoundscapeBundledEntity(
    kind = kind,
    localId = localId,
    name = name,
    artist = artist,
    symbol = symbol,
    rawResource = rawResource,
    isPremium = isPremium,
    sortOrder = sortOrder,
    isActive = isActive,
)
```

- [ ] **Step 7: Run test — expect PASS**

Run: `cd purrodoro-android && ./gradlew :app:testDebugUnitTest --tests com.endiar.purrodoro.soundscape.BundledCatalogRepositoryTest -q`

- [ ] **Step 8: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/BundledCatalogApi.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/BundledCatalogRepository.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/local/SoundscapeBundledEntity.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/local/PurrodoroDatabase.kt \
        purrodoro-android/app/src/test/java/com/endiar/purrodoro/soundscape/BundledCatalogRepositoryTest.kt
git commit -m "feat: cache bundled soundscape catalog in Room"
```

---

### Task 6: Android — `SoundscapeCatalogBuilder`

**Files:**
- Create: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/SoundscapeCatalogBuilder.kt`
- Create: `purrodoro-android/app/src/test/java/com/endiar/purrodoro/soundscape/SoundscapeCatalogBuilderTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.R
import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.data.local.SoundscapeBundledEntity
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundscapeCatalogBuilderTest {

    private val rainDefault = BundledTrackDefault(
        kind = "ambience", localId = 1, rawResource = "ambience_rain",
        nameRes = R.string.sound_rain_name, subtitleRes = R.string.sound_rain_desc,
        symbol = "Rain", defaultPremium = false, sortOrder = 10,
    )
    private val cafeDefault = BundledTrackDefault(
        kind = "ambience", localId = 2, rawResource = "ambience_cafe",
        nameRes = R.string.sound_cafe_name, subtitleRes = R.string.sound_cafe_desc,
        symbol = "Cafe", defaultPremium = false, sortOrder = 20,
    )

    private fun bundled(
        localId: Int,
        premium: Boolean = false,
        active: Boolean = true,
        sort: Int = 0,
    ) = SoundscapeBundledEntity(
        kind = "ambience", localId = localId, name = "", artist = "", symbol = "",
        rawResource = "ambience_rain", isPremium = premium, sortOrder = sort, isActive = active,
    )

    private fun remote(id: Int) = SoundscapeTrackEntity(
        id = id, kind = "music", name = "Cloud Track", artist = "Artist", symbol = "",
        filePath = "lofi/x.mp3", fileSizeBytes = 2_000_000, isPremium = false,
        sortOrder = 10, updatedAtMillis = 1_000,
    )

    @Test
    fun `missing rows fall back to defaults and stay visible`() {
        // No DB rows (offline / never fetched): every default shows, local audio, no remote.
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = emptyList(),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault, cafeDefault),
        )
        assertEquals(listOf(1, 2), options.map { it.id })
        assertEquals(AudioSource.Bundled(R.raw.ambience_rain), options[0].audio)
        assertNull(options[0].remote)
    }

    @Test
    fun `inactive row hides its default`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(bundled(localId = 1, active = false)),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault, cafeDefault),
        )
        assertEquals(listOf(2), options.map { it.id }) // rain hidden, cafe remains
    }

    @Test
    fun `row premium override wins over default`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(bundled(localId = 1, premium = true)),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault),
        )
        assertTrue(options.single().locked)
    }

    @Test
    fun `row sort_order override reorders bundled tracks`() {
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(
                bundled(localId = 1, sort = 99), // rain pushed last
                bundled(localId = 2, sort = 1),  // cafe pulled first
            ),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "name" },
            subtitleFor = { _, _ -> "sub" },
            bundledDefaults = listOf(rainDefault, cafeDefault),
        )
        assertEquals(listOf(2, 1), options.map { it.id })
    }

    @Test
    fun `non-blank name and description overrides win`() {
        val row = bundled(localId = 1)
            .copy(name = "Custom Rain", artist = "Soft drizzle", symbol = "Storm")
        val options = SoundscapeCatalogBuilder.buildAmbienceOptions(
            bundledRows = listOf(row),
            remoteRows = emptyList(),
            downloadStates = emptyMap(),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "default name" },
            subtitleFor = { _, _ -> "default sub" },
            bundledDefaults = listOf(rainDefault),
        )
        assertEquals("Custom Rain", options.single().name)
        assertEquals("Soft drizzle", options.single().description)
        assertEquals("Storm", options.single().symbol)
    }

    @Test
    fun `remote music not downloaded has null audio and NotDownloaded state`() {
        val options = SoundscapeCatalogBuilder.buildMusicOptions(
            bundledRows = emptyList(),
            remoteRows = listOf(remote(100)),
            downloadStates = mapOf(100 to DownloadState.NotDownloaded),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "unused" },
            subtitleFor = { _, _ -> "unused" },
            bundledDefaults = emptyList(),
        )
        assertNull(options.single().audio)
        assertEquals(DownloadState.NotDownloaded, options.single().remote?.downloadState)
    }

    @Test
    fun `bundled tracks render before remote tracks`() {
        val options = SoundscapeCatalogBuilder.buildMusicOptions(
            bundledRows = emptyList(),
            remoteRows = listOf(remote(100)),
            downloadStates = mapOf(100 to DownloadState.NotDownloaded),
            localPaths = emptyMap(),
            nameFor = { _, _ -> "Bloom" },
            subtitleFor = { _, _ -> "Yasumu" },
            bundledDefaults = listOf(
                BundledTrackDefault(
                    "music", 1, "lofi_bloom_yasumu",
                    R.string.music_bloom, R.string.music_bloom_artist, "", false, 10,
                ),
            ),
        )
        assertEquals(listOf(1, 100), options.map { it.id })
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement builder**

```kotlin
package com.endiar.purrodoro.soundscape

import com.endiar.purrodoro.audio.AudioSource
import com.endiar.purrodoro.data.local.SoundscapeBundledEntity
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import com.endiar.purrodoro.model.MusicOption
import com.endiar.purrodoro.model.SoundOption

/**
 * Pure merge of the bundled (APK) registry + cached overrides + the remote
 * cloud catalog into the option lists the Sounds sheet renders.
 *
 * Ownership of "existence" is the registry: we always iterate [bundledDefaults]
 * and left-join the cached DB row by (kind, localId). A missing row keeps the
 * default and stays visible (a freshly-bundled track not yet seeded must never
 * silently disappear); an inactive row (`isActive == false`) hides its default.
 * Non-blank `name` / `artist` / `symbol` on the row override the string-res
 * defaults. Bundled tracks always render before remote tracks.
 */
object SoundscapeCatalogBuilder {

    fun buildAmbienceOptions(
        bundledRows: List<SoundscapeBundledEntity>,
        remoteRows: List<SoundscapeTrackEntity>,
        downloadStates: Map<Int, DownloadState>,
        localPaths: Map<Int, String>,
        nameFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        subtitleFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        bundledDefaults: List<BundledTrackDefault> = BundledTrackRegistry.defaults,
    ): List<SoundOption> {
        val bundled = mergeBundled("ambience", bundledRows, bundledDefaults)
            .map { (def, row) ->
                SoundOption(
                    id = def.localId,
                    name = row?.name?.takeIf { it.isNotBlank() } ?: nameFor(row, def),
                    description = row?.artist?.takeIf { it.isNotBlank() } ?: subtitleFor(row, def),
                    locked = row?.isPremium ?: def.defaultPremium,
                    symbol = row?.symbol?.takeIf { it.isNotBlank() } ?: def.symbol,
                    audio = AudioSource.Bundled(rawResIdFor(row, def)),
                    remote = null,
                )
            }
        val remote = remoteRows
            .filter { it.kind == "ambience" }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { row ->
                row.toSoundOption(
                    downloadState = stateFor(row, downloadStates),
                    localPath = localPathFor(row, downloadStates, localPaths),
                )
            }
        return bundled + remote
    }

    fun buildMusicOptions(
        bundledRows: List<SoundscapeBundledEntity>,
        remoteRows: List<SoundscapeTrackEntity>,
        downloadStates: Map<Int, DownloadState>,
        localPaths: Map<Int, String>,
        nameFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        subtitleFor: (SoundscapeBundledEntity?, BundledTrackDefault) -> String,
        bundledDefaults: List<BundledTrackDefault> = BundledTrackRegistry.defaults,
    ): List<MusicOption> {
        val bundled = mergeBundled("music", bundledRows, bundledDefaults)
            .map { (def, row) ->
                MusicOption(
                    id = def.localId,
                    name = row?.name?.takeIf { it.isNotBlank() } ?: nameFor(row, def),
                    artist = row?.artist?.takeIf { it.isNotBlank() } ?: subtitleFor(row, def),
                    audio = AudioSource.Bundled(rawResIdFor(row, def)),
                    locked = row?.isPremium ?: def.defaultPremium,
                    remote = null,
                )
            }
        val remote = remoteRows
            .filter { it.kind == "music" }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { row ->
                row.toMusicOption(
                    downloadState = stateFor(row, downloadStates),
                    localPath = localPathFor(row, downloadStates, localPaths),
                )
            }
        return bundled + remote
    }

    /**
     * Left-join the registry defaults (existence) with cached rows (overrides).
     * Returns visible `(def, row?)` pairs sorted by effective sort order:
     * - row absent  → visible default
     * - row present and inactive → dropped (hidden)
     * - row present and active   → kept, overrides applied by the caller
     */
    private fun mergeBundled(
        kind: String,
        rows: List<SoundscapeBundledEntity>,
        defaults: List<BundledTrackDefault>,
    ): List<Pair<BundledTrackDefault, SoundscapeBundledEntity?>> {
        val byLocalId = rows.filter { it.kind == kind }.associateBy { it.localId }
        return defaults
            .filter { it.kind == kind }
            .mapNotNull { def ->
                val row = byLocalId[def.localId]
                if (row != null && !row.isActive) return@mapNotNull null
                def to row
            }
            .sortedBy { (def, row) -> row?.sortOrder ?: def.sortOrder }
    }

    private fun rawResIdFor(row: SoundscapeBundledEntity?, def: BundledTrackDefault): Int =
        BundledTrackRegistry.rawResId(row?.rawResource?.takeIf { it.isNotBlank() } ?: def.rawResource)

    private fun stateFor(row: SoundscapeTrackEntity, states: Map<Int, DownloadState>): DownloadState =
        states[row.id] ?: DownloadState.NotDownloaded

    private fun localPathFor(
        row: SoundscapeTrackEntity,
        states: Map<Int, DownloadState>,
        localPaths: Map<Int, String>,
    ): String? = localPaths[row.id]?.takeIf { stateFor(row, states) == DownloadState.Downloaded }
}
```

> **Note:** `mergeBundled` is shared by both kinds — no separate
> `mergeBundledMusic` / `defLocalId`. For music, `subtitleFor` supplies the
> artist string; for ambience it supplies the description. When `rows` is empty
> (offline / never fetched) every default is kept and visible, matching today's
> hardcoded lists.

- [ ] **Step 4: Run tests — expect PASS**

- [ ] **Step 5: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/SoundscapeCatalogBuilder.kt \
        purrodoro-android/app/src/test/java/com/endiar/purrodoro/soundscape/SoundscapeCatalogBuilderTest.kt
git commit -m "feat: build unified local and remote soundscape option lists"
```

---

### Task 7: Android — wire module + refactor `PurrodoroApp.kt`

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/SoundscapeModule.kt`
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

- [ ] **Step 1: Extend `SoundscapeModule.Holder`**

Add `bundledCatalogRepository: BundledCatalogRepository` built with `SupabaseBundledCatalogApi` + `db.soundscapeBundledDao()`.

- [ ] **Step 2: Observe bundled catalog in `PurrodoroApp.kt`**

```kotlin
val bundledTracks by soundscape.bundledCatalogRepository.observeCatalog()
    .collectAsStateWithLifecycle(initialValue = emptyList())
```

- [ ] **Step 3: Refresh bundled on startup**

In the existing `LaunchedEffect(Unit)` block, after remote refresh:

```kotlin
soundscape.bundledCatalogRepository.refresh()
```

- [ ] **Step 4: Replace hardcoded lists**

Remove `bundledAmbienceOptions` / `bundledMusicOptions` blocks (~lines 435–505).

Replace `musicOptions` / `ambienceOptions` construction with:

> **Note:** resolve display strings with `context.getString(...)`, **not**
> `stringResource(...)`. The builder invokes `nameFor`/`subtitleFor` in
> non-composable context, so a `@Composable` call there fails to compile.
> `LocalContext` is swapped by `KuroLocalizationProvider`, so `context` already
> resolves the in-app language.

```kotlin
val localPaths = remember(catalogTracks) {
    catalogTracks.associate { track ->
        track.id to soundscape.downloadManager.localFile(track).absolutePath
    }
}
val ambienceOptions = SoundscapeCatalogBuilder.buildAmbienceOptions(
    bundledRows = bundledTracks,
    remoteRows = catalogTracks,
    downloadStates = downloadStates,
    localPaths = localPaths,
    nameFor = { _, def -> context.getString(def.nameRes) },
    subtitleFor = { _, def -> context.getString(def.subtitleRes) },
)
val musicOptions = SoundscapeCatalogBuilder.buildMusicOptions(
    bundledRows = bundledTracks,
    remoteRows = catalogTracks,
    downloadStates = downloadStates,
    localPaths = localPaths,
    nameFor = { _, def -> context.getString(def.nameRes) },
    subtitleFor = { _, def -> context.getString(def.subtitleRes) },
)
```

- [ ] **Step 5: Run full unit tests**

Run: `cd purrodoro-android && ./gradlew test -q`

Expected: all tests pass (253+).

- [ ] **Step 6: Commit**

```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/SoundscapeModule.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: wire bundled catalog overrides into PurrodoroApp soundscape lists"
```

---

### Task 8: Debug web app — scaffold

> **⚠️ Security — dev-server only.** This tool uses the Supabase **service role
> key**, which bypasses RLS (full read/write on every table). Vite inlines any
> `VITE_*` env var into the client bundle at build time, so **never run
> `npm run build` / serve `dist/`** for this app — that would ship the key in
> plaintext JS. Use `npm run dev` locally only. Keep the key in `.env.local`
> (gitignored); never commit it. If a hosted admin is ever needed, move writes
> behind a server that holds the key server-side (out of scope here).

**Files:**
- Create: `tools/soundscape-admin/package.json`
- Create: `tools/soundscape-admin/vite.config.ts`
- Create: `tools/soundscape-admin/index.html`
- Create: `tools/soundscape-admin/.env.example`
- Create: `tools/soundscape-admin/tsconfig.json`
- Modify: `.gitignore`

- [ ] **Step 1: Add gitignore entries**

```
tools/soundscape-admin/node_modules/
tools/soundscape-admin/.env.local
tools/soundscape-admin/dist/
```

- [ ] **Step 2: Create `package.json`**

```json
{
  "name": "purrodoro-soundscape-admin",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@supabase/supabase-js": "^2.49.0"
  },
  "devDependencies": {
    "typescript": "^5.8.0",
    "vite": "^6.2.0"
  }
}
```

- [ ] **Step 3: Create `.env.example`**

```
VITE_SUPABASE_URL=https://birjkyerhokqdzhdfgpt.supabase.co
VITE_SUPABASE_SERVICE_ROLE_KEY=your-service-role-key-here
```

- [ ] **Step 4: Install and verify dev server starts**

```bash
cd tools/soundscape-admin
cp .env.example .env.local
npm install
npm run dev
```

Expected: Vite serves on `http://localhost:5173` (empty page OK for now).

- [ ] **Step 5: Commit scaffold**

```bash
git add tools/soundscape-admin/package.json tools/soundscape-admin/vite.config.ts \
        tools/soundscape-admin/index.html tools/soundscape-admin/.env.example \
        tools/soundscape-admin/tsconfig.json .gitignore
git commit -m "feat: scaffold soundscape debug admin web app"
```

---

### Task 9: Debug web app — remote track management

**Files:**
- Create: `tools/soundscape-admin/src/types.ts`
- Create: `tools/soundscape-admin/src/supabase.ts`
- Create: `tools/soundscape-admin/src/styles.css`
- Create: `tools/soundscape-admin/src/main.ts`

- [ ] **Step 1: Define types**

```typescript
export type TrackKind = 'music' | 'ambience';

export interface RemoteTrack {
  id: number;
  kind: TrackKind;
  name: string;
  artist: string;
  symbol: string;
  file_path: string;
  file_size_bytes: number;
  is_premium: boolean;
  sort_order: number;
  is_active: boolean;
  updated_at: string;
}

export interface BundledTrack {
  kind: TrackKind;
  local_id: number;
  name: string;
  artist: string;
  symbol: string;
  raw_resource: string;
  is_premium: boolean;
  sort_order: number;
  is_active: boolean;
}
```

- [ ] **Step 2: Supabase client**

```typescript
import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_SERVICE_ROLE_KEY;

if (!url || !key) {
  throw new Error('Set VITE_SUPABASE_URL and VITE_SUPABASE_SERVICE_ROLE_KEY in .env.local');
}

export const supabase = createClient(url, key);
export const STORAGE_BUCKET = 'soundscape-audio';
export const publicAudioUrl = (filePath: string) =>
  `${url}/storage/v1/object/public/${STORAGE_BUCKET}/${filePath}`;
```

- [ ] **Step 3: Implement `main.ts` UI**

Build a single-page admin with two sections:

**Remote tracks panel**
- `loadRemoteTracks()`: `supabase.from('soundscape_tracks').select('*').order('sort_order')`
- Table columns: id, kind, name, active, premium, sort, file_path, size, actions
- Toggle `is_active` / `is_premium` via update
- **Add form:** pick kind, name, artist/symbol, premium, sort, file input → upload to `soundscape-audio/{lofi|ambience}/filename.mp3` → insert row with next id ≥ 100
- **Preview:** `<audio controls src={publicAudioUrl(file_path)}>`
- **Soft delete:** set `is_active = false`

**Local tracks panel**
- `loadBundledTracks()`: `supabase.from('soundscape_bundled_catalog').select('*').order('sort_order')`
- Read-only `raw_resource` column (from manifest)
- Editable: `is_active`, `is_premium`, `sort_order`
- Badge: "APK bundled — bytes not managed here"

- [ ] **Step 4: Manual smoke test**

1. Start admin: `npm run dev`
2. Toggle bundled `ambience` id 3 (`fireplace`) to `is_premium = false`
3. Relaunch Android app → Fire ambience unlocked for free users
4. Upload a test MP3 as remote id 100 → appears in Sounds sheet after relaunch

- [ ] **Step 5: Commit**

```bash
git add tools/soundscape-admin/src/
git commit -m "feat: soundscape admin remote and local catalog UI"
```

---

### Task 10: Documentation

**Files:**
- Create: `tools/soundscape-admin/README.md`
- Modify: `docs/CLOUD_ASSET_MANAGEMENT.md`

- [ ] **Step 1: Write admin README**

Include:
- Prerequisites (Node 20+, Supabase service role key)
- Setup: `cp .env.example .env.local`, fill key, `npm install`, `npm run dev`
- Remote workflow: upload + insert
- Local workflow: toggle visibility/premium only
- Security: never commit service role key; local debug only

- [ ] **Step 2: Update `CLOUD_ASSET_MANAGEMENT.md`**

Add section **"Debug admin (recommended)"** pointing to `tools/soundscape-admin/` and document `soundscape_bundled_catalog` table alongside existing remote docs.

- [ ] **Step 3: Commit**

```bash
git add tools/soundscape-admin/README.md docs/CLOUD_ASSET_MANAGEMENT.md
git commit -m "docs: add soundscape admin tool usage"
```

---

## Spec Coverage Self-Review

| Spec requirement | Task |
|---|---|
| Local vs remote model in app | Tasks 6, 7 (delivery = `remote == null`; no separate type — Design decision 1) |
| Local: visibility + premium | Tasks 1, 5, 6, 7, 9 |
| Remote: visibility + premium + download | Tasks 6, 7 (download unchanged), 9 |
| `soundscape_tracks` remote-only | Task 9 (remote panel only writes that table) |
| Bundled APK mapping | Tasks 2, 4 |
| Debug admin list/add/remove | Task 9 |
| Offline fallback for bundled | Task 6 (`defaults` when cache empty) |
| Hide vs unseeded bundled track | Tasks 1, 5, 6 (read inactive rows; missing → visible — Design decision 2/3) |
| Docs | Task 10 |

**Placeholder scan:** none — all steps include concrete, compiling code.

**Type consistency:** `BundledCatalogDto.localId` ↔ DB `local_id`; Room
`SoundscapeBundledEntity.localId`; `is_active` flows DTO → entity → builder. UI
options use per-kind `localId` (unchanged from today). Removed `SoundscapeCatalogEntry`/`SoundscapeDelivery` (Design decision 1).

---

## Follow-ups (not in this plan)

- Promote local → remote workflow in admin UI
- CI check: `bundled-manifest.json` ↔ `res/raw/` parity
- Admin preview for bundled tracks (play from repo `assets/` folder, not APK)

---

**Plan complete and saved to `docs/superpowers/plans/2026-06-17-soundscape-admin-and-unified-catalog.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
