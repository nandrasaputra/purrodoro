package com.endiar.purrodoro.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Caches the signed-in account's profile photo on disk (when Google/Supabase
 * provides one). Keyed by auth user id so re-login reuses the file.
 */
class AccountAvatarStore private constructor(context: Context) {

    private val cacheDir = File(context.applicationContext.filesDir, "account-avatars")
    private val httpClient = HttpClient(OkHttp)

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun localBitmap(userId: String): Bitmap? {
        val file = imageFile(userId)
        if (!file.exists() || file.length() == 0L) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    /**
     * Downloads [avatarUrl] when it differs from the cached revision. No-op when
     * the URL is blank (accounts without a Google profile photo).
     */
    suspend fun sync(userId: String, avatarUrl: String?): Boolean {
        val url = avatarUrl?.trim()?.takeIf { it.isNotBlank() } ?: return false
        val imageFile = imageFile(userId)
        val urlFile = urlMetaFile(userId)
        if (imageFile.exists() && urlFile.exists() && urlFile.readText() == url) {
            return true
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = httpClient.get(url)
                check(response.status.isSuccess()) { "HTTP ${response.status}" }
                val bytes = response.readRawBytes()
                check(bytes.isNotEmpty()) { "empty body" }
                cacheDir.mkdirs()
                imageFile.writeBytes(bytes)
                urlFile.writeText(url)
                bumpRevision()
                true
            }.getOrDefault(false)
        }
    }

    fun delete(userId: String) {
        imageFile(userId).delete()
        urlMetaFile(userId).delete()
        bumpRevision()
    }

    private fun imageFile(userId: String) = File(cacheDir, "$userId.img")

    private fun urlMetaFile(userId: String) = File(cacheDir, "$userId.url")

    private fun bumpRevision() {
        _revision.value++
    }

    companion object {
        @Volatile private var instance: AccountAvatarStore? = null

        fun get(context: Context): AccountAvatarStore =
            instance ?: synchronized(this) {
                instance ?: AccountAvatarStore(context.applicationContext).also { instance = it }
            }
    }
}
