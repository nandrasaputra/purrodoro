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
        rawResource = "l_amb_rain",
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
