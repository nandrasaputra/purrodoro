package com.endiar.purrodoro.sync

import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncModuleTest {

    // --- canSync eligibility gate ---

    @Test
    fun `canSync is false when signed out`() {
        assertFalse(canSync(userId = null, ownerId = null))
        assertFalse(canSync(userId = null, ownerId = "user-A"))
    }

    @Test
    fun `canSync is false when local data belongs to another account or is unclaimed`() {
        assertFalse(canSync(userId = "user-A", ownerId = null))
        assertFalse(canSync(userId = "user-A", ownerId = "user-B"))
    }

    @Test
    fun `canSync is true when the signed-in user owns the local data`() {
        assertTrue(canSync(userId = "user-A", ownerId = "user-A"))
    }

    // --- 401 sign-out decision ---

    @Test
    fun `shouldSignOut is true for a postgrest unauthorized failure`() = runTest {
        val response = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) })
            .get("https://localhost/rest/v1/tasks")
        assertTrue(shouldSignOut(UnauthorizedRestException("unauthorized", response)))
    }

    @Test
    fun `shouldSignOut is false for transient failures`() {
        assertFalse(shouldSignOut(IOException("network down")))
        assertFalse(shouldSignOut(IllegalStateException("no auth user")))
    }
}
