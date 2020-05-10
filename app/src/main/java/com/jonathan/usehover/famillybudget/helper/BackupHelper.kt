/*
 *   Copyright 2020 Jonathan MONGA
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.jonathan.usehover.famillybudget.helper

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import com.jonathan.usehover.famillybudget.auth.Auth
import com.jonathan.usehover.famillybudget.auth.AuthState
import com.jonathan.usehover.famillybudget.cloudstorage.CloudStorage
import com.jonathan.usehover.famillybudget.cloudstorage.FileMetaData
import com.jonathan.usehover.famillybudget.db.DB
import com.jonathan.usehover.famillybudget.db.impl.DB_NAME
import com.jonathan.usehover.famillybudget.iab.Iab
import com.jonathan.usehover.famillybudget.job.BackupJob
import com.jonathan.usehover.famillybudget.parameters.Parameters
import com.jonathan.usehover.famillybudget.parameters.saveLastBackupDate
import com.jonathan.usehover.famillybudget.parameters.setShouldResetInitDate
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import java.util.concurrent.TimeUnit

private const val BACKUP_JOB_REQUEST_TAG = "backuptag"

private const val BACKUP_VERSION = 1
private const val BACKUP_VERSION_FILENAME = "version"
private const val BACKUP_DB_FILENAME = "db_backup"

suspend fun backupDB(context: Context,
                     db: DB,
                     cloudStorage: CloudStorage,
                     auth: Auth,
                     parameters: Parameters,
                     iab: Iab): ListenableWorker.Result {
    val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser
    if( currentUser == null ) {
        Log.e(
            "BackupJob",
            "Not authenticated"
        )

        return ListenableWorker.Result.failure()
    }

    if( !iab.isUserPremium() ) {
        Log.e(
            "BackupJob",
            "Not premium"
        )

        return ListenableWorker.Result.failure()
    }

    try {
        db.use { openedDB ->
            openedDB.triggerForceWriteToDisk()
        }
    } catch (error: Throwable) {
        Log.e(
            "BackupJob",
            "Error writing DB to disk",
            error
        )

        return ListenableWorker.Result.failure()
    }

    val dbFileCopy = File(context.cacheDir, BACKUP_DB_FILENAME)
    try {
        context.getDatabasePath(DB_NAME)
            .copyTo(dbFileCopy, overwrite = true)
    } catch (error: Throwable) {
        Log.e(
            "BackupJob",
            "Error copying DB",
            error
        )

        return ListenableWorker.Result.failure()
    }

    val archiveVersionFile = File(context.cacheDir, BACKUP_VERSION_FILENAME)
    val archiveFile = File(context.cacheDir, "backup.zip")

    try {
        archiveVersionFile.writeBackupVersion()

        val archive = ZipFile(archiveFile)
        archive.addFile(dbFileCopy)
        archive.addFile(archiveVersionFile)

        cloudStorage.uploadFile(archiveFile, getRemoteBackupPath(currentUser.id))
        parameters.saveLastBackupDate(Date())
    } catch (error: Throwable) {
        Log.e(
            "BackupJob",
            "Error backuping",
            error
        )

        return ListenableWorker.Result.retry()
    } finally {
        try {
            dbFileCopy.delete()
            archiveVersionFile.delete()
            archiveFile.delete()
        } catch (error: Throwable) {
            Log.e(
                "BackupJob",
                "Error deleting temp file",
                error
            )
        }
    }

    return ListenableWorker.Result.success()
}

suspend fun getBackupDBMetaData(cloudStorage: CloudStorage,
                                auth: Auth): FileMetaData? {
    val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser
        ?: throw IllegalStateException("Not authenticated")

    return cloudStorage.getFileMetaData(getRemoteBackupPath(currentUser.id))
}

suspend fun restoreLatestDBBackup(context: Context,
                                  auth: Auth,
                                  cloudStorage: CloudStorage,
                                  iab: Iab,
                                  parameters: Parameters) {

    val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser
        ?: throw IllegalStateException("Not authenticated")

    if( !iab.isUserPremium() ) {
        throw IllegalStateException("User not premium")
    }

    val backupFile = File(context.cacheDir, "backup_download.zip")
    val backupFolder = File(context.cacheDir, "backup_download")

    try {
        cloudStorage.downloadFile(getRemoteBackupPath(currentUser.id), backupFile)

        val archive = ZipFile(backupFile)

        backupFolder.mkdir()
        archive.extractAll(backupFolder.absolutePath)

        val versionFile = File(backupFolder, BACKUP_VERSION_FILENAME)
        val dbBackupFile = File(backupFolder, BACKUP_DB_FILENAME)

        restoreDBBackup(versionFile.readBackupVersion(), dbBackupFile, context)
        parameters.setShouldResetInitDate(true)
    } finally {
        try {
            backupFile.delete()
            backupFolder.deleteRecursively()
        } catch (error: Throwable) {
            Log.e(
                "DB restore",
                "Error deleting temp file",
                error
            )
        }
    }
}

suspend fun deleteBackup(auth: Auth,
                         cloudStorage: CloudStorage,
                         iab: Iab): Boolean {
    val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser
        ?: throw IllegalStateException("Not authenticated")

    if( !iab.isUserPremium() ) {
        throw IllegalStateException("User not premium")
    }

    return cloudStorage.deleteFile(getRemoteBackupPath(currentUser.id))
}

private fun restoreDBBackup(backupVersion: Int, dbBackupFile: File, context: Context) {
    context.deleteDatabase(DB_NAME)
    dbBackupFile.copyTo(context.getDatabasePath(DB_NAME), overwrite = true)
}

private fun File.writeBackupVersion() {
    val writer = FileWriter(this)
    writer.append(BACKUP_VERSION.toString())
    writer.flush()
    writer.close()
}

private fun File.readBackupVersion(): Int {
    val reader = FileReader(this)

    val version = reader.readText().toInt()
    reader.close()

    return version
}

private fun getRemoteBackupPath(userId: String): String {
    return "user/$userId/backup.zip"
}

fun scheduleBackup(context: Context) {
    unscheduleBackup(context)

    val constraints = Constraints.Builder()
        .setRequiresCharging(true)
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val backupRequest = PeriodicWorkRequestBuilder<BackupJob>(7, TimeUnit.DAYS)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
        .setInitialDelay(1, TimeUnit.DAYS)
        .addTag(BACKUP_JOB_REQUEST_TAG)
        .build()

    WorkManager.getInstance(context).enqueue(backupRequest)
}

fun unscheduleBackup(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(BACKUP_JOB_REQUEST_TAG)
}

fun getBackupJobInfosLiveData(context: Context): LiveData<List<WorkInfo>> {
    return WorkManager.getInstance(context).getWorkInfosByTagLiveData(BACKUP_JOB_REQUEST_TAG)
}