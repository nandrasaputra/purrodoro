package com.endiar.purrodoro.soundscape

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads cloud soundscape tile art over HTTPS using the same Ktor/OkHttp stack as
 * [com.endiar.purrodoro.audio.TrackDownloadManager]. Coil was replaced because
 * its default fetcher was failing on-device while R2 URLs are otherwise reachable.
 *
 * Files are cached under `filesDir/soundscape-thumbs/` keyed by object path +
 * [cacheVersion] (catalog `updated_at` millis) so metadata bumps invalidate art.
 */
class SoundscapeThumbnailLoader(
    context: Context,
    private val httpClient: HttpClient,
    private val publicBaseUrl: String,
) {
    private val cacheDir = File(context.applicationContext.filesDir, "soundscape-thumbs")

    suspend fun loadBitmap(objectKey: String, cacheVersion: Long = 0L): Bitmap? {
        val key = objectKey.trim().trimStart('/')
        if (key.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            val cacheFile = cacheFileFor(key, cacheVersion)
            decodeFile(cacheFile) ?: fetchAndCache(key, cacheVersion, cacheFile)
        }
    }

    private fun cacheFileFor(objectKey: String, cacheVersion: Long): File {
        val safeName = objectKey.replace('/', '_')
        val versionSuffix = if (cacheVersion > 0L) "-v$cacheVersion" else ""
        return File(cacheDir, "$safeName$versionSuffix.webp")
    }

    private fun decodeFile(file: File): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    private suspend fun fetchAndCache(
        objectKey: String,
        cacheVersion: Long,
        cacheFile: File,
    ): Bitmap? = runCatching {
        val url = "${publicBaseUrl.trimEnd('/')}/$objectKey"
        val response = httpClient.get(url)
        check(response.status.isSuccess()) { "HTTP ${response.status}" }
        val bytes = response.readBytes()
        check(bytes.isNotEmpty()) { "empty body" }
        cacheDir.mkdirs()
        pruneStaleVersions(objectKey, cacheVersion)
        cacheFile.writeBytes(bytes)
        decodeBytes(bytes)
    }.getOrNull()

    private fun decodeBytes(bytes: ByteArray): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /** Drop older cached revisions when updated_at moves forward. */
    private fun pruneStaleVersions(objectKey: String, cacheVersion: Long) {
        val prefix = objectKey.replace('/', '_')
        cacheDir.listFiles().orEmpty()
            .filter { it.name.startsWith(prefix) && it.name != cacheFileFor(objectKey, cacheVersion).name }
            .forEach { it.delete() }
    }
}
