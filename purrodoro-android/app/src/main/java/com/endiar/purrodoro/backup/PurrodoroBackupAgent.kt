package com.endiar.purrodoro.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import com.endiar.purrodoro.data.local.PurrodoroDatabase

/**
 * Checkpoints the Room WAL before Auto Backup copies files, so the latest
 * session history is inside `purrodoro.db`.
 */
class PurrodoroBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        runFullBackup(
            checkpoint = { checkpointWal() },
            packageFiles = { packageFullBackup(data) },
            recordSuccess = { BackupStatusStore.recordAutoBackup(filesDir) },
        )
    }

    private fun checkpointWal() {
        val db = PurrodoroDatabase.get(applicationContext)
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    private fun packageFullBackup(data: FullBackupDataOutput) {
        super.onFullBackup(data)
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) = Unit
}

/**
 * Checkpoint failures must not block packaging. A phone-backup timestamp is
 * written only after packaging returns, so a failed backup stays "Not yet".
 */
internal fun runFullBackup(
    checkpoint: () -> Unit,
    packageFiles: () -> Unit,
    recordSuccess: () -> Unit,
) {
    runCatching { checkpoint() }
    packageFiles()
    recordSuccess()
}
