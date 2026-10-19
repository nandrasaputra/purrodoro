package com.endiar.purrodoro.backup

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesTest {

    @Test
    fun `auto backup includes the database and skips audio and json copies`() {
        listOf(
            "src/main/res/xml/backup_rules.xml",
            "src/main/res/xml/data_extraction_rules.xml",
        ).forEach { path ->
            val text = File(path).readText()
            assertFalse(path, text.contains("purrodoro.db"))
            assertFalse(path, text.contains("purrodoro_auth_session"))
            assertTrue(path, text.contains("backups/"))
            assertTrue(path, text.contains("backup-status/"))
            assertTrue(path, text.contains("soundscapes/"))
            assertTrue(path, text.contains("soundscape-thumbs/"))
        }
        val extraction = File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue(extraction.contains("<cloud-backup>"))
        assertTrue(extraction.contains("<device-transfer>"))
    }
}
