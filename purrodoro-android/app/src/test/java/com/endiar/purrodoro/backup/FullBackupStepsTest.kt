package com.endiar.purrodoro.backup

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullBackupStepsTest {

    @Test
    fun `checkpoint failure still packages and then records success`() {
        val order = mutableListOf<String>()
        runFullBackup(
            checkpoint = { throw IOException("wal") },
            packageFiles = { order += "package" },
            recordSuccess = { order += "record" },
        )
        assertEquals(listOf("package", "record"), order)
    }

    @Test
    fun `packaging failure does not record a phone backup time`() {
        var recorded = false
        var packaged = false
        val error = runCatching {
            runFullBackup(
                checkpoint = { },
                packageFiles = {
                    packaged = true
                    throw IOException("transport")
                },
                recordSuccess = { recorded = true },
            )
        }.exceptionOrNull()
        assertTrue(error is IOException)
        assertTrue(packaged)
        assertEquals(false, recorded)
    }
}
