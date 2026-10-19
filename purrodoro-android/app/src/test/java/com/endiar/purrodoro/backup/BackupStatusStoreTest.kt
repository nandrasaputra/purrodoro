package com.endiar.purrodoro.backup

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupStatusStoreTest {

    @Test
    fun `manual and phone timestamps stay independent and latest wins`() {
        val dir = File.createTempFile("backup-status", "").apply {
            delete()
            mkdirs()
        }
        assertNull(BackupStatusStore.read(dir).latestAt())

        BackupStatusStore.recordManualExport(dir, at = 10)
        BackupStatusStore.recordAutoBackup(dir, at = 30)
        val status = BackupStatusStore.read(dir)
        assertEquals(10L, status.manualExportAt)
        assertEquals(30L, status.autoBackupAt)
        assertEquals(30L, status.latestAt())

        BackupStatusStore.recordManualExport(dir, at = 40)
        val updated = BackupStatusStore.read(dir)
        assertEquals(40L, updated.manualExportAt)
        assertEquals(30L, updated.autoBackupAt)
        assertEquals(40L, updated.latestAt())
    }

    @Test
    fun `status file lives under backup-status so auto backup can exclude it`() {
        val dir = tempDir()
        BackupStatusStore.recordManualExport(dir, at = 1)
        val file = File(dir, "backup-status/status.txt")
        assertTrue(file.isFile)
        assertTrue(file.readText().contains("manual=1"))
    }

    @Test
    fun `corrupt lines and unknown keys are ignored`() {
        val dir = tempDir()
        val file = File(dir, "backup-status/status.txt")
        file.parentFile?.mkdirs()
        file.writeText(
            """
            garbage
            manual=nope
            other=99
            auto=15
            manual=7
            """.trimIndent(),
        )
        val status = BackupStatusStore.read(dir)
        assertEquals(7L, status.manualExportAt)
        assertEquals(15L, status.autoBackupAt)
        assertEquals(15L, status.latestAt())
    }

    @Test
    fun `latest uses the only timestamp that exists`() {
        val dir = tempDir()
        BackupStatusStore.recordAutoBackup(dir, at = 5)
        assertEquals(5L, BackupStatusStore.read(dir).latestAt())
        assertNull(BackupStatusStore.read(dir).manualExportAt)
    }

    private fun tempDir(): File = File.createTempFile("backup-status", "").apply {
        delete()
        mkdirs()
    }
}
