package com.endiar.purrodoro.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.local.SoundscapeTrackEntity
import com.endiar.purrodoro.model.DownloadState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackDownloadManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audioBytes = ByteArray(1024) { it.toByte() }

    private fun track(id: Int, version: Long = 1_000) = SoundscapeTrackEntity(
        id = id,
        kind = "music",
        name = "Track $id",
        artist = "Artist",
        symbol = "",
        filePath = "lofi/track_$id.mp3",
        fileSizeBytes = audioBytes.size.toLong(),
        isPremium = false,
        sortOrder = 0,
        updatedAtMillis = version,
    )

    private fun manager(engine: MockEngine) = TrackDownloadManager(
        context = context,
        httpClient = HttpClient(engine),
        baseUrl = "https://example.supabase.co",
    )

    @Before
    fun cleanDir() {
        File(context.filesDir, "soundscapes").deleteRecursively()
    }

    @Test
    fun `download writes the versioned file and ends in Downloaded`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(
                "https://example.supabase.co/storage/v1/object/public/soundscape-audio/lofi/track_100.mp3",
                request.url.toString(),
            )
            respond(audioBytes)
        }
        val manager = manager(engine)
        assertTrue(manager.download(track(100)))
        val file = File(context.filesDir, "soundscapes/100-v1000.mp3")
        assertTrue(file.exists())
        assertEquals(audioBytes.size.toLong(), file.length())
        assertEquals(DownloadState.Downloaded, manager.states.value[100])
    }

    @Test
    fun `failed download leaves no file and reverts to NotDownloaded`() = runTest {
        val manager = manager(MockEngine { respondError(HttpStatusCode.NotFound) })
        assertFalse(manager.download(track(100)))
        assertFalse(File(context.filesDir, "soundscapes/100-v1000.mp3").exists())
        assertEquals(DownloadState.NotDownloaded, manager.states.value[100])
    }

    @Test
    fun `refreshStates reports Downloaded only for the current version`() = runTest {
        val manager = manager(MockEngine { respond(audioBytes) })
        manager.download(track(100, version = 1_000))
        // server replaced the audio: same id, new updated_at
        manager.refreshStates(listOf(track(100, version = 2_000)))
        assertEquals(DownloadState.NotDownloaded, manager.states.value[100])
    }

    @Test
    fun `delete removes the file and updates state`() = runTest {
        val manager = manager(MockEngine { respond(audioBytes) })
        val t = track(100)
        manager.download(t)
        manager.delete(t)
        assertFalse(File(context.filesDir, "soundscapes/100-v1000.mp3").exists())
        assertEquals(DownloadState.NotDownloaded, manager.states.value[100])
    }

    @Test
    fun `prune deletes files for removed and replaced tracks`() = runTest {
        val manager = manager(MockEngine { respond(audioBytes) })
        manager.download(track(100, version = 1_000))
        manager.download(track(101, version = 1_000))
        // 100 was removed from the catalog, 101 was replaced (new version)
        manager.prune(listOf(track(101, version = 2_000)))
        val dir = File(context.filesDir, "soundscapes")
        assertEquals(emptyList<String>(), dir.listFiles().orEmpty().map { it.name })
    }
}
