package com.devicesync.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.devicesync.app.data.model.SyncFile
import com.devicesync.app.data.model.SyncStatus
import com.devicesync.app.data.repository.SyncFileRepository
import com.devicesync.app.data.repository.SyncPairRepository
import com.devicesync.app.util.DeviceUtil
import com.devicesync.app.util.FileUtil
import com.devicesync.app.util.NetworkUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WiFi-only background sync worker.
 * Scans the child folder, detects new/modified files, uploads them to Firebase Storage,
 * and records sync state in both Room (local) and Firestore (remote).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val networkUtil: NetworkUtil,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val syncFileRepository: SyncFileRepository,
    private val syncPairRepository: SyncPairRepository,
    private val deviceUtil: DeviceUtil
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_PAIR_ID = "pair_id"
        const val KEY_FOLDER_PATH = "folder_path"
        const val WORK_NAME_PREFIX = "sync_"

        fun buildRequest(pairId: String, folderPath: String): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
                        .build()
                )
                .setInputData(
                    workDataOf(
                        KEY_PAIR_ID to pairId,
                        KEY_FOLDER_PATH to folderPath
                    )
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

        fun buildOneTimeRequest(pairId: String, folderPath: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()
                )
                .setInputData(
                    workDataOf(
                        KEY_PAIR_ID to pairId,
                        KEY_FOLDER_PATH to folderPath
                    )
                )
                .build()
    }

    override suspend fun doWork(): Result {
        if (!networkUtil.isOnWifi()) {
            Timber.d("SyncWorker: not on WiFi, skipping sync")
            return Result.retry()
        }

        val uid = auth.currentUser?.uid ?: return Result.failure()
        val pairId = inputData.getString(KEY_PAIR_ID) ?: return Result.failure()
        val folderPath = inputData.getString(KEY_FOLDER_PATH) ?: return Result.failure()
        val folder = File(folderPath)

        if (!folder.exists() || !folder.isDirectory) {
            Timber.w("SyncWorker: folder does not exist: $folderPath")
            return Result.failure()
        }

        return try {
            syncFolder(uid, pairId, folder)
            syncPairRepository.updateLastSynced(uid, pairId)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: sync failed for pair $pairId")
            Result.retry()
        }
    }

    private suspend fun syncFolder(uid: String, pairId: String, folder: File) {
        val allFiles = FileUtil.walkFolder(folder)
        Timber.d("SyncWorker: found ${allFiles.size} files in ${folder.path}")

        for (file in allFiles) {
            try {
                syncFile(uid, pairId, file, folder)
            } catch (e: Exception) {
                Timber.e(e, "SyncWorker: failed to sync file ${file.name}")
                val existingRecord = syncFileRepository.getFileByLocalPath(file.absolutePath)
                if (existingRecord != null) {
                    syncFileRepository.updateStatusWithError(
                        existingRecord.id, SyncStatus.FAILED, e.message
                    )
                }
            }
        }
    }

    private suspend fun syncFile(uid: String, pairId: String, file: File, baseFolder: File) {
        val checksum = FileUtil.md5(file)
        val existing = syncFileRepository.getFileByLocalPath(file.absolutePath)

        // Skip if already synced and unchanged
        if (existing != null && existing.syncStatus == SyncStatus.SYNCED && existing.checksum == checksum) {
            Timber.v("SyncWorker: skipping unchanged file ${file.name}")
            return
        }

        val fileId = existing?.id ?: UUID.randomUUID().toString()
        val relativePath = FileUtil.relativePath(file, baseFolder)
        val remotePath = FileUtil.buildRemotePath(uid, pairId, relativePath)

        // Mark as syncing
        val syncFile = SyncFile(
            id = fileId,
            syncPairId = pairId,
            localPath = file.absolutePath,
            remotePath = remotePath,
            fileName = file.name,
            fileSizeBytes = file.length(),
            lastModified = file.lastModified(),
            checksum = checksum,
            syncStatus = SyncStatus.SYNCING
        )
        syncFileRepository.upsert(syncFile)

        // Upload to Firebase Storage
        val storageRef = storage.reference.child(remotePath)
        storageRef.putFile(android.net.Uri.fromFile(file)).await()

        // Mark as synced
        val syncedAt = System.currentTimeMillis()
        syncFileRepository.updateStatus(fileId, SyncStatus.SYNCED, syncedAt)

        // Mirror to Firestore so parent device sees status
        syncPairRepository.upsertFileRecord(
            uid, pairId, syncFile.copy(syncStatus = SyncStatus.SYNCED, syncedAt = syncedAt)
        )

        Timber.d("SyncWorker: synced ${file.name}")
    }
}
