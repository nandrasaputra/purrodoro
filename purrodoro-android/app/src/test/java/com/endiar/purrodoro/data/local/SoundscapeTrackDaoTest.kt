package com.endiar.purrodoro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SoundscapeTrackDaoTest {

    private lateinit var db: PurrodoroDatabase
    private lateinit var dao: SoundscapeTrackDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PurrodoroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.soundscapeTrackDao()
    }

    @After
    fun tearDown() = db.close()

    private fun track(id: Int, sortOrder: Int = 0) = SoundscapeTrackEntity(
        id = id,
        kind = "music",
        name = "Track $id",
        artist = "Artist",
        symbol = "",
        filePath = "lofi/track_$id.mp3",
        thumbnailPath = "lofi/track_$id.webp",
        fileSizeBytes = 2_000_000,
        isPremium = false,
        sortOrder = sortOrder,
        updatedAtMillis = 1_000,
    )

    @Test
    fun `replaceAll swaps the whole snapshot`() = runTest {
        dao.replaceAll(listOf(track(100), track(101)))
        dao.replaceAll(listOf(track(101), track(102)))
        assertEquals(listOf(101, 102), dao.getAll().map { it.id })
    }

    @Test
    fun `rows come back ordered by sortOrder then id`() = runTest {
        dao.replaceAll(listOf(track(102, sortOrder = 2), track(100, sortOrder = 1), track(101, sortOrder = 1)))
        assertEquals(listOf(100, 101, 102), dao.getAll().map { it.id })
    }
}
