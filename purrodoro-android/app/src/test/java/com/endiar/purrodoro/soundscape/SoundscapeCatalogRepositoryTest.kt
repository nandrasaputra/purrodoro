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
class SoundscapeCatalogRepositoryTest {

    private lateinit var db: PurrodoroDatabase
    private lateinit var repository: SoundscapeCatalogRepository
    private var apiResult: () -> List<SoundscapeTrackDto> = { emptyList() }

    private val fakeApi = object : SoundscapeCatalogApi {
        override suspend fun fetchActiveTracks(): List<SoundscapeTrackDto> = apiResult()
    }

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PurrodoroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SoundscapeCatalogRepository(fakeApi, db.soundscapeTrackDao())
    }

    @After
    fun tearDown() = db.close()

    private fun dto(id: Int) = SoundscapeTrackDto(
        id = id,
        kind = "music",
        name = "Track $id",
        artist = "Artist",
        symbol = "",
        filePath = "lofi/track_$id.mp3",
        thumbnailPath = "lofi/track_$id.webp",
        fileSizeBytes = 2_000_000,
        isPremium = false,
        sortOrder = id,
        updatedAt = "2026-06-13T00:00:00.000000+00:00",
    )

    @Test
    fun `refresh stores fetched rows and parses updated_at to millis`() = runTest {
        apiResult = { listOf(dto(100)) }
        assertTrue(repository.refresh())
        val rows = repository.snapshot()
        assertEquals(listOf(100), rows.map { it.id })
        assertEquals(1_781_308_800_000L, rows.single().updatedAtMillis) // 2026-06-13T00:00Z
    }

    @Test
    fun `refresh drops rows below the remote id floor`() = runTest {
        apiResult = { listOf(dto(5), dto(100)) }
        assertTrue(repository.refresh())
        assertEquals(listOf(100), repository.snapshot().map { it.id })
    }

    @Test
    fun `removed-on-server rows disappear on next refresh`() = runTest {
        apiResult = { listOf(dto(100), dto(101)) }
        repository.refresh()
        apiResult = { listOf(dto(101)) }
        repository.refresh()
        assertEquals(listOf(101), repository.snapshot().map { it.id })
    }

    @Test
    fun `failed refresh keeps the cache and returns false`() = runTest {
        apiResult = { listOf(dto(100)) }
        repository.refresh()
        apiResult = { throw RuntimeException("offline") }
        assertFalse(repository.refresh())
        assertEquals(listOf(100), repository.snapshot().map { it.id })
    }
}
