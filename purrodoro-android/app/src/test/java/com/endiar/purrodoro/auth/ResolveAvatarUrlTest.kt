package com.endiar.purrodoro.auth

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveAvatarUrlTest {

    @Test
    fun prefersAvatarUrlKey() {
        val meta = buildJsonObject {
            put("avatar_url", "https://example.com/a.jpg")
            put("picture", "https://example.com/p.jpg")
        }
        assertEquals("https://example.com/a.jpg", resolveAvatarUrl(meta))
    }

    @Test
    fun fallsBackToPicture() {
        val meta = buildJsonObject {
            put("picture", "https://lh3.googleusercontent.com/photo")
        }
        assertEquals("https://lh3.googleusercontent.com/photo", resolveAvatarUrl(meta))
    }

    @Test
    fun blankValuesAreIgnored() {
        val meta = buildJsonObject {
            put("avatar_url", "   ")
            put("picture", "https://example.com/p.jpg")
        }
        assertEquals("https://example.com/p.jpg", resolveAvatarUrl(meta))
    }

    @Test
    fun nullMetaReturnsNull() {
        assertNull(resolveAvatarUrl(null))
    }
}
