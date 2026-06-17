package com.endiar.purrodoro.ui

import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StorageModelsTest {

    private fun track(id: Int, kind: String = "music", name: String = "Track $id") =
        SoundscapeTrackEntity(
            id = id, kind = kind, name = name, artist = "", symbol = "",
            filePath = "x/$id.mp3", fileSizeBytes = 0, isPremium = false,
            sortOrder = 0, updatedAtMillis = 1,
        )

    @Test
    fun `only downloaded and downloading tracks appear, not-downloaded omitted`() {
        val catalog = listOf(track(100), track(101), track(102))
        val states = mapOf(
            100 to DownloadState.Downloaded,
            101 to DownloadState.NotDownloaded,
            102 to DownloadState.Downloading(0.5f),
        )
        val summary = buildStorageList(catalog, states) { 1_000 }
        assertEquals(listOf(100, 102), summary.items.map { it.id })
    }

    @Test
    fun `total counts downloaded sizes only, not in-progress`() {
        val catalog = listOf(track(100), track(101))
        val states = mapOf(
            100 to DownloadState.Downloaded,
            101 to DownloadState.Downloading(0.2f),
        )
        val summary = buildStorageList(catalog, states) { if (it.id == 100) 2_000 else 9_999 }
        assertEquals(2_000L, summary.totalBytes)
    }

    @Test
    fun `downloaded rows sort before downloading, then by size desc`() {
        val catalog = listOf(track(100), track(101), track(102))
        val states = mapOf(
            100 to DownloadState.Downloaded,
            101 to DownloadState.Downloaded,
            102 to DownloadState.Downloading(0.1f),
        )
        val summary = buildStorageList(catalog, states) { if (it.id == 100) 500 else 5_000 }
        // 101 (5000) before 100 (500), downloading 102 last.
        assertEquals(listOf(101, 100, 102), summary.items.map { it.id })
    }

    @Test
    fun `downloading row is not removable and carries progress`() {
        val summary = buildStorageList(
            listOf(track(102)),
            mapOf(102 to DownloadState.Downloading(0.42f)),
        ) { 0 }
        val row = summary.items.single()
        assertFalse(row.isRemovable)
        assertEquals(0.42f, row.progress)
    }

    @Test
    fun `downloaded row is removable with null progress`() {
        val summary = buildStorageList(
            listOf(track(100)),
            mapOf(100 to DownloadState.Downloaded),
        ) { 1_234 }
        val row = summary.items.single()
        assertTrue(row.isRemovable)
        assertEquals(null, row.progress)
        assertEquals(1_234L, row.sizeBytes)
    }

    @Test
    fun `formatBytes renders zero, KB, and MB`() {
        assertEquals("0 MB", formatBytes(0, Locale.US))
        assertEquals("740 KB", formatBytes(740L * 1024, Locale.US))
        assertEquals("2.0 MB", formatBytes(2L * 1024 * 1024, Locale.US))
    }
}
